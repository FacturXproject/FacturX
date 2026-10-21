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

# A totals row sitting inside the lines table is not an invoice line.
_TOTALS_LABEL = re.compile(
    r"(?:sous[-\s]?total|total(?:\s+(?:h\.?t\.?|t\.?t\.?c\.?|tva))?|(?:montant\s+)?tva(?:\s+\d{1,2}(?:[.,]\d+)?\s*%)?"
    r"|net\s+[àa]\s+payer)\s*:?",
    re.IGNORECASE,
)

# Below the lines table, outside any grid: the totals block ends the table.
_TOTALS_START = re.compile(
    r"\s*(?:sous[-\s]?total|sub\s*total|total|montant|tva\b|tax\b|net\s+[àa]\s+payer|reste\s+[àa]\s+payer|acompte)",
    re.IGNORECASE,
)

# Column labels of a lines table header, one word at a time. Checked in this
# order: "Montant TVA" is a VAT column, "Prix total" a total, "Prix unitaire" a
# unit price.
_COLUMN_LABELS = [
    ("vat", re.compile(r"tva|vat|tax|taxe|taux|steuer\w*|ust")),
    ("total", re.compile(r"total|sous-total|subtotal|montant|amount|\w*betrag")),
    ("quantity", re.compile(r"qt[eé]s?|qty|\w+/qty|quantit[eé]s?|quantity|nb|nombre|menge")),
    ("unitPrice", re.compile(r"p\.?u\.?|prix|tarif|rate|price|\w+/price|\w*preis")),
    ("description", re.compile(
        r"d[eé]signation|description|libell[eé]|articles?|produits?|prestations?|services?"
        r"|intitul[eé]|d[eé]tails?|objet|nature|items?|beschreibung|bezeichnung"
    )),
]
_AMOUNT_COLUMNS = {"quantity", "unitPrice", "total"}

# An amount inside a cell or a word: "3", "1 080,00", "1.080,00", "1,080.50"
# (thousands separated by a space, a no-break space, a dot or a comma).
_NUMBER = re.compile(r"[-+]?\d{1,3}(?:[   .,]\d{3})+(?:[.,]\d+)?(?!\d)|[-+]?\d+(?:[.,]\d+)?")
# One word of a text line that is an amount, maybe with its currency or a "%".
_NUMBER_WORD = re.compile(r"[€$£]?(?P<n>" + _NUMBER.pattern + r")(?:€|\$|£|EUR)?(?P<pct>%)?", re.IGNORECASE)
_CURRENCY_WORD = re.compile(r"€|\$|£|EUR|USD", re.IGNORECASE)
_THOUSANDS_HEAD = re.compile(r"[-+]?\d{1,3}")
_THOUSANDS_TAIL = re.compile(r"\d{3}(?:[ .,]|$)")

# Words closer than this (in font sizes) belong to the same value: "3 750,00",
# "Office 365". Columns are always further apart.
_SAME_VALUE_GAP = 0.45


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


# --- Invoice lines ---------------------------------------------------------------
# Lines are read under the header that names the table columns (Désignation, Qté,
# P.U., Total...): from a ruled table when the PDF draws one, otherwise - most
# invoices have no grid - from the position of the words under that header. A
# table without such a header (a key/value block like "Order Number | 12345", the
# totals block) holds no invoice line.

def _column_of(label: str) -> str | None:
    words = [re.sub(r"^\W+|[^\w.]+$", "", w) for w in label.lower().split()]
    for kind, pattern in _COLUMN_LABELS:
        if any(pattern.fullmatch(w) for w in words if w):
            return kind
    return None


def _is_header(kinds) -> bool:
    return "description" in kinds and len(_AMOUNT_COLUMNS & set(kinds)) >= 2


def _number(value: str | None) -> str | None:
    if not value:
        return None
    match = _NUMBER.search(value.replace(" ", " ").replace(" ", " "))
    return match.group(0) if match else None


def _line(description, quantity, unit_price, total, confidence) -> dict | None:
    amounts = [v for v in (quantity, unit_price, total) if v is not None]
    # A real line has at least two amounts, or a description and its total.
    if len(amounts) < 2 and not (description and total):
        return None
    return {
        "description": description or None,
        "quantity": quantity,
        "unitPrice": unit_price,
        "total": total,
        "confidence": confidence,
    }


def _table_lines(table: list[list]) -> list[dict]:
    rows: list[dict] = []
    columns: dict[str, int] | None = None
    for raw_row in table:
        if raw_row is None:
            continue
        # A cell wrapped over several lines in the PDF comes back with line
        # breaks: collapse them, a description is a single line.
        cells = [re.sub(r"\s+", " ", c).strip() if c else "" for c in raw_row]
        if columns is None:
            found: dict[str, int] = {}
            for index, cell in enumerate(cells):
                kind = _column_of(cell)
                if kind and kind not in found:
                    found[kind] = index
            if _is_header(found):
                columns = found
            continue
        # A totals row sitting inside the table is not an invoice line.
        if any(_TOTALS_LABEL.fullmatch(c) for c in cells if c):
            continue

        def cell(kind):
            index = columns.get(kind)
            return cells[index] if index is not None and index < len(cells) else ""

        line = _line(
            cell("description"),
            _number(cell("quantity")),
            _number(cell("unitPrice")),
            _number(cell("total")),
            0.5,
        )
        if line:
            rows.append(line)
    return rows


