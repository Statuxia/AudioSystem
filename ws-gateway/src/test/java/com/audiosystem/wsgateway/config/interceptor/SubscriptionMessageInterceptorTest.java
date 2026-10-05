package com.audiosystem.wsgateway.config.interceptor;

import com.audiosystem.wsgateway.dto.JobQueueResponse;
import com.audiosystem.wsgateway.dto.JobStateItem;
import com.audiosystem.wsgateway.dto.JobStateResponse;
import com.audiosystem.wsgateway.dto.JobStatus;
import com.audiosystem.wsgateway.service.RedisService;
import com.audiosystem.wsgateway.service.SubscriptionRegistryService;
import com.audiosystem.wsgateway.service.WebSocketMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionMessageInterceptorTest {

    @InjectMocks
    private SubscriptionMessageInterceptor interceptor;

    @Mock
    private RedisService redisService;

    @Mock
    private SubscriptionRegistryService subscriptionRegistryService;

    @Mock
    private WebSocketMessageService webSocketMessageService;

    @ParameterizedTest
    @MethodSource("skippedCases")
    void testAfterMessageHandledSkippedCases(
        SimpMessageType messageType,
        MessageHandler messageHandler,
        Exception ex,
        String destination
    ) {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService, never()).getState(any());
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testAfterMessageHandledUnknownTopic() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000/garbage";

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setSessionId("sess");
        accessor.setSubscriptionId("sub");
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService, never()).getState(any());
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testAfterMessageHandledThrowRedisException() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setSessionId("sess");
        accessor.setSubscriptionId("sub");
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doThrow(RedisConnectionFailureException.class)
            .when(redisService).getState(jobId);

        assertDoesNotThrow(() -> interceptor.afterMessageHandled(message, null, messageHandler, ex));
        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getState(jobId);
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testAfterMessageHandledThrowAnyException() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setSessionId("sess");
        accessor.setSubscriptionId("sub");
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doThrow(RuntimeException.class)
            .when(redisService).getState(jobId);

        assertDoesNotThrow(() -> interceptor.afterMessageHandled(message, null, messageHandler, ex));
        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getState(jobId);
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testProcessSubscribeToQueueNoRank() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000/queue";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setSessionId("sess");
        accessor.setSubscriptionId("sub");
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService).subscribe("sess", "sub", jobId);
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getJobIdRank(any());
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testProcessSubscribeToQueueNoQueuePosition() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000/queue";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setSessionId("sess");
        accessor.setSubscriptionId("sub");
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doReturn(OptionalLong.of(1L)).when(redisService).getJobIdRank(jobId);

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService).subscribe("sess", "sub", jobId);
        verify(subscriptionRegistryService).updateQueuePosition(any(), anyLong());
        verify(redisService).getJobIdRank(jobId);
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testProcessSubscribeToQueueThrowExceptionAfterSubscribe() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000/queue";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setSessionId("sess");
        accessor.setSubscriptionId("sub");
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doThrow(RedisConnectionFailureException.class).when(redisService).getJobIdRank(jobId);

        assertDoesNotThrow(() -> interceptor.afterMessageHandled(message, null, messageHandler, ex));
        verify(subscriptionRegistryService).subscribe("sess", "sub", jobId);
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getJobIdRank(jobId);
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testProcessSubscribeToQueueWithSendMessage() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000/queue";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setSessionId("sess");
        accessor.setSubscriptionId("sub");
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doReturn(OptionalLong.of(3L)).when(redisService).getJobIdRank(jobId);
        BDDMockito.doReturn(OptionalLong.of(2L))
            .when(subscriptionRegistryService).updateQueuePosition(jobId, 3L);

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService).subscribe("sess", "sub", jobId);
        verify(subscriptionRegistryService).updateQueuePosition(jobId, 3L);
        verify(redisService).getJobIdRank(jobId);
        verify(webSocketMessageService).sendMessage(jobId, new JobQueueResponse(jobId, 2L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/"})
    void testSendResultIfTerminalNoState(String destinationSuffix) {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000" + destinationSuffix;
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getState(jobId);
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testSendResultInTerminalInQueueState() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doReturn(new JobStateItem(jobId, JobStatus.IN_QUEUE, 1L))
            .when(redisService).getState(jobId);

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getState(jobId);
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService, never()).sendMessage(any(), any());
    }

    @Test
    void testSendResultInTerminalDoneState() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doReturn(new JobStateItem(jobId, JobStatus.DONE, 2L))
            .when(redisService).getState(jobId);

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getState(jobId);
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService).sendMessage(jobId, new JobStateResponse(jobId, JobStatus.DONE, 2L));
    }

    @Test
    void testSendResultInTerminalErrorState() {
        final SimpMessageType messageType = SimpMessageType.SUBSCRIBE;
        final SimpleBrokerMessageHandler messageHandler = Mockito.mock(SimpleBrokerMessageHandler.class);
        final Exception ex = null;
        final String destination = "/topic/job/00000000-0000-0000-0000-000000000000";
        final UUID jobId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(messageType);
        accessor.setDestination(destination);
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doReturn(new JobStateItem(jobId, JobStatus.ERROR, 1L))
            .when(redisService).getState(jobId);

        interceptor.afterMessageHandled(message, null, messageHandler, ex);

        verify(subscriptionRegistryService, never()).subscribe(any(), any(), any());
        verify(subscriptionRegistryService, never()).updateQueuePosition(any(), anyLong());
        verify(redisService).getState(jobId);
        verify(redisService, never()).getJobIdRank(any());
        verify(webSocketMessageService).sendMessage(jobId, new JobStateResponse(jobId, JobStatus.ERROR, 1L));
    }

    private static Stream<Arguments> skippedCases() {
        return Stream.of(
            Arguments.argumentSet(
                "invalid message type",
                SimpMessageType.MESSAGE,
                Mockito.mock(SimpleBrokerMessageHandler.class),
                null,
                "/topic/job/00000000-0000-0000-0000-000000000000/queue"
            ),
            Arguments.argumentSet(
                "wrong message handler",
                SimpMessageType.SUBSCRIBE,
                Mockito.mock(WebSocketAnnotationMethodMessageHandler.class),
                null,
                "/topic/job/00000000-0000-0000-0000-000000000000/queue"
            ),
            Arguments.argumentSet(
                "exception persist",
                SimpMessageType.SUBSCRIBE,
                Mockito.mock(SimpleBrokerMessageHandler.class),
                new RuntimeException(),
                "/topic/job/00000000-0000-0000-0000-000000000000/queue"
            ),
            Arguments.argumentSet(
                "no destination",
                SimpMessageType.SUBSCRIBE,
                Mockito.mock(SimpleBrokerMessageHandler.class),
                null,
                null
            ),
            Arguments.argumentSet(
                "wrong destination prefix",
                SimpMessageType.SUBSCRIBE,
                Mockito.mock(SimpleBrokerMessageHandler.class),
                null,
                "/topic/jjob/"
            ),
            Arguments.argumentSet(
                "empty path",
                SimpMessageType.SUBSCRIBE,
                Mockito.mock(SimpleBrokerMessageHandler.class),
                null,
                "/topic/job/"
            ),
            Arguments.argumentSet(
                "bad job format",
                SimpMessageType.SUBSCRIBE,
                Mockito.mock(SimpleBrokerMessageHandler.class),
                null,
                "/topic/job/1234/queue"
            )
        );
    }
}