package com.facturx.app.auth;

import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    // Manual (controller-driven) login needs to run the same session-fixation
    // protection and context persistence that Spring Security's own filter
    // chain would run for a form login.
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy = new ChangeSessionIdAuthenticationStrategy();

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
    }

    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = normalize(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setStatus(User.STATUS_ACTIVE);
        userRepository.save(user);

        Authentication authentication = authenticate(normalizedEmail, request.password(), httpRequest, httpResponse);
        return UserResponse.from(((AppUserPrincipal) authentication.getPrincipal()).getUser());
    }

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = normalize(request.email());
        if (loginAttemptService.isBlocked(normalizedEmail)) {
            throw new TooManyAttemptsException();
        }

        try {
            Authentication authentication = authenticate(normalizedEmail, request.password(), httpRequest, httpResponse);
            loginAttemptService.recordSuccess(normalizedEmail);
            return UserResponse.from(((AppUserPrincipal) authentication.getPrincipal()).getUser());
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(normalizedEmail);
            throw ex;
        }
    }

    private Authentication authenticate(String normalizedEmail, String rawPassword,
                                         HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authenticationRequest = new UsernamePasswordAuthenticationToken(normalizedEmail, rawPassword);
        Authentication authentication = authenticationManager.authenticate(authenticationRequest);

        // Regenerate the session id (session fixation protection) before storing the context.
        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return authentication;
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler()
                .logout(httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
