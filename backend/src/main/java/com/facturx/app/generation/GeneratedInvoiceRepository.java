package com.facturx.app.generation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneratedInvoiceRepository extends JpaRepository<GeneratedInvoice, Long> {
    List<GeneratedInvoice> findBySourceDocumentIdOrderByCreatedAtDesc(Long sourceDocumentId);
}
