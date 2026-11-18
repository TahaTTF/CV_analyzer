# Backend next steps

This roadmap starts from the verified 2026-11-14 implementation. It proposes work only; none of these features is implemented by this document.

## Prioritization scale

- **Priority:** P0 blocks safe frontend integration/production; P1 is MVP value; P2 improves quality/operations; P3 is future scope.
- **Complexity:** S (days), M (roughly one sprint), L (multiple coordinated sprints).
- **Dependency:** identifies work that must precede or coordinate the task.

## Recommended next milestone

The next backend milestone should be **API stabilization and single-upload CV analysis workflow**, not another provider or model. It should establish the frontend contract, eliminate duplicate parsing/upload, and define privacy boundaries before any CV persistence.

## Roadmap

| Priority | Task | Scope | Frontend dependency | Complexity | Target |
|---|---|---|---|---:|---|
| P0 | Freeze implemented OpenAPI contract | Replace/retire the obsolete proposed YAML with the actual synchronous endpoints, DTOs, enums and errors; add contract tests | Person 2 needs stable generated/manual models | S | MVP |
| P0 | Development CORS/proxy decision | Prefer Angular dev proxy; if cross-origin is required, add a narrow configurable origin policy—never wildcard credentials | Coordinate local Angular origin | S | MVP |
| P0 | CV privacy/retention design | Decide whether CV bytes/profiles may be stored, TTL, encryption, ownership, deletion and audit requirements before persistence | UI must state actual retention | M | MVP prerequisite |
| P1 | Reuse parsed CV without reupload | Add a minimal analysis workflow so review and recommendations use one upload and one parse | Replaces in-memory duplicate `File` submission | M | MVP |
| P1 | Recommendation DTO metadata | Consider adding `remote`, `publishedAt`, and `employmentTypes` to avoid frontend detail N+1 calls | Joint DTO review | S | MVP |
| P1 | Repository-level eligible-job query | Replace `findAll()` plus Java filtering with a MongoDB query/projection for active non-negative IT candidates | No contract change | S–M | MVP scalability |
| P1 | Authentication and ownership | Define users, sessions/tokens, route authorization and ownership before durable CV/profile/history storage | Frontend auth flow required | L | Production blocker |
| P1 | CV correction model | Let a user correct target role, skills and structured entries; validate corrections and distinguish parser/user provenance | Requires review UI design | M–L | Post-MVP |
| P2 | Parser confidence | Add per-field/entry confidence and evidence spans rather than only global warnings | Frontend confidence UX | M | Quality |
| P2 | Human relevance evaluation | Create anonymized labelled CV/job judgments with reviewer agreement; calculate Precision@K/NDCG/MRR and tune transparently | Optional evaluator UI | M | Quality |
| P2 | Seniority and experience matching | Extend internal DTOs with supported CV/job experience and seniority evidence; add conservative score component and tests | Display explanation changes | M | Quality |
| P2 | Language requirement matching | Extract explicit job languages/levels, compare only evidence-backed CV languages, and explain gaps | UI evidence display | M | Quality |
| P2 | Encoding repair at ingestion boundary | Detect provider UTF-8/mojibake before MongoDB storage; add provider fixtures; plan a separately approved data repair | Display quality improves | M | Data quality |
| P2 | Cache warm-up/operations | Optional startup/background warm-up, metrics endpoint integration and worker-count guidance; preserve bounded memory | None | S–M | Reliability |
| P2 | CI pipeline | Run Python and Maven unit suites, secret scanning and contract checks on pull requests | Frontend CI can join later | M | Reliability |
| P2 | Observability/rate limiting | Structured request metrics, correlation IDs across Spring/FastAPI, upload limits and per-client throttling without CV logging | Error/trace UX | M | Production |
| P3 | OCR | Add an explicitly bounded OCR path for scanned PDFs with resource/security tests | Upload UX/progress | L | Future |
| P3 | Additional job provider | Add only after quota, legal terms, mapping, deduplication and regression fixtures are agreed | New source filter | M–L | Future |
| P3 | Persistent/vector index | Re-evaluate only when job volume/latency proves the in-memory cache insufficient; avoid premature infrastructure | None initially | L | Future scale |
| P3 | Deployment hardening | Container images, secret manager, TLS, production MongoDB backup/restore, health/readiness and resource limits | Deployment config | L | Production |

