"""F13 : génération d'une facture Factur-X (PDF/A-3 + XML CII embarqué).

Le service reçoit un brouillon déjà validé et recalculé par Spring Boot (F12) et
renvoie le fichier Factur-X. Il ne valide pas lui-même la conformité : c'est le
moteur Java (Mustang) qui contrôle le fichier généré, indépendamment ("le
générateur ne corrige jamais sa propre copie").

Profils supportés : BASIC et EN 16931 (voir roadmap F13). Les autres profils
(MINIMUM, BASIC WL, EXTENDED, XRechnung) ne sont pas proposés.
"""

from __future__ import annotations

import io
import os
from dataclasses import dataclass
from datetime import date
from decimal import ROUND_HALF_UP, Decimal
from xml.sax.saxutils import escape

from facturx import generate_from_binary
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

PROFILES = {
    # profil API -> (identifiant CII du profil, niveau attendu par la librairie factur-x)
    "EN16931": ("urn:cen.eu:en16931:2017", "en16931"),
    "BASIC": ("urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:basic", "basic"),
}

# PDF/A impose des polices embarquées : DejaVu (paquet fonts-dejavu-core, installé
# dans l'image Docker). FACTURX_FONT_DIR permet de pointer ailleurs en local.
FONT_DIRS = [
    os.environ.get("FACTURX_FONT_DIR", ""),
    "/usr/share/fonts/truetype/dejavu",
    "/usr/share/fonts/dejavu",
    "/Library/Fonts",
]
_fonts_registered = False

TWO = Decimal("0.01")

# BR-FR-05 : mentions légales obligatoires sur une facture B2B française (articles
# L441-10 et D441-5 du Code de commerce). Textes standard, appliqués à toutes les
# factures générées ; ils figurent aussi sur le PDF.
LEGAL_NOTES = (
    ("PMT", "Indemnité forfaitaire pour frais de recouvrement en cas de retard de paiement : 40 €."),
    ("PMD", "Pénalités de retard : trois fois le taux d'intérêt légal en vigueur."),
    ("AAB", "Pas d'escompte pour paiement anticipé."),
)


class GenerationError(ValueError):
    """Données insuffisantes pour produire une facture (message en français)."""


@dataclass
class Line:
    description: str
    quantity: Decimal
    unit_price: Decimal
    vat_rate: Decimal
    total: Decimal


@dataclass
class Tax:
    rate: Decimal
    taxable_amount: Decimal
    tax_amount: Decimal


@dataclass
class Invoice:
    number: str
    issue_date: date
    due_date: date | None
    seller_name: str
    seller_siren: str | None
    seller_vat: str | None
    seller_address: str | None
    buyer_name: str
    buyer_siren: str | None
    buyer_address: str | None
    currency: str
    lines: list[Line]
    taxes: list[Tax]
    total_ht: Decimal
    total_vat: Decimal
    total_ttc: Decimal


# --------------------------------------------------------------------------- XML

def _amount(value: Decimal) -> str:
    return str(value.quantize(TWO, rounding=ROUND_HALF_UP))


def _decimal(value: Decimal) -> str:
    text = format(value.normalize(), "f")
    return text if text not in ("-0", "") else "0"


def _category(rate: Decimal) -> str:
    # S = taux standard/réduit ; Z = taux zéro. Les exonérations (E, AE, K, G...)
    # demandent un motif d'exonération que le brouillon ne porte pas encore.
    return "Z" if rate == 0 else "S"


def _digits(value: str | None) -> str | None:
    if not value:
        return None
    digits = "".join(ch for ch in value if ch.isdigit())
    return digits or None


