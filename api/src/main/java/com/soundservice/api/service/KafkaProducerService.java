package com.soundservice.api.service;

import com.soundservice.api.dto.JobQueueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Log4j2
public class KafkaProducerService {

    private final KafkaTemplate<String, JobQueueMessage> kafkaTemplate;

    public boolean sendMessage(UUID key, JobQueueMessage message) {
        try {
            kafkaTemplate.send("queue", key.toString(), message).get(10, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException e) {
            log.error("timeout on sending message with key {} and message {}", key, message, e);
            return false;
        } catch (Exception e) {
            log.error("caught exception on sending message with key {} and message {}", key, message, e);
            return false;
        }
    }
}
