package com.sample.app.ai.model;

/**
 * Represents a structured assistant response that may include both the human-readable
 * assistant message and a machine-readable GridAiResponse for client-side updates.
 */
public record AssistantMessageEntity(
        String assistantMessage,
        GridAiResponse gridUpdate
) {}
