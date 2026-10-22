package com.facturx.app.job;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.generation.GenerationResponse;
import com.facturx.app.generation.GenerationService;
import com.facturx.app.generation.GenerationStatus;
import com.facturx.app.validation.FacturXValidationService;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JobWorker {

	private final JobRepository jobRepository;
	private final DocumentService documentService;
	private final FacturXValidationService validationService;
	private final GenerationService generationService;

	private static final int MAX_ATTEMPTS = 3;

	public JobWorker(
			JobRepository jobRepository,
			DocumentService documentService,
			FacturXValidationService validationService,
			GenerationService generationService) {

		this.jobRepository = jobRepository;
		this.documentService = documentService;
		this.validationService = validationService;
		this.generationService = generationService;
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

				processValidation(job);

			} else if (job.getType() == JobType.GENERATION) {

				processGeneration(job);

			} else {

				throw new UnsupportedOperationException(
						"Job type not implemented: " + job.getType()
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

	private void processValidation(Job job) {

		/* //test failure job
		boolean forceFailure = true;

		if (forceFailure) {
			throw new IllegalStateException("F14 retry test");
		} */

		Document document = job.getDocument();

		byte[] bytes =
				documentService.readFileBytes(document);

		validationService.validate(
				bytes,
				document.getFilename(),
				document.getId()
		);
	}

	private void processGeneration(Job job) {

		GenerationResponse response =
				generationService.generate(
						job.getDocument().getId(),
						job.getProfile(),
						job.getRequestedBy()
				);

		if (response.status() == GenerationStatus.FAILED) {

			throw new IllegalStateException(
					response.errorMessage() != null
							? response.errorMessage()
							: "Document generation failed"
			);
		}
	}
}
