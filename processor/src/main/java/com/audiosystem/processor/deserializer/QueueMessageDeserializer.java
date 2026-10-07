package com.audiosystem.processor.deserializer;

import com.audiosystem.processor.dto.JobQueueMessage;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

public class QueueMessageDeserializer implements Deserializer<JobQueueMessage> {

    private final StringDeserializer stringDeserializer = new StringDeserializer();
    private final ObjectMapper objectMapper = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        stringDeserializer.configure(configs, isKey);
    }

    @Override
    public JobQueueMessage deserialize(String topic, byte[] data) {
        return objectMapper.readValue(data, JobQueueMessage.class);
    }

    @Override
    public JobQueueMessage deserialize(String topic, Headers headers, byte[] data) {
        return objectMapper.readValue(stringDeserializer.deserialize(topic, headers, data), JobQueueMessage.class);
    }
}
