# Person 2 frontend handoff

This is the verified integration handoff for the Angular developer. The complete component tree, TypeScript interfaces, error matrix, security notes, and phased implementation roadmap are in `frontend-end-to-end-implementation-plan.md`.

## 1. What exists now

The backend currently supports:

- Synchronous PDF/DOCX CV parsing and structured candidate profiles.
- Paginated job search and job details from MongoDB.
- Synchronous CV-to-job recommendations using real multilingual MiniLM embeddings.
- Overall, semantic, skill, and role scores from 0 to 100.
- Evidence-backed matched/missing skills and deterministic explanations.
- Safe `application/problem+json` errors.

The Angular application has **not** been generated. `frontend/` currently contains only a README. There is no authentication, CV persistence, recommendation history, SSE/progress API, saved jobs, or user profile API.

The browser must call Spring Boot only. `http://127.0.0.1:8000/match` is an internal Spring-to-FastAPI contract.

## 2. Local startup

Prerequisites: Docker Desktop, Java 21, the committed Maven wrapper, Python 3.12, and the existing `ai-service/.venv`. Do not put credentials in frontend environment files.

### MongoDB

```powershell
Set-Location C:\CV_analyzer
docker compose up -d mongodb
docker compose ps
```

Expected binding: `127.0.0.1:27018`. Do not run ingestion for frontend development; `JOB_INGESTION_ENABLED` defaults to false.

### FastAPI

```powershell
Set-Location C:\CV_analyzer\ai-service
$env:HF_HUB_OFFLINE='1'
$env:TRANSFORMERS_OFFLINE='1'
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Check:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

### Spring Boot

```powershell
Set-Location C:\CV_analyzer\backend
$env:SPRING_PROFILES_ACTIVE='local'
$env:JOB_INGESTION_ENABLED='false'
.\mvnw.cmd spring-boot:run
```

Check:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

For Angular development, prefer a dev proxy from `/api` to `http://localhost:8080`; Spring currently has no CORS configuration.

## 3. Existing browser-facing API

| Method | Endpoint | Request | Successful response |
|---|---|---|---|
| GET | `/actuator/health` | none | `{ "status": "UP" }` |
| POST | `/api/cv/parse` | multipart `file` | `CvParseResponse` |
| GET | `/api/jobs` | query parameters | paginated job summaries |
| GET | `/api/jobs/{jobId}` | path ID | job details |
| POST | `/api/recommendations/from-cv?topK=10` | multipart `file` | ranked recommendation response |

There is no `/api/cvs`, `/api/auth`, `/api/users`, persisted `/api/recommendations`, or `/api/analysis` route. The routes in `api-contract.yaml` marked proposed are not implemented.

## 4. CV upload and parser response

Accepted files:

- `.pdf` with valid PDF magic and selectable text.
- `.docx` with a valid non-macro Office package.
- Maximum 5 MiB; maximum 100 PDF pages.

Angular request:

```ts
const body = new FormData();
body.append('file', file, file.name);
return this.http.post<CvParseResponse>('/api/cv/parse', body);
```

Do not manually set the multipart `Content-Type`; the browser must generate its boundary.

Example response:

```json
{
  "parsingStatus": "SUCCESS",
  "parserVersion": "cv-parser-1.3",
  "profile": {
    "targetRole": "Data Engineer",
    "professionalSummary": "Synthetic summary",
    "technicalSkills": ["Python", "SQL"],
    "yearsOfExperience": null,
    "experience": [],
    "projects": [],
    "education": [],
    "languages": ["French"]
  },
  "professionalSummaryText": "Synthetic summary",
  "skillsText": "Python | SQL",
  "experienceText": null,
  "projectsText": null,
  "educationText": null,
  "semanticProfileText": "Target role: Data Engineer. Skills: Python, SQL.",
  "warnings": ["WORK_EXPERIENCE_NOT_FOUND"]
}
```

Render `null` and parser warnings as “not reliably detected”; never infer missing information in the UI.

## 5. Current duplicate-upload limitation

CV parsing and recommendation are two independent synchronous endpoints. There is no persisted `cvId`.

The current frontend flow must:

1. Keep the selected `File` in an in-memory feature store.
2. POST it to `/api/cv/parse` for review.
3. Keep the same `File` object while navigating to review.
4. POST it again to `/api/recommendations/from-cv` when the user requests matches.

Do not put the file or parsed profile in local storage, session storage, URLs, browser logs, analytics, or error tracking. A browser refresh should safely return the user to the upload page.

## 6. Recommendation contract

Request:

```ts
const body = new FormData();
body.append('file', file, file.name);
return this.http.post<RecommendationResponse>(
  '/api/recommendations/from-cv?topK=10', body
);
```

`topK` defaults to 10 and accepts 1–50.

Example response:

```json
{
  "parserVersion": "cv-parser-1.3",
  "modelVersion": "hybrid-multilingual-minilm-v2",
  "parsingWarnings": [],
  "recommendations": [{
    "jobId": "job-1",
    "jobTitle": "Data Engineer",
    "company": "Example Company",
    "location": "Paris",
    "source": "ARBEITNOW",
    "jobUrl": "https://example.test/job/1",
    "matchScore": 86.4,
    "semanticScore": 82.0,
    "skillScore": 100.0,
    "roleScore": 100.0,
    "matchedSkills": ["python", "sql"],
    "missingSkills": [],
    "explanation": "Matched skills: python, sql. Semantic profile alignment is strong. Role-family compatibility is strong."
  }]
}
```

