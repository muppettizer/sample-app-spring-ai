package com.sample.app.ai.service;

import com.sample.app.ai.model.GridAiRequest;
import com.sample.app.ai.model.GridAiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GridAiService {

    private final ChatClient chatClient;

    public GridAiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public GridAiResponse process(GridAiRequest request) {

        String prompt = """
                You are a FinTech Security Screening AG Grid AI assistant.
                Convert the user query into AG Grid state changes.

                STRICT RULES:
                - Output ONLY valid JSON (no text, no explanation)
                - MUST match the schema exactly
                - Include ALL required top-level fields:
                  - filter
                  - sort
                  - columnVisibility
                  - columnSizing
                - If a section is not modified, return it as null
                - Do NOT invent fields
                - Do NOT omit required fields
                - "filter" must be a valid AG Grid filter model object or null
                - "sort" must be an array like [{"colId":"riskScore","sort":"desc"}] or null
                - "columnVisibility" must be an object like {"sanctionsFlag":true} or null
                - "columnSizing" must be an object like {"issuer":220} or null
                - For categorical columns assetClass, country, and currency (CCY), use AG Grid Set Filter format:
                  {"filterType":"set","values":["..."]}
                - If user says "CCY", map to the "currency" column
                - For set filters, values must be explicit lists (never contains/equals type)
                
                USER QUERY:
                %s

                CURRENT GRID STATE:
                %s

                JSON SCHEMA:
                %s

                EXAMPLE JSON RESPONSE:
                {
                  "filter": {
                    "riskScore": {
                      "filterType": "number",
                      "type": "greaterThan",
                      "filter": 70
                    }
                  },
                  "sort": [{"colId": "riskScore", "sort": "desc"}],
                  "columnVisibility": {"securityId": true, "notes": false},
                  "columnSizing": {"issuer": 220, "securityName": 260}
                }

                Return ONLY JSON:
                """.formatted(
                request.userQuery(),
                request.gridState(),
                request.structuredSchema()
        );

        log.info("Sending prompt to AI: {}", prompt);

        GridAiResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(GridAiResponse.class); // 🔥 auto JSON mapping

        GridAiResponse normalized = normalizeSetFilters(response);
        log.info("Received response from AI: {}", normalized);

        return normalized;
    }

    private GridAiResponse normalizeSetFilters(GridAiResponse response) {
        if (response == null || response.filter() == null) {
            return response;
        }

        Map<String, Object> normalizedFilter = new LinkedHashMap<>(response.filter());
        normalizeSetFilterForColumn(normalizedFilter, "assetClass");
        normalizeSetFilterForColumn(normalizedFilter, "country");
        normalizeSetFilterForColumn(normalizedFilter, "currency");

        return new GridAiResponse(
                normalizedFilter,
                response.sort(),
                response.columnVisibility(),
                response.columnSizing()
        );
    }

    @SuppressWarnings("unchecked")
    private void normalizeSetFilterForColumn(Map<String, Object> filterModel, String column) {
        Object columnFilter = filterModel.get(column);
        if (!(columnFilter instanceof Map<?, ?> columnFilterMapRaw)) {
            return;
        }

        Map<String, Object> columnFilterMap = (Map<String, Object>) columnFilterMapRaw;
        Object filterType = columnFilterMap.get("filterType");
        if ("set".equals(filterType)) {
            return;
        }

        if ("text".equals(filterType) && columnFilterMap.get("filter") != null) {
            Object singleValue = columnFilterMap.get("filter");
            Map<String, Object> setFilter = new LinkedHashMap<>();
            setFilter.put("filterType", "set");
            List<Object> values = new ArrayList<>();
            values.add(singleValue);
            setFilter.put("values", values);
            filterModel.put(column, setFilter);
            return;
        }

        Object values = columnFilterMap.get("values");
        if (values instanceof List<?>) {
            Map<String, Object> setFilter = new LinkedHashMap<>();
            setFilter.put("filterType", "set");
            setFilter.put("values", values);
            filterModel.put(column, setFilter);
        }
    }
}
