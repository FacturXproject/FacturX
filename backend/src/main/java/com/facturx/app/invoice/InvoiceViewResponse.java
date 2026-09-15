package com.facturx.app.document;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceViewResponse(
	String invoiceNumber,
	String invoiceDate,
	String currency,
	Party seller,
	Party buyer,
	List<InvoiceLine> lines,
	BigDecimal subtotal,
	BigDecimal vat,
	BigDecimal total
) {
	public record Party(
		String name,
		String address,
		String vatId
	) {}

	public record InvoiceLine(
		String description,
		BigDecimal quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal
	) {}
}
