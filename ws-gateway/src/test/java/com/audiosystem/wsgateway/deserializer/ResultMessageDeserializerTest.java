package com.audiosystem.wsgateway.deserializer;

import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.dto.JobStatus;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultMessageDeserializerTest {

    private final ResultMessageDeserializer deserializer = new ResultMessageDeserializer();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testDeserializeNullBytes() {
        assertNull(deserializer.deserialize("any", null));
        assertNull(deserializer.deserialize("any", new RecordHeaders(), (ByteBuffer) null));
    }

    @Test
    void testDeserializeMessage() {
        final JobResultMessage message = new JobResultMessage(JobStatus.DONE, 0L);
        final byte[] bytes = mapper.writeValueAsBytes(message);

        assertEquals(message, deserializer.deserialize("any", bytes));
    }
}