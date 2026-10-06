package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.JobQueueMessage;
import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.dto.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    @Mock
    private RedisService redisService;

    @Test
    void testConsumeQueueAddJobToQueuePositions() {
        final UUID jobId = new UUID(0, 3L);
        final JobQueueMessage message = new JobQueueMessage("any", 1F, 1F);
        kafkaConsumerService.consumeQueue(jobId.toString(), message);

        verify(redisService).addJobToQueuePositions(jobId);
    }

    @Test
    void testConsumeQueueWrongKeyFormat() {
        kafkaConsumerService.consumeQueue("dd", new JobQueueMessage("any", 1F, 1F));
        verifyNoInteractions(redisService);
    }

    @Test
    void testConsumeResultCallsOperationsInOrder() {
        final UUID jobId = new UUID(0, 3L);
        final JobResultMessage message = new JobResultMessage(JobStatus.DONE, 0L);

        kafkaConsumerService.consumeResult(jobId.toString(), message);

        final InOrder inOrder = Mockito.inOrder(redisService);
        inOrder.verify(redisService).removeJobFromQueuePositions(jobId);
        inOrder.verify(redisService).publishResult(jobId, message);
    }

    @Test
    void testConsumeResultWrongKeyFormat() {
        kafkaConsumerService.consumeResult("dd", new JobResultMessage(JobStatus.DONE, 0L));
        verifyNoInteractions(redisService);
    }
}