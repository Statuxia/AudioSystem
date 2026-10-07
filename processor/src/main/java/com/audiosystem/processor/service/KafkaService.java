package com.audiosystem.processor.service;

import com.audiosystem.processor.dto.JobResultMessage;
import com.audiosystem.processor.dto.JobStatus;
import com.audiosystem.processor.exception.KafkaSendMessageException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<UUID, JobResultMessage> kafkaTemplate;

    public void sendDoneMessage(UUID jobId) {
        sendMessage(
            jobId,
            new JobResultMessage(JobStatus.DONE, Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())
        );
    }

    public void sendErrorMessage(UUID jobId) {
        sendMessage(
            jobId,
            new JobResultMessage(JobStatus.ERROR, Instant.now().toEpochMilli())
        );
    }

    private void sendMessage(UUID jobId, JobResultMessage message) {
        try {
            kafkaTemplate.send("result", jobId, message).get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new KafkaSendMessageException("operation processes too long", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaSendMessageException("thread interrupted", e);
        } catch (Exception e) {
            throw new KafkaSendMessageException("caught exception on sending message", e);
        }
    }
}
