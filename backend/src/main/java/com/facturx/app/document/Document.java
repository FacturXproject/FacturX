package com.facturx.app.document;

import com.facturx.app.organization.Organization;
import com.facturx.app.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
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

    private LocalDateTime uploadedAt = LocalDateTime.now();

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
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    // setters
    public void setOrganization(Organization organization) { this.organization = organization; }
    public void setOwner(User owner) { this.owner = owner; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setType(String type) { this.type = type; }
    public void setSize(long size) { this.size = size; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
}