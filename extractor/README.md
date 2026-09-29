# Extractor (F11 — Extraction PDF)

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
