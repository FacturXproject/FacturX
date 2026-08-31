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

    public InvitationResponse create(Long orgId, InvitationRequest request, Long currentUserId) {
        Organization organization = organizationRepository.findById(orgId)
            .orElseThrow(OrganizationNotFoundException::new);

        OrganizationMember requester = organizationMemberRepository
            .findByUserIdAndOrganizationId(currentUserId, orgId)
            .orElseThrow(MemberNotFoundException::new);

        if (requester.getRole() != Role.ADMIN) {
            throw new InsufficientPermissionException();
        }

        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {
            if (organizationMemberRepository
                    .findByUserIdAndOrganizationId(existingUser.getId(), orgId)
                    .isPresent()) {
                throw new UserAlreadyMemberException();
            }
        });

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

    // PUBLIC : consultation de l'invitation avant authentification
    // (utilisé par le frontend pour afficher l'email/rôle et rediriger vers login/register)
    public InvitationResponse check(String token) {
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

        return toResponse(invitation);
    }

    // AUTHENTIFIÉ : accepte réellement l'invitation
    public InvitationResponse accept(String token, Long currentUserId) {
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

        User user = userRepository.findById(currentUserId)
            .orElseThrow(MemberNotFoundException::new);

        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new InsufficientPermissionException();
        }

        Organization organization = invitation.getOrganization();

        if (organizationMemberRepository
                .findByUserIdAndOrganizationId(user.getId(), organization.getId())
                .isPresent()) {
            throw new UserAlreadyMemberException();
        }

        OrganizationMember member = new OrganizationMember();
        member.setUser(user);
        member.setOrganization(organization);
        member.setRole(invitation.getRole());
        organizationMemberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return toResponse(invitation);
    }

    public List<InvitationResponse> getByOrganization(Long orgId, Long currentUserId) {
        organizationMemberRepository
            .findByUserIdAndOrganizationId(currentUserId, orgId)
            .orElseThrow(MemberNotFoundException::new);

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