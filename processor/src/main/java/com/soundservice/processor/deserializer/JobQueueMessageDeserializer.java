package com.soundservice.processor.deserializer;

import com.soundservice.processor.dto.JobQueueMessage;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

public class JobQueueMessageDeserializer implements Deserializer<JobQueueMessage> {

    private final StringDeserializer stringDeserializer = new StringDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        stringDeserializer.configure(configs, isKey);
    }

    @Override
    public JobQueueMessage deserialize(String topic, byte[] data) {
        if (data == null) return null;
        return objectMapper.readValue(data, JobQueueMessage.class);
    }

    @Override
    public JobQueueMessage deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) {
            return null;
        }

        return objectMapper.readValue(stringDeserializer.deserialize(topic, headers, data), JobQueueMessage.class);
    }
}
