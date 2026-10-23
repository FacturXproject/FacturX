import pytest

from app.extraction import extract_fields


def test_extracts_expected_fields_from_a_clean_invoice(sample_invoice_pdf):
    result = extract_fields(sample_invoice_pdf)

    assert result.fields["invoiceNumber"][0] == "FACT-2026-00142"
    assert result.fields["invoiceDate"][0] == "28/07/2026"
    assert result.fields["dueDate"][0] == "27/08/2026"
    assert result.fields["sellerSiren"][0] == "452 891 237"
    assert result.fields["sellerVat"][0] == "FR45452891237"
    assert result.fields["totalHt"][0] == "4808.00"
    assert result.fields["vatRate"][0] == "20"
    assert result.fields["totalTtc"][0] == "5769.60"
    assert "SARL Dupont Informatique" in (result.fields["sellerName"][0] or "")
    assert "Martin" in (result.fields["buyerName"][0] or "")


def test_every_matched_field_has_a_positive_confidence(sample_invoice_pdf):
    result = extract_fields(sample_invoice_pdf)

    for name, (value, confidence) in result.fields.items():
        if value is not None:
            assert 0.0 < confidence <= 1.0, f"{name} matched but has confidence {confidence}"


def test_a_pdf_with_no_recognizable_fields_yields_low_confidence_nulls_not_a_crash(blank_pdf):
    result = extract_fields(blank_pdf)

    assert result.fields["invoiceNumber"][0] is None
    assert result.fields["invoiceNumber"][1] == 0.0
    assert result.fields["totalTtc"][0] is None


def test_extraction_never_returns_a_confidence_above_one_or_below_zero(sample_invoice_pdf):
    result = extract_fields(sample_invoice_pdf)
    for _, confidence in result.fields.values():
        assert 0.0 <= confidence <= 1.0


def test_line_description_is_text_even_when_shorter_than_an_amount(table_invoice_pdf):
    # "Audit" (5 chars) is shorter than "240,00" (6): the description must not
    # fall back to the longest cell of the row.
    lines = extract_fields(table_invoice_pdf).lines

    assert lines[0] == {
        "description": "Audit",
        "quantity": "3",
        "unitPrice": "80,00",
        "total": "240,00",
        "confidence": 0.5,
    }


def test_line_quantity_ignores_text_cells_that_contain_digits(table_invoice_pdf):
    # Neither the reference ("B07") nor the description ("... 365") is a quantity.
    line = extract_fields(table_invoice_pdf).lines[1]

    assert line["description"] == "Licence Office 365"
    assert line["quantity"] == "2"
    assert line["unitPrice"] == "1 080,00"
    assert line["total"] == "2 160,00"


def test_totals_row_inside_the_table_is_not_an_invoice_line(table_invoice_pdf):
    lines = extract_fields(table_invoice_pdf).lines

    assert len(lines) == 2
    assert all(line["description"] != "Total HT" for line in lines)


def test_address_includes_the_street_line_above_the_postal_code(table_invoice_pdf):
    fields = extract_fields(table_invoice_pdf).fields

    assert fields["sellerAddress"][0] == "10 rue de la Republique, 06000 Nice"
    assert fields["buyerAddress"][0] == "25 avenue Victor Hugo, 75016 Paris"


@pytest.mark.parametrize("label", [
    "Facturé à :",
    "Facturée à :",
    "Facture à :",
    "Facturé a :",
    "Facture a :",
    "FACTURE A :",
    "Adressé à :",
    "Adresse a :",
    "Destinataire :",
    "Client :",
])
def test_buyer_block_is_found_with_or_without_accents(make_invoice_pdf, label):
    pdf = make_invoice_pdf([
        "SARL Dupont Informatique",
        "14 rue des Lilas, 75011 Paris",
        "SIREN : 452 891 237",
        "",
        "Facture N° FACT-2026-00142",
        "",
        label,
        "SAS Martin Associés",
        "8 avenue Foch",
        "69002 Lyon",
        "SIREN : 987 654 321",
    ])
    fields = extract_fields(pdf).fields

    assert fields["buyerName"][0] == "SAS Martin Associés"
    assert fields["buyerSiren"][0] == "987 654 321"
    assert fields["buyerAddress"][0] == "8 avenue Foch, 69002 Lyon"
    # The buyer label must not disturb what was already found.
    assert fields["invoiceNumber"][0] == "FACT-2026-00142"
    assert fields["sellerSiren"][0] == "452 891 237"


def test_a_description_wrapped_on_two_lines_is_returned_on_one_line(make_table_invoice_pdf):
    from reportlab.lib.styles import getSampleStyleSheet
    from reportlab.platypus import Paragraph

    wrapped = Paragraph("Prestation de conseil en organisation", getSampleStyleSheet()["Normal"])
    pdf = make_table_invoice_pdf(
        ["Facture n° FAC-1"],
        [
            ["Description", "Qte", "P.U.", "TVA", "Total"],
            [wrapped, "2", "100,00", "20 %", "200,00"],
        ],
        ["Total HT : 200,00"],
        col_widths=[120, 40, 60, 50, 60],
    )
    lines = extract_fields(pdf).lines

    assert len(lines) == 1
    assert lines[0]["description"] == "Prestation de conseil en organisation"
    assert lines[0]["quantity"] == "2"
    assert lines[0]["total"] == "200,00"


# --- Lines without a ruled table (most real invoices) ------------------------------

