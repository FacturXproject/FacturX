package com.facturx.app.gdpr;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public record GdprExportResponse(
				UserData user,
				List<OrganizationData> organizations,
				List<DocumentData> documents,
				List<ApiKeyData> apiKeys,
				List<InvitationData> invitations
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

		public record DocumentData(
						Long id,
						Long organizationId,
						String organizationName,
						String filename,
						String type,
						long size,
						String status,
						Instant uploadedAt
		) {
		}

		public record ApiKeyData(
						Long id,
						Long organizationId,
						String organizationName,
						String name,
						String prefix,
						List<String> scopes,
						Instant createdAt,
						Instant lastUsedAt,
						Instant revokedAt
		) {
		}

		public record InvitationData(
						Long id,
						Long organizationId,
						String organizationName,
						String email,
						String role,
						String status,
						LocalDateTime expiresAt,
						LocalDateTime createdAt
		) {
		}
}
