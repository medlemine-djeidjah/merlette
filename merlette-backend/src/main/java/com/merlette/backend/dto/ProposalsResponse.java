package com.merlette.backend.dto;

import java.util.List;

/** Réponse de GET /observations/:id/proposals. */
public record ProposalsResponse(int totalVotes, Consensus consensus, List<ProposalDto> proposals) {

    public record Consensus(boolean reached, Integer proposalId, Integer speciesId) {}
}
