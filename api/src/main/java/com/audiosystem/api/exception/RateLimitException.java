package com.audiosystem.api.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends ApiException {
    public RateLimitException(HttpStatus status, String message) {
        super(status, message);
    }
}
