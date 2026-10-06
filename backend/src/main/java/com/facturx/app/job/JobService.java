package com.facturx.app.job;

import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import org.springframework.stereotype.Service;
import java.util.List;

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

	public synchronized Job createJob(Long documentId, JobType type) {

		Document document = documentService.getDocument(documentId);

		boolean alreadyRunning = jobRepository.existsByDocument_IdAndStatusIn(
				documentId,
				List.of(JobStatus.QUEUED, JobStatus.PROCESSING)
		);

		if (alreadyRunning) {
			throw new IllegalStateException(
					"A job is already queued or processing for this document."
			);
		}

		Job job = new Job();
		job.setDocument(document);
		job.setType(type);
		job.setStatus(JobStatus.QUEUED);

		Job savedJob = jobRepository.save(job);

		jobWorker.process(savedJob);

		return savedJob;
	}

	public Job getJob(Long jobId) {
		return jobRepository.findById(jobId)
				.orElseThrow(() -> new RuntimeException("Job not found"));
	}

	public Job retryJob(Long jobId) {

		Job job = getJob(jobId);

		if (job.getStatus() != JobStatus.FAILED) {
			throw new IllegalStateException(
					"Only failed jobs can be retried."
			);
		}

		if (job.getAttempts() >= 3) {
			throw new IllegalStateException(
					"Maximum number of attempts reached."
			);
		}

		job.setStatus(JobStatus.QUEUED);
		job.setError(null);
		job.setFinishedAt(null);

		Job savedJob = jobRepository.save(job);

		jobWorker.process(savedJob);

		return savedJob;
	}

}
