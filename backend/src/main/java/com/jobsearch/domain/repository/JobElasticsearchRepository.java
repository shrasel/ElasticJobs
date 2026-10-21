package com.jobsearch.domain.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.jobsearch.domain.model.JobDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JobElasticsearchRepository {

    private final ElasticsearchClient client;

    @Value("${elasticsearch.index-alias:jobs}")
    private String indexAlias;

    public void save(JobDocument job) throws IOException {
        client.index(i -> i
                .index(indexAlias)
                .id(job.getId())
                .document(job)
        );
    }

    public void saveAll(List<JobDocument> jobs) throws IOException {
        if (jobs == null || jobs.isEmpty()) return;

        BulkRequest.Builder br = new BulkRequest.Builder();
        for (JobDocument job : jobs) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index(indexAlias)
                            .id(job.getId())
                            .document(job)
                    )
            );
        }

        BulkResponse response = client.bulk(br.build());
        if (response.errors()) {
            log.error("Bulk indexing encountered errors");
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    log.error("Failed to index doc {}: {}", item.id(), item.error().reason());
                }
            }
        } else {
            log.info("Successfully bulk indexed {} job documents into [{}]", jobs.size(), indexAlias);
        }
    }

    public Optional<JobDocument> findById(String id) throws IOException {
        GetResponse<JobDocument> response = client.get(g -> g
                .index(indexAlias)
                .id(id),
                JobDocument.class
        );
        return response.found() ? Optional.ofNullable(response.source()) : Optional.empty();
    }

    public boolean deleteById(String id) throws IOException {
        var resp = client.delete(d -> d.index(indexAlias).id(id));
        return "deleted".equalsIgnoreCase(resp.result().jsonValue());
    }
}
