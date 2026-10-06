package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobQueueMessage;
import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.utils.JobIdUtils;
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
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Payload JobQueueMessage message
    ) {
        log.debug("[{}] message: {}", key, message);

        final UUID jobId = JobIdUtils.parseJobUuid(key);
        if (jobId == null) {
            log.warn("skipping wrong jobId format: {}", key);
            return;
        }

        redisService.addJobToQueuePositions(jobId);
    }

    @KafkaListener(
        topics = "result",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "resultContainerFactory"
    )
    public void consumeResult(
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Payload JobResultMessage message
    ) {
        log.debug("[{}] message: {}", key, message);

        final UUID jobId = JobIdUtils.parseJobUuid(key);
        if (jobId == null) {
            log.warn("skipping wrong jobId format: {}", key);
            return;
        }

        redisService.removeJobFromQueuePositions(jobId);
        redisService.publishResult(jobId, message);
    }
}
