package com.sample.app.ai.screeningchat.model.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record ChatRequest(
        @NotBlank String message,
        @NotNull GridContext gridContext
) {

    public record GridContext(
            @NotNull JsonNode gridState,
            @NotNull JsonNode gridSchema
    ) {
    }
}