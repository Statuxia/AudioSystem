package com.audiosystem.wsgateway.integration;

import com.audiosystem.wsgateway.dto.JobQueueMessage;
import com.audiosystem.wsgateway.listener.LoggingRetryListener;
import com.audiosystem.wsgateway.service.RedisService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.*;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("kafka-test")
@DirtiesContext
@EmbeddedKafka(
    count = 1,
    partitions = 1,
    topics = {
        KafkaConsumerServiceIntegrationTest.QUEUE,
        KafkaConsumerServiceIntegrationTest.QUEUE_DLT,
        KafkaConsumerServiceIntegrationTest.RESULT
    }
)
public class KafkaConsumerServiceIntegrationTest {

    public static final String QUEUE = "queue";
    public static final String QUEUE_DLT = "queue-dlt";
    public static final String RESULT = "result";

    @MockitoBean
    private RedisService redisClient;

    @MockitoBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @MockitoSpyBean
    private LoggingRetryListener loggingRetryListener;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private DefaultErrorHandler defaultErrorHandler;

    @Test
    void testDefaultErrorHandlerDefaultFalseExceptionWithoutRetry(TestInfo info) throws ExecutionException, InterruptedException, TimeoutException {
        final KafkaTemplate<UUID, byte[]> template = getKafkaTemplate();
        final DefaultKafkaConsumerFactory<UUID, String> defaultKafkaConsumerFactory = getDltConsumer(info);
        waitForAssignment();

        // test data
        final UUID key = new UUID(0, 0);
        final byte[] value = new byte[0];

        // consuming records from dlt topic
        final ConsumerRecords<UUID, String> records;
        try (Consumer<UUID, String> consumer = defaultKafkaConsumerFactory.createConsumer()) {
            final TopicPartition dlt = new TopicPartition(QUEUE_DLT, 0);
            consumer.assign(List.of(dlt));
            consumer.seekToEnd(List.of(dlt));
            consumer.position(dlt);

            template.send(
                new ProducerRecord<>("queue", key, value)
            ).get(3, TimeUnit.SECONDS);

            records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(3));
        }

        // verify record exists
        assertFalse(records.isEmpty());
        final ConsumerRecord<UUID, String> record = records.iterator().next();
        assertEquals(key, record.key());
        assertArrayEquals(value, record.value().getBytes());

        // verify header with attempts returns 1
        final Header header = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
        assertNotNull(header);
        final Integer attempt = new IntegerDeserializer().deserialize("", header.value());
        assertEquals(1, attempt);

        // verify retrylistener delivers only once
        verify(loggingRetryListener, times(1)).failedDelivery(any(ConsumerRecord.class), any(), eq(1));
    }

    @Test
    void testDefaultErrorHandlerDefaultFalseExceptionWithRetry(TestInfo info) throws ExecutionException, InterruptedException, TimeoutException {
        final KafkaTemplate<UUID, byte[]> template = getKafkaTemplate();
        final DefaultKafkaConsumerFactory<UUID, String> defaultKafkaConsumerFactory = getDltConsumer(info);
        waitForAssignment();

        BDDMockito
            .doThrow(RedisConnectionFailureException.class)
            .doThrow(RedisConnectionFailureException.class)
            .doNothing()
            .when(redisClient).addJobToQueuePositions(any());

        // test data
        final UUID key = new UUID(0, 0);
        final byte[] value = new JacksonJsonSerializer(new TypeReference<JobQueueMessage>() {
        }).serialize("", new JobQueueMessage("mp3", 1f, 1f));

        // consuming records from dlt topic
        final ConsumerRecords<UUID, String> records;
        try (Consumer<UUID, String> consumer = defaultKafkaConsumerFactory.createConsumer()) {
            final TopicPartition dlt = new TopicPartition(QUEUE_DLT, 0);
            consumer.assign(List.of(dlt));
            consumer.seekToEnd(List.of(dlt));
            consumer.position(dlt);

            template.send(
                new ProducerRecord<>("queue", key, value)
            ).get(3, TimeUnit.SECONDS);

            records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(3));
        }

        // verify record is not exists
        assertTrue(records.isEmpty());

        // verify retrylistener delivers 2 times and never 3
        verify(redisClient, timeout(10_000).times(3)).addJobToQueuePositions(key);
        verify(loggingRetryListener, times(1)).failedDelivery(any(ConsumerRecord.class), any(), eq(1));
        verify(loggingRetryListener, times(1)).failedDelivery(any(ConsumerRecord.class), any(), eq(2));
        verify(loggingRetryListener, never()).failedDelivery(any(ConsumerRecord.class), any(), eq(3));
    }

    private void waitForAssignment() {
        registry.getListenerContainers().forEach(container -> {
            ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
        });
    }

    private @NonNull KafkaTemplate<UUID, byte[]> getKafkaTemplate() {
        final Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        final ProducerFactory<UUID, byte[]> pf = new DefaultKafkaProducerFactory<>(producerProps);
        return new KafkaTemplate<>(pf);
    }

    private @NonNull DefaultKafkaConsumerFactory<UUID, String> getDltConsumer(TestInfo info) {
        final String className = info.getTestClass().get().getName();
        final String methodName = info.getTestMethod().get().getName();

        final Map<String, Object> consumerProps
            = KafkaTestUtils.consumerProps(embeddedKafka, className + "#" + methodName, false);
        return new DefaultKafkaConsumerFactory<>(
            consumerProps, new UUIDDeserializer(), new StringDeserializer()
        );
    }
}
