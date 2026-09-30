package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(UUID jobId, JobResponse response) {
        messagingTemplate.convertAndSend(response.getDestination(jobId), response);
    }
}
