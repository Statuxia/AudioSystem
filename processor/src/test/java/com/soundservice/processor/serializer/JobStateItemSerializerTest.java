package com.soundservice.processor.serializer;

import com.soundservice.processor.dto.JobStateItem;
import com.soundservice.processor.dto.JobStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobStateItemSerializerTest {

    private final JobStateItemSerializer serializer = new JobStateItemSerializer();

    @Test
    void testSerializeNull() {
        assertArrayEquals(new byte[0], serializer.serialize(null));
    }

    @Test
    void testDeserializeNull() {
        assertNull(serializer.deserialize(null));
    }

    @Test
    void testSerializeDeserializeRoundTrip() {
        final JobStateItem item = new JobStateItem(UUID.randomUUID(), JobStatus.IN_QUEUE, 123456789L);

        final byte[] serialized = serializer.serialize(item);
        final JobStateItem deserialized = serializer.deserialize(serialized);

        assertEquals(item, deserialized);
    }
}
