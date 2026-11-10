package com.merlette.backend.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.merlette.backend.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** #3 Commenter, #18 Modifier / supprimer mes commentaires, #19 Modération. */
class CommentTest extends IntegrationTest {

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

    private String commentsUrl() {
        return "/api/observations/" + observation + "/comments";
    }

    @Test
    void createAndListInChronologicalOrder() throws Exception {
        mvc.perform(postJson(commentsUrl(), karim.token(), "{\"content\":\"  Calotte bleue  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.content").value("Calotte bleue"))
                .andExpect(jsonPath("$.author.id").value(karim.id()))
                .andExpect(jsonPath("$.edited").value(false));
        comment(sam, observation, "D'accord");

        mvc.perform(get(commentsUrl()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].author.id", contains(karim.id(), sam.id())));
        mvc.perform(get("/api/observations/" + observation))
                .andExpect(jsonPath("$.commentCount").value(2));
    }

    @Test
    void createErrors() throws Exception {
        expectError(mvc.perform(postJson(commentsUrl(), null, "{\"content\":\"Bonjour\"}")), 401, "UNAUTHORIZED");
        expectError(mvc.perform(postJson(commentsUrl(), karim.token(), "{\"content\":\"   \"}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(postJson(commentsUrl(), karim.token(), "{}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(postJson(commentsUrl(), karim.token(),
                "{\"content\":\"" + "a".repeat(1001) + "\"}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(postJson("/api/observations/999999/comments", karim.token(),
                "{\"content\":\"Bonjour\"}")), 404, "NOT_FOUND");
        expectError(mvc.perform(get("/api/observations/999999/comments")), 404, "NOT_FOUND");
    }

    @Test
    void contentOfExactly1000CharsIsAccepted() throws Exception {
        mvc.perform(postJson(commentsUrl(), karim.token(), "{\"content\":\"" + "a".repeat(1000) + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void onlyAuthorCanEdit() throws Exception {
        int id = comment(karim, observation, "Mésange bleue");
        expectError(mvc.perform(patchJson("/api/comments/" + id, sam.token(), "{\"content\":\"Modifié\"}")), 403, "FORBIDDEN");
        expectError(mvc.perform(patchJson("/api/comments/" + id, admin().token(), "{\"content\":\"Admin\"}")), 403, "FORBIDDEN");
        expectError(mvc.perform(patchJson("/api/comments/" + id, karim.token(), "{\"content\":\" \"}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(patchJson("/api/comments/999999", karim.token(), "{\"content\":\"x\"}")), 404, "NOT_FOUND");

        mvc.perform(patchJson("/api/comments/" + id, karim.token(), "{\"content\":\"Mésange bleue, ventre jaune\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Mésange bleue, ventre jaune"))
                .andExpect(jsonPath("$.edited").value(true));
        mvc.perform(get(commentsUrl()))
                .andExpect(jsonPath("$[0].edited").value(true));
    }

    @Test
    void deleteByAuthorOrAdminOnly() throws Exception {
        int byKarim = comment(karim, observation, "Un");
        int bySam = comment(sam, observation, "Deux");

        expectError(mvc.perform(auth(delete("/api/comments/" + byKarim), sam.token())), 403, "FORBIDDEN");
        expectError(mvc.perform(auth(delete("/api/comments/" + byKarim), lea.token())), 403, "FORBIDDEN"); // auteur de l'observation
        expectError(mvc.perform(delete("/api/comments/" + byKarim)), 401, "UNAUTHORIZED");

        mvc.perform(auth(delete("/api/comments/" + byKarim), karim.token())).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/api/comments/" + bySam), admin().token())).andExpect(status().isNoContent());
        expectError(mvc.perform(auth(delete("/api/comments/" + bySam), sam.token())), 404, "NOT_FOUND");
        mvc.perform(get(commentsUrl())).andExpect(jsonPath("$", hasSize(0)));
    }
}
