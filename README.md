# AI CV Analyzer & Job Recommendation Platform

A local-first university project that turns PDF/DOCX CVs into structured profiles and returns ranked, explainable IT job recommendations. Spring Boot, MongoDB, CV parsing, FastAPI matching, MiniLM embeddings, caching, and automated tests are implemented. The Angular frontend remains to be generated.

## Implemented workflow

1. Import jobs from Arbeitnow, Adzuna, and La bonne alternance when guarded ingestion is deliberately enabled.
2. Normalize, deduplicate, and store jobs in MongoDB.
3. Clean descriptions and extract evidence-backed technical requirements offline.
4. Accept PDF/DOCX CVs and extract layout-aware text with PDFBox/Apache POI.
5. Build a structured candidate profile and CV embedding.
6. Reuse bounded in-memory job embeddings and compute multilingual cosine similarity.
7. Combine semantic similarity with explicit requirement matching.
8. Return ranked, explainable results synchronously through Spring Boot.

RAG-generated explanations are a possible later extension, not part of V1.

## Technology

- Angular frontend (not generated yet)
- Java and Spring Boot REST backend
- Python 3.12, FastAPI, NumPy, NLP, and Sentence-Transformers AI service
- MongoDB as the primary job database; process-local job-vector cache for current scale
- Docker Compose for local MongoDB
- Synchronous REST for the implemented MVP

## Architecture

```text
Angular client (next implementation)
  | REST
Spring Boot API ---- MongoDB
  | internal HTTP
FastAPI AI service (embeddings, cached ranking)
  |
External job APIs (Arbeitnow, France Travail, Adzuna)
```

See [docs/architecture.md](docs/architecture.md), the proposed [API contract](docs/api-contract.yaml), and [database model](docs/database-schema.md).

## Repository layout

```text
backend/       Spring Boot API, CV parser, MongoDB, ingestion and orchestration
ai-service/    FastAPI embeddings, caching and hybrid ranking
frontend/      Angular placeholder; implementation has not started
database/      MongoDB setup notes and future scripts
docs/          Architecture, API, schema, setup, and workflow documentation
docs/team/     Individual development guides
tests/         Cross-service notes; service tests live beside each implementation
.github/       Pull request template and future CI workflows
```

## Team responsibilities

- **Person 1 — AI Engineer + Backend Lead + Data & QA Engineer:** backend, AI service, data model, ingestion, matching, server-side progress, API documentation, and integration testing. See [PERSON1_README.md](docs/team/PERSON1_README.md).
- **Person 2 — Frontend & Real-Time Collaboration Engineer:** Angular structure and UI, API client, client-side SSE progress, frontend tests, mocks, and shared QA. See [PERSON2_README.md](docs/team/PERSON2_README.md).

Shared integration points are the OpenAPI contract, MongoDB-facing DTO expectations, SSE event format, mock fixtures, and end-to-end acceptance tests. Changes to shared contracts require both developers' agreement.

## Branches and collaboration

- `main`: stable shared branch; changes arrive through reviewed pull requests.
- `dev/person1-backend-ai`: Person 1's working branch.
- `dev/person2-frontend`: Person 2's working branch.

Read [docs/git-workflow.md](docs/git-workflow.md) before contributing.

## Local prerequisites

- Git
- Docker Desktop with Docker Compose
- Java 21, Maven wrapper, Python 3.12 and the existing AI virtual environment
- Node.js LTS and Angular tooling when frontend implementation begins

Copy `.env.example` to `.env`, keeping all real credentials local. Start the currently defined MongoDB service with:

```bash
docker compose up -d mongodb
```

Follow [backend/README.md](backend/README.md) and [ai-service/README.md](ai-service/README.md). Start MongoDB, FastAPI, Spring Boot, then Angular once it exists.

## Current status

**Implemented:** guarded multi-provider ingestion, deterministic classification/enrichment, MongoDB job APIs, PDF/DOCX parser v1.3, explainable MiniLM recommendation engine, bounded job-vector cache, error contracts, Postman collection, and automated tests.

**Not implemented:** Angular UI, authentication/authorization, production CORS, persisted CV/recommendation history, OCR, automated CI, or production deployment.

## Next milestones

1. Freeze the actual synchronous OpenAPI contract and Angular proxy/CORS decision.
2. Person 2 scaffolds Angular using [the frontend handoff](docs/person2-frontend-handoff.md).
3. Add a privacy-safe single-upload analysis workflow so parsed profiles can be reused.
4. Add authentication/ownership before durable CV persistence.
5. Build a human-labelled relevance evaluation before further score tuning.

See [backend progress](docs/backend-progress-report.md), [optimization results](docs/milestone-3b3-optimization-report.md), and [backend next steps](docs/backend-next-steps.md).

