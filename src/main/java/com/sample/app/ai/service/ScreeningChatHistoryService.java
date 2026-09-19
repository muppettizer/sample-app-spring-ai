package com.sample.app.ai.service;

import com.sample.app.ai.model.AssistantMessageEntity;
import com.sample.app.ai.model.ScreeningChatHistoryMessage;
import com.sample.app.ai.model.ScreeningChatHistoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Slf4j
@Service
public class ScreeningChatHistoryService {

    private final ChatMemory chatMemory;
    private final ChatMemoryRepository chatMemoryRepository;
    private final JsonMapper jsonMapper;

    public ScreeningChatHistoryService(ChatMemory chatMemory, ChatMemoryRepository chatMemoryRepository, JsonMapper jsonMapper) {
        this.chatMemory = chatMemory;
        this.chatMemoryRepository = chatMemoryRepository;
        this.jsonMapper = jsonMapper;
    }

    public ScreeningChatHistoryResponse getHistory(String portfolioManagerId) {
        String pmId = normalizePortfolioManagerId(portfolioManagerId);
        log.info("Retrieving chat history for portfolioManagerId={}", pmId);

        // DEBUG conversation ids in the ChatMemoryRepository
        try {
            var convIds = chatMemoryRepository.findConversationIds();
            log.info("ChatMemoryRepository conversationIds count={}", convIds == null ? 0 : convIds.size());
            if (convIds != null) {
                convIds.stream().limit(50).forEach(id -> log.info("conversationId from repo={}", id));
            }
        } catch (Exception e) {
            log.warn("Unable to query ChatMemoryRepository for conversation ids", e);
        }

        // Debug: inspect what the ChatMemory stored for this conversation id
        try {
            log.info("ChatMemoryRepository class={}", chatMemoryRepository.getClass().getName());
            var stored = chatMemory.get(pmId);
            log.info("ChatMemory stored messages for {}: count={}", pmId, stored.size());
            stored.stream()
                    .limit(50)
                    .forEach(m -> log.info("Stored message: class={}, preview={}", m.getClass().getName(),
                            m.getText() == null ? "" : (m.getText().length() <= 200 ? m.getText() : m.getText().substring(0, 200) + "...")));
        } catch (Exception e) {
            log.warn("Failed to read chatMemory for {}", pmId, e);
        }

        List<ScreeningChatHistoryMessage> history = chatMemory.get(pmId).stream()
                .map(this::toChatMessage)
                .toList();

        return new ScreeningChatHistoryResponse(
                pmId,
                history);
    }

    public void clearHistory(String portfolioManagerId) {
        String pmId = normalizePortfolioManagerId(portfolioManagerId);
        log.info("Clearing chat history for portfolioManagerId={}", pmId);
        chatMemory.clear(pmId);
    }

    private ScreeningChatHistoryMessage toChatMessage(Message message) {
        if (message instanceof UserMessage user) {
            return new ScreeningChatHistoryMessage(
                    "user",
                    user.getText());
        }

        // Treat any non-user message as assistant. Some ChatMemory
        // implementations return message types other than
        // org.springframework.ai.chat.messages.AssistantMessage (e.g.,
        // Redis-backed or JSON-wrapped messages). Try parsing the
        // message text as AssistantMessageEntity, otherwise fall back
        // to returning the raw text.
        return toAssistantChatMessage(message);
    }

    private ScreeningChatHistoryMessage toAssistantChatMessage(
            Message message) {

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
