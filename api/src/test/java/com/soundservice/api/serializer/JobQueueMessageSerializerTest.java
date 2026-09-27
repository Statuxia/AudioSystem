package com.soundservice.api.serializer;

import com.soundservice.api.dto.JobQueueMessage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JobQueueMessageSerializerTest {

    private final JobQueueMessageSerializer serializer = new JobQueueMessageSerializer();

    @Test
    void testSerializeNull() {
        assertNull(serializer.serialize("any", null));
    }

    @Test
    void testSerializeValid() {
        final JobQueueMessage message = new JobQueueMessage(
            "format",
            1.0F,
            1.0F,
            false
        );

        final byte[] serialized = serializer.serialize("any", message);
        final JsonNode mappedObject = new ObjectMapper().readValue(serialized, JsonNode.class);

        assertTrue(mappedObject.has("format"));
        assertTrue(mappedObject.has("speed"));
        assertTrue(mappedObject.has("pitchSemitones"));
        assertTrue(mappedObject.has("preservePitch"));
        assertEquals("format", mappedObject.get("format").asString());
        assertEquals(1.0F, mappedObject.get("speed").asFloat(), 0.001);
        assertEquals(1.0F, mappedObject.get("pitchSemitones").asFloat(), 0.001);
        assertFalse(mappedObject.get("preservePitch").asBoolean());
    }

}