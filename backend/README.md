# CV Analyzer backend

Spring Boot 3.5 / Java 21 service for normalized job persistence, guarded multi-provider ingestion, offline enrichment, PDF/DOCX CV parsing, and explainable AI job recommendations through the internal FastAPI service.

## Prerequisites

- Java 21
- Docker Desktop with Docker Compose
- PowerShell

The Maven Wrapper is committed, so a global Maven installation is optional.

## Start locally

From the repository root:

~~~powershell
Copy-Item .env.example .env
docker compose up -d mongodb
docker compose ps
Set-Location backend
$env:MAVEN_OPTS = "-Xms128m -Xmx512m"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.jvmArguments=-Xms128m -Xmx512m"
~~~

Docker Compose reads the root .env file automatically and publishes MongoDB on loopback port 27018 to avoid common Windows/WSL conflicts on 27017. Spring Boot reads process environment variables, so export any non-default value in the PowerShell window before starting it, for example `$env:MONGODB_URI = "mongodb://..."`. Keep real credentials out of Git. The main variables are MONGODB_URI, MONGODB_DATABASE, BACKEND_PORT, ARBEITNOW_API_URL, and ARBEITNOW_MAX_PAGES. The default one-page limit avoids accidentally abusing the free upstream API.

The memory options above bound both the Maven launcher and the forked Spring Boot JVM. This is useful on Windows when Docker Desktop, WSL, VS Code, and Java language servers leave little physical RAM available. They are process-local PowerShell settings; they do not change the garbage collector, Windows page file, or system-wide Java configuration. Close the PowerShell window or run `Remove-Item Env:MAVEN_OPTS` to clear the Maven setting.

Check application and MongoDB health:

~~~powershell
Invoke-RestMethod http://localhost:8080/actuator/health
~~~

## Public API

~~~text
POST /api/cv/parse
GET  /api/jobs
GET  /api/jobs/{jobId}
POST /api/recommendations/from-cv?topK=10
GET  /actuator/health
~~~

CV endpoints accept multipart field `file`. Supported files are text-based PDF and DOCX up to 5 MiB. Recommendation requests require FastAPI on port 8000; see `../ai-service/README.md`.

## Ingest and read jobs

The ingestion trigger exists only when the local Spring profile is active:

~~~powershell
Invoke-RestMethod -Method Post http://localhost:8080/api/dev/ingestion/arbeitnow
Invoke-RestMethod "http://localhost:8080/api/jobs?page=0&size=20"
Invoke-RestMethod "http://localhost:8080/api/jobs?query=java&location=Berlin&source=ARBEITNOW"
Invoke-RestMethod http://localhost:8080/api/jobs/JOB_ID
~~~

The ingestion result reports fetched, inserted, updated, skipped, and failed. Repeating the same ingestion is idempotent by the unique (source, externalId) index and content comparison. Incomplete source records are counted as failed; database and provider outages return safe structured errors.

## MongoDB Compass or VS Code

With the example local credentials, connect Compass to:

~~~text
mongodb://cv_analyzer:change-me@localhost:27018/cv_analyzer?authSource=admin
~~~

Open the cv_analyzer database and jobs collection. The service creates source_external_id_unique, status_fetched_at, and publishedAt indexes. Change the example password for any non-local environment.

The runnable Postman collection is documented in `../postman/README.md`.

## Tests and build

~~~powershell
Set-Location backend
$env:MAVEN_OPTS = "-Xms128m -Xmx512m"
.\mvnw.cmd test "-DargLine=-Xms64m -Xmx384m"
.\mvnw.cmd clean package "-DargLine=-Xms64m -Xmx384m"
~~~

Unit, mocked HTTP, and MVC tests run without external services. Run the repository integration test explicitly with:

~~~powershell
.\mvnw.cmd "-Dtest=JobRepositoryIT" test
~~~

It uses Testcontainers and requires Docker plus access to download the MongoDB and Testcontainers helper images.

## Implemented architecture

- controller: public CV/job/recommendation endpoints plus local developer routes
- cv: layout-aware PDF/DOCX extraction and candidate profile composition
- service: paginated MongoDB querying
- ingestion: provider clients, validation, normalization, classification, and idempotent persistence
- enrichment: offline description cleaning, skill extraction, and location normalization
- recommendation: HTTP/1.1 FastAPI orchestration and authoritative job response mapping
- model/repository: MongoDB job document and indexed repository
- exception: consistent `application/problem+json` errors

Original provider descriptions are retained for traceability while offline enrichment stores cleaned text and evidence-backed required/acquired skills separately.

## Milestone 1.5 provider safety and classification

Manual local routes now support Arbeitnow, Adzuna, and La bonne alternance:

~~~text
POST /api/dev/ingestion/arbeitnow
POST /api/dev/ingestion/adzuna
POST /api/dev/ingestion/la-bonne-alternance
~~~

All three return HTTP 403 unless JOB_INGESTION_ENABLED=true was set before startup. Each
provider has a separate request budget; Adzuna defaults to one request and cannot be configured
above 25. Credentials are checked only when a provider is deliberately invoked.

The offline-only POST /api/dev/ingestion/classify-existing operation classifies legacy jobs that
do not yet have classification metadata. It writes MongoDB but makes no provider call, and it is
never run automatically. GET /api/jobs includes unclassified legacy records until this operation
has completed. Use relevance=ALL, IT_ONLY, NON_IT, UNCLASSIFIED, or AMBIGUOUS to audit results.

Provider provenance and classification evidence are stored with each new job. The
status_it_published_at index supports the default relevant-job query. See
../docs/milestone-1.5-job-ingestion.md for request shapes, mapping details, and the planned
offline enrichment boundary.
