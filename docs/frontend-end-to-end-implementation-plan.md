# Frontend end-to-end implementation plan

This is the implementation handoff for Person 2. It describes the repository as it exists on 2026-11-14. It does not treat the proposed contracts in `docs/api-contract.yaml` as implemented.

## 1. Verified current state

`frontend/` contains only `README.md`. There is no Angular workspace, `package.json`, Angular version, routing, component, service, environment configuration, authentication, styling library, or frontend test. The browser-facing implementation must therefore be generated from scratch inside the existing `frontend/` directory.

The current backend is synchronous:

```text
Angular -> Spring Boot :8080 -> MongoDB :27018
                              -> FastAPI :8000 -> cached multilingual MiniLM
```

The browser must never call FastAPI directly. Spring Boot is the public boundary. No authentication, authorization, CORS configuration, saved CV, saved recommendation, progress stream, or user/profile API currently exists. CV files are processed in memory; no CV document is persisted by the implemented flow.

The proposed asynchronous `/api/cvs`, `/api/analysis`, and persisted `/api/recommendations` endpoints in `docs/api-contract.yaml` do **not** exist.

## 2. Recommended frontend structure

Use standalone Angular components, typed reactive forms, Angular signals for page state, and `HttpClient`. NgRx is unnecessary for the current synchronous workflow. Select and record a team-approved supported Angular version when generating the workspace; the repository provides no existing version to preserve.

```text
frontend/
├── angular.json
├── package.json
├── proxy.conf.json
├── src/
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.development.ts
│   ├── styles.scss
│   └── app/
│       ├── app.component.ts
│       ├── app.config.ts
│       ├── app.routes.ts
│       ├── core/
│       │   ├── config/api.config.ts
│       │   ├── interceptors/api-error.interceptor.ts
│       │   └── services/
│       │       ├── cv-api.service.ts
│       │       ├── job-api.service.ts
│       │       ├── recommendation-api.service.ts
│       │       └── health-api.service.ts
│       ├── shared/
│       │   ├── components/
│       │   │   ├── error-panel/
│       │   │   ├── loading-indicator/
│       │   │   ├── score-meter/
│       │   │   └── skill-chips/
│       │   ├── models/
│       │   │   ├── api-error.model.ts
│       │   │   ├── cv.model.ts
│       │   │   ├── job.model.ts
│       │   │   └── recommendation.model.ts
│       │   └── utils/file-validation.ts
│       ├── layouts/
│       │   └── application-shell/
│       └── features/
│           ├── dashboard/
│           ├── cv-analysis/
│           │   ├── cv-upload/
│           │   └── profile-review/
│           ├── recommendations/
│           │   ├── recommendation-page/
│           │   └── recommendation-card/
│           └── jobs/
│               ├── job-search-page/
│               └── job-details-page/
└── e2e/
```

Responsibilities:

- `core/config`: API origin/token-free runtime configuration. Never place secrets here.
- `core/services`: one typed adapter for each Spring resource; components never construct URLs.
- `core/interceptors`: translate `application/problem+json` and network failures into the shared error model. It must not log CV files or response profiles.
- `shared/models`: exact TypeScript representations of the implemented Java records.
- `shared/components`: presentation-only loading, error, score, and skill UI.
- `features/cv-analysis`: owns the selected `File`, parse result, validation, and review UI.
- `features/recommendations`: submits the retained file to the recommendation endpoint and preserves backend ordering.
- `features/jobs`: paginated search and on-demand job details.
- `layouts`: navigation and page outlet only.

## 3. Routes and component contracts

| Route | Component | Inputs / outputs | Services | State and required states |
|---|---|---|---|---|
| `/` | `DashboardComponent` | none | `HealthApiService` optionally | Intro/actions; backend unavailable banner must not block static navigation. |
| `/cv` | `CvUploadComponent` | output selected file and parsed result to feature state | `CvApiService` | Idle, local-validation error, uploading, parsed, backend error. Disable duplicate submission while active. |
| `/cv/review` | `ProfileReviewComponent` | selected file and `CvParseResponse` from in-memory feature store | none | Structured profile, warnings, missing sections. Redirect to `/cv` after refresh because no persisted CV exists. |
| `/recommendations` | `RecommendationPageComponent` | retained file; optional parse result for context | `RecommendationApiService` | Matching, results, no jobs, timeout, AI unavailable, retry. Preserve backend order. |
| `/jobs` | `JobSearchPageComponent` | query parameters for filters/page | `JobApiService` | Initial loading, page, empty search, validation/database/network error. |
| `/jobs/:jobId` | `JobDetailsPageComponent` | route `jobId` | `JobApiService` | Loading, details, 404, invalid ID, network error. |

