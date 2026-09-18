package com.sample.app.holdings;

import com.sample.app.ai.model.PortfolioHoldingsAssistantRequest;
import com.sample.app.ai.model.PortfolioHoldingsAssistantResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Uses Spring AI tool calling: the model may invoke {@link PortfolioHoldingsTools} to reach the (WireMock) Holdings API.
 */
@Service
public class PortfolioHoldingsAssistantService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioHoldingsAssistantService.class);

    private final ChatClient chatClient;
    private final PortfolioHoldingsTools portfolioHoldingsTools;

    public PortfolioHoldingsAssistantService(ChatClient chatClient, PortfolioHoldingsTools portfolioHoldingsTools) {
        this.chatClient = chatClient;
        this.portfolioHoldingsTools = portfolioHoldingsTools;
    }

    public PortfolioHoldingsAssistantResponse explainHoldings(PortfolioHoldingsAssistantRequest request) {
        String instrumentId = request.instrumentId() != null ? request.instrumentId().trim() : "";
        if (instrumentId.isEmpty()) {
            return new PortfolioHoldingsAssistantResponse("Missing instrument ID for holdings lookup.", false);
        }

        String snapshot = request.rowSnapshot() != null ? request.rowSnapshot().toString() : "{}";

        String userPrompt = """
                Get portfolio holdings for security instrument ID: %s.
                Supplemental row context from the screening grid (not authoritative): %s
                
                Prefer calling the holdings tool once you know the instrument id.
                Explain which portfolios hold the security, approximate exposure (USD market value), concentration, and one risk note if relevant.
                """.formatted(instrumentId, snapshot);

        log.info("Portfolio holdings assistant prompt for {}", instrumentId);

        String assistantMessage = chatClient.prompt()
                .system("""
                        You assist portfolio managers with security screening.
                        Use the provided portfolio-holdings tool for factual allocations from the holdings service.
                        If the tool returns JSON, summarize it plainly; mention instrumentId once.""")
                .user(userPrompt)
                .tools(portfolioHoldingsTools)
                .call()
                .content();

        return new PortfolioHoldingsAssistantResponse(assistantMessage != null ? assistantMessage : "", true);
    }
}
