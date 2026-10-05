package com.audiosystem.wsgateway.utils;

import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Log4j2
public class JobIdUtils {

    private JobIdUtils() {
        // stub
    }

    public static @Nullable UUID parseJobUuid(String jobId) {
        final UUID uuid;
        try {
            uuid = UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            log.error("wrong jobId format", e);
            return null;
        }
        return uuid;
    }
}