`CvAnalysisStore` can be a feature-scoped injectable with signals for `file`, `parseResponse`, and `recommendationResponse`. It is intentionally ephemeral: do not put CV contents in `localStorage`, `sessionStorage`, URL parameters, analytics, or browser logs.

## 4. Verified endpoint inventory

Authentication is `None` for every implemented endpoint. That is a current limitation, not a production recommendation.

| Method | Endpoint | Backend | Purpose | Auth | Request | Response | Frontend consumer |
|---|---|---|---|---|---|---|---|
| GET | `/actuator/health` | Spring | Backend/Mongo health summary | None | none | `{ "status": "UP" }` | Optional dashboard availability indicator |
| POST | `/api/cv/parse` | Spring | Parse a PDF/DOCX synchronously | None | multipart field `file` | `CvParseResponse` | CV upload/review |
| GET | `/api/jobs` | Spring | Search active jobs with relevance filter | None | query parameters below | `PageResponse<JobSummary>` | Job search |
| GET | `/api/jobs/{jobId}` | Spring | Read one job | None | path `jobId` | `JobDetails` | Job details |
| POST | `/api/recommendations/from-cv` | Spring | Parse CV and return ranked jobs synchronously | None | multipart `file`, query `topK` | `RecommendationResponse` | Recommendations |
| GET | `/api/dev/enrichment/audit` | Spring, `local` profile only | Read data-quality audit | None | none | `DataQualityAudit` | Not for end-user UI |
| POST | `/api/dev/enrichment/jobs` | Spring, `local` only | Dry-run or execute enrichment | None | `dryRun`, `batchSize` | `EnrichmentSummary` | Developer tooling only; do not expose |
| POST | `/api/dev/ingestion/*` | Spring, `local` only | Provider ingestion/classification | None | none | ingestion/classification summaries | Never expose in user UI |
| GET | `/health` | FastAPI internal | AI/cache health | None | none | status/model/cache diagnostics | Spring/operations only; browser must not call |
| POST | `/match` | FastAPI internal | Internal ranking contract | None | `MatchRequest` JSON | `MatchResponse` | Spring only |

There is no login, registration, current-user, profile, CV management, recommendation history, saved job, or progress endpoint.

### 4.1 `POST /api/cv/parse`

- Base URL: configured Spring origin, locally `http://localhost:8080`.
- Content type: `multipart/form-data`; let the browser set the boundary.
- Field: `file`.
- Supported names/content: `.pdf` with PDF magic bytes; `.docx` with a valid non-macro DOCX package.
- Maximum upload: 5 MB. PDF maximum: 100 pages. Extracted text maximum: 500,000 characters.
- Successful status: `200`.

Response shape:

```ts
interface CvParseResponse {
  parsingStatus: 'SUCCESS';
  parserVersion: string;
  profile: {
    targetRole: string | null;
    professionalSummary: string | null;
    technicalSkills: string[];
    yearsOfExperience: number | null;
    experience: Array<{ role: string | null; organization: string | null; period: string | null; description: string | null }>;
    projects: Array<{ name: string | null; description: string | null; technologies: string[] }>;
    education: Array<{ qualification: string | null; institution: string | null; period: string | null; description: string | null }>;
    languages: string[];
  };
  professionalSummaryText: string | null;
  skillsText: string | null;
  experienceText: string | null;
  projectsText: string | null;
  educationText: string | null;
  semanticProfileText: string | null;
  warnings: string[];
}
```

An abbreviated successful example (all collections remain present in the real response):

