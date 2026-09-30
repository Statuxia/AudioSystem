package com.audiosystem.wsgateway.config;

import com.audiosystem.wsgateway.deserializer.QueueMessageDeserializer;
import com.audiosystem.wsgateway.deserializer.ResultMessageDeserializer;
import com.audiosystem.wsgateway.dto.JobQueueMessage;
import com.audiosystem.wsgateway.dto.JobResultMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;
    @Value("${spring.kafka.consumer.group-id}")
    private String kafkaGroupId;

    @Bean
    public ConsumerFactory<String, JobQueueMessage> queueConsumerFactory() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, QueueMessageDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, JobResultMessage> resultConsumerFactory() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResultMessageDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JobQueueMessage> queueContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, JobQueueMessage> factory
            = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(queueConsumerFactory());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, JobResultMessage> resultContainerFactory() {

        final ConcurrentKafkaListenerContainerFactory<String, JobResultMessage> factory
            = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(resultConsumerFactory());
        return factory;
    }
}
