package com.jobsearch.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDocument {
    private String id;
    private String title;
    private Company company;
    private String description;
    private List<Skill> skills;
    private Location location;

    @JsonProperty("workplace_type")
    private String workplaceType; // REMOTE, HYBRID, ON_SITE

    @JsonProperty("employment_type")
    private String employmentType; // FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP

    @JsonProperty("experience_level")
    private String experienceLevel; // ENTRY, MID, SENIOR, LEAD, EXECUTIVE

    private Salary salary;

    @JsonProperty("visa_sponsored")
    private Boolean visaSponsored;

    @JsonProperty("posted_at")
    private Instant postedAt;

    @JsonProperty("is_active")
    private Boolean isActive;

    private List<Float> embedding; // 384 dimensions
}
