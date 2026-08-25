package com.facturx.app.auth;

import com.facturx.app.user.User;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
