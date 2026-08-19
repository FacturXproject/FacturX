/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   OrganizationRepository.java                        :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: yseddiki <yseddiki@student.42.fr>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/19 17:28:31 by yseddiki             #+#    #+#             */
/*   Updated: 2026/08/19 17:28:32 by yseddiki            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.facturx.app.organization;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}