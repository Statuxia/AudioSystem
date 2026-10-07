package com.audiosystem.processor.config;

import com.audiosystem.processor.deserializer.QueueMessageDeserializer;
import com.audiosystem.processor.dto.JobQueueMessage;
import com.audiosystem.processor.dto.JobResultMessage;
import com.audiosystem.processor.exception.RedisStatusUpdateException;
import com.audiosystem.processor.recoverer.DeadLetterPublisherRecovererWrapper;
import com.audiosystem.processor.serializer.JobResultMessageSerializer;
import com.audiosystem.processor.service.KafkaService;
import com.audiosystem.processor.service.RedisService;
import com.audiosystem.processor.validator.QueueMessageValidator;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    @Value("${spring.kafka.concurrency:1}")
    private Integer concurrency;

    @Bean
    public ProducerFactory<UUID, JobResultMessage> kafkaProducer() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JobResultMessageSerializer.class);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<UUID, JobResultMessage> kafkaTemplate() {
        return new KafkaTemplate<>(kafkaProducer());
    }

    @Bean
    public ConsumerFactory<UUID, JobQueueMessage> consumerFactory() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, QueueMessageDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALIDATOR_CLASS, QueueMessageValidator.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, JobQueueMessage> kafkaListenerContainerFactory(
        DefaultErrorHandler errorHandler,
        ConsumerFactory<UUID, JobQueueMessage> consumerFactory
    ) {
        final ConcurrentKafkaListenerContainerFactory<UUID, JobQueueMessage> factory
            = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.setConcurrency(concurrency);

        return factory;
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        final DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(
            Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
            new DelegatingByTypeSerializer(Map.of(
                UUID.class, new UUIDSerializer(),
                byte[].class, new ByteArraySerializer()
            )),
            new DelegatingByTypeSerializer(Map.of(
                byte[].class, new ByteArraySerializer(),
                JobQueueMessage.class, new JacksonJsonSerializer<>()
            ))
        );
        final KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(factory);
        return new DeadLetterPublishingRecoverer(kafkaTemplate);
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublisherRecovererWrapper dltWrapper) {
        final ExponentialBackOff backOff = new ExponentialBackOff();
        final DefaultErrorHandler handler = new DefaultErrorHandler(dltWrapper, backOff);
        handler.defaultFalse();
        handler.addRetryableExceptions(
            RedisConnectionFailureException.class,
            QueryTimeoutException.class,
            RedisStatusUpdateException.class
        );

        return handler;
    }

    @Bean
    public DeadLetterPublisherRecovererWrapper dltWrapper(
        DeadLetterPublishingRecoverer recoverer, KafkaService kafkaService, RedisService redisService
    ) {
        return new DeadLetterPublisherRecovererWrapper(recoverer, kafkaService, redisService);
    }
}
