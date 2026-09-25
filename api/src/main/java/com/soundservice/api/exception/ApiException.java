package com.soundservice.api.exception;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
