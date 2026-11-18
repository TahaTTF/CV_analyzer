# Milestone 3A: offline CV parsing

## What is implemented

The backend exposes a synchronous, stateless endpoint:

```http
POST /api/cv/parse
Content-Type: multipart/form-data
```

The multipart field must be named `file`. The endpoint accepts text-based PDF and
DOCX files up to 5 MB. It returns an extracted candidate profile plus deterministic,
NLP-ready text fields. It does not persist the uploaded file, extracted text, or
candidate profile, and it does not call an external AI or document-processing service.

## Processing architecture

```text
CvController
  -> CvParsingService
     -> CvDocumentExtractionService (format validation and text extraction)
     -> CandidateProfileExtractor (conservative structured extraction)
     -> SemanticProfileComposer (PII-redacted NLP text)
     -> CvParseResponse
```

- Apache PDFBox 3 supplies text coordinates. `LayoutAwarePdfTextExtractor` groups
  glyphs into visual lines, detects a probable two-column gutter per page, and reads
  column bands without interleaving left- and right-column text.
- Apache POI extracts text from DOCX documents.
- `TechnicalSkillExtractor` reuses the Milestone 2 skill dictionary.
- English and French section headings are supported. Experience, education, and
  project sections use date/title-aware entry segmentation rather than relying only
  on blank lines. A small set of exact Arabic
  headings is recognized where extraction is reliable.
- Certification, volunteering, and extracurricular headings terminate the preceding
  section. Their sanitized text is retained internally for labelled semantic context
  but is not misrepresented as employment or education and does not alter the public
  `profile` JSON structure.
- Missing or uncertain values remain `null` or empty and produce warning codes;
  the parser does not invent experience, education, or skills.

## Upload safeguards

- The filename extension and actual file signature/package structure must agree.
- PDFs with passwords/encryption are rejected.
- image-only/scanned PDFs are rejected because OCR is not implemented yet.
- DOCX packages must contain the required Office document entries.
- macro-enabled DOCX files and embedded VBA projects are rejected.
- PDF page count, extracted text length, ZIP entry count, ZIP expansion, and POI ZIP
  inflation safeguards limit resource use.
- Responses do not contain the complete raw CV text.
- Email addresses, phone numbers, URLs, and contact-labelled lines are removed from
  NLP-ready fields. Clients should still treat all profile output as personal data.

## Example request

Postman:

1. Create a `POST` request to `http://localhost:8080/api/cv/parse`.
2. Select **Body > form-data**.
3. Add a key named `file`, change its type from Text to File, and select a PDF or DOCX.
4. Send the request. Postman supplies the multipart boundary automatically.

PowerShell 7:

```powershell
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/cv/parse' `
  -Form @{ file = Get-Item 'C:\path\candidate-cv.pdf' }
```

Windows PowerShell 5 can use curl:

```powershell
curl.exe -X POST -F "file=@C:\path\candidate-cv.pdf" http://localhost:8080/api/cv/parse
```

## Example success response

```json
{
  "parsingStatus": "SUCCESS",
  "parserVersion": "cv-parser-1.3",
  "profile": {
    "targetRole": "Backend Developer",
    "professionalSummary": "Backend engineer building Java services.",
    "technicalSkills": ["Java", "Spring Boot", "MongoDB"],
    "yearsOfExperience": 5,
    "experience": [
      {
        "role": "Java Developer",
        "organization": "Example Company",
        "period": "2021 - Present",
        "description": "Built Spring Boot APIs."
      }
    ],
    "projects": [
      {
        "name": "Inventory API",
        "description": "Built a stock-management REST service.",
        "technologies": ["Java", "Spring Boot"]
      }
    ],
    "education": [],
    "languages": ["English", "French"]
  },
  "professionalSummaryText": "Backend engineer building Java services.",
  "skillsText": "Technical skills: Java, Spring Boot, MongoDB.",
  "experienceText": "Professional experience: Java Developer. Example Company. 2021 - Present. Built Spring Boot APIs.",
  "projectsText": "Projects: Inventory API. Built a stock-management REST service. Technologies: Java, Spring Boot.",
  "educationText": null,
  "semanticProfileText": "Backend Developer. Backend engineer building Java services. Technical skills: Java, Spring Boot, MongoDB. Professional experience: Java Developer. Example Company. 2021 - Present. Built Spring Boot APIs. Projects: Inventory API. Built a stock-management REST service. Technologies: Java, Spring Boot.",
  "warnings": ["EDUCATION_NOT_FOUND"]
}
```

The NLP-ready representation is composed only from useful professional fields:
target role, professional summary, technical skills, work experience, personal or
academic projects, and education. Project technologies are included only when the
CV text explicitly contains them.
It deliberately excludes raw document text and contact details. This stable text can
later be embedded and compared with enriched job text in Milestone 3B/4.

## Expected errors

Errors use `application/problem+json` and include a stable `code` and a short trace ID.
Common codes are:

| HTTP | Code | Meaning |
|---:|---|---|
| 400 | `CV_FILE_REQUIRED`, `CV_FILE_EMPTY` | Missing or empty multipart file |
| 413 | `CV_FILE_TOO_LARGE`, `CV_PDF_TOO_MANY_PAGES`, `CV_TEXT_TOO_LARGE` | A configured processing limit was exceeded |
| 415 | `CV_FORMAT_UNSUPPORTED`, `CV_DOCX_MACROS_NOT_ALLOWED` | Unsupported, disguised, or macro-bearing format |
| 422 | `CV_PDF_ENCRYPTED`, `CV_PDF_NO_TEXT`, `CV_PDF_CORRUPTED` | PDF cannot be safely parsed |
| 422 | `CV_DOCX_NO_TEXT`, `CV_DOCX_CORRUPTED` | DOCX cannot be safely parsed |

## Current limitations

- No OCR for scanned/image-only PDFs.
- No legacy `.doc` or other office formats.
- Coordinate-based extraction supports common one- and two-column layouts, multiple
  pages, colored text, and documents containing decorative photos. Three or more
  columns, overlapping text boxes, complex tables, rotated text, unusual headings,
  and RTL ordering can still reduce extraction quality.
- Arabic support is conservative rather than a general Arabic NLP parser.
- Years of experience are returned only when explicitly and reliably stated; dates are
  not converted into a guessed total.
- This milestone performs parsing only. Upload persistence, embeddings, semantic job
  matching, and ranking belong to later milestones.
