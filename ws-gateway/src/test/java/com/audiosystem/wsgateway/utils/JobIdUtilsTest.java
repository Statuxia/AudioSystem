package com.audiosystem.wsgateway.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobIdUtilsTest {

    private static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";

    @Test
    void testParseJobUuidBadFormat() {
        final UUID uuid = JobIdUtils.parseJobUuid("bad-format");
        assertNull(uuid);
    }

    @Test
    void testParseJobUuidValidFormat() {
        final UUID uuid = JobIdUtils.parseJobUuid(DEFAULT_UUID);
        assertEquals(DEFAULT_UUID, uuid.toString());
    }
}