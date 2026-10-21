package com.jobsearch.util;

import com.jobsearch.domain.model.*;
import com.jobsearch.domain.repository.JobElasticsearchRepository;
import com.jobsearch.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SampleDataSeeder {

    private final JobElasticsearchRepository repository;
    private final EmbeddingService embeddingService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            seedJobs();
        } catch (Exception e) {
            log.error("Failed to seed sample jobs on startup: {}", e.getMessage(), e);
        }
    }

    public int seedJobs() throws Exception {
        log.info("Starting seed data ingestion into Elasticsearch...");
        List<JobDocument> jobs = createSampleJobs();

        // Compute dense embeddings for all jobs
        for (JobDocument job : jobs) {
            String semanticText = job.getTitle() + " at " + (job.getCompany() != null ? job.getCompany().getName() : "")
                    + ". " + (job.getDescription() != null ? job.getDescription() : "");
            job.setEmbedding(embeddingService.generateEmbedding(semanticText));
        }

        repository.saveAll(jobs);
        log.info("Completed seeding {} job documents into Elasticsearch.", jobs.size());
        return jobs.size();
    }

    private List<JobDocument> createSampleJobs() {
        List<JobDocument> list = new ArrayList<>();
        Instant now = Instant.now();

        // 1. Plano, TX - Healthcare Mobile (Target for Semantic "building mobile apps for hospitals")
        list.add(JobDocument.builder()
                .id("job-001")
                .title("iOS Healthcare Engineer")
                .company(Company.builder().id("comp-01").name("CarePoint Health").website("https://carepoint.io").build())
                .description("Build native Swift iOS applications for hospital doctors, patient bedside monitoring, and clinical workflows. Integrate with HL7 and FHIR EHR systems.")
                .skills(List.of(
                        Skill.builder().name("Swift").category("LANGUAGE").build(),
                        Skill.builder().name("iOS").category("FRAMEWORK").build(),
                        Skill.builder().name("HIPAA").category("COMPLIANCE").build()
                ))
                .location(Location.builder().city("Plano").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(33.0198).lon(-96.6989).build()).build())
                .workplaceType("HYBRID")
                .employmentType("FULL_TIME")
                .experienceLevel("SENIOR")
                .salary(Salary.builder().min(145000).max(175000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(12, ChronoUnit.HOURS)) // Last 24h
                .isActive(true)
                .build());

        // 2. Dallas, TX - React Native Clinical Developer (Target for Semantic "building mobile apps for hospitals")
        list.add(JobDocument.builder()
                .id("job-002")
                .title("React Native Clinical Developer")
                .company(Company.builder().id("comp-02").name("MedTech Dynamics").website("https://medtechdynamics.com").build())
                .description("Design and develop cross-platform mobile apps for clinical healthcare providers, nurses, and medical centers in North Texas.")
                .skills(List.of(
                        Skill.builder().name("React Native").category("FRAMEWORK").build(),
                        Skill.builder().name("TypeScript").category("LANGUAGE").build(),
                        Skill.builder().name("Mobile").category("TOOL").build()
                ))
                .location(Location.builder().city("Dallas").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(32.7767).lon(-96.7970).build()).build())
                .workplaceType("HYBRID")
                .employmentType("FULL_TIME")
                .experienceLevel("MID")
                .salary(Salary.builder().min(125000).max(150000).currency("USD").period("ANNUAL").build())
                .visaSponsored(false)
                .postedAt(now.minus(2, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 3. Austin, TX - Target for Geo-Radius check (Distance ~190 miles from Plano -> should NOT match within 25 miles!)
        list.add(JobDocument.builder()
                .id("job-003")
                .title("Mobile Application Architect")
                .company(Company.builder().id("comp-03").name("Austin Digital Health").website("https://austindigital.com").build())
                .description("Architect scalable patient-facing Android and iOS solutions.")
                .skills(List.of(
                        Skill.builder().name("Kotlin").category("LANGUAGE").build(),
                        Skill.builder().name("Swift").category("LANGUAGE").build()
                ))
                .location(Location.builder().city("Austin").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(30.2672).lon(-97.7431).build()).build())
                .workplaceType("ON_SITE")
                .employmentType("FULL_TIME")
                .experienceLevel("LEAD")
                .salary(Salary.builder().min(160000).max(190000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(4, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 4. Senior React & TypeScript Architect (Target for "react OR vue -junior")
        list.add(JobDocument.builder()
                .id("job-004")
                .title("Senior React Engineer")
                .company(Company.builder().id("comp-04").name("CloudScale Innovations").build())
                .description("Lead frontend architecture building performant microfrontends with React, Next.js, and TypeScript.")
                .skills(List.of(
                        Skill.builder().name("React").category("FRAMEWORK").build(),
                        Skill.builder().name("TypeScript").category("LANGUAGE").build(),
                        Skill.builder().name("GraphQL").category("TOOL").build()
                ))
                .location(Location.builder().city("San Francisco").state("CA").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(37.7749).lon(-122.4194).build()).build())
                .workplaceType("REMOTE")
                .employmentType("FULL_TIME")
                .experienceLevel("SENIOR")
                .salary(Salary.builder().min(170000).max(210000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(1, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 5. Staff Vue.js Frontend Engineer (Target for "react OR vue -junior")
        list.add(JobDocument.builder()
                .id("job-005")
                .title("Staff Vue.js Frontend Engineer")
                .company(Company.builder().id("comp-05").name("FinTech Global").build())
                .description("Spearhead global financial dashboards with Vue 3, Pinia, and Vite.")
                .skills(List.of(
                        Skill.builder().name("Vue").category("FRAMEWORK").build(),
                        Skill.builder().name("JavaScript").category("LANGUAGE").build()
                ))
                .location(Location.builder().city("New York").state("NY").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(40.7128).lon(-74.0060).build()).build())
                .workplaceType("REMOTE")
                .employmentType("FULL_TIME")
                .experienceLevel("LEAD")
                .salary(Salary.builder().min(185000).max(230000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(5, ChronoUnit.HOURS))
                .isActive(true)
                .build());

        // 6. Junior React Developer (MUST be filtered out when query is "react OR vue -junior")
        list.add(JobDocument.builder()
                .id("job-006")
                .title("Junior React Developer")
                .company(Company.builder().id("comp-06").name("StartupLaunch").build())
                .description("Entry-level software engineer assisting in frontend development.")
                .skills(List.of(
                        Skill.builder().name("React").category("FRAMEWORK").build(),
                        Skill.builder().name("CSS").category("LANGUAGE").build()
                ))
                .location(Location.builder().city("Plano").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(33.0198).lon(-96.6989).build()).build())
                .workplaceType("ON_SITE")
                .employmentType("FULL_TIME")
                .experienceLevel("ENTRY")
                .salary(Salary.builder().min(65000).max(80000).currency("USD").period("ANNUAL").build())
                .visaSponsored(false)
                .postedAt(now.minus(3, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 7. Product Manager (Exact title match for "product manager")
        list.add(JobDocument.builder()
                .id("job-007")
                .title("Product Manager")
                .company(Company.builder().id("comp-07").name("SaaS Dynamics").build())
                .description("Manage roadmap, define customer user stories, and partner with engineering to deliver enterprise B2B SaaS features.")
                .skills(List.of(
                        Skill.builder().name("Product Strategy").category("DOMAIN").build(),
                        Skill.builder().name("Agile").category("PROCESS").build()
                ))
                .location(Location.builder().city("Chicago").state("IL").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(41.8781).lon(-87.6298).build()).build())
                .workplaceType("REMOTE")
                .employmentType("FULL_TIME")
                .experienceLevel("MID")
                .salary(Salary.builder().min(130000).max(155000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(18, ChronoUnit.HOURS))
                .isActive(true)
                .build());

        // 8. Software Engineer (Target for typo "softwre enginer")
        list.add(JobDocument.builder()
                .id("job-008")
                .title("Software Engineer - Core Services")
                .company(Company.builder().id("comp-08").name("OmniCorp Systems").build())
                .description("Build highly scalable distributed backend APIs in Java and Spring Boot.")
                .skills(List.of(
                        Skill.builder().name("Java").category("LANGUAGE").build(),
                        Skill.builder().name("Spring Boot").category("FRAMEWORK").build(),
                        Skill.builder().name("PostgreSQL").category("DATABASE").build()
                ))
                .location(Location.builder().city("Plano").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(33.0198).lon(-96.6989).build()).build())
                .workplaceType("HYBRID")
                .employmentType("FULL_TIME")
                .experienceLevel("MID")
                .salary(Salary.builder().min(115000).max(135000).currency("USD").period("ANNUAL").build())
                .visaSponsored(false)
                .postedAt(now.minus(6, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 9. Kubernetes Platform Engineer (Target for typo "kubernets" & synonym "k8s")
        list.add(JobDocument.builder()
                .id("job-009")
                .title("Kubernetes Platform Engineer")
                .company(Company.builder().id("comp-09").name("Datadog Partner Network").build())
                .description("Maintain large-scale Kubernetes clusters on AWS and Google Cloud Platform. Setup Datadog telemetry and Prometheus.")
                .skills(List.of(
                        Skill.builder().name("Kubernetes").category("CLOUD").build(),
                        Skill.builder().name("Docker").category("TOOL").build(),
                        Skill.builder().name("AWS").category("CLOUD").build()
                ))
                .location(Location.builder().city("Seattle").state("WA").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(47.6062).lon(-122.3321).build()).build())
                .workplaceType("REMOTE")
                .employmentType("FULL_TIME")
                .experienceLevel("SENIOR")
                .salary(Salary.builder().min(165000).max(195000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(1, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 10. Data Scientist (Target for autocomplete "dat...")
        list.add(JobDocument.builder()
                .id("job-010")
                .title("Data Scientist - Machine Learning")
                .company(Company.builder().id("comp-10").name("DataRobotix").build())
                .description("Build predictive ML models and deep learning pipelines with Python and PyTorch.")
                .skills(List.of(
                        Skill.builder().name("Python").category("LANGUAGE").build(),
                        Skill.builder().name("Machine Learning").category("DOMAIN").build(),
                        Skill.builder().name("Databricks").category("TOOL").build()
                ))
                .location(Location.builder().city("San Jose").state("CA").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(37.3382).lon(-121.8863).build()).build())
                .workplaceType("REMOTE")
                .employmentType("FULL_TIME")
                .experienceLevel("SENIOR")
                .salary(Salary.builder().min(175000).max(220000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(3, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 11. Data Analyst (Target for autocomplete "dat...")
        list.add(JobDocument.builder()
                .id("job-011")
                .title("Senior Data Analyst")
                .company(Company.builder().id("comp-11").name("Datadog").build())
                .description("Extract strategic business insights using SQL, Tableau, and Databricks analytics.")
                .skills(List.of(
                        Skill.builder().name("SQL").category("LANGUAGE").build(),
                        Skill.builder().name("Tableau").category("TOOL").build(),
                        Skill.builder().name("Databricks").category("TOOL").build()
                ))
                .location(Location.builder().city("New York").state("NY").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(40.7128).lon(-74.0060).build()).build())
                .workplaceType("HYBRID")
                .employmentType("FULL_TIME")
                .experienceLevel("MID")
                .salary(Salary.builder().min(110000).max(135000).currency("USD").period("ANNUAL").build())
                .visaSponsored(false)
                .postedAt(now.minus(8, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 12. Data Engineer (Target for autocomplete "dat...")
        list.add(JobDocument.builder()
                .id("job-012")
                .title("Principal Data Engineer")
                .company(Company.builder().id("comp-12").name("Enterprise Data Systems").build())
                .description("Build petabyte-scale real-time data pipelines using Apache Spark, Kafka, and Snowflake.")
                .skills(List.of(
                        Skill.builder().name("Python").category("LANGUAGE").build(),
                        Skill.builder().name("Spark").category("FRAMEWORK").build(),
                        Skill.builder().name("Kafka").category("TOOL").build()
                ))
                .location(Location.builder().city("Plano").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(33.0198).lon(-96.6989).build()).build())
                .workplaceType("HYBRID")
                .employmentType("FULL_TIME")
                .experienceLevel("LEAD")
                .salary(Salary.builder().min(170000).max(205000).currency("USD").period("ANNUAL").build())
                .visaSponsored(true)
                .postedAt(now.minus(2, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 13. Audit Manager - CPA (Target for Hybrid Search "CPA license audit manager")
        list.add(JobDocument.builder()
                .id("job-013")
                .title("Audit Manager")
                .company(Company.builder().id("comp-13").name("Grant & Sterling Accounting").build())
                .description("Lead financial audits, SOX compliance assessments, and internal controls reviews. Active CPA license required.")
                .skills(List.of(
                        Skill.builder().name("CPA").category("CERTIFICATION").build(),
                        Skill.builder().name("Audit").category("DOMAIN").build(),
                        Skill.builder().name("SOX").category("COMPLIANCE").build()
                ))
                .location(Location.builder().city("Dallas").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(32.7767).lon(-96.7970).build()).build())
                .workplaceType("HYBRID")
                .employmentType("FULL_TIME")
                .experienceLevel("SENIOR")
                .salary(Salary.builder().min(135000).max(160000).currency("USD").period("ANNUAL").build())
                .visaSponsored(false)
                .postedAt(now.minus(5, ChronoUnit.DAYS))
                .isActive(true)
                .build());

        // 14. Senior Security Engineer - Security Clearance
        list.add(JobDocument.builder()
                .id("job-014")
                .title("Senior Security Clearance Engineer")
                .company(Company.builder().id("comp-14").name("DefenseTech Aerospace").build())
                .description("Perform threat modeling and penetration testing on aerospace avionics. Active Top Secret Security Clearance required.")
                .skills(List.of(
                        Skill.builder().name("Security Clearance").category("REQUIREMENT").build(),
                        Skill.builder().name("Cybersecurity").category("DOMAIN").build()
                ))
                .location(Location.builder().city("Plano").state("TX").country("USA")
                        .coordinates(Location.GeoCoordinates.builder().lat(33.0198).lon(-96.6989).build()).build())
                .workplaceType("ON_SITE")
                .employmentType("FULL_TIME")
                .experienceLevel("SENIOR")
                .salary(Salary.builder().min(150000).max(180000).currency("USD").period("ANNUAL").build())
                .visaSponsored(false)
                .postedAt(now.minus(12, ChronoUnit.HOURS))
                .isActive(true)
                .build());

        return list;
    }
}
