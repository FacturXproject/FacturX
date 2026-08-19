/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   OrganizationMemberRepository.java                  :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: yseddiki <yseddiki@student.42.fr>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/19 17:28:28 by yseddiki             #+#    #+#             */
/*   Updated: 2026/08/19 17:28:29 by yseddiki            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.facturx.app.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    List<OrganizationMember> findByOrganizationId(Long organizationId);

    List<OrganizationMember> findByUserId(Long userId);

    Optional<OrganizationMember> findByUserIdAndOrganizationId(Long userId, Long organizationId);
}