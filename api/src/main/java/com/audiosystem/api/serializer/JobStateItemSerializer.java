package com.audiosystem.api.serializer;

import com.audiosystem.api.dto.JobStateItem;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class JobStateItemSerializer implements RedisSerializer<JobStateItem> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serialize(@Nullable JobStateItem value) throws SerializationException {
        if (value == null) {
            throw new SerializationException("value is null");
        }

        return mapper.writeValueAsBytes(value);
    }

    @Override
    public @Nullable JobStateItem deserialize(byte @Nullable [] bytes) throws SerializationException {
        try {
            return bytes == null ? null : mapper.readValue(bytes, JobStateItem.class);
        } catch (JacksonException e) {
            throw new SerializationException("failed to deserialize job state", e);
        }
    }
}
