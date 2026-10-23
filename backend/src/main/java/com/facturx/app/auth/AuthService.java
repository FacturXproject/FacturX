package com.facturx.app.auth;

import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import com.facturx.app.websocket.DocumentWebSocketHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
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
    private final DocumentWebSocketHandler webSocketHandler;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    private final SessionAuthenticationStrategy sessionAuthenticationStrategy =
            new ChangeSessionIdAuthenticationStrategy();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            LoginAttemptService loginAttemptService,
            DocumentWebSocketHandler webSocketHandler) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.loginAttemptService = loginAttemptService;
        this.webSocketHandler = webSocketHandler;
    }

    public UserResponse register(
            RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

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

        Authentication authentication = authenticate(
                normalizedEmail,
                request.password(),
                httpRequest,
                httpResponse
        );

        return UserResponse.from(
                ((AppUserPrincipal) authentication.getPrincipal()).getUser()
        );
    }

    public UserResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String normalizedEmail = normalize(request.email());

        if (loginAttemptService.isBlocked(normalizedEmail)) {
            throw new TooManyAttemptsException();
        }

        try {
            Authentication authentication = authenticate(
                    normalizedEmail,
                    request.password(),
                    httpRequest,
                    httpResponse
            );

            loginAttemptService.recordSuccess(normalizedEmail);

            return UserResponse.from(
                    ((AppUserPrincipal) authentication.getPrincipal()).getUser()
            );
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(normalizedEmail);
            throw ex;
        }
    }

    private Authentication authenticate(
            String normalizedEmail,
            String rawPassword,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        Authentication authenticationRequest =
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        rawPassword
                );

        Authentication authentication =
                authenticationManager.authenticate(authenticationRequest);

        // Regenerate the session ID before saving the security context.
        sessionAuthenticationStrategy.onAuthentication(
                authentication,
                httpRequest,
                httpResponse
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return authentication;
    }

    public void logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        var session = httpRequest.getSession(false);
        String sessionId = session != null ? session.getId() : null;

        try {
            webSocketHandler.closeForHttpSession(sessionId);
        } finally {
            new SecurityContextLogoutHandler().logout(
                    httpRequest,
                    httpResponse,
                    authentication
            );
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}