"""F13 : génération Factur-X. La conformité complète (schematron EN 16931, PDF/A-3)
est contrôlée côté Java par Mustang ; ici on vérifie la structure produite."""

import io
from datetime import date
from decimal import Decimal as D

import pytest
from fastapi.testclient import TestClient
from pypdf import PdfReader

from app.generation import GenerationError, Invoice, Line, Tax, build_cii_xml, generate_facturx
from app.main import app

client = TestClient(app)

NS_CHECKS = 'xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"'


def make_invoice(**overrides) -> Invoice:
    values = dict(
        number="FACT-2026-00142",
        issue_date=date(2026, 7, 28),
        due_date=date(2026, 8, 27),
        seller_name="SARL Dupont Informatique",
        seller_siren="452 891 237",
        seller_vat="FR45452891237",
        seller_address="14 rue des Lilas, 75011 Paris",
        buyer_name="SAS Martin & Associés",
        buyer_siren="987654321",
        buyer_address="8 avenue Foch, 69002 Lyon",
        currency="EUR",
        lines=[
            Line("Manuels scolaires", D("30"), D("18.5"), D("5.5"), D("555.00")),
            Line("Fournitures", D("10"), D("12"), D("20"), D("120.00")),
        ],
        taxes=[Tax(D("20"), D("120.00"), D("24.00")), Tax(D("5.5"), D("555.00"), D("30.53"))],
        total_ht=D("675.00"),
        total_vat=D("54.53"),
        total_ttc=D("729.53"),
    )
    values.update(overrides)
    return Invoice(**values)


def embedded_xml(pdf: bytes) -> str:
    reader = PdfReader(io.BytesIO(pdf))
    files = reader.attachments
    assert list(files) == ["factur-x.xml"]
    return files["factur-x.xml"][0].decode("utf-8")


@pytest.mark.parametrize(
    "profile, guideline",
    [
        ("EN16931", "<ram:ID>urn:cen.eu:en16931:2017</ram:ID>"),
        ("BASIC", "<ram:ID>urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:basic</ram:ID>"),
    ],
)
def test_generates_a_pdf_with_the_cii_xml_embedded(profile, guideline):
    pdf, xml = generate_facturx(make_invoice(), profile)

    assert pdf.startswith(b"%PDF-")
    embedded = embedded_xml(pdf)
    assert embedded == xml.decode("utf-8")
    assert guideline in embedded
    assert "<ram:ID>FACT-2026-00142</ram:ID>" in embedded
    assert '<udt:DateTimeString format="102">20260728</udt:DateTimeString>' in embedded


def test_pdf_is_declared_pdf_a3_and_embeds_all_its_fonts():
    pdf, _ = generate_facturx(make_invoice(), "EN16931")
    reader = PdfReader(io.BytesIO(pdf))

    xmp = reader.trailer["/Root"]["/Metadata"].get_object().get_data().decode("utf-8")
    assert "<pdfaid:part>3</pdfaid:part>" in xmp
    assert "fx:ConformanceLevel" in xmp and "EN 16931" in xmp

    for page in reader.pages:
        for font in page["/Resources"]["/Font"].values():
            descriptor = font.get_object()["/FontDescriptor"].get_object()
            assert "/FontFile2" in descriptor, f"police non embarquée : {font.get_object()['/BaseFont']}"


def test_amounts_taxes_and_totals_are_written_as_computed_by_the_backend():
    xml = build_cii_xml(make_invoice(), "EN16931").decode("utf-8")

    assert "<ram:ChargeAmount>18.5</ram:ChargeAmount>" in xml
    assert '<ram:BilledQuantity unitCode="C62">30</ram:BilledQuantity>' in xml
    assert "<ram:LineTotalAmount>555.00</ram:LineTotalAmount>" in xml
    assert (
        "<ram:CalculatedAmount>30.53</ram:CalculatedAmount><ram:TypeCode>VAT</ram:TypeCode>"
        "<ram:BasisAmount>555.00</ram:BasisAmount><ram:CategoryCode>S</ram:CategoryCode>"
        "<ram:RateApplicablePercent>5.5</ram:RateApplicablePercent>"
    ) in xml
    assert '<ram:TaxTotalAmount currencyID="EUR">54.53</ram:TaxTotalAmount>' in xml
    assert "<ram:GrandTotalAmount>729.53</ram:GrandTotalAmount>" in xml
    assert "<ram:DuePayableAmount>729.53</ram:DuePayableAmount>" in xml


