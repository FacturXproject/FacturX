"""Deterministic, regex/heuristic field extraction from a plain invoice PDF.

This is not OCR and not machine learning: it reads the text layer of the PDF
(pdfplumber) and looks for known French invoice patterns (SIREN, VAT number,
totals, dates...). Every extracted value carries a confidence score in [0, 1]
so a human can tell which fields to double-check before generation (F12).

No field is required to be found: a missing match just yields a null value
with confidence 0.0, so a hard-to-read PDF still produces a usable (if mostly
empty) draft instead of failing the request.
"""

import io
import re
from dataclasses import dataclass, field

import pdfplumber

SOURCE = "pdf-extraction-v1"

_AMOUNT = r"([\d\s]{1,12}[.,]\d{2})"
_DATE = r"(\d{2}[\/\.\-]\d{2}[\/\.\-]\d{4})"

INVOICE_NUMBER_PATTERNS = [
    (r"(?:facture|invoice)\s*n[°o]?\s*[:\-]?\s*([A-Za-z0-9][A-Za-z0-9\-\/]{2,24})", 0.85),
    (r"\bn[°o]\s*[:\-]\s*([A-Za-z0-9][A-Za-z0-9\-\/]{2,24})", 0.6),
]

SIREN_PATTERNS = [
    (r"siren\s*[:\-]?\s*(\d{3}\s?\d{3}\s?\d{3})", 0.85),
]

VAT_PATTERNS = [
    # Labelled match first: the bare pattern below matches everything this one
    # does, so listed second it could never be reached.
    (r"tva\s*intra\w*\s*[:\-]?\s*(FR\s?\d{2}\s?\d{9})", 0.9),
    (r"\b(FR\s?\d{2}\s?\d{9})\b", 0.85),
]

VAT_RATE_PATTERNS = [
    (r"tva\s*(\d{1,2}(?:[.,]\d+)?)\s*%", 0.75),
]

TOTAL_HT_PATTERNS = [
    (r"total\s*ht\s*[:\-]?\s*" + _AMOUNT, 0.85),
]

TOTAL_TTC_PATTERNS = [
    (r"total\s*ttc\s*[:\-]?\s*" + _AMOUNT, 0.85),
]

TOTAL_VAT_PATTERNS = [
    (r"(?:montant\s*)?tva\s*(?:\d{1,2}(?:[.,]\d+)?\s*%)?\s*[:\-]?\s*" + _AMOUNT, 0.65),
]

LEGAL_FORM = r"(?:SARL|SAS(?:U)?|SA|EURL|EI|SCI)"
SELLER_NAME_PATTERN = re.compile(r"^.*\b" + LEGAL_FORM + r"\b.*$", re.IGNORECASE | re.MULTILINE)

# Accents are optional: invoices in capitals ("FACTURE A :") or produced by
# software that drops them ("Facture a :") are common.
BUYER_BLOCK_PATTERN = re.compile(
    r"(?:factur[ée]e?\s*[àa]|adress[ée]e?\s*[àa]|destinataire|client)\s*[:\n]\s*\n?(.+)",
    re.IGNORECASE,
)

POSTAL_ADDRESS_LINE = re.compile(r".{0,60}\b\d{5}\b.{0,60}")

# Table cells: a pure number/amount ("3", "1 080,00", "50,00 €") and a VAT-rate
# cell ("20 %"). Anything else is text, even if it contains a digit
# ("Licence Office 365", a reference like "A12").
_NUMBER_CELL = re.compile(r"-?\d[\d\s]*(?:[.,]\d+)?\s*(?:€|EUR)?", re.IGNORECASE)
_PERCENT_CELL = re.compile(r"\d{1,2}(?:[.,]\d+)?\s*%")

