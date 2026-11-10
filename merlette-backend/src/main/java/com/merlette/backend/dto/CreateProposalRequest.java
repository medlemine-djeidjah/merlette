package com.merlette.backend.dto;

import jakarta.validation.constraints.NotNull;

public record CreateProposalRequest(@NotNull(message = "speciesId obligatoire") Integer speciesId) {}
