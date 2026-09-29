package com.soundservice.processor.service;

import com.soundservice.processor.dto.JobResultMessage;
import com.soundservice.processor.dto.JobStatus;
import com.soundservice.processor.exception.KafkaSendMessageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

    @InjectMocks
    private KafkaService service;

    @Mock
    private KafkaTemplate<String, JobResultMessage> kafkaTemplate;

    @Captor
    private ArgumentCaptor<JobResultMessage> messageCaptor;

    @Test
    void testSendDoneMessageTimeoutException() throws ExecutionException, InterruptedException, TimeoutException {
        final CompletableFuture mock = Mockito.mock(CompletableFuture.class);

        BDDMockito.doThrow(TimeoutException.class).when(mock).get(eq(10L), eq(TimeUnit.SECONDS));
        BDDMockito.doReturn(mock).when(kafkaTemplate).send(Mockito.anyString(), Mockito.anyString(), Mockito.any());

        assertThrows(KafkaSendMessageException.class, () -> service.sendDoneMessage(UUID.randomUUID()));
        verify(mock).get(eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testSendErrorMessageInterruptedException() throws ExecutionException, InterruptedException, TimeoutException {
        final CompletableFuture mock = Mockito.mock(CompletableFuture.class);

        BDDMockito.doThrow(InterruptedException.class).when(mock).get(eq(10L), eq(TimeUnit.SECONDS));
        BDDMockito.doReturn(mock).when(kafkaTemplate).send(Mockito.anyString(), Mockito.anyString(), Mockito.any());

        assertThrows(KafkaSendMessageException.class, () -> service.sendErrorMessage(UUID.randomUUID()));
        assertTrue(Thread.interrupted());
        verify(mock).get(eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    void testSendMessageGenericException() {
        BDDMockito.doThrow(RuntimeException.class)
            .when(kafkaTemplate).send(Mockito.anyString(), Mockito.anyString(), Mockito.any());

        assertThrows(KafkaSendMessageException.class, () -> service.sendDoneMessage(UUID.randomUUID()));
    }

    @Test
    void testSendDoneMessageValid() throws ExecutionException, InterruptedException, TimeoutException {
        final UUID key = UUID.randomUUID();
        final CompletableFuture mock = Mockito.mock(CompletableFuture.class);

        BDDMockito.doReturn(mock).when(kafkaTemplate).send(eq("result"), eq(key.toString()), messageCaptor.capture());

        assertDoesNotThrow(() -> service.sendDoneMessage(key));
        verify(mock).get(eq(10L), eq(TimeUnit.SECONDS));

        final JobResultMessage sent = messageCaptor.getValue();
        assertEquals(JobStatus.DONE, sent.status());
        assertEquals(0, sent.expireAt() - Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli(), 3000);
    }

    @Test
    void testSendErrorMessageValid() throws ExecutionException, InterruptedException, TimeoutException {
        final UUID key = UUID.randomUUID();
        final CompletableFuture mock = Mockito.mock(CompletableFuture.class);

        BDDMockito.doReturn(mock).when(kafkaTemplate).send(eq("result"), eq(key.toString()), messageCaptor.capture());

        assertDoesNotThrow(() -> service.sendErrorMessage(key));
        verify(mock).get(eq(10L), eq(TimeUnit.SECONDS));

        final JobResultMessage sent = messageCaptor.getValue();
        assertEquals(JobStatus.ERROR, sent.status());
        assertEquals(0, sent.expireAt() - Instant.now().toEpochMilli(), 3000);
    }
}
