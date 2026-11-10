package com.merlette.backend.service;

import com.merlette.backend.entity.Observation;
import com.merlette.backend.entity.ObservationStatus;
import com.merlette.backend.entity.Proposal;
import com.merlette.backend.entity.Species;
import com.merlette.backend.entity.User;
import com.merlette.backend.repository.ProposalRepository;
import com.merlette.backend.repository.VoteRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Règle de consensus (PRD §6.1), recalculée après chaque création, déplacement ou retrait de vote.
 * La meilleure proposition est retenue si elle a au moins 3 votes ET au moins 2/3 des votes.
 */
@Service
@RequiredArgsConstructor
public class ConsensusService {

    private final ProposalRepository proposalRepository;
    private final VoteRepository voteRepository;
    private final NotificationService notificationService;

    /** Met à jour le statut de l'observation ; notifie si elle devient identifiée. */
    public void recompute(Observation observation, User actor) {
        List<Proposal> proposals = proposalRepository.findForObservation(observation.getId()); // plus ancienne d'abord
        Map<Integer, Long> votesByProposal = voteRepository.findForObservation(observation.getId()).stream()
                .collect(Collectors.groupingBy(v -> v.getProposal().getId(), Collectors.counting()));
        List<Integer> votes = proposals.stream()
                .map(p -> votesByProposal.getOrDefault(p.getId(), 0L).intValue())
                .toList();

        int winnerIndex = findWinner(votes);
        Species before = observation.getIdentifiedSpecies();

        if (winnerIndex < 0) {
            observation.setStatus(ObservationStatus.NEEDS_ID);
            observation.setIdentifiedSpecies(null);
            return;
        }

        Proposal winner = proposals.get(winnerIndex);
        observation.setStatus(ObservationStatus.IDENTIFIED);
        observation.setIdentifiedSpecies(winner.getSpecies());
        boolean newlyIdentified = before == null || !before.getId().equals(winner.getSpecies().getId());
        if (newlyIdentified) {
            notificationService.notifyIdentified(observation, winner, actor);
        }
    }

    /**
     * Fonction pure de la règle §6.1.
     *
     * @param votes nombre de votes de chaque proposition, de la plus ancienne à la plus récente
     * @return l'index de la proposition retenue, ou -1 s'il n'y a pas de consensus
     */
    public static int findWinner(List<Integer> votes) {
        int total = 0;
        int best = -1;
        for (int i = 0; i < votes.size(); i++) {
            total += votes.get(i);
            // en cas d'égalité, la plus ancienne (index le plus petit) reste en tête
            if (best == -1 || votes.get(i) > votes.get(best)) {
                best = i;
            }
        }
        if (best == -1) {
            return -1;
        }
        int bestVotes = votes.get(best);
        boolean reached = bestVotes >= 3 && bestVotes * 3 >= total * 2; // bestVotes / total >= 2/3
        return reached ? best : -1;
    }
}
