# Local setup guide

MongoDB infrastructure and the Spring Boot job-ingestion backend are implemented. FastAPI and Angular commands remain future work.

## Prerequisites

- Git
- Docker Desktop and Docker Compose
- Backend: Java 21; use the committed Maven Wrapper
- Future AI service: Python 3.11+
- Future frontend: a project-supported Node.js LTS release and npm

## Current setup

1. Clone the repository and switch to your development branch.
2. Copy `.env.example` to `.env` and replace local-only placeholder credentials.
3. Start MongoDB:

   ```bash
   docker compose up -d mongodb
   docker compose ps
   ```

4. Stop it with `docker compose down`. Add `-v` only when intentionally deleting local database data.

The Compose MongoDB credentials are intended only for local development. Atlas environments should use a separately provisioned least-privilege application user and secret management.

## Run the backend on Windows

~~~powershell
Copy-Item .env.example .env
docker compose up -d mongodb
Set-Location backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
~~~

Compose reads the root `.env` file and binds the container's MongoDB port 27017 to Windows loopback port 27018. For non-default backend settings, assign the corresponding PowerShell process variables (for example, `$env:MONGODB_URI = "mongodb://..."`) before starting Spring Boot.

In another PowerShell window:

~~~powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod -Method Post http://localhost:8080/api/dev/ingestion/arbeitnow
Invoke-RestMethod "http://localhost:8080/api/jobs?page=0&size=20"
~~~

Run tests with `.\mvnw.cmd test` from the backend directory. The Testcontainers repository test requires a working Docker daemon.

## Inspect with MongoDB Compass

Use `mongodb://cv_analyzer:change-me@localhost:27018/cv_analyzer?authSource=admin` with the example configuration, then open the `cv_analyzer.jobs` collection. Replace the example credentials outside local development.

## Planned services

Angular will use port `4200` and FastAPI will use `${AI_SERVICE_PORT:-8000}` once implemented. Their service READMEs will become authoritative when those applications are scaffolded.

