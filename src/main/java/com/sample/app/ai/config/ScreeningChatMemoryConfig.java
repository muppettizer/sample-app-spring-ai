package com.sample.app.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.RedisClient;

import java.time.Duration;

@Configuration
public class ScreeningChatMemoryConfig {

//    @Bean
//    public ChatMemory screeningChatMemory() {
//        return MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(50)
//                .build();
//    }

    @Bean
    public ChatMemory screeningChatMemory(ChatMemoryRepository chatMemoryRepository) {

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(50)
                .build();
    }

    @Bean
    public ChatMemoryRepository chatMemoryRepository(DataRedisProperties redisProperties) {
        RedisClient redisClient = RedisClient.builder()
                .hostAndPort(
                        redisProperties.getHost(),
                        redisProperties.getPort())
                .build();
        return RedisChatMemoryRepository.builder()
                .jedisClient(redisClient)
                .indexName("my-chat-index")
                .keyPrefix("my-chat:")
                .timeToLive(Duration.ofDays(7))
                .build();
    }

}
