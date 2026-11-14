# Backend progress report — Person 1

Status date: 2026-11-14
Branch: `dev/person1-backend-ai`

## 1. Project objective

CV Analyzer is a local-first IT job recommendation platform. It imports and normalizes job offers, extracts structured candidate information from PDF/DOCX CVs, and ranks eligible jobs using multilingual semantic embeddings plus explainable skill and role evidence.

The implemented backend is synchronous and does not persist uploaded CVs. Angular is the next client layer; the browser must call Spring Boot rather than FastAPI directly.

## 2. Implemented architecture

```text
Angular (not generated yet)
        |
        | public REST
        v
Spring Boot 3.5 / Java 21 ---- MongoDB 8 (`jobs`)
        |
        | internal HTTP/1.1
        v
FastAPI / Python 3.12 ---- cached multilingual MiniLM on CPU
```

- Spring Boot owns public validation, CV parsing/orchestration, job querying, error contracts, and authoritative job metadata.
- MongoDB stores normalized jobs, classification and enrichment metadata. It does not store CVs or embedding vectors.
- FastAPI owns semantic embeddings, exact skill normalization, role compatibility, hybrid scoring, cache metrics, and ranking.
- MiniLM is `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`, producing 384-dimensional normalized vectors.
- Docker Compose runs MongoDB only, bound to Windows loopback port `27018` with persistent volume `mongodb_data`.

Primary entry points are `backend/src/main/java/com/cvanalyzer/backend/CvAnalyzerApplication.java` and `ai-service/app/main.py`.

## 3. Job ingestion and normalization

Implemented provider adapters:

- Arbeitnow: `ingestion/arbeitnow/`
- Adzuna: `ingestion/adzuna/`
- La bonne alternance: `ingestion/labonnealternance/`

All external ingestion routes exist only under the Spring `local` profile and return `403 INGESTION_DISABLED` unless `JOB_INGESTION_ENABLED=true` was intentionally set before startup. Request budgets and page limits reduce accidental quota use.

Provider DTOs are mapped into `JobDocument` with source/external identity, title, company, description, location, employment types, URL, timestamps, status, and provenance metadata. Persistence is idempotent:

- Unique compound MongoDB index: `(source, externalId)`.
- Existing content is compared before updating.
- Unchanged jobs are skipped rather than rewritten.
- Invalid external records are counted as failures without aborting the whole batch.

`ItJobClassifier` applies deterministic English/French/German IT evidence. It retains `IT_ONLY`, `NON_IT`, `AMBIGUOUS`, and unclassified behavior through `JobRelevanceFilter`. Generic terms alone do not make a job IT.

Implemented MongoDB indexes are declared in `model/JobDocument.java`:

- `source_external_id_unique`
- `status_fetched_at`
- `status_it_published_at`
- single-field `publishedAt`

## 4. Offline job enrichment

`LocalJobEnrichmentService` performs offline deterministic enrichment—no paid service or provider call:

- `DescriptionCleaningService` converts provider HTML/noise into cleaned text.
- `TechnicalSkillExtractor` extracts supported skills with evidence sources.
- Required skills and skills to be acquired remain separate.
- `LocationNormalizationService` adds city, region, country, work mode, and confidence when supported.
- Enrichment stores a version and timestamp so outdated records can be identified.

`POST /api/dev/enrichment/jobs` defaults to dry-run, and `GET /api/dev/enrichment/audit` reports source quality and duplicate indicators. These local developer endpoints are not intended for the Angular user interface.

## 5. CV parsing

Public endpoint: `POST /api/cv/parse` with multipart field `file`.

Implemented parser version: `cv-parser-1.3`.

The parser supports:

- Selectable-text PDF using PDFBox and coordinate-aware `LayoutAwarePdfTextExtractor`.
- DOCX using Apache POI, with ZIP bomb limits and macro rejection.
- File magic/extension validation, 5 MiB limit, 100-page PDF limit, and 500,000 extracted-character limit.
- English/French section recognition for profile, experience, education, projects, skills, languages, certifications, and volunteering boundaries.
- Structured target role, summary, technical skills, experience, projects, education, and languages.
- Personal-data redaction before semantic profile composition.
- Parser warnings when a field cannot be reliably extracted.

The CV is processed in memory; the implementation has no CV MongoDB collection, CV ID, retention workflow, or download endpoint.

Known parser limitations:

- Scanned/image-only PDFs require OCR, which is not implemented.
- Highly decorative layouts can still produce incorrect reading order or field assignments.
- Some dates, organization names, and language levels remain heuristic.
- The parser uses `null`/warnings rather than inventing unsupported information.

## 6. AI recommendation engine

Public endpoint: `POST /api/recommendations/from-cv?topK=10`.

Flow:

1. Spring validates and parses the CV.
2. MongoDB jobs are read and filtered to `ACTIVE` jobs not explicitly classified as non-IT.
3. Spring sends sanitized semantic text, technical skills, target role, and 225 eligible job candidates to internal FastAPI `POST /match`.
4. FastAPI embeds the CV, reuses cached job embeddings, computes evidence, ranks results, and returns job IDs plus score components.
5. Spring resolves IDs against authoritative MongoDB documents and returns titles, companies, locations, sources, URLs, scores, skills, and explanations.

