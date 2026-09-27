package com.soundservice.api.serializer;

import com.soundservice.api.dto.JobQueueMessage;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

public class JobQueueMessageSerializer implements Serializer<JobQueueMessage> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, JobQueueMessage data) {
        return data == null ? null : mapper.writeValueAsBytes(data);
    }
}
