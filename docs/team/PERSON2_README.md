# Person 2 development guide

**Role:** Frontend & Real-Time Collaboration Engineer

Frontend work should begin against approved OpenAPI examples and mock adapters; it does not need to wait for the AI service. Keep mock and live implementations behind the same TypeScript interfaces.

## Responsibility plan

### 1. Initialize the Angular application

- **Objective:** Create a reproducible Angular workspace with routing, testing, formatting, and strict TypeScript.
- **Files/modules:** `frontend/package.json`, Angular workspace config, `src/`, test configuration.
- **Dependencies:** Approved Node/Angular versions and browser support.
- **Deliverable:** Bootable empty application with pinned dependencies and documented commands.
- **Verify:** Clean install, build, lint, test, and local serve all succeed.

### 2. Define the frontend folder architecture

- **Objective:** Separate core infrastructure, shared UI, and feature-owned code.
- **Files/modules:** `src/app/core`, `shared`, `features/cv`, `features/jobs`, `features/recommendations`.
- **Dependencies:** Angular scaffold and planned routes/state approach.
- **Deliverable:** Documented boundaries with lazy routes and no circular dependencies.
- **Verify:** Architecture/lint rules pass and a sample feature loads through its public boundary.

### 3. Create shared layouts and reusable UI components

- **Objective:** Establish accessible, responsive visual primitives without feature coupling.
- **Files/modules:** shared layout, buttons, cards, badges, feedback components, styles/tokens.
- **Dependencies:** Agreed lightweight design direction and accessibility baseline.
- **Deliverable:** Application shell and reusable components with documented inputs/states.
- **Verify:** Component tests cover behavior; keyboard, focus, contrast, and mobile layouts are checked.

### 4. Create the CV upload interface

- **Objective:** Let users choose one PDF, validate it, upload it, and understand consent/error states.
- **Files/modules:** `features/cv/upload`, file validator, upload service facade.
- **Dependencies:** Proposed multipart contract and agreed size limit.
- **Deliverable:** Accessible picker/drop zone with filename, validation, progress, cancel/retry behavior as supported.
- **Verify:** Tests cover PDF/non-PDF, limits, keyboard use, success, network error, and retry.

### 5. Create candidate profile visualization

- **Objective:** Clearly present extracted skills, experience, education, languages, and uncertain/missing data.
- **Files/modules:** `features/cv/profile`, profile view model and components.
- **Dependencies:** `CandidateProfile` schema and synthetic examples.
- **Deliverable:** Responsive profile view that handles partial results without inventing fields.
- **Verify:** Component tests render full, partial, empty, and long-content fixtures.

### 6. Create recommendation cards and list

- **Objective:** Show ranked jobs with useful scanability and source links.
- **Files/modules:** `features/recommendations/list`, card, pagination/filter controls.
- **Dependencies:** Recommendation and job summary schemas.
- **Deliverable:** Responsive, paginated recommendation list with safe external links.
- **Verify:** Tests preserve rank/order, navigation, pagination, and unusual text lengths.

### 7. Display compatibility scores and matched/missing skills

- **Objective:** Explain the score without presenting it as certainty.
- **Files/modules:** score component, skill chips/list, explanation copy and formatting utilities.
- **Dependencies:** Agreed 0–1 score semantics and backend evidence fields.
- **Deliverable:** Consistent percentage/component display with matched and missing requirements.
- **Verify:** Boundary values, rounding, absent components, accessibility labels, and explanatory copy are tested.

### 8. Implement frontend API services

- **Objective:** Centralize typed HTTP, pagination, errors, cancellation, and base-URL configuration.
- **Files/modules:** `core/api`, generated or hand-maintained contract types, interceptors, feature facades.
- **Dependencies:** Approved OpenAPI contract and Angular HTTP setup.
- **Deliverable:** Typed clients that return domain-facing models rather than raw transport responses.
- **Verify:** HTTP tests assert URL, method, multipart body, query parameters, mapping, and error translation.

### 9. Start with mock API responses

