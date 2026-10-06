package com.facturx.app.job;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.validation.FacturXValidationService;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JobWorker {

	private final JobRepository jobRepository;
	private final DocumentService documentService;
	private final FacturXValidationService validationService;
	private static final int MAX_ATTEMPTS = 3;

	public JobWorker(
			JobRepository jobRepository,
			DocumentService documentService,
			FacturXValidationService validationService) {

		this.jobRepository = jobRepository;
		this.documentService = documentService;
		this.validationService = validationService;
	}
	
	@Async
	public void process(Job job) {

		if (job.getAttempts() >= MAX_ATTEMPTS) {
			return;
		}

		job.setStatus(JobStatus.PROCESSING);
		job.setStartedAt(Instant.now());
		job.setAttempts(job.getAttempts() + 1);
		job.setError(null);

		jobRepository.save(job);

		try {

			if (job.getType() == JobType.VALIDATION) {

				Document document = job.getDocument();

				byte[] bytes = documentService.readFileBytes(document);

				validationService.validate(
						bytes,
						document.getFilename(),
						document.getId()
				);

			} else {
				throw new UnsupportedOperationException(
						"Job type not implemented yet: " + job.getType()
				);
			}

			job.setStatus(JobStatus.DONE);

		} catch (Exception e) {

			job.setStatus(JobStatus.FAILED);

			job.setError(
					e.getMessage() != null
							? e.getMessage()
							: "Job processing failed"
			);

		} finally {

			job.setFinishedAt(Instant.now());
			jobRepository.save(job);
		}
	}
}
