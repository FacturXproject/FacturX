package com.facturx.app.document;

import com.facturx.app.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;

    private String contentType;

    private long size;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    public Document() {}

    // getters
    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public byte[] getData() { return data; }
    public User getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    // setters
    public void setFilename(String filename) { this.filename = filename; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setSize(long size) { this.size = size; }
    public void setData(byte[] data) { this.data = data; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
}