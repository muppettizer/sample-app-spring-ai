package com.sample.app.ai.model;

import java.util.List;

public record ScreeningChatHistoryResponse(
        String portfolioManagerId,
        List<ChatMessageDto> history
) {}
