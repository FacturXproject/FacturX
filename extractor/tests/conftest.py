import io

import pytest
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas


def _build_invoice_pdf(lines_text: list[str]) -> bytes:
    buffer = io.BytesIO()
    c = canvas.Canvas(buffer, pagesize=A4)
    width, height = A4

    y = height - 50
    for line in lines_text:
        c.drawString(50, y, line)
        y -= 18

    c.showPage()
    c.save()
    return buffer.getvalue()


@pytest.fixture
def sample_invoice_pdf() -> bytes:
    return _build_invoice_pdf([
        "SARL Dupont Informatique",
        "14 rue des Lilas, 75011 Paris",
        "SIREN : 452 891 237",
        "TVA intracommunautaire : FR45452891237",
        "",
        "Facture N° FACT-2026-00142",
        "Date de facture : 28/07/2026",
        "Date d'échéance : 27/08/2026",
        "",
        "Facturé à :",
        "SAS Martin & Associés",
        "8 avenue Foch, 69002 Lyon",
        "",
        "Description               Qte    PU       Total",
        "Prestation conseil IT      5    750.00   3750.00",
        "",
        "Total HT : 4808.00 EUR",
        "TVA 20% : 961.60 EUR",
        "Total TTC : 5769.60 EUR",
    ])


@pytest.fixture
def blank_pdf() -> bytes:
    return _build_invoice_pdf(["This is a blank scan placeholder with no invoice data."])


@pytest.fixture
def corrupt_pdf() -> bytes:
    return b"not a real pdf file at all"
