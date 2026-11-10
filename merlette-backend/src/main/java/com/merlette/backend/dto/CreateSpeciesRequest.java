package com.merlette.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSpeciesRequest(
        @NotBlank(message = "Nom commun obligatoire") @Size(max = 150) String commonName,
        @NotBlank(message = "Nom scientifique obligatoire") @Size(max = 150) String scientificName,
        @NotNull(message = "Catégorie obligatoire") Integer categoryId) {}
