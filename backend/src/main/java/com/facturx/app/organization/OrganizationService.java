package com.facturx.app.organization;

import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository memberRepository,
            UserRepository userRepository,
            PermissionService permissionService) {

        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
    }

    // Creer une organisation et ajouter son createur comme ADMIN
    @Transactional
    public Organization createOrganization(String name, Long creatorUserId) {
        User creator = userRepository.findById(creatorUserId)
            .orElseThrow(MemberNotFoundException::new);

        Organization organization = new Organization();
        organization.setName(name);
        organizationRepository.save(organization);

        OrganizationMember member = new OrganizationMember();
        member.setUser(creator);
        member.setOrganization(organization);
        member.setRole(Role.ADMIN);
        memberRepository.save(member);

        return organization;
    }

    // get members
    public List<OrganizationMember> getMembers(
            Long organizationId,
            Long currentUserId) {

        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException();
        }

        memberRepository
            .findByUserIdAndOrganizationId(currentUserId, organizationId)
            .orElseThrow(MemberNotFoundException::new);

        return memberRepository.findByOrganizationId(organizationId);
    }

    //get organization
    public Organization getOrganization(
            Long organizationId,
            Long currentUserId) {

        Organization organization = organizationRepository
            .findById(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);

        memberRepository
            .findByUserIdAndOrganizationId(currentUserId, organizationId)
            .orElseThrow(MemberNotFoundException::new);

        return organization;
    }


    // Lister les organisations d'un utilisateur
    public List<OrganizationMember> getUserOrganizations(Long userId) {
        return memberRepository.findByUserId(userId);
    }

    //MANAGE_ORGANIZATION
    public Organization updateOrganization(
            Long organizationId,
            String newName,
            Long currentUserId) {

        permissionService.requirePermission(
            currentUserId,
            organizationId,
            Permission.MANAGE_ORGANIZATION
        );

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);

        organization.setName(newName);

        return organizationRepository.save(organization);
    }

    // Retirer un membre d'une organisation
    public void removeMember(
            Long organizationId,
            Long userId,
            Long currentUserId) {

        permissionService.requirePermission(
            currentUserId,
            organizationId,
            Permission.MANAGE_MEMBERS
        );

        OrganizationMember member = memberRepository
            .findByUserIdAndOrganizationId(userId, organizationId)
            .orElseThrow(MemberNotFoundException::new);

        memberRepository.delete(member);
    }

    //update memberrole
    public OrganizationMember updateMemberRole(
        Long organizationId,
        Long userId,
        Role role,
        Long currentUserId) {

    permissionService.requirePermission(
        currentUserId,
        organizationId,
        Permission.MANAGE_MEMBERS
    );

    OrganizationMember member = memberRepository
        .findByUserIdAndOrganizationId(userId, organizationId)
        .orElseThrow(MemberNotFoundException::new);

    member.setRole(role);

    return memberRepository.save(member);
    }

    // Supprimer une organisation et tous ses membres
    @Transactional
    public void deleteOrganization(
            Long organizationId,
            Long currentUserId) {

        permissionService.requirePermission(
            currentUserId,
            organizationId,
            Permission.MANAGE_ORGANIZATION
        );

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);

        memberRepository.deleteAll(
            memberRepository.findByOrganizationId(organizationId)
        );

        organizationRepository.delete(organization);
    }
}
