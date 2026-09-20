package com.sample.app.ai.screeningchat.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class ChatMemoryConfig {

    public static final String CHAT_MEMORY_REDIS_INDEX_NAME = "screening-chat-index";
    public static final String CHAT_MEMORY_REDIS_KEY_PREFIX = "chat-memory:";

    @Bean
    public ChatMemory chatMemory(
            ChatMemoryRepository chatMemoryRepository) {

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(50)
                .build();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(
            DataRedisProperties redisProperties) {

        RedisClient redisClient = RedisClient.builder()
                .hostAndPort(
                        redisProperties.getHost(),
                        redisProperties.getPort())
                .build();

        // FIX for AssistantMessage not being returned
        // https://github.com/spring-projects/spring-ai/issues/5365
        List<Map<String, String>> metadataFields = List.of(
                Map.of("name", "id", "type", "text"),
                Map.of("name", "messageType", "type", "tag"),
                Map.of("name", "choiceIndex", "type", "numeric")
        );

        return RedisChatMemoryRepository.builder()
                .jedisClient(redisClient)
                .indexName(CHAT_MEMORY_REDIS_INDEX_NAME)
                .keyPrefix(CHAT_MEMORY_REDIS_KEY_PREFIX)
                .metadataFields(metadataFields)
                .timeToLive(Duration.ofDays(7))
                .build();
    }
}
