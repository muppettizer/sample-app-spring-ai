package com.sample.app.ai.model;

/**
 * Represents a structured assistant response that may include both the human-readable
 * explanation and a machine-readable GridUpdate for AgGrid update.
 */
public record AssistantMessageEntity(
        String explanation,
        GridUpdate gridUpdate
) {}
