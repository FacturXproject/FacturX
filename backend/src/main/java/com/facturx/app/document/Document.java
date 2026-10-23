package com.facturx.app.document;

import com.facturx.app.organization.Organization;
import com.facturx.app.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
// F16: every search filters by organisation first, so each index leads with
// organization_id and then the filtered/sorted column. Hibernate (ddl-auto=update)
// creates any missing one by name on startup. Substring search on the filename
// additionally uses a pg_trgm index declared in data.sql (JPA can't express it).
@Table(name = "documents", indexes = {
        @Index(name = "idx_documents_org_uploaded_at", columnList = "organization_id, uploaded_at"),
        @Index(name = "idx_documents_org_status", columnList = "organization_id, status"),
        @Index(name = "idx_documents_org_type", columnList = "organization_id, type"),
        @Index(name = "idx_documents_org_filename", columnList = "organization_id, filename"),
        @Index(name = "idx_documents_org_owner", columnList = "organization_id, owner_id")
})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    private String filename;

    private String type;

    private long size;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    private String storagePath;

    @Column(name = "uploaded_at")
    private Instant uploadedAt = Instant.now();
    public Document() {}

    // getters
    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public User getOwner() { return owner; }
    public String getFilename() { return filename; }
    public String getType() { return type; }
    public long getSize() { return size; }
    public DocumentStatus getStatus() { return status; }
    public String getStoragePath() { return storagePath; }
    public Instant getUploadedAt() { return uploadedAt; }
    // setters
    public void setOrganization(Organization organization) { this.organization = organization; }
    public void setOwner(User owner) { this.owner = owner; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setType(String type) { this.type = type; }
    public void setSize(long size) { this.size = size; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
}
