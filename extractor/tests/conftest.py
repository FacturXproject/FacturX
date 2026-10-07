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


def _build_table_invoice_pdf(head: list[str], rows: list[list[str]], tail: list[str]) -> bytes:
    # A real ruled table (unlike _build_invoice_pdf's plain text), so that
    # pdfplumber's extract_tables() - and therefore line extraction - is exercised.
    from reportlab.lib import colors
    from reportlab.lib.styles import getSampleStyleSheet
    from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

    buffer = io.BytesIO()
    style = getSampleStyleSheet()["Normal"]
    table = Table(rows)
    table.setStyle(TableStyle([("GRID", (0, 0), (-1, -1), 0.5, colors.black)]))

    elements = [Paragraph(line, style) for line in head]
    elements += [Spacer(1, 10), table, Spacer(1, 10)]
    elements += [Paragraph(line, style) for line in tail]
    SimpleDocTemplate(buffer, pagesize=A4).build(elements)
    return buffer.getvalue()


@pytest.fixture
def table_invoice_pdf() -> bytes:
    return _build_table_invoice_pdf(
        [
            "Facture n° FAC-2026-002",
            "Date facture : 07/10/2026",
            "SARL Demo Conseil",
            "10 rue de la Republique",
            "06000 Nice",
            "SIREN : 123 456 789",
            "Client:",
            "SAS Client Exemple",
            "25 avenue Victor Hugo",
            "75016 Paris",
            "SIREN : 987 654 321",
        ],
        [
            ["Ref", "Description", "Qte", "P.U.", "TVA", "Total"],
            ["A12", "Audit", "3", "80,00", "20 %", "240,00"],
            ["B07", "Licence Office 365", "2", "1 080,00", "20 %", "2 160,00"],
            ["", "", "", "", "Total HT", "2 400,00"],
        ],
        ["Total HT : 2 400,00", "TVA 20 % : 480,00", "Total TTC : 2 880,00"],
    )
