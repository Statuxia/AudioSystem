package com.audiosystem.api.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public ValidationException(Map<String, String> validationErrors) {
        super(HttpStatus.BAD_REQUEST, validationErrors);
    }
}
