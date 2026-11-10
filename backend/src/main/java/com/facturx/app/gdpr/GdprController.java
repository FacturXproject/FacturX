package com.facturx.app.gdpr;

import com.facturx.app.auth.AppUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.facturx.app.auth.AuthService;
import com.facturx.app.organization.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/gdpr")
public class GdprController {

	private static final Logger log = LoggerFactory.getLogger(GdprController.class);

	private final GdprService gdprService;
	private final AuthService authService;
	private final EmailService emailService;

	public GdprController(
			GdprService gdprService,
			AuthService authService,
			EmailService emailService) {

		this.gdprService = gdprService;
		this.authService = authService;
		this.emailService = emailService;
	}

	@GetMapping("/export")
	public ResponseEntity<GdprExportResponse> exportUserData(
			Authentication authentication) {

		AppUserPrincipal principal =
				(AppUserPrincipal) authentication.getPrincipal();

		Long userId = principal.getUser().getId();

		GdprExportResponse export = gdprService.exportUserData(userId);

		sendConfirmation(() ->
				emailService.sendGdprExportConfirmation(export.user().email()));

		return ResponseEntity.ok(export);
	}

	@PostMapping("/delete")
	public ResponseEntity<Void> deleteUserData(
			@RequestBody GdprDeleteRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {

		AppUserPrincipal principal =
				(AppUserPrincipal) authentication.getPrincipal();

		Long userId = principal.getUser().getId();

		String principalName = principal.getUsername();

		var session = httpRequest.getSession(false);
		String currentSessionId =
				session != null ? session.getId() : null;

		String email = gdprService.deleteOrAnonymizeUser(
				userId,
				request.confirmation()
		);

		sendConfirmation(() ->
				emailService.sendAccountDeletionConfirmation(email));

		authService.logoutOtherSessions(
			principalName,
			currentSessionId
		);

		authService.logout(
				httpRequest,
				httpResponse
		);

		return ResponseEntity.noContent().build();
	}

	// Un echec d'envoi ne doit pas annuler une operation deja effectuee
	private void sendConfirmation(Runnable send) {
		try {
			send.run();
		} catch (RuntimeException e) {
			log.warn("GDPR confirmation email could not be sent", e);
		}
	}
}
