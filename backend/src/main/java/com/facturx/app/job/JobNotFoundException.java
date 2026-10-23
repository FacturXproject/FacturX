package com.facturx.app.job;

public class JobNotFoundException extends RuntimeException {

	public JobNotFoundException(Long jobId) {
		super("Job not found: " + jobId);
	}
}
