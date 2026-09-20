package com.sample.app.ai.screeningchat.error;

import java.util.UUID;

public class ChatSessionNotFoundException extends RuntimeException {

    public ChatSessionNotFoundException(UUID chatSessionId) {
        super("Chat session not found for ID: " + chatSessionId);
    }
}
