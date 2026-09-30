package com.audiosystem.processor.service;

import com.audiosystem.processor.dto.JobStateItem;
import com.audiosystem.processor.dto.JobStatus;
import com.audiosystem.processor.exception.RedisStatusUpdateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @InjectMocks
    private RedisService redisService;

    @Mock
    private RedisTemplate<String, JobStateItem> redisTemplate;

    @Mock
    private RedisService instance;

    @Captor
    private ArgumentCaptor<Long> expireAtCaptor;

    @Test
    void testSetDoneDelegatesWithOneDayExpiry() {
        final UUID key = UUID.randomUUID();
        final long expected = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();

        redisService.setDone(key);

        verify(instance).setStatus(eq(key), eq(JobStatus.DONE), expireAtCaptor.capture());
        assertEquals(0, expireAtCaptor.getValue() - expected, 3000);
    }

    @Test
    void testSetErrorDelegatesWithNowExpiry() {
        final UUID key = UUID.randomUUID();
        final long expected = Instant.now().toEpochMilli();

        redisService.setError(key);

        verify(instance).setStatus(eq(key), eq(JobStatus.ERROR), expireAtCaptor.capture());
        assertEquals(0, expireAtCaptor.getValue() - expected, 3000);
    }

    @Test
    void testSetStatusWritesJobStateItem() {
        final ValueOperations<String, JobStateItem> mock = BDDMockito.mock();

        BDDMockito.doReturn(mock).when(redisTemplate).opsForValue();

        final UUID key = UUID.randomUUID();

        assertDoesNotThrow(() -> redisService.setStatus(key, JobStatus.IN_QUEUE, 123L));
        verify(mock).set(key.toString(), new JobStateItem(key, JobStatus.IN_QUEUE, 123L));
    }

    @Test
    void testSetStatusWrapsConnectionFailure() {
        final ValueOperations<String, JobStateItem> mock = BDDMockito.mock();

        BDDMockito.doReturn(mock).when(redisTemplate).opsForValue();
        BDDMockito.doThrow(new RedisConnectionFailureException("down"))
            .when(mock).set(Mockito.anyString(), Mockito.any());

        assertThrows(
            RedisStatusUpdateException.class,
            () -> redisService.setStatus(UUID.randomUUID(), JobStatus.ERROR, 1L)
        );
    }

    @Test
    void testSetStatusWrapsQueryTimeout() {
        final ValueOperations<String, JobStateItem> mock = BDDMockito.mock();

        BDDMockito.doReturn(mock).when(redisTemplate).opsForValue();
        BDDMockito.doThrow(new QueryTimeoutException("timeout"))
            .when(mock).set(Mockito.anyString(), Mockito.any());

        assertThrows(
            RedisStatusUpdateException.class,
            () -> redisService.setStatus(UUID.randomUUID(), JobStatus.ERROR, 1L)
        );
    }
}
