package com.audiosystem.wsgateway.deserializer;

import com.audiosystem.wsgateway.dto.JobResultMessage;
import com.audiosystem.wsgateway.dto.JobStatus;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultMessageDeserializerTest {

    private final ResultMessageDeserializer deserializer = new ResultMessageDeserializer();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testDeserializeWithUnknownProperty() {
        assertThrows(
            UnrecognizedPropertyException.class,
            () -> deserializer.deserialize("any", "{\"test\": 1}".getBytes())
        );
    }

    @Test
    void testDeserializeMessage() {
        final JobResultMessage message = new JobResultMessage(JobStatus.DONE, 0L);
        final byte[] bytes = mapper.writeValueAsBytes(message);

        assertEquals(message, deserializer.deserialize("any", bytes));
    }

    @Test
    void testDeserializeByteBufferWithUnknownProperty() {
        final ByteBuffer buffer = ByteBuffer.wrap("{\"test\": 1}".getBytes(StandardCharsets.UTF_8));

        assertThrows(
            UnrecognizedPropertyException.class,
            () -> deserializer.deserialize("any", new RecordHeaders(), buffer)
        );
    }

    @Test
    void testDeserializeByteBufferMessage() {
        final JobResultMessage message = new JobResultMessage(JobStatus.DONE, 0L);
        final ByteBuffer buffer = ByteBuffer.wrap(mapper.writeValueAsBytes(message));

        assertEquals(message, deserializer.deserialize("any", new RecordHeaders(), buffer));
    }

    /**
     * Kafka передаёт не весь массив, а срез буфера пакета записей: данные начинаются не с нулевого
     * индекса массива и не заканчиваются в его конце. Десериализатор должен читать только
     * {@code position..limit}, а не весь {@code array()}.
     */
    @Test
    void testDeserializeByteBufferSlice() {
        final JobResultMessage message = new JobResultMessage(JobStatus.DONE, 0L);
        final byte[] json = mapper.writeValueAsBytes(message);
        final byte[] prefix = "garbage-before".getBytes(StandardCharsets.UTF_8);
        final byte[] suffix = "garbage-after".getBytes(StandardCharsets.UTF_8);

        final byte[] batch = new byte[prefix.length + json.length + suffix.length];
        System.arraycopy(prefix, 0, batch, 0, prefix.length);
        System.arraycopy(json, 0, batch, prefix.length, json.length);
        System.arraycopy(suffix, 0, batch, prefix.length + json.length, suffix.length);
        final ByteBuffer buffer = ByteBuffer.wrap(batch, prefix.length, json.length).slice();

        assertEquals(message, deserializer.deserialize("any", new RecordHeaders(), buffer));
    }
}
