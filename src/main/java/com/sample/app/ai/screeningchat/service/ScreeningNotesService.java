package com.sample.app.ai.screeningchat.service;

import com.sample.app.ai.screeningchat.model.ScreeningNotesRequest;
import com.sample.app.ai.screeningchat.model.ScreeningNotesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ScreeningNotesService {

    private static final String PM_NOTES_INDEX_PREFIX = "pm:notes:";
    private static final String NOTE_FIELD_SEPARATOR = "::";

    private final RedisTemplate<String, String> redisTemplate;

    public ScreeningNotesService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ScreeningNotesResponse getNotes(String portfolioManagerId) {
        String effectivePortfolioManagerId = normalizePortfolioManagerId(portfolioManagerId);
        log.info("Getting screening notes for portfolioManagerId={}", effectivePortfolioManagerId);
        return new ScreeningNotesResponse(effectivePortfolioManagerId, readNotesFromRedis(effectivePortfolioManagerId));
    }

    public ScreeningNotesResponse saveNotes(ScreeningNotesRequest request) {
        String portfolioManagerId = normalizePortfolioManagerId(request == null ? null : request.portfolioManagerId());
        log.info("Saving screening notes for portfolioManagerId={}", portfolioManagerId);
        Map<String, Map<String, String>> notes = request == null ? Map.of() : request.notes() == null ? Map.of() : request.notes();
        writeNotesToRedis(portfolioManagerId, notes);
        return new ScreeningNotesResponse(portfolioManagerId, notes);
    }

    public String normalizePortfolioManagerId(String portfolioManagerId) {
        return portfolioManagerId == null || portfolioManagerId.isBlank() ? "default" : portfolioManagerId.trim();
    }

    private String notesKey(String portfolioManagerId) {
        return PM_NOTES_INDEX_PREFIX + normalizePortfolioManagerId(portfolioManagerId);
    }

    private Map<String, Map<String, String>> readNotesFromRedis(String portfolioManagerId) {
        Map<Object, Object> rawEntries = redisTemplate.opsForHash().entries(notesKey(portfolioManagerId));
        if (rawEntries.isEmpty()) {
            return Map.of();
        }

        Map<String, Map<String, String>> notes = new HashMap<>();
        rawEntries.forEach((field, value) -> {
            String fieldName = String.valueOf(field);
            int separatorIndex = fieldName.lastIndexOf(NOTE_FIELD_SEPARATOR);
            if (separatorIndex <= 0 || separatorIndex == fieldName.length() - NOTE_FIELD_SEPARATOR.length()) {
                return;
            }

            String rowKey = fieldName.substring(0, separatorIndex);
            String columnKey = fieldName.substring(separatorIndex + NOTE_FIELD_SEPARATOR.length());
            notes.computeIfAbsent(rowKey, ignored -> new HashMap<>())
                    .put(columnKey, String.valueOf(value));
        });

        return notes;
    }

    private void writeNotesToRedis(String portfolioManagerId, Map<String, Map<String, String>> notes) {
        String key = notesKey(portfolioManagerId);
        Map<String, String> flattenedNotes = new HashMap<>();

        notes.forEach((rowKey, columnNotes) -> {
            if (columnNotes == null) {
                return;
            }
            columnNotes.forEach((columnKey, noteText) -> {
                if (columnKey != null && noteText != null) {
                    flattenedNotes.put(rowKey + NOTE_FIELD_SEPARATOR + columnKey, noteText);
                }
            });
        });

        redisTemplate.delete(key);
        if (!flattenedNotes.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, flattenedNotes);
        }
    }
}
