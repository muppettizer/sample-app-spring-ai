package com.sample.app.ai.service;

import com.sample.app.ai.model.GridAiRequest;
import com.sample.app.ai.model.GridAiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GridAiService {

    private final ChatClient chatClient;

    public GridAiService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public GridAiResponse process(GridAiRequest request) {

        String prompt = """
            You are an AG Grid AI assistant.

            User query:
            %s

            Grid state:
            %s

            Schema:
            %s

            Interpret the query and return a response that strictly follows the schema.
            """.formatted(
                request.userQuery(),
                request.gridState(),
                request.structuredSchema()
        );

        log.info("Sending prompt to AI: {}", prompt);

        GridAiResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(GridAiResponse.class); // 🔥 auto JSON mapping

        log.info("Received response from AI: {}", response);

        return response;
    }
}
