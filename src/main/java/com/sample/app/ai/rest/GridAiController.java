package com.sample.app.ai.rest;

import com.sample.app.ai.model.GridAiRequest;
import com.sample.app.ai.model.GridAiResponse;
import com.sample.app.ai.model.ScreeningChatHistoryResponse;
import com.sample.app.ai.model.ScreeningChatRequest;
import com.sample.app.ai.model.ScreeningChatResponse;
import com.sample.app.ai.model.PortfolioHoldingsAssistantRequest;
import com.sample.app.ai.model.PortfolioHoldingsAssistantResponse;
import com.sample.app.ai.service.GridAiService;
import com.sample.app.ai.service.ScreeningChatService;
import com.sample.app.holdings.PortfolioHoldingsAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/ai")
public class GridAiController {

    private final GridAiService service;
    private final ScreeningChatService screeningChatService;
    private final PortfolioHoldingsAssistantService portfolioHoldingsAssistantService;

    public GridAiController(
            GridAiService service,
            ScreeningChatService screeningChatService,
            PortfolioHoldingsAssistantService portfolioHoldingsAssistantService
    ) {
        this.service = service;
        this.screeningChatService = screeningChatService;
        this.portfolioHoldingsAssistantService = portfolioHoldingsAssistantService;
    }

    @PostMapping("/grid-query")
    public GridAiResponse query(@RequestBody GridAiRequest request) {
        log.info("Processing grid query request: {}", request);
        return service.process(request);
    }

    @PostMapping("/screening-chat")
    public ScreeningChatResponse screeningChat(@RequestBody ScreeningChatRequest request) {
        log.info("Processing screening chat for portfolioManagerId={}", request.portfolioManagerId());
        return screeningChatService.chat(request);
    }

    @GetMapping("/screening-chat/{portfolioManagerId}/history")
    public ScreeningChatHistoryResponse screeningChatHistory(@PathVariable String portfolioManagerId) {
        return screeningChatService.getHistory(portfolioManagerId);
    }

    @DeleteMapping("/screening-chat/{portfolioManagerId}/history")
    public void clearScreeningChatHistory(@PathVariable String portfolioManagerId) {
        screeningChatService.clearHistory(portfolioManagerId);
    }

    @PostMapping("/portfolio-holdings-assistant")
    public PortfolioHoldingsAssistantResponse portfolioHoldingsAssistant(@RequestBody PortfolioHoldingsAssistantRequest request) {
        log.info("Portfolio holdings assistant for instrumentId={}", request.instrumentId());
        return portfolioHoldingsAssistantService.explainHoldings(request);
    }
}
