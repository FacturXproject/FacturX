package com.facturx.app.organization;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInvitation(
            String email,
            String organizationName,
            String token) {

        String invitationLink =
            "https://localhost:8443/invitations/" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Invitation à rejoindre " + organizationName);

        message.setText(
            "Vous avez été invité à rejoindre l'organisation "
            + organizationName
            + ".\n\n"
            + "Cliquez sur ce lien pour accepter l'invitation :\n"
            + invitationLink
        );

        mailSender.send(message);
    }
}