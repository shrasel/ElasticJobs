package com.jobsearch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private String city;
    private String state;
    private String country;
    private GeoCoordinates coordinates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoCoordinates {
        private Double lat;
        private Double lon;
    }
}
