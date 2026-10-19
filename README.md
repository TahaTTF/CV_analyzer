# AI CV Analyzer & Job Recommendation Platform

A university project that will help candidates turn a PDF CV into a structured profile and receive ranked, explainable job recommendations. This repository currently provides the agreed architecture, API and data-model proposals, collaboration rules, and local MongoDB infrastructure. The Spring Boot, FastAPI, and Angular applications are **planned and not yet implemented**.

## Planned workflow

1. Import jobs from Arbeitnow, France Travail, and Adzuna.
2. Normalize, deduplicate, and store jobs in MongoDB.
3. Extract requirements and generate Sentence-BERT job embeddings.
4. Accept a candidate's PDF CV and extract its text with PyMuPDF.
5. Build a structured candidate profile and CV embedding.
6. retrieve similar jobs with MongoDB Atlas Vector Search.
7. Combine semantic similarity with explicit requirement matching.
8. Return ranked results and analysis progress to the Angular client.

RAG-generated explanations are a possible later extension, not part of V1.

## Technology plan

- Angular frontend
- Java and Spring Boot REST backend
- Python, FastAPI, PyMuPDF, NLP, and Sentence-Transformers AI service
- MongoDB as the primary database; Atlas Vector Search for V1 similarity search
- Docker Compose for local MongoDB
- Server-Sent Events (SSE) for one-way analysis progress updates

## Architecture

```text
Angular client
  | REST + SSE
Spring Boot API ---- MongoDB / Atlas Vector Search
  | internal HTTP
FastAPI AI service (CV parsing, profile extraction, embeddings)
  |
External job APIs (Arbeitnow, France Travail, Adzuna)
```

See [docs/architecture.md](docs/architecture.md), the proposed [API contract](docs/api-contract.yaml), and [database model](docs/database-schema.md).

## Repository layout

```text
backend/       Spring Boot service (planned)
ai-service/    FastAPI and NLP service (planned)
frontend/      Angular application (planned)
database/      MongoDB setup notes and future scripts
docs/          Architecture, API, schema, setup, and workflow documentation
docs/team/     Individual development guides
tests/         Cross-service fixtures and integration/E2E tests (planned)
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
- Later, when applications exist: Java 21, Maven (or wrapper), Python 3.11+, Node.js LTS, and Angular CLI through the project toolchain

Copy `.env.example` to `.env`, keeping all real credentials local. Start the currently defined MongoDB service with:

```bash
docker compose up -d mongodb
```

Once implemented, the intended startup sequence will be MongoDB, FastAPI, Spring Boot, then Angular. Exact commands will be added to each service README; the application services cannot be run yet.

## Current status

**Implemented:** repository skeleton, collaboration documentation, proposed OpenAPI contract, proposed MongoDB schema, environment template, and a local MongoDB Compose definition.

**Not implemented:** application source code, external API ingestion, parsing, embeddings, vector search, matching, authentication, UI, automated CI, and production deployment.

## Next milestones

1. Both developers review and approve the API and data-model proposals.
2. Person 1 scaffolds Spring Boot, MongoDB migrations/index setup, and the Arbeitnow ingestion slice.
3. Person 2 scaffolds Angular and builds the upload/recommendation flow against mocks.
4. Person 1 scaffolds FastAPI and implements PDF extraction plus profile parsing.
5. Integrate upload, SSE progress, recommendation retrieval, and cross-service tests.

