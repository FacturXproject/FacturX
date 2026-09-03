package com.facturx.app.organization;

public class LastAdminRequiredException extends RuntimeException {

	public LastAdminRequiredException() {
		super("An organization must have at least one administrator.");
	}
}
