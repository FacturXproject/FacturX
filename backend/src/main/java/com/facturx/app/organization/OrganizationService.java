/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   OrganizationService.java                           :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: yseddiki <yseddiki@student.42.fr>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/25 by yseddiki                    #+#    #+#             */
/*   Updated: 2026/08/25 by yseddiki                   ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.facturx.app.organization;

import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final UserRepository userRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
                                OrganizationMemberRepository memberRepository,
                                UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
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

    // Lister les membres d'une organisation
    public List<OrganizationMember> getMembers(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException();
        }
        return memberRepository.findByOrganizationId(organizationId);
    }

    // Lister les organisations d'un utilisateur
    public List<OrganizationMember> getUserOrganizations(Long userId) {
        return memberRepository.findByUserId(userId);
    }

    // Ajouter un membre a une organisation
    public OrganizationMember addMember(Long organizationId, Long userId, Role role) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);

        User user = userRepository.findById(userId)
            .orElseThrow(MemberNotFoundException::new);

        // Verifier que le user n'est pas deja membre
        memberRepository.findByUserIdAndOrganizationId(userId, organizationId)
            .ifPresent(existing -> { throw new UserAlreadyMemberException(); });

        OrganizationMember member = new OrganizationMember();
        member.setUser(user);
        member.setOrganization(organization);
        member.setRole(role);
        return memberRepository.save(member);
    }

    // Retirer un membre d'une organisation
    public void removeMember(Long organizationId, Long userId) {
        OrganizationMember member = memberRepository
            .findByUserIdAndOrganizationId(userId, organizationId)
            .orElseThrow(MemberNotFoundException::new);
        memberRepository.delete(member);
    }

    // Renommer une organisation
    public Organization updateOrganization(Long organizationId, String newName) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);
        organization.setName(newName);
        return organizationRepository.save(organization);
    }

    // Supprimer une organisation et tous ses membres
    @Transactional
    public void deleteOrganization(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(OrganizationNotFoundException::new);

        // Supprimer d'abord les membres (cle etrangere vers l'organisation)
        memberRepository.deleteAll(memberRepository.findByOrganizationId(organizationId));

        organizationRepository.delete(organization);
    }
}