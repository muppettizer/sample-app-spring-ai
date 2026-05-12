package com.sample.app.ai.model;

import java.util.Map;

public record ScreeningChatRequest(
        String portfolioManagerId,
        String message,
        Map<String, Object> gridState,
        Map<String, Object> structuredSchema
) {}
