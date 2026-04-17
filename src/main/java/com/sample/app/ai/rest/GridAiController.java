package com.sample.app.ai.rest;

import com.sample.app.ai.model.GridAiRequest;
import com.sample.app.ai.model.GridAiResponse;
import com.sample.app.ai.service.GridAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/ai")
public class GridAiController {

    private final GridAiService service;

    public GridAiController(GridAiService service) {
        this.service = service;
    }

    @PostMapping("/grid-query")
    public GridAiResponse query(@RequestBody GridAiRequest request) {
        log.info("Processing grid query request: {}", request);
        return service.process(request);
    }
}
