package com.audiosystem.wsgateway.config.interceptor;

import com.audiosystem.wsgateway.service.SubscriptionRegistryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnsubscriptionMessageInterceptorTest {

    @InjectMocks
    private UnsubscriptionMessageInterceptor interceptor;

    @Mock
    private SubscriptionRegistryService subscriptionRegistryService;

    @Captor
    private ArgumentCaptor<String> sessionCaptor;
    @Captor
    private ArgumentCaptor<String> subscriptionCaptor;

    @Test
    void preSendDisconnectMessage() {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
        accessor.setSessionId("sess-1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        final Message<?> producedMessage = interceptor.preSend(message, null);

        assertSame(message, producedMessage);
        verify(subscriptionRegistryService, never()).unsubscribe(any(), any());
        verify(subscriptionRegistryService).disconnect(sessionCaptor.capture());
        assertEquals("sess-1", sessionCaptor.getValue());
    }

    @Test
    void preSendUnsubscribeMessage() {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
        accessor.setSessionId("sess-1");
        accessor.setSubscriptionId("sub-1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        final Message<?> producedMessage = interceptor.preSend(message, null);

        assertSame(message, producedMessage);
        verify(subscriptionRegistryService).unsubscribe(sessionCaptor.capture(), subscriptionCaptor.capture());
        verify(subscriptionRegistryService, never()).disconnect(any());
        assertEquals("sess-1", sessionCaptor.getValue());
        assertEquals("sub-1", subscriptionCaptor.getValue());
    }

    @Test
    void preSendUnsupportedMessageType() {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.HEARTBEAT);
        accessor.setSessionId("sess-1");
        accessor.setSubscriptionId("sub-1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        final Message<?> producedMessage = interceptor.preSend(message, null);

        assertSame(message, producedMessage);
        verify(subscriptionRegistryService, never()).unsubscribe(any(), any());
        verify(subscriptionRegistryService, never()).disconnect(any());
    }

    @Test
    void preSendNullMessageType() {
        final Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        final Message<?> producedMessage = interceptor.preSend(message, null);

        assertSame(message, producedMessage);
        verify(subscriptionRegistryService, never()).unsubscribe(any(), any());
        verify(subscriptionRegistryService, never()).disconnect(any());
    }

    @Test
    void preSendExceptionThrown() {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.UNSUBSCRIBE);
        accessor.setSessionId("sess-1");
        accessor.setSubscriptionId("sub-1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        BDDMockito.doThrow(new RuntimeException("something gone wrong"))
            .when(subscriptionRegistryService).unsubscribe(any(), any());

        final Message<?> producedMessage = assertDoesNotThrow(() -> interceptor.preSend(message, null));

        assertSame(message, producedMessage);
        verify(subscriptionRegistryService).unsubscribe(any(), any());
        verify(subscriptionRegistryService, never()).disconnect(any());
    }
}