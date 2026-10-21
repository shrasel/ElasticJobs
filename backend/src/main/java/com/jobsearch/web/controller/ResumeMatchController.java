package com.jobsearch.web.controller;

import com.jobsearch.service.ResumeMatchingService;
import com.jobsearch.web.dto.response.JobCardDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Resume Matching & Similar Jobs", description = "AI vector matching for resumes and similar jobs")
public class ResumeMatchController {

    private final ResumeMatchingService resumeMatchingService;

    @PostMapping(value = "/match-resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload PDF/DOCX resume and find top matching jobs via AI vector kNN in Elasticsearch")
    public ResponseEntity<Map<String, Object>> matchResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "min_salary", required = false) Integer minSalary,
            @RequestParam(value = "visa_required", required = false) Boolean visaRequired,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) throws Exception {
        long start = System.currentTimeMillis();
        List<JobCardDto> jobs = resumeMatchingService.matchResumeToJobs(file, minSalary, visaRequired, limit);
        long tookMs = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
                "filename", file.getOriginalFilename(),
                "took_ms", tookMs,
                "matches_count", jobs.size(),
                "jobs", jobs
        ));
    }

    @PostMapping("/match-resume-text")
    @Operation(summary = "Submit raw candidate resume/profile text to find top matching jobs via vector kNN")
    public ResponseEntity<Map<String, Object>> matchResumeText(
            @RequestBody Map<String, Object> payload,
            @RequestParam(value = "min_salary", required = false) Integer minSalary,
            @RequestParam(value = "visa_required", required = false) Boolean visaRequired,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) throws IOException {
        String text = (String) payload.getOrDefault("text", "");
        long start = System.currentTimeMillis();
        List<JobCardDto> jobs = resumeMatchingService.matchTextToJobs(text, minSalary, visaRequired, limit);
        long tookMs = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
                "took_ms", tookMs,
                "matches_count", jobs.size(),
                "jobs", jobs
        ));
    }

    @GetMapping("/{id}/similar")
    @Operation(summary = "Find similar jobs to a given job posting based on Elasticsearch vector similarity")
    public ResponseEntity<Map<String, Object>> findSimilarJobs(
            @PathVariable("id") String id,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) throws IOException {
        long start = System.currentTimeMillis();
        List<JobCardDto> similarJobs = resumeMatchingService.findSimilarJobs(id, limit);
        long tookMs = System.currentTimeMillis() - start;

        return ResponseEntity.ok(Map.of(
                "target_job_id", id,
                "took_ms", tookMs,
                "similar_count", similarJobs.size(),
                "similar_jobs", similarJobs
        ));
    }
}
