package com.facturx.app.organization;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;

    public InvitationService(InvitationRepository invitationRepository,
                              OrganizationRepository organizationRepository) {
        this.invitationRepository = invitationRepository;
        this.organizationRepository = organizationRepository;
    }
}
