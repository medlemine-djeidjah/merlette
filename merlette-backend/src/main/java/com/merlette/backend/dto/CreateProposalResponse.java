package com.merlette.backend.dto;

public record CreateProposalResponse(Created proposal, String observationStatus) {

    public record Created(Integer id, Integer speciesId, int votes) {}
}
