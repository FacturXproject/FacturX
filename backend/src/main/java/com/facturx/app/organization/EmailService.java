package com.facturx.app.organization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final String appBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String mailFrom,
            @Value("${app.base-url}") String appBaseUrl) {

        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.appBaseUrl = appBaseUrl;
    }

    public void sendInvitation(
            String email,
            String organizationName,
            String token) {

        String invitationLink =
            appBaseUrl + "/invitations/" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(
            "Invitation to join " + organizationName
        );

        message.setText(
            "You have been invited to join "
            + organizationName
            + ".\n\n"
            + "Click the following link to accept the invitation:\n"
            + invitationLink
        );

        mailSender.send(message);
    }
}
