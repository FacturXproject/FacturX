package com.facturx.app.job;

public class JobRetryException extends RuntimeException {

	public JobRetryException(String message) {
		super(message);
	}
}
