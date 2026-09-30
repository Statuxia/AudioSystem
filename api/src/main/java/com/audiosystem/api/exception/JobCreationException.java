package com.audiosystem.api.exception;

import org.springframework.http.HttpStatus;

public class JobCreationException extends ApiException {

    public JobCreationException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
