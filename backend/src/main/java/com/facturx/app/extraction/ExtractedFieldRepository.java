package com.facturx.app.extraction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractedFieldRepository extends JpaRepository<ExtractedField, Long> {
    List<ExtractedField> findByDraftInvoiceId(Long draftInvoiceId);
}
