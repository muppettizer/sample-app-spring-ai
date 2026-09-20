package com.sample.app.ai.screeningchat.model;

import java.util.Map;

public record ScreeningNotesRequest(
        String portfolioManagerId,
        Map<String, Map<String, String>> notes
) {
    public ScreeningNotesRequest {
        notes = notes == null ? Map.of() : notes;
    }
}
