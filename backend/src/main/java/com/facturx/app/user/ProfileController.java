package com.facturx.app.user;

import com.facturx.app.auth.AppUserPrincipal;
import com.facturx.app.auth.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.facturx.app.user.UserRepository;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final UserService userService;
	private final UserRepository userRepository;

	public ProfileController(
			UserService userService,
			UserRepository userRepository) {
		this.userService = userService;
		this.userRepository = userRepository;
	}

    @GetMapping("/me")
	public ResponseEntity<UserResponse> getMe(
			Authentication authentication) {

		AppUserPrincipal principal =
				(AppUserPrincipal) authentication.getPrincipal();

		User user = userRepository.findById(
				principal.getUser().getId()
		).orElseThrow();

		return ResponseEntity.ok(
				UserResponse.from(user)
		);
	}

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {

        AppUserPrincipal principal =
                (AppUserPrincipal) authentication.getPrincipal();

        User updatedUser = userService.updateProfile(
                principal.getUser(),
                request.firstName(),
                request.lastName()
        );

        return ResponseEntity.ok(
                UserResponse.from(updatedUser)
        );
    }
}