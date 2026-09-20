package com.sample.app.ai.screeningchat.model;

import java.util.Map;

public record PortfolioHoldingsAssistantRequest(
        String instrumentId,
        Map<String, Object> rowSnapshot
) {}
