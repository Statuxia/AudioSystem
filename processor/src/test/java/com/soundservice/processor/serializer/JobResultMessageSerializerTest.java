package com.soundservice.processor.serializer;

import com.soundservice.processor.dto.JobResultMessage;
import com.soundservice.processor.dto.JobStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JobResultMessageSerializerTest {

    private final JobResultMessageSerializer serializer = new JobResultMessageSerializer();

    @Test
    void testSerializeNull() {
        assertNull(serializer.serialize("any", null));
    }

    @Test
    void testSerializeValid() {
        final JobResultMessage message = new JobResultMessage(JobStatus.DONE, 123456789L);

        final byte[] serialized = serializer.serialize("any", message);
        final JsonNode mappedObject = new ObjectMapper().readValue(serialized, JsonNode.class);

        assertTrue(mappedObject.has("status"));
        assertTrue(mappedObject.has("expireAt"));
        assertEquals("DONE", mappedObject.get("status").asString());
        assertEquals(123456789L, mappedObject.get("expireAt").asLong());
    }
}
