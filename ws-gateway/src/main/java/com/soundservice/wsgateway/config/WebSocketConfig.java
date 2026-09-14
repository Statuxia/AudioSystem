package com.soundservice.wsgateway.config;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String BROKER_DESTINATION = "/topic";
    private static final String PUBLISHING_EXCEPTION_MESSAGE
        = "Publishing is not allowed to %s destination".formatted(BROKER_DESTINATION);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            new ChannelInterceptor() {
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
        );
    }
}
