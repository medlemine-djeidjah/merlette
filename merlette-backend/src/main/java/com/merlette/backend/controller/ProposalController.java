package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.CreateProposalRequest;
import com.merlette.backend.dto.CreateProposalResponse;
import com.merlette.backend.dto.ProposalsResponse;
import com.merlette.backend.dto.VoteRequest;
import com.merlette.backend.dto.VoteResponse;
import com.merlette.backend.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Propositions d'espèces et votes. */
@RestController
@Tag(name = "Identification", description = "Propositions d'espèces, votes et consensus")
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @GetMapping("/observations/{id}/proposals")
    public ProposalsResponse list(@PathVariable Integer id) {
        return proposalService.getProposals(id);
    }

    @PostMapping("/observations/{id}/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateProposalResponse create(@PathVariable Integer id,
                                         @Valid @RequestBody CreateProposalRequest request) {
        return proposalService.create(id, request.speciesId());
    }

    @DeleteMapping("/proposals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        proposalService.delete(id);
    }

    @PutMapping("/observations/{id}/vote")
    public VoteResponse vote(@PathVariable Integer id, @Valid @RequestBody VoteRequest request) {
        return proposalService.vote(id, request.proposalId());
    }

    @DeleteMapping("/observations/{id}/vote")
    public VoteResponse removeVote(@PathVariable Integer id) {
        return proposalService.removeVote(id);
    }
}
