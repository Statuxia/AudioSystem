package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobQueueMessage;
import com.audiosystem.wsgateway.dto.JobResultMessage;
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

    private final RedisService redisService;

    @KafkaListener(
        topics = "queue",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "queueContainerFactory"
    )
    public void consumeQueue(
        @Header(KafkaHeaders.RECEIVED_KEY) UUID jobId,
        @Payload JobQueueMessage message
    ) {
        log.debug("[{}] message: {}", jobId, message);
        redisService.addJobToQueuePositions(jobId);
    }

    @KafkaListener(
        topics = "result",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "resultContainerFactory"
    )
    public void consumeResult(
        @Header(KafkaHeaders.RECEIVED_KEY) UUID jobId,
        @Payload JobResultMessage message
    ) {
        log.debug("[{}] message: {}", jobId, message);

        redisService.removeJobFromQueuePositions(jobId);
        redisService.publishResult(jobId, message);
    }
}
