package com.soundservice.processor.service;

import com.soundservice.processor.dto.JobQueueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class KafkaConsumerService {

    private final ProcessorService processorService;

    @KafkaListener(topics = "queue", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeQueueMessage(
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Payload JobQueueMessage message
    ) {
        log.debug("[{}] message: {}", key, message);

        final UUID jobId;
        try {
            jobId = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            log.error("[{}] key is not uuid format. Skip", key, e);
            return;
        }

        processorService.process(jobId, message);
    }
}