- **Objective:** Develop all primary screens independently of backend/AI readiness.
- **Files/modules:** `frontend/src/mocks` or dev mock adapter, synthetic fixtures based on OpenAPI examples.
- **Dependencies:** Stable TypeScript interfaces and approved example payloads.
- **Deliverable:** Environment-switchable mock implementation supporting success, slow, empty, and failure scenarios.
- **Verify:** Run the full UI journey offline; contract tests reject fixtures that drift from the OpenAPI schema.

### 10. Implement client-side live analysis progress

- **Objective:** Consume SSE and show honest progress through terminal state.
- **Files/modules:** analysis event service, progress store/component, reconnect and cleanup logic.
- **Dependencies:** Approved SSE event format and mock event stream.
- **Deliverable:** EventSource-based client handling progress, reconnect, duplicate events, completion, failure, and navigation cleanup.
- **Verify:** Tests simulate event sequences/disconnects; browser test confirms no leaked connection and final resource refresh.

### 11. Integrate with Spring Boot endpoints

- **Objective:** Replace mocks selectively without changing feature components.
- **Files/modules:** environment configuration, API adapters/facades, integration scenarios.
- **Dependencies:** Running contract-conformant backend, CORS/security agreement.
- **Deliverable:** Upload, CV/profile, jobs, recommendations, and SSE use the real backend in integration mode.
- **Verify:** Compare network traffic with OpenAPI and complete happy/error paths without mock handlers.

### 12. Implement loading, empty, and error states

- **Objective:** Make every asynchronous screen understandable and recoverable.
- **Files/modules:** shared feedback components and each feature container.
- **Dependencies:** API error mapping and UX language.
- **Deliverable:** Context-specific skeleton/loading, empty, validation, offline, server-error, and retry states.
- **Verify:** Component/E2E tests force each state; focus and announcements work for assistive technology.

### 13. Test frontend components and user workflows

- **Objective:** Protect UI behavior and the main candidate journey.
- **Files/modules:** component tests, API tests, E2E specs, accessibility checks and synthetic fixtures.
- **Dependencies:** Stable feature flows and selected test runner.
- **Deliverable:** Fast component suite and focused E2E coverage for upload-to-recommendations.
- **Verify:** Run from a clean checkout in headed/headless modes and confirm deterministic results.

### 14. Assist with Postman collections, fixtures, and E2E testing

- **Objective:** Share ownership of contract verification and integration quality.
- **Files/modules:** `tests/` Postman collection/environment, synthetic PDFs/jobs, E2E runbook.
- **Dependencies:** OpenAPI examples, local services, privacy-safe data.
- **Deliverable:** Reusable requests and synthetic fixtures for success and documented failures.
- **Verify:** Import/run the collection against local backend and execute the jointly agreed demo scenario.

## First three development days

### Day 1 — contract review and Angular foundation

- [ ] Review OpenAPI payloads, score semantics, SSE events, and error needs with Person 1.
- [ ] Agree Node and Angular versions, then scaffold strict Angular with routing and tests.
- [ ] Establish core/shared/features boundaries and environment-based API configuration.
- [ ] Add the application shell and one accessible shared feedback component.
- [ ] Commit the frontend foundation to `dev/person2-frontend` and open a small pull request.

### Day 2 — mock-first candidate journey

- [ ] Create TypeScript models from the agreed contract and synthetic fixtures.
- [ ] Add a mock adapter selectable through development configuration.
- [ ] Build PDF selection/validation and mocked upload acceptance.
- [ ] Build analysis progress UI driven by a timed mock SSE-equivalent stream.
- [ ] Test success, invalid file, slow processing, and failed analysis states.

### Day 3 — results, quality, and backend handoff

- [ ] Build candidate profile and recommendation list/card components.
- [ ] Add scores, matched/missing skills, pagination, loading, empty, and retry states.
- [ ] Add HTTP client tests for all proposed endpoints and SSE lifecycle tests.
- [ ] Export or review Postman requests and fixtures with Person 1.
- [ ] Demo the complete mock journey and agree the first real-backend integration checkpoint.

