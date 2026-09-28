package com.soundservice.api.exception;

import org.springframework.http.HttpStatus;

public class JobStateException extends ApiException {

    public JobStateException(HttpStatus status, String message) {
        super(status, message);
    }
}
