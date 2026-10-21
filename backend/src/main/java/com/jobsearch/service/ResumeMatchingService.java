package com.jobsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jobsearch.domain.model.JobDocument;
import com.jobsearch.domain.repository.JobElasticsearchRepository;
import com.jobsearch.web.dto.response.JobCardDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeMatchingService {

    private final ElasticsearchClient client;
    private final JobElasticsearchRepository jobRepository;
    private final EmbeddingService embeddingService;
    private final Tika tika = new Tika();

    @Value("${elasticsearch.index-alias:jobs}")
    private String indexAlias;

    /**
     * Parses uploaded Resume PDF/DOCX, converts to a dense vector, and finds top matching jobs via Elasticsearch kNN.
     */
    public List<JobCardDto> matchResumeToJobs(
            MultipartFile resumeFile,
            Integer minSalary,
            Boolean visaRequired,
            int limit
    ) throws Exception {
        String parsedText;
        try (InputStream is = resumeFile.getInputStream()) {
            parsedText = tika.parseToString(is);
        }

        if (parsedText == null || parsedText.isBlank()) {
            log.warn("Resume parsing produced empty text");
            return Collections.emptyList();
        }

        log.info("Successfully parsed resume [{}], extracted {} characters of text",
                resumeFile.getOriginalFilename(), parsedText.length());

        return matchTextToJobs(parsedText, minSalary, visaRequired, limit);
    }

    /**
     * Matches raw resume text against jobs in Elasticsearch via kNN.
     */
    public List<JobCardDto> matchTextToJobs(
            String resumeText,
            Integer minSalary,
            Boolean visaRequired,
            int limit
    ) throws IOException {
        List<Float> resumeEmbedding = embeddingService.generateEmbedding(resumeText);

        List<Query> filterClauses = new ArrayList<>();
        filterClauses.add(Query.of(q -> q.term(t -> t.field("is_active").value(true))));

        if (minSalary != null) {
            filterClauses.add(Query.of(q -> q
                    .range(r -> r.number(n -> n
                            .field("salary.min")
                            .gte((double) minSalary)
                    ))
            ));
        }

        if (Boolean.TRUE.equals(visaRequired)) {
            filterClauses.add(Query.of(q -> q.term(t -> t.field("visa_sponsored").value(true))));
        }

        SearchResponse<JobDocument> response = client.search(s -> s
                .index(indexAlias)
                .size(limit)
                .knn(k -> k
                        .field("embedding")
                        .queryVector(resumeEmbedding)
                        .k(limit)
                        .numCandidates(limit * 3)
                        .filter(filterClauses)
                ),
                JobDocument.class
        );

        return mapHitsToJobCards(response);
    }

    /**
     * Finds Similar Jobs based on the selected job's dense vector embedding.
     */
    public List<JobCardDto> findSimilarJobs(String jobId, int limit) throws IOException {
        Optional<JobDocument> targetJobOpt = jobRepository.findById(jobId);
        if (targetJobOpt.isEmpty()) {
            log.warn("Job not found with id: {}", jobId);
            return Collections.emptyList();
        }

        JobDocument targetJob = targetJobOpt.get();
        List<Float> jobVector = targetJob.getEmbedding();
        if (jobVector == null || jobVector.isEmpty()) {
            jobVector = embeddingService.generateEmbedding(targetJob.getTitle() + " " + targetJob.getDescription());
        }

        // Filter: active jobs, excluding self
        List<Query> filterClauses = List.of(
                Query.of(q -> q.term(t -> t.field("is_active").value(true))),
                Query.of(q -> q.bool(b -> b.mustNot(mn -> mn.term(t -> t.field("id").value(jobId)))))
        );

        List<Float> finalVector = jobVector;
        SearchResponse<JobDocument> response = client.search(s -> s
                .index(indexAlias)
                .size(limit)
                .knn(k -> k
                        .field("embedding")
                        .queryVector(finalVector)
                        .k(limit)
                        .numCandidates(limit * 3)
                        .filter(filterClauses)
                ),
                JobDocument.class
        );

        return mapHitsToJobCards(response);
    }

    private List<JobCardDto> mapHitsToJobCards(SearchResponse<JobDocument> response) {
        List<JobCardDto> list = new ArrayList<>();
        for (Hit<JobDocument> hit : response.hits().hits()) {
            JobDocument doc = hit.source();
            if (doc != null) {
                list.add(JobCardDto.builder()
                        .id(doc.getId())
                        .title(doc.getTitle())
                        .company(doc.getCompany())
                        .description(doc.getDescription())
                        .skills(doc.getSkills())
                        .location(doc.getLocation())
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
        return list;
    }
}
