package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobStateItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.OptionalLong;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, JobStateItem> redisTemplate;

    public JobStateItem getState(UUID jobId) {
        return redisTemplate.opsForValue().get(jobId.toString());
    }

    public OptionalLong getJobIdRank(UUID jobId) {
        return OptionalLong.empty(); // todo: next step
    }
}
