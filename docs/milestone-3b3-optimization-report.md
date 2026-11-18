# Milestone 3B.3 — matching optimization report

Validation date: 2026-11-14
Branch: `dev/person1-backend-ai`

## Outcome

The existing synchronous Spring Boot → FastAPI → MiniLM architecture was retained. FastAPI now caches unchanged job embeddings in bounded process memory, applies multilingual role-family compatibility, exposes cache/timing diagnostics, and logs non-sensitive phase timings. Spring logs parser, MongoDB, candidate preparation, AI round-trip, response, and total durations.

No MongoDB document, collection, index, or Docker volume was changed. No job-provider API was called. The browser/FastAPI boundary and response DTO fields remain compatible.

## Implemented changes

- Thread-safe, lazy MiniLM initialization and serialized model inference.
- Bounded LRU job-vector cache; default maximum 5,000 entries.
- SHA-256 cache identity over model identity, preprocessing version, job ID, and normalized embedding input.
- Batch embedding of cache misses and zero recomputation for unchanged hits.
- Automatic invalidation when the same job ID's title/description/categories input changes.
- Model/preprocessing version isolation, explicit deletion hook, immutable cached vectors, deterministic ordering, and bounded eviction.
- Cache diagnostics: hits, misses, hit rate, embeddings computed, generation seconds, invalidations, evictions, entries, and vector bytes.
- English/French role families, common title aliases, and preserved Arabic aliases for core families.
- Related-family compatibility for data, BI, software, cloud/DevOps, systems/network, database, security, mobile, and QA roles.
- Explicit scientific, business-operations, and industrial-role mismatch detection. A mismatch applies a bounded penalty equal to the existing 10% role component; it never excludes a job.
- Skill alias additions for Azure, Spring Boot, Power BI, scikit-learn, SQL Server, and .NET.
- Deterministic explanations now state role-family compatibility and explicit title-family mismatch where applicable.
- Synthetic/provisional role fixture derived from observed real eligible titles. Its labels are rules for regression, not human relevance ground truth.

## Cache architecture

The cache is process-local because the service currently has one local FastAPI process and only 225 eligible jobs. MongoDB persistence would add schema/migration/write complexity and was not justified. Redis/vector infrastructure would be disproportionate.

Embedding input remains the normalized concatenation of:

```text
job title + cleaned/fallback description + source categories/tags
```

Skills remain a separate exact normalized comparison and are not duplicated in semantic text.

Cache key:

```text
SHA-256(model identity, preprocessing version, job ID, normalized embedding input)
```

Behavior:

- Unchanged job: hit, vector reused.
- New job: one cache miss and one embedding.
- Changed relevant text: old key for that job ID is removed and one replacement vector is generated.
- Changed model/preprocessor: key changes and vectors are recomputed.
- Deleted job: it is immediately absent from request/ranking; the bounded LRU entry is harmless and can also be explicitly removed through the cache hook.
- Concurrent requests: one cache lock owns miss generation, so the same missing job is embedded once; waiting requests reuse it.
- Restart/multiple worker: each process starts with an empty independent cache. This is an accepted V1 limitation.

At 225 jobs × 384 dimensions × four bytes, vectors consume 345,600 bytes (about 338 KiB), excluding small Python dictionary/array overhead. The 5,000-entry bound caps raw vector storage near 7.3 MiB.

## Real cache validation

With all 225 eligible MongoDB jobs:

| Step | Hits | Misses | Embeddings computed | Result |
|---|---:|---:|---:|---|
| First CV, cold cache | 0 | 225 | 225 | All vectors generated in one batch |
| Same CV, warm cache | 225 | 0 | 0 | Identical recommendation response |
| Different CV-07 | 225 | 0 | 0 | Cache reused |
| Different CV-09 | 225 | 0 | 0 | Cache reused |
| Four-request total | 675 | 225 | 225 | 75% hit rate |

A synthetic job sent only to FastAPI was matched twice unchanged and once with modified text. The second call hit the cache; the change produced exactly one invalidation and one new vector. MongoDB was not involved.

Two concurrent real-CV requests returned HTTP 200 in 0.42 and 0.65 seconds. Both responses were identical to their sequential equivalents. Cache totals rose by 450 hits and zero real-job computations.

## Recommendation-quality method

Quality was evaluated against the same three real, local CVs and unchanged 225-job candidate set used in Milestone 3B.2. Reports use anonymous identifiers only. A role is objectively “compatible” here when the documented role-family rules produce at least 55/100. “Explicit mismatch” means a recognized IT target and a title belonging only to a recognized scientific, business-operations, or industrial family.

These are provisional title-family labels. They do not measure overall hiring suitability, and Precision/NDCG are intentionally not claimed without independent human labels.

| CV | Before | After | Objective role-family change |
|---|---|---|---|
| CV-04 | Doctoral multi-sensor role ranked 1; Data Analyst ranked 2 | Data Analyst ranked 1; doctoral role left top 10; Data Engineer entered rank 5 | Compatible top-10 roles 4 → 7; explicit mismatches 1 → 0 |
| CV-07 | Data Engineer rank 10; bioengineering/lab/business/science dominated ranks 2-9 | Data Engineer rank 2; no explicitly mismatched scientific/business role in top 10 | Compatible roles 2 → 7; explicit mismatches 8 → 0 |
| CV-09 | Data Engineer rank 1 | Data Engineer remains rank 1; systems, software, data, and DevOps dominate | Compatible roles 5 → 8; explicit mismatches 1 → 0 |

