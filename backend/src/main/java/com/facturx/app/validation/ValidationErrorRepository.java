package com.facturx.app.validation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationErrorRepository extends JpaRepository<ValidationErrorEntity, Long> {

    List<ValidationErrorEntity> findByRunId(Long runId);
}
