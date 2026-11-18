# Milestone 1.5: multi-provider IT job ingestion

## Safety model

- No provider is called during startup, scheduled work, or a GET request.
- Ingestion routes exist only under the local Spring profile.
- JOB_INGESTION_ENABLED defaults to false; disabled calls return 403.
- Each provider has an independent request budget. Adzuna is additionally capped at 25 pages
  by configuration validation and defaults to one request.
- Missing credentials do not prevent application startup. An intentional provider call fails
  with a safe 503 error when its credentials are absent.
- Automated tests use Spring mock HTTP servers and never call a live provider.
- Existing documents are neither deleted nor globally replaced. Identity is the unique
  (source, externalId) pair.

## Provider requests

### Adzuna

The client follows the official search shape:

GET https://api.adzuna.com/v1/api/jobs/{country}/search/{page}

It sends app_id, app_key, results_per_page, category=it-jobs, and
content-type=application/json. what is sent only when ADZUNA_QUERY is non-empty.
The country defaults to fr. Calls are sequential and stop when a short page is returned or the
smaller of ADZUNA_MAX_PAGES and ADZUNA_REQUEST_BUDGET is reached.

### La bonne alternance

The client follows the official OpenAPI search operation:

GET https://api.apprentissage.beta.gouv.fr/api/job/v1/search

Authentication is Authorization: Bearer API_KEY. The romes query parameter defaults to
the M18 IT family (M1801 through M1810). The current search API is not paginated and can
return at most 150 results per source, so one request is used. identifier.partner_label and
identifier.partner_job_id are retained, and normalized external identity is
partner_label:partner_job_id.

## Normalization and provenance

| Provider field | Stored field |
| --- | --- |
| Adzuna id | externalId |
| Adzuna company.display_name | company.name |
| Adzuna location.display_name | location.displayName |
| Adzuna category | sourceMetadata.categories |
| LBA identifier.partner_* | sourceMetadata.partner* and externalId |
| LBA workplace.name / brand / legal name | company.name |
| LBA workplace.location.address | location.displayName |
| LBA offer.desired_skills | requirements.skills |
| LBA offer.to_be_acquired_skills | requirements.toBeAcquiredSkills |
| LBA offer.rome_codes | sourceMetadata.romeCodes |

Required IDs, titles, and HTTP(S) application URLs are validated. Lists are trimmed,
de-duplicated, and null-safe. Descriptions remain provider text and may still contain HTML.

## Deterministic IT classification

ItJobClassifier normalizes case and accents, then uses word/phrase boundary rules in English,
French, and German. Strong job-title evidence, provider categories/tags, and M18 ROME codes can
mark a job relevant. Generic technical terms found only in a description produce an ambiguous
result instead of a false positive. Explicit non-IT title safeguards override weak evidence.

Every classified document records:

- isItRelevant: true, false, or null for ambiguous evidence;
- matchedCategories;
- relevanceReason;
- classifier version;
- classifiedAt.

Legacy documents without itClassification remain readable. The local-only
POST /api/dev/ingestion/classify-existing operation backfills only unclassified records. It
does not call a provider, but it does write MongoDB and therefore must be run deliberately.

The default GET /api/jobs filter returns isItRelevant=true plus documents with no
classification. Once the backfill is complete, only relevant jobs remain in the default view.
Review filters are available through the optional relevance query parameter.

## Future offline enrichment

LocalJobEnrichmentPort reserves a local-only pipeline boundary for HTML cleanup, location
normalization, skill normalization, and later embedding preparation. It is intentionally only an
interface in this milestone. Future implementations must operate on MongoDB data and must not
re-call external job APIs.

## One approved local ingestion

1. Copy .env.example to .env and put real credentials only in the ignored .env file or in the
   current PowerShell process environment.
2. Start MongoDB with docker compose up -d mongodb.
3. Set JOB_INGESTION_ENABLED=true and keep the selected provider request budget at 1.
4. Start Spring Boot with the local profile.
5. Send exactly one chosen provider POST request. Do not run the whole Postman collection with
   writes enabled.
6. Stop Spring Boot, set JOB_INGESTION_ENABLED=false, and start it again for normal browsing.

The Postman collection adds a second guard: allowWrites defaults to false, so its POST requests
are skipped. Provider credentials belong in Spring Boot environment variables, not Postman.

Read-only browsing never consumes provider quota:

GET /api/jobs?page=0&size=20

Example read-only MongoDB checks (run inside mongosh after authenticating without placing the
password in shell history):

~~~javascript
db.jobs.aggregate([{ $group: { _id: "$source", count: { $sum: 1 } } }])
db.jobs.aggregate([{ $group: { _id: "$itClassification.isItRelevant", count: { $sum: 1 } } }])
db.jobs.countDocuments({ itClassification: { $exists: false } })
~~~

## Deliberately not implemented

- scheduled or automatic ingestion;
- live provider calls in tests;
- AI/LLM classification or embeddings;
- cross-provider fuzzy duplicate merging;
- automatic HTML stripping;
- automatic execution of the legacy classification backfill.
