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
