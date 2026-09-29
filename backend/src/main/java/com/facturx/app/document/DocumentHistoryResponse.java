package com.facturx.app.document;

import java.time.Instant;

public record DocumentHistoryResponse(
        Long id,
        Long organizationId,
        String organizationName,
        Long ownerId,
        String ownerName,
        String filename,
        String type,
        long size,
        DocumentStatus status,
        Instant uploadedAt
) {
    public static DocumentHistoryResponse from(Document document) {
        return new DocumentHistoryResponse(
                document.getId(),
                document.getOrganization() != null ? document.getOrganization().getId() : null,
                document.getOrganization() !=null ? document.getOrganization().getName() : null,
                document.getOwner() != null ? document.getOwner().getId() : null,
                document.getOwner() != null ? document.getOwner().getFirstName() + " " + document.getOwner().getLastName() : null,
                document.getFilename(),
                document.getType(),
                document.getSize(),
                document.getStatus(),
                document.getUploadedAt()
        );
    }

}