All 30 final job IDs resolved against MongoDB. Scores remained within 0-100 and descending. There were no exact title/company duplicates. Every matched/missing skill was supported by parsed-CV and stored-job evidence; explanations agreed with the lists and contained no email/phone pattern. No skill-less job appeared in these top tens, so neutral 50-point handling is covered by automated tests.

## Performance

Comparable local measurements used the same machine, 225 eligible jobs, real cached MiniLM, local MongoDB, and three CVs. OS/model file-system caching differs between process starts, so the cold improvement must not be attributed solely to application code.

| Metric | Before 3B.3 | After 3B.3 | Observed change |
|---|---:|---:|---:|
| Cold Spring end-to-end request | 50.82 s | 22.32 s | 28.50 s faster in the final instrumented run |
| Identical warm request | about 8.87 s | 0.46 s | about 95% lower |
| Different-CV warm request | 8.86-8.87 s | 0.39-0.50 s | about 94-96% lower |
| Cache hit rate after 4 requests | 0% | 75% | 675 hits / 900 lookups |
| Job embedding computations | 225 per request | 225 once, then 0 | 675 computations avoided over three warm requests |
| Cache raw vector memory | none | 345,600 bytes | bounded and measured |
| FastAPI working set snapshot | about 294 MB | about 489 MB | snapshots are not controlled peak benchmarks; cache itself is only ~338 KiB |
| Recommendation quality | adjacent scientific/business roles high | explicit mismatches removed from all three top tens | provisional role-family evaluation improved |

Final per-stage measurements:

| Stage | Cold | Warm identical CV |
|---|---:|---:|
| Spring CV parse | 287 ms | 91 ms |
| MongoDB `findAll` | 311 ms | 106 ms |
| Eligibility filter + candidate preparation | 2 ms | <1 ms |
| Spring AI HTTP round-trip | 21,687 ms | 251 ms |
| FastAPI total match | 21,516 ms | 220 ms |
| CV/model embedding | 11,383 ms (includes first inference) | 42 ms |
| Job cache lookup / miss handling | includes 9,801 ms job batch | 9 ms, zero embedding |
| Hybrid scoring + ranking | within remaining cold time | 152 ms |
| Spring response assembly | <1 ms | <1 ms |
| Spring measured total | 22,289 ms | 450 ms |
| Client-observed total | 22,322 ms | 458 ms |

The embedder recorded 1.507 seconds for model object construction; first inference initialization is included in the 11.383-second cold CV-embedding stage. Warm Java-to-FastAPI transport/serialization overhead was approximately 31 ms (251 ms round-trip minus 220 ms FastAPI work). Timing data is available from FastAPI `/health` under `embeddingCache.lastMatch` and concise `matching_complete` / `recommendation_complete` logs. No CV text, title list, skill list, or contact field is logged.

## API compatibility

- Spring `POST /api/recommendations/from-cv` and every existing response field are unchanged.
- FastAPI `/match` request/response fields are unchanged.
- `modelVersion` advanced from `hybrid-multilingual-minilm-v1` to `hybrid-multilingual-minilm-v2` to identify the scoring change.
- FastAPI `/health` adds `embeddingCache`; existing `status` and `modelVersion` remain.
- Scores remain 0-100 and results descending. Scores are ranking signals, not hiring probabilities.

## Automated coverage

Python tests cover cold/warm reuse, unchanged results, content invalidation, model/preprocessor versioning, new/deleted/empty inputs, LRU eviction, deterministic keys, concurrent calls, multilingual aliases, explicit mismatch penalties, API diagnostics, and provisional real-title fixtures. Spring tests continue to cover HTTP/1.1, timeouts/errors, DTO mapping, controllers, parsing, repositories, ingestion mocks, enrichment, and recommendation orchestration.

- Python: 18 passed, 0 failed; one Starlette/AnyIO deprecation warning.
- Spring/Maven: 92 passed, 0 failed, 0 errors, 0 skipped; `BUILD SUCCESS` on Java 21.
- Live: three final CVs returned HTTP 200 and ten recommendations; cold/warm, concurrent, invalidation, and AI-unavailable paths were exercised.

## Reproduce locally

```powershell
# MongoDB must already be healthy on localhost:27018. Keep ingestion disabled.

cd C:\CV_analyzer\ai-service
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000

cd C:\CV_analyzer\backend
$env:SPRING_PROFILES_ACTIVE='local'
$env:JOB_INGESTION_ENABLED='false'
.\mvnw.cmd spring-boot:run

curl.exe -F "file=@C:\path\to\local-cv.pdf" `
  "http://localhost:8080/api/recommendations/from-cv?topK=10"

Invoke-RestMethod http://127.0.0.1:8000/health | ConvertTo-Json -Depth 6
```

Tests:

```powershell
cd C:\CV_analyzer\ai-service
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
.\.venv\Scripts\python.exe -m pytest -q

cd C:\CV_analyzer\backend
.\mvnw.cmd clean test
```

## Remaining limitations and risks

- Process-local cache is empty after restart and duplicated across multiple FastAPI workers.
- There is no proactive startup warm-up; first matching request still owns model/job-vector cost.
- Stored job titles contain replacement characters/mojibake, preventing some French titles from entering a role family.
- Role families are transparent rules, not learned relevance labels; human evaluation is still required.
- Seniority, required experience, and languages are not present in the internal match DTO, so they are not scored.
- Narrow job skill lists can still produce 100% skill scores; explanations correctly limit this to identified explicit requirements.
- Full process memory is dominated by Python/PyTorch/model runtime, not cached vectors.
- Spring currently reads all jobs then filters in memory; this is acceptable for 391 records but should become a repository query at larger scale.
- CV parsing warnings still influence semantic input quality.
