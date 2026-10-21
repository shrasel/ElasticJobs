# Enterprise Job Search Platform (Pure Elasticsearch 8.x + Spring Boot 3.3 + Java 21)

A high-performance Job Search Platform engineered **100% on Elasticsearch 8.x** as the primary datastore, vector database, and search engine (zero Postgres, zero external vector DBs).

---

## Architecture Highlights
- **Primary Datastore & Search Engine:** Elasticsearch 8.15.2 (Documents, Inverted Index, BKD-Tree Spatial Index, In-Memory Trie Autocomplete, HNSW Dense Vector Index, Native RRF)
- **Application Framework:** Spring Boot 3.3.4 (Java 21 Virtual Threads / Project Loom)
- **Elasticsearch Client:** Official Modern Client (`co.elastic.clients:elasticsearch-java`)
- **Document & Resume Parser:** Apache Tika 2.9.2
- **Containerization:** Docker Compose / Rancher Desktop

---

## 8 Native Search Capabilities

| # | Feature | Native Elasticsearch Implementation | Example Query / Endpoint |
|---|---|---|---|
| 1 | **Weighted Keyword & Boolean** | `queryString` / `multi_match` with field boosts (`title^4.0`, `skills^2.5`, `company^2.0`, `description^1.0`) | `POST /api/v1/jobs/search` with `"query": "react OR vue -junior"` |
| 2 | **Typo-Tolerant (Fuzzy)** | Levenshtein edit automaton (`fuzziness: "AUTO:3,6"`, `prefix_length: 2`) + Tech synonym graphs | `"query": "softwre enginer"` or `"kubernets"` |
| 3 | **Instant Autocomplete** | `search_as_you_type` n-gram trie field (<20ms response time) | `GET /api/v1/jobs/autocomplete?q=dat&size=10` |
| 4 | **Geo-Radius & Location** | `geo_point` BKD-Tree index, `geo_distance` filter & `_geo_distance` sorting | 25-mile radius around Plano, TX (lat: 33.0198, lon: -96.6989) |
| 5 | **Multi-Faceted Filtering** | Cached bitset `filter` context + dynamic `aggs` (salary, workplace, skills) | Filter by Remote/Hybrid, Min Salary $140k, Visa Sponsored |
| 6 | **Semantic (AI Meaning)** | `dense_vector` (384-dim) with HNSW graph indexing & cosine similarity | `"query": "building mobile apps for hospitals"` |
| 7 | **Hybrid (Keyword + AI)** | Native Elasticsearch Reciprocal Rank Fusion (`rank: { rrf: ... }`) | `"query": "CPA license audit manager"`, `"search_mode": "HYBRID"` |
| 8 | **Resume & Similar Jobs** | Apache Tika PDF/DOCX parser + vector kNN matching & document similarity | `POST /api/v1/jobs/match-resume` and `GET /api/v1/jobs/{id}/similar` |

---

## Quickstart

### 1. Start Infrastructure (Rancher Desktop / Docker)
```bash
docker compose up -d elasticsearch
```

### 2. Build & Run Spring Boot Service
```bash
mvn clean package -DskipTests
java -jar target/es-job-search-service-1.0.0-SNAPSHOT.jar
```
*Note: On startup, `IndexInitializationService` automatically verifies, creates, and aliases the index schema, and `SampleDataSeeder` indexes 14 test jobs.*

### 3. Interactive API Documentation (Swagger)
Open in your browser:
[http://localhost:8090/swagger-ui/index.html](http://localhost:8090/swagger-ui/index.html)

---

## Verification cURL Examples

### 1. Weighted Keyword & Boolean Search (`react OR vue -junior`)
```bash
curl -X POST "http://localhost:8090/api/v1/jobs/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "react OR vue -junior",
    "search_mode": "KEYWORD"
  }'
```

### 2. Typo-Tolerant (Fuzzy) Search (`softwre enginer`)
```bash
curl -X POST "http://localhost:8090/api/v1/jobs/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "softwre enginer",
    "fuzzy_enabled": true,
    "search_mode": "KEYWORD"
  }'
```

### 3. Instant Autocomplete (<20ms)
```bash
curl "http://localhost:8090/api/v1/jobs/autocomplete?q=dat&size=10"
```

### 4. Geo-Radius Search (Within 25 miles of Plano, TX)
```bash
curl -X POST "http://localhost:8090/api/v1/jobs/search" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "geo_radius": {
        "lat": 33.0198,
        "lon": -96.6989,
        "radius_miles": 25.0
      }
    },
    "sort_by": "GEO_DISTANCE"
  }'
```

### 5. Multi-Faceted Filtering & Aggregations
```bash
curl -X POST "http://localhost:8090/api/v1/jobs/search" \
  -H "Content-Type: application/json" \
  -d '{
    "filters": {
      "workplace_types": ["REMOTE", "HYBRID"],
      "min_salary": 140000,
      "visa_sponsored": true,
      "experience_levels": ["SENIOR", "LEAD"]
    }
  }'
```

### 6. Semantic (AI Meaning) Search
```bash
curl -X POST "http://localhost:8090/api/v1/jobs/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "building mobile apps for hospitals",
    "search_mode": "SEMANTIC",
    "size": 5
  }'
```

### 7. Hybrid Search (Lexical + AI with RRF)
```bash
curl -X POST "http://localhost:8090/api/v1/jobs/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "CPA license audit manager",
    "search_mode": "HYBRID",
    "size": 5
  }'
```

### 8a. Resume-to-Job Matching (File Upload)
```bash
curl -X POST "http://localhost:8090/api/v1/jobs/match-resume" \
  -F "file=@test_resume.txt" \
  -F "min_salary=150000" \
  -F "visa_required=true" \
  -F "limit=5"
```

### 8b. Similar Jobs
```bash
curl "http://localhost:8090/api/v1/jobs/job-001/similar?limit=3"
```
