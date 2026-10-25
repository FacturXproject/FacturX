package com.facturx.app.organization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;

@Service
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PermissionService permissionService;
    private final EmailService emailService;

    public InvitationService(
            InvitationRepository invitationRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PermissionService permissionService,
            EmailService emailService) {

        this.invitationRepository = invitationRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.permissionService = permissionService;
        this.emailService = emailService;
    }


    public InvitationResponse create(
            Long orgId,
            InvitationRequest request,
            Long currentUserId) {

        Organization organization = organizationRepository.findById(orgId)
            .orElseThrow(OrganizationNotFoundException::new);

        permissionService.requirePermission(
            currentUserId,
            orgId,
            Permission.INVITE_MEMBER
        );

        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {

            if (organizationMemberRepository
                    .findByUserIdAndOrganizationId(
                        existingUser.getId(),
                        orgId
                    )
                    .isPresent()) {

                throw new UserAlreadyMemberException();
            }
        });

        if (invitationRepository
                .findByEmailAndOrganizationIdAndStatus(
                    request.email(),
                    orgId,
                    InvitationStatus.PENDING
                )
                .isPresent()) {

            throw new InvitationAlreadyPendingException();
        }

        Invitation invitation = new Invitation();

        invitation.setOrganization(organization);
        invitation.setEmail(request.email());
        invitation.setRole(request.role());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(
            LocalDateTime.now().plusDays(7)
        );

        invitationRepository.save(invitation);

        sendInvitationEmail(invitation, organization);

        return toResponse(invitation);
    }


    // F20 : dans une transaction (import CSV), l'email part seulement après le commit,
    // pour qu'un import annulé n'envoie jamais de lien vers une invitation inexistante.
    private void sendInvitationEmail(
            Invitation invitation,
            Organization organization) {

        String email = invitation.getEmail();
        String organizationName = organization.getName();
        String token = invitation.getToken();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailService.sendInvitation(email, organizationName, token);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        emailService.sendInvitation(email, organizationName, token);
                    } catch (RuntimeException e) {
                        // L'invitation est enregistrée : l'admin peut la renvoyer.
                        log.warn("Invitation email to {} failed", email, e);
                    }
                }
            }
        );
    }


    // PUBLIC : consultation avant authentification
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
    public InvitationResponse accept(
            String token,
            Long currentUserId) {

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

        if (!user.getEmail()
                .equalsIgnoreCase(invitation.getEmail())) {

            throw new InvitationNotForCurrentUserException();
        }

        Organization organization =
            invitation.getOrganization();

        if (organizationMemberRepository
                .findByUserIdAndOrganizationId(
                    user.getId(),
                    organization.getId()
                )
                .isPresent()) {

            throw new UserAlreadyMemberException();
        }

        OrganizationMember member =
            new OrganizationMember();

        member.setUser(user);
        member.setOrganization(organization);
        member.setRole(invitation.getRole());

        organizationMemberRepository.save(member);

        invitation.setStatus(
            InvitationStatus.ACCEPTED
        );

        invitationRepository.save(invitation);

        return toResponse(invitation);
    }


    public InvitationResponse revoke(
            Long invitationId,
            Long currentUserId) {

        Invitation invitation =
            invitationRepository.findById(invitationId)
                .orElseThrow(
                    InvitationNotFoundException::new
                );

        if (invitation.getStatus()
                != InvitationStatus.PENDING) {

            throw new InvitationNotPendingException();
        }

        if (invitation.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            invitation.setStatus(
                InvitationStatus.EXPIRED
            );

            invitationRepository.save(invitation);

            throw new InvitationExpiredException();
        }

        Organization organization =
            invitation.getOrganization();

        permissionService.requirePermission(
            currentUserId,
            organization.getId(),
            Permission.INVITE_MEMBER
        );

        invitation.setStatus(
            InvitationStatus.REVOKED
        );

        invitationRepository.save(invitation);

        return toResponse(invitation);
    }


    public InvitationResponse resend(
            Long invitationId,
            Long currentUserId) {

        Invitation invitation =
            invitationRepository.findById(invitationId)
                .orElseThrow(
                    InvitationNotFoundException::new
                );

        Organization organization =
            invitation.getOrganization();

        permissionService.requirePermission(
            currentUserId,
            organization.getId(),
            Permission.INVITE_MEMBER
        );

        if (invitation.getStatus()
                == InvitationStatus.ACCEPTED) {

            throw new InvitationNotPendingException();
        }

        invitation.setStatus(
            InvitationStatus.PENDING
        );

        invitation.setToken(
            UUID.randomUUID().toString()
        );

        invitation.setExpiresAt(
            LocalDateTime.now().plusDays(7)
        );

        invitationRepository.save(invitation);

        emailService.sendInvitation(
            invitation.getEmail(),
            organization.getName(),
            invitation.getToken()
        );

        return toResponse(invitation);
    }


    public List<InvitationResponse> getByOrganization(
            Long orgId,
            Long currentUserId) {

        permissionService.requirePermission(
            currentUserId,
            orgId,
            Permission.INVITE_MEMBER
        );

        return invitationRepository
            .findByOrganizationId(orgId)
            .stream()
            .map(this::toResponse)
            .toList();
    }


    private InvitationResponse toResponse(
            Invitation invitation) {

        return new InvitationResponse(
            invitation.getId(),
            invitation.getEmail(),
            invitation.getRole(),
            invitation.getStatus(),
            invitation.getCreatedAt(),
            invitation.getExpiresAt()
        );
    }

}