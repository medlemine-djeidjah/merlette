package com.merlette.backend.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Page détail d'une observation. */
public record ObservationDetailDto(
        Integer id,
        String title,
        String description,
        String status,
        SpeciesSummaryDto identifiedSpecies,
        CategoryDto category,
        UserSummaryDto author,
        LocalDate observedAt,
        String locationName,
        Double latitude,
        Double longitude,
        List<PhotoDto> photos,
        List<ProposalDto> proposals,
        Integer myVoteProposalId,
        long commentCount,
        Instant createdAt,
        Instant updatedAt) {}
