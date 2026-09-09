package com.facturx.app.document;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        Long organizationId,
        Long ownerId,
        String filename,
        String type,
        long size,
        DocumentStatus status,
        LocalDateTime uploadedAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOrganization() != null ? document.getOrganization().getId() : null,
                document.getOwner() != null ? document.getOwner().getId() : null,
                document.getFilename(),
                document.getType(),
                document.getSize(),
                document.getStatus(),
                document.getUploadedAt()
        );
    }
}