```json
{
  "parsingStatus": "SUCCESS",
  "parserVersion": "cv-parser-1.3",
  "profile": {
    "targetRole": "Data Engineer",
    "professionalSummary": "Synthetic profile summary",
    "technicalSkills": ["Python", "SQL"],
    "yearsOfExperience": null,
    "experience": [],
    "projects": [],
    "education": [],
    "languages": ["French"]
  },
  "professionalSummaryText": "Synthetic profile summary",
  "skillsText": "Python | SQL",
  "experienceText": null,
  "projectsText": null,
  "educationText": null,
  "semanticProfileText": "Target role: Data Engineer. Skills: Python, SQL.",
  "warnings": ["WORK_EXPERIENCE_NOT_FOUND"]
}
```

Example request:

```ts
const body = new FormData();
body.append('file', file, file.name);
return http.post<CvParseResponse>(`${apiBaseUrl}/api/cv/parse`, body);
```

Relevant error codes include `CV_FILE_REQUIRED` (400), `CV_FILE_EMPTY` (400), `CV_FORMAT_UNSUPPORTED` (415), `CV_DOCX_MACROS_NOT_ALLOWED` (415), `CV_PDF_ENCRYPTED` (422), `CV_PDF_NO_TEXT` (422), `CV_PDF_CORRUPTED` (422), `CV_DOCX_NO_TEXT` (422), `CV_DOCX_CORRUPTED` (422), `CV_FILE_TOO_LARGE` (413), `CV_PDF_TOO_MANY_PAGES` (413), and `CV_TEXT_TOO_LARGE` (413).

### 4.2 `GET /api/jobs`

Query parameters:

| Name | Type | Default | Validation |
|---|---|---|---|
| `page` | integer | `0` | minimum 0 |
| `size` | integer | `20` | 1-100 |
| `query` | string | absent | maximum 200 characters; title/company/description regex search |
| `location` | string | absent | maximum 120 characters |
| `source` | enum | absent | `ARBEITNOW`, `FRANCE_TRAVAIL`, `ADZUNA`, `LA_BONNE_ALTERNANCE` |
| `relevance` | enum | `DEFAULT` | `DEFAULT`, `ALL`, `IT_ONLY`, `NON_IT`, `UNCLASSIFIED`, `AMBIGUOUS` |

The public UI should normally use `DEFAULT` or `IT_ONLY`; do not expose non-IT/developer audit filters without product approval. Results are sorted by `publishedAt` descending in the backend.

```ts
interface JobPage {
  items: JobSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
interface JobSummary {
  id: string; title: string; company: string | null; location: string | null;
  remote: boolean; source: JobSource; skills: string[]; url: string | null;
  isItRelevant: boolean | null; matchedCategories: string[]; classificationVersion: string | null;
}
```

