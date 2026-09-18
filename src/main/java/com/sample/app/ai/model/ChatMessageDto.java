package com.sample.app.ai.model;

public record ChatMessageDto(
        String role,
        String content,
        String summary
) {}
