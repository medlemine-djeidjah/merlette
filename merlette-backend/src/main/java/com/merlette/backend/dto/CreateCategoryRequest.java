package com.merlette.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Nom obligatoire") @Size(max = 50) String name,
        @NotBlank(message = "Slug obligatoire") @Size(max = 50)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Lettres minuscules, chiffres et tirets uniquement")
        String slug) {}
