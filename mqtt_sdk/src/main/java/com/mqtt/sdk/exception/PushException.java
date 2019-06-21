package com.mqtt.sdk.exception;

public class PushException extends Exception {
    public PushException() {
        super();
    }
    public PushException(String message) {
        super(message);
    }
    public PushException(String message, Throwable cause) {
        super(message, cause);
    }
    public PushException(Throwable cause) {
        super(cause);
    }
}
