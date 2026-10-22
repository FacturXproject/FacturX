package com.facturx.app.job;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Service
public class JobService {

	private final JobRepository jobRepository;
	private final DocumentService documentService;
	private final JobWorker jobWorker;

	public JobService(
			JobRepository jobRepository,
			DocumentService documentService,
			JobWorker jobWorker) {

		this.jobRepository = jobRepository;
		this.documentService = documentService;
		this.jobWorker = jobWorker;
	}

	public Job createValidationJob(Long documentId) {
		return createJob(
				documentId,
				JobType.VALIDATION,
				null,
				null
		);
	}

	public Job createGenerationJob(
			Long documentId,
			String profile,
			Long userId) {

		return createJob(
				documentId,
				JobType.GENERATION,
				userId,
				profile
		);
	}

	private synchronized Job createJob(
			Long documentId,
			JobType type,
			Long requestedBy,
			String profile) {

		Document document =
				documentService.getDocument(documentId);

		boolean alreadyRunning =
				jobRepository.existsByDocument_IdAndStatusIn(
						documentId,
						List.of(
								JobStatus.QUEUED,
								JobStatus.PROCESSING
						)
				);

		if (alreadyRunning) {
			throw new JobConflictException(
					"A job is already queued or processing for this document."
			);
		}

		Job job = new Job();

		job.setDocument(document);
		job.setType(type);
		job.setStatus(JobStatus.QUEUED);
		job.setRequestedBy(requestedBy);
		job.setProfile(profile);

		Job savedJob =
				jobRepository.save(job);

		jobWorker.process(savedJob);

		return savedJob;
	}

	public Job getJob(Long jobId) {
		return jobRepository.findById(jobId)
				.orElseThrow(() -> new JobNotFoundException(jobId));
	}

	public synchronized Job retryJob(Long jobId) {
		Job job = getJob(jobId);

		if (job.getStatus() != JobStatus.FAILED) {
			throw new JobRetryException(
					"Only failed jobs can be retried."
			);
		}

		if (job.getAttempts() >= 3) {
			throw new JobRetryException(
					"Maximum number of attempts reached."
			);
		}

		boolean anotherJobRunning =
			jobRepository.existsByDocument_IdAndStatusIn(
					job.getDocument().getId(),
					List.of(
							JobStatus.QUEUED,
							JobStatus.PROCESSING
					)
			);

	if (anotherJobRunning) {
		throw new JobConflictException(
				"A job is already queued or processing for this document."
		);
	}

		job.setStatus(JobStatus.QUEUED);
		job.setError(null);
		job.setFinishedAt(null);

		Job savedJob = jobRepository.save(job);

		jobWorker.process(savedJob);

		return savedJob;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void recoverInterruptedJobs() {

		List<Job> interruptedJobs =
				jobRepository.findByStatusIn(
						List.of(
								JobStatus.QUEUED,
								JobStatus.PROCESSING
						)
				);

		for (Job job : interruptedJobs) {
			job.setStatus(JobStatus.FAILED);
			job.setError(
					"Job interrupted by application restart."
			);
			job.setFinishedAt(Instant.now());
		}

		jobRepository.saveAll(interruptedJobs);
	}

}
