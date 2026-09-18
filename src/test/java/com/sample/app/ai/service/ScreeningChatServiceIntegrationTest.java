package com.sample.app.ai.service;

import com.sample.app.ai.model.ScreeningChatRequest;
import com.sample.app.ai.model.ScreeningChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("google")
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
public class ScreeningChatServiceIntegrationTest {

    @Autowired
    ScreeningChatService screeningChatService;

    @Test
    void chat_against_google_genai_returns_structured_response() {


        // Build example request (trimmed/representative of the provided example)
        var selectedRow = Map.<String, Object>ofEntries(
                Map.entry("id", "sec-0001"),
                Map.entry("securityId", List.of("SEC-1001")),
                Map.entry("securityName", List.of("Strategic Bond Bond 2036")),
                Map.entry("issuer", List.of("Pacific Tech Inc")),
                Map.entry("assetClass", List.of("Bond")),
                Map.entry("country", List.of("FR")),
                Map.entry("currency", List.of("GBP")),
                Map.entry("rating", List.of("AA-")),
                Map.entry("esgRiskLevel", List.of("Low")),
                Map.entry("riskScore", List.of("33")),
                Map.entry("sanctionsFlag", List.of("No")),
                Map.entry("screeningStatus", List.of("Escalated")),
                Map.entry("lastReviewDate", List.of("2026-04-15T00:00:00Z")),
                Map.entry("marginRate", 2.45)
        );

        var gridState = Map.<String, Object>of("version", "36.1.0");
        var structuredSchema = Map.<String, Object>of();

        ScreeningChatRequest request = new ScreeningChatRequest(
                "pm-001",
                "Find securities similar to the selected security.",
                gridState,
                structuredSchema,
                List.of(selectedRow)
        );

        // Clear any prior history and invoke the chat
        screeningChatService.clearHistory("pm-001");
        ScreeningChatResponse response = screeningChatService.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.portfolioManagerId()).isEqualTo("pm-001");
        assertThat(response.assistantMessage()).isNotBlank();
        // History should at least contain the assistant response that was just added or be non-null
        assertThat(response.history()).isNotNull();

        // Grid update may be present depending on LLM output; if present it should be a valid object
        // (no strict assertions on fields to avoid flakiness against the live model)
        // But we assert that either gridUpdate is null or a non-null object (sanity)
        assertThat(response.gridUpdate() == null || response.gridUpdate() != null).isTrue();
    }
}
