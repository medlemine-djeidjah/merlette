package com.merlette.backend.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.merlette.backend.IntegrationTest;
import org.junit.jupiter.api.Test;

/** #12 Profil, #10 Mes discussions. */
class UserTest extends IntegrationTest {

    @Test
    void publicProfileHasStatsAndNoEmail() throws Exception {
        TestUser lea = register("lea");
        TestUser karim = register("karim");
        TestUser sam = register("sam");
        int obs = createObservation(lea);
        createObservation(lea);
        int p = propose(karim, obs, speciesId("Cyanistes caeruleus"));
        propose(lea, obs, speciesId("Parus major"));
        vote(sam, obs, p);
        vote(register("x"), obs, p); // 3 votes sur 4 → identifiée

        mvc.perform(get("/api/users/" + lea.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(lea.username()))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.stats.observations").value(2))
                .andExpect(jsonPath("$.stats.proposals").value(1))
                .andExpect(jsonPath("$.stats.identifiedObservations").value(1));
        expectError(mvc.perform(get("/api/users/999999")), 404, "NOT_FOUND");
    }

    @Test
    void updateBioAndAvatarOnly() throws Exception {
        TestUser lea = register("lea");
        mvc.perform(patchJson("/api/users/me", lea.token(),
                        "{\"bio\":\"Randonneuse\",\"avatarUrl\":\"https://img/a.png\",\"username\":\"hacked\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Randonneuse"))
                .andExpect(jsonPath("$.avatarUrl").value("https://img/a.png"))
                .andExpect(jsonPath("$.username").value(lea.username()))
                .andExpect(jsonPath("$.role").value("USER"));

        // champ absent = inchangé
        mvc.perform(patchJson("/api/users/me", lea.token(), "{\"bio\":\"Curieuse\"}"))
                .andExpect(jsonPath("$.avatarUrl").value("https://img/a.png"));

        expectError(mvc.perform(patchJson("/api/users/me", lea.token(), "{\"bio\":\"" + "x".repeat(501) + "\"}")),
                400, "VALIDATION_ERROR");
        expectError(mvc.perform(patchJson("/api/users/me", null, "{\"bio\":\"x\"}")), 401, "UNAUTHORIZED");
    }

    @Test
    void conversationsListParticipationWithRoles() throws Exception {
        TestUser lea = register("lea");
        TestUser karim = register("karim");
        int mine = createObservation(karim);
        int commented = createObservation(lea);
        int proposed = createObservation(lea);
        int unrelated = createObservation(lea);

        comment(karim, commented, "Joli");
        propose(karim, proposed, speciesId("Parus major"));

        mvc.perform(auth(get("/api/users/me/conversations"), karim.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                // tri par dernière activité : la proposition est la plus récente
                .andExpect(jsonPath("$.data[*].observationId", contains(proposed, commented, mine)))
                .andExpect(jsonPath("$.data[*].observationId", not(hasItem(unrelated))))
                .andExpect(jsonPath("$.data[0].myRole", contains("PROPOSER")))
                .andExpect(jsonPath("$.data[1].myRole", contains("COMMENTER")))
                .andExpect(jsonPath("$.data[2].myRole", contains("AUTHOR")))
                .andExpect(jsonPath("$.data[0].status").value("NEEDS_ID"))
                .andExpect(jsonPath("$.data[0].coverUrl").isString())
                .andExpect(jsonPath("$.data[0].lastActivity").isString());

        // une nouvelle activité fait remonter la discussion
        comment(lea, mine, "Réponse");
        mvc.perform(auth(get("/api/users/me/conversations"), karim.token()))
                .andExpect(jsonPath("$.data[0].observationId").value(mine));
    }

    @Test
    void conversationsWithSeveralRoles() throws Exception {
        TestUser lea = register("lea");
        int obs = createObservation(lea);
        comment(lea, obs, "Je cherche le nom");
        propose(lea, obs, speciesId("Parus major"));
        mvc.perform(auth(get("/api/users/me/conversations"), lea.token()))
                .andExpect(jsonPath("$.data[0].myRole", containsInAnyOrder("AUTHOR", "COMMENTER", "PROPOSER")));
    }

    @Test
    void emptyConversationsAndAuthRequired() throws Exception {
        TestUser tom = register("tom");
        mvc.perform(auth(get("/api/users/me/conversations"), tom.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        expectError(mvc.perform(get("/api/users/me/conversations")), 401, "UNAUTHORIZED");
    }
}
