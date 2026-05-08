package com.sample.app.ai.model;

import java.util.Map;
import java.util.List;

public record GridAiResponse(
        Map<String, Object> filter,
        List<Map<String, String>> sort,
        Map<String, Boolean> columnVisibility,
        Map<String, Integer> columnSizing
) {}
