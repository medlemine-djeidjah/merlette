package com.merlette.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.merlette.backend.IntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** #1 Poster des images, #13 Décrire, #8 Fil récent, #9 Filtres, #15 Détail, #14 Gérer mes observations. */
class ObservationTest extends IntegrationTest {

    // --- Publication (#1, #13) -------------------------------------------------

    @Test
    void publishWithoutTokenReturns401() throws Exception {
        expectError(mvc.perform(newObservation(null, photo())
                .param("categoryId", "2").param("observedAt", "2026-10-01")), 401, "UNAUTHORIZED");
    }

    @Test
    void publishCreatesObservationWithStatusNeedsId() throws Exception {
        TestUser lea = register("lea");
        mvc.perform(newObservation(lea.token(), photo(), photo())
                        .param("categoryId", String.valueOf(categoryId("oiseaux")))
                        .param("observedAt", "2026-10-01")
                        .param("title", "Petit oiseau")
                        .param("description", "Sur la mangeoire")
                        .param("locationName", "Parc de la Tête d'Or")
                        .param("latitude", "45.7772")
                        .param("longitude", "4.8551"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("NEEDS_ID"))
                .andExpect(jsonPath("$.identifiedSpecies").isEmpty())
                .andExpect(jsonPath("$.author.id").value(lea.id()))
                .andExpect(jsonPath("$.category.slug").value("oiseaux"))
                .andExpect(jsonPath("$.observedAt").value("2026-10-01"))
                .andExpect(jsonPath("$.locationName").value("Parc de la Tête d'Or"))
                .andExpect(jsonPath("$.latitude").value(45.7772))
                .andExpect(jsonPath("$.photos", hasSize(2)))
                .andExpect(jsonPath("$.photos[0].position").value(0))
                .andExpect(jsonPath("$.photos[1].position").value(1))
                .andExpect(jsonPath("$.photos[0].url", startsWith("/uploads/")))
                .andExpect(jsonPath("$.proposals", hasSize(0)))
                .andExpect(jsonPath("$.commentCount").value(0));
    }

    @Test
    void publishedPhotoIsServed() throws Exception {
        TestUser lea = register("lea");
        int id = createObservation(lea);
        String url = JsonPath.read(json(mvc.perform(get("/api/observations/" + id))), "$.photos[0].url");

        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void publishValidationErrors() throws Exception {
        TestUser lea = register("lea");
        String oiseaux = String.valueOf(categoryId("oiseaux"));
        String tomorrow = LocalDate.now().plusDays(1).toString();

        // sans photo
        expectError(mvc.perform(newObservation(lea.token())
                .param("categoryId", oiseaux).param("observedAt", "2026-10-01")), 400, "VALIDATION_ERROR");
        // fichier texte
        MockMultipartFile txt = new MockMultipartFile("photos", "a.txt", "text/plain", "hello".getBytes());
        expectError(mvc.perform(newObservation(lea.token(), txt)
                .param("categoryId", oiseaux).param("observedAt", "2026-10-01")), 400, "VALIDATION_ERROR");
        // texte déguisé en .jpg : le contenu est vérifié, pas l'extension
        MockMultipartFile fake = new MockMultipartFile("photos", "fake.jpg", "image/jpeg", "hello".getBytes());
        expectError(mvc.perform(newObservation(lea.token(), fake)
                .param("categoryId", oiseaux).param("observedAt", "2026-10-01")), 400, "VALIDATION_ERROR");
        // date future
        expectError(mvc.perform(newObservation(lea.token(), photo())
                .param("categoryId", oiseaux).param("observedAt", tomorrow)), 400, "VALIDATION_ERROR");
        // date invalide
        expectError(mvc.perform(newObservation(lea.token(), photo())
                .param("categoryId", oiseaux).param("observedAt", "pas-une-date")), 400, "VALIDATION_ERROR");
        // sans catégorie
        expectError(mvc.perform(newObservation(lea.token(), photo())
                .param("observedAt", "2026-10-01")), 400, "VALIDATION_ERROR");
        // catégorie inconnue
        expectError(mvc.perform(newObservation(lea.token(), photo())
                .param("categoryId", "999999").param("observedAt", "2026-10-01")), 400, "VALIDATION_ERROR");
        // sans date
        expectError(mvc.perform(newObservation(lea.token(), photo())
                .param("categoryId", oiseaux)), 400, "VALIDATION_ERROR");
        // latitude hors limites
        expectError(mvc.perform(newObservation(lea.token(), photo())
                .param("categoryId", oiseaux).param("observedAt", "2026-10-01").param("latitude", "91")), 400, "VALIDATION_ERROR");
        // titre trop long
        expectError(mvc.perform(newObservation(lea.token(), photo())
                .param("categoryId", oiseaux).param("observedAt", "2026-10-01").param("title", "x".repeat(121))), 400, "VALIDATION_ERROR");
        // 6 photos
        expectError(mvc.perform(newObservation(lea.token(), photo(), photo(), photo(), photo(), photo(), photo())
                .param("categoryId", oiseaux).param("observedAt", "2026-10-01")), 400, "VALIDATION_ERROR");
    }

    @Test
    void photoOver5MbReturns413() throws Exception {
        TestUser lea = register("lea");
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);
        expectError(mvc.perform(newObservation(lea.token(), new MockMultipartFile("photos", "big.jpg", "image/jpeg", big))
                .param("categoryId", String.valueOf(categoryId("oiseaux"))).param("observedAt", "2026-10-01")),
                413, "FILE_TOO_LARGE");
    }

    @Test
    void acceptsPngAndWebp() throws Exception {
        TestUser lea = register("lea");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes();
        mvc.perform(newObservation(lea.token(),
                        new MockMultipartFile("photos", "a.png", "image/png", png),
                        new MockMultipartFile("photos", "b.webp", "image/webp", webp))
                        .param("categoryId", String.valueOf(categoryId("plantes"))).param("observedAt", "2026-10-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos[0].url", org.hamcrest.Matchers.endsWith(".png")))
                .andExpect(jsonPath("$.photos[1].url", org.hamcrest.Matchers.endsWith(".webp")));
    }

    // --- Fil (#8, #9) ------------------------------------------------------------

    @Test
    void feedIsPaginatedAndSortedByMostRecent() throws Exception {
        TestUser lea = register("lea");
        createObservation(lea);
        createObservation(lea);
        createObservation(lea);

        String body = json(mvc.perform(get("/api/observations").param("page", "1").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.limit").value(2))
                .andExpect(jsonPath("$.pagination.total").isNumber())
                .andExpect(jsonPath("$.pagination.totalPages").isNumber()));
        List<String> dates = JsonPath.read(body, "$.data[*].createdAt");
        assertThat(Instant.parse(dates.get(0))).isAfterOrEqualTo(Instant.parse(dates.get(1)));
    }

    @Test
    void feedCardHasExpectedFields() throws Exception {
        TestUser lea = register("lea");
        int id = createObservation(lea);
        mvc.perform(get("/api/observations").param("userId", String.valueOf(lea.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].title").value("Petit oiseau bleu"))
                .andExpect(jsonPath("$.data[0].category.slug").value("oiseaux"))
                .andExpect(jsonPath("$.data[0].author.username").value(lea.username()))
                .andExpect(jsonPath("$.data[0].coverUrl", startsWith("/uploads/")))
                .andExpect(jsonPath("$.data[0].proposalCount").value(0))
                .andExpect(jsonPath("$.data[0].commentCount").value(0))
                .andExpect(jsonPath("$.data[0].identifiedSpecies").isEmpty());
    }

    @Test
    void feedSortOldest() throws Exception {
        TestUser lea = register("lea");
        int first = createObservation(lea);
        int second = createObservation(lea);
        mvc.perform(get("/api/observations").param("userId", String.valueOf(lea.id())).param("sort", "oldest"))
                .andExpect(jsonPath("$.data[0].id").value(first))
                .andExpect(jsonPath("$.data[1].id").value(second));
    }

    @Test
    void feedLimitIsCappedAt50() throws Exception {
        mvc.perform(get("/api/observations").param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.limit").value(50))
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(50)));
    }

    @Test
    void feedFiltersByCategoryStatusAndAuthor() throws Exception {
        TestUser lea = register("lea");
        int bird = createObservation(lea, "oiseaux");
        int plant = createObservation(lea, "plantes");

        mvc.perform(get("/api/observations").param("category", "oiseaux").param("status", "NEEDS_ID").param("limit", "50"))
                .andExpect(jsonPath("$.data[*].category.slug", everyItem(is("oiseaux"))))
                .andExpect(jsonPath("$.data[*].status", everyItem(is("NEEDS_ID"))))
                .andExpect(jsonPath("$.data[*].id", hasItem(bird)))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(plant))));

        mvc.perform(get("/api/observations").param("userId", String.valueOf(lea.id())))
                .andExpect(jsonPath("$.pagination.total").value(2));

        mvc.perform(get("/api/observations").param("status", "IDENTIFIED").param("limit", "50"))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(bird))));
    }

    @Test
    void feedInvalidParametersReturn400() throws Exception {
        expectError(mvc.perform(get("/api/observations").param("status", "WRONG")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(get("/api/observations").param("sort", "random")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(get("/api/observations").param("page", "abc")), 400, "VALIDATION_ERROR");
    }

    // --- Détail (#15) -------------------------------------------------------------

    @Test
    void detailIsPublic_andUnknownIs404() throws Exception {
        TestUser lea = register("lea");
        int id = createObservation(lea);
        mvc.perform(get("/api/observations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos", hasSize(1)))
                .andExpect(jsonPath("$.proposals").isArray())
                .andExpect(jsonPath("$.myVoteProposalId").isEmpty())
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString());
        expectError(mvc.perform(get("/api/observations/999999")), 404, "NOT_FOUND");
    }

    // --- Gestion (#14) ------------------------------------------------------------

    @Test
    void onlyAuthorCanUpdate() throws Exception {
        TestUser lea = register("lea");
        TestUser karim = register("karim");
        int id = createObservation(lea);

        expectError(mvc.perform(patchJson("/api/observations/" + id, karim.token(), "{\"title\":\"Piraté\"}")), 403, "FORBIDDEN");
        expectError(mvc.perform(patchJson("/api/observations/" + id, admin().token(), "{\"title\":\"Admin\"}")), 403, "FORBIDDEN");
        expectError(mvc.perform(patchJson("/api/observations/" + id, null, "{\"title\":\"Anonyme\"}")), 401, "UNAUTHORIZED");

        mvc.perform(patchJson("/api/observations/" + id, lea.token(),
                        "{\"title\":\"Nouveau titre\",\"categoryId\":%d,\"locationName\":\"Lyon\"}".formatted(categoryId("plantes"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nouveau titre"))
                .andExpect(jsonPath("$.category.slug").value("plantes"))
                .andExpect(jsonPath("$.locationName").value("Lyon"))
                .andExpect(jsonPath("$.observedAt").value("2026-10-01")); // champ absent = inchangé
    }

    @Test
    void updateValidation() throws Exception {
        TestUser lea = register("lea");
        int id = createObservation(lea);
        String tomorrow = LocalDate.now().plusDays(1).toString();
        expectError(mvc.perform(patchJson("/api/observations/" + id, lea.token(), "{\"observedAt\":\"" + tomorrow + "\"}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(patchJson("/api/observations/" + id, lea.token(), "{\"categoryId\":999999}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(patchJson("/api/observations/" + id, lea.token(), "{\"longitude\":200}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(patchJson("/api/observations/999999", lea.token(), "{\"title\":\"x\"}")), 404, "NOT_FOUND");
    }

    @Test
    void addPhotos_authorOnly_max5() throws Exception {
        TestUser lea = register("lea");
        TestUser karim = register("karim");
        int id = createObservation(lea);
        String url = "/api/observations/" + id + "/photos";

        expectError(mvc.perform(auth(multipart(url).file(photo()), karim.token())), 403, "FORBIDDEN");

        mvc.perform(auth(multipart(url).file(photo()).file(photo()).file(photo()).file(photo()), lea.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos", hasSize(5)))
                .andExpect(jsonPath("$.photos[4].position").value(4));

        expectError(mvc.perform(auth(multipart(url).file(photo()), lea.token())), 400, "VALIDATION_ERROR");
    }

    @Test
    void deletePhoto_keepsAtLeastOne_andReusesFreePosition() throws Exception {
        TestUser lea = register("lea");
        TestUser karim = register("karim");
        int id = createObservation(lea);
        String detail = json(mvc.perform(auth(multipart("/api/observations/" + id + "/photos").file(photo()), lea.token())));
        int first = JsonPath.read(detail, "$.photos[0].id");
        int second = JsonPath.read(detail, "$.photos[1].id");
        String firstUrl = JsonPath.read(detail, "$.photos[0].url");

        expectError(mvc.perform(auth(delete("/api/observations/" + id + "/photos/" + first), karim.token())), 403, "FORBIDDEN");
        expectError(mvc.perform(auth(delete("/api/observations/" + id + "/photos/999999"), lea.token())), 404, "NOT_FOUND");

        mvc.perform(auth(delete("/api/observations/" + id + "/photos/" + first), lea.token()))
                .andExpect(status().isNoContent());
        assertThat(Files.exists(Path.of("target/test-uploads", firstUrl.substring("/uploads/".length())))).isFalse();

        expectError(mvc.perform(auth(delete("/api/observations/" + id + "/photos/" + second), lea.token())), 400, "VALIDATION_ERROR");

        // la nouvelle photo prend la position 0 libérée
        mvc.perform(auth(multipart("/api/observations/" + id + "/photos").file(photo()), lea.token()))
                .andExpect(jsonPath("$.photos[0].position").value(0));
    }

    @Test
    void deleteObservation_byAuthor_cascadesAndRemovesFiles() throws Exception {
        TestUser lea = register("lea");
        TestUser karim = register("karim");
        int id = createObservation(lea);
        int proposal = propose(karim, id, speciesId("Parus major"));
        comment(karim, id, "Joli");
        String url = JsonPath.read(json(mvc.perform(get("/api/observations/" + id))), "$.photos[0].url");

        expectError(mvc.perform(auth(delete("/api/observations/" + id), karim.token())), 403, "FORBIDDEN");
        expectError(mvc.perform(delete("/api/observations/" + id)), 401, "UNAUTHORIZED");

        mvc.perform(auth(delete("/api/observations/" + id), lea.token())).andExpect(status().isNoContent());

        expectError(mvc.perform(get("/api/observations/" + id)), 404, "NOT_FOUND");
        expectError(mvc.perform(get("/api/observations/" + id + "/proposals")), 404, "NOT_FOUND");
        expectError(mvc.perform(get("/api/observations/" + id + "/comments")), 404, "NOT_FOUND");
        expectError(mvc.perform(auth(delete("/api/proposals/" + proposal), karim.token())), 404, "NOT_FOUND");
        assertThat(Files.exists(Path.of("target/test-uploads", url.substring("/uploads/".length())))).isFalse();
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeleteAnyObservation() throws Exception {
        TestUser lea = register("lea");
        int id = createObservation(lea);
        mvc.perform(auth(delete("/api/observations/" + id), admin().token())).andExpect(status().isNoContent());
        expectError(mvc.perform(get("/api/observations/" + id)), 404, "NOT_FOUND");
    }

    @Test
    void myObservationsListsOnlyMine() throws Exception {
        TestUser lea = register("lea");
        TestUser karim = register("karim");
        int a = createObservation(lea);
        int b = createObservation(lea);
        int other = createObservation(karim);

        mvc.perform(auth(get("/api/users/me/observations"), lea.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.containsInAnyOrder(a, b)))
                .andExpect(jsonPath("$.data[*].id", not(hasItem(other))))
                .andExpect(jsonPath("$.pagination.total").value(2));
        expectError(mvc.perform(get("/api/users/me/observations")), 401, "UNAUTHORIZED");
    }
}
