package com.soundservice.wsgateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Временный эндпоинт для ручной проверки STOMP-рассылки без Kafka.
 * Будет заменён Kafka-консьюмером topic:queue/topic:result.
 */
@RestController
@RequiredArgsConstructor
public class DebugPushController {

	private final SimpMessagingTemplate messagingTemplate;

	@PostMapping("/debug/push/{jobId}")
	public void push(@PathVariable String jobId, @RequestBody String payload) {
		messagingTemplate.convertAndSend("/topic/job/" + jobId, payload);
	}
}
