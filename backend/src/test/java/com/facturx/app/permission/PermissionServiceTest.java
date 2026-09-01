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
    void adminShouldHaveExpectedPermissions() {
        OrganizationMember member = new OrganizationMember();
        member.setRole(Role.ADMIN);

        when(memberRepository.findByUserIdAndOrganizationId(1L, 10L))
            .thenReturn(Optional.of(member));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.UPLOAD_DOCUMENT));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.VIEW_ALL_DOCUMENTS));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.VIEW_OWN_DOCUMENTS));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.VALIDATE_DOCUMENT));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.INVITE_MEMBER));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.MANAGE_MEMBERS));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.MANAGE_ORGANIZATION));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.DELETE_ANY_DOCUMENT));

        assertTrue(permissionService.hasPermission(
            1L, 10L, Permission.DELETE_OWN_PENDING_DOCUMENT));
    }

    @Test
    void accountantShouldHaveExpectedPermissions() {
        OrganizationMember member = new OrganizationMember();
        member.setRole(Role.ACCOUNTANT);

        when(memberRepository.findByUserIdAndOrganizationId(2L, 10L))
            .thenReturn(Optional.of(member));

        assertTrue(permissionService.hasPermission(
            2L, 10L, Permission.UPLOAD_DOCUMENT));

        assertTrue(permissionService.hasPermission(
            2L, 10L, Permission.VIEW_ALL_DOCUMENTS));

        assertTrue(permissionService.hasPermission(
            2L, 10L, Permission.VIEW_OWN_DOCUMENTS));

        assertTrue(permissionService.hasPermission(
            2L, 10L, Permission.VALIDATE_DOCUMENT));

        assertTrue(permissionService.hasPermission(
            2L, 10L, Permission.DELETE_OWN_PENDING_DOCUMENT));

        assertFalse(permissionService.hasPermission(
            2L, 10L, Permission.INVITE_MEMBER));

        assertFalse(permissionService.hasPermission(
            2L, 10L, Permission.MANAGE_MEMBERS));

        assertFalse(permissionService.hasPermission(
            2L, 10L, Permission.MANAGE_ORGANIZATION));

        assertFalse(permissionService.hasPermission(
            2L, 10L, Permission.DELETE_ANY_DOCUMENT));
    }

    @Test
    void clientShouldHaveExpectedPermissions() {
        OrganizationMember member = new OrganizationMember();
        member.setRole(Role.CLIENT);

        when(memberRepository.findByUserIdAndOrganizationId(3L, 10L))
            .thenReturn(Optional.of(member));

        assertTrue(permissionService.hasPermission(
            3L, 10L, Permission.UPLOAD_DOCUMENT));

        assertTrue(permissionService.hasPermission(
            3L, 10L, Permission.VIEW_OWN_DOCUMENTS));

        assertTrue(permissionService.hasPermission(
            3L, 10L, Permission.DELETE_OWN_PENDING_DOCUMENT));

        assertFalse(permissionService.hasPermission(
            3L, 10L, Permission.VIEW_ALL_DOCUMENTS));

        assertFalse(permissionService.hasPermission(
            3L, 10L, Permission.VALIDATE_DOCUMENT));

        assertFalse(permissionService.hasPermission(
            3L, 10L, Permission.INVITE_MEMBER));

        assertFalse(permissionService.hasPermission(
            3L, 10L, Permission.MANAGE_MEMBERS));

        assertFalse(permissionService.hasPermission(
            3L, 10L, Permission.MANAGE_ORGANIZATION));

        assertFalse(permissionService.hasPermission(
            3L, 10L, Permission.DELETE_ANY_DOCUMENT));
    }

    @Test
    void nonMemberShouldHaveNoPermission() {
        when(memberRepository.findByUserIdAndOrganizationId(4L, 10L))
            .thenReturn(Optional.empty());

        assertFalse(permissionService.hasPermission(
            4L,
            10L,
            Permission.UPLOAD_DOCUMENT
        ));
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
