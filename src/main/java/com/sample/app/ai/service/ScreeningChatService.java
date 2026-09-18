package com.sample.app.ai.service;

import com.sample.app.ai.model.ChatMessageDto;
import com.sample.app.ai.model.GridAiResponse;
import com.sample.app.ai.model.ScreeningChatHistoryResponse;
import com.sample.app.ai.model.ScreeningChatRequest;
import com.sample.app.ai.model.ScreeningChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScreeningChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ObjectMapper objectMapper;

    public ScreeningChatService(ChatClient chatClient, ChatMemory screeningChatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = screeningChatMemory;
        this.objectMapper = new ObjectMapper();
    }

    public ScreeningChatResponse chat(ScreeningChatRequest request) {
        String pmId = normalizePortfolioManagerId(request.portfolioManagerId());
        String userMessage = request.message() == null ? "" : request.message().trim();
        if (userMessage.isEmpty()) {
            return new ScreeningChatResponse(
                    pmId,
                    "Please provide a screening question.",
                    getHistory(pmId).history(),
                    null
            );
        }

        // Log a concise, non-verbose summary and the generated prompt for debugging
        log.info("Processing screening chat for portfolioManagerId={}", pmId);
        log.info("User message preview: {}", truncateForLog(userMessage, 200));
        List<Map<String, Object>> selectedRows = request.selectedRows() == null ? List.of() : request.selectedRows();
        log.info("Selected grid rows for {} ({}): {}", pmId, selectedRows.size(), selectedRows);

        String context = buildContextForPrompt(
                request.gridState(), request.structuredSchema(), selectedRows, userMessage);

        // Print the full prompt separated by delimiters to make it clear in logs (truncate to avoid huge output)
        log.info("Prompt for {}:\n---\n{}\n---", pmId, truncateForLog(context, 4000));

        var chatCallResult = chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, pmId))
                .user(context)
                .call();

        // Try to map the full assistant response into a structured entity first
        String assistantMessage = null;
        GridAiResponse gridUpdate = null;
        try {
            var assistantEntity = chatCallResult.entity(com.sample.app.ai.model.AssistantMessageEntity.class);
            if (assistantEntity != null) {
                assistantMessage = assistantEntity.assistantMessage();
                gridUpdate = assistantEntity.gridUpdate();
                log.info("AssistantMessageEntity mapped: gridUpdate {}", gridUpdate != null ? "present" : "absent");
            }
        } catch (Exception e) {
            log.debug("AssistantMessageEntity mapping failed: {}", e.getMessage());
        }

        // Fallback: if we didn't get a structured assistant message, use raw content and try mapping GridAiResponse alone
        if (assistantMessage == null) {
            assistantMessage = chatCallResult.content();
            try {
                gridUpdate = chatCallResult.entity(GridAiResponse.class);
                if (gridUpdate != null) {
                    log.info("GridAiResponse parsed via entity mapping");
                }
            } catch (Exception e) {
                log.debug("Automatic entity mapping to GridAiResponse failed: {}", e.getMessage());
                gridUpdate = parseGridAiResponse(assistantMessage);
            }
        }

        log.info("GridAiResponse parsed: {}", gridUpdate != null ? "SUCCESS" : "NOT FOUND");

        // Log result summary (length and a short preview) and history size
        ScreeningChatHistoryResponse history = getHistory(pmId);
        log.info("Assistant response for {} ({} chars). History size={}", pmId,
                assistantMessage == null ? 0 : assistantMessage.length(), history.history().size());
        log.info("Assistant preview: {}", truncateForLog(assistantMessage, 200));

        log.info("Grid Update:\n\n{}\n", gridUpdate);

        return new ScreeningChatResponse(pmId, assistantMessage, history.history(), gridUpdate);
    }

    private String truncateForLog(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "... (truncated)";
    }

    public ScreeningChatHistoryResponse getHistory(String portfolioManagerId) {
        String pmId = normalizePortfolioManagerId(portfolioManagerId);
        List<Message> messages = chatMemory.get(pmId);
        List<ChatMessageDto> history = messages.stream()
                .map(this::toDto)
                .toList();
        return new ScreeningChatHistoryResponse(pmId, history);
    }

    public void clearHistory(String portfolioManagerId) {
        chatMemory.clear(normalizePortfolioManagerId(portfolioManagerId));
    }

    private String buildContextForPrompt(
            Map<String, Object> gridState,
            Map<String, Object> structuredSchema,
            List<Map<String, Object>> selectedRows,
            String userMessage
    ) {
        return """
                You are a Screening AI Chat Assistant for a FinTech Portfolio Manager.
                Provide concise, practical screening guidance and explain reasoning clearly.
                When relevant, reference available grid fields and suggest concrete filter/sort ideas.

                RETURN FORMAT (REQUIRED):
                Always return a single JSON object only (no additional explanatory text before or after)
                matching this exact shape so the server can automatically deserialize it into an
                AssistantMessageEntity:

                {
                  "assistantMessage": "<brief explanation for the user>",
                  "gridUpdate": {
                    "filter": {},
                    "sort": [],
                    "columnVisibility": {},
                    "columnSizing": {}
                  }
                }

                NOTES:
                - Use empty objects/arrays when a section has no changes (do not omit keys).
                - The values for "sort" should be an array of objects like {"colId": "<id>", "sort": "asc|desc"}.
                - For filter, use simple descriptors such as {"riskScore": {"type": "greaterThan", "value": 70}}.

                EXAMPLE (preferred JSON-only response):
                {
                  "assistantMessage": "Showing high-risk securities (riskScore > 70), sorted by risk descending.",
                  "gridUpdate": {
                    "filter": {"riskScore": {"type": "greaterThan", "value": 70}},
                    "sort": [{"colId": "riskScore", "sort": "desc"}],
                    "columnVisibility": {},
                    "columnSizing": {}
                  }
                }

                CURRENT GRID STATE:
                %s

                GRID SCHEMA:
                %s

                SELECTED GRID ROWS:
                %s

                Use the selected grid rows as additional context. If none are selected, this will be an empty list.

                USER MESSAGE:
                %s

                Respond only with the JSON object described above.
                """.formatted(gridState, structuredSchema, selectedRows, userMessage);
    }

    private ChatMessageDto toDto(Message message) {
        String role;
        if (message instanceof UserMessage) {
            role = "user";
        } else if (message instanceof AssistantMessage) {
            role = "assistant";
        } else if (message instanceof SystemMessage) {
            role = "system";
        } else {
            role = "assistant";
        }
        String content = message.getText();
        String summary = generateSummary(content);
        return new ChatMessageDto(role, content, summary);
    }

    private String generateSummary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        // For verbose content, create a concise summary
        if (content.length() > 150) {
            return content.substring(0, 150).trim() + "...";
        }
        return content;
    }

    private GridAiResponse parseGridAiResponse(String assistantMessage) {
        try {
            if (assistantMessage == null || assistantMessage.isBlank()) {
                log.debug("Assistant message is empty, no GridAiResponse to parse");
                return null;
            }

            // Look for delimiter first
            String delimiter = "===GRID_UPDATE===";
            int delimiterIndex = assistantMessage.indexOf(delimiter);

            String jsonStr = null;

            if (delimiterIndex >= 0) {
                // JSON is after the delimiter
                String afterDelimiter = assistantMessage.substring(delimiterIndex + delimiter.length()).trim();
                int jsonStart = afterDelimiter.indexOf("{");
                int jsonEnd = afterDelimiter.lastIndexOf("}");

                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    jsonStr = afterDelimiter.substring(jsonStart, jsonEnd + 1);
                    log.debug("Found JSON after delimiter");
                }
            } else {
                // Fallback: look for JSON in the entire message
                int jsonStart = assistantMessage.indexOf("{");
                if (jsonStart < 0) {
                    log.debug("No JSON object found in assistant message");
                    return null;
                }

                int jsonEnd = assistantMessage.lastIndexOf("}");
                if (jsonEnd <= jsonStart) {
                    log.debug("Invalid JSON structure in assistant message");
                    return null;
                }

                jsonStr = assistantMessage.substring(jsonStart, jsonEnd + 1);
                log.debug("Found JSON without delimiter");
            }

            if (jsonStr == null || jsonStr.isBlank()) {
                log.debug("No valid JSON string extracted from message");
                return null;
            }

            log.debug("Attempting to parse JSON: {}", jsonStr.substring(0, Math.min(200, jsonStr.length())));

            try {
                GridAiResponse response = objectMapper.readValue(jsonStr, GridAiResponse.class);
                log.info("Successfully parsed GridAiResponse from assistant message");
                log.info("GridAiResponse - filter: {}, sort: {}, columnVisibility: {}, columnSizing: {}",
                    response.filter() != null && !response.filter().isEmpty(),
                    response.sort() != null && !response.sort().isEmpty(),
                    response.columnVisibility() != null && !response.columnVisibility().isEmpty(),
                    response.columnSizing() != null && !response.columnSizing().isEmpty());
                return response;
            } catch (Exception e) {
                log.warn("Could not parse GridAiResponse from extracted JSON: {}", e.getMessage());
                log.debug("JSON attempted: {}", jsonStr);
                return null;
            }
        } catch (Exception e) {
            log.warn("Error parsing GridAiResponse: {}", e.getMessage());
            return null;
        }
    }

    private String normalizePortfolioManagerId(String portfolioManagerId) {
        if (portfolioManagerId == null || portfolioManagerId.isBlank()) {
            return "pm-default";
        }
        return portfolioManagerId.trim();
    }
}
