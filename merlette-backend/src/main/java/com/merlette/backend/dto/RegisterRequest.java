package com.merlette.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull(message = "Nom d'utilisateur obligatoire")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$",
                message = "3 à 30 caractères : lettres, chiffres ou _")
        String username,

        @NotBlank(message = "Email obligatoire")
        @Email(message = "Email invalide")
        @Size(max = 255)
        String email,

        @NotNull(message = "Mot de passe obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password) {}
