package com.merlette.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.merlette.backend.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** #6 Notifications (PRD §6.2) : jamais pour ses propres actions. */
class NotificationTest extends IntegrationTest {

    private TestUser lea;
    private TestUser karim;
    private TestUser sam;
    private int observation;

    @BeforeEach
    void setUp() throws Exception {
        lea = register("lea");
        karim = register("karim");
        sam = register("sam");
        observation = createObservation(lea);
    }

    @Test
    void newProposalNotifiesAuthor() throws Exception {
        propose(karim, observation, speciesId("Cyanistes caeruleus"));

        mvc.perform(auth(get("/api/notifications"), lea.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].type").value("NEW_PROPOSAL"))
                .andExpect(jsonPath("$.data[0].observationId").value(observation))
                .andExpect(jsonPath("$.data[0].actor.id").value(karim.id()))
                .andExpect(jsonPath("$.data[0].isRead").value(false))
                .andExpect(jsonPath("$.data[0].message").isString())
                .andExpect(jsonPath("$.pagination.total").value(1));
        assertThat(notificationTypes(karim)).isEmpty();
    }

    @Test
    void authorProposingOnOwnObservationIsNotNotified() throws Exception {
        propose(lea, observation, speciesId("Cyanistes caeruleus"));
        comment(lea, observation, "Je pense que c'est une mésange");
        assertThat(notificationTypes(lea)).isEmpty();
    }

    @Test
    void newCommentNotifiesAuthorAndPreviousCommenters() throws Exception {
        comment(karim, observation, "Premier");
        assertThat(notificationTypes(lea)).containsExactly("NEW_COMMENT");
        assertThat(notificationTypes(karim)).isEmpty();

        comment(sam, observation, "Deuxième");
        assertThat(notificationTypes(lea)).containsExactly("NEW_COMMENT", "NEW_COMMENT");
        assertThat(notificationTypes(karim)).containsExactly("NEW_COMMENT");
        assertThat(notificationTypes(sam)).isEmpty();

        // Lea répond : Karim et Sam sont notifiés, pas Lea
        comment(lea, observation, "Merci !");
        assertThat(notificationTypes(karim)).hasSize(2);
        assertThat(notificationTypes(sam)).containsExactly("NEW_COMMENT");
        assertThat(notificationTypes(lea)).hasSize(2);
    }

    @Test
    void consensusNotifiesAuthorAndWinningProposer() throws Exception {
        int p1 = propose(karim, observation, speciesId("Cyanistes caeruleus"));
        vote(sam, observation, p1);
        vote(lea, observation, p1).andExpect(jsonPath("$.observationStatus").value("IDENTIFIED"));

        // Lea a déclenché le consensus elle-même : pas de notification pour elle
        assertThat(notificationTypes(lea)).containsExactly("NEW_PROPOSAL");
        assertThat(notificationTypes(karim)).containsExactly("OBSERVATION_IDENTIFIED");
        assertThat(notificationTypes(sam)).isEmpty();
    }

    @Test
    void consensusReachedByThirdPartyNotifiesAuthor() throws Exception {
        int p1 = propose(karim, observation, speciesId("Cyanistes caeruleus"));
        vote(lea, observation, p1);
        vote(sam, observation, p1).andExpect(jsonPath("$.observationStatus").value("IDENTIFIED"));

        String body = json(mvc.perform(auth(get("/api/notifications"), lea.token())));
        assertThat((List<String>) JsonPath.read(body, "$.data[*].type"))
                .containsExactly("OBSERVATION_IDENTIFIED", "NEW_PROPOSAL");
        assertThat((String) JsonPath.read(body, "$.data[0].message")).contains("Mésange bleue");
        assertThat((Integer) JsonPath.read(body, "$.data[0].actor.id")).isEqualTo(sam.id());

        // un vote de plus ne renvoie pas de nouvelle notification (déjà identifiée)
        vote(register("other"), observation, p1);
        assertThat(notificationTypes(lea)).hasSize(2);
    }

    @Test
    void unreadCountAndMarkAsRead() throws Exception {
        comment(karim, observation, "Un");
        comment(sam, observation, "Deux");
        propose(karim, observation, speciesId("Parus major"));

        mvc.perform(auth(get("/api/notifications/unread-count"), lea.token()))
                .andExpect(jsonPath("$.count").value(3));

        int first = JsonPath.read(json(mvc.perform(auth(get("/api/notifications"), lea.token()))), "$.data[0].id");

        // Sam ne peut pas lire la notification de Lea
        expectError(mvc.perform(auth(patch("/api/notifications/" + first + "/read"), sam.token())), 404, "NOT_FOUND");
        expectError(mvc.perform(auth(patch("/api/notifications/999999/read"), lea.token())), 404, "NOT_FOUND");

        mvc.perform(auth(patch("/api/notifications/" + first + "/read"), lea.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first))
                .andExpect(jsonPath("$.isRead").value(true));

        mvc.perform(auth(get("/api/notifications/unread-count"), lea.token()))
                .andExpect(jsonPath("$.count").value(2));
        mvc.perform(auth(get("/api/notifications").param("unread", "true"), lea.token()))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].isRead", everyItem(is(false))));

        mvc.perform(auth(patch("/api/notifications/read-all"), lea.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));
        mvc.perform(auth(get("/api/notifications/unread-count"), lea.token()))
                .andExpect(jsonPath("$.count").value(0));
        mvc.perform(auth(get("/api/notifications"), lea.token()))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    void notificationsAreSortedNewestFirstAndPaginated() throws Exception {
        for (int i = 0; i < 22; i++) {
            comment(karim, observation, "Commentaire " + i);
        }
        mvc.perform(auth(get("/api/notifications"), lea.token()))
                .andExpect(jsonPath("$.data", hasSize(20)))
                .andExpect(jsonPath("$.pagination.total").value(22))
                .andExpect(jsonPath("$.pagination.totalPages").value(2));
        mvc.perform(auth(get("/api/notifications").param("page", "2"), lea.token()))
                .andExpect(jsonPath("$.data", hasSize(2)));

        List<Integer> ids = JsonPath.read(json(mvc.perform(auth(get("/api/notifications"), lea.token()))), "$.data[*].id");
        assertThat(ids).isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void notificationRoutesRequireAuth() throws Exception {
        expectError(mvc.perform(get("/api/notifications")), 401, "UNAUTHORIZED");
        expectError(mvc.perform(get("/api/notifications/unread-count")), 401, "UNAUTHORIZED");
        expectError(mvc.perform(patch("/api/notifications/read-all")), 401, "UNAUTHORIZED");
    }
}
