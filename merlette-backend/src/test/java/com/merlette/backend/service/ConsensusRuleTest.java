package com.merlette.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Règle de consensus du PRD §6.1 : ≥ 3 votes ET ≥ 2/3 des votes. */
class ConsensusRuleTest {

    @Test
    void noProposals_noConsensus() {
        assertEquals(-1, ConsensusService.findWinner(List.of()));
    }

    @Test
    void lessThanThreeVotes_noConsensus() {
        assertEquals(-1, ConsensusService.findWinner(List.of(2)));
    }

    @Test
    void threeOutOfThree_identified() {
        assertEquals(0, ConsensusService.findWinner(List.of(3)));
    }

    @Test
    void threeOutOfFour_identified() {
        assertEquals(0, ConsensusService.findWinner(List.of(3, 1)));
    }

    @Test
    void twoOutOfFour_noConsensus() {
        assertEquals(-1, ConsensusService.findWinner(List.of(2, 2)));
    }

    @Test
    void exactlyTwoThirds_identified() {
        assertEquals(1, ConsensusService.findWinner(List.of(2, 4)));
    }

    @Test
    void belowTwoThirds_noConsensus() {
        assertEquals(-1, ConsensusService.findWinner(List.of(2, 3)));
    }

    @Test
    void tie_oldestWins_butNoConsensus() {
        assertEquals(-1, ConsensusService.findWinner(List.of(3, 3)));
    }
}
