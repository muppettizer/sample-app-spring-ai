package com.sample.app.ai.screeningchat.model.chat;

import jakarta.validation.constraints.NotBlank;

public record CreateChatSessionRequest(
        @NotBlank String portfolioManagerId
) {
}