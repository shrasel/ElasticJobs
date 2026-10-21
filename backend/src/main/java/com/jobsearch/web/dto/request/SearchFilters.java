package com.jobsearch.web.dto.request;

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
public class SearchFilters {

    @JsonProperty("workplace_types")
    private List<String> workplaceTypes; // e.g., ["REMOTE", "HYBRID", "ON_SITE"]

    @JsonProperty("employment_types")
    private List<String> employmentTypes; // e.g., ["FULL_TIME", "PART_TIME", "CONTRACT"]

    @JsonProperty("experience_levels")
    private List<String> experienceLevels; // e.g., ["ENTRY", "MID", "SENIOR", "LEAD"]

    @JsonProperty("visa_sponsored")
    private Boolean visaSponsored;

    @JsonProperty("min_salary")
    private Integer minSalary;

    @JsonProperty("max_salary")
    private Integer maxSalary;

    @JsonProperty("posted_within_days")
    private Integer postedWithinDays; // e.g. 1 (24h), 7 (last 7 days), 30

    @JsonProperty("skills")
    private List<String> skills;

    @JsonProperty("geo_radius")
    private GeoRadiusFilter geoRadius;
}