Preserve backend order. The overall score combines semantic, skill and role evidence; it is **not a probability of being hired**. Missing skills mean “not identified in the parsed CV,” not proof that the candidate lacks them.

Recommended card content:

- Backend rank and overall score.
- Job title, company, location, source.
- Matched/missing skill chips.
- Explanation.
- Expandable semantic/skill/role component scores.
- Link to `/jobs/:jobId` for authoritative details.
- Safe external apply link with `target="_blank" rel="noopener noreferrer"` after HTTP(S) URL validation.

The recommendation response does not contain publication date, employment type, remote status, or full description. Load those only on the job-details page; do not automatically make ten detail requests just to decorate cards.

## 7. Job search and details

`GET /api/jobs` parameters:

| Parameter | Default | Rules |
|---|---:|---|
| `page` | 0 | zero-based, minimum 0 |
| `size` | 20 | 1–100 |
| `query` | absent | maximum 200 characters |
| `location` | absent | maximum 120 characters |
| `source` | absent | `ARBEITNOW`, `FRANCE_TRAVAIL`, `ADZUNA`, `LA_BONNE_ALTERNANCE` |
| `relevance` | `DEFAULT` | `DEFAULT`, `ALL`, `IT_ONLY`, `NON_IT`, `UNCLASSIFIED`, `AMBIGUOUS` |

The end-user UI should normally use `DEFAULT` or `IT_ONLY`.

Response:

```json
{
  "items": [{
    "id": "job-1",
    "title": "Data Engineer",
    "company": "Example Company",
    "location": "Paris",
    "remote": false,
    "source": "ARBEITNOW",
    "skills": ["Python", "SQL"],
    "url": "https://example.test/job/1",
    "isItRelevant": true,
    "matchedCategories": ["data"],
    "classificationVersion": "it-classifier-v2"
  }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`GET /api/jobs/{jobId}` additionally returns original/cleaned descriptions, requirements, employment types, publication date, classification reason, extracted/acquired skills with evidence, normalized location/work mode, and enrichment version. Render descriptions as text; never bypass Angular sanitization with raw provider HTML.

## 8. Error handling

Spring errors use `application/problem+json`:

```json
{
  "timestamp": "2026-11-14T12:00:00Z",
  "status": 503,
  "code": "AI_SERVICE_UNAVAILABLE",
  "message": "AI matching service is unavailable",
  "path": "/api/recommendations/from-cv",
  "fieldErrors": [],
  "traceId": "abcd1234"
}
```

Important UI cases:

| Code/status | UI behavior |
|---|---|
| `CV_FILE_REQUIRED`, `CV_FILE_EMPTY` / 400 | File validation message |
| `CV_FORMAT_UNSUPPORTED`, `CV_DOCX_MACROS_NOT_ALLOWED` / 415 | Explain accepted PDF/DOCX formats |
| `CV_PDF_ENCRYPTED`, `CV_PDF_NO_TEXT`, corrupt PDF/DOCX / 422 | Ask for an unencrypted text-based file |
| size/page/text limits / 413 | Show configured limit |
| `NO_JOBS_AVAILABLE` / 404 | Recommendation empty state |
| `AI_SERVICE_UNAVAILABLE` / 503 | Keep in-memory file and offer retry |
| `AI_SERVICE_TIMEOUT` / 504 | Explain cold startup and offer one manual retry |
| `AI_SERVICE_FAILURE`, `AI_RESPONSE_INVALID` / 502 | Generic error plus trace ID |
| `DATABASE_UNAVAILABLE` / 500 | Data unavailable plus retry/trace ID |
| HTTP status 0 | Network/CORS failure |

Never automatically retry multipart requests. Disable duplicate submission while a request is active. A frontend timeout, if added, must exceed Spring’s 60-second AI timeout plus upload/parser time; use at least 75 seconds locally.

## 9. Suggested Angular pages

- `/` — dashboard and optional backend availability.
- `/cv` — file selection/upload.
- `/cv/review` — structured profile and warnings.
- `/recommendations` — ranked cards, evidence and retry.
- `/jobs` — filters, pagination and empty state.
- `/jobs/:jobId` — authoritative job details and safe external link.

Use standalone components, typed reactive forms, `HttpClient`, and feature-scoped signals. NgRx is unnecessary for this synchronous MVP.

## 10. Privacy and security

- No authentication or authorization exists; do not claim multi-user security.
- Use an Angular proxy or same-origin deployment until a narrow CORS policy is implemented.
- Frontend environment files contain only public API origins—never MongoDB/provider credentials.
- Never log or persist CV files, structured profiles, or recommendation explanations in client telemetry.
- CVs are currently processed in memory and are not persisted by Spring.
- If CV persistence is added later, authenticated ownership, retention and deletion contracts are prerequisites.

## 11. Integration acceptance criteria

- PDF and DOCX validation works locally and backend validation errors render correctly.
- Profile review displays all structured sections, nulls and warnings without invented values.
- Recommendation results preserve backend rank and show the non-probability disclaimer.
- Matched and missing skills plus explanations are visible and accessible.
- AI unavailable, timeout, no jobs, validation, database, and network states have retry/empty/error UI.
- Job search synchronizes filters/page with the URL and uses backend pagination.
- Job descriptions are rendered safely and external links accept only HTTP(S).
- No browser call goes directly to FastAPI.
- No CV-derived content enters persistent browser storage or logs.
- Unit, service, routing, loading/error component, and synthetic end-to-end tests pass.

For the exact proposed Angular arborescence and implementation phases, continue with `frontend-end-to-end-implementation-plan.md`.
