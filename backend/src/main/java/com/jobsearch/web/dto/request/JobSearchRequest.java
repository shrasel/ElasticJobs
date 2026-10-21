package com.jobsearch.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchRequest {

    /**
     * Search query string (e.g. "react OR vue -junior", "product manager", "softwre enginer")
     */
    private String query;

    /**
     * Search mode: KEYWORD, SEMANTIC, HYBRID
     */
    @Builder.Default
    @JsonProperty("search_mode")
    private SearchMode searchMode = SearchMode.HYBRID;

    /**
     * Enable fuzzy typo tolerance (AUTO:3,6)
     */
    @Builder.Default
    @JsonProperty("fuzzy_enabled")
    private Boolean fuzzyEnabled = true;

    /**
     * Multi-faceted structured filters
     */
    private SearchFilters filters;

    /**
     * Sorting options: RELEVANCE, GEO_DISTANCE, SALARY_DESC, POSTED_DATE_DESC
     */
    @Builder.Default
    @JsonProperty("sort_by")
    private SortBy sortBy = SortBy.RELEVANCE;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    public enum SearchMode {
        KEYWORD,
        SEMANTIC,
        HYBRID
    }

    public enum SortBy {
        RELEVANCE,
        GEO_DISTANCE,
        SALARY_DESC,
        POSTED_DATE_DESC
    }
}
