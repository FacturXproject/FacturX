package com.facturx.app.publicapi;

import com.facturx.app.apikey.ApiKeyPrincipal;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentNotFoundException;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.document.DocumentResponse;
import com.facturx.app.document.DocumentService;
import com.facturx.app.job.Job;
import com.facturx.app.job.JobService;
import com.facturx.app.permission.AccessDeniedException;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;
import com.facturx.app.validation.ValidationReport;
import com.facturx.app.validation.ValidationReportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * F17 : la logique de l'API publique. Elle ne reimplemente rien : elle reutilise les
 * services des features F06-F14, en ajoutant deux regles propres aux cles API :
 * - une cle n'agit que dans SON organisation (un document d'une autre organisation
 *   est un 404, on ne revele pas son existence) ;
 * - une cle ne fait jamais plus que son proprietaire (permissions du role F04).
 */
@Service
public class PublicApiService {

    static final int MAX_PAGE_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final PermissionService permissionService;
    private final ValidationReportService validationReportService;
    private final JobService jobService;

    public PublicApiService(
            DocumentRepository documentRepository,
            DocumentService documentService,
            PermissionService permissionService,
            ValidationReportService validationReportService,
            JobService jobService) {

        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.permissionService = permissionService;
        this.validationReportService = validationReportService;
        this.jobService = jobService;
    }

    public PublicPage<DocumentResponse> listDocuments(ApiKeyPrincipal key, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "uploadedAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Document> documents;
        if (permissionService.hasPermission(key.userId(), key.organizationId(), Permission.VIEW_ALL_DOCUMENTS)) {
            documents = documentRepository.findByOrganizationId(key.organizationId(), pageable);
        } else if (permissionService.hasPermission(key.userId(), key.organizationId(), Permission.VIEW_OWN_DOCUMENTS)) {
            documents = documentRepository.findByOrganizationIdAndOwnerId(key.organizationId(), key.userId(), pageable);
        } else {
            throw new AccessDeniedException();
        }

        return PublicPage.from(documents.map(DocumentResponse::from));
    }

    /** Un document lisible par cette cle, ou 404 / 403. */
    public Document getDocument(ApiKeyPrincipal key, Long documentId) {
        Document document = documentRepository.findById(documentId)
                .filter(doc -> doc.getOrganization() != null
                        && doc.getOrganization().getId().equals(key.organizationId()))
                .orElseThrow(DocumentNotFoundException::new);

        documentService.requireViewPermission(document, key.userId());
        return document;
    }

    public Document upload(ApiKeyPrincipal key, MultipartFile file) {
        permissionService.requirePermission(key.userId(), key.organizationId(), Permission.UPLOAD_DOCUMENT);
        try {
            return documentService.upload(new UploadedFile(file), key.organizationId(), key.userId());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    public ValidationReport getReport(ApiKeyPrincipal key, Long documentId) {
        getDocument(key, documentId);
        return validationReportService.getReportForDocument(documentId);
    }

    public byte[] download(Document document) {
        return documentService.readFileBytes(document);
    }

    public Job requestValidation(ApiKeyPrincipal key, Long documentId) {
        Document document = getDocument(key, documentId);
        permissionService.requirePermission(key.userId(), key.organizationId(), Permission.VALIDATE_DOCUMENT);
        return jobService.createValidationJob(document.getId());
    }
}
