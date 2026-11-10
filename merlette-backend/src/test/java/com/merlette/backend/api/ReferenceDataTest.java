package com.merlette.backend.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.merlette.backend.IntegrationTest;
import org.junit.jupiter.api.Test;

/** #9 Catégories, #16 Rechercher une espèce, #20 Gérer le référentiel. */
class ReferenceDataTest extends IntegrationTest {

    @Test
    void listsSeededCategories() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(8))))
                .andExpect(jsonPath("$[*].slug", hasItems("oiseaux", "plantes", "insectes")));
    }

    @Test
    void speciesSearchIgnoresAccentsAndCase() throws Exception {
        mvc.perform(get("/api/species").param("q", "MESANGE").param("category", "oiseaux"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].scientificName", hasItems("Cyanistes caeruleus", "Parus major")))
                .andExpect(jsonPath("$[*].category.slug", everyItem(is("oiseaux"))));
    }

    @Test
    void speciesSearchByScientificName() throws Exception {
        mvc.perform(get("/api/species").param("q", "parus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].scientificName", hasItem("Parus major")));
    }

    @Test
    void speciesSearchCategoryFilterExcludesOtherCategories() throws Exception {
        mvc.perform(get("/api/species").param("q", "mesange").param("category", "plantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void speciesSearchTooShortReturns400() throws Exception {
        expectError(mvc.perform(get("/api/species").param("q", "m")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(get("/api/species").param("q", "  ")), 400, "VALIDATION_ERROR");
    }

    @Test
    void speciesSearchTreatsWildcardsLiterally() throws Exception {
        mvc.perform(get("/api/species").param("q", "%%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void speciesSearchRespectsLimit() throws Exception {
        mvc.perform(get("/api/species").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getSpeciesById() throws Exception {
        int id = speciesId("Cyanistes caeruleus");
        mvc.perform(get("/api/species/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commonName").value("Mésange bleue"))
                .andExpect(jsonPath("$.category.slug").value("oiseaux"));
        expectError(mvc.perform(get("/api/species/999999")), 404, "NOT_FOUND");
    }

    // --- Admin ---------------------------------------------------------------

    @Test
    void onlyAdminCanCreateSpecies() throws Exception {
        TestUser user = register("user");
        String body = "{\"commonName\":\"Sittelle\",\"scientificName\":\"Sitta user %d\",\"categoryId\":%d}"
                .formatted(user.id(), categoryId("oiseaux"));

        expectError(mvc.perform(post("/api/species").contentType("application/json").content(body)), 401, "UNAUTHORIZED");
        expectError(mvc.perform(postJson("/api/species", user.token(), body)), 403, "FORBIDDEN");
    }

    @Test
    void adminCreatesSpecies_thenDuplicateIs409() throws Exception {
        TestUser admin = admin();
        String name = "Sitta europaea " + System.nanoTime();
        String body = "{\"commonName\":\"Sittelle torchepot\",\"scientificName\":\"%s\",\"categoryId\":%d}"
                .formatted(name, categoryId("oiseaux"));

        mvc.perform(postJson("/api/species", admin.token(), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.scientificName").value(name))
                .andExpect(jsonPath("$.category.slug").value("oiseaux"));
        expectError(mvc.perform(postJson("/api/species", admin.token(), body)), 409, "CONFLICT");
    }

    @Test
    void createSpeciesValidation() throws Exception {
        TestUser admin = admin();
        expectError(mvc.perform(postJson("/api/species", admin.token(),
                "{\"commonName\":\"X\",\"scientificName\":\"Xus unknown\",\"categoryId\":999999}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(postJson("/api/species", admin.token(),
                "{\"commonName\":\"\",\"scientificName\":\"Xus\",\"categoryId\":1}")), 400, "VALIDATION_ERROR");
    }

    @Test
    void adminCreatesCategory_thenDuplicateIs409() throws Exception {
        TestUser admin = admin();
        String slug = "lichens-" + System.nanoTime();
        String body = "{\"name\":\"Lichens %s\",\"slug\":\"%s\"}".formatted(slug, slug);

        mvc.perform(postJson("/api/categories", admin.token(), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(slug));
        expectError(mvc.perform(postJson("/api/categories", admin.token(), body)), 409, "CONFLICT");
    }

    @Test
    void createCategoryValidationAndRights() throws Exception {
        TestUser admin = admin();
        TestUser user = register("user");
        expectError(mvc.perform(postJson("/api/categories", admin.token(),
                "{\"name\":\"Bad\",\"slug\":\"Bad Slug!\"}")), 400, "VALIDATION_ERROR");
        expectError(mvc.perform(postJson("/api/categories", user.token(),
                "{\"name\":\"Nope\",\"slug\":\"nope\"}")), 403, "FORBIDDEN");
    }
}
