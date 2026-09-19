package com.sample.app.ai.service;

import com.sample.app.ai.model.AssistantMessageEntity;
import com.sample.app.ai.model.ScreeningChatHistoryMessage;
import com.sample.app.ai.model.ScreeningChatHistoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Service
public class ScreeningChatHistoryService {

    private final ChatMemory chatMemory;
    private final JsonMapper jsonMapper;

    public ScreeningChatHistoryService(ChatMemory chatMemory, JsonMapper jsonMapper) {
        this.chatMemory = chatMemory;
        this.jsonMapper = jsonMapper;
    }

    public ScreeningChatHistoryResponse getHistory(
            String portfolioManagerId) {

        String pmId = normalizePortfolioManagerId(portfolioManagerId);

        List<ScreeningChatHistoryMessage> history = chatMemory.get(pmId).stream()
                .filter(message ->
                        message instanceof UserMessage ||
                        message instanceof AssistantMessage)
                .map(this::toChatMessage)
                .toList();

        return new ScreeningChatHistoryResponse(
                pmId,
                history);
    }

    public void clearHistory(String portfolioManagerId) {
        chatMemory.clear(
                normalizePortfolioManagerId(portfolioManagerId));
    }

    private ScreeningChatHistoryMessage toChatMessage(Message message) {
        return switch (message) {
            case UserMessage user ->
                    new ScreeningChatHistoryMessage(
                            "user",
                            user.getText());

            case AssistantMessage assistant ->
                    toAssistantChatMessage(assistant);

            default -> throw new IllegalArgumentException(
                    "Unsupported message type: " +
                            message.getClass().getName());
        };
    }

    private ScreeningChatHistoryMessage toAssistantChatMessage(
            AssistantMessage message) {

        try {
            AssistantMessageEntity entity =
                    jsonMapper.readValue(
                            message.getText(),
                            AssistantMessageEntity.class);

            return new ScreeningChatHistoryMessage(
                    "assistant",
                    entity.explanation());
        } catch (Exception e) {
            log.warn("Unable to parse assistant message", e);

            return new ScreeningChatHistoryMessage(
                    "assistant",
                    message.getText());
        }
    }

    public String normalizePortfolioManagerId(
            String portfolioManagerId) {

        return portfolioManagerId == null ||
                portfolioManagerId.isBlank()
                ? "default"
                : portfolioManagerId.trim();
    }

    private String createPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        if (content.length() <= 150) {
            return content;
        }

        return content.substring(0, 150).trim() + "...";
    }
}
