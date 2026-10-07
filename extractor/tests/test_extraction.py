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
