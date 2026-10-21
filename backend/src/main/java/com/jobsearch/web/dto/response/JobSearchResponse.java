package com.jobsearch.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchResponse {

    @JsonProperty("total_hits")
    private Long totalHits;

    private Integer page;
    private Integer size;

    @JsonProperty("took_ms")
    private Long tookMs;

    @JsonProperty("search_mode_used")
    private String searchModeUsed;

    private List<JobCardDto> jobs;

    private FacetResult facets;
}