Default hybrid score:

```text
0.60 × semanticScore + 0.30 × skillScore + 0.10 × roleScore
- bounded explicit-role-mismatch penalty
```

- Semantic score is normalized cosine similarity on multilingual MiniLM vectors.
- Skill score is exact set overlap after accent/case/punctuation and known-alias normalization. Missing job skills are reported only from explicit job evidence. Skill-less jobs receive neutral 50.
- Role score uses transparent multilingual title families and related-family compatibility, with token fallback.
- The mismatch penalty is limited to the existing 10% role weight and applies only to explicit unrelated scientific, business-operations, or industrial title families.
- Scores are ranking signals, not hiring probabilities.

Spring uses a reusable Java HTTP client forced to HTTP/1.1 because Uvicorn rejected the JDK's clear-text HTTP/2 upgrade. The AI read timeout is 60 seconds to cover cold model startup.

## 7. Matching optimization

FastAPI maintains a bounded process-local LRU cache for job vectors:

```text
SHA-256(model identity, preprocessing version, job ID, normalized job text)
```

- Cold misses are encoded as one batch.
- Unchanged jobs reuse immutable float32 vectors.
- Changed text/model/preprocessor keys trigger recomputation and stale-entry invalidation.
- Cache access and miss generation are thread-safe.
- Default maximum is 5,000 entries.
- Health diagnostics expose hits, misses, hit rate, invalidations, evictions, embedding time, vector bytes, and last-match timings without CV content.

Measured with 225 eligible jobs:

- Cold instrumented end-to-end request: 22.32 seconds.
- Warm requests: 0.39–0.51 seconds, down from approximately 8.9 seconds.
- First request: 225 computed job vectors.
- Next three requests: 675 cache hits and zero additional real-job vectors.
- Raw cached vector memory: 345,600 bytes.
- Concurrent warm requests completed in 0.42/0.65 seconds with results identical to sequential requests.

Role optimization preserved CV-09's Data Engineer rank 1, moved CV-04 from a doctoral sensor role to Data Analyst at rank 1, and moved CV-07 Data Engineer from rank 10 to rank 2 while removing explicit scientific/business mismatches from its top ten.

## 8. Implemented endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/actuator/health` | Spring/MongoDB health |
| POST | `/api/cv/parse` | Synchronous PDF/DOCX parsing |
| GET | `/api/jobs` | Active job search, filtering and pagination |
| GET | `/api/jobs/{jobId}` | Job details and enrichment evidence |
| POST | `/api/recommendations/from-cv` | Parse uploaded CV and return ranked jobs |
| GET | `/api/dev/enrichment/audit` | Local data-quality audit |
| POST | `/api/dev/enrichment/jobs` | Local dry-run/execute enrichment |
| POST | `/api/dev/ingestion/arbeitnow` | Guarded Arbeitnow ingestion |
| POST | `/api/dev/ingestion/adzuna` | Guarded Adzuna ingestion |
| POST | `/api/dev/ingestion/la-bonne-alternance` | Guarded LBA ingestion |
| POST | `/api/dev/ingestion/classify-existing` | Offline reclassification |

Internal FastAPI endpoints:

- `GET /health`
- `POST /match`

Every Spring error uses `application/problem+json` with timestamp, HTTP status, stable code, safe message, request path, field errors, and trace ID.

## 9. Verification

Latest automated results:

- Python: 18 passed, 0 failed; one dependency deprecation warning.
- Spring Boot: 92 passed, 0 failures/errors/skips; Maven build success on Java 21.
- MongoDB: healthy, 391 jobs, 225 eligible recommendation candidates.
- Three anonymous real CVs: HTTP 200, ten sorted recommendations each.
- All 30 final job IDs existed; scores were 0–100; matched/missing skills were evidence-backed; explanations were consistent; no exact duplicate or contact-data pattern appeared.
- AI unavailable path: Spring returned `503 AI_SERVICE_UNAVAILABLE` with trace ID.
- No external provider call or database mutation was used during matching validation.

## 10. Known limitations and technical debt

- No authentication, authorization, ownership, or production CORS policy.
- No persisted CV/profile/recommendation workflow; review and recommendations require uploading the same `File` twice.
- Cache is process-local and cold after restart; each multi-worker process would own a copy.
- Stored provider text includes replacement characters/mojibake, weakening some title aliases.
- No human-labelled relevance dataset; role labels are provisional transparent rules.
- Seniority, required experience, and language requirements are not part of the match DTO.
- Spring currently reads all jobs before filtering; acceptable for 391 records, not a large production dataset.
- No OCR, CI workflow, production deployment, rate limiting, audit ownership, or retention policy.
- `docs/api-contract.yaml` remains an older proposed asynchronous design and is not the implemented API source of truth.

See `milestone-3b3-optimization-report.md` for detailed measurements and `frontend-end-to-end-implementation-plan.md` for the exact frontend contract.
