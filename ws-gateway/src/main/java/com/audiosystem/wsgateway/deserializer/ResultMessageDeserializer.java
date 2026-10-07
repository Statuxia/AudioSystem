package com.audiosystem.wsgateway.deserializer;

import com.audiosystem.wsgateway.dto.JobResultMessage;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;

public class ResultMessageDeserializer implements Deserializer<JobResultMessage> {
    private final StringDeserializer stringDeserializer = new StringDeserializer();
    private final ObjectMapper objectMapper = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    @Override
    public JobResultMessage deserialize(String topic, byte[] data) {
        return objectMapper.readValue(data, JobResultMessage.class);
    }

    @Override
    public JobResultMessage deserialize(String topic, Headers headers, ByteBuffer data) {
        final String deserialize = stringDeserializer.deserialize(topic, headers, data);
        return objectMapper.readValue(deserialize, JobResultMessage.class);
    }
}
