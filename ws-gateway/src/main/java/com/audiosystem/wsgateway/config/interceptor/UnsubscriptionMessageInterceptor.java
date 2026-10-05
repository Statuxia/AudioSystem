package com.audiosystem.wsgateway.config.interceptor;

import com.audiosystem.wsgateway.service.SubscriptionRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class UnsubscriptionMessageInterceptor implements ChannelInterceptor {

    private final SubscriptionRegistryService subscriptionRegistryService;

    @Override
    public @Nullable Message<?> preSend(
        @NonNull Message<?> message,
        @NonNull MessageChannel channel
    ) {
        final SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        final SimpMessageType messageType = accessor.getMessageType();

        try {
            switch (messageType) {
                case DISCONNECT -> disconnect(accessor.getSessionId());
                case UNSUBSCRIBE -> unsubscribe(accessor.getSessionId(), accessor.getSubscriptionId());
                case null, default -> {
                    // nothing to do
                }
            }
        } catch (Exception e) {
            log.error("caught exception on processing message type: {}", messageType, e);
        }
        return message;
    }

    void unsubscribe(String sessionId, String subscriptionId) {
        subscriptionRegistryService.unsubscribe(sessionId, subscriptionId);
    }

    void disconnect(String sessionId) {
        subscriptionRegistryService.disconnect(sessionId);
    }
}
