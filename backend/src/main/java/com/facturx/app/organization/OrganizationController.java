/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   OrganizationController.java                        :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: yseddiki <yseddiki@student.42.fr>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/25 by yseddiki                    #+#    #+#             */
/*   Updated: 2026/08/25 by yseddiki                   ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.facturx.app.organization;

import com.facturx.app.auth.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    } 

    // Recupere l'id de l'utilisateur connecte depuis la session
    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    // POST /api/organizations?name=Cabinet
    @PostMapping
    public OrganizationResponse create(@RequestParam String name, Authentication authentication) {
        return OrganizationResponse.from(
            organizationService.createOrganization(name, currentUserId(authentication))
        );
    }

    // GET /api/organizations - mes organisations
    @GetMapping
    public List<MemberResponse> getMyOrganizations(Authentication authentication) {
        return organizationService.getUserOrganizations(currentUserId(authentication))
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    // GET /api/organizations/{id}/members
    @GetMapping("/{id}/members")
    public List<MemberResponse> getMembers(@PathVariable Long id) {
        return organizationService.getMembers(id)
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    // POST /api/organizations/{id}/members?userId=2&role=ACCOUNTANT
    @PostMapping("/{id}/members")
    public MemberResponse addMember(@PathVariable Long id,
                                     @RequestParam Long userId,
                                     @RequestParam Role role) {
        return MemberResponse.from(organizationService.addMember(id, userId, role));
    }

    // DELETE /api/organizations/{id}/members/{userId}
    @DeleteMapping("/{id}/members/{userId}")
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        organizationService.removeMember(id, userId);
    }

    // PUT /api/organizations/{id}?name=NouveauNom
    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable Long id, @RequestParam String name) {
        return OrganizationResponse.from(organizationService.updateOrganization(id, name));
    }

    // DELETE /api/organizations/{id}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
    }
}
