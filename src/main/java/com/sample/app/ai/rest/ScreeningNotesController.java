package com.sample.app.ai.rest;

import com.sample.app.ai.model.ScreeningNotesRequest;
import com.sample.app.ai.model.ScreeningNotesResponse;
import com.sample.app.ai.service.ScreeningNotesService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/ai")
public class ScreeningNotesController {

    private final ScreeningNotesService screeningNotesService;

    public ScreeningNotesController(ScreeningNotesService screeningNotesService) {
        this.screeningNotesService = screeningNotesService;
    }

    @GetMapping("/screening-notes")
    public ScreeningNotesResponse getScreeningNotes(@RequestParam(required = false) String portfolioManagerId) {
        return screeningNotesService.getNotes(portfolioManagerId);
    }

    @GetMapping("/screening-notes/{portfolioManagerId}")
    public ScreeningNotesResponse getScreeningNotesByPath(@PathVariable String portfolioManagerId) {
        return screeningNotesService.getNotes(portfolioManagerId);
    }

    @PostMapping("/screening-notes")
    public ScreeningNotesResponse saveScreeningNotes(@RequestBody ScreeningNotesRequest request) {
        return screeningNotesService.saveNotes(request);
    }

    @PostMapping("/screening-notes/{portfolioManagerId}")
    public ScreeningNotesResponse saveScreeningNotesByPath(@PathVariable String portfolioManagerId,
                                                         @RequestBody ScreeningNotesRequest request) {
        String effectivePortfolioManagerId = screeningNotesService.normalizePortfolioManagerId(
                request == null || request.portfolioManagerId() == null ? portfolioManagerId : request.portfolioManagerId());
        return screeningNotesService.saveNotes(new ScreeningNotesRequest(effectivePortfolioManagerId, request == null ? java.util.Map.of() : request.notes()));
    }
}
