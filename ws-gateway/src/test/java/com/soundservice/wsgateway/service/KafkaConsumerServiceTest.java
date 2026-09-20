package com.soundservice.wsgateway.service;

import com.soundservice.wsgateway.dto.JobQueueMessage;
import com.soundservice.wsgateway.dto.JobQueueResponse;
import com.soundservice.wsgateway.dto.JobResultMessage;
import com.soundservice.wsgateway.dto.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    @Mock
    private WebSocketMessageService webSocketMessageService;

    @Captor
    private ArgumentCaptor<UUID> jobIdCaptor;
    @Captor
    private ArgumentCaptor<JobQueueResponse> jobQueueResponseCaptor;

    @Test
    @DisplayName("tests notification order when consumes jobId higher up in the queue")
    void testNotificationByUnorderedResult() {
        final int limit = 5;
        prepareQueue(limit);

        kafkaConsumerService.consumeResult(
            new UUID(0, 3L).toString(),
            new JobResultMessage(JobStatus.DONE, 0L)
        );

        verify(webSocketMessageService, times(4))
            .sendMessage(jobIdCaptor.capture(), jobQueueResponseCaptor.capture());

        assertEquals(
            List.of(
                new UUID(0L, 0L),
                new UUID(0L, 1L),
                new UUID(0L, 2L),
                new UUID(0L, 4L)
            ),
            jobIdCaptor.getAllValues()
        );
        assertEquals(
            List.of(
                new JobQueueResponse(new UUID(0L, 0L), 1L),
                new JobQueueResponse(new UUID(0L, 1L), 2L),
                new JobQueueResponse(new UUID(0L, 2L), 3L),
                new JobQueueResponse(new UUID(0L, 4L), 4L)
            ),
            jobQueueResponseCaptor.getAllValues()
        );

    }

    private void prepareQueue(int queueLength) {
        for (int i = 0; i < queueLength; i++) {
            kafkaConsumerService.consumeQueue(
                new UUID(0L, i).toString(),
                new JobQueueMessage("mp3", 1F, 0F, false)
            );
        }
    }
}