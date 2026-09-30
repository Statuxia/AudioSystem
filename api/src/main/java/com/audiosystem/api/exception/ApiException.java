package com.audiosystem.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> validationErrors;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus status, Map<String, String> validationErrors) {
        this(status, null, validationErrors);
    }

    public ApiException(HttpStatus status, String message, Map<String, String> validationErrors) {
        super(message);
        this.status = status;
        this.validationErrors = validationErrors;
    }
}
