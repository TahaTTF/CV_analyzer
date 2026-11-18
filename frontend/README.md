# CV Analyzer: Frontend

Angular client for the AI CV Analyzer & Job Recommendation Platform. It lets a candidate upload a CV, review the extracted profile, and see ranked, explainable IT job recommendations.

The frontend talks **only to Spring Boot** (port 8080). It never calls the FastAPI AI service directly.

## Status

| Area | Status |
|---|---|
| Angular workspace, SCSS, dev proxy to Spring Boot | Done |
| Typed models matching the backend DTOs | Done |
| API services (CV, recommendations, jobs, health) | Done |
| Error interceptor (problem+json, network and gateway errors) | Done |
| In-memory CV store (signals) and route guards | Done |
| Design system (dark theme, glass cards, buttons, chips, badges) | Done |
| App shell (sticky header, responsive navigation, footer) | Done |
| Home page `/` with live backend status badge | Done |
| CV upload `/cv` (drag and drop, validation, progress, cancel, errors) | Done |
| Profile review `/cv/review` (verified against a real CV) | Done |
| Recommendations `/recommendations` (cards, score meter, skill chips, breakdown, retry) | Done, error path verified; ranked cards need jobs in MongoDB and the AI service |
| Job search `/jobs` and job details `/jobs/:jobId` | Next |
| Unit, component and end-to-end tests | Planned |

## Run locally

Start the services in this order, each in its own terminal.

```powershell
# 1. MongoDB (repository root)
docker compose up -d mongodb

# 2. Spring Boot (Java 21)
cd backend
$env:SPRING_PROFILES_ACTIVE='local'
$env:JOB_INGESTION_ENABLED='false'
.\mvnw.cmd spring-boot:run

# 3. FastAPI AI service (needed for recommendations, see ai-service/README.md)

# 4. Angular
cd frontend
npm install
ng serve
```

Open http://localhost:4200. The badge on the home page turns green when Spring Boot is reachable.

- Parsing a CV needs only Spring Boot and MongoDB.
- Recommendations also need jobs in MongoDB and the FastAPI service on port 8000. With an empty database the page shows the `NO_JOBS_AVAILABLE` error panel, which is expected.

## How the frontend reaches the backend

`proxy.conf.json` forwards `/api` and `/actuator` from the Angular dev server to `http://localhost:8080`, so there are no CORS problems in development. The proxy is enabled in `angular.json` under `serve > options > proxyConfig`.

## Structure

```
src/app/
  core/
    config/api.config.ts            API base (empty: same origin via proxy)
    interceptors/                   problem+json and network error mapping
    services/                       cv, recommendation, job and health API services
  shared/
    components/
      error-panel/                  reusable error display with retry
      skill-chips/                  default, matched and missing skill chips
      score-meter/                  accessible 0-100 meter
    models/                         TypeScript interfaces for the backend DTOs
    utils/                          file validation, safe URL check, error and warning messages
  features/
    dashboard/                      home page
    cv-analysis/                    upload page, profile review page, in-memory store
    recommendations/                recommendation page and card
```

## Pages and backend endpoints

| Route | Page | Backend endpoint |
|---|---|---|
| `/` | Home | `GET /actuator/health` |
| `/cv` | CV upload | `POST /api/cv/parse` (multipart field `file`) |
| `/cv/review` | Profile review | none (in-memory result) |
| `/recommendations` | Ranked jobs | `POST /api/recommendations/from-cv?topK=10` |
| `/jobs` | Job search (planned) | `GET /api/jobs` |
| `/jobs/:jobId` | Job details (planned) | `GET /api/jobs/{jobId}` |

The routes in `docs/api-contract.yaml` are an old proposal and are **not** used. The verified contract is `docs/person2-frontend-handoff.md` and `docs/frontend-end-to-end-implementation-plan.md`.

## Behavior and privacy rules

- The selected CV file and the parsed profile live **in memory only** (`CvAnalysisStore`).
- Nothing CV-related goes to localStorage, sessionStorage, URLs, analytics or console logs.
- A page refresh clears the CV; guards send the user back to `/cv` with a message.
- Missing parser fields are shown as "Not detected"; the UI never guesses values.
- Recommendations keep the backend order. The score is a ranking signal, not a hiring probability, and "not identified" skills are not proof the candidate lacks them.
- External apply links are accepted only for http(s) URLs and open with `rel="noopener noreferrer"`.
- Requests are never retried automatically, and duplicate submissions are blocked while one is running.

## Design

Dark "midnight" theme with purple and blue accents and frosted-glass cards, based on a Dribbble reference. Design tokens (colors, radius, font) are CSS variables in `src/styles.scss`. The font is Inter.

## Known limitations

- There is no login: the backend has no authentication.
- Parse and recommend are two separate requests, so the same file is sent twice (no `cvId` exists yet).
- The first recommendation request can take tens of seconds while the AI model starts.
- Scanned (image-only) PDFs are rejected because the backend has no OCR.
- The Angular project is still named `frontend-tmp` in `angular.json` and `package.json` (cosmetic).

## Useful commands

```powershell
ng serve        # development server on http://localhost:4200
ng build        # production build
ng test         # unit tests (to be written)
```