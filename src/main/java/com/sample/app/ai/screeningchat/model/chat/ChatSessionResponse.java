package com.sample.app.ai.screeningchat.model.chat;

import java.util.UUID;

public record ChatSessionResponse(
        UUID chatSessionId,
        String pmId
) {
}
