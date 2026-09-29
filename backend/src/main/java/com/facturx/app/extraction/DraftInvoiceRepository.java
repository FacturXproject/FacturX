package com.facturx.app.extraction;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftInvoiceRepository extends JpaRepository<DraftInvoice, Long> {
    Optional<DraftInvoice> findFirstByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
