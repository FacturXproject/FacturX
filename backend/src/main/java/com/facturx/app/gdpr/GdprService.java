package com.facturx.app.gdpr;

import com.facturx.app.apikey.ApiKey;
import com.facturx.app.apikey.ApiKeyRepository;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentRepository;
import com.facturx.app.organization.Invitation;
import com.facturx.app.organization.InvitationRepository;
import com.facturx.app.organization.LastAdminRequiredException;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.Role;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GdprService {

		private final UserRepository userRepository;
		private final OrganizationMemberRepository organizationMemberRepository;
		private final DocumentRepository documentRepository;
		private final ApiKeyRepository apiKeyRepository;
		private final InvitationRepository invitationRepository;

		public GdprService(
						UserRepository userRepository,
						OrganizationMemberRepository organizationMemberRepository,
						DocumentRepository documentRepository,
						ApiKeyRepository apiKeyRepository,
						InvitationRepository invitationRepository
		) {
				this.userRepository = userRepository;
				this.organizationMemberRepository = organizationMemberRepository;
				this.documentRepository = documentRepository;
				this.apiKeyRepository = apiKeyRepository;
				this.invitationRepository = invitationRepository;
		}

		public GdprExportResponse exportUserData(Long userId) {

				User user = userRepository.findById(userId)
								.orElseThrow(GdprUserNotFoundException::new);

				List<OrganizationMember> memberships =
								organizationMemberRepository.findByUserId(userId);

				List<GdprExportResponse.OrganizationData> organizations =
								memberships.stream()
												.map(member ->
																new GdprExportResponse.OrganizationData(
																				member.getOrganization().getId(),
																				member.getOrganization().getName(),
																				member.getRole().name()
																)
												)
												.toList();

				GdprExportResponse.UserData userData =
								new GdprExportResponse.UserData(
												user.getId(),
												user.getEmail(),
												user.getFirstName(),
												user.getLastName(),
												user.getStatus(),
												user.getCreatedAt(),
												user.getUpdatedAt()
								);

				List<Document> userDocuments =
								documentRepository.findByOwnerIdOrderByUploadedAtDesc(userId);

				List<GdprExportResponse.DocumentData> documents =
								userDocuments.stream()
												.map(document ->
																new GdprExportResponse.DocumentData(
																				document.getId(),
																				document.getOrganization() != null
																								? document.getOrganization().getId()
																								: null,
																				document.getOrganization() != null
																								? document.getOrganization().getName()
																								: null,
																				document.getFilename(),
																				document.getType(),
																				document.getSize(),
																				document.getStatus().name(),
																				document.getUploadedAt()
																)
												)
												.toList();

				List<ApiKey> userApiKeys =
								apiKeyRepository.findByOwnerIdOrderByCreatedAtDesc(userId);

				List<GdprExportResponse.ApiKeyData> apiKeys =
								userApiKeys.stream()
												.map(apiKey ->
																new GdprExportResponse.ApiKeyData(
																				apiKey.getId(),
																				apiKey.getOrganization().getId(),
																				apiKey.getOrganization().getName(),
																				apiKey.getName(),
																				apiKey.getPrefix(),
																				apiKey.getScopes()
																								.stream()
																								.map(Enum::name)
																								.sorted()
																								.toList(),
																				apiKey.getCreatedAt(),
																				apiKey.getLastUsedAt(),
																				apiKey.getRevokedAt()
																)
												)
												.toList();

				List<Invitation> userInvitations =
								invitationRepository
												.findByEmailIgnoreCaseOrderByCreatedAtDesc(
																user.getEmail()
												);

				List<GdprExportResponse.InvitationData> invitations =
								userInvitations.stream()
												.map(invitation ->
																new GdprExportResponse.InvitationData(
																				invitation.getId(),
																				invitation.getOrganization().getId(),
																				invitation.getOrganization().getName(),
																				invitation.getEmail(),
																				invitation.getRole().name(),
																				invitation.getStatus().name(),
																				invitation.getExpiresAt(),
																				invitation.getCreatedAt()
																)
												)
												.toList();

				return new GdprExportResponse(
								userData,
								organizations,
								documents,
								apiKeys,
								invitations
				);
		}

		@Transactional
		public void deleteOrAnonymizeUser(
						Long userId,
						String confirmation
		) {

				if (!"SUPPRIMER".equals(confirmation)) {
						throw new InvalidGdprConfirmationException();
				}

				User user = userRepository.findById(userId)
								.orElseThrow(GdprUserNotFoundException::new);

				List<OrganizationMember> memberships =
								organizationMemberRepository.findByUserId(userId);

				for (OrganizationMember membership : memberships) {

						if (membership.getRole() == Role.ADMIN) {

								long adminCount =
												organizationMemberRepository
																.countByOrganizationIdAndRole(
																				membership.getOrganization().getId(),
																				Role.ADMIN
																);

								if (adminCount == 1) {
										throw new LastAdminRequiredException();
								}
						}
				}

				boolean hasDocuments =
								documentRepository.existsByOwnerId(userId);

				organizationMemberRepository.deleteAll(memberships);
				organizationMemberRepository.flush();

				if (hasDocuments) {

						user.setEmail(
										"deleted-" + user.getId() + "@anonymized.local"
						);

						user.setFirstName("Deleted");
						user.setLastName("User");
						user.setPasswordHash("ANONYMIZED");
						user.setStatus(User.STATUS_ANONYMIZED);

						userRepository.save(user);

						return;
				}

				userRepository.delete(user);
		}
}
