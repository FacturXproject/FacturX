package com.facturx.app.permission;

import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.Role;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class PermissionService {

	private final OrganizationMemberRepository memberRepository;

	private static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = Map.of(

		Role.ADMIN, EnumSet.of(
			Permission.UPLOAD_DOCUMENT,
			Permission.VIEW_ALL_DOCUMENTS,
			Permission.VIEW_OWN_DOCUMENTS,
			Permission.VALIDATE_DOCUMENT,
			Permission.INVITE_MEMBER,
			Permission.MANAGE_MEMBERS,
			Permission.MANAGE_ORGANIZATION,
			Permission.DELETE_ANY_DOCUMENT,
			Permission.DELETE_OWN_PENDING_DOCUMENT
		),

		Role.ACCOUNTANT, EnumSet.of(
			Permission.UPLOAD_DOCUMENT,
			Permission.VIEW_ALL_DOCUMENTS,
			Permission.VIEW_OWN_DOCUMENTS,
			Permission.VALIDATE_DOCUMENT,
			Permission.DELETE_OWN_PENDING_DOCUMENT
		),

		Role.CLIENT, EnumSet.of(
			Permission.UPLOAD_DOCUMENT,
			Permission.VIEW_OWN_DOCUMENTS,
			Permission.DELETE_OWN_PENDING_DOCUMENT
		)
	);

	public PermissionService(OrganizationMemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public boolean hasPermission(
			Long userId,
			Long organizationId,
			Permission permission) {

		return memberRepository
			.findByUserIdAndOrganizationId(userId, organizationId)
			.map(member -> member.getRole())
			.map(role -> ROLE_PERMISSIONS
				.getOrDefault(role, Set.of())
				.contains(permission))
			.orElse(false);
	}

	public void requirePermission(
			Long userId,
			Long organizationId,
			Permission permission) {

		if (!hasPermission(userId, organizationId, permission)) {
			throw new AccessDeniedException();
		}
	}
}