```json
{
  "items": [{
    "id": "job-1", "title": "Data Engineer", "company": "Example Company",
    "location": "Paris", "remote": false, "source": "ARBEITNOW",
    "skills": ["Python", "SQL"], "url": "https://example.test/job/1",
    "isItRelevant": true, "matchedCategories": ["data"],
    "classificationVersion": "it-classifier-v2"
  }],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

Errors: invalid query values return `400 VALIDATION_ERROR`; MongoDB failures return `500 DATABASE_UNAVAILABLE`.

### 4.3 `GET /api/jobs/{jobId}`

`jobId` length is 1-100. A missing record returns `404 RESOURCE_NOT_FOUND`.

`JobDetails` contains every `JobSummary` display field plus:

```ts
interface JobDetails {
  id: string; title: string; company: string | null; location: string | null;
  remote: boolean; source: JobSource; skills: string[]; url: string | null;
  description: string | null;
  requirements: { skills: string[] };
  employmentTypes: string[];
  publishedAt: string | null;
  isItRelevant: boolean | null;
  matchedCategories: string[];
  relevanceReason: string | null;
  classificationVersion: string | null;
  descriptionText: string | null;
  extractedSkills: Array<{ name: string; evidenceSources: string[] }>;
  toBeAcquiredSkills: Array<{ name: string; evidenceSources: string[] }>;
  normalizedLocation: null | {
    original: string | null; city: string | null; region: string | null;
    countryCode: string | null; workMode: 'REMOTE' | 'HYBRID' | 'ON_SITE' | 'UNKNOWN' | null;
    confidence: number | null;
  };
  enrichmentVersion: string | null;
}
```

Example additions to the summary fields:

```json
{
  "description": "Original provider description",
  "requirements": {"skills": ["Python", "SQL"]},
  "employmentTypes": ["FULL_TIME"],
  "publishedAt": "2026-11-01T09:00:00Z",
  "relevanceReason": "Strong IT-specific evidence",
  "descriptionText": "Cleaned provider description",
  "extractedSkills": [{"name": "Python", "evidenceSources": ["description"]}],
  "toBeAcquiredSkills": [],
  "normalizedLocation": {
    "original": "Paris", "city": "Paris", "region": null,
    "countryCode": "FR", "workMode": "ON_SITE", "confidence": 0.9
  },
  "enrichmentVersion": "job-enrichment-v1"
}
```

Render `descriptionText` when present and fall back to `description`. Treat descriptions as text; do not inject raw HTML with `innerHTML`.

### 4.4 `POST /api/recommendations/from-cv`

- Multipart field: `file`, with the same validation as CV parsing.
- Query: `topK`, default 10, range 1-50.
- Successful status: `200`.
- The endpoint parses the CV again; no `cvId` flow exists.

```ts
interface RecommendationResponse {
  parserVersion: string;
  modelVersion: string;
  parsingWarnings: string[];
  recommendations: Recommendation[];
}
interface Recommendation {
  jobId: string; jobTitle: string; company: string | null; location: string | null;
  source: string | null; jobUrl: string | null;
  matchScore: number; semanticScore: number; skillScore: number; roleScore: number;
  matchedSkills: string[]; missingSkills: string[]; explanation: string;
}
```

```json
{
  "parserVersion": "cv-parser-1.3",
  "modelVersion": "hybrid-multilingual-minilm-v2",
  "parsingWarnings": [],
  "recommendations": [{
    "jobId": "job-1", "jobTitle": "Data Engineer", "company": "Example Company",
    "location": "Paris", "source": "ARBEITNOW",
    "jobUrl": "https://example.test/job/1",
    "matchScore": 86.4, "semanticScore": 82.0, "skillScore": 100.0,
    "roleScore": 100.0, "matchedSkills": ["python", "sql"],
    "missingSkills": [],
    "explanation": "Matched skills: python, sql. Semantic profile alignment is strong. Role-family compatibility is strong."
  }]
}
```

Error mapping:

| Status/code | UI behavior |
|---|---|
| `404 NO_JOBS_AVAILABLE` | Empty-state panel; offer job search and retry later. |
| `503 AI_SERVICE_UNAVAILABLE` | Keep selected file in memory; show retry action. |
| `504 AI_SERVICE_TIMEOUT` | Explain that first model start can take longer; allow retry once. |
| `502 AI_SERVICE_FAILURE` / `AI_RESPONSE_INVALID` | Generic matching failure with trace ID; do not display internal details. |
| CV parsing codes | Reuse upload error messages. |
| `500 DATABASE_UNAVAILABLE` | Backend data unavailable; retry, then show support trace ID. |

The scores are 0-100 ranking signals, **not probabilities of being hired**.

### 4.5 Common error contract

All Spring errors use `application/problem+json`:

```ts
interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: Array<{ field: string; message: string }>;
  traceId: string;
}
```

Network/CORS failures do not have this body; the interceptor must create a separate client-side `NETWORK_UNAVAILABLE` representation.

## 5. Missing backend contracts

These are proposals only and must not be called until Person 1 implements and documents them.

| Required feature | Missing endpoint | Minimal proposed contract | Priority | Owner |
|---|---|---|---|---|
| Avoid uploading/parsing the same CV twice | `POST /api/cvs` plus recommendation by ID | Upload returns an opaque `cvId`; `POST /api/cvs/{cvId}/recommendations?topK=10` | High after V1 demo | Person 1 |
| Refresh-safe profile review/history | `GET /api/cvs/{cvId}` | Return metadata and the current structured profile, never raw file bytes by default | Medium | Person 1 |
| Explicit CV retention/deletion | `DELETE /api/cvs/{cvId}` | `204`; authenticated owner only | Required before persistence | Person 1 |
| Authentication/user profile | `/api/auth/*`, `/api/users/me` | Must be designed with authorization and ownership | Production blocker | Joint decision/Person 1 |
| Saved jobs | `/api/users/me/saved-jobs` | Authenticated CRUD by job ID | Low | Person 1 |
| Recommendation history | `GET /api/cvs/{cvId}/recommendations` | Paginated, versioned ranking snapshot | Low | Person 1 |
| Asynchronous progress | `/api/analysis/{id}` or SSE | Only if latency/UX requires persisted tasks | Low after caching | Person 1 |
| Recommendation filters | extend recommendation request | Country/remote/source filters applied before AI ranking | Medium | Joint contract |
| Avoid detail N+1 for recommendation metadata | extend `Recommendation` | Add `remote`, `publishedAt`, `employmentTypes` | Medium | Joint contract |

The cached warm request is currently sub-second locally, so asynchronous progress is not required for the first frontend implementation. The cold path can still take tens of seconds and needs clear loading/cancel UI.

## 6. End-to-end user journey

1. **Open dashboard** — `/`; optionally call `/actuator/health`. Show navigation even if health is unavailable.
2. **Choose CV** — `/cv`; accept `.pdf,.docx`, validate one non-empty file and `size <= 5 MiB` locally. Backend remains authoritative.
3. **Parse** — POST `/api/cv/parse`. Disable submit, show an indeterminate progress indicator, and prevent duplicate requests.
4. **Review profile** — navigate to `/cv/review`; display target role, summary, skills, experience, projects, education, languages, and warning badges. Unknown values remain “Not detected”; never guess.
5. **Request matches** — retain the same `File` object in memory and POST it to `/api/recommendations/from-cv?topK=10`. This unavoidable second parse is a current backend limitation.
6. **Display ranking** — `/recommendations`; retain response order. Do not client-sort by one component by default because that contradicts hybrid rank semantics.
7. **Inspect evidence** — expand a card to show semantic/skill/role components, matched skills, missing skills, explanation, parser warnings, and the “ranking signal” disclaimer.
8. **Open details** — navigate to `/jobs/:jobId` and GET `/api/jobs/{jobId}`. This is where publication date, normalized work mode, employment type, full description, and enrichment evidence are available.
9. **Apply externally** — allow only an `http:` or `https:` parsed URL. Open with `target="_blank" rel="noopener noreferrer"`.
10. **Browse alternatives** — `/jobs`; GET `/api/jobs` with debounced query/location and URL-synchronized page/filter parameters.
11. **Recover** — preserve the in-memory file for retry after timeout/service/network errors. A browser refresh intentionally clears sensitive state and returns the user to upload.

## 7. Recommendation UI specification

Each card should show rank, title, company, location, source, overall score, matched skills, missing skills, and explanation. Use a neutral score meter; avoid red/green “pass/fail” semantics. A disclosure can show semantic, skill, and role components.

Do not show publication date or remote status until job details are loaded because the recommendation DTO does not contain them. Do not issue ten detail requests automatically solely to fill cards; either show current fields or coordinate the proposed DTO extension.

Filtering recommendations client-side is acceptable only for display facets already in the returned ten. Keep an obvious “backend rank” label and a reset control. Do not recompute scores or claim that missing skills prove a candidate lacks them.

Empty state: “No recommendations were returned.” Distinguish it from `NO_JOBS_AVAILABLE`. The current backend normally returns a non-empty list when eligible jobs exist.

## 8. State and error strategy

Represent each remote state as a discriminated union: `idle | loading | success | empty | error`. Use `switchMap` or an `AbortController`-compatible request lifecycle so navigating away cancels stale UI subscriptions. Disable submission while a request is active.

The error interceptor should:

1. Detect `application/problem+json` and preserve `code`, `message`, `fieldErrors`, and `traceId`.
2. Map status `0` to a network/CORS error.
3. Never retry uploads automatically. Offer a user-controlled retry to avoid duplicate expensive work.
4. Never log the multipart body, filenames, parsed profile, or server response containing CV-derived data.
5. Show field errors near their controls and a shared error panel for service errors.

Timeout UI should not use a shorter client timeout than Spring’s 60-second AI read timeout plus upload/parser overhead. If a client timeout is introduced, use at least 75 seconds locally and make it configurable.

## 9. Security and privacy

- Accept only PDF/DOCX in the picker, but rely on backend magic/package validation.
- Never store CV files or profiles in browser persistence, error trackers, analytics, console logs, or URLs.
- No authentication/authorization exists. Do not present the application as multi-user secure.
- No Spring CORS configuration exists. Prefer Angular’s local proxy during development; production should use a same-origin gateway or an explicit narrow Spring CORS policy coordinated with Person 1.
- Frontend environment files contain public API origins only—never MongoDB credentials, provider keys, or AI secrets.
- Render job descriptions as text. Angular sanitization must not be bypassed.
- Validate external job links with the URL API and permit only HTTP(S).
- The current synchronous backend does not persist uploaded CVs. Consequently there is no retention/deletion control to display. If persistence is added, retention and authenticated deletion are prerequisites.

## 10. Testing strategy and acceptance criteria

### Unit/service tests

- Assert exact URLs, multipart field name, query parameters, and typed response mapping.
- Verify all `ApiError` codes and status-0 network mapping.
- Verify no manual `Content-Type` is set for `FormData`.
- Verify job filter serialization and zero-based pagination.

### Component tests

- Upload: PDF/DOCX accepted; empty, wrong extension, and >5 MiB rejected; duplicate submit disabled.
- Review: null fields and every warning render without fabricated values.
- Recommendations: backend order preserved; score disclaimer visible; matched/missing skills and explanation render; retry keeps file in memory.
- Job search: loading, page, empty, invalid filter, and server failure states.
- Job details: full/fallback description, 404, null metadata, and safe external link.

### Integration tests

Use `HttpTestingController` fixtures matching the Java records in this document. Add one integration environment against Spring—not FastAPI—for parse, jobs, job details, recommendation success, unavailable AI, and timeout responses.

### End-to-end tests

Use a synthetic PDF/DOCX with no personal data:

1. Upload and review structured fields.
2. Request recommendations and assert ten ordered cards or the fixture count.
3. Open a job detail and return.
4. Search/filter jobs and paginate.
5. Exercise unsupported file, empty file, AI unavailable, timeout, no jobs, and network-disconnected states.

Major-page acceptance:

- Dashboard remains usable with health unavailable.
- Upload never transmits an invalid local file and correctly displays backend validation.
- Review renders every structured profile collection and warning.
- Recommendations preserve rank, display evidence, contain no probability language, and recover from service errors.
- Jobs preserve URL filter state and use backend pagination.
- Details never inject raw HTML and external links are safe.

## 11. Person 2 roadmap

### Phase A — foundation

- Generate the Angular workspace in `frontend/`; add standalone routing and application shell.
- Create environment/API configuration, local `/api` proxy to Spring `:8080`, exact shared models, and error interceptor.
- Add health service only if the dashboard displays availability.
- Acceptance: app builds/tests; navigation works; no secrets; no direct FastAPI URL.
- Coordination: agree Angular version and dev-origin/CORS strategy with Person 1.

### Phase B — CV upload and analysis

- Create file validation utility, `CvApiService`, upload, review, and feature signal store.
- Implement every parse error and warning state.
- Acceptance: synthetic PDF and DOCX parse; fields are accurate; refresh safely returns to upload; no browser persistence.
- Blocker: none for synchronous V1.

### Phase C — job search/details

- Create `JobApiService`, search form, URL query state, paginator, summary cards, and detail page.
- Acceptance: all implemented filters/enums work; details/fallback text/404 are correct; external link is safe.
- Coordination: ask Person 1 before exposing `ALL`, `NON_IT`, or developer-oriented relevance filters.

### Phase D — recommendations

- Create recommendation service/page/cards, component score display, evidence chips, disclaimer, retry, and job-detail navigation.
- Reuse the retained `File`; do not invent a `cvId`.
- Acceptance: ordered results match backend; all score/evidence fields render; timeout/unavailable/no-jobs states are recoverable.
- Coordination: decide whether `remote` and `publishedAt` justify extending the recommendation DTO.

### Phase E — UX and resilience

- Accessibility, responsive layouts, keyboard/focus management, skeletons, cancellation, duplicate-submit protection, and consistent errors.
- Acceptance: WCAG-oriented labels/focus; no sensitive logs; mobile and desktop layouts; network recovery works.

### Phase F — integrated QA

- Run component/service suites and synthetic end-to-end journey against Spring/Mongo/FastAPI.
- Validate the cold loading state and warm sub-second flow separately.
- Deliver screenshots/test report and list any proposed backend contracts still blocked.

Do not implement authentication, SSE, CV persistence, or saved jobs in the frontend until their backend contracts exist and ownership/privacy rules are agreed.
