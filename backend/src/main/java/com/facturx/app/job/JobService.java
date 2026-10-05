package com.facturx.app.job;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import org.springframework.stereotype.Service;

@Service
public class JobService {

	private final JobRepository jobRepository;
	private final DocumentService documentService;

	public JobService(
			JobRepository jobRepository,
			DocumentService documentService) {

		this.jobRepository = jobRepository;
		this.documentService = documentService;
	}

	public Job createJob(Long documentId, JobType type) {

		Document document = documentService.getDocument(documentId);

		Job job = new Job();
		job.setDocument(document);
		job.setType(type);
		job.setStatus(JobStatus.QUEUED);

		return jobRepository.save(job);
	}
}
