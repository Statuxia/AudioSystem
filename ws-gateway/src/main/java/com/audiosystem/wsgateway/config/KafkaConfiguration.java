package com.audiosystem.wsgateway.config;

import com.audiosystem.wsgateway.deserializer.QueueMessageDeserializer;
import com.audiosystem.wsgateway.deserializer.ResultMessageDeserializer;
import com.audiosystem.wsgateway.dto.JobQueueMessage;
import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.listener.LoggingRetryListener;
import com.audiosystem.wsgateway.validator.ResultMessageValidator;
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
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
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
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;
    @Value("${spring.kafka.consumer.group-id}")
    private String kafkaGroupId;

    @Bean
    public ConsumerFactory<UUID, JobQueueMessage> queueConsumerFactory() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, QueueMessageDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<UUID, JobResultMessage> resultConsumerFactory() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, ResultMessageDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALIDATOR_CLASS, ResultMessageValidator.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, JobQueueMessage> queueContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<UUID, JobQueueMessage> factory
            = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(queueConsumerFactory());
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, JobResultMessage> resultContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<UUID, JobResultMessage> factory
            = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(resultConsumerFactory());
        factory.setCommonErrorHandler(defaultErrorHandler());
        return factory;
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        final DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(
            Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers),
            new DelegatingByTypeSerializer(Map.of(
                UUID.class, new UUIDSerializer(),
                byte[].class, new ByteArraySerializer()
            )),
            new DelegatingByTypeSerializer(Map.of(
                byte[].class, new ByteArraySerializer(),
                JobQueueMessage.class, new JacksonJsonSerializer<>(),
                JobResultMessage.class, new JacksonJsonSerializer<>()
            ))
        );
        final KafkaTemplate<Object, Object> kafkaTemplate = new KafkaTemplate<>(factory);
        return new DeadLetterPublishingRecoverer(kafkaTemplate);
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler() {
        final ExponentialBackOff backOff = new ExponentialBackOff();
        final DefaultErrorHandler handler = new DefaultErrorHandler(deadLetterPublishingRecoverer(), backOff);
        handler.defaultFalse();
        handler.setRetryListeners(new LoggingRetryListener());
        handler.addRetryableExceptions(RedisConnectionFailureException.class, QueryTimeoutException.class);

        return handler;
    }
}
