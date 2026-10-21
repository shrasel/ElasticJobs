package com.jobsearch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Salary {
    private Integer min;
    private Integer max;
    private String currency; // "USD", "EUR", etc.
    private String period;   // "ANNUAL", "HOURLY"
}
