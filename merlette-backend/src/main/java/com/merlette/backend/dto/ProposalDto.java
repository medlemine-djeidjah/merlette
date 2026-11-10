package com.merlette.backend.dto;

import java.time.Instant;
import java.util.List;

/** Une proposition avec ses votes. */
public record ProposalDto(
        Integer id,
        SpeciesSummaryDto species,
        UserSummaryDto proposedBy,
        int votes,
        double percent,
        List<UserSummaryDto> voters,
        Instant createdAt) {}
