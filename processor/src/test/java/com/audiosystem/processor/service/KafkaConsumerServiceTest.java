package com.audiosystem.processor.service;

import com.audiosystem.processor.dto.JobQueueMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    @Mock
    private ProcessorService processorService;

    @Test
    void testConsumeValidUuidKeyDelegatesToProcessorService() {
        final UUID jobId = UUID.randomUUID();
        final JobQueueMessage message = new JobQueueMessage("mp3", 1.5F, -4F);

        kafkaConsumerService.consumeQueueMessage(jobId, message);

        verify(processorService).process(eq(jobId), eq(message));
    }
}