def _party(tag: str, name: str, siren: str | None, vat: str | None, address: str | None) -> str:
    parts = [f"<ram:{tag}>", f"<ram:Name>{escape(name)}</ram:Name>"]
    siren_digits = _digits(siren)
    if siren_digits:
        # schemeID 0002 = SIREN (liste ISO 6523 ICD)
        parts.append(
            f'<ram:SpecifiedLegalOrganization><ram:ID schemeID="0002">{siren_digits}</ram:ID>'
            "</ram:SpecifiedLegalOrganization>"
        )
    parts.append("<ram:PostalTradeAddress>")
    if address:
        parts.append(f"<ram:LineOne>{escape(address)}</ram:LineOne>")
    # Le brouillon ne porte pas encore de pays : factures françaises (voir limites F13).
    parts.append("<ram:CountryID>FR</ram:CountryID></ram:PostalTradeAddress>")
    if siren_digits:
        # BT-34 / BT-49 (BR-FR-13 / BR-FR-12) : adresse électronique de facturation,
        # identifiée par le SIREN dans l'annuaire français (schéma 0225).
        parts.append(
            f'<ram:URIUniversalCommunication><ram:URIID schemeID="0225">{siren_digits}</ram:URIID>'
            "</ram:URIUniversalCommunication>"
        )
    if vat:
        parts.append(
            f'<ram:SpecifiedTaxRegistration><ram:ID schemeID="VA">{escape(vat.replace(" ", ""))}</ram:ID>'
            "</ram:SpecifiedTaxRegistration>"
        )
    parts.append(f"</ram:{tag}>")
    return "".join(parts)