def _columns_invoice_pdf(rows: list[tuple]) -> bytes:
    # Columns placed like an invoice software does: no grid, the description on the
    # left and the amounts right-aligned under their header.
    import io

    from reportlab.lib.pagesizes import A4
    from reportlab.pdfgen import canvas

    buffer = io.BytesIO()
    c = canvas.Canvas(buffer, pagesize=A4)
    c.setFont("Helvetica", 9)
    y = 760
    c.drawString(50, y, "Facture N° FAC-2026-010")
    y -= 40
    for row in rows:
        if isinstance(row, str):
            # A wrapped description line sits closer than the next invoice line.
            y += 5
            c.drawString(50, y, row)
        else:
            description, *amounts = row
            c.drawString(50, y, description)
            for x, value in zip((330, 410, 460, 540), amounts):
                if value:
                    c.drawRightString(x, y, value)
        y -= 16
    c.showPage()
    c.save()
    return buffer.getvalue()


def _values(lines: list[dict]) -> list[tuple]:
    return [(line["description"], line["quantity"], line["unitPrice"], line["total"]) for line in lines]


def test_lines_are_found_when_the_pdf_draws_no_table(sample_invoice_pdf):
    # pdfplumber's extract_tables() finds nothing here: the lines used to be lost.
    assert extract_fields(sample_invoice_pdf).lines == [{
        "description": "Prestation conseil IT",
        "quantity": "5",
        "unitPrice": "750.00",
        "total": "3750.00",
        "confidence": 0.4,
    }]


def test_lines_without_table_are_read_column_by_column():
    pdf = _columns_invoice_pdf([
        ("Désignation", "Qté", "P.U. HT", "TVA", "Total HT"),
        ("Licence Office 365", "2", "1 080,00 €", "20 %", "2 160,00 €"),
        ("Livre technique", "3", "12,50 €", "5,5 %", "37,50 €"),
        "édition reliée, couverture rigide",
        ("Audit", "1", "80,00 €", "20 %", "80,00 €"),
        ("Total HT", "", "", "", "2 277,50 €"),
        ("TVA 20 %", "", "", "", "448,00 €"),
    ])
    lines = extract_fields(pdf).lines

    assert _values(lines) == [
        # "365" belongs to the description, the VAT rate is not a price, the
        # thousands are kept, the currency is dropped.
        ("Licence Office 365", "2", "1 080,00", "2 160,00"),
        # A description wrapped on the next line is joined.
        ("Livre technique édition reliée, couverture rigide", "3", "12,50", "37,50"),
        ("Audit", "1", "80,00", "80,00"),
    ]


def test_text_after_the_lines_table_is_not_a_line():
    pdf = _columns_invoice_pdf([
        ("Description", "Quantité", "Prix", "", "Montant"),
        ("Formation", "2", "500,00", "", "1 000,00"),
        "",
        "",
        "",
        ("Commande 2026 livrée", "12", "", "", "45"),
    ])
    lines = extract_fields(pdf).lines

    assert len(lines) == 1
    assert lines[0]["description"] == "Formation"


def test_lines_of_a_pdf_produced_by_our_generator_are_read_back():
    from datetime import date
    from decimal import Decimal as D

    from app.generation import Invoice, Line, Tax, render_pdf

    invoice = Invoice(
        number="FACT-2026-00142", issue_date=date(2026, 7, 28), due_date=None,
        seller_name="SARL Dupont Informatique", seller_siren="452891237", seller_vat="FR45452891237",
        seller_address="14 rue des Lilas, 75011 Paris", buyer_name="SAS Martin", buyer_siren=None,
        buyer_address="8 avenue Foch, 69002 Lyon", currency="EUR",
        lines=[
            Line("Prestation conseil IT", D("5"), D("750"), D("20"), D("3750.00")),
            Line("Livre technique", D("3"), D("12.5"), D("5.5"), D("37.50")),
        ],
        taxes=[Tax(D("20"), D("3750.00"), D("750.00")), Tax(D("5.5"), D("37.50"), D("2.06"))],
        total_ht=D("3787.50"), total_vat=D("752.06"), total_ttc=D("4539.56"),
    )
    lines = extract_fields(render_pdf(invoice)).lines

    assert _values(lines) == [
        ("Prestation conseil IT", "5", "750,00", "3 750,00"),
        ("Livre technique", "3", "12,50", "37,50"),
    ]


def test_a_ruled_table_without_column_header_holds_no_line(make_table_invoice_pdf):
    # A key/value block drawn as a table ("Order Number | 12345") used to come
    # back as an invoice line.
    pdf = make_table_invoice_pdf(
        ["Facture n° FAC-1"],
        [["Numéro de commande", "12345"], ["Total HT", "400,00"], ["Total TTC", "480,00"]],
        [],
    )

    assert extract_fields(pdf).lines == []


def test_ruled_table_columns_are_read_from_the_header(make_table_invoice_pdf):
    # Unit price before quantity, a unit column, currency in the cells, a unit
    # price with 4 decimals, a reference column with digits.
    pdf = make_table_invoice_pdf(
        ["Facture n° FAC-1"],
        [
            ["Pos", "Référence", "Désignation", "Prix unitaire", "Quantité", "Unité", "TVA", "Montant HT"],
            ["1", "TB100A4", "Trennblätter A4", "9,9000 €", "20", "Stk", "19%", "198,00 €"],
            ["2", "ARNR2", "Joghurt Banane", "5,5000 €", "50", "Stk", "7%", "275,00 €"],
        ],
        [],
    )
    lines = extract_fields(pdf).lines

    assert _values(lines) == [
        ("Trennblätter A4", "20", "9,9000", "198,00"),
        ("Joghurt Banane", "50", "5,5000", "275,00"),
    ]
