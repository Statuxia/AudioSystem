package com.audiosystem.wsgateway.serializer;

import com.audiosystem.wsgateway.dto.JobResultMessageEnvelope;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class JobResultMessageEnvelopeSerializer implements RedisSerializer<JobResultMessageEnvelope> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Class<?> getTargetType() {
        return JobResultMessageEnvelope.class;
    }

    @Override
    public byte[] serialize(@Nullable JobResultMessageEnvelope value) throws SerializationException {
        if (value == null) {
            throw new SerializationException("value is null");
        }

        return mapper.writeValueAsBytes(value);
    }

    @Override
    public @Nullable JobResultMessageEnvelope deserialize(byte @Nullable [] bytes) throws SerializationException {
        try {
            return bytes == null ? null : mapper.readValue(bytes, JobResultMessageEnvelope.class);
        } catch (JacksonException e) {
            throw new SerializationException("failed to deserialize job result message", e);
        }
    }
}
