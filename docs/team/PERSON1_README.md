# Person 1 development guide

**Role:** AI Engineer + Backend Lead + Data & QA Engineer

This guide sequences backend, AI, data, and integration ownership. Shared-contract changes require Person 2's review before implementation.

## Responsibility plan

### 1. Design MongoDB collections and indexes

- **Objective:** Turn `docs/database-schema.md` into validated persistence models with privacy and retention rules.
- **Files/modules:** `docs/database-schema.md`, future `backend` document classes/repositories, `database` index scripts.
- **Dependencies:** Approved field semantics, MongoDB/Atlas environment, selected embedding dimensions.
- **Deliverable:** Reviewed schemas and idempotent index creation, including unique `(source, externalId)`.
- **Verify:** Run index setup twice; inspect indexes; prove duplicate provider jobs are rejected/upserted as designed.

### 2. Initialize the Spring Boot REST backend

- **Objective:** Create a maintainable Java service with configuration, health checks, validation, and test foundations.
- **Files/modules:** `backend/pom.xml`, `backend/src/main`, `backend/src/test`, profile configuration.
- **Dependencies:** Java/Maven versions and approved package naming.
- **Deliverable:** Bootable service with local profile and MongoDB connectivity health reporting.
- **Verify:** Run unit tests, package the application, start locally, and query its health endpoint.

### 3. Implement job ingestion, starting with Arbeitnow

- **Objective:** Fetch provider pages reliably through a replaceable adapter.
- **Files/modules:** backend `jobs/ingestion` client, DTO, mapper, scheduler/service, fixtures.
- **Dependencies:** Spring scaffold, provider terms and API behavior, normalized job model.
- **Deliverable:** Arbeitnow adapter with timeouts, pagination, rate-limit/error handling, and synthetic fixture tests.
- **Verify:** Contract tests map representative responses and a controlled sync records accurate counts.

### 4. Implement normalization, deduplication, and updates

- **Objective:** Produce stable normalized jobs and idempotent synchronization.
- **Files/modules:** backend normalization/domain/repository services and `sync_logs` persistence.
- **Dependencies:** Ingestion adapter and approved job schema/index.
- **Deliverable:** Deterministic mapping and upsert by `(source, externalId)`, with last-seen and lifecycle handling.
- **Verify:** Import the same fixture twice without duplicates; import a changed fixture and observe one update.

### 5. Initialize the FastAPI AI service

- **Objective:** Establish a versioned internal AI API, settings, health route, and tests.
- **Files/modules:** `ai-service/pyproject.toml`, application package, configuration, API routes, tests.
- **Dependencies:** Supported Python version and backend-to-AI boundary decision.
- **Deliverable:** Reproducible environment and a bootable, tested FastAPI service without model logic yet.
- **Verify:** Install from a clean environment, run lint/type/tests, start the service, and query health/OpenAPI.

### 6. Implement PDF text extraction with PyMuPDF

- **Objective:** Safely extract ordered text and metadata from supported PDFs.
- **Files/modules:** AI `cv/extraction`, validation, synthetic PDF fixtures and tests.
- **Dependencies:** FastAPI scaffold, upload limits, privacy/logging rules.
- **Deliverable:** Extraction component handling normal, empty, corrupt, encrypted, and oversized inputs.
- **Verify:** Golden tests assert expected text; malformed inputs return stable error codes and never leak content to logs.

### 7. Implement structured CV profile extraction

- **Objective:** Convert extracted text into skills, experience, education, and languages with traceable confidence.
- **Files/modules:** AI `cv/profile`, taxonomies/normalizers, schemas, evaluation fixtures.
- **Dependencies:** Text extraction and agreed candidate profile schema.
- **Deliverable:** Versioned, deterministic extraction pipeline with source evidence where practical.
- **Verify:** Evaluate on synthetic labelled CVs; review field accuracy and regression-test edge cases.

### 8. Generate Sentence-BERT embeddings

- **Objective:** Generate compatible vectors for CVs and jobs through one versioned model boundary.
- **Files/modules:** AI `embeddings`, model configuration/cache, batch code and tests.
- **Dependencies:** Normalized text strategy, approved model/license, dimension and resource budget.
- **Deliverable:** Batched embedding service recording model, version, dimensions, and normalization settings.
- **Verify:** Assert vector dimensions/finiteness, deterministic similarity tolerance, and acceptable latency on a fixture batch.

### 9. Integrate MongoDB Atlas Vector Search

