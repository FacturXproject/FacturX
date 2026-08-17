package com.facturx.app.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "Adresse email invalide.")
        @Size(max = 255, message = "Adresse email invalide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 10, max = 72, message = "Le mot de passe doit contenir au moins 10 caractères.")
        String password,

        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 50, message = "Le prénom ne doit pas dépasser 50 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères.")
        String lastName
) {
}
