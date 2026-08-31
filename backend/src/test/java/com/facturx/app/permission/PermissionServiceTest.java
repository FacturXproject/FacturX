 package com.facturx.app.permission;

import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

	@Mock
	private OrganizationMemberRepository memberRepository;

	private PermissionService permissionService;

	@BeforeEach
	void setUp() {
		permissionService = new PermissionService(memberRepository);
	}

	@Test
	void adminShouldHaveManageMembersPermission() {
		OrganizationMember member = new OrganizationMember();
		member.setRole(Role.ADMIN);

		when(memberRepository.findByUserIdAndOrganizationId(1L, 10L))
			.thenReturn(Optional.of(member));

		boolean result = permissionService.hasPermission(
				1L,
				10L,
				Permission.MANAGE_MEMBERS
		);

		assertTrue(result);
	}

	@Test
	void accountantShouldNotHaveManageMembersPermission() {
		OrganizationMember member = new OrganizationMember();
		member.setRole(Role.ACCOUNTANT);

		when(memberRepository.findByUserIdAndOrganizationId(2L, 10L))
				.thenReturn(Optional.of(member));

		boolean result = permissionService.hasPermission(
				2L,
				10L,
				Permission.MANAGE_MEMBERS
		);

		assertFalse(result);
	}

	@Test
	void clientShouldHaveViewOwnDocumentsPermission() {
		OrganizationMember member = new OrganizationMember();
		member.setRole(Role.CLIENT);

		when(memberRepository.findByUserIdAndOrganizationId(3L, 10L))
				.thenReturn(Optional.of(member));

		boolean result = permissionService.hasPermission(
				3L,
				10L,
				Permission.VIEW_OWN_DOCUMENTS
		);

		assertTrue(result);
	}

	@Test
	void nonMemberShouldHaveNoPermission() {
		when(memberRepository.findByUserIdAndOrganizationId(4L, 10L))
				.thenReturn(Optional.empty());

		boolean result = permissionService.hasPermission(
				4L,
				10L,
				Permission.UPLOAD_DOCUMENT
		);

		assertFalse(result);
	}

	@Test
	void requirePermissionShouldNotThrowWhenPermissionExists() {
		OrganizationMember member = new OrganizationMember();
		member.setRole(Role.ADMIN);

		when(memberRepository.findByUserIdAndOrganizationId(1L, 10L))
				.thenReturn(Optional.of(member));

		assertDoesNotThrow(() ->
				permissionService.requirePermission(
						1L,
						10L,
						Permission.INVITE_MEMBER
				)
		);
	}

	@Test
	void requirePermissionShouldThrowWhenPermissionIsMissing() {
		OrganizationMember member = new OrganizationMember();
		member.setRole(Role.CLIENT);

		when(memberRepository.findByUserIdAndOrganizationId(3L, 10L))
				.thenReturn(Optional.of(member));

		assertThrows(
				AccessDeniedException.class,
				() -> permissionService.requirePermission(
						3L,
						10L,
						Permission.MANAGE_MEMBERS
				)
		);
	}
}
