from datetime import date
from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, Field


class FieldValue(BaseModel):
    value: str | None
    confidence: float


class LineValue(BaseModel):
    description: str | None
    quantity: str | None
    unitPrice: str | None
    total: str | None
    confidence: float


class ExtractionResponse(BaseModel):
    fields: dict[str, FieldValue]
    lines: list[LineValue]
    source: str


# --- F13 : génération Factur-X -------------------------------------------------
# Les montants arrivent en texte décimal ("1080.50") et sont lus en Decimal :
# pas de flottant entre Spring Boot et le générateur.


class GenerationParty(BaseModel):
    name: str = Field(min_length=1, max_length=500)
    siren: str | None = Field(default=None, max_length=500)
    vatNumber: str | None = Field(default=None, max_length=500)
    address: str | None = Field(default=None, max_length=500)


class GenerationLine(BaseModel):
    description: str = Field(min_length=1, max_length=500)
    quantity: Decimal
    unitPrice: Decimal
    vatRate: Decimal
    total: Decimal


class GenerationTax(BaseModel):
    rate: Decimal
    taxableAmount: Decimal
    taxAmount: Decimal


class GenerationInvoice(BaseModel):
    number: str = Field(min_length=1, max_length=500)
    issueDate: date
    dueDate: date | None = None
    currency: str = Field(default="EUR", pattern="^[A-Z]{3}$")
    seller: GenerationParty
    buyer: GenerationParty
    lines: list[GenerationLine] = Field(min_length=1, max_length=500)
    taxes: list[GenerationTax] = Field(min_length=1)
    totalHt: Decimal
    totalVat: Decimal
    totalTtc: Decimal


class GenerationRequest(BaseModel):
    profile: Literal["EN16931", "BASIC"] = "EN16931"
    invoice: GenerationInvoice
