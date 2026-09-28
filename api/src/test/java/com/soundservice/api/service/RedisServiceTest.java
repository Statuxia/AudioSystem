package com.soundservice.api.service;

import com.soundservice.api.dto.JobStateItem;
import com.soundservice.api.dto.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @InjectMocks
    private RedisService redisService;

    @Mock
    private RedisTemplate<String, JobStateItem> redisTemplate;

    @Captor
    private ArgumentCaptor<String> keyCaptor;
    @Captor
    private ArgumentCaptor<JobStateItem> valueCaptor;
    @Captor
    private ArgumentCaptor<Duration> durationCaptor;

    @Test
    void testSaveJobStateValid() {
        final ValueOperations<String, JobStateItem> mock = BDDMockito.mock();
        BDDMockito.doReturn(mock).when(redisTemplate).opsForValue();

        final UUID key = UUID.randomUUID();
        final JobStatus status = JobStatus.IN_QUEUE;
        final Instant expireAt = Instant.now().plus(1, ChronoUnit.HOURS);

        redisService.saveJobState(key, status, expireAt);
        verify(mock).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());

        assertEquals(key.toString(), keyCaptor.getValue());
        assertEquals(new JobStateItem(key, status, expireAt.toEpochMilli()), valueCaptor.getValue());
        assertEquals(0, Instant.now().until(expireAt).minus(durationCaptor.getValue()).getSeconds(), 3);
    }
}