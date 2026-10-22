package com.facturx.app.job;

public class JobConflictException extends RuntimeException {

	public JobConflictException(String message) {
		super(message);
	}
}
