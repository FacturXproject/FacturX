
package com.facturx.app.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    List<OrganizationMember> findByOrganizationId(Long organizationId);

    List<OrganizationMember> findByUserId(Long userId);

    Optional<OrganizationMember> findByUserIdAndOrganizationId(Long userId, Long organizationId);

    long countByOrganizationIdAndRole(Long organizationId, Role role);
}
