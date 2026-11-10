package com.merlette.backend.service;

import com.merlette.backend.dto.CreateProposalResponse;
import com.merlette.backend.dto.ProposalDto;
import com.merlette.backend.dto.ProposalsResponse;
import com.merlette.backend.dto.SpeciesSummaryDto;
import com.merlette.backend.dto.UserSummaryDto;
import com.merlette.backend.dto.VoteResponse;
import com.merlette.backend.entity.Observation;
import com.merlette.backend.entity.ObservationStatus;
import com.merlette.backend.entity.Proposal;
import com.merlette.backend.entity.Species;
import com.merlette.backend.entity.User;
import com.merlette.backend.entity.Vote;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.ObservationRepository;
import com.merlette.backend.repository.ProposalRepository;
import com.merlette.backend.repository.SpeciesRepository;
import com.merlette.backend.repository.UserRepository;
import com.merlette.backend.repository.VoteRepository;
import com.merlette.backend.security.CurrentUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Propositions d'espèces et votes (#17, #4, #5). */
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final VoteRepository voteRepository;
    private final ObservationRepository observationRepository;
    private final SpeciesRepository speciesRepository;
    private final UserRepository userRepository;
    private final ConsensusService consensusService;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    // --- Lecture -----------------------------------------------------------

    @Transactional(readOnly = true)
    public ProposalsResponse getProposals(Integer observationId) {
        return buildProposals(findObservation(observationId));
    }

    /** Propositions avec votes, %, votants ; triées par votes décroissants puis ancienneté. */
    public ProposalsResponse buildProposals(Observation observation) {
        List<Proposal> proposals = proposalRepository.findForObservation(observation.getId());
        Map<Integer, List<User>> votersByProposal = voteRepository.findForObservation(observation.getId()).stream()
                .collect(Collectors.groupingBy(v -> v.getProposal().getId(),
                        Collectors.mapping(Vote::getUser, Collectors.toList())));
        int totalVotes = votersByProposal.values().stream().mapToInt(List::size).sum();

        List<ProposalDto> items = new ArrayList<>();
        for (Proposal p : proposals) {
            List<User> voters = votersByProposal.getOrDefault(p.getId(), List.of());
            double percent = totalVotes == 0 ? 0 : Math.round(voters.size() * 1000.0 / totalVotes) / 10.0;
            items.add(new ProposalDto(p.getId(), SpeciesSummaryDto.from(p.getSpecies()),
                    UserSummaryDto.from(p.getAuthor()), voters.size(), percent,
                    voters.stream().map(UserSummaryDto::from).toList(), p.getCreatedAt()));
        }
        // tri stable : à égalité de votes, l'ordre d'ancienneté est conservé
        items.sort(Comparator.comparingInt(ProposalDto::votes).reversed());

        Species identified = observation.getIdentifiedSpecies();
        Integer winnerId = identified == null ? null : proposals.stream()
                .filter(p -> p.getSpecies().getId().equals(identified.getId()))
                .map(Proposal::getId).findFirst().orElse(null);
        ProposalsResponse.Consensus consensus = new ProposalsResponse.Consensus(
                observation.getStatus() == ObservationStatus.IDENTIFIED, winnerId,
                identified == null ? null : identified.getId());
        return new ProposalsResponse(totalVotes, consensus, items);
    }

    /** Proposition pour laquelle l'utilisateur a voté, ou null. */
    public Integer myVoteProposalId(Integer observationId, Integer userId) {
        if (userId == null) {
            return null;
        }
        return voteRepository.findByObservationIdAndUserId(observationId, userId)
                .map(v -> v.getProposal().getId()).orElse(null);
    }

    // --- Propositions ------------------------------------------------------

    /** Propose une espèce ; la proposition vaut vote du proposeur. */
    @Transactional
    public CreateProposalResponse create(Integer observationId, Integer speciesId) {
        User me = findMe();
        Observation observation = findObservation(observationId);
        Species species = speciesRepository.findById(speciesId)
                .orElseThrow(() -> ApiException.notFound("Espèce introuvable."));
        if (proposalRepository.existsByObservationIdAndSpeciesId(observationId, speciesId)) {
            throw ApiException.conflict("Cette espèce est déjà proposée : votez pour elle.");
        }

        Proposal proposal = new Proposal();
        proposal.setObservation(observation);
        proposal.setSpecies(species);
        proposal.setAuthor(me);
        proposalRepository.save(proposal);

        placeVote(observation, proposal, me);
        notificationService.notifyNewProposal(observation, proposal, me);
        consensusService.recompute(observation, me);

        return new CreateProposalResponse(new CreateProposalResponse.Created(proposal.getId(), speciesId, 1),
                observation.getStatus().name());
    }

    /** Supprime sa proposition, seulement si personne d'autre n'a voté pour elle. */
    @Transactional
    public void delete(Integer proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> ApiException.notFound("Proposition introuvable."));
        Integer authorId = proposal.getAuthor().getId();
        currentUser.requireOwnerOrAdmin(authorId);

        List<Vote> votes = voteRepository.findByProposalId(proposalId);
        boolean othersVoted = votes.stream().anyMatch(v -> !v.getUser().getId().equals(authorId));
        if (othersVoted) {
            throw ApiException.conflict("D'autres utilisateurs ont voté pour cette proposition.");
        }
        Observation observation = proposal.getObservation();
        voteRepository.deleteAll(votes);
        proposalRepository.delete(proposal);
        consensusService.recompute(observation, findMe());
    }

    // --- Votes -------------------------------------------------------------

    /** Vote (ou déplace son vote) ; idempotent. */
    @Transactional
    public VoteResponse vote(Integer observationId, Integer proposalId) {
        User me = findMe();
        Observation observation = findObservation(observationId);
        Proposal proposal = proposalRepository.findById(proposalId)
                .filter(p -> p.getObservation().getId().equals(observationId))
                .orElseThrow(() -> ApiException.notFound("Proposition introuvable pour cette observation."));

        placeVote(observation, proposal, me);
        consensusService.recompute(observation, me);
        return voteResponse(observation, proposalId);
    }

    @Transactional
    public VoteResponse removeVote(Integer observationId) {
        User me = findMe();
        Observation observation = findObservation(observationId);
        Vote vote = voteRepository.findByObservationIdAndUserId(observationId, me.getId())
                .orElseThrow(() -> ApiException.notFound("Aucun vote sur cette observation."));
        voteRepository.delete(vote);
        consensusService.recompute(observation, me);
        return voteResponse(observation, null);
    }

    /** Crée le vote de l'utilisateur, ou le déplace s'il en avait déjà un (1 vote par observation). */
    private void placeVote(Observation observation, Proposal proposal, User user) {
        Vote vote = voteRepository.findByObservationIdAndUserId(observation.getId(), user.getId())
                .orElseGet(() -> {
                    Vote v = new Vote();
                    v.setObservationId(observation.getId());
                    v.setUser(user);
                    return v;
                });
        vote.setProposal(proposal);
        voteRepository.save(vote);
    }

    private VoteResponse voteResponse(Observation observation, Integer myProposalId) {
        return new VoteResponse(myProposalId, observation.getStatus().name(),
                SpeciesSummaryDto.from(observation.getIdentifiedSpecies()));
    }

    private Observation findObservation(Integer id) {
        return observationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Observation introuvable."));
    }

    private User findMe() {
        return userRepository.findById(currentUser.id()).orElseThrow(ApiException::unauthorized);
    }
}
