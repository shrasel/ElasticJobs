package com.jobsearch.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetResult {

    @JsonProperty("workplace_types")
    private Map<String, Long> workplaceTypes;

    @JsonProperty("employment_types")
    private Map<String, Long> employmentTypes;

    @JsonProperty("experience_levels")
    private Map<String, Long> experienceLevels;

    @JsonProperty("salary_ranges")
    private Map<String, Long> salaryRanges;

    @JsonProperty("top_skills")
    private Map<String, Long> topSkills;

    @JsonProperty("visa_sponsored")
    private Map<String, Long> visaSponsored;
}
