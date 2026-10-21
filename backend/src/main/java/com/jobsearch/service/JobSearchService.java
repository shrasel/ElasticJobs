package com.jobsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jobsearch.domain.model.JobDocument;
import com.jobsearch.domain.model.Location;
import com.jobsearch.web.dto.request.GeoRadiusFilter;
import com.jobsearch.web.dto.request.JobSearchRequest;
import com.jobsearch.web.dto.request.SearchFilters;
import com.jobsearch.web.dto.response.FacetResult;
import com.jobsearch.web.dto.response.JobCardDto;
import com.jobsearch.web.dto.response.JobSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSearchService {

    private final ElasticsearchClient client;
    private final EmbeddingService embeddingService;

    @Value("${elasticsearch.index-alias:jobs}")
    private String indexAlias;

    public JobSearchResponse search(JobSearchRequest request) throws IOException {
        long startTime = System.currentTimeMillis();

        String rawQuery = request.getQuery() != null ? request.getQuery().trim() : "";
        JobSearchRequest.SearchMode mode = request.getSearchMode() != null ? request.getSearchMode() : JobSearchRequest.SearchMode.HYBRID;

        // If no query string is provided, fallback to match_all with filters
        if (rawQuery.isEmpty()) {
            mode = JobSearchRequest.SearchMode.KEYWORD;
        }

        SearchRequest.Builder srb = new SearchRequest.Builder()
                .index(indexAlias)
                .from(request.getPage() * request.getSize())
                .size(request.getSize());

        // 1. Build Filter Clauses (cached bitsets for fast faceted search)
        List<Query> filterClauses = buildFilterClauses(request.getFilters());

        // 2. Build Query & Knn according to mode
        switch (mode) {
            case SEMANTIC -> {
                List<Float> queryVector = embeddingService.generateEmbedding(rawQuery);
                srb.knn(k -> k
                        .field("embedding")
                        .queryVector(queryVector)
                        .k(request.getSize() * 3)
                        .numCandidates(request.getSize() * 5)
                        .filter(filterClauses)
                );
            }
            case HYBRID -> {
                // Lexical query
                Query lexicalQuery = buildLexicalQuery(rawQuery, request.getFuzzyEnabled());
                Query combinedQuery = Query.of(q -> q
                        .bool(b -> b
                                .must(lexicalQuery)
                                .filter(filterClauses)
                        )
                );
                srb.query(combinedQuery);

                // Vector query
                List<Float> queryVector = embeddingService.generateEmbedding(rawQuery);
                srb.knn(k -> k
                        .field("embedding")
                        .queryVector(queryVector)
                        .k(request.getSize() * 3)
                        .numCandidates(request.getSize() * 5)
                        .filter(filterClauses)
                );

                // Native RRF ranking (Elasticsearch 8.8+)
                srb.rank(r -> r
                        .rrf(rrf -> rrf
                                .rankWindowSize(50L)
                                .rankConstant(60L)
                        )
                );
            }
            case KEYWORD -> {
                Query lexicalQuery = rawQuery.isEmpty()
                        ? Query.of(q -> q.matchAll(m -> m))
                        : buildLexicalQuery(rawQuery, request.getFuzzyEnabled());

                Query combinedQuery = Query.of(q -> q
                        .bool(b -> b
                                .must(lexicalQuery)
                                .filter(filterClauses)
                        )
                );
                srb.query(combinedQuery);
            }
        }

        // 3. Sorting (when not using RRF ranking)
        if (mode != JobSearchRequest.SearchMode.HYBRID) {
            applySorting(srb, request.getSortBy(), request.getFilters());
        }

        // 4. Multi-Faceted Aggregations
        addAggregations(srb);

        // 5. Execute Search against Elasticsearch
        SearchResponse<JobDocument> response = client.search(srb.build(), JobDocument.class);

        // 6. Map Hits to DTOs
        List<JobCardDto> jobCards = new ArrayList<>();
        GeoRadiusFilter geo = request.getFilters() != null ? request.getFilters().getGeoRadius() : null;

        for (Hit<JobDocument> hit : response.hits().hits()) {
            JobDocument doc = hit.source();
            if (doc != null) {
                Double distance = null;
                if (geo != null && geo.getLat() != null && geo.getLon() != null
                        && doc.getLocation() != null && doc.getLocation().getCoordinates() != null) {
                    distance = calculateDistanceMiles(
                            geo.getLat(), geo.getLon(),
                            doc.getLocation().getCoordinates().getLat(),
                            doc.getLocation().getCoordinates().getLon()
                    );
                }

                jobCards.add(JobCardDto.builder()
                        .id(doc.getId())
                        .title(doc.getTitle())
                        .company(doc.getCompany())
                        .description(doc.getDescription())
                        .skills(doc.getSkills())
                        .location(doc.getLocation())
                        .distanceMiles(distance != null ? Math.round(distance * 10.0) / 10.0 : null)
                        .workplaceType(doc.getWorkplaceType())
                        .employmentType(doc.getEmploymentType())
                        .experienceLevel(doc.getExperienceLevel())
                        .salary(doc.getSalary())
                        .visaSponsored(doc.getVisaSponsored())
                        .postedAt(doc.getPostedAt())
                        .score(hit.score() != null ? Math.round(hit.score() * 10000.0) / 10000.0 : null)
                        .build());
            }
        }

        // 7. Extract Facets
        FacetResult facets = extractFacets(response.aggregations());

        long tookMs = System.currentTimeMillis() - startTime;
        long totalHits = response.hits().total() != null ? response.hits().total().value() : jobCards.size();

        return JobSearchResponse.builder()
                .totalHits(totalHits)
                .page(request.getPage())
                .size(request.getSize())
                .tookMs(tookMs)
                .searchModeUsed(mode.name())
                .jobs(jobCards)
                .facets(facets)
                .build();
    }

    /**
     * Builds weighted keyword query with boolean logic and fuzzy typo-tolerance support.
     */
    private Query buildLexicalQuery(String queryText, Boolean fuzzyEnabled) {
        boolean hasBooleanOperators = queryText.contains(" OR ") || queryText.contains(" AND ")
                || queryText.contains(" -") || queryText.startsWith("-") || queryText.contains(" + ")
                || queryText.contains("\"");

        if (hasBooleanOperators) {
            // Feature 1: Boolean syntax with field boosting (Lucene syntax: AND, OR, NOT, -, +, parentheses)
            return Query.of(q -> q
                    .queryString(qs -> qs
                            .query(queryText)
                            .fields(
                                    "title^4.0",
                                    "company.name^2.0",
                                    "skills.name^2.5",
                                    "description^1.0"
                            )
                            .defaultOperator(Operator.Or)
                    )
            );
        }

        if (Boolean.TRUE.equals(fuzzyEnabled)) {
            // Feature 2: Typo-Tolerant (Fuzzy) Search with field weights
            return Query.of(q -> q
                    .multiMatch(mm -> mm
                            .query(queryText)
                            .fields(
                                    "title^4.0",
                                    "company.name^2.0",
                                    "skills.name^2.5",
                                    "description^1.0"
                            )
                            .fuzziness("AUTO:3,6")
                            .prefixLength(2)
                            .maxExpansions(50)
                            .type(TextQueryType.MostFields)
                    )
            );
        } else {
            // Standard multi_match with boosts
            return Query.of(q -> q
                    .multiMatch(mm -> mm
                            .query(queryText)
                            .fields(
                                    "title^4.0",
                                    "company.name^2.0",
                                    "skills.name^2.5",
                                    "description^1.0"
                            )
                            .type(TextQueryType.CrossFields)
                    )
            );
        }
    }

    /**
     * Builds structured non-scoring filter clauses (Feature 4 Geo & Feature 5 Multi-Faceted)
     */
    private List<Query> buildFilterClauses(SearchFilters filters) {
        List<Query> clauses = new ArrayList<>();

        // Always filter active jobs by default
        clauses.add(Query.of(q -> q.term(t -> t.field("is_active").value(true))));

        if (filters == null) {
            return clauses;
        }

        // Workplace Type (REMOTE, HYBRID, ON_SITE)
        if (filters.getWorkplaceTypes() != null && !filters.getWorkplaceTypes().isEmpty()) {
            clauses.add(Query.of(q -> q
                    .terms(t -> t
                            .field("workplace_type")
                            .terms(terms -> terms.value(filters.getWorkplaceTypes().stream().map(co.elastic.clients.elasticsearch._types.FieldValue::of).collect(Collectors.toList())))
                    )
            ));
        }

        // Employment Type (FULL_TIME, etc.)
        if (filters.getEmploymentTypes() != null && !filters.getEmploymentTypes().isEmpty()) {
            clauses.add(Query.of(q -> q
                    .terms(t -> t
                            .field("employment_type")
                            .terms(terms -> terms.value(filters.getEmploymentTypes().stream().map(co.elastic.clients.elasticsearch._types.FieldValue::of).collect(Collectors.toList())))
                    )
            ));
        }

        // Experience Level (ENTRY, SENIOR, etc.)
        if (filters.getExperienceLevels() != null && !filters.getExperienceLevels().isEmpty()) {
            clauses.add(Query.of(q -> q
                    .terms(t -> t
                            .field("experience_level")
                            .terms(terms -> terms.value(filters.getExperienceLevels().stream().map(co.elastic.clients.elasticsearch._types.FieldValue::of).collect(Collectors.toList())))
                    )
            ));
        }

        // Visa Sponsorship
        if (filters.getVisaSponsored() != null) {
            clauses.add(Query.of(q -> q.term(t -> t.field("visa_sponsored").value(filters.getVisaSponsored()))));
        }

        // Salary Range
        if (filters.getMinSalary() != null || filters.getMaxSalary() != null) {
            clauses.add(Query.of(q -> q
                    .range(r -> r.number(n -> {
                        n.field("salary.min");
                        if (filters.getMinSalary() != null) {
                            n.gte((double) filters.getMinSalary());
                        }
                        if (filters.getMaxSalary() != null) {
                            n.lte((double) filters.getMaxSalary());
                        }
                        return n;
                    }))
            ));
        }

        // Posted within days
        if (filters.getPostedWithinDays() != null && filters.getPostedWithinDays() > 0) {
            String gteDate = "now-" + filters.getPostedWithinDays() + "d/d";
            clauses.add(Query.of(q -> q
                    .range(r -> r.date(d -> d
                            .field("posted_at")
                            .gte(gteDate)
                    ))
            ));
        }

        // Specific Skills (nested)
        if (filters.getSkills() != null && !filters.getSkills().isEmpty()) {
            for (String skillName : filters.getSkills()) {
                clauses.add(Query.of(q -> q
                        .nested(n -> n
                                .path("skills")
                                .query(nq -> nq
                                        .term(t -> t
                                                .field("skills.name.raw")
                                                .value(skillName)
                                        )
                                )
                        )
                ));
            }
        }

        // Feature 4: Geo-Radius Location Search
        GeoRadiusFilter geo = filters.getGeoRadius();
        if (geo != null && geo.getLat() != null && geo.getLon() != null) {
            double radius = geo.getRadiusMiles() != null ? geo.getRadiusMiles() : 25.0;
            clauses.add(Query.of(q -> q
                    .geoDistance(gd -> gd
                            .field("location.coordinates")
                            .distance(radius + "mi")
                            .location(l -> l.latlon(ll -> ll.lat(geo.getLat()).lon(geo.getLon())))
                            .distanceType(GeoDistanceType.Arc)
                    )
            ));
        }

        return clauses;
    }

    private void applySorting(SearchRequest.Builder srb, JobSearchRequest.SortBy sortBy, SearchFilters filters) {
        if (sortBy == null) {
            sortBy = JobSearchRequest.SortBy.RELEVANCE;
        }

        switch (sortBy) {
            case GEO_DISTANCE -> {
                if (filters != null && filters.getGeoRadius() != null
                        && filters.getGeoRadius().getLat() != null && filters.getGeoRadius().getLon() != null) {
                    double lat = filters.getGeoRadius().getLat();
                    double lon = filters.getGeoRadius().getLon();
                    srb.sort(SortOptions.of(so -> so
                            .geoDistance(gd -> gd
                                    .field("location.coordinates")
                                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                                    .order(SortOrder.Asc)
                            )
                    ));
                }
            }
            case SALARY_DESC -> srb.sort(SortOptions.of(so -> so
                    .field(f -> f.field("salary.min").order(SortOrder.Desc))
            ));
            case POSTED_DATE_DESC -> srb.sort(SortOptions.of(so -> so
                    .field(f -> f.field("posted_at").order(SortOrder.Desc))
            ));
            case RELEVANCE -> srb.sort(SortOptions.of(so -> so
                    .score(sc -> sc.order(SortOrder.Desc))
            ));
        }
    }

    private void addAggregations(SearchRequest.Builder srb) {
        srb.aggregations("workplace_types", a -> a.terms(t -> t.field("workplace_type").size(10)));
        srb.aggregations("employment_types", a -> a.terms(t -> t.field("employment_type").size(10)));
        srb.aggregations("experience_levels", a -> a.terms(t -> t.field("experience_level").size(10)));
        srb.aggregations("visa_sponsored", a -> a.terms(t -> t.field("visa_sponsored").size(5)));

        srb.aggregations("salary_ranges", a -> a.range(r -> r
                .field("salary.min")
                .ranges(
                        AggregationRange.of(rg -> rg.to(60000.0).key("<$60k")),
                        AggregationRange.of(rg -> rg.from(60000.0).to(100000.0).key("$60k-$100k")),
                        AggregationRange.of(rg -> rg.from(100000.0).to(140000.0).key("$100k-$140k")),
                        AggregationRange.of(rg -> rg.from(140000.0).to(180000.0).key("$140k-$180k")),
                        AggregationRange.of(rg -> rg.from(180000.0).key("$180k+"))
                )
        ));

        srb.aggregations("top_skills", a -> a
                .nested(n -> n.path("skills"))
                .aggregations("skill_names", sa -> sa.terms(t -> t.field("skills.name.raw").size(15)))
        );
    }

    private FacetResult extractFacets(Map<String, Aggregate> aggs) {
        if (aggs == null) return null;

        Map<String, Long> workplaceTypes = extractStringTerms(aggs.get("workplace_types"));
        Map<String, Long> employmentTypes = extractStringTerms(aggs.get("employment_types"));
        Map<String, Long> experienceLevels = extractStringTerms(aggs.get("experience_levels"));
        Map<String, Long> visaSponsored = extractStringTerms(aggs.get("visa_sponsored"));
        Map<String, Long> salaryRanges = extractRangeBuckets(aggs.get("salary_ranges"));

        Map<String, Long> topSkills = new LinkedHashMap<>();
        if (aggs.containsKey("top_skills") && aggs.get("top_skills").isNested()) {
            NestedAggregate nested = aggs.get("top_skills").nested();
            if (nested.aggregations().containsKey("skill_names")) {
                topSkills = extractStringTerms(nested.aggregations().get("skill_names"));
            }
        }

        return FacetResult.builder()
                .workplaceTypes(workplaceTypes)
                .employmentTypes(employmentTypes)
                .experienceLevels(experienceLevels)
                .visaSponsored(visaSponsored)
                .salaryRanges(salaryRanges)
                .topSkills(topSkills)
                .build();
    }

    private Map<String, Long> extractStringTerms(Aggregate aggregate) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (aggregate != null && aggregate.isSterms()) {
            StringTermsAggregate sterms = aggregate.sterms();
            for (StringTermsBucket b : sterms.buckets().array()) {
                result.put(b.key().stringValue(), b.docCount());
            }
        }
        return result;
    }

    private Map<String, Long> extractRangeBuckets(Aggregate aggregate) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (aggregate != null && aggregate.isRange()) {
            RangeAggregate rangeAgg = aggregate.range();
            for (RangeBucket b : rangeAgg.buckets().array()) {
                if (b.docCount() > 0) {
                    result.put(b.key(), b.docCount());
                }
            }
        }
        return result;
    }

    private double calculateDistanceMiles(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusMiles = 3958.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMiles * c;
    }
}
