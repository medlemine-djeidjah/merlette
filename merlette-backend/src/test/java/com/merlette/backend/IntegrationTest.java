package com.merlette.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/**
 * Base des tests d'intégration : application complète + vraie base PostgreSQL (Testcontainers).
 * La base est partagée entre les tests : chaque test crée ses propres utilisateurs et observations.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-for-integration-tests-0123456789",
        "app.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

    /** En-tête JPEG minimal : suffisant pour la détection du type d'image. */
    protected static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0};
    protected static final String PASSWORD = "Secret123";

    private static final int RUN = ThreadLocalRandom.current().nextInt(10_000, 99_999);
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    protected record TestUser(int id, String username, String email, String token) {}

    // --- Utilisateurs --------------------------------------------------------

    /** Inscrit un nouvel utilisateur unique (ex. "lea_12345_7"). */
    protected TestUser register(String name) throws Exception {
        String username = name + "_" + RUN + "_" + COUNTER.incrementAndGet();
        String email = username + "@test.local";
        String body = mvc.perform(postJson("/api/auth/register", null, """
                        {"username":"%s","email":"%s","password":"%s"}""".formatted(username, email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new TestUser(JsonPath.read(body, "$.user.id"), username, email, JsonPath.read(body, "$.token"));
    }

    protected TestUser admin() throws Exception {
        String body = mvc.perform(postJson("/api/auth/login", null,
                        "{\"email\":\"admin@merlette.local\",\"password\":\"Admin1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new TestUser(JsonPath.read(body, "$.user.id"), "admin", "admin@merlette.local", JsonPath.read(body, "$.token"));
    }

    // --- Référentiel ---------------------------------------------------------

    protected int categoryId(String slug) throws Exception {
        String body = mvc.perform(get("/api/categories")).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[?(@.slug == '" + slug + "')].id");
        return ids.getFirst();
    }

    protected int speciesId(String scientificName) throws Exception {
        String body = mvc.perform(get("/api/species").param("q", scientificName))
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$[?(@.scientificName == '" + scientificName + "')].id");
        return ids.getFirst();
    }

    // --- Observations --------------------------------------------------------

    /** Publie une observation d'oiseau avec une photo ; renvoie son id. */
    protected int createObservation(TestUser author) throws Exception {
        return createObservation(author, "oiseaux");
    }

    protected int createObservation(TestUser author, String categorySlug) throws Exception {
        String body = mvc.perform(newObservation(author.token(), photo())
                        .param("categoryId", String.valueOf(categoryId(categorySlug)))
                        .param("observedAt", "2026-10-01")
                        .param("title", "Petit oiseau bleu"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected static MockMultipartFile photo() {
        return new MockMultipartFile("photos", "bird.jpg", "image/jpeg", JPEG);
    }

    /** Requête multipart POST /api/observations, sans les champs texte. */
    protected static MockMultipartHttpServletRequestBuilder newObservation(String token, MockMultipartFile... files) {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/observations");
        for (MockMultipartFile f : files) {
            req.file(f);
        }
        auth(req, token);
        return req;
    }

    // --- Identification ------------------------------------------------------

    protected int propose(TestUser user, int observationId, int speciesId) throws Exception {
        String body = mvc.perform(postJson("/api/observations/" + observationId + "/proposals", user.token(),
                        "{\"speciesId\":" + speciesId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.proposal.id");
    }

    protected ResultActions vote(TestUser user, int observationId, int proposalId) throws Exception {
        return mvc.perform(putJson("/api/observations/" + observationId + "/vote", user.token(),
                "{\"proposalId\":" + proposalId + "}"));
    }

    protected int comment(TestUser user, int observationId, String content) throws Exception {
        String body = mvc.perform(postJson("/api/observations/" + observationId + "/comments", user.token(),
                        "{\"content\":\"" + content + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Types des notifications de l'utilisateur (les plus récentes d'abord). */
    protected List<String> notificationTypes(TestUser user) throws Exception {
        String body = mvc.perform(auth(get("/api/notifications"), user.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data[*].type");
    }

    // --- Requêtes ------------------------------------------------------------

    protected static <T extends AbstractMockHttpServletRequestBuilder<?>> T auth(T request, String token) {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return request;
    }

    protected static MockHttpServletRequestBuilder postJson(String url, String token, String json) {
        return auth(post(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    protected static MockHttpServletRequestBuilder putJson(String url, String token, String json) {
        return auth(put(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    protected static MockHttpServletRequestBuilder patchJson(String url, String token, String json) {
        return auth(patch(url).contentType(MediaType.APPLICATION_JSON).content(json), token);
    }

    /** Vérifie le statut HTTP et le format d'erreur uniforme { error: { code, message } }. */
    protected static void expectError(ResultActions result, int httpStatus, String code) throws Exception {
        result.andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").isString());
    }

    protected static String json(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }
}
