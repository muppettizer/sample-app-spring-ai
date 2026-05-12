package com.sample.app.holdings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Tools exposed to the LLM via Spring AI {@code ChatClient.prompt().tools(...)}.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Spring AI Tool calling</a>
 */
@Component
public class PortfolioHoldingsTools {

    private static final Logger log = LoggerFactory.getLogger(PortfolioHoldingsTools.class);

    private final SecurityHoldingsApiClient holdingsApiClient;

    public PortfolioHoldingsTools(SecurityHoldingsApiClient holdingsApiClient) {
        this.holdingsApiClient = holdingsApiClient;
    }

    @Tool(description = """
            Fetch aggregated portfolio holdings for a security instrument from the Securities Holdings REST API.
            Use this whenever the portfolio manager asks which portfolios hold a given instrument, exposures, or allocations.
            The JSON includes portfolio identifiers, quantities, USD market values, and weights.""")
    public String getPortfolioHoldingsForSecurity(
            @ToolParam(description = "Instrument/security ID from the grid, e.g. SEC-1042.") String instrumentId
    ) {
        log.info("Tool getPortfolioHoldingsForSecurity invoked for instrumentId={}", instrumentId);
        return holdingsApiClient.fetchPortfolioHoldingsJson(instrumentId);
    }
}
