package com.facturx.app.invoice;

import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.document.FileValidator;

@Service
public class InvoiceViewService {

	private final DocumentService documentService;
	private final CiiInvoiceParser ciiInvoiceParser;
	private final PermissionService permissionService;

	public InvoiceViewService(
			DocumentService documentService,
			CiiInvoiceParser ciiInvoiceParser,
			PermissionService permissionService
	) {
		this.documentService = documentService;
		this.ciiInvoiceParser = ciiInvoiceParser;
		this.permissionService = permissionService;
	}

	public InvoiceViewResponse getInvoiceView(
			Long documentId,
			Long currentUserId
	) {
		Document document = documentService.getDocument(documentId);

		Long organizationId = document.getOrganization().getId();

		boolean canView = permissionService.hasPermission(
				currentUserId,
				organizationId,
				Permission.VIEW_ALL_DOCUMENTS
		);

		if (!canView
				&& permissionService.hasPermission(
						currentUserId,
						organizationId,
						Permission.VIEW_OWN_DOCUMENTS
				)
				&& document.getOwner() != null
				&& document.getOwner().getId().equals(currentUserId)) {

			canView = true;
		}

		if (!canView) {
			throw new AccessDeniedException(
					"Access denied to this document."
			);
		}

		byte[] bytes = documentService.readFileBytes(document);

		if (!FileValidator.isXml(bytes)) {
			throw new InvalidInvoiceXmlException();
		}

		return ciiInvoiceParser.parse(bytes);
	}

	public String getRawXml(
		Long documentId,
		Long currentUserId)
	{
		Document document = documentService.getDocument(documentId);

		Long organizationId = document.getOrganization().getId();

		boolean canView = permissionService.hasPermission(
				currentUserId,
				organizationId,
				Permission.VIEW_ALL_DOCUMENTS
		);

		if (!canView
				&& permissionService.hasPermission(
						currentUserId,
						organizationId,
						Permission.VIEW_OWN_DOCUMENTS
				)
				&& document.getOwner() != null
				&& document.getOwner().getId().equals(currentUserId)) {

			canView = true;
		}

		if (!canView) {
			throw new AccessDeniedException(
					"Access denied to this document."
			);
		}

		byte[] bytes = documentService.readFileBytes(document);

		if (!FileValidator.isXml(bytes)) {
			throw new InvalidInvoiceXmlException();
		}

		return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
	}
}
