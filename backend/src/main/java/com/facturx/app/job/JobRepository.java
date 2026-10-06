package com.facturx.app.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

public interface JobRepository extends JpaRepository<Job, Long> {

	boolean existsByDocument_IdAndStatusIn(Long documentId, Collection<JobStatus> statuses);


}
