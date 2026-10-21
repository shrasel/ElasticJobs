package com.jobsearch.web.controller;

import com.jobsearch.domain.model.JobDocument;
import com.jobsearch.domain.repository.JobElasticsearchRepository;
import com.jobsearch.service.EmbeddingService;
import com.jobsearch.service.IndexInitializationService;
import com.jobsearch.service.JobSearchService;
import com.jobsearch.util.SampleDataSeeder;
import com.jobsearch.web.dto.request.JobSearchRequest;
import com.jobsearch.web.dto.response.JobSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Search & Management", description = "Unified search (Keyword, Boolean, Fuzzy, Geo, Facets, Hybrid RRF)")
public class JobSearchController {

    private final JobSearchService searchService;
    private final JobElasticsearchRepository jobRepository;
    private final EmbeddingService embeddingService;
    private final IndexInitializationService indexService;
    private final SampleDataSeeder seeder;

    @PostMapping("/search")
    @Operation(summary = "Search jobs with weighted lexical, fuzzy, geo-radius, multi-faceted filtering, or hybrid RRF")
    public ResponseEntity<JobSearchResponse> search(@RequestBody JobSearchRequest request) throws IOException {
        return ResponseEntity.ok(searchService.search(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single job details by ID directly from Elasticsearch")
    public ResponseEntity<JobDocument> getJobById(@PathVariable String id) throws IOException {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create and index a new job posting in Elasticsearch")
    public ResponseEntity<JobDocument> createJob(@RequestBody JobDocument job) throws IOException {
        if (job.getId() == null || job.getId().isBlank()) {
            job.setId("job-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (job.getPostedAt() == null) {
            job.setPostedAt(Instant.now());
        }
        if (job.getIsActive() == null) {
            job.setIsActive(true);
        }

        // Auto-generate dense embedding vector if not present
        if (job.getEmbedding() == null || job.getEmbedding().isEmpty()) {
            String combinedText = job.getTitle() + " " + (job.getDescription() != null ? job.getDescription() : "");
            job.setEmbedding(embeddingService.generateEmbedding(combinedText));
        }

        jobRepository.save(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @PostMapping("/seed")
    @Operation(summary = "Re-seed Elasticsearch index with realistic test jobs")
    public ResponseEntity<Map<String, Object>> seedData() throws Exception {
        indexService.recreateIndex();
        int count = seeder.seedJobs();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Index recreated and seeded",
                "indexed_jobs_count", count
        ));
    }
}