def build_cii_xml(invoice: Invoice, profile: str) -> bytes:
    guideline, _ = PROFILES[profile]
    cur = invoice.currency
    out = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<rsm:CrossIndustryInvoice'
        ' xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"'
        ' xmlns:qdt="urn:un:unece:uncefact:data:standard:QualifiedDataType:100"'
        ' xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"'
        ' xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100">',
        "<rsm:ExchangedDocumentContext><ram:GuidelineSpecifiedDocumentContextParameter>"
        f"<ram:ID>{guideline}</ram:ID>"
        "</ram:GuidelineSpecifiedDocumentContextParameter></rsm:ExchangedDocumentContext>",
        "<rsm:ExchangedDocument>",
        f"<ram:ID>{escape(invoice.number)}</ram:ID>",
        "<ram:TypeCode>380</ram:TypeCode>",
        "<ram:IssueDateTime>"
        f'<udt:DateTimeString format="102">{invoice.issue_date:%Y%m%d}</udt:DateTimeString>'
        "</ram:IssueDateTime>",
        *(
            f"<ram:IncludedNote><ram:Content>{escape(text)}</ram:Content><ram:SubjectCode>{code}</ram:SubjectCode>"
            "</ram:IncludedNote>"
            for code, text in LEGAL_NOTES
        ),
        "</rsm:ExchangedDocument>",
        "<rsm:SupplyChainTradeTransaction>",
    ]

    for index, line in enumerate(invoice.lines, start=1):
        out.append(
            "<ram:IncludedSupplyChainTradeLineItem>"
            f"<ram:AssociatedDocumentLineDocument><ram:LineID>{index}</ram:LineID></ram:AssociatedDocumentLineDocument>"
            f"<ram:SpecifiedTradeProduct><ram:Name>{escape(line.description)}</ram:Name></ram:SpecifiedTradeProduct>"
            "<ram:SpecifiedLineTradeAgreement><ram:NetPriceProductTradePrice>"
            f"<ram:ChargeAmount>{_decimal(line.unit_price)}</ram:ChargeAmount>"
            "</ram:NetPriceProductTradePrice></ram:SpecifiedLineTradeAgreement>"
            "<ram:SpecifiedLineTradeDelivery>"
            # C62 = "unité" (le brouillon ne porte pas encore d'unité de mesure)
            f'<ram:BilledQuantity unitCode="C62">{_decimal(line.quantity)}</ram:BilledQuantity>'
            "</ram:SpecifiedLineTradeDelivery>"
            "<ram:SpecifiedLineTradeSettlement>"
            "<ram:ApplicableTradeTax><ram:TypeCode>VAT</ram:TypeCode>"
            f"<ram:CategoryCode>{_category(line.vat_rate)}</ram:CategoryCode>"
            f"<ram:RateApplicablePercent>{_decimal(line.vat_rate)}</ram:RateApplicablePercent>"
            "</ram:ApplicableTradeTax>"
            "<ram:SpecifiedTradeSettlementLineMonetarySummation>"
            f"<ram:LineTotalAmount>{_amount(line.total)}</ram:LineTotalAmount>"
            "</ram:SpecifiedTradeSettlementLineMonetarySummation>"
            "</ram:SpecifiedLineTradeSettlement>"
            "</ram:IncludedSupplyChainTradeLineItem>"
        )

    out.append("<ram:ApplicableHeaderTradeAgreement>")
    out.append(_party("SellerTradeParty", invoice.seller_name, invoice.seller_siren,
                      invoice.seller_vat, invoice.seller_address))
    out.append(_party("BuyerTradeParty", invoice.buyer_name, invoice.buyer_siren, None, invoice.buyer_address))
    out.append("</ram:ApplicableHeaderTradeAgreement>")
    out.append("<ram:ApplicableHeaderTradeDelivery/>")

    out.append("<ram:ApplicableHeaderTradeSettlement>")
    out.append(f"<ram:InvoiceCurrencyCode>{cur}</ram:InvoiceCurrencyCode>")
    for tax in invoice.taxes:
        out.append(
            "<ram:ApplicableTradeTax>"
            f"<ram:CalculatedAmount>{_amount(tax.tax_amount)}</ram:CalculatedAmount>"
            "<ram:TypeCode>VAT</ram:TypeCode>"
            f"<ram:BasisAmount>{_amount(tax.taxable_amount)}</ram:BasisAmount>"
            f"<ram:CategoryCode>{_category(tax.rate)}</ram:CategoryCode>"
            f"<ram:RateApplicablePercent>{_decimal(tax.rate)}</ram:RateApplicablePercent>"
            "</ram:ApplicableTradeTax>"
        )
    # BR-CO-25 : un montant dû positif exige une échéance ou des conditions de paiement.
    out.append("<ram:SpecifiedTradePaymentTerms>")
    if invoice.due_date:
        out.append(
            "<ram:DueDateDateTime>"
            f'<udt:DateTimeString format="102">{invoice.due_date:%Y%m%d}</udt:DateTimeString>'
            "</ram:DueDateDateTime>"
        )
    else:
        out.append("<ram:Description>Paiement à réception de la facture.</ram:Description>")
    out.append("</ram:SpecifiedTradePaymentTerms>")
    out.append(
        "<ram:SpecifiedTradeSettlementHeaderMonetarySummation>"
        f"<ram:LineTotalAmount>{_amount(invoice.total_ht)}</ram:LineTotalAmount>"
        f"<ram:TaxBasisTotalAmount>{_amount(invoice.total_ht)}</ram:TaxBasisTotalAmount>"
        f'<ram:TaxTotalAmount currencyID="{cur}">{_amount(invoice.total_vat)}</ram:TaxTotalAmount>'
        f"<ram:GrandTotalAmount>{_amount(invoice.total_ttc)}</ram:GrandTotalAmount>"
        f"<ram:DuePayableAmount>{_amount(invoice.total_ttc)}</ram:DuePayableAmount>"
        "</ram:SpecifiedTradeSettlementHeaderMonetarySummation>"
    )
    out.append("</ram:ApplicableHeaderTradeSettlement>")
    out.append("</rsm:SupplyChainTradeTransaction></rsm:CrossIndustryInvoice>")
    return "".join(out).encode("utf-8")


# --------------------------------------------------------------------------- PDF

def _find_font(filename: str) -> str:
    for directory in FONT_DIRS:
        path = os.path.join(directory, filename)
        if directory and os.path.isfile(path):
            return path
    raise RuntimeError(f"Police {filename} introuvable (installer fonts-dejavu-core ou définir FACTURX_FONT_DIR).")


