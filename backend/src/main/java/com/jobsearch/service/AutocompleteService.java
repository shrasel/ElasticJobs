package com.jobsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.jobsearch.domain.model.JobDocument;
import com.jobsearch.domain.model.Skill;
import com.jobsearch.web.dto.response.AutocompleteSuggestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutocompleteService {

    private final ElasticsearchClient client;

    @Value("${elasticsearch.index-alias:jobs}")
    private String indexAlias;

    public List<AutocompleteSuggestion> getSuggestions(String prefix, int size) throws IOException {
        if (prefix == null || prefix.trim().length() < 1) {
            return Collections.emptyList();
        }

        String normalizedPrefix = prefix.trim().toLowerCase(Locale.ROOT);

        // Sub-15ms multi_match query against search_as_you_type root and edge n-gram fields
        SearchResponse<JobDocument> response = client.search(s -> s
                .index(indexAlias)
                .size(size * 2)
                .source(src -> src.filter(f -> f.includes("title", "company.name", "skills.name")))
                .query(q -> q
                        .multiMatch(mm -> mm
                                .query(normalizedPrefix)
                                .type(TextQueryType.BoolPrefix)
                                .fields(
                                        "title.suggest^4.0",
                                        "title.suggest._2gram^3.0",
                                        "title.suggest._3gram^2.0",
                                        "company.name.suggest^2.5",
                                        "company.name.suggest._2gram^2.0",
                                        "company.name.suggest._3gram^1.5",
                                        "skills.name.suggest^3.0",
                                        "skills.name.suggest._2gram^2.5",
                                        "skills.name.suggest._3gram^2.0"
                                )
                        )
                ),
                JobDocument.class
        );

        Map<String, AutocompleteSuggestion> suggestionMap = new LinkedHashMap<>();

        for (Hit<JobDocument> hit : response.hits().hits()) {
            JobDocument doc = hit.source();
            if (doc == null) continue;
            double score = hit.score() != null ? hit.score() : 1.0;

            // 1. Check Title match
            if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(normalizedPrefix)) {
                String key = "TITLE:" + doc.getTitle();
                suggestionMap.putIfAbsent(key, new AutocompleteSuggestion(doc.getTitle(), "TITLE", score + 2.0));
            }

            // 2. Check Company match
            if (doc.getCompany() != null && doc.getCompany().getName() != null
                    && doc.getCompany().getName().toLowerCase().contains(normalizedPrefix)) {
                String key = "COMPANY:" + doc.getCompany().getName();
                suggestionMap.putIfAbsent(key, new AutocompleteSuggestion(doc.getCompany().getName(), "COMPANY", score + 1.0));
            }

            // 3. Check Skills match
            if (doc.getSkills() != null) {
                for (Skill skill : doc.getSkills()) {
                    if (skill.getName() != null && skill.getName().toLowerCase().contains(normalizedPrefix)) {
                        String key = "SKILL:" + skill.getName();
                        suggestionMap.putIfAbsent(key, new AutocompleteSuggestion(skill.getName(), "SKILL", score + 1.5));
                    }
                }
            }
        }

        List<AutocompleteSuggestion> list = new ArrayList<>(suggestionMap.values());
        list.sort(Comparator.comparing(AutocompleteSuggestion::getScore).reversed());

        return list.size() > size ? list.subList(0, size) : list;
    }
}
