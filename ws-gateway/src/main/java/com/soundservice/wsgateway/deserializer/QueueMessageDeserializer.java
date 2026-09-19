package com.soundservice.wsgateway.deserializer;

import com.soundservice.wsgateway.dto.JobQueueMessage;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;

public class QueueMessageDeserializer implements Deserializer<JobQueueMessage> {
    private final StringDeserializer stringDeserializer = new StringDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JobQueueMessage deserialize(String topic, byte[] data) {
        if (data == null) return null;
        return objectMapper.readValue(data, JobQueueMessage.class);
    }

    @Override
    public JobQueueMessage deserialize(String topic, Headers headers, ByteBuffer data) {
        if (data == null) {
            return null;
        }

        final String deserialize = stringDeserializer.deserialize(topic, headers, data);
        return deserialize == null ? null : objectMapper.readValue(deserialize, JobQueueMessage.class);
    }
}
