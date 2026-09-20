package com.sample.app.ai.screeningchat.service;

import com.sample.app.ai.screeningchat.model.chat.ChatSessionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ScreeningChatSessionService {

    private static final String PM_SESSIONS_INDEX_PREFIX = "pm:sessions:";

    private final RedisTemplate<String, String> redisTemplate;

    public ScreeningChatSessionService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ChatSessionResponse create(String portfolioManagerId) {
        var chatSessionId = UUID.randomUUID();
        var response = new ChatSessionResponse(chatSessionId, portfolioManagerId);

        redisTemplate.opsForHash().put(sessionsKey(portfolioManagerId), chatSessionId.toString(), portfolioManagerId);

        log.info("Creating screening chat session: portfolioManagerId={}, chatSessionId={}", portfolioManagerId, chatSessionId);

        return response;
    }

    public List<ChatSessionResponse> getSessions(String portfolioManagerId) {
        Map<Object, Object> rawEntries = redisTemplate.opsForHash().entries(sessionsKey(portfolioManagerId));

        if (rawEntries.isEmpty()) {
            return List.of();
        }

        return rawEntries.entrySet().stream()
                .map(entry -> new ChatSessionResponse(
                        UUID.fromString(String.valueOf(entry.getKey())),
                        String.valueOf(entry.getValue())
                ))
                .sorted(Comparator.comparing(ChatSessionResponse::chatSessionId))
                .toList();
    }

    private String sessionsKey(String portfolioManagerId) {
        return PM_SESSIONS_INDEX_PREFIX + portfolioManagerId;
    }
}