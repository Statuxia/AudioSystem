package com.audiosystem.processor.service;

import com.audiosystem.processor.dto.JobStateItem;
import com.audiosystem.processor.dto.JobStatus;
import com.audiosystem.processor.exception.RedisStatusUpdateException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, JobStateItem> redisTemplate;
    @Lazy
    private final RedisService instance;

    public boolean isInQueue(UUID key) {
        final JobStateItem item = redisTemplate.opsForValue().get(key.toString());
        return item == null || item.status() == JobStatus.IN_QUEUE;
    }

    public void setDone(UUID key) {
        instance.setStatus(key, JobStatus.DONE, Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli());
    }

    public void setError(UUID key) {
        instance.setStatus(key, JobStatus.ERROR, Instant.now().toEpochMilli());
    }

    @Retryable(includes = {RedisStatusUpdateException.class}, maxRetries = 3)
    public void setStatus(UUID key, JobStatus status, Long expireAt) {
        try {
            redisTemplate.opsForValue().set(key.toString(), new JobStateItem(key, status, expireAt));
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            throw new RedisStatusUpdateException("failed to set job status", e);
        }
    }
}
