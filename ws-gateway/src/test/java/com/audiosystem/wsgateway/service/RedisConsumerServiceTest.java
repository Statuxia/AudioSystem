package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisConsumerServiceTest {

    private static final UUID JOB_ID = new UUID(0, 0);
    private static final UUID SUBSCRIBED_JOB_ID_1 = new UUID(0, 1);
    private static final UUID SUBSCRIBED_JOB_ID_2 = new UUID(0, 2);
    private static final UUID SUBSCRIBED_JOB_ID_3 = new UUID(0, 3);

    @InjectMocks
    private RedisConsumerService redisConsumerService;

    @Mock
    private RedisService redisService;

    @Mock
    private SubscriptionRegistryService subscriptionRegistryService;

    @Mock
    private WebSocketMessageService webSocketMessageService;

    @Captor
    private ArgumentCaptor<List<UUID>> jobIdsCaptor;

    @Test
    void consumeResultSendsResultThenQueuePositions() {
        final JobResultMessage resultMessage = new JobResultMessage(JobStatus.DONE, 1L);
        final JobStateResponse resultResponse = new JobStateResponse(JOB_ID, resultMessage.status(), resultMessage.expireAt());
        final JobQueueResponse positionResponse1 = new JobQueueResponse(SUBSCRIBED_JOB_ID_1, 1L);
        final JobQueueResponse positionResponse3 = new JobQueueResponse(SUBSCRIBED_JOB_ID_3, 3L);

        // RedisService.getJobIdsRank order
        final Map<UUID, Long> queuePositions = new LinkedHashMap<>();
        queuePositions.put(SUBSCRIBED_JOB_ID_1, 1L);
        queuePositions.put(SUBSCRIBED_JOB_ID_2, 2L);
        queuePositions.put(SUBSCRIBED_JOB_ID_3, 3L);

        BDDMockito.doReturn(Set.of(SUBSCRIBED_JOB_ID_1, SUBSCRIBED_JOB_ID_2, SUBSCRIBED_JOB_ID_3))
            .when(subscriptionRegistryService).getAllSubscribedJobs();
        BDDMockito.doReturn(queuePositions).when(redisService).getJobIdsRank(anyList());
        BDDMockito.doReturn(OptionalLong.of(1L))
            .when(subscriptionRegistryService).decreaseQueuePosition(SUBSCRIBED_JOB_ID_1, 1L);
        // position has not decreased — nothing to send
        BDDMockito.doReturn(OptionalLong.empty())
            .when(subscriptionRegistryService).decreaseQueuePosition(SUBSCRIBED_JOB_ID_2, 2L);
        BDDMockito.doReturn(OptionalLong.of(3L))
            .when(subscriptionRegistryService).decreaseQueuePosition(SUBSCRIBED_JOB_ID_3, 3L);

        // lenient: sendMessage is also called with other args (strict stubs would fail)
        Mockito.lenient().doThrow(new RuntimeException())
            .when(webSocketMessageService).sendMessage(SUBSCRIBED_JOB_ID_1, positionResponse1);

        assertDoesNotThrow(() -> redisConsumerService.consumeResult(new JobResultMessageEnvelope(JOB_ID, resultMessage)));

        final InOrder inOrder = Mockito.inOrder(redisService, subscriptionRegistryService, webSocketMessageService);
        inOrder.verify(webSocketMessageService).sendMessage(JOB_ID, resultResponse);
        inOrder.verify(subscriptionRegistryService).getAllSubscribedJobs();
        inOrder.verify(redisService).getJobIdsRank(jobIdsCaptor.capture());
        inOrder.verify(subscriptionRegistryService).decreaseQueuePosition(SUBSCRIBED_JOB_ID_1, 1L);
        inOrder.verify(webSocketMessageService).sendMessage(SUBSCRIBED_JOB_ID_1, positionResponse1);
        inOrder.verify(subscriptionRegistryService).decreaseQueuePosition(SUBSCRIBED_JOB_ID_2, 2L);
        inOrder.verify(subscriptionRegistryService).decreaseQueuePosition(SUBSCRIBED_JOB_ID_3, 3L);
        inOrder.verify(webSocketMessageService).sendMessage(SUBSCRIBED_JOB_ID_3, positionResponse3);

        assertEquals(
            Set.of(SUBSCRIBED_JOB_ID_1, SUBSCRIBED_JOB_ID_2, SUBSCRIBED_JOB_ID_3),
            Set.copyOf(jobIdsCaptor.getValue())
        );
        verify(webSocketMessageService, never()).sendMessage(eq(SUBSCRIBED_JOB_ID_2), any());
    }

    @Test
    void consumeResultSendsResultEvenIfRedisFails() {
        final JobResultMessage resultMessage = new JobResultMessage(JobStatus.ERROR, 1L);
        final JobStateResponse resultResponse = new JobStateResponse(JOB_ID, resultMessage.status(), resultMessage.expireAt());

        BDDMockito.doReturn(Set.of(SUBSCRIBED_JOB_ID_1)).when(subscriptionRegistryService).getAllSubscribedJobs();
        BDDMockito.doThrow(new QueryTimeoutException("redis is unavailable"))
            .when(redisService).getJobIdsRank(anyList());

        assertThrows(
            QueryTimeoutException.class,
            () -> redisConsumerService.consumeResult(new JobResultMessageEnvelope(JOB_ID, resultMessage))
        );

        verify(webSocketMessageService).sendMessage(JOB_ID, resultResponse);
        verify(subscriptionRegistryService, never()).decreaseQueuePosition(any(), anyLong());
    }
}
