package com.audiosystem.api.service;

import com.audiosystem.api.dto.JobStateItem;
import com.audiosystem.api.dto.JobStatus;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, JobStateItem> redisTemplate;
    @Lazy
    private final RedisService instance;

    public void saveInQueueJobState(UUID jobId) {
        instance.saveJobState(jobId, JobStatus.IN_QUEUE, Instant.now().plus(1, ChronoUnit.HOURS));
    }

    @Retryable(includes = {RedisConnectionFailureException.class, QueryTimeoutException.class}, maxRetries = 3)
    public void saveJobState(UUID jobId, JobStatus status, Instant expireAt) {
        redisTemplate.opsForValue().set(
            jobId.toString(),
            new JobStateItem(jobId, status, expireAt.toEpochMilli()),
            Instant.now().until(expireAt)
        );
    }

    public @Nullable JobStateItem getJobState(UUID jobId) {
        return redisTemplate.opsForValue().get(jobId.toString());
    }
}
