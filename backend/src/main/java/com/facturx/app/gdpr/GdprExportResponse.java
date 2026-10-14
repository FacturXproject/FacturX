package com.facturx.app.gdpr;

import java.time.OffsetDateTime;
import java.util.List;

public record GdprExportResponse(
		UserData user,
		List<OrganizationData> organizations
	) {

	public record UserData(
			Long id,
			String email,
			String firstName,
			String lastName,
			String status,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt
	) {
	}

	public record OrganizationData(
			Long id,
			String name,
			String role
	) {
	}
}
