//package com.sample.app.ai.repository;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.memory.ChatMemoryRepository;
//import org.springframework.ai.chat.messages.AssistantMessage;
//import org.springframework.ai.chat.messages.Message;
//import org.springframework.ai.chat.messages.SystemMessage;
//import org.springframework.ai.chat.messages.UserMessage;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//
//@Slf4j
//@Component
//public class RedisChatMemoryRepository implements ChatMemoryRepository {
//
//    private static final String PREFIX = "screening:chat:";
//    private static final TypeReference<List<Map<String, String>>> HISTORY_TYPE = new TypeReference<>() {};
//
//    private final StringRedisTemplate redisTemplate;
//    private final ObjectMapper objectMapper;
//
//    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
//        this.redisTemplate = redisTemplate;
//        this.objectMapper = objectMapper;
//    }
//
//    @Override
//    public List<String> findConversationIds() {
//        Set<String> keys = redisTemplate.keys(PREFIX + "*");
//        if (keys == null || keys.isEmpty()) {
//            return Collections.emptyList();
//        }
//        return keys.stream()
//                .map(key -> key.substring(PREFIX.length()))
//                .toList();
//    }
//
//    @Override
//    public List<Message> findByConversationId(String conversationId) {
//        String json = redisTemplate.opsForValue().get(key(conversationId));
//        if (json == null || json.isBlank()) {
//            return new ArrayList<>();
//        }
//
//        try {
//            List<Map<String, String>> stored = objectMapper.readValue(json, HISTORY_TYPE);
//            List<Message> messages = new ArrayList<>();
//            for (Map<String, String> item : stored) {
//                String role = item.getOrDefault("role", "assistant");
//                String message = item.getOrDefault("message", "");
//                messages.add(toMessage(role, message));
//            }
//            return messages;
//        } catch (Exception e) {
//            log.warn("Failed to parse Redis chat memory for conversationId={}", conversationId, e);
//            return new ArrayList<>();
//        }
//    }
//
//    @Override
//    public void saveAll(String conversationId, List<Message> messages) {
//        List<Map<String, String>> stored = messages.stream()
//                .filter(Objects::nonNull)
//                .map(message -> Map.of(
//                        "role", roleOf(message),
//                        "message", message.getText() == null ? "" : message.getText()
//                ))
//                .toList();
//        try {
//            String json = objectMapper.writeValueAsString(stored);
//            redisTemplate.opsForValue().set(key(conversationId), json);
//        } catch (Exception e) {
//            throw new IllegalStateException("Failed to serialize Redis chat memory", e);
//        }
//    }
//
//    @Override
//    public void deleteByConversationId(String conversationId) {
//        redisTemplate.delete(key(conversationId));
//    }
//
//    private String key(String conversationId) {
//        return PREFIX + conversationId;
//    }
//
//    private Message toMessage(String role, String message) {
//        return switch (role) {
//            case "user" -> new UserMessage(message);
//            case "system" -> new SystemMessage(message);
//            default -> new AssistantMessage(message);
//        };
//    }
//
//    private String roleOf(Message message) {
//        if (message instanceof UserMessage) {
//            return "user";
//        }
//        if (message instanceof SystemMessage) {
//            return "system";
//        }
//        return "assistant";
//    }
//}
