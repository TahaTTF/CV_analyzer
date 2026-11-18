# Milestone 2 - Offline job enrichment
## Scope and safety

Milestone 2 enriches jobs already stored in MongoDB. It never calls job providers,
geocoding services, LLMs, or paid APIs. Original descriptions, locations, requirements,
identifiers, URLs, source metadata, and IT classifications are preserved.

Results are stored in an optional enrichment subdocument with version
job-enrichment-1.0. Legacy documents remain readable when this field is absent.

## Architecture

The pipeline reuses LocalJobEnrichmentPort:

1. DescriptionCleaningService parses HTML with JSoup, removes script/style content,
   decodes entities, retains paragraph/list boundaries, and normalizes whitespace.
2. TechnicalSkillExtractor combines provider skills, provider training fields, useful
   tags, title, and cleaned description.
3. LocationNormalizationService prefers structured city/country fields and then
   conservatively parses postal codes and city/region forms. Work mode requires
   explicit remote, hybrid, or on-site evidence.
4. LocalJobEnrichmentService assembles the immutable versioned subdocument.
5. JobEnrichmentBatchService reads pages, skips current-version jobs, and only writes
   changed documents when write mode is explicitly selected.
6. JobDataQualityAuditService provides the read-only per-source audit and duplicate
   grouping.

DevEnrichmentController is protected by the local Spring profile.

## Files and dependency

Added production files under backend/src/main/java/com/cvanalyzer/backend/enrichment:

- DescriptionCleaningService, TechnicalSkillExtractor, LocationNormalizationService.
- LocalJobEnrichmentService, JobEnrichmentBatchService, and EnrichmentSummary.
- JobDataQualityAuditService and DataQualityAudit.

DevEnrichmentController was added under controller. JobDocument and JobDtos were
extended compatibly, and LocalJobEnrichmentPort was reused unchanged. Tests were added
under the matching enrichment and controller test packages. The only new runtime
dependency is org.jsoup:jsoup:1.18.3 in backend/pom.xml.

## Data model and API

JobDocument.enrichment contains:

- descriptionText: cleaned text; the raw description is preserved.
- requiredSkills: canonical skills with evidence sources.
- toBeAcquiredSkills: skills explicitly found in provider training fields.
- normalizedLocation: original value, city, region, country code, work mode and
  confidence.
- version and enrichedAt.

Evidence values are provider-field, provider-training, provider-tag, title, and
description. Work mode is REMOTE, HYBRID, ON_SITE, or UNKNOWN.

Existing response fields remain. Job details add optional descriptionText,
extractedSkills, toBeAcquiredSkills, normalizedLocation, and enrichmentVersion.
The raw description remains available. Summary skills use enriched canonical skills
when present and otherwise fall back to provider skills.

## Skill dictionary

The deterministic dictionary maps aliases to canonical names. Unicode-aware boundaries
prevent Java matching JavaScript, C matching C++/C#, and SQL matching PostgreSQL.
Punctuation-heavy C++, C#, .NET, Node.js, and CI/CD remain intact. English, French and
German aliases cover the requested programming, web, database, cloud, data/AI,
security/network, testing, and enterprise families.

This is evidence extraction, not semantic inference. Skills are not guessed.

## Local commands

Start the backend after configuring MONGODB_URI securely:

~~~powershell
cd C:\CV_analyzer\backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
~~~

Read-only audit:

~~~powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/dev/enrichment/audit' |
  ConvertTo-Json -Depth 10
~~~

Safe dry-run (also the default when dryRun is omitted):

~~~powershell
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/dev/enrichment/jobs?dryRun=true&batchSize=100' |
  ConvertTo-Json -Depth 10
~~~

Write mode changes MongoDB and requires explicit approval:

~~~powershell
# REQUIRES EXPLICIT APPROVAL
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/dev/enrichment/jobs?dryRun=false&batchSize=100' |
  ConvertTo-Json -Depth 10
~~~

A second run of the same version is idempotent. Raising the version intentionally
makes old records eligible for reprocessing.

## Read-only baseline audit

Audit date: 2026-10-21. Database state: 391 documents and zero stored enrichment
subdocuments.

| Source | Total | IT | Non-IT | Ambiguous | Missing descriptions | HTML descriptions | Empty enriched skills | Missing city | Missing country | Unknown work mode |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| ARBEITNOW | 300 | 76 | 158 | 66 | 0 | 300 | 300 | 300 | 300 | 300 |
| ADZUNA | 50 | 28 | 8 | 14 | 0 | 0 | 50 | 50 | 0 | 50 |
| LA_BONNE_ALTERNANCE | 41 | 41 | 0 | 0 | 0 | 25 | 41 | 41 | 0 | 41 |

Missing city and work mode describe the current pre-enrichment fields.

Duplicate audit:

- Same source/external-ID groups: 0.
- Identical content with different upstream IDs: 1 group.
- Repeated normalized-title groups: 8 review candidates.
- The identical-content group is the two WOODOO full-stack apprenticeship documents.
  They have different upstream IDs but identical offer content and URL.

## Dry-run validation and examples

The final real dry-run examined all 391 records with zero failures and zero writes:

- wouldUpdate: 391; updated: 0; unchanged: 0; failed: 0.
- jobsWithCleanedDescriptions: 391.
- jobsWithExtractedSkills: 197.
- jobsWithNormalizedLocations: 387.

A separate MongoDB check confirmed total=391 and enrichment fields=0 afterward.

Representative in-memory behavior:

1. Arbeitnow HTML software descriptions retain raw HTML and gain readable paragraphs
   and lists. Only explicit technologies such as JavaScript, TypeScript, React, Git,
   or cloud tools are extracted.
2. The Adzuna SAP functional-consultant record retains its French source text and gains
   canonical SAP with title/description provenance; its French country metadata remains.
3. The WOODOO full-stack apprenticeship retains raw entity-containing text, gains clean
   French text, extracts explicitly present JavaScript, TypeScript, Node.js, NoSQL,
   Docker and CI/CD, and normalizes 75001 Paris to Paris / FR.

## Frontend and future FastAPI use

Angular should prefer descriptionText and fall back to description for legacy records.
It can display extracted skill names, evidence tooltips, and normalized location while
retaining the original display value.

A future FastAPI matcher can consume canonical required-skill names directly, keep
training skills separate, and use evidence sources for explanations. Embeddings should
be a later separately versioned stage.

## Known limitations

- No geocoder is used; uncertain country/region fields remain null.
- The dictionary cannot understand implied skills or negation.
- Collision-prone C, Go, Java, React and Swift use conservative case handling.
- Raw description remains HTML; clients should prefer descriptionText after enrichment.
- Duplicate groups require human review; no record is deleted or merged.
