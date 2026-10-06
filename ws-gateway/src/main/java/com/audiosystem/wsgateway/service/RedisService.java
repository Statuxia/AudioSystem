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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisService {

    public static final String QUEUE_POSITIONS_ZSET = "queuePositions";
    public static final String RECEIVED_JOB_RESULTS_TOPIC = "receivedJobResults";

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

    public Boolean addJobToQueuePositions(UUID jobId) {
        return queuePositionsTemplate.opsForZSet().add(
            QUEUE_POSITIONS_ZSET,
            jobId.toString(),
            jobId.getMostSignificantBits() >>> 16
        );
    }

    public Long removeJobFromQueuePositions(UUID jobId) {
        return queuePositionsTemplate.opsForZSet().remove(
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

    public Long publishResult(UUID jobId, JobResultMessage resultMessage) {
        return receivedJobResultsTemplate.convertAndSend(
            RECEIVED_JOB_RESULTS_TOPIC,
            new JobResultMessageEnvelope(jobId, resultMessage)
        );
    }
}
