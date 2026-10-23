package com.facturx.app.extraction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "draft_invoices")
public class DraftInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // F12 : volontairement nullable. Avec ddl-auto=update, un "add column ... not null"
    // echoue sur une table qui contient deja des brouillons (F11) : la colonne n'est
    // alors jamais creee et toute lecture de draft_invoices plante. Les anciennes
    // lignes ont donc updated_at = null, et getUpdatedAt() retombe sur created_at.
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Tronque a la microseconde : c'est la precision de Postgres, sinon la valeur
    // en memoire et celle relue en base different et le controle de conflit (F12)
    // rejetterait a tort la sauvegarde suivante.
    private static OffsetDateTime now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public Long getOrganizationId() { return organizationId; }
    public String getSource() { return source; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt != null ? updatedAt : createdAt; }

    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public void setSource(String source) { this.source = source; }

    // Appele explicitement : une correction ne modifie souvent que les champs et
    // les lignes (autres tables), un @PreUpdate ne se declencherait donc pas.
    public void markUpdated() { this.updatedAt = now(); }
}
