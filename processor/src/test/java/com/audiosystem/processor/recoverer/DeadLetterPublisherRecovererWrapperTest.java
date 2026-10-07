package com.audiosystem.processor.recoverer;

import com.audiosystem.processor.service.KafkaService;
import com.audiosystem.processor.service.RedisService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadLetterPublisherRecovererWrapperTest {

    @InjectMocks
    private DeadLetterPublisherRecovererWrapper dltWrapper;
    @Mock
    private DeadLetterPublishingRecoverer dlt;
    @Mock
    private KafkaService kafkaService;
    @Mock
    private RedisService redisService;

    @Test
    void testNotUUID() {
        final ConsumerRecord record = Mockito.mock();

        dltWrapper.accept(record, null);

        final InOrder inOrder = inOrder(dlt);
        inOrder.verify(dlt).accept(any(), any());

        verify(kafkaService, never()).sendErrorMessage(any());
        verify(redisService, never()).setError(any());
    }

    @Test
    void testUUID() {
        final ConsumerRecord record = Mockito.mock();

        BDDMockito.doReturn(new UUID(0, 1)).when(record).key();

        dltWrapper.accept(record, null);

        final InOrder inOrder = inOrder(dlt, redisService, kafkaService);
        inOrder.verify(dlt).accept(any(), any());
        inOrder.verify(redisService).setError(any());
        inOrder.verify(kafkaService).sendErrorMessage(any());
    }

    @Test
    void testDLTThrowsException() {
        final ConsumerRecord record = Mockito.mock();

        BDDMockito.doThrow(new RuntimeException()).when(dlt).accept(any(), any());

        assertThrows(RuntimeException.class, () -> dltWrapper.accept(record, null));

        final InOrder inOrder = inOrder(dlt);
        inOrder.verify(dlt).accept(any(), any());

        verify(kafkaService, never()).sendErrorMessage(any());
        verify(redisService, never()).setError(any());
    }

    @Test
    void testRedisThrowsException() {
        final ConsumerRecord record = Mockito.mock();

        BDDMockito.doReturn(new UUID(0, 1)).when(record).key();
        BDDMockito.doThrow(new RuntimeException()).when(redisService).setError(any());

        assertDoesNotThrow(() -> dltWrapper.accept(record, null));

        final InOrder inOrder = inOrder(dlt, redisService, kafkaService);
        inOrder.verify(dlt).accept(any(), any());
        inOrder.verify(redisService).setError(any());
        inOrder.verify(kafkaService).sendErrorMessage(any());
    }

    @Test
    void testKafkaThrowsException() {
        final ConsumerRecord record = Mockito.mock();

        BDDMockito.doReturn(new UUID(0, 1)).when(record).key();
        BDDMockito.doThrow(new RuntimeException()).when(kafkaService).sendErrorMessage(any());

        assertDoesNotThrow(() -> dltWrapper.accept(record, null));

        final InOrder inOrder = inOrder(dlt, redisService, kafkaService);
        inOrder.verify(dlt).accept(any(), any());
        inOrder.verify(redisService).setError(any());
        inOrder.verify(kafkaService).sendErrorMessage(any());
    }
}