package com.sample.app.ai.screeningchat.rest;

import com.sample.app.ai.screeningchat.model.AssistantMessageEntity;
import com.sample.app.ai.screeningchat.model.chat.ChatRequest;
import com.sample.app.ai.screeningchat.model.chat.ChatSessionResponse;
import com.sample.app.ai.screeningchat.model.chat.CreateChatSessionRequest;
import com.sample.app.ai.screeningchat.service.ScreeningChatService;
import com.sample.app.ai.screeningchat.service.ScreeningChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/screening-chat")
@RequiredArgsConstructor
public class ScreeningChatController {

    private final ScreeningChatSessionService sessionService;
    private final ScreeningChatService chatService;

    @GetMapping("/sessions")
    public List<ChatSessionResponse> getSessions(@RequestParam String portfolioManagerId) {
        return sessionService.getSessions(portfolioManagerId);
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionResponse createSession(
            @RequestBody CreateChatSessionRequest request) {
        return sessionService.create(request.portfolioManagerId());
    }

    @PostMapping("/sessions/{chatSessionId}/chat")
    public AssistantMessageEntity chat(
            @PathVariable UUID chatSessionId,
            @RequestParam String portfolioManagerId,
            @RequestBody ChatRequest request) {

        return chatService.chat(
                portfolioManagerId,
                chatSessionId,
                request
        );
    }

    @GetMapping("/sessions/{chatSessionId}/messages")
    public List<Message> getChatMessages(
            @PathVariable UUID chatSessionId,
            @RequestParam String portfolioManagerId) {

        return chatService.getChatMessages(
                portfolioManagerId,
                chatSessionId
        );
    }

    @DeleteMapping("/sessions/{chatSessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(
            @PathVariable UUID chatSessionId,
            @RequestParam(name = "portfolioManagerId", required = false) String portfolioManagerId,
            @RequestParam(name = "pmId", required = false) String pmId) {

        String effectivePortfolioManagerId = portfolioManagerId != null ? portfolioManagerId : pmId;
        chatService.delete(
                effectivePortfolioManagerId,
                chatSessionId
        );
    }

    @DeleteMapping("/sessions/{chatSessionId}/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory(
            @PathVariable UUID chatSessionId,
            @RequestParam(name = "portfolioManagerId", required = false) String portfolioManagerId,
            @RequestParam(name = "pmId", required = false) String pmId) {

        String effectivePortfolioManagerId = portfolioManagerId != null ? portfolioManagerId : pmId;
        chatService.delete(
                effectivePortfolioManagerId,
                chatSessionId
        );
    }
}
