package com.audiosystem.api.service;

import com.audiosystem.api.dto.JobQueueMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    private static final UUID DEFAULT_KEY = UUID.randomUUID();
    private static final JobQueueMessage DEFAULT_MESSAGE = new JobQueueMessage(
        "format",
        1.0F,
        1.0F
    );

    @InjectMocks
    private KafkaProducerService service;

    @Mock
    private KafkaTemplate<String, JobQueueMessage> kafkaTemplate;

    @Test
    void testSendMessageTimeoutException() throws ExecutionException, InterruptedException, TimeoutException {
        final CompletableFuture mock = Mockito.mock(CompletableFuture.class);

        BDDMockito.doThrow(TimeoutException.class).when(mock).get(eq(10L), eq(TimeUnit.SECONDS));
        BDDMockito.doReturn(mock).when(kafkaTemplate).send(anyString(), anyString(), any(JobQueueMessage.class));

        assertFalse(service.sendMessage(DEFAULT_KEY, DEFAULT_MESSAGE));
        verify(mock).get(eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testSendMessageInterruptedException() throws ExecutionException, InterruptedException, TimeoutException {
        final CompletableFuture mock = Mockito.mock(CompletableFuture.class);

        BDDMockito.doThrow(InterruptedException.class).when(mock).get(eq(10L), eq(TimeUnit.SECONDS));
        BDDMockito.doReturn(mock).when(kafkaTemplate).send(anyString(), anyString(), any(JobQueueMessage.class));

        assertFalse(service.sendMessage(DEFAULT_KEY, DEFAULT_MESSAGE));
        assertTrue(Thread.interrupted());

        verify(mock).get(eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testSendMessageException() {
        BDDMockito.doThrow(RuntimeException.class).when(kafkaTemplate).send(anyString(), anyString(), any(JobQueueMessage.class));

        assertFalse(service.sendMessage(DEFAULT_KEY, DEFAULT_MESSAGE));
        verify(kafkaTemplate).send(anyString(), anyString(), any(JobQueueMessage.class));
    }

    @Test
    void testSendMessageValid() throws ExecutionException, InterruptedException, TimeoutException {
        final CompletableFuture mock = Mockito.mock(CompletableFuture.class);
        BDDMockito.doReturn(mock).when(kafkaTemplate).send(anyString(), anyString(), any(JobQueueMessage.class));

        assertTrue(service.sendMessage(DEFAULT_KEY, DEFAULT_MESSAGE));
        verify(mock).get(eq(10L), eq(TimeUnit.SECONDS));
    }
}