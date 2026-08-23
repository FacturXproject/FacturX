package com.facturx.app.organization;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;

    public InvitationService(InvitationRepository invitationRepository,
                              OrganizationRepository organizationRepository) {
        this.invitationRepository = invitationRepository;
        this.organizationRepository = organizationRepository;
    }

    public InvitationResponse create(Long orgId, InvitationRequest request) {
        Organization organization = organizationRepository.findById(orgId)
            .orElseThrow(() -> new RuntimeException("Organization not found"));

        Invitation invitation = new Invitation();
        invitation.setOrganization(organization);
        invitation.setEmail(request.email());
        invitation.setRole(request.role());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        invitationRepository.save(invitation);

        return toResponse(invitation);
    }

    public List<InvitationResponse> getByOrganization(Long orgId) {
        return invitationRepository.findByOrganizationId(orgId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
            invitation.getId(),
            invitation.getEmail(),
            invitation.getRole(),
            invitation.getStatus(),
            invitation.getExpiresAt()
        );
    }
}