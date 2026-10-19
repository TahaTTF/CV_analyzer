# Local setup guide

Only MongoDB infrastructure and contracts exist today. Service commands below marked “future” become valid after each application is scaffolded.

## Prerequisites

- Git
- Docker Desktop and Docker Compose
- Future backend: Java 21 and Maven or the committed Maven wrapper
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

## Planned service startup

After scaffolding, each service README will provide authoritative commands. The intended ports are Angular `4200`, Spring Boot `${BACKEND_PORT:-8080}`, FastAPI `${AI_SERVICE_PORT:-8000}`, and MongoDB `27017`. Do not infer readiness from an open port alone; health endpoints and integration tests will be added.

