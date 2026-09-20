package com.sample.app.ai.screeningchat.service;

import com.sample.app.ai.screeningchat.error.ScreeningChatException;
import com.sample.app.ai.screeningchat.model.AssistantMessageEntity;
import com.sample.app.ai.screeningchat.model.chat.ChatRequest;
import com.sample.app.ai.screeningchat.model.chat.ScreeningChatConversationId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatMemoryRepository chatMemoryRepository;

    public AssistantMessageEntity chat(
            String portfolioManagerId,
            UUID chatSessionId,
            ChatRequest request) {

        var conversationId = ScreeningChatConversationId.of(portfolioManagerId, chatSessionId);

        try {
            log.info(
                    "Calling '{}' for conversationId={}",
                    aiBackend(),
                    conversationId
            );

            return chatClient.prompt()
                    .advisors(
                            MessageChatMemoryAdvisor
                                    .builder(chatMemory)
                                    .build()
                    )
                    .advisors(spec ->
                            spec.param(
                                    ChatMemory.CONVERSATION_ID,
                                    conversationId
                            )
                    )
                    .system(buildSystemPrompt(request.gridContext()))
                    .user(request.message())
                    .call()
                    .entity(AssistantMessageEntity.class);

        } catch (Exception ex) {
            throw new ScreeningChatException(
                    "Chat with " + aiBackend() + " failed.",
                    ex
            );
        }
    }

    public List<Message> getChatMessages(String portfolioManagerId, UUID chatSessionId) {
        var conversationId = ScreeningChatConversationId.of(portfolioManagerId, chatSessionId);

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
            var stored = chatMemory.get(conversationId);
            log.info("ChatMemory stored messages for {}: count={}", conversationId, stored.size());
            stored.stream()
                    .limit(50)
                    .forEach(m -> log.info("Stored message: class={}, preview={}", m.getClass().getName(),
                            m.getText() == null ? "" : (m.getText().length() <= 200 ? m.getText() : m.getText().substring(0, 200) + "...")));
        } catch (Exception e) {
            log.warn("Failed to read chatMemory for {}", conversationId, e);
        }

        return chatMemory.get(conversationId);
    }

    public void delete(
            String portfolioManagerId,
            UUID chatSessionId) {

        var conversationId =
                ScreeningChatConversationId.of(portfolioManagerId, chatSessionId);

        log.info(
                "Deleting screening chat: conversationId={}",
                conversationId
        );

        chatMemoryRepository.deleteByConversationId(
                conversationId
        );
    }

    private String buildSystemPrompt(ChatRequest.GridContext gridContext) {
        return """
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

                CURRENT GRID STATE:
                %s
    
                CURRENT GRID SCHEMA:
                %s
                """.formatted(
                    gridContext.gridState(),
                    gridContext.gridSchema()
        );
    }

    private String aiBackend() {
        return "AI";
    }
}