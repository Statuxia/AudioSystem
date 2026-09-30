package com.audiosystem.wsgateway.config.interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalMessageInterceptorTest {

    private final ExternalMessageInterceptor interceptor = new ExternalMessageInterceptor();

    @Test
    void testPreventMessageDelivery() {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setDestination("/topic/job/1");
        final Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(
            MessageDeliveryException.class,
            () -> interceptor.preSend(message, null),
            ExternalMessageInterceptor.PUBLISHING_EXCEPTION_MESSAGE
        );
    }

    @ParameterizedTest
    @EnumSource(value = SimpMessageType.class, names = {"SUBSCRIBE", "UNSUBSCRIBE"})
    void testSubscribeMessageDelivery(SimpMessageType type) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(type);
        accessor.setDestination("/topic/job/1");
        accessor.setLeaveMutable(true);

        assertDoesNotThrow(
            () -> interceptor.preSend(new GenericMessage<>(new byte[0], accessor.getMessageHeaders()), null)
        );
    }

}