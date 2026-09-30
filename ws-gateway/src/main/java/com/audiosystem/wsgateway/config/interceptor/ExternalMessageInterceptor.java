package com.audiosystem.wsgateway.config.interceptor;

import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;

public class ExternalMessageInterceptor implements ChannelInterceptor {
    public static final String BROKER_DESTINATION = "/topic";
    public static final String PUBLISHING_EXCEPTION_MESSAGE
        = "Publishing is not allowed to %s destination".formatted(BROKER_DESTINATION);

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        if (accessor.getMessageType() == SimpMessageType.MESSAGE) {
            final String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(BROKER_DESTINATION)) {
                throw new MessageDeliveryException(PUBLISHING_EXCEPTION_MESSAGE);
            }
        }
        return message;
    }
}
