package com.jobsearch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobSearchOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pure Elasticsearch Job Search Platform API")
                        .description("High-performance Job Search Engine supporting Weighted Lexical, Fuzzy, Instant Autocomplete, Geo-Radius, Multi-Faceted Filtering, Semantic Vector, Hybrid RRF, and Resume Matching powered 100% by Elasticsearch 8.x.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Engineering Team").email("eng@jobsearch.io"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
