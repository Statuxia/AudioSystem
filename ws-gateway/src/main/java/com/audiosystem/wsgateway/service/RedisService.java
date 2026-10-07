package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.dto.JobResultMessageEnvelope;
import com.audiosystem.wsgateway.dto.JobStateItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisService {

    public static final String QUEUE_POSITIONS_ZSET = "queuePositions";
    public static final String RECEIVED_JOB_RESULTS_TOPIC = "receivedJobResults";
    public static final String FINISHED_KEY_PREFIX = "finished:";
    public static final RedisScript<Long> ADD_IF_NOT_FINISHED = new DefaultRedisScript<>(
        """
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            return redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])
            """, Long.class
    );

    private final RedisTemplate<String, JobStateItem> jobStatesTemplate;
    @Qualifier("queuePositionsTemplate")
    private final RedisTemplate<String, String> queuePositionsTemplate;
    @Qualifier("receivedJobResultsTemplate")
    private final RedisTemplate<String, JobResultMessageEnvelope> receivedJobResultsTemplate;

    public JobStateItem getState(UUID jobId) {
        return jobStatesTemplate.opsForValue().get(jobId.toString());
    }

    public OptionalLong getJobIdRank(UUID jobId) {
        final Long rank = queuePositionsTemplate.opsForZSet().rank(QUEUE_POSITIONS_ZSET, jobId.toString());
        return rank == null ? OptionalLong.empty() : OptionalLong.of(rank + 1);
    }

    public Map<UUID, Long> getJobIdsRank(List<UUID> jobIds) {
        if (CollectionUtils.isEmpty(jobIds)) {
            return Collections.emptyMap();
        }

        final Map<UUID, Long> positions = new LinkedHashMap<>();
        final List<Object> results = queuePositionsTemplate.executePipelined(
            new SessionCallback<>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    final ZSetOperations<String, String> zSet = operations.opsForZSet();
                    for (UUID jobId : jobIds) {
                        zSet.rank(
                            QUEUE_POSITIONS_ZSET,
                            jobId.toString()
                        );
                    }
                    return null;
                }
            }
        );

        for (int i = 0; i < jobIds.size(); i++) {
            final Object rank = results.get(i);

            if (rank != null) {
                positions.put(jobIds.get(i), ((Number) rank).longValue() + 1);
            }
        }

        return positions;
    }

    public void addJobToQueuePositions(UUID jobId) {
        queuePositionsTemplate.execute(
            ADD_IF_NOT_FINISHED,
            List.of(QUEUE_POSITIONS_ZSET, FINISHED_KEY_PREFIX + jobId),
            String.valueOf(jobId.getMostSignificantBits() >>> 16),
            jobId.toString()
        );
    }

    public void removeJobFromQueuePositions(UUID jobId) {
        queuePositionsTemplate.opsForValue().set(
            FINISHED_KEY_PREFIX + jobId,
            "1",
            Expiration.from(Duration.ofHours(1))
        );
        queuePositionsTemplate.opsForZSet().remove(
            QUEUE_POSITIONS_ZSET,
            jobId.toString()
        );
    }

    public Long removeJobQueueByOutdatedScore() {
        return queuePositionsTemplate.opsForZSet().removeRangeByScore(
            QUEUE_POSITIONS_ZSET,
            Double.NEGATIVE_INFINITY,
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()
        );
    }

    public void publishResult(UUID jobId, JobResultMessage resultMessage) {
        receivedJobResultsTemplate.convertAndSend(
            RECEIVED_JOB_RESULTS_TOPIC,
            new JobResultMessageEnvelope(jobId, resultMessage)
        );
    }
}
