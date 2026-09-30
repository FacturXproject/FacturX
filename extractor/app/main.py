from fastapi import FastAPI, File, HTTPException, UploadFile

from .extraction import SOURCE, extract_fields
from .schemas import ExtractionResponse, FieldValue, LineValue

# F11: internal-only service. It never touches the database and never
# authenticates a caller - Spring Boot checks the user/org/permissions and
# the document *before* delegating here, and persists the result afterwards.
# This service only ever receives bytes and returns JSON.
app = FastAPI(title="FacturX Extractor", version="1.0.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/extract", response_model=ExtractionResponse)
async def extract(file: UploadFile = File(...)):
    if file.content_type not in ("application/pdf", "application/octet-stream") and not (
        file.filename or ""
    ).lower().endswith(".pdf"):
        raise HTTPException(status_code=415, detail="Only PDF files are supported.")

    pdf_bytes = await file.read()
    if not pdf_bytes:
        raise HTTPException(status_code=400, detail="Empty file.")

    try:
        extracted = extract_fields(pdf_bytes)
    except Exception as exc:  # pdfplumber raises various low-level errors on a corrupt PDF
        raise HTTPException(status_code=422, detail=f"Unable to read this PDF: {exc}") from exc

    fields = {
        name: FieldValue(value=value, confidence=confidence)
        for name, (value, confidence) in extracted.fields.items()
    }
    lines = [LineValue(**row) for row in extracted.lines]

    return ExtractionResponse(fields=fields, lines=lines, source=SOURCE)
