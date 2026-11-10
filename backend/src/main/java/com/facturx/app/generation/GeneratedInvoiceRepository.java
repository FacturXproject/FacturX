package com.facturx.app.generation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedInvoiceRepository extends JpaRepository<GeneratedInvoice, Long> {
    List<GeneratedInvoice> findBySourceDocumentIdOrderByCreatedAtDesc(Long sourceDocumentId);

    Optional<GeneratedInvoice> findFirstByGeneratedDocumentId(Long generatedDocumentId);
}
