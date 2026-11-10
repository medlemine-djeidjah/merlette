package com.merlette.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email obligatoire") String email,
        @NotBlank(message = "Mot de passe obligatoire") String password) {}
