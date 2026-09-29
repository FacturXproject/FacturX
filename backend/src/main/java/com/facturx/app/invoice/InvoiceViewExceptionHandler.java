package com.facturx.app.invoice;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InvoiceViewExceptionHandler {

	@ExceptionHandler(InvalidInvoiceXmlException.class)
	public ResponseEntity<Map<String, String>> handleInvalidInvoiceXml() {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
				"error", "INVALID_INVOICE_XML",
				"message", "Le fichier XML est invalide ou mal formé."
		));
	}
}
