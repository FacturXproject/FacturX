package com.facturx.app.validation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "validation_runs", indexes = {
        // F16: "latest run of a document" lookups (report + error search)
        @Index(name = "idx_validation_runs_document", columnList = "document_id")
})
public class ValidationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable until F06 (document upload) exists and a run can be tied to a real document.
    @Column(name = "document_id")
    private Long documentId;

    // Auteur d'une validation brute (POST /api/validate, sans document) : seul lui peut
    // relire le rapport. Null pour les validations liees a un document.
    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false)
    private boolean valid;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer_reached", length = 20)
    private ValidationLayer layerReached;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @PrePersist
    void onCreate() {
        this.startedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public ValidationLayer getLayerReached() {
        return layerReached;
    }

    public void setLayerReached(ValidationLayer layerReached) {
        this.layerReached = layerReached;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
