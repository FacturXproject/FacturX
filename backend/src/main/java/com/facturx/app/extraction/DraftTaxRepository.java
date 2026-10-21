package com.facturx.app.extraction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftTaxRepository extends JpaRepository<DraftTax, Long> {
    List<DraftTax> findByDraftInvoiceIdOrderByPositionAsc(Long draftInvoiceId);
}
