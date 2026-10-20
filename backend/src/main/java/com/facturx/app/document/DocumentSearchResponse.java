package com.facturx.app.document;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * F16: one page of search results. An explicit DTO rather than Spring's Page so the
 * JSON shape is a stable contract for the frontend.
 */
public record DocumentSearchResponse(
        List<DocumentHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort
) {
    public static DocumentSearchResponse from(Page<Document> page, String sort) {
        return new DocumentSearchResponse(
                page.getContent().stream().map(DocumentHistoryResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                sort
        );
    }
}
