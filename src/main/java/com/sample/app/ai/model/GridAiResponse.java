package com.sample.app.ai.model;

import java.util.Map;

public record GridAiResponse(
        String action, // "filter" | "sort" | "highlight"
        Map<String, Object> payload
) {}
