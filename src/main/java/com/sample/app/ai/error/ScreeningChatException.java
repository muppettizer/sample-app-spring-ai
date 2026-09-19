package com.sample.app.ai.error;

public class ScreeningChatException extends RuntimeException {

    public ScreeningChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
