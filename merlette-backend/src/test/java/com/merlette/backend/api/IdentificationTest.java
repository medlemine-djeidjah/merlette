package com.merlette.backend.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.merlette.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** #17 Proposer une espèce, #4 Voter, #5 Consulter les votes — règle de consensus §6.1. */
class IdentificationTest extends IntegrationTest {

    private TestUser lea;
    private TestUser karim;
    private TestUser sam;
    private TestUser tom;
    private int observation;
    private int bleue;
    private int charbonniere;

    @BeforeEach
    void setUp() throws Exception {
        lea = register("lea");
        karim = register("karim");
        sam = register("sam");
        tom = register("tom");
        observation = createObservation(lea);
        bleue = speciesId("Cyanistes caeruleus");
        charbonniere = speciesId("Parus major");
    }

    private String proposalsUrl() {
        return "/api/observations/" + observation + "/proposals";
    }

    /** Scénario complet du PRD : 3 votes et ≥ 2/3 → IDENTIFIED, puis le consensus se casse et revient. */
    @Test
    void fullConsensusScenario() throws Exception {
        // 1. Karim propose la mésange bleue : 1 vote
        mvc.perform(postJson(proposalsUrl(), karim.token(), "{\"speciesId\":" + bleue + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposal.votes").value(1))
                .andExpect(jsonPath("$.proposal.speciesId").value(bleue))
                .andExpect(jsonPath("$.observationStatus").value("NEEDS_ID"));
        int p1 = (Integer) JsonPath.read(json(mvc.perform(get(proposalsUrl()))), "$.proposals[0].id");

        // Sam propose la même espèce → 409
        expectError(mvc.perform(postJson(proposalsUrl(), sam.token(), "{\"speciesId\":" + bleue + "}")), 409, "CONFLICT");

        // 2. Sam vote → 2 votes, toujours NEEDS_ID ; revoter est idempotent
        vote(sam, observation, p1).andExpect(status().isOk())
                .andExpect(jsonPath("$.myVoteProposalId").value(p1))
                .andExpect(jsonPath("$.observationStatus").value("NEEDS_ID"));
        vote(sam, observation, p1).andExpect(jsonPath("$.observationStatus").value("NEEDS_ID"));

        // 3. Lea vote → 3/3 → IDENTIFIED
        vote(lea, observation, p1).andExpect(status().isOk())
                .andExpect(jsonPath("$.observationStatus").value("IDENTIFIED"))
                .andExpect(jsonPath("$.identifiedSpecies.id").value(bleue))
                .andExpect(jsonPath("$.identifiedSpecies.commonName").value("Mésange bleue"));

        mvc.perform(get(proposalsUrl()))
                .andExpect(jsonPath("$.totalVotes").value(3))
                .andExpect(jsonPath("$.consensus.reached").value(true))
                .andExpect(jsonPath("$.consensus.proposalId").value(p1))
                .andExpect(jsonPath("$.consensus.speciesId").value(bleue))
                .andExpect(jsonPath("$.proposals[0].votes").value(3))
                .andExpect(jsonPath("$.proposals[0].percent").value(100.0))
                .andExpect(jsonPath("$.proposals[0].proposedBy.id").value(karim.id()))
                .andExpect(jsonPath("$.proposals[0].voters[*].id", containsInAnyOrder(karim.id(), sam.id(), lea.id())));

        // 4. Tom propose la charbonnière → 3/4 = 75 % : reste IDENTIFIED
        mvc.perform(postJson(proposalsUrl(), tom.token(), "{\"speciesId\":" + charbonniere + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.observationStatus").value("IDENTIFIED"));
        int p2 = (Integer) JsonPath.read(json(mvc.perform(get(proposalsUrl()))), "$.proposals[1].id");

        mvc.perform(get(proposalsUrl()))
                .andExpect(jsonPath("$.proposals[*].votes", contains(3, 1)))
                .andExpect(jsonPath("$.proposals[*].percent", contains(75.0, 25.0)));

        // 5. Sam déplace son vote → 2/4 : repasse NEEDS_ID
        vote(sam, observation, p2)
                .andExpect(jsonPath("$.observationStatus").value("NEEDS_ID"))
                .andExpect(jsonPath("$.identifiedSpecies").isEmpty());
        mvc.perform(get(proposalsUrl()))
                .andExpect(jsonPath("$.totalVotes").value(4))
                .andExpect(jsonPath("$.consensus.reached").value(false))
                .andExpect(jsonPath("$.consensus.proposalId").isEmpty())
                .andExpect(jsonPath("$.proposals[0].voters[*].id", not(hasItem(sam.id()))));

        // 6. Sam retire son vote, puis une 2e fois → 404
        mvc.perform(auth(delete("/api/observations/" + observation + "/vote"), sam.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myVoteProposalId").isEmpty())
                .andExpect(jsonPath("$.observationStatus").value("NEEDS_ID"));
        expectError(mvc.perform(auth(delete("/api/observations/" + observation + "/vote"), sam.token())), 404, "NOT_FOUND");

        // 7. Sam revote bleue → 3/4 : IDENTIFIED
        vote(sam, observation, p1).andExpect(jsonPath("$.observationStatus").value("IDENTIFIED"));

        // Le fil reflète le statut
        mvc.perform(get("/api/observations").param("status", "IDENTIFIED").param("userId", String.valueOf(lea.id())))
                .andExpect(jsonPath("$.data[*].id", hasItem(observation)))
                .andExpect(jsonPath("$.data[0].identifiedSpecies.id").value(bleue))
                .andExpect(jsonPath("$.data[0].proposalCount").value(2));

        // Tom supprime sa proposition (il est le seul votant) → 204
        mvc.perform(auth(delete("/api/proposals/" + p2), tom.token())).andExpect(status().isNoContent());
        mvc.perform(get(proposalsUrl()))
                .andExpect(jsonPath("$.proposals", hasSize(1)))
                .andExpect(jsonPath("$.totalVotes").value(3));
    }

    @Test
    void tieKeepsOldestAsBestButNoConsensus() throws Exception {
        TestUser u5 = register("u5");
        TestUser u6 = register("u6");
        int p1 = propose(karim, observation, bleue);
        int p2 = propose(tom, observation, charbonniere);
        vote(sam, observation, p1);
        vote(lea, observation, p1).andExpect(jsonPath("$.observationStatus").value("IDENTIFIED")); // 3/4
        vote(u5, observation, p2);
        vote(u6, observation, p2).andExpect(jsonPath("$.observationStatus").value("NEEDS_ID"));   // 3/6
        mvc.perform(get(proposalsUrl()))
                .andExpect(jsonPath("$.proposals[0].id").value(p1)) // égalité : la plus ancienne d'abord
                .andExpect(jsonPath("$.proposals[*].percent", contains(50.0, 50.0)));
    }

    @Test
    void detailShowsMyVote() throws Exception {
        int p1 = propose(karim, observation, bleue);
        mvc.perform(auth(get("/api/observations/" + observation), karim.token()))
                .andExpect(jsonPath("$.myVoteProposalId").value(p1))
                .andExpect(jsonPath("$.proposals", hasSize(1)));
        mvc.perform(auth(get("/api/observations/" + observation), sam.token()))
                .andExpect(jsonPath("$.myVoteProposalId").isEmpty());
        mvc.perform(get("/api/observations/" + observation))
                .andExpect(jsonPath("$.myVoteProposalId").isEmpty());
    }

    @Test
    void proposingMovesMyExistingVote() throws Exception {
        int p1 = propose(karim, observation, bleue);
        vote(sam, observation, p1);
        int p2 = propose(sam, observation, charbonniere); // Sam change d'avis en proposant
        mvc.perform(get(proposalsUrl()))
                .andExpect(jsonPath("$.totalVotes").value(2))
                .andExpect(jsonPath("$.proposals[0].voters[*].id", contains(karim.id())));
        mvc.perform(auth(get("/api/observations/" + observation), sam.token()))
                .andExpect(jsonPath("$.myVoteProposalId").value(p2));
    }

    @Test
    void proposalErrors() throws Exception {
        expectError(mvc.perform(postJson(proposalsUrl(), null, "{\"speciesId\":" + bleue + "}")), 401, "UNAUTHORIZED");
        expectError(mvc.perform(postJson(proposalsUrl(), karim.token(), "{}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(postJson(proposalsUrl(), karim.token(), "{\"speciesId\":999999}")), 404, "NOT_FOUND");
        expectError(mvc.perform(postJson("/api/observations/999999/proposals", karim.token(),
                "{\"speciesId\":" + bleue + "}")), 404, "NOT_FOUND");
        expectError(mvc.perform(get("/api/observations/999999/proposals")), 404, "NOT_FOUND");
    }

    @Test
    void voteErrors() throws Exception {
        int p1 = propose(karim, observation, bleue);
        int otherObservation = createObservation(lea);

        expectError(mvc.perform(putJson("/api/observations/" + observation + "/vote", null,
                "{\"proposalId\":" + p1 + "}")), 401, "UNAUTHORIZED");
        expectError(mvc.perform(putJson("/api/observations/" + observation + "/vote", sam.token(), "{}")), 400, "VALIDATION_ERROR");
        expectError(vote(sam, otherObservation, p1), 404, "NOT_FOUND");      // proposition d'une autre observation
        expectError(vote(sam, observation, 999999), 404, "NOT_FOUND");
        expectError(vote(sam, 999999, p1), 404, "NOT_FOUND");
        expectError(mvc.perform(auth(delete("/api/observations/" + observation + "/vote"), sam.token())), 404, "NOT_FOUND");
    }

    @Test
    void cannotDeleteProposalWithOtherVotes() throws Exception {
        int p1 = propose(karim, observation, bleue);
        vote(sam, observation, p1);
        expectError(mvc.perform(auth(delete("/api/proposals/" + p1), karim.token())), 409, "CONFLICT");
    }

    @Test
    void onlyProposerOrAdminCanDeleteProposal() throws Exception {
        int p1 = propose(karim, observation, bleue);
        expectError(mvc.perform(auth(delete("/api/proposals/" + p1), sam.token())), 403, "FORBIDDEN");
        expectError(mvc.perform(auth(delete("/api/proposals/" + p1), lea.token())), 403, "FORBIDDEN");
        expectError(mvc.perform(delete("/api/proposals/" + p1)), 401, "UNAUTHORIZED");
        mvc.perform(auth(delete("/api/proposals/" + p1), admin().token())).andExpect(status().isNoContent());
        expectError(mvc.perform(auth(delete("/api/proposals/" + p1), karim.token())), 404, "NOT_FOUND");
    }

    @Test
    void deletingWinningProposalRecomputesConsensus() throws Exception {
        int p1 = propose(karim, observation, bleue);
        vote(sam, observation, p1);
        vote(lea, observation, p1).andExpect(jsonPath("$.observationStatus").value("IDENTIFIED"));
        // les votants retirent leur vote, puis Karim supprime sa proposition
        mvc.perform(auth(delete("/api/observations/" + observation + "/vote"), sam.token()));
        mvc.perform(auth(delete("/api/observations/" + observation + "/vote"), lea.token()));
        mvc.perform(auth(delete("/api/proposals/" + p1), karim.token())).andExpect(status().isNoContent());
        mvc.perform(get("/api/observations/" + observation))
                .andExpect(jsonPath("$.status").value("NEEDS_ID"))
                .andExpect(jsonPath("$.identifiedSpecies").isEmpty())
                .andExpect(jsonPath("$.proposals", hasSize(0)));
    }
}