## 1. API stabilization

`docs/api-contract.yaml` currently describes unimplemented asynchronous CV IDs, recommendation history and SSE. The first task is to publish the actual Spring contract:

- `POST /api/cv/parse`
- `POST /api/recommendations/from-cv`
- `GET /api/jobs`
- `GET /api/jobs/{jobId}`
- common `application/problem+json`

Add automated schema/serialization tests so Java record changes cannot silently break Angular. Version breaking changes rather than changing field meaning in place.

## 2. One-upload analysis options

Choose one design explicitly:

### MVP option: short-lived parsed-profile token

`POST /api/cv/parse` could return an opaque `analysisId` alongside the response. Spring stores only the sanitized parsed representation in a bounded TTL cache, not raw bytes. A new recommendation route consumes that ID. This supports review-before-match without durable personal-data storage.

Required controls: cryptographically random IDs, short TTL, maximum entries, no profile logging, explicit expiration error, and a deletion endpoint. Without authentication, document that this is for local/demo use only.

### Production option: authenticated CV resource

Persist encrypted CV metadata/profile under an authenticated owner with retention and deletion. This should not begin until authentication, authorization, storage location, encryption, access logging and retention are approved.

Do not implement the older proposed asynchronous/SSE design merely because it exists in YAML; warm matching is currently sub-second and does not justify task infrastructure.

## 3. CV parsing confidence and corrections

Recommended sequence:

1. Add evidence spans/section source and confidence to internal extraction results.
2. Define which fields are user-editable.
3. Store parser value and user-corrected value separately with provenance.
4. Recompose semantic text from validated corrected fields.
5. Add synthetic tests for corrections, null values, multilingual text and personal-data redaction.

Never silently train or tune against real CVs without consent and anonymization.

## 4. Recommendation evaluation

Current transparent role fixtures are provisional regression rules. A proper evaluation needs:

- Anonymized candidate profiles.
- A stable snapshot of eligible jobs or synthetic equivalents.
- At least two reviewers for relevant/partial/unrelated judgments.
- Documented disagreement resolution.
- Baseline and changed Precision@1/5/10, NDCG and MRR only after labels exist.
- Slices for data, backend/frontend, DevOps/cloud, systems/network, mobile and security roles.

Quality changes must preserve skill evidence and privacy checks. Do not tune only for CV-04/07/09.

## 5. Seniority, experience and languages

These factors are not currently scored because the internal job candidate DTO lacks reliable structured evidence. Implement extraction first, then matching:

- Seniority aliases: internship/junior/mid/senior/lead with multilingual title patterns.
- Explicit minimum experience only; do not infer from marketing language.
- Language name/level only when present in CV/job evidence.
- Conservative penalties and neutral behavior for missing data.
- Explanation text tied to evidence.

## 6. Job query and payload efficiency

At 391 jobs, `findAll()` is acceptable, but the next implementation should query eligible records directly and project only fields required by FastAPI. Measure Mongo time, serialization size and correctness before/after. Preserve the authoritative ID-resolution check for AI responses.

If a vector store is considered later, first demonstrate that the current bounded cache and vectorized scoring fail a measured volume/latency target.

## 7. Security and production readiness

Minimum production checklist:

- Authentication and object ownership.
- Narrow CORS and CSRF/session decision.
- Secret manager rather than `.env` deployment secrets.
- CV retention/deletion and encryption.
- Rate limits and abuse protection for file parsing/model inference.
- Malware/content scanning decision for uploads.
- Correlation IDs without request bodies.
- Dependency, container and secret scanning in CI.
- MongoDB backup/restore drill and least-privilege user.
- TLS and reverse proxy limits.

## Suggested delivery order

1. Actual OpenAPI + contract tests and frontend proxy/CORS agreement.
2. Single-upload short-lived analysis workflow with privacy controls.
3. Recommendation DTO metadata and repository-level eligible query.
4. Angular integration feedback/fixes without widening scope.
5. Authentication/ownership before durable CV persistence.
6. Confidence/corrections and labelled recommendation evaluation.
7. Seniority/experience/language matching.
8. CI, observability, rate limiting and deployment hardening.

This order reduces immediate frontend guesswork while avoiding premature persistence, vector infrastructure, OCR, or additional providers.
