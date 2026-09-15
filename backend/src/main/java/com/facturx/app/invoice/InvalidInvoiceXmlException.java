package com.facturx.app.document;

public class InvalidInvoiceXmlException extends RuntimeException {

	public InvalidInvoiceXmlException() {
		super("Invalid or malformed invoice XML.");
	}

	public InvalidInvoiceXmlException(Throwable cause) {
		super("Invalid or malformed invoice XML.", cause);
	}
}
