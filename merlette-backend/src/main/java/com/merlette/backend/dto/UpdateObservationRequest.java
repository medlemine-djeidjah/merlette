package com.merlette.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Modification partielle : un champ absent (null) n'est pas modifié. */
public record UpdateObservationRequest(
        Integer categoryId,
        @PastOrPresent(message = "La date ne peut pas être dans le futur") LocalDate observedAt,
        @Size(max = 120, message = "120 caractères maximum") String title,
        String description,
        @Size(max = 200, message = "200 caractères maximum") String locationName,
        @DecimalMin(value = "-90", message = "Latitude invalide")
        @DecimalMax(value = "90", message = "Latitude invalide")
        Double latitude,
        @DecimalMin(value = "-180", message = "Longitude invalide")
        @DecimalMax(value = "180", message = "Longitude invalide")
        Double longitude) {}