def _text_rows(page) -> list[dict]:
    # Words of the page grouped into visual lines (same baseline), left to right.
    rows: list[dict] = []
    for word in sorted(page.extract_words(), key=lambda w: (round(w["top"]), w["x0"])):
        word["text"] = word["text"].replace(" ", " ").replace(" ", " ")
        if rows and abs(word["top"] - rows[-1]["top"]) <= 3:
            rows[-1]["words"].append(word)
        else:
            rows.append({"top": word["top"], "words": [word]})
    for row in rows:
        row["words"].sort(key=lambda w: w["x0"])
        row["size"] = max(w["bottom"] - w["top"] for w in row["words"])
        row["text"] = " ".join(w["text"] for w in row["words"])
    return rows


def _header_columns(row: dict) -> list[dict] | None:
    columns: list[dict] = []
    for word in row["words"]:
        kind = _column_of(word["text"])
        if kind and all(c["kind"] != kind for c in columns):
            columns.append({"kind": kind, "x0": word["x0"], "x1": word["x1"]})
        elif columns and word["x0"] - columns[-1]["x1"] < row["size"]:
            # Second word of a label: "P.U. HT", "Prix unitaire", "Total HT".
            columns[-1]["x1"] = word["x1"]
    return columns if _is_header([c["kind"] for c in columns]) else None


def _trailing_amounts(words: list[dict], size: float, limit: int) -> tuple[list[dict], int]:
    """Amounts at the end of a text line, right to left, and how many words are
    left for the description. A number glued to the text before it is part of the
    description ("Licence Office 365"), not a column."""
    gap = size * _SAME_VALUE_GAP
    amounts: list[dict] = []
    percent = False
    i = len(words) - 1
    while i >= 0 and len(amounts) < limit:
        text = words[i]["text"]
        if _CURRENCY_WORD.fullmatch(text):
            i -= 1
            continue
        if text == "%":
            percent = True
            i -= 1
            continue
        match = _NUMBER_WORD.fullmatch(text)
        if not match:
            break
        value, first = match.group("n"), i
        # "3 750,00" drawn as two words.
        while (first > 0 and _THOUSANDS_TAIL.match(value)
               and _THOUSANDS_HEAD.fullmatch(words[first - 1]["text"])
               and words[first]["x0"] - words[first - 1]["x1"] < gap):
            first -= 1
            value = f"{words[first]['text']} {value}"
        before = words[first - 1] if first > 0 else None
        if (before and words[first]["x0"] - before["x1"] < gap
                and not _NUMBER_WORD.fullmatch(before["text"])
                and not _CURRENCY_WORD.fullmatch(before["text"]) and before["text"] != "%"):
            break
        amounts.insert(0, {
            "value": value,
            "x0": words[first]["x0"],
            "x1": words[i]["x1"],
            "percent": percent or bool(match.group("pct")),
        })
        percent = False
        i = first - 1
    return amounts, i + 1


def _assign_columns(amounts: list[dict], columns: list[dict]) -> dict[str, str]:
    """Each amount goes to the header column right above it; a VAT rate is not a
    price. If two amounts land in the same column, fall back to their order."""
    values: dict[str, str] = {}
    for amount in amounts:
        candidates = [c for c in columns if c["kind"] != "description" and (c["kind"] == "vat") == amount["percent"]]
        if not candidates:
            continue
        middle = (amount["x0"] + amount["x1"]) / 2
        best = min(candidates, key=lambda c: (
            max(c["x0"] - amount["x1"], amount["x0"] - c["x1"], 0),
            abs((c["x0"] + c["x1"]) / 2 - middle),
        ))
        if best["kind"] in values:
            break
        values[best["kind"]] = amount["value"]
    else:
        return values
    plain = [a["value"] for a in amounts if not a["percent"]]
    return {
        "quantity": plain[0],
        "total": plain[-1],
        # Without a third amount, quantity and unit price can't be told apart.
        "unitPrice": plain[-2] if len(plain) >= 3 else None,
    }


def _layout_lines(page) -> list[dict]:
    lines: list[dict] = []
    columns = None
    previous_top = None
    for row in _text_rows(page):
        if columns is None:
            columns = _header_columns(row)
            previous_top = row["top"]
            continue
        size = row["size"]
        # The table ends at the totals block or at a blank gap.
        if row["top"] - previous_top > 4 * size or _TOTALS_START.match(row["text"]):
            break
        limit = sum(1 for c in columns if c["kind"] != "description")
        amounts, text_words = _trailing_amounts(row["words"], size, limit)
        description = " ".join(w["text"] for w in row["words"][:text_words])
        if sum(1 for a in amounts if not a["percent"]) >= 2 and description:
            values = _assign_columns(amounts, columns)
            line = _line(description, values.get("quantity"), values.get("unitPrice"), values.get("total"), 0.4)
            if line:
                lines.append(line)
        elif (not amounts and lines and row["top"] - previous_top <= 1.6 * size
              and row["words"][0]["x0"] < min(c["x0"] for c in columns if c["kind"] != "description")):
            # A description wrapped onto the next line.
            lines[-1]["description"] = f"{lines[-1]['description']} {row['text']}"
        previous_top = row["top"]
    return lines


def _extract_lines(pdf: "pdfplumber.PDF") -> list[dict]:
    rows: list[dict] = []
    for page in pdf.pages:
        page_rows: list[dict] = []
        for table in page.extract_tables() or []:
            page_rows += _table_lines(table)
        rows += page_rows or _layout_lines(page)
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