def test_parties_use_siren_as_legal_id_and_electronic_address():
    xml = build_cii_xml(make_invoice(), "EN16931").decode("utf-8")

    assert '<ram:ID schemeID="0002">452891237</ram:ID>' in xml
    assert '<ram:URIID schemeID="0225">452891237</ram:URIID>' in xml
    assert '<ram:URIID schemeID="0225">987654321</ram:URIID>' in xml
    assert '<ram:ID schemeID="VA">FR45452891237</ram:ID>' in xml
    assert "SAS Martin &amp; Associés" in xml


def test_party_without_siren_has_no_legal_id_nor_electronic_address():
    xml = build_cii_xml(make_invoice(buyer_siren=None), "EN16931").decode("utf-8")

    assert '<ram:URIID schemeID="0225">987654321</ram:URIID>' not in xml
    assert xml.count('schemeID="0225"') == 1  # vendeur uniquement


def test_french_legal_mentions_are_included():
    xml = build_cii_xml(make_invoice(), "EN16931").decode("utf-8")

    for code in ("PMT", "PMD", "AAB"):
        assert f"<ram:SubjectCode>{code}</ram:SubjectCode>" in xml


def test_without_due_date_payment_terms_are_given_instead():
    xml = build_cii_xml(make_invoice(due_date=None), "EN16931").decode("utf-8")

    assert "DueDateDateTime" not in xml
    assert "<ram:Description>Paiement à réception de la facture.</ram:Description>" in xml


def test_zero_rate_uses_category_z():
    invoice = make_invoice(
        lines=[Line("Export", D("1"), D("100"), D("0"), D("100.00"))],
        taxes=[Tax(D("0"), D("100.00"), D("0.00"))],
        total_ht=D("100.00"), total_vat=D("0.00"), total_ttc=D("100.00"),
    )
    xml = build_cii_xml(invoice, "EN16931").decode("utf-8")

    assert "<ram:CategoryCode>Z</ram:CategoryCode>" in xml
    assert "<ram:CategoryCode>S</ram:CategoryCode>" not in xml


def test_long_invoices_span_several_pages():
    lines = [Line(f"Article n°{i} avec une description longue " * 2, D("1"), D("10"), D("20"), D("10.00"))
             for i in range(60)]
    invoice = make_invoice(lines=lines, taxes=[Tax(D("20"), D("600.00"), D("120.00"))],
                           total_ht=D("600.00"), total_vat=D("120.00"), total_ttc=D("720.00"))
    pdf, xml = generate_facturx(invoice, "EN16931")

    assert len(PdfReader(io.BytesIO(pdf)).pages) > 1
    assert xml.decode("utf-8").count("<ram:IncludedSupplyChainTradeLineItem>") == 60


def test_unsupported_profile_is_refused():
    with pytest.raises(GenerationError, match="Profil non supporté"):
        generate_facturx(make_invoice(), "EXTENDED")


def test_invoice_without_lines_is_refused():
    with pytest.raises(GenerationError):
        generate_facturx(make_invoice(lines=[]), "EN16931")


def api_payload(**invoice_overrides):
    invoice = {
        "number": "FACT-2026-00142",
        "issueDate": "2026-07-28",
        "dueDate": "2026-08-27",
        "currency": "EUR",
        "seller": {"name": "SARL Dupont", "siren": "452891237", "vatNumber": "FR45452891237",
                   "address": "14 rue des Lilas, 75011 Paris"},
        "buyer": {"name": "SAS Martin", "siren": None, "address": "8 avenue Foch, 69002 Lyon"},
        "lines": [{"description": "Conseil", "quantity": "5", "unitPrice": "750", "vatRate": "20",
                   "total": "3750.00"}],
        "taxes": [{"rate": "20", "taxableAmount": "3750.00", "taxAmount": "750.00"}],
        "totalHt": "3750.00",
        "totalVat": "750.00",
        "totalTtc": "4500.00",
    }
    invoice.update(invoice_overrides)
    return {"profile": "EN16931", "invoice": invoice}


def test_generate_endpoint_returns_the_facturx_pdf():
    response = client.post("/generate", json=api_payload())

    assert response.status_code == 200
    assert response.headers["content-type"] == "application/pdf"
    assert response.headers["x-facturx-profile"] == "EN16931"
    assert "<ram:GrandTotalAmount>4500.00</ram:GrandTotalAmount>" in embedded_xml(response.content)


def test_generate_endpoint_rejects_an_invalid_request():
    assert client.post("/generate", json=api_payload(lines=[])).status_code == 422
    assert client.post("/generate", json=api_payload(issueDate="28/07/2026")).status_code == 422
    assert client.post("/generate", json={**api_payload(), "profile": "EXTENDED"}).status_code == 422
