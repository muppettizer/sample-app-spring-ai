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
                You are an AG Grid AI assistant that converts user queries into grid configuration JSON.

                STRICT RULES:
                - Output ONLY valid JSON (no text, no explanation)
                - MUST match the schema exactly
                - Include ALL required top-level fields:
                  - filter
                  - sort
                  - columnVisibility
                  - columnSizing
                - If a section is not modified, return it as null
                - Do NOT invent fields
                - Do NOT omit required fields
                
                USER QUERY:
                %s

                CURRENT GRID STATE:
                %s

                JSON SCHEMA:
                %s

                Return ONLY JSON:
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
