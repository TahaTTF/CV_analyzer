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
| In-memory CV store (signals) | Done |
| Design system (dark theme, glass cards, buttons, chips, badges) | Done |
| App shell (sticky header, responsive navigation, footer) | Done |
| Home page `/` with live backend status badge | Done |
| CV upload page `/cv` (drag and drop, validation, progress, cancel, error panel) | Done |
| Profile review page `/cv/review` | Next |
| Recommendations page `/recommendations` | Planned |
| Job search `/jobs` and job details `/jobs/:jobId` | Planned |
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

# 3. Angular
cd frontend
npm install
ng serve
```

Open http://localhost:4200. The badge on the home page turns green when Spring Boot is reachable.

Parsing a CV needs only Spring Boot and MongoDB. Recommendations additionally need the FastAPI service on port 8000 (see `ai-service/README.md`).

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
    components/error-panel/         reusable error display with retry
    models/                         TypeScript interfaces for the backend DTOs
    utils/                          file validation, safe URL check, error messages
  features/
    dashboard/                      home page
    cv-analysis/                    CV upload page and in-memory store
```

## Backend endpoints used

| Method | Endpoint | Used by |
|---|---|---|
| GET | `/actuator/health` | Home page status badge |
| POST | `/api/cv/parse` | CV upload (multipart field `file`) |
| POST | `/api/recommendations/from-cv?topK=10` | Recommendations (planned) |
| GET | `/api/jobs` | Job search (planned) |
| GET | `/api/jobs/{jobId}` | Job details (planned) |

The routes in `docs/api-contract.yaml` are an old proposal and are **not** used. The verified contract is `docs/person2-frontend-handoff.md` and `docs/frontend-end-to-end-implementation-plan.md`.

## Privacy rules

- The selected CV file and the parsed profile live **in memory only** (`CvAnalysisStore`).
- Nothing CV-related goes to localStorage, sessionStorage, URLs, analytics or console logs.
- A page refresh clears the CV and returns the user to the upload page.
- The backend does not store CVs either.

## Design

Dark "midnight" theme with purple and blue accents and frosted-glass cards, based on a Dribbble reference. Design tokens (colors, radius, font) are CSS variables in `src/styles.scss`. The font is Inter.

## Known limitations

- There is no login: the backend has no authentication.
- Parse and recommend are two separate requests, so the same file is sent twice (no `cvId` exists yet).
- The first recommendation request can take tens of seconds while the AI model starts.
- The Angular project is still named `frontend-tmp` in `angular.json` and `package.json` (cosmetic).

## Useful commands

```powershell
ng serve        # development server on http://localhost:4200
ng build        # production build
ng test         # unit tests (to be written)
```