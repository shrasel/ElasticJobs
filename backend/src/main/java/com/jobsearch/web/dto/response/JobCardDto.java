package com.jobsearch.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobsearch.domain.model.Company;
import com.jobsearch.domain.model.Location;
import com.jobsearch.domain.model.Salary;
import com.jobsearch.domain.model.Skill;
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
public class JobCardDto {
    private String id;
    private String title;
    private Company company;
    private String description;
    private List<Skill> skills;
    private Location location;

    @JsonProperty("distance_miles")
    private Double distanceMiles;

    @JsonProperty("workplace_type")
    private String workplaceType;

    @JsonProperty("employment_type")
    private String employmentType;

    @JsonProperty("experience_level")
    private String experienceLevel;

    private Salary salary;

    @JsonProperty("visa_sponsored")
    private Boolean visaSponsored;

    @JsonProperty("posted_at")
    private Instant postedAt;

    private Double score;
}