# A totals row sitting inside the lines table is not an invoice line.
_TOTALS_LABEL = re.compile(
    r"(?:sous[-\s]?total|total(?:\s+(?:h\.?t\.?|t\.?t\.?c\.?|tva))?|(?:montant\s+)?tva(?:\s+\d{1,2}(?:[.,]\d+)?\s*%)?"
    r"|net\s+[àa]\s+payer)\s*:?",
    re.IGNORECASE,
)


@dataclass
class Extracted:
    fields: dict[str, tuple[str | None, float]] = field(default_factory=dict)
    lines: list[dict] = field(default_factory=list)

    def set(self, name: str, value: str | None, confidence: float):
        self.fields[name] = (value.strip() if value else None, confidence if value else 0.0)


def _find_first(patterns, text: str) -> tuple[str | None, float]:
    for pattern, confidence in patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            return match.group(1).strip(), confidence
    return None, 0.0


def _find_dates(text: str) -> tuple[tuple[str | None, float], tuple[str | None, float]]:
    invoice_date = (None, 0.0)
    due_date = (None, 0.0)

    for line in text.splitlines():
        match = re.search(_DATE, line)
        if not match:
            continue
        if re.search(r"facture|émission", line, re.IGNORECASE) and invoice_date[0] is None:
            invoice_date = (match.group(1), 0.8)
        elif re.search(r"échéance|due", line, re.IGNORECASE) and due_date[0] is None:
            due_date = (match.group(1), 0.8)

    if invoice_date[0] is None or due_date[0] is None:
        all_dates = re.findall(_DATE, text)
        if invoice_date[0] is None and len(all_dates) >= 1:
            invoice_date = (all_dates[0], 0.5)
        if due_date[0] is None and len(all_dates) >= 2:
            due_date = (all_dates[1], 0.5)

    return invoice_date, due_date


def _find_seller(text: str) -> tuple[str | None, float]:
    match = SELLER_NAME_PATTERN.search(text)
    if match:
        return match.group(0).strip(), 0.6
    return None, 0.0


def _find_buyer(text: str) -> tuple[str | None, float]:
    match = BUYER_BLOCK_PATTERN.search(text)
    if match:
        candidate = match.group(1).strip().splitlines()[0].strip()
        if candidate:
            return candidate, 0.55
    return None, 0.0


def _find_address_near(text: str, anchor: str | None) -> tuple[str | None, float]:
    if not anchor:
        return None, 0.0
    idx = text.find(anchor)
    if idx == -1:
        return None, 0.0
    window = text[idx:idx + 200]
    match = POSTAL_ADDRESS_LINE.search(window)
    if match:
        address = match.group(0).strip()
        # "06000 Nice" alone: the street is on the line above (but that line
        # must not be the anchor itself, nor a labelled line such as SIREN).
        if re.match(r"\d{5}\b", address):
            before = window[:match.start()].strip().splitlines()
            if len(before) >= 2:
                street = before[-1].strip()
                if street and ":" not in street:
                    address = f"{street}, {address}"
        return address, 0.5
    return None, 0.0


def _find_siren_near(text: str, anchor: str | None) -> tuple[str | None, float]:
    # Reuses SIREN_PATTERNS (otherwise only applied once, for the seller) in a
    # window right after the buyer block, so a second "SIREN: ..." near the
    # buyer's details isn't missed just because it comes after the seller's.
    if not anchor:
        return None, 0.0
    idx = text.find(anchor)
    if idx == -1:
        return None, 0.0
    window = text[idx:idx + 300]
    return _find_first(SIREN_PATTERNS, window)


