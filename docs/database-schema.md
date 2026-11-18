# MongoDB data model proposal

This is a V1 proposal requiring review before implementation. MongoDB is the primary database, including Atlas Vector Search in hosted environments. Qdrant is intentionally excluded from V1. Timestamps use UTC BSON dates; identifiers below are illustrative.

## Relationships

`users._id` is referenced by CVs and candidate profiles. A CV has one current candidate profile and can have many analysis tasks and recommendation runs. Recommendations reference normalized jobs. `sync_logs` records provider import runs.

## Collections

### `users` (MVP only if accounts are in scope)

Important fields: `_id`, `email`, `displayName`, `roles`, `createdAt`, `updatedAt`.

```json
{"_id":"ObjectId","email":"student@example.test","displayName":"Test Candidate","roles":["CANDIDATE"],"createdAt":"Date"}
```

Indexes: unique normalized `email`; optional TTL index for unverified temporary accounts.

### `cvs` (MVP essential)

Important fields: `_id`, optional `userId`, safe file metadata, storage reference, checksum, extraction state, timestamps. Store files outside MongoDB unless GridFS is deliberately selected; never expose a server filesystem path.

```json
{"_id":"cv_01","userId":"user_01","fileName":"candidate-cv.pdf","contentType":"application/pdf","sizeBytes":184220,"sha256":"hex-digest","storageKey":"private/cvs/generated-key","status":"PROCESSING","createdAt":"Date"}
```

Indexes: `{userId:1, createdAt:-1}`, `sha256` (non-unique unless reuse semantics are agreed), and optional retention TTL.

### `candidate_profiles` (MVP essential)

Important fields: `_id`, `cvId`, contact-safe structured fields, `skills`, `experience`, `education`, `languages`, source spans/confidence, `embedding`, and extraction/model versions.

```json
{"_id":"profile_01","cvId":"cv_01","headline":"Backend developer","skills":[{"name":"Java","normalized":"java","confidence":0.96}],"experience":[{"title":"Developer","company":"Example","startDate":"2024-01","endDate":null}],"education":[],"languages":[{"code":"fr","level":"C1"}],"embedding":{"model":"sentence-transformers/example","version":"1","dimensions":768,"values":[0.012,-0.034]},"createdAt":"Date"}
```

Indexes: unique `cvId`; optional multikey `skills.normalized`. Restrict access because extracted data is personal data.

### `jobs` (MVP essential)

Important fields: `_id`, `source`, `externalId`, normalized title/company/location/description, requirements, canonical URL, dates, raw-payload trace metadata, lifecycle status, and embedding metadata.

```json
{"_id":"job_01","source":"ARBEITNOW","externalId":"12345","title":"Java Developer","company":{"name":"Example SAS"},"location":{"city":"Paris","countryCode":"FR","remote":true},"description":"Normalized description","requirements":{"skills":["java","spring boot"],"languages":[{"code":"en","level":"B2"}],"minExperienceYears":2},"embedding":{"model":"sentence-transformers/example","version":"1","dimensions":768,"values":[0.021,-0.018]},"sourceUpdatedAt":"Date","lastSeenAt":"Date","status":"ACTIVE"}
```

Indexes: unique compound `{source:1, externalId:1}` for job identity; `{status:1, lastSeenAt:-1}`; location and skills indexes based on query evidence; an Atlas Vector Search index on `embedding.values` with matching dimensions and cosine similarity. Arrays must not mix vectors from different models.

### `recommendations` (MVP essential)

Important fields: `_id`, `cvId`, `profileId`, run/model version, ranked job results, semantic and explicit component scores, evidence, and timestamps.

```json
{"_id":"rec_01","cvId":"cv_01","profileId":"profile_01","scoringVersion":"v1","results":[{"jobId":"job_01","rank":1,"score":0.87,"semanticScore":0.82,"requirementsScore":0.92,"matchedSkills":["java"],"missingSkills":["kubernetes"]}],"createdAt":"Date"}
```

Indexes: `{cvId:1, createdAt:-1}` and `results.jobId` only if reverse lookup is required. Preserve score components for explainability.

### `analysis_tasks` (MVP essential)

Important fields: `_id`, `cvId`, `status`, current stage, integer progress percent, safe message, error code (not a stack trace), attempt count, and timestamps.

```json
{"_id":"analysis_01","cvId":"cv_01","status":"PROCESSING","stage":"GENERATING_EMBEDDING","progressPercent":65,"message":"Generating candidate representation","attempt":1,"createdAt":"Date","updatedAt":"Date"}
```

Allowed status values are `PENDING`, `PROCESSING`, `COMPLETED`, and `FAILED`. Indexes: `{cvId:1, createdAt:-1}`, `{status:1, updatedAt:1}`, and an optional TTL on a separate `expiresAt`. SSE consumers receive progress derived from durable updates; MongoDB is the recovery source after reconnects.

### `sync_logs` (MVP essential for ingestion operations)

Important fields: `_id`, provider, status, cursor/page metadata, fetched/inserted/updated/skipped/failed counts, safe errors, and timing.

```json
{"_id":"sync_01","source":"ARBEITNOW","status":"COMPLETED","counts":{"fetched":100,"inserted":70,"updated":25,"skipped":5,"failed":0},"startedAt":"Date","finishedAt":"Date"}
```

Indexes: `{source:1, startedAt:-1}`, `{status:1, startedAt:-1}`, and an optional retention TTL.

## Consistency and privacy rules

- Use immutable IDs and application-level references; do not rely on joins in latency-critical paths.
- Validate documents at the application boundary and consider MongoDB JSON Schema validators after models stabilize.
- Store embedding values as numeric arrays supported by the selected Atlas index, together with model/version/dimensions.
- Recompute job and CV vectors together when changing embedding models; never compare incompatible vector spaces.
- Encrypt transport and storage, minimize retained raw text, redact logs, and define deletion/retention behavior before real candidate data is used.

## Implemented Milestone 1.5 job additions

The current Spring document keeps the original normalized provider text plus source metadata:
partner label/ID, provider categories, tags, and ROME codes. requirements now also supports
toBeAcquiredSkills. itClassification stores isItRelevant, matchedCategories, relevanceReason,
version, and classifiedAt. All fields are optional for backward compatibility with the 300
existing Arbeitnow documents.

Implemented indexes are:

- unique source_external_id_unique on source + externalId;
- status_fetched_at;
- status_it_published_at for relevant-job browsing;
- publishedAt.

