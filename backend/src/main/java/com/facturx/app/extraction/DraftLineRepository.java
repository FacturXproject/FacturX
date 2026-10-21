package com.facturx.app.extraction;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftLineRepository extends JpaRepository<DraftLine, Long> {
    List<DraftLine> findByDraftInvoiceIdOrderByPositionAsc(Long draftInvoiceId);
}
