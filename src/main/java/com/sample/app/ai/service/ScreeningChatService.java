package com.sample.app.ai.service;

import com.sample.app.ai.model.ChatMessageDto;
import com.sample.app.ai.model.ScreeningChatHistoryResponse;
import com.sample.app.ai.model.ScreeningChatRequest;
import com.sample.app.ai.model.ScreeningChatResponse;
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

    public ScreeningChatService(ChatClient chatClient, ChatMemory screeningChatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = screeningChatMemory;
    }

    public ScreeningChatResponse chat(ScreeningChatRequest request) {
        String pmId = normalizePortfolioManagerId(request.portfolioManagerId());
        String userMessage = request.message() == null ? "" : request.message().trim();
        if (userMessage.isEmpty()) {
            return new ScreeningChatResponse(
                    pmId,
                    "Please provide a screening question.",
                    getHistory(pmId).history()
            );
        }

        String context = buildContextForPrompt(request.gridState(), request.structuredSchema(), userMessage);
        String assistantMessage = chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, pmId))
                .user(context)
                .call()
                .content();

        return new ScreeningChatResponse(pmId, assistantMessage, getHistory(pmId).history());
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
            String userMessage
    ) {
        return """
                You are a Screening AI Chat Assistant for a FinTech Portfolio Manager.
                Provide concise, practical screening guidance and explain reasoning clearly.
                When relevant, reference available grid fields and suggest concrete filter/sort ideas.

                CURRENT GRID STATE:
                %s

                GRID SCHEMA:
                %s

                USER MESSAGE:
                %s

                Respond as assistant:
                """.formatted(gridState, structuredSchema, userMessage);
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
        return new ChatMessageDto(role, message.getText());
    }

    private String normalizePortfolioManagerId(String portfolioManagerId) {
        if (portfolioManagerId == null || portfolioManagerId.isBlank()) {
            return "pm-default";
        }
        return portfolioManagerId.trim();
    }
}
