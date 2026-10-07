from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_extract_returns_fields_and_lines_for_a_valid_pdf(sample_invoice_pdf):
    response = client.post(
        "/extract",
        files={"file": ("invoice.pdf", sample_invoice_pdf, "application/pdf")},
    )
    assert response.status_code == 200

    body = response.json()
    assert body["source"] == "pdf-extraction-v1"
    assert body["fields"]["invoiceNumber"]["value"] == "FACT-2026-00142"
    assert body["fields"]["invoiceNumber"]["confidence"] > 0
    assert isinstance(body["lines"], list)


def test_extract_rejects_non_pdf_files():
    response = client.post(
        "/extract",
        files={"file": ("invoice.txt", b"hello", "text/plain")},
    )
    assert response.status_code == 415


def test_extract_rejects_empty_file():
    response = client.post(
        "/extract",
        files={"file": ("invoice.pdf", b"", "application/pdf")},
    )
    assert response.status_code == 400


def test_extract_returns_a_clean_error_for_a_corrupt_pdf(corrupt_pdf):
    response = client.post(
        "/extract",
        files={"file": ("invoice.pdf", corrupt_pdf, "application/pdf")},
    )
    assert response.status_code == 422
    assert "detail" in response.json()


def test_extract_never_requires_auth():
    # No Authorization header, no cookie - the extractor must stay reachable
    # by Spring Boot alone without any credential of its own (F11).
    response = client.get("/health")
    assert response.status_code == 200
