package com.audiosystem.wsgateway.deserializer;

import com.audiosystem.wsgateway.dto.JobResultMessage;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;

public class ResultMessageDeserializer implements Deserializer<JobResultMessage> {
    private final StringDeserializer stringDeserializer = new StringDeserializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JobResultMessage deserialize(String topic, byte[] data) {
        if (data == null) return null;
        return objectMapper.readValue(data, JobResultMessage.class);
    }

    @Override
    public JobResultMessage deserialize(String topic, Headers headers, ByteBuffer data) {
        if (data == null) {
            return null;
        }

        final String deserialize = stringDeserializer.deserialize(topic, headers, data);
        return deserialize == null ? null : objectMapper.readValue(deserialize, JobResultMessage.class);
    }
}
