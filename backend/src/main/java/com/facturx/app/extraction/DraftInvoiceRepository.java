package com.facturx.app.extraction;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DraftInvoiceRepository extends JpaRepository<DraftInvoice, Long> {
    Optional<DraftInvoice> findFirstByDocumentIdOrderByCreatedAtDesc(Long documentId);

    // F12 : meme recherche, mais avec verrou ligne (SELECT ... FOR UPDATE) pendant
    // une correction : deux sauvegardes simultanees ne peuvent pas passer toutes les
    // deux le controle de conflit sur updated_at.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DraftInvoice> findTopByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
