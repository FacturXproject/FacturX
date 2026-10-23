package com.facturx.app.gdpr;

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

	public GdprService(
			UserRepository userRepository,
			OrganizationMemberRepository organizationMemberRepository
	) {
		this.userRepository = userRepository;
		this.organizationMemberRepository = organizationMemberRepository;
	}

	public GdprExportResponse exportUserData(Long userId) {

		User user = userRepository.findById(userId)
				.orElseThrow(() ->
						new IllegalArgumentException("User not found")
				);

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

		return new GdprExportResponse(
				userData,
				organizations
		);
	}

	@Transactional
	public void anonymizeUser(
			Long userId,
			String confirmation
	) {

		if (!"DELETE".equals(confirmation)) {
			throw new IllegalArgumentException(
					"Confirmation must be DELETE"
			);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() ->
						new IllegalArgumentException("User not found")
				);

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
					throw new IllegalStateException(
							"User is the last admin of an organization"
					);
				}
			}
		}

		organizationMemberRepository.deleteAll(memberships);

		user.setEmail(
				"deleted-" + user.getId() + "@anonymized.local"
		);

		user.setFirstName("Deleted");
		user.setLastName("User");
		user.setPasswordHash("ANONYMIZED");
		user.setStatus(User.STATUS_ANONYMIZED);

		userRepository.save(user);
	}
}
