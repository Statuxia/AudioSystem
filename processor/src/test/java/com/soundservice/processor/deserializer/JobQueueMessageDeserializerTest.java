package com.soundservice.processor.deserializer;

import com.soundservice.processor.dto.JobQueueMessage;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobQueueMessageDeserializerTest {

    private final JobQueueMessageDeserializer deserializer = new JobQueueMessageDeserializer();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testDeserializeNullBytes() {
        assertNull(deserializer.deserialize("any", (byte[]) null));
        assertNull(deserializer.deserialize("any", new RecordHeaders(), (byte[]) null));
    }

    @Test
    void testDeserializeMessage() {
        final JobQueueMessage message = new JobQueueMessage("mp3", 1F, 0F);
        final byte[] bytes = mapper.writeValueAsBytes(message);

        assertEquals(message, deserializer.deserialize("any", bytes));
    }

    @Test
    void testDeserializeMessageWithHeaders() {
        final JobQueueMessage message = new JobQueueMessage("mp3", 1.5F, -4F);
        final byte[] bytes = mapper.writeValueAsBytes(message);

        assertEquals(message, deserializer.deserialize("any", new RecordHeaders(), bytes));
    }
}
