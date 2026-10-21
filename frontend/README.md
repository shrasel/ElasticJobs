# ElasticJobs Frontend

A high-performance, mobile-first web interface built for **Pure Elasticsearch 8.x** featuring Hybrid RRF, Semantic Vector similarity, Typo-Tolerant search, and Geo-Radius filtering.

## 🌟 Highlights
- **Design System**: Modern, clean Light Theme with high contrast typography and responsive micro-interactions.
- **Pure JavaScript & CSS**: Zero heavy runtime frameworks; instant first-contentful paint (<50ms).
- **Search Integrations**:
  - Instant Autocomplete with sub-20ms feedback
  - Mode Switcher (Hybrid RRF, Semantic Vector, Weighted Keyword)
  - Interactive Multi-Faceted dynamic aggregations (Experience, Workplace, Salary slider)
  - Location Distance Radius with Geolocation ("Near Me")
  - Resume Parsing & Semantic Job Recommendation Modal

## 🚀 Running Locally
```bash
# Direct access via Spring Boot (default):
open http://localhost:8090/

# Standalone development:
npm run dev

# Production Docker container:
docker build -t elasticjobs-frontend:latest .
docker run -p 3000:80 elasticjobs-frontend:latest
```

## 🐳 Production Container (NGINX)
The production image uses `nginx:alpine` configured with:
- Gzip compression on all static text assets
- One-year immutable cache headers for versioned CSS/JS
- Reverse proxy pass for `/api/` upstreaming to Spring Boot
