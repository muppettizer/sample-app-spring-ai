package com.sample.app.ai.screeningchat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AiModelConfig {

    @Profile("openai")
    @Configuration
    static class OpenAiConfig {

        @Bean
        public ChatClient openAiChatClient(OpenAiChatModel model) {
            return ChatClient.create(model);
        }
    }

    @Profile("google")
    @Configuration
    static class GoogleConfig {
        @Bean
        public ChatClient googleChatClient(GoogleGenAiChatModel model) {
            return ChatClient.create(model);
        }
    }

//    @Profile("mistral")
//    @Configuration
//    static class MistralConfig {
//        @Bean
//        public ChatClient mistralChatClient(MistralAiChatModel model) {
//            return ChatClient.create(model);
//        }
//    }
}
