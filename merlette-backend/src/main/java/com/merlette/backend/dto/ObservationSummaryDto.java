package com.merlette.backend.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Carte du fil des observations. */
public record ObservationSummaryDto(
        Integer id,
        String title,
        String status,
        CategoryDto category,
        UserSummaryDto author,
        String coverUrl,
        LocalDate observedAt,
        Instant createdAt,
        long proposalCount,
        long commentCount,
        SpeciesSummaryDto identifiedSpecies) {}
