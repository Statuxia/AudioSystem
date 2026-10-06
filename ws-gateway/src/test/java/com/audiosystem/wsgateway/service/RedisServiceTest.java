package com.audiosystem.wsgateway.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @InjectMocks
    private RedisService redisService;

    @Mock
    private RedisTemplate<String, String> queuePositionsTemplate;

    @Test
    void testGetJobIdRankReturnsEmptyIfNull() {
        final ZSetOperations<String, String> zSetOperations = Mockito.mock(ZSetOperations.class);
        BDDMockito.doReturn(zSetOperations).when(queuePositionsTemplate).opsForZSet();
        BDDMockito.doReturn(null).when(zSetOperations).rank(eq(RedisService.QUEUE_POSITIONS_ZSET), any());

        final OptionalLong rank = redisService.getJobIdRank(new UUID(0, 0));
        assertTrue(rank.isEmpty());
    }

    @Test
    void testGetJobIdRankReturnsRankPlusOne() {
        final ZSetOperations<String, String> zSetOperations = Mockito.mock(ZSetOperations.class);
        BDDMockito.doReturn(zSetOperations).when(queuePositionsTemplate).opsForZSet();
        BDDMockito.doReturn(1L).when(zSetOperations).rank(eq(RedisService.QUEUE_POSITIONS_ZSET), any());

        final OptionalLong rank = redisService.getJobIdRank(new UUID(0, 0));
        assertTrue(rank.isPresent());
        assertEquals(2, rank.getAsLong());
    }

    @Test
    void testGetJobIdsRankReturnsEmptyMapIfEmptySet() {
        final Map<UUID, Long> jobRanks = redisService.getJobIdsRank(List.of());
        assertTrue(jobRanks.isEmpty());
        assertSame(Collections.EMPTY_MAP, jobRanks);
    }

    @Test
    void testGetJobIdsRankReturnsEmptyLinkedHashMapIfAllJobsRankIsNulls() {
        final List<UUID> jobIds = List.of(new UUID(0, 0), new UUID(0, 1));
        final List<Object> objects = new ArrayList<>();
        objects.add(null);
        objects.add(null);

        BDDMockito.doReturn(objects).when(queuePositionsTemplate).executePipelined(any(SessionCallback.class));

        final Map<UUID, Long> jobRanks = redisService.getJobIdsRank(jobIds);
        assertTrue(jobRanks.isEmpty());
        assertNotSame(Collections.EMPTY_MAP, jobRanks);

        assertSessionCallback(jobIds, 2);
    }

    @Test
    void testGetJobIdsRankReturnsRanksPlusOne() {
        final List<UUID> jobIds = List.of(new UUID(0, 0), new UUID(0, 1));
        final List<Object> objects = List.of(2, 3);

        BDDMockito.doReturn(objects).when(queuePositionsTemplate).executePipelined(any(SessionCallback.class));

        final Map<UUID, Long> jobRanks = redisService.getJobIdsRank(jobIds);
        assertFalse(jobRanks.isEmpty());

        final Long rank1 = jobRanks.get(new UUID(0, 0));
        assertNotNull(rank1);
        assertEquals(3, rank1);

        final Long rank2 = jobRanks.get(new UUID(0, 1));
        assertNotNull(rank2);
        assertEquals(4, rank2);

        assertSessionCallback(jobIds, 2);
    }

    @Test
    void testGetJobIdsRankReturnsReturnsKnown() {
        final List<UUID> jobIds = List.of(new UUID(0, 0), new UUID(0, 1));
        final List<Object> objects = new ArrayList<>();
        objects.add(null);
        objects.add(3);

        BDDMockito.doReturn(objects).when(queuePositionsTemplate).executePipelined(any(SessionCallback.class));

        final Map<UUID, Long> jobRanks = redisService.getJobIdsRank(jobIds);
        assertFalse(jobRanks.isEmpty());

        final Long rank1 = jobRanks.get(new UUID(0, 0));
        assertNull(rank1);

        final Long rank2 = jobRanks.get(new UUID(0, 1));
        assertNotNull(rank2);
        assertEquals(4, rank2);

        assertSessionCallback(jobIds, 2);
    }

    @Test
    void testAddJobToQueuePositions() {
        final UUID uuidV7 = UUID.fromString("01a1126d-915a-7339-913c-df379ead1754");
        final long expectedTimestamp = 1791310532954L;
        final ZSetOperations<String, String> zSetOperations = Mockito.mock(ZSetOperations.class);
        final ArgumentCaptor<Double> captor = ArgumentCaptor.captor();

        BDDMockito.doReturn(zSetOperations).when(queuePositionsTemplate).opsForZSet();

        redisService.addJobToQueuePositions(uuidV7);

        verify(zSetOperations).add(eq(RedisService.QUEUE_POSITIONS_ZSET), eq(uuidV7.toString()), captor.capture());
        assertEquals(expectedTimestamp, captor.getValue());
    }

    private void assertSessionCallback(List<UUID> jobIds, int wantedTimes) {
        final ArgumentCaptor<SessionCallback> sessionCallbackArgumentCaptor = ArgumentCaptor.captor();
        final ArgumentCaptor<String> zSetValueArgumentCaptor = ArgumentCaptor.captor();
        final RedisOperations redisOperationsMock = Mockito.mock();
        final ZSetOperations zSetOperations = Mockito.mock();

        BDDMockito.doReturn(zSetOperations).when(redisOperationsMock).opsForZSet();

        verify(queuePositionsTemplate).executePipelined(sessionCallbackArgumentCaptor.capture());
        final SessionCallback sessionCallback = sessionCallbackArgumentCaptor.getValue();
        sessionCallback.execute(redisOperationsMock);

        verify(zSetOperations, times(wantedTimes)).rank(eq(RedisService.QUEUE_POSITIONS_ZSET), zSetValueArgumentCaptor.capture());

        assertEquals(jobIds.stream().map(UUID::toString).toList(), zSetValueArgumentCaptor.getAllValues());
    }
}