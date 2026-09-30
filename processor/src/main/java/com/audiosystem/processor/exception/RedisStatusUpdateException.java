package com.audiosystem.processor.exception;

public class RedisStatusUpdateException extends RuntimeException {
    public RedisStatusUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
