package com.facturx.app.document;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String filename,
        String contentType,
        long size,
        Long uploadedById,
        LocalDateTime uploadedAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getSize(),
                document.getUploadedBy() != null ? document.getUploadedBy().getId() : null,
                document.getUploadedAt()
        );
    }
}