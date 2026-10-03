package com.audiosystem.api.dto;

import java.util.Objects;

public record RateLimitKey(String source, String identifier) {

    public String toStringKey() {
        Objects.requireNonNull(source);
        Objects.requireNonNull(identifier);
        return "%s:%s".formatted(source, identifier);
    }
}
