package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;
    @Lazy
    @Qualifier("clientOutboundChannel")
    private final MessageChannel messageChannel;

    public void sendMessage(UUID jobId, JobResponse response) {
        messagingTemplate.convertAndSend(response.getDestination(jobId), response);
    }

    public void sendError(String sessionId, String message) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(message);
        accessor.setSessionId(sessionId);

        messageChannel.send(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));
    }
}
