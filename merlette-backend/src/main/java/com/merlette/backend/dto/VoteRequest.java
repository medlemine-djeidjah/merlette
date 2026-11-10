package com.merlette.backend.dto;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull(message = "proposalId obligatoire") Integer proposalId) {}
