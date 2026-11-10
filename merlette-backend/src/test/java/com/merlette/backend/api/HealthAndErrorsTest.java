package com.merlette.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.merlette.backend.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Socle technique : health check et format d'erreur uniforme. */
class HealthAndErrorsTest extends IntegrationTest {

    @Test
    void healthReturns200() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void unknownRouteForLoggedUserReturns404Json() throws Exception {
        TestUser user = register("err");
        expectError(mvc.perform(auth(get("/api/nope"), user.token())), 404, "NOT_FOUND");
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        expectError(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{oops")),
                400, "VALIDATION_ERROR");
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        expectError(mvc.perform(get("/api/observations/abc")), 400, "VALIDATION_ERROR");
    }

    @Test
    void validationErrorsListTheFields() throws Exception {
        mvc.perform(postJson("/api/auth/register", null, "{\"username\":\"ok_name\",\"email\":\"bad\",\"password\":\"Secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("email"));
    }
}
