package com.facturx.app.publicapi;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentNotFoundException;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.DocumentService;
import com.facturx.app.permission.AccessDeniedException;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;
import com.facturx.app.validation.FacturXValidationService;
import com.facturx.app.validation.ValidationReport;
import com.facturx.app.validation.ValidationReportService;
import com.facturx.app.validation.ValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * F17: what the public API does with documents. It adds no business logic of its own -
 * upload, validation, report and deletion are the existing services (F06-F09). What
 * it does add is the access rule applied before each of them:
 *
 * 1. the document must belong to the organization the key is bound to - otherwise it
 *    is answered as "not found", so a key cannot even learn that it exists;
 * 2. the key owner's role in that organization must allow the action, checked through
 *    PermissionService on every request (a member who was downgraded or removed loses
 *    access immediately, without having to revoke the key).
 */
@Service
public class PublicApiService {

    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final PermissionService permissionService;
    private final FacturXValidationService validationService;
    private final ValidationReportService validationReportService;

    public PublicApiService(DocumentRepository documentRepository,
                            DocumentService documentService,
                            PermissionService permissionService,
                            FacturXValidationService validationService,
                            ValidationReportService validationReportService) {
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.permissionService = permissionService;
        this.validationService = validationService;
        this.validationReportService = validationReportService;
    }

    public Page<Document> list(ApiKeyPrincipal key, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "uploadedAt"));

        if (can(key, Permission.VIEW_ALL_DOCUMENTS)) {
            return documentRepository.findByOrganizationId(key.organizationId(), pageable);
        }
        if (can(key, Permission.VIEW_OWN_DOCUMENTS)) {
            return documentRepository.findByOrganizationIdAndOwnerId(key.organizationId(), key.userId(), pageable);
        }
        throw new AccessDeniedException();
    }

    /** A document the key is allowed to read: status, report and download all go through here. */
    public Document get(ApiKeyPrincipal key, Long documentId) {
        Document document = findInKeyOrganization(key, documentId);

        boolean allowed = can(key, Permission.VIEW_ALL_DOCUMENTS)
                || (can(key, Permission.VIEW_OWN_DOCUMENTS) && isOwner(key, document));
        if (!allowed) {
            throw new AccessDeniedException();
        }
        return document;
    }

    public Document upload(ApiKeyPrincipal key, MultipartFile file) {
        require(key, Permission.UPLOAD_DOCUMENT);
        return documentService.upload(file, key.organizationId(), key.userId());
    }

    // Renaming follows the deletion rule: your own document, or any document for a role
    // that manages all of them (DELETE_ANY_DOCUMENT, i.e. an admin).
    public Document rename(ApiKeyPrincipal key, Long documentId, String filename) {
        Document document = findInKeyOrganization(key, documentId);

        boolean allowed = can(key, Permission.DELETE_ANY_DOCUMENT)
                || (can(key, Permission.UPLOAD_DOCUMENT) && isOwner(key, document));
        if (!allowed) {
            throw new AccessDeniedException();
        }

        document.setFilename(filename.trim());
        return documentRepository.save(document);
    }

    public void delete(ApiKeyPrincipal key, Long documentId) {
        findInKeyOrganization(key, documentId);
        // Role rules (admin: any document; others: their own, not yet processed) are
        // the ones already enforced by DocumentService for the web application.
        documentService.deleteDocument(documentId, key.userId());
    }

    public ValidationResult validate(ApiKeyPrincipal key, Long documentId) {
        Document document = findInKeyOrganization(key, documentId);
        require(key, Permission.VALIDATE_DOCUMENT);

        byte[] bytes = documentService.readFileBytes(document);
        return validationService.validate(bytes, document.getFilename(), document.getId());
    }

    public ValidationReport report(ApiKeyPrincipal key, Long documentId) {
        Document document = get(key, documentId);
        return validationReportService.getReportForDocument(document.getId());
    }

    public byte[] content(Document document) {
        return documentService.readFileBytes(document);
    }

    private Document findInKeyOrganization(ApiKeyPrincipal key, Long documentId) {
        Document document = documentService.getDocument(documentId);

        if (document.getOrganization() == null
                || !document.getOrganization().getId().equals(key.organizationId())) {
            throw new DocumentNotFoundException();
        }
        return document;
    }

    private boolean isOwner(ApiKeyPrincipal key, Document document) {
        return document.getOwner() != null && document.getOwner().getId().equals(key.userId());
    }

    private boolean can(ApiKeyPrincipal key, Permission permission) {
        return permissionService.hasPermission(key.userId(), key.organizationId(), permission);
    }

    private void require(ApiKeyPrincipal key, Permission permission) {
        permissionService.requirePermission(key.userId(), key.organizationId(), permission);
    }
}
