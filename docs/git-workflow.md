# Git and collaboration workflow

## Branch model

- `main` is stable and shared. Both developers initially branch from it.
- Person 1 works on `dev/person1-backend-ai`.
- Person 2 works on `dev/person2-frontend`.
- Commit to the relevant development branch and open a pull request into `main`.
- The other developer reviews each pull request. Do not force push.

Keep commits small and descriptive. Recommended conventional commit patterns include:

```text
chore: initialize project structure
docs: add team responsibilities
feat(backend): add job ingestion
feat(frontend): add CV upload page
feat(ai): implement CV embeddings
test: add matching tests
```

Synchronize regularly without rewriting shared history:

```bash
git switch main
git pull --ff-only origin main
git switch dev/person1-backend-ai  # or dev/person2-frontend
git merge main
```

Resolve conflicts on the development branch, test, then push. Never commit `.env`, credentials, real CVs, `node_modules`, virtual environments, generated binaries, or build output.

## Shared-file rules

Changes to `docs/api-contract.yaml`, `docs/database-schema.md`, `docker-compose.yml`, shared fixtures, or cross-service conventions must be discussed before implementation. The proposing developer updates the contract and examples in the same pull request; the other developer explicitly approves it. Avoid simultaneous edits to the same shared file by announcing ownership on the task board.

## Team cadence

- Hold a daily 10-minute check: completed work, today's plan, integration needs, and blockers.
- Hold a weekly integration/demo checkpoint on `main` using a documented happy path.
- Maintain one shared task board with owner, status, acceptance criteria, dependency, and target milestone.
- Report a blocker as soon as it threatens a dependency: state what is blocked, evidence, attempts, decision needed, and owner/date for follow-up.
- Person 1 owns the first API draft. Both developers approve public contract changes before implementation.

## Definition of Done

A task is done when acceptance criteria are met, relevant tests pass, formatting/static checks pass, documentation and examples match behavior, no secrets or personal data are present, errors and observable states are handled, and a reviewed pull request is merged. Shared changes also require consumer verification.

## QA collaboration

Person 2 can create Postman requests from the OpenAPI examples, maintain synthetic CV/job fixtures, verify status/error/UI behavior, and pair on end-to-end scenarios. Person 1 provides stable local endpoints, deterministic fixtures, logs without personal data, and reproducible setup instructions.

