package com.soundservice.processor.exception;

public class KafkaSendMessageException extends RuntimeException {
    public KafkaSendMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
