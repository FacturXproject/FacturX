package com.facturx.app.validation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationRunRepository extends JpaRepository<ValidationRun, Long> {

    Optional<ValidationRun> findTopByDocumentIdOrderByFinishedAtDesc(Long documentId);
}
