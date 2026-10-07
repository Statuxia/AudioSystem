package com.audiosystem.processor.service;

import com.audiosystem.processor.dto.JobQueueMessage;
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
        @Header(KafkaHeaders.RECEIVED_KEY) UUID jobId,
        @Payload JobQueueMessage message
    ) {
        log.debug("[{}] message: {}", jobId, message);
        processorService.process(jobId, message);
    }
}
