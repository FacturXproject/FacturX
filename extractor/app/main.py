from fastapi import FastAPI, File, HTTPException, Response, UploadFile

from .extraction import SOURCE, extract_fields
from .generation import GenerationError, Invoice, Line, Tax, generate_facturx
from .schemas import ExtractionResponse, FieldValue, GenerationRequest, LineValue

# F11: internal-only service. It never touches the database and never
# authenticates a caller - Spring Boot checks the user/org/permissions and
# the document *before* delegating here, and persists the result afterwards.
# This service only ever receives bytes and returns JSON.
# F13: same rules for /generate - it receives an already validated draft and
# returns the Factur-X file. It does not judge conformity: the Java engine
# validates the generated file independently.
app = FastAPI(title="FacturX Extractor", version="1.1.0")


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


@app.post("/generate", response_class=Response)
def generate(request: GenerationRequest):
    """F13: brouillon validé -> fichier Factur-X (PDF/A-3 avec XML CII embarqué)."""
    data = request.invoice
    invoice = Invoice(
        number=data.number,
        issue_date=data.issueDate,
        due_date=data.dueDate,
        seller_name=data.seller.name,
        seller_siren=data.seller.siren,
        seller_vat=data.seller.vatNumber,
        seller_address=data.seller.address,
        buyer_name=data.buyer.name,
        buyer_siren=data.buyer.siren,
        buyer_address=data.buyer.address,
        currency=data.currency,
        lines=[Line(l.description, l.quantity, l.unitPrice, l.vatRate, l.total) for l in data.lines],
        taxes=[Tax(t.rate, t.taxableAmount, t.taxAmount) for t in data.taxes],
        total_ht=data.totalHt,
        total_vat=data.totalVat,
        total_ttc=data.totalTtc,
    )
    try:
        pdf, _xml = generate_facturx(invoice, request.profile)
    except GenerationError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc
    except Exception as exc:  # factur-x raises on an XML rejected by the official XSD
        raise HTTPException(status_code=422, detail=f"Génération impossible : {exc}") from exc

    return Response(
        content=pdf,
        media_type="application/pdf",
        headers={"X-Facturx-Profile": request.profile},
    )
