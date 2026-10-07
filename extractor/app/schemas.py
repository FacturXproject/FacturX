from pydantic import BaseModel


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
