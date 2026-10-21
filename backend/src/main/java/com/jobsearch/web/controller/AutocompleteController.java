package com.jobsearch.web.controller;

import com.jobsearch.service.AutocompleteService;
import com.jobsearch.web.dto.response.AutocompleteSuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Instant Autocomplete", description = "Sub-15ms Typeahead using Elasticsearch search_as_you_type")
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    @GetMapping("/autocomplete")
    @Operation(summary = "Instant typeahead suggestions for job titles, companies, and skills")
    public ResponseEntity<Map<String, Object>> autocomplete(
            @RequestParam("q") String query,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) throws IOException {
        long start = System.currentTimeMillis();
        List<AutocompleteSuggestion> suggestions = autocompleteService.getSuggestions(query, size);
        long tookMs = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
                "query", query,
                "took_ms", tookMs,
                "count", suggestions.size(),
                "suggestions", suggestions
        ));
    }
}
