# Milestone 3B — AI CV-to-job matching

## Architecture

```text
CV upload
  -> Spring POST /api/recommendations/from-cv
  -> existing cv-parser-1.3 (sanitized semantic profile + structured skills)
  -> read active, non-NON_IT jobs from MongoDB collection `jobs`
  -> FastAPI POST /match
       -> one lazy CPU multilingual MiniLM instance
       -> bounded job-embedding cache + batch cache-miss encoding
       -> exact normalized skill comparison + multilingual role-family compatibility
  -> Spring enriches ranked IDs with authoritative job metadata
  -> explainable recommendation response
```

Spring never sends CV contact fields. It sends only `semanticProfile`, parsed technical skills and the optional target role. The AI service does not log request bodies.

## Ranking algorithm

Default formula:

```text
overall = 0.60 * semanticScore + 0.30 * skillScore + 0.10 * roleScore
          - explicitRoleMismatchPenalty
```

- Semantic score: cosine similarity of normalized multilingual sentence embeddings, mapped to 0–100.
- Skill score: exact set overlap after case, punctuation and known-alias normalization. It does not use substring matching. If a job has no explicit skills, this component is neutral (`50`) and no missing skills are claimed.
- Role score: transparent English/French role families and aliases (with core Arabic aliases), related-family scores, then normalized token overlap as fallback. A missing target role is neutral (`50`).
- Explicit mismatch penalty: only a recognized IT target versus a title exclusively in a recognized scientific, business-operations or industrial family. It equals the existing 10% role component and never excludes a job.

The weights are configurable constructor defaults and have not been empirically calibrated. Explanations are deterministic and reference explicit normalized skills, semantic alignment and role-family evidence.

## Model and resources

Model: `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`. It supports English and French, runs on CPU and is substantially smaller than large transformer models. `SentenceTransformerEmbedder` loads it lazily once and reuses it. All job texts are encoded in batches of 32. Local validation runs with `HF_HUB_OFFLINE=1` and `TRANSFORMERS_OFFLINE=1`, so the cached model is used without a network download.

For 391 stored/225 eligible jobs, request-time in-memory scoring is intentionally simpler than a vector database. Job vectors use a process-local LRU cache keyed by SHA-256 over model identity, preprocessing version, job ID and normalized semantic input. Text/model changes miss and replace the stale vector. The default 5,000-entry bound is configurable with `JOB_EMBEDDING_CACHE_MAX_ENTRIES`.

## FastAPI

Requires Python 3.11 or newer. The validated local environment uses Python 3.12.10.

```powershell
cd C:\CV_analyzer\ai-service
.\.venv\Scripts\Activate.ps1
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Endpoints:

- `GET /health` (including non-sensitive cache and last-match timing diagnostics)
- `POST /match`

Example request:

```json
{
  "semanticProfile": "Data engineer experienced with Python and SQL.",
  "skills": ["Python", "PostgreSQL"],
  "targetRole": "Data Engineer",
  "topK": 10,
  "jobs": [{
    "jobId": "job-1",
    "title": "Data Engineer",
    "description": "Build batch data pipelines.",
    "skills": ["Python", "SQL", "Spark"],
    "categories": ["data"]
  }]
}
```

The response contains `modelVersion` and ranked entries with overall, semantic, skill and role scores, matched/missing skills and an explanation.

## Spring Boot integration

Configuration:

```text
AI_SERVICE_URL=http://localhost:8000
AI_CONNECT_TIMEOUT=3s
AI_READ_TIMEOUT=60s
```

Start the backend:

```powershell
cd C:\CV_analyzer\backend
$env:SPRING_PROFILES_ACTIVE='local'
.\mvnw.cmd spring-boot:run
```

Call the integration endpoint:

```powershell
curl.exe -X POST "http://localhost:8080/api/recommendations/from-cv?topK=10" `
  -F "file=@C:\path\to\synthetic-cv.pdf"
```

`topK` defaults to 10 and must be from 1 to 50. Unsupported CVs retain existing CV parser errors. Empty candidate sets return `NO_JOBS_AVAILABLE`; AI connection, timeout and malformed response errors are explicit and never produce fabricated recommendations.

Job candidate mapping uses the actual schema:

- Description: `enrichment.descriptionText`, falling back to `description`.
- Skills: names from `enrichment.requiredSkills`, falling back to `requirements.skills`.
- Categories: `sourceMetadata.categories` plus `sourceMetadata.tags`.
- Only `ACTIVE` jobs not explicitly classified as non-IT are sent.

## Live end-to-end validation (2026-11-14)

The local validation used Docker MongoDB on port `27018`, Spring Boot on port `8080`, FastAPI on port `8000`, and three anonymized real CVs. No ingestion or database writes were performed.