- **Objective:** Retrieve semantically similar active jobs from the same embedding space.
- **Files/modules:** Atlas index definition/runbook, backend repository/query service, integration tests.
- **Dependencies:** Atlas cluster, populated compatible vectors, index availability.
- **Deliverable:** Parameterized top-K vector query with filters and documented local fallback/testing approach.
- **Verify:** Known fixtures rank expected neighbours; incompatible/missing embeddings fail safely.

### 10. Implement matching and scoring

- **Objective:** Combine semantic similarity with explicit requirements into a transparent score.
- **Files/modules:** backend recommendation/scoring domain, configuration, AI helpers if justified, test datasets.
- **Dependencies:** Vector candidates, structured profiles/jobs, agreed score scale and weights.
- **Deliverable:** Versioned scoring function returning total, component scores, matched skills, and missing skills.
- **Verify:** Unit-test boundaries and weights; run labelled ranking fixtures and document observed metrics.

### 11. Implement backend REST endpoints

- **Objective:** Implement the approved public API with validation and stable errors.
- **Files/modules:** backend controllers, DTOs, mappers, services, exception handler, OpenAPI tests.
- **Dependencies:** Approved `docs/api-contract.yaml` and relevant application services.
- **Deliverable:** Contract-conformant CV, job, and recommendation endpoints with no internal entities exposed.
- **Verify:** Run controller/contract tests for success and every documented error status; validate responses against OpenAPI.

### 12. Implement server-side live analysis progress

- **Objective:** Publish durable analysis state through the proposed SSE endpoint.
- **Files/modules:** backend analysis task service/repository, SSE controller, timeout/reconnect handling.
- **Dependencies:** Approved event schema and asynchronous workflow boundaries.
- **Deliverable:** Ordered progress events with terminal completion/failure and recovery from persisted state.
- **Verify:** Test connection, reconnect, concurrent listeners, timeout, terminal closure, and frontend mock compatibility.

### 13. Write backend, AI, database, and integration tests

- **Objective:** Protect service boundaries, persistence behavior, and the core analysis path.
- **Files/modules:** service-local test trees, `tests/` fixtures/contract/E2E assets, CI workflow later.
- **Dependencies:** Implemented slices and deterministic synthetic data.
- **Deliverable:** Fast unit suite plus container-backed integration and contract tests with documented commands.
- **Verify:** Run from a clean checkout; ensure tests are repeatable, isolated, and fail on intentional contract regressions.

### 14. Prepare API documentation

- **Objective:** Keep the OpenAPI description, examples, errors, and operational notes aligned with behavior.
- **Files/modules:** `docs/api-contract.yaml`, backend generated-doc configuration, Postman assets in `tests/` if adopted.
- **Dependencies:** Both developers' approval and endpoint behavior.
- **Deliverable:** Valid OpenAPI contract and usable examples for frontend/mock development.
- **Verify:** Validate the spec, render it, exercise examples, and obtain Person 2 approval on shared changes.

### 15. Integrate backend with Angular

- **Objective:** Deliver an end-to-end upload, progress, profile, and recommendations journey.
- **Files/modules:** backend CORS/security/API configuration, shared fixtures, `tests/` E2E scenarios, frontend API consumers with Person 2.
- **Dependencies:** Stable endpoints, SSE, Angular screens, local configuration.
- **Deliverable:** One reproducible happy path plus documented error and reconnect paths.
- **Verify:** Start from a clean environment; execute the journey in-browser and automated E2E tests without mock APIs.

## First three development days

### Day 1 — agreements and backend foundation

- [ ] Review `docs/api-contract.yaml` and `docs/database-schema.md` with Person 2; record decisions.
- [ ] Select Java, Spring Boot, Python, and embedding model versions; document compatibility.
- [ ] Scaffold Spring Boot with web, validation, actuator, MongoDB, and test dependencies.
- [ ] Add local configuration binding without secrets and verify health/test/package commands.
- [ ] Commit a small backend-foundation change and open a reviewable pull request.

### Day 2 — persistence and first ingestion slice

- [ ] Implement normalized job and sync-log persistence models.
- [ ] Add idempotent indexes, especially unique `(source, externalId)`.
- [ ] Build an Arbeitnow client behind a provider adapter with checked-in synthetic responses.
- [ ] Implement mapping and upsert tests, including repeated and updated imports.
- [ ] Give Person 2 current job examples for frontend mocks.

### Day 3 — AI foundation and integration seam

- [ ] Scaffold FastAPI with settings, health route, schemas, and tests.
- [ ] Define the versioned internal backend/AI request and error boundary.
- [ ] Implement a first safe PyMuPDF extraction component with synthetic PDFs.
- [ ] Add backend client contract tests using a stubbed AI service.
- [ ] Demo ingestion and extraction, update the task board, and identify next-week risks.

