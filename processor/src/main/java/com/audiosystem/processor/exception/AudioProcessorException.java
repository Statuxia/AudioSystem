package com.audiosystem.processor.exception;

public class AudioProcessorException extends RuntimeException {
    public AudioProcessorException(String message) {
        super(message);
    }

    public AudioProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