def _register_fonts() -> None:
    global _fonts_registered
    if not _fonts_registered:
        # On n'utilise jamais les polices standard de reportlab (Helvetica...) :
        # elles ne sont pas embarquées, ce qui est interdit en PDF/A.
        pdfmetrics.registerFont(TTFont("DejaVu", _find_font("DejaVuSans.ttf")))
        pdfmetrics.registerFont(TTFont("DejaVu-Bold", _find_font("DejaVuSans-Bold.ttf")))
        _fonts_registered = True


def _money(value: Decimal, currency: str) -> str:
    text = f"{value.quantize(TWO, rounding=ROUND_HALF_UP):,.2f}".replace(",", " ").replace(".", ",")
    return f"{text} {'€' if currency == 'EUR' else currency}"


def _num(value: Decimal) -> str:
    return _decimal(value).replace(".", ",")


def render_pdf(invoice: Invoice) -> bytes:
    """Représentation visuelle de la facture (la partie lisible par un humain)."""
    _register_fonts()
    buffer = io.BytesIO()
    # initialFontName : sans cela reportlab référence Helvetica (police non embarquée,
    # interdite en PDF/A) dans les ressources de chaque page.
    c = canvas.Canvas(buffer, pagesize=A4, pageCompression=1, invariant=1,
                      initialFontName="DejaVu", initialFontSize=9)
    c.setTitle(f"Facture {invoice.number}")
    c.setAuthor(invoice.seller_name)
    c.setSubject("Facture Factur-X")
    c.setCreator("FacturX - generateur F13")
    width, height = A4
    left, right = 18 * mm, width - 18 * mm
    dark = HexColor("#1a2744")
    grey = HexColor("#6b7280")

    def text(x, y, value, size=9.5, bold=False, color=dark, align="left"):
        c.setFont("DejaVu-Bold" if bold else "DejaVu", size)
        c.setFillColor(color)
        if align == "right":
            c.drawRightString(x, y, value)
        else:
            c.drawString(x, y, value)

    def wrap(value: str, size: float, max_width: float) -> list[str]:
        words, rows, current = value.split(), [], ""
        for word in words:
            candidate = f"{current} {word}".strip()
            if pdfmetrics.stringWidth(candidate, "DejaVu", size) <= max_width or not current:
                current = candidate
            else:
                rows.append(current)
                current = word
        return rows + ([current] if current else [])

    def header(y):
        text(left, y, "FACTURE", 20, bold=True)
        text(right, y, f"N° {invoice.number}", 11, bold=True, align="right")
        y -= 16
        text(right, y, f"Date : {invoice.issue_date:%d/%m/%Y}", align="right")
        if invoice.due_date:
            y -= 13
            text(right, y, f"Échéance : {invoice.due_date:%d/%m/%Y}", align="right")
        return y

    def party(x, y, title, name, siren, vat, address):
        text(x, y, title.upper(), 8, bold=True, color=grey)
        y -= 14
        text(x, y, name, 10.5, bold=True)
        for row in ([address] if address else []) + ([f"SIREN : {siren}"] if siren else []) + (
            [f"TVA : {vat}"] if vat else []
        ):
            for chunk in wrap(row, 9, 80 * mm):
                y -= 12
                text(x, y, chunk, 9)
        return y

    # Bords droits des colonnes numériques (alignées à droite).
    x_qty, x_price, x_vat, x_total = left + 104 * mm, left + 132 * mm, left + 150 * mm, right - 2
    desc_width = 84 * mm
    page = [1]

    def table_header(y):
        c.setFillColor(HexColor("#f3f4f6"))
        c.rect(left, y - 4, right - left, 16, stroke=0, fill=1)
        for label, x, align in (("Désignation", left + 2, "left"), ("Qté", x_qty, "right"),
                                ("P.U. HT", x_price, "right"), ("TVA", x_vat, "right"),
                                ("Total HT", x_total, "right")):
            text(x, y + 1, label, 8.5, bold=True, color=grey, align=align)
        return y - 18

    def new_page():
        text(right, 12 * mm, "Suite page suivante", 8, color=grey, align="right")
        c.showPage()
        page[0] += 1
        text(left, height - 15 * mm, f"Facture N° {invoice.number} (suite)", 9, bold=True)
        text(right, height - 15 * mm, f"Page {page[0]}", 8, color=grey, align="right")

    y = header(height - 25 * mm)
    y_seller = party(left, y - 30, "Vendeur", invoice.seller_name, invoice.seller_siren,
                     invoice.seller_vat, invoice.seller_address)
    y_buyer = party(left + 95 * mm, y - 30, "Client", invoice.buyer_name, invoice.buyer_siren,
                    None, invoice.buyer_address)
    y = table_header(min(y_seller, y_buyer) - 28)

    for line in invoice.lines:
        rows = wrap(line.description, 9, desc_width) or [""]
        needed = 13 * len(rows) + 6
        if y - needed < 45 * mm:
            new_page()
            y = table_header(height - 25 * mm)
        text(left + 2, y, rows[0], 9)
        text(x_qty, y, _num(line.quantity), 9, align="right")
        text(x_price, y, _money(line.unit_price, invoice.currency), 9, align="right")
        text(x_vat, y, f"{_num(line.vat_rate)} %", 9, align="right")
        text(x_total, y, _money(line.total, invoice.currency), 9, align="right")
        for extra in rows[1:]:
            y -= 12
            text(left + 2, y, extra, 9)
        y -= 6
        c.setStrokeColor(HexColor("#e5e7eb"))
        c.line(left, y + 2, right, y + 2)
        y -= 12

    if y - 14 * (len(invoice.taxes) + 3) < 45 * mm:
        new_page()
        y = height - 30 * mm
    y -= 6
    x_label = left + 96 * mm
    totals = [("Total HT", invoice.total_ht)] + [
        (f"TVA {_num(t.rate)} % sur {_money(t.taxable_amount, invoice.currency)}", t.tax_amount) for t in invoice.taxes
    ]
    for label, value in totals:
        text(x_label, y, label, 9, color=grey)
        text(x_total, y, _money(value, invoice.currency), 9, align="right")
        y -= 14
    c.setStrokeColor(dark)
    c.line(x_label, y + 8, right, y + 8)
    y -= 4
    text(x_label, y, "Total TTC", 11, bold=True)
    text(x_total, y, _money(invoice.total_ttc, invoice.currency), 11, bold=True, align="right")

    footer = 14 * mm + 10 * len(LEGAL_NOTES)
    for _, note in LEGAL_NOTES:
        text(left, footer, note, 7.5, color=grey)
        footer -= 10
    text(left, footer, "Facture électronique Factur-X : les données structurées (XML CII) sont jointes à ce PDF.",
         7.5, color=grey)
    c.showPage()
    c.save()
    return buffer.getvalue()


# --------------------------------------------------------------------------- assemblage

def generate_facturx(invoice: Invoice, profile: str) -> tuple[bytes, bytes]:
    """Retourne (pdf Factur-X, xml CII)."""
    if profile not in PROFILES:
        raise GenerationError(f"Profil non supporté : {profile}. Profils disponibles : BASIC, EN16931.")
    if not invoice.lines:
        raise GenerationError("La facture doit contenir au moins une ligne.")
    xml = build_cii_xml(invoice, profile)
    pdf = render_pdf(invoice)
    facturx_pdf = generate_from_binary(
        pdf,
        xml,
        flavor="factur-x",
        level=PROFILES[profile][1],
        check_xsd=True,
        pdf_metadata={
            "author": invoice.seller_name,
            "keywords": "Factur-X, facture",
            "title": f"{invoice.seller_name} : Facture {invoice.number}",
            "subject": f"Facture {invoice.number} du {invoice.issue_date:%d/%m/%Y}",
        },
        lang="fr-FR",
    )
    return facturx_pdf, xml
