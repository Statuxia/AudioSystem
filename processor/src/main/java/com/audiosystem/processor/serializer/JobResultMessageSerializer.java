package com.audiosystem.processor.serializer;

import com.audiosystem.processor.dto.JobResultMessage;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

public class JobResultMessageSerializer implements Serializer<JobResultMessage> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, JobResultMessage data) {
        return data == null ? null : mapper.writeValueAsBytes(data);
    }
}
