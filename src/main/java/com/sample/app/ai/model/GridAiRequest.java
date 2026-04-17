package com.sample.app.ai.model;

import java.util.Map;

public record GridAiRequest(
        String userQuery,
        Map<String, Object> gridState,
        Map<String, Object> structuredSchema
) {}
