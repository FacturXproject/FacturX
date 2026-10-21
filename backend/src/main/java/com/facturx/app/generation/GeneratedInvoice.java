package com.facturx.app.generation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * F13 : une generation Factur-X. Relie le document source (PDF depose) au document
 * genere, et garde le statut et le rapport de la validation Java qui a suivi.
 */
@Entity
@Table(name = "generated_invoices")
public class GeneratedInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_document_id", nullable = false)
    private Long sourceDocumentId;

    // Null si la generation a echoue avant de produire un fichier.
    @Column(name = "generated_document_id")
    private Long generatedDocumentId;

    @Column(name = "draft_invoice_id", nullable = false)
    private Long draftInvoiceId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "profile", nullable = false, length = 20)
    private String profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GenerationStatus status;

    // Rapport de la validation du fichier genere (F08/F09).
    @Column(name = "validation_run_id")
    private Long validationRunId;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSourceDocumentId() { return sourceDocumentId; }
    public Long getGeneratedDocumentId() { return generatedDocumentId; }
    public Long getDraftInvoiceId() { return draftInvoiceId; }
    public Long getOrganizationId() { return organizationId; }
    public String getProfile() { return profile; }
    public GenerationStatus getStatus() { return status; }
    public Long getValidationRunId() { return validationRunId; }
    public String getErrorMessage() { return errorMessage; }
    public Long getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setSourceDocumentId(Long sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }
    public void setGeneratedDocumentId(Long generatedDocumentId) { this.generatedDocumentId = generatedDocumentId; }
    public void setDraftInvoiceId(Long draftInvoiceId) { this.draftInvoiceId = draftInvoiceId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public void setProfile(String profile) { this.profile = profile; }
    public void setStatus(GenerationStatus status) { this.status = status; }
    public void setValidationRunId(Long validationRunId) { this.validationRunId = validationRunId; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