- MongoDB: healthy; 391 total jobs, 225 eligible matching candidates, and 197 eligible candidates with enriched required skills.
- Model: real cached MiniLM checkpoint, 384-dimensional finite embeddings, response model version `hybrid-multilingual-minilm-v1`.
- Model initialization: 12.33 seconds in an isolated offline load-and-encode check.
- Cold Spring end-to-end request: 50.82 seconds. Warm requests: 8.87 and 8.86 seconds.
- Process resources after the three requests: FastAPI about 294 MB working set / 1.64 GB private bytes and 115 CPU-seconds; Spring about 237 MB working set / 296 MB private bytes and 8 CPU-seconds. These are approximate Windows process snapshots, not capacity benchmarks.

| CV | Eligible jobs | Returned | Top-10 score range | Highest-ranked title | Quality observation |
|---|---:|---:|---:|---|---|
| CV-04 | 225 | 10 | 74.46-77.27 | Doctoral multi-sensor platform role | Data vocabulary matched, but the top result is weaker than lower-ranked data analyst/scientist roles. |
| CV-07 | 225 | 10 | 73.85-82.86 | Digital transformation / BI / data / ML consultant | The first result is plausible; several bioengineering, lab automation, and business-operations results rank too highly. |
| CV-09 | 225 | 10 | 73.08-80.10 | Data Engineer | Strongest qualitative result; the remaining top results broadly cover Linux, software, networks, and systems. |

All 30 job IDs resolved through the read-only job API, scores were within 0-100 and sorted descending, and no exact title/company duplicates dominated the top tens. Every reported matched and missing skill was supported by the parsed CV/job evidence used by the matcher. Explanations were consistent with those lists and contained no email address or phone-number pattern. The tested top tens contained no skill-less job, so neutral missing-skill behavior is covered by automated tests rather than this live sample.

Semantic scores varied by about 9-11 points within each top ten, confirming that the real embeddings contribute differentiation. The quality review also shows that semantic similarity and broad skill overlap can over-rank adjacent scientific or business roles. Scores are ranking signals, not hiring probabilities.

### Integration defects found and fixed

1. Java's HTTP client attempted an HTTP/2 clear-text upgrade that Uvicorn rejected. `AiMatchingClient` now explicitly uses HTTP/1.1, with a local protocol regression test.
2. The original 30-second read timeout expired during the first lazy model load. The default is now 60 seconds. A controlled cold restart completed in 50.82 seconds and subsequent requests reused the loaded model.

The stored job titles also contain some pre-existing mojibake (for example, malformed French punctuation). This affects display and semantic input quality but was not changed during integration validation.

## Milestone 3B.3 optimization validation (2026-11-14)

`modelVersion` is now `hybrid-multilingual-minilm-v2`. The same three anonymous CVs and unchanged 225 eligible jobs were used.

- Cold requests: 20.81 seconds in the quality run and 22.32 seconds in the final instrumented run.
- Warm identical request: 0.46-0.51 seconds.
- Different-CV warm requests: 0.39 and 0.50 seconds.
- Cold cache: 225 misses/225 vectors; next three requests: 675 hits/zero new real-job vectors.
- Cache vectors: 345,600 bytes; 75% four-request hit rate.
- Concurrent warm requests: HTTP 200 in 0.42/0.65 seconds, identical to sequential results.
- Synthetic live content change: exactly one invalidation and one replacement vector; no database write.

Quality changes:

- CV-04: Data Analyst moved to rank 1; the doctoral multi-sensor role left the top ten.
- CV-07: Data Engineer moved from rank 10 to rank 2; explicit scientific/business mismatches fell from eight to zero in the top ten.
- CV-09: Data Engineer remained rank 1; compatible title families increased from five to eight in the top ten.

Across the 30 final results, IDs, ordering, score ranges, evidence, explanations, duplicates and contact-data patterns were rechecked successfully. See `docs/milestone-3b3-optimization-report.md` for the cache design, provisional-label methodology, detailed results and reproduction steps.

## Tests

```powershell
cd C:\CV_analyzer\ai-service
$env:PYTHONPATH=(Get-Location).Path
pytest -q

cd C:\CV_analyzer\backend
.\mvnw.cmd clean test
```

Python ranking tests use a deterministic fake embedder and therefore never download the model. Spring tests mock the AI client and use synthetic CV data.

## Known limitations

- Scores and weights need evaluation against human relevance labels.
- Missing skills mean “not identified in the parsed CV,” not proof the candidate lacks them.
- Parser errors can affect ranking input.
- Role relevance uses transparent role families and token fallback rather than a second embedding; human-labelled calibration is still needed.
- Job vectors are cached per FastAPI process and are rebuilt after restart or in each additional worker.
- Stored replacement characters prevent some otherwise recognizable French titles from matching role aliases.
- Scanned CVs still require OCR, which is outside this milestone.
