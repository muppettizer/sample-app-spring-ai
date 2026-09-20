package com.sample.app.ai.model;

import java.util.Map;

public record ScreeningNotesResponse(
        String portfolioManagerId,
        Map<String, Map<String, String>> notes
) {
    public ScreeningNotesResponse {
        notes = notes == null ? Map.of() : notes;
    }
}
