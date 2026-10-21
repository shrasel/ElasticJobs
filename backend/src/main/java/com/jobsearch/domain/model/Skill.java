package com.jobsearch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    private String name;
    private String category; // e.g., "LANGUAGE", "FRAMEWORK", "CLOUD", "TOOL", "DATABASE"
}
