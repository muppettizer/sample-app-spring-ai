package com.sample.app.ai.model;

import java.util.List;

public record ScreeningChatResponse(
        String portfolioManagerId,
        String assistantMessage,
        List<ScreeningChatHistoryMessage> history,
        GridUpdate gridUpdate
) {}
