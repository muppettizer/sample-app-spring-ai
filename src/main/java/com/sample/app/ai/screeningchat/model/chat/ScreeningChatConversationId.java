package com.sample.app.ai.screeningchat.model.chat;

import java.util.UUID;

public final class ScreeningChatConversationId {

    private ScreeningChatConversationId() {
    }

    public static String of(String pmId, UUID chatSessionId) {
        return pmId + ":" + chatSessionId;
    }
}
