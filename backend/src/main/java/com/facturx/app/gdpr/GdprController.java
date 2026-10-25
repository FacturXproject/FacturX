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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/gdpr")
public class GdprController {

	private final GdprService gdprService;
	private final AuthService authService;

	public GdprController(
			GdprService gdprService,
			AuthService authService) {

		this.gdprService = gdprService;
		this.authService = authService;
	}

	@GetMapping("/export")
	public ResponseEntity<GdprExportResponse> exportUserData(
			Authentication authentication) {

		AppUserPrincipal principal =
				(AppUserPrincipal) authentication.getPrincipal();

		Long userId = principal.getUser().getId();

		return ResponseEntity.ok(
				gdprService.exportUserData(userId)
		);
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

		gdprService.deleteOrAnonymizeUser(
				userId,
				request.confirmation()
		);

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
}
