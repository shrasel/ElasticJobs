package com.jobsearch.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutocompleteSuggestion {
    private String text;
    private String category; // "TITLE", "COMPANY", "SKILL"
    private Double score;
}
