/* ************************************************************************** */
/*                                                                            */
/*                                                        :::      ::::::::   */
/*   OrganizationMember.java                            :+:      :+:    :+:   */
/*                                                    +:+ +:+         +:+     */
/*   By: yseddiki <yseddiki@student.42.fr>                +#+  +:+       +#+        */
/*                                                +#+#+#+#+#+   +#+           */
/*   Created: 2026/08/19 17:28:25 by yseddiki             #+#    #+#             */
/*   Updated: 2026/08/19 17:28:26 by yseddiki            ###   ########.fr       */
/*                                                                            */
/* ************************************************************************** */

package com.facturx.app.organization;

import com.facturx.app.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_members")
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String role; // ADMIN, COMPTABLE, CLIENT

    private LocalDateTime joinedAt = LocalDateTime.now();

    public OrganizationMember() {}

    // getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Organization getOrganization() { return organization; }
    public String getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }

    // setters
    public void setUser(User user) { this.user = user; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public void setRole(String role) { this.role = role; }
}