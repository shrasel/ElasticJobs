package com.jobsearch.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoRadiusFilter {
    private Double lat;
    private Double lon;
    @Builder.Default
    private Double radiusMiles = 25.0;
}
