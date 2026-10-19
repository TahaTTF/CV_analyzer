# Architecture proposal

This document describes the target architecture; application components are not yet implemented.

## Component boundaries

| Component | Owns | Communicates through |
| --- | --- | --- |
| Angular | Upload UI, profiles, recommendation views, live progress | Public REST API and SSE |
| Spring Boot | Public API, validation, job ingestion, persistence, orchestration, authorization later | REST/SSE, internal FastAPI HTTP, MongoDB driver |
| FastAPI | PDF extraction, NLP profile extraction, embeddings, AI scoring inputs | Internal versioned HTTP API |
| MongoDB | Operational documents, analysis state, embeddings | Spring Data MongoDB; Atlas Vector Search in hosted environments |
| Job providers | Source job advertisements | Provider-specific HTTP clients behind adapters |

The browser talks only to Spring Boot. Spring Boot is the system boundary and delegates AI-specific processing to FastAPI. Provider payloads are retained only as needed for traceability; normalized jobs drive matching.

## Main data flows

### Job synchronization

1. A scheduled or manually triggered provider adapter fetches a page of jobs.
2. The backend maps the provider response into the normalized job model.
3. MongoDB upserts on `(source, externalId)` and records a `sync_logs` summary.
4. Requirements and embeddings are generated asynchronously and stored with model metadata.

### CV analysis and recommendation

1. Angular uploads one PDF to Spring Boot.
2. Spring Boot validates size and media type, creates `cvs` and `analysis_tasks` records, and returns IDs.
3. FastAPI extracts text, a structured profile, and an embedding.
4. Spring Boot persists the profile and advances durable progress state.
5. Atlas Vector Search retrieves candidates; explicit rules re-rank them.
6. Recommendations are persisted and returned to Angular. SSE publishes progress while processing continues.

## V1 decisions

- MongoDB is the sole primary database; do not add Qdrant in V1.
- SSE is preferred over WebSockets because progress is primarily server-to-client.
- Store embedding model name, version, and dimension beside vectors to support safe re-embedding.
- Treat uploaded CVs and extracted personal data as sensitive. Do not log raw CV text; add retention and access-control rules before production use.
- RAG explanations are deferred until deterministic matching and evidence are reliable.

## Shared integration points

- `docs/api-contract.yaml`: browser/backend contract; approval by both developers is required.
- Backend/FastAPI internal contract: Person 1 owns it and versions breaking changes.
- SSE event names and payload schema: produced by backend and consumed by frontend.
- Job/profile field semantics and score scale: reflected consistently in schema, DTOs, fixtures, and UI.
- Test fixtures: synthetic only; never commit real candidate data.

