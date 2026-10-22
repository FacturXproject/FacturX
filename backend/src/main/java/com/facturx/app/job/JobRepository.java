package com.facturx.app.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

	boolean existsByDocument_IdAndStatusIn(Long documentId, Collection<JobStatus> statuses);

	List<Job> findByStatusIn(Collection<JobStatus> statuses);
}
