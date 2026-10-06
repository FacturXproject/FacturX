package com.facturx.app.job;

import java.time.Instant;

public record JobResponse(
		Long id,
		Long documentId,
		JobType type,
		JobStatus status,
		int attempts,
		String error,
		Instant startedAt,
		Instant finishedAt
) {

	public static JobResponse from(Job job) {
		return new JobResponse(
				job.getId(),
				job.getDocument().getId(),
				job.getType(),
				job.getStatus(),
				job.getAttempts(),
				job.getError(),
				job.getStartedAt(),
				job.getFinishedAt()
		);
	}
}
