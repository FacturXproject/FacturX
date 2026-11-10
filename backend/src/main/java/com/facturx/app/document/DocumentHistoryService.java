package com.facturx.app.document;

import com.facturx.app.generation.GeneratedInvoice;
import com.facturx.app.generation.GeneratedInvoiceRepository;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class DocumentHistoryService {

    private final DocumentRepository documentRepository;
    private final PermissionService permissionService;
    private final GeneratedInvoiceRepository generatedInvoiceRepository;

    public DocumentHistoryService(
            DocumentRepository documentRepository,
            PermissionService permissionService,
            GeneratedInvoiceRepository generatedInvoiceRepository) {

        this.documentRepository = documentRepository;
        this.permissionService = permissionService;
        this.generatedInvoiceRepository = generatedInvoiceRepository;
    }
public Page <DocumentHistoryResponse> getDocuments(Long organizationId, Long userId, Pageable pageable)
{
    
    Page<Document> documents;
    
    if(permissionService.hasPermission(userId, organizationId, Permission.VIEW_ALL_DOCUMENTS))
    {
       documents = documentRepository.findByOrganizationId(organizationId, pageable);
    }
    else if (permissionService.hasPermission(userId, organizationId, Permission.VIEW_OWN_DOCUMENTS))
    {
        documents = documentRepository.findByOrganizationIdAndOwnerId(organizationId, userId, pageable);
    }
    else
    {
        throw new RuntimeException("No permission");
    }
    
    return documents.map(document -> DocumentHistoryResponse.from(document));
}

public DocumentHistoryResponse detail(Long documentId, Long userId)
{
    Document document = documentRepository.findById(documentId)
        .orElseThrow(DocumentNotFoundException::new);
    
    Long organizationId = document.getOrganization().getId();

    if (permissionService.hasPermission(userId, organizationId, Permission.VIEW_ALL_DOCUMENTS))
    {
        return DocumentHistoryResponse.from(document, sourceDocumentId(documentId));
    }
    else if (permissionService.hasPermission(userId, organizationId, Permission.VIEW_OWN_DOCUMENTS) && document.getOwner().getId().equals(userId))
    {
        return DocumentHistoryResponse.from(document, sourceDocumentId(documentId));
    }

    throw new RuntimeException("No permission");
}

// F13 : document d'origine d'une facture generee, s'il existe encore.
private Long sourceDocumentId(Long documentId)
{
    return generatedInvoiceRepository.findFirstByGeneratedDocumentId(documentId)
        .map(GeneratedInvoice::getSourceDocumentId)
        .filter(documentRepository::existsById)
        .orElse(null);
}

}
