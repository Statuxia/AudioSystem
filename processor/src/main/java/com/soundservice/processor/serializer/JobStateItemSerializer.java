package com.soundservice.processor.serializer;

import com.soundservice.processor.dto.JobStateItem;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

public class JobStateItemSerializer implements RedisSerializer<JobStateItem> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] serialize(@Nullable JobStateItem value) throws SerializationException {
        if (value == null) return new byte[0];
        return mapper.writeValueAsBytes(value);
    }

    @Override
    public @Nullable JobStateItem deserialize(byte @Nullable [] bytes) throws SerializationException {
        if (bytes == null) return null;
        return mapper.readValue(bytes, JobStateItem.class);
    }
}
