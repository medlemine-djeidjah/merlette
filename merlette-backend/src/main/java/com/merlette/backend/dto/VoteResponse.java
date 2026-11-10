package com.merlette.backend.dto;

public record VoteResponse(Integer myVoteProposalId, String observationStatus,
                           SpeciesSummaryDto identifiedSpecies) {}
