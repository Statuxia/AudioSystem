package com.soundservice.wsgateway.service;

import com.soundservice.wsgateway.dto.JobStateItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, JobStateItem> redisTemplate;

    public JobStateItem getState(UUID jobId) {
        return redisTemplate.opsForValue().get(jobId.toString());
    }
}
