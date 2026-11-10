package com.merlette.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Le commentaire ne peut pas être vide")
        @Size(max = 1000, message = "1000 caractères maximum")
        String content) {}
