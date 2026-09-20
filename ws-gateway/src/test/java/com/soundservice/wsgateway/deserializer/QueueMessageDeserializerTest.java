package com.soundservice.wsgateway.deserializer;

import com.soundservice.wsgateway.dto.JobQueueMessage;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QueueMessageDeserializerTest {

    private final QueueMessageDeserializer deserializer = new QueueMessageDeserializer();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testDeserializeNullBytes() {
        assertNull(deserializer.deserialize("any", null));
        assertNull(deserializer.deserialize("any", new RecordHeaders(), (ByteBuffer) null));
    }

    @Test
    void testDeserializeMessage() {
        final JobQueueMessage message = new JobQueueMessage("mp3", 1F, 0F, false);
        final byte[] bytes = mapper.writeValueAsBytes(message);

        assertEquals(message, deserializer.deserialize("any", bytes));
    }
}