def _extract_lines(pdf: "pdfplumber.PDF") -> list[dict]:
    rows: list[dict] = []
    for page in pdf.pages:
        for table in page.extract_tables() or []:
            for raw_row in table:
                if raw_row is None:
                    continue
                # A cell wrapped over several lines in the PDF comes back with
                # line breaks: collapse them, a description is a single line.
                cells = [re.sub(r"\s+", " ", c).strip() if c else "" for c in raw_row]
                if not any(cells):
                    continue
                # Skip header-looking rows (no digits at all - a real invoice
                # line always has a quantity or an amount somewhere).
                if not any(re.search(r"\d", c) for c in cells):
                    continue
                # A TVA/VAT-rate cell (e.g. "20 %") is not a price - excluded here
                # so it can't be mistaken for unit_price/total when a table has a
                # separate VAT column (Qte | P.U. | TVA | Total).
                amount_cells = [c for c in cells if _NUMBER_CELL.fullmatch(c)]
                text_cells = [
                    c for c in cells
                    if c and not _NUMBER_CELL.fullmatch(c) and not _PERCENT_CELL.fullmatch(c)
                ]
                # No pure number at all (e.g. a header like "Prix 2026"), or a
                # totals row inside the table: not an invoice line.
                if not amount_cells:
                    continue
                if any(_TOTALS_LABEL.fullmatch(c) for c in text_cells):
                    continue
                # The description is the longest *text* cell - taking the longest
                # cell overall picked an amount whenever the description was
                # shorter than it ("Audit" vs "240,00").
                description = max(text_cells, key=len) if text_cells else ""
                quantity = amount_cells[0]
                total = amount_cells[-1]
                # Need at least 3 distinct amount cells to tell quantity and unit
                # price apart unambiguously - otherwise leave unit_price unset
                # rather than guessing (e.g. duplicating quantity or total).
                unit_price = amount_cells[-2] if len(amount_cells) >= 3 else None
                rows.append({
                    "description": description or None,
                    "quantity": quantity,
                    "unitPrice": unit_price,
                    "total": total,
                    "confidence": 0.5,
                })
    return rows


def extract_fields(pdf_bytes: bytes) -> Extracted:
    result = Extracted()

    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        text = "\n".join(page.extract_text() or "" for page in pdf.pages)
        result.lines = _extract_lines(pdf)

    number, number_conf = _find_first(INVOICE_NUMBER_PATTERNS, text)
    result.set("invoiceNumber", number, number_conf)

    invoice_date, due_date = _find_dates(text)
    result.set("invoiceDate", invoice_date[0], invoice_date[1])
    result.set("dueDate", due_date[0], due_date[1])

    seller_name, seller_conf = _find_seller(text)
    result.set("sellerName", seller_name, seller_conf)

    seller_siren, seller_siren_conf = _find_first(SIREN_PATTERNS, text)
    result.set("sellerSiren", seller_siren, seller_siren_conf)

    seller_vat, seller_vat_conf = _find_first(VAT_PATTERNS, text)
    result.set("sellerVat", seller_vat, seller_vat_conf)

    seller_address, seller_address_conf = _find_address_near(text, seller_name)
    result.set("sellerAddress", seller_address, seller_address_conf)

    buyer_name, buyer_conf = _find_buyer(text)
    result.set("buyerName", buyer_name, buyer_conf)

    buyer_siren, buyer_siren_conf = _find_siren_near(text, buyer_name)
    result.set("buyerSiren", buyer_siren, buyer_siren_conf)

    buyer_address, buyer_address_conf = _find_address_near(text, buyer_name)
    result.set("buyerAddress", buyer_address, buyer_address_conf)

    total_ht, total_ht_conf = _find_first(TOTAL_HT_PATTERNS, text)
    result.set("totalHt", total_ht, total_ht_conf)

    vat_rate, vat_rate_conf = _find_first(VAT_RATE_PATTERNS, text)
    result.set("vatRate", vat_rate, vat_rate_conf)

    total_vat, total_vat_conf = _find_first(TOTAL_VAT_PATTERNS, text)
    result.set("totalVat", total_vat, total_vat_conf)

    total_ttc, total_ttc_conf = _find_first(TOTAL_TTC_PATTERNS, text)
    result.set("totalTtc", total_ttc, total_ttc_conf)

    return result
