package com.facturx.app.document;

import org.springframework.stereotype.Service;

@Service
public class InvoiceViewService {

	private final DocumentService documentService;
	private final CiiInvoiceParser ciiInvoiceParser;

	public InvoiceViewService(
			DocumentService documentService,
			CiiInvoiceParser ciiInvoiceParser
	) {
		this.documentService = documentService;
		this.ciiInvoiceParser = ciiInvoiceParser;
	}

	public InvoiceViewResponse getInvoiceView(Long documentId) {
		Document document = documentService.getDocument(documentId);
		byte[] bytes = documentService.readFileBytes(document);

		return ciiInvoiceParser.parse(bytes);
	}
}
