package com.facturx.app.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvitationRequest(
    @NotBlank(message = "L'adresse email est obligatoire.")
    @Email(message = "Adresse email invalide.")
    String email,

    @NotNull(message = "Le rôle est obligatoire.")
    Role role
) {}