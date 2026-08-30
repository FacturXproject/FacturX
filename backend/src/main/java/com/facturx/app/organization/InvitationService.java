package com.facturx.app.organization;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public InvitationService(InvitationRepository invitationRepository,
                              OrganizationRepository organizationRepository,
                              UserRepository userRepository,
                              OrganizationMemberRepository organizationMemberRepository) {
        this.invitationRepository = invitationRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    public InvitationResponse create(Long orgId, InvitationRequest request) {
        Organization organization = organizationRepository.findById(orgId)
            .orElseThrow(OrganizationNotFoundException::new);
        
        if (invitationRepository.findByEmailAndOrganizationIdAndStatus(
            request.email(), orgId, InvitationStatus.PENDING).isPresent()) {
        throw new InvitationAlreadyPendingException();
        }
        
        Invitation invitation = new Invitation();
        invitation.setOrganization(organization);
        invitation.setEmail(request.email());
        invitation.setRole(request.role());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        invitationRepository.save(invitation);

        return toResponse(invitation);
    }

    public InvitationResponse accept(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(InvitationNotFoundException::new);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException();
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new InvitationExpiredException();
        }

        Organization organization = invitation.getOrganization();

        User user = userRepository.findByEmail(invitation.getEmail())
            .orElseThrow(() -> new NoAccountFoundException(token));
            //FRONTED : redirection to register page /register?invitationToken=xxx

        if (organizationMemberRepository.findByUserIdAndOrganizationId(user.getId(), organization.getId()).isPresent())
            throw new UserAlreadyMemberException();

        OrganizationMember organizationMember = new OrganizationMember();
        organizationMember.setUser(user);
        organizationMember.setOrganization(organization);
        organizationMember.setRole(invitation.getRole());

        organizationMemberRepository.save(organizationMember);
        invitation.setStatus(InvitationStatus.ACCEPTED);
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