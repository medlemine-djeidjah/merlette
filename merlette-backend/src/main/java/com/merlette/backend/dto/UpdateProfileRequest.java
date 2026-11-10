package com.merlette.backend.dto;

import jakarta.validation.constraints.Size;

/** Seuls la bio et l'avatar sont modifiables ; un champ absent n'est pas modifié. */
public record UpdateProfileRequest(
        @Size(max = 500, message = "500 caractères maximum") String bio,
        @Size(max = 500, message = "500 caractères maximum") String avatarUrl) {}
