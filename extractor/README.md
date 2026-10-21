# Extractor (F11 — Extraction PDF, F13 — Génération Factur-X)

Internal FastAPI service. It has **no database access and no authentication**
of its own: it receives a PDF, returns JSON, nothing else. Spring Boot checks
the user, the organisation, the permissions and the document *before* calling
this service, and persists the result afterwards (see
`backend/.../extraction/ExtractionService.java`).

It is reachable only on the internal Docker network (`facturx-network`), not
published to the host.

## What it does

`POST /extract` (multipart, field `file`) reads the PDF's text layer with
`pdfplumber` and looks for known French invoice patterns: invoice number,
invoice/due dates, seller/buyer name and address, SIREN, intra-EU VAT number,
VAT rate, HT/VAT/TTC totals, and line items (via table detection).

This is deterministic pattern matching, not OCR and not machine learning.
Every field carries a `confidence` score in `[0, 1]`. A field that was not
found comes back as `value: null, confidence: 0.0` rather than failing the
request — a hard-to-read PDF still produces a (mostly empty) draft that a
human can complete in the correction form (F12), instead of a crash.

**Extraction must always be checked by a human before a Factur-X file is
generated from it (F12/F13). No field returned here is authoritative.**

## Endpoints

- `GET /health` — liveness check, included in the aggregate health response.
- `POST /extract` — see above. Returns `422` if the file cannot be opened as
  a PDF at all, `415` if it isn't a PDF, `400` if it's empty.
- `POST /generate` (F13) — JSON body `{profile, invoice}` (see
  `app/schemas.py`, `GenerationRequest`) built by Spring Boot from a draft that
  was corrected by a human and re-validated (F12). Returns the Factur-X file
  (`application/pdf`): a PDF/A-3 rendering of the invoice with the CII XML
  embedded as `factur-x.xml`. `422` if the request is invalid.

## Factur-X generation (F13)

`app/generation.py` builds the CII XML (profiles **EN 16931** and **BASIC**
only), renders the invoice with reportlab (DejaVu fonts, embedded — PDF/A
forbids non-embedded fonts; package `fonts-dejavu-core`, or set
`FACTURX_FONT_DIR`), then embeds the XML and the PDF/A-3 metadata with the
`factur-x` library (which also checks the XML against the official XSD).

This service does **not** decide whether the result is conformant: the Java
engine (Mustang, F08) validates every generated file independently. The
generator never marks its own homework.

Limits: EUR invoices, VAT categories S (standard/reduced) and Z (zero rate)
only, unit of measure "C62" (unit), country FR for both parties, no
allowances/charges (discount lines), no exemption reasons. MINIMUM, BASIC WL,
EXTENDED and XRechnung profiles are not produced.

## Development

```bash
cd extractor
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
uvicorn app.main:app --reload --port 8000
pytest
```

## Accuracy

Regex/heuristic extraction is only as good as the corpus it was tuned
against. Per-field accuracy should be measured against a set of real sample
invoices as they become available (see the root `roadmap.md`, "Numbers to
collect as we go") — this is tracked by the Java side's mutation/accuracy
tests once real invoices are gathered, not hardcoded here.
