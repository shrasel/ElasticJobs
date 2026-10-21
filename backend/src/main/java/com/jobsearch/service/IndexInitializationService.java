package com.jobsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexInitializationService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${elasticsearch.index-name:jobs_v1}")
    private String indexName;

    @Value("${elasticsearch.index-alias:jobs}")
    private String indexAlias;

    @Value("classpath:elasticsearch/jobs-index-mapping.json")
    private Resource mappingResource;

    @PostConstruct
    public void initializeIndex() {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                    .value();

            if (!exists) {
                log.info("Elasticsearch index [{}] does not exist. Creating schema...", indexName);
                try (InputStream is = mappingResource.getInputStream()) {
                    CreateIndexRequest request = CreateIndexRequest.of(c -> c
                            .index(indexName)
                            .withJson(is)
                    );
                    elasticsearchClient.indices().create(request);
                    log.info("Successfully created index [{}]", indexName);
                }

                // Create alias
                elasticsearchClient.indices().updateAliases(UpdateAliasesRequest.of(u -> u
                        .actions(a -> a.add(add -> add.index(indexName).alias(indexAlias)))
                ));
                log.info("Successfully pointed alias [{}] to index [{}]", indexAlias, indexName);
            } else {
                log.info("Elasticsearch index [{}] already exists.", indexName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Elasticsearch index [{}]: {}", indexName, e.getMessage(), e);
            throw new RuntimeException("Index initialization failed", e);
        }
    }

    public void recreateIndex() {
        try {
            log.warn("Recreating index [{}]...", indexName);
            boolean exists = elasticsearchClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
            if (exists) {
                elasticsearchClient.indices().delete(d -> d.index(indexName));
                log.info("Deleted existing index [{}]", indexName);
            }
            initializeIndex();
        } catch (Exception e) {
            log.error("Failed to recreate index: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to recreate index", e);
        }
    }
}
