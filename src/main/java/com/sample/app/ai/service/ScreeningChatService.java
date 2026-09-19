package com.sample.app.ai.service;

import com.sample.app.ai.error.ScreeningChatException;
import com.sample.app.ai.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class ScreeningChatService {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final ChatMemoryRepository chatMemoryRepository;
    private final ScreeningChatHistoryService chatHistoryService;

    public ScreeningChatService(
            ChatClient chatClient,
            ChatModel chatModel,
            ChatMemory screeningChatMemory,
            ChatMemoryRepository chatMemoryRepository,
            ScreeningChatHistoryService chatHistoryService) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.chatMemory = screeningChatMemory;
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatHistoryService = chatHistoryService;
    }

    public ScreeningChatResponse chat(ScreeningChatRequest request) {
        String pmId = chatHistoryService.normalizePortfolioManagerId(
                request.portfolioManagerId());

        String userMessage = request.message() == null
                ? ""
                : request.message().trim();

        if (userMessage.isEmpty()) {
            return new ScreeningChatResponse(
                    pmId,
                    "Please provide a screening question.",
                    chatHistoryService.getHistory(pmId).messages(),
                    null
            );
        }

        log.info(
                "Processing screening chat for portfolioManagerId={}",
                pmId);

        log.info(
                "User message preview: {}",
                truncateForLog(userMessage, 200));

        log.info(
                "Selected grid rows for {}: count={}",
                pmId,
                request.selectedRows().size());

        String gridContext = buildGridContext(request);

        log.debug(
                "Grid context for {}:\n{}",
                pmId,
                truncateForLog(gridContext, 4000));

        AssistantMessageEntity response;

        try {
            log.info("Calling '{}' for portfolioManagerId={} ...", aiBackend(), pmId);
            response = chatClient.prompt()
                    .advisors(
                            MessageChatMemoryAdvisor
                                    .builder(chatMemory)
                                    .build()
                    )
                    .advisors(spec ->
                            spec.param(
                                    ChatMemory.CONVERSATION_ID,
                                    pmId
                            )
                    )
                    .system("""
                            You are a Screening AI Chat Assistant for a
                            FinTech Portfolio Manager.
                            
                            Provide concise, practical screening guidance.
                            
                            When appropriate, translate the user's request
                            into grid changes such as:
                            - filters
                            - sorting
                            - column visibility
                            - column sizing
                            
                            The current grid context is provided below.
                            Treat it as the current state of the UI for this
                            request.
                            
                            CURRENT GRID CONTEXT:
                            %s
                            """.formatted(gridContext))
                    .user(userMessage)
                    .call()
                    .entity(AssistantMessageEntity.class);

        } catch (Exception ex) {
            throw new ScreeningChatException("Chat with " + aiBackend() + " failed.", ex);
        }

        String assistantMessage = response.explanation();
        GridUpdate gridUpdate = response.gridUpdate();

        ScreeningChatHistoryResponse history = chatHistoryService.getHistory(pmId);

        log.info(
                "Assistant response for {} ({} chars). History size={}",
                pmId,
                assistantMessage == null
                        ? 0
                        : assistantMessage.length(),
                history.messages().size());

        log.debug(
                "Assistant preview: {}",
                truncateForLog(assistantMessage, 200));

        log.debug(
                "Grid Update:\n{}",
                toPrettyJson(gridUpdate));

        return new ScreeningChatResponse(
                pmId,
                assistantMessage,
                history.messages(),
                gridUpdate
        );
    }

    private String buildGridContext(
            ScreeningChatRequest request) {

        return """
                CURRENT GRID STATE:
                %s
                
                GRID SCHEMA:
                %s
                
                SELECTED GRID ROWS:
                %s
                """.formatted(
                toJson(request.gridState()),
                toJson(request.structuredSchema()),
                toJson(request.selectedRows())
        );
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize grid context",
                    e);
        }
    }

    private String toPrettyJson(Object value) {
        if (value == null) {
            return "{}";
        }

        try {
            return jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String truncateForLog(String value, int max) {
        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max) + "... (truncated)";
    }

    private String aiBackend() {
        return switch (chatModel) {
            case GoogleGenAiChatModel ignored -> "Google GenAI";
            case OpenAiChatModel ignored -> "OpenAI";
            default -> chatModel.getClass().getSimpleName();
        };
    }
}