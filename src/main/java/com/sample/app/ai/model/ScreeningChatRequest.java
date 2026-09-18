package com.sample.app.ai.model;

import java.util.Map;
import java.util.List;

public record ScreeningChatRequest(
        String portfolioManagerId,
        String message,
        Map<String, Object> gridState,
        Map<String, Object> structuredSchema,
        List<Map<String, Object>> selectedRows
) {
    public ScreeningChatRequest {
        gridState = gridState == null ? Map.of() : gridState;
        structuredSchema = structuredSchema == null ? Map.of() : structuredSchema;
        selectedRows = selectedRows == null ? List.of() : selectedRows;
    }
}
