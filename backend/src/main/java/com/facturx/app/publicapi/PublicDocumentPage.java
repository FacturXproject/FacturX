package com.facturx.app.publicapi;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentResponse;
import java.util.List;
import org.springframework.data.domain.Page;

// A fixed, documented shape for paginated lists - not Spring's internal Page JSON,
// which is not meant to be a stable contract for outside clients.
public record PublicDocumentPage(
        List<DocumentResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
    public static PublicDocumentPage from(Page<Document> documents) {
        return new PublicDocumentPage(
                documents.getContent().stream().map(DocumentResponse::from).toList(),
                documents.getNumber(),
                documents.getSize(),
                documents.getTotalElements(),
                documents.getTotalPages()
        );
    }
}
