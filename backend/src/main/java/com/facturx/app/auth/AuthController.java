package com.facturx.app.auth;

import com.facturx.app.user.User;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/// this is the entry point of the external world from the backend we can condier it as a listner 
/// once recived to redirect the logic to @service


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService; // init the class AuthService once recive an api on endpoint /api/auth

    public AuthController(AuthService authService) { // init the AuthController with the object AuthService
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        return authService.register(user);

    }
}