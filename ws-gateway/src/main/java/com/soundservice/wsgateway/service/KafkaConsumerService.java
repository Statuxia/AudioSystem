package com.soundservice.wsgateway.service;

import com.soundservice.wsgateway.dto.JobQueueMessage;
import com.soundservice.wsgateway.dto.JobQueueResponse;
import com.soundservice.wsgateway.dto.JobResultMessage;
import com.soundservice.wsgateway.dto.JobStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final Map<String, Long> jobQueue = new ConcurrentHashMap<>();
    private final AtomicLong queueIndex = new AtomicLong(1);
    private final WebSocketMessageService webSocketMessageService;

    @KafkaListener(
        topics = "queue",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "queueContainerFactory"
    )
    public void consumeQueue(
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Payload JobQueueMessage message
    ) {
        final long queuePosition = queueIndex.getAndIncrement();
        jobQueue.putIfAbsent(key, queuePosition);
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
        final Long queuePosition = jobQueue.remove(key);
        if (queuePosition != null) {
            notifyAllQueue();
        }

        final UUID jobId = UUID.fromString(key);
        webSocketMessageService.sendMessage(jobId, new JobStateResponse(jobId, message.status(), message.expireAt()));
    }

    private void notifyAllQueue() {
        final Map<String, Long> queue = new HashMap<>(jobQueue);
        final List<String> sortedQueue = queue.entrySet().stream()
            .sorted(Comparator.comparingLong(Map.Entry::getValue))
            .map(Map.Entry::getKey).toList();

        for (int i = 0; i < sortedQueue.size(); i++) {
            final UUID targetJobId = UUID.fromString(sortedQueue.get(i));
            webSocketMessageService.sendMessage(
                targetJobId,
                new JobQueueResponse(targetJobId, i + 1L)
            );
        }
    }
}
