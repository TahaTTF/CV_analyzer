# CV Analyzer AI matching service

FastAPI service for multilingual CV-to-job ranking. See `../docs/milestone-3b-ai-job-matching.md`.

```powershell
.\.venv\Scripts\Activate.ps1
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

The existing multilingual MiniLM model is loaded once, lazily on the first non-empty match request.
Job vectors are stored in a thread-safe process-local LRU cache. The default maximum is 5,000 entries
and can be changed with `JOB_EMBEDDING_CACHE_MAX_ENTRIES`.

`GET /health` reports the model version, cache hits/misses, invalidations, evictions, vector bytes,
model load time, and the last non-sensitive matching timings. A service restart starts with an empty cache.

