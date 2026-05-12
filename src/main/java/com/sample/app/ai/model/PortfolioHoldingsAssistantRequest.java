package com.sample.app.ai.model;

import java.util.Map;

public record PortfolioHoldingsAssistantRequest(
        String instrumentId,
        Map<String, Object> rowSnapshot
) {}
