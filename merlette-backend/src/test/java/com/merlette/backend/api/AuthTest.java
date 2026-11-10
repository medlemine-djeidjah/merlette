package com.merlette.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.merlette.backend.IntegrationTest;
import org.junit.jupiter.api.Test;

/** #11 Créer un compte, #10 Se connecter. */
class AuthTest extends IntegrationTest {

    private static String registerBody(String username, String email, String password) {
        return """
                {"username":"%s","email":"%s","password":"%s"}""".formatted(username, email, password);
    }

    @Test
    void registerReturnsTokenAndUserWithoutPassword() throws Exception {
        mvc.perform(postJson("/api/auth/register", null, registerBody("new_user_1", "new_user_1@test.local", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.username").value("new_user_1"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.createdAt").isString())
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void emailAlreadyUsedReturns409_caseInsensitive() throws Exception {
        TestUser lea = register("lea");
        expectError(mvc.perform(postJson("/api/auth/register", null,
                registerBody("other_" + lea.id(), lea.email().toUpperCase(), PASSWORD))), 409, "CONFLICT");
    }

    @Test
    void usernameAlreadyUsedReturns409() throws Exception {
        TestUser lea = register("lea");
        expectError(mvc.perform(postJson("/api/auth/register", null,
                registerBody(lea.username(), "x" + lea.email(), PASSWORD))), 409, "CONFLICT");
    }

    @Test
    void invalidRegistrationsReturn400() throws Exception {
        String[] bodies = {
                registerBody("ab", "ab@test.local", PASSWORD),                         // pseudo trop court
                registerBody("a".repeat(31), "long@test.local", PASSWORD),             // pseudo trop long
                registerBody("bad name!", "bad@test.local", PASSWORD),                 // caractères interdits
                registerBody("valid_name", "pas-un-email", PASSWORD),                  // email invalide
                registerBody("valid_name", "short@test.local", "1234567"),             // mot de passe < 8
                "{\"email\":\"missing@test.local\",\"password\":\"Secret123\"}"        // pseudo manquant
        };
        for (String body : bodies) {
            expectError(mvc.perform(postJson("/api/auth/register", null, body)), 400, "VALIDATION_ERROR");
        }
    }

    @Test
    void loginReturnsToken() throws Exception {
        TestUser lea = register("lea");
        mvc.perform(postJson("/api/auth/login", null,
                        "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(lea.email(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.id").value(lea.id()));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSameError() throws Exception {
        TestUser lea = register("lea");
        String wrongPassword = json(mvc.perform(postJson("/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"Mauvais123\"}".formatted(lea.email()))));
        String unknownEmail = json(mvc.perform(postJson("/api/auth/login", null,
                "{\"email\":\"nobody@test.local\",\"password\":\"Secret123\"}")));

        assertThat((String) JsonPath.read(wrongPassword, "$.error.code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat((String) JsonPath.read(unknownEmail, "$.error.code")).isEqualTo("INVALID_CREDENTIALS");
        assertThat((String) JsonPath.read(wrongPassword, "$.error.message"))
                .isEqualTo(JsonPath.read(unknownEmail, "$.error.message"));
    }

    @Test
    void loginWithMissingFieldsReturns400() throws Exception {
        expectError(mvc.perform(postJson("/api/auth/login", null, "{}")), 400, "VALIDATION_ERROR");
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        TestUser lea = register("lea");
        mvc.perform(auth(get("/api/auth/me"), lea.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lea.id()))
                .andExpect(jsonPath("$.email").value(lea.email()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.bio").isEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void meWithoutTokenReturns401() throws Exception {
        expectError(mvc.perform(get("/api/auth/me")), 401, "UNAUTHORIZED");
    }

    @Test
    void meWithInvalidTokenReturns401() throws Exception {
        expectError(mvc.perform(auth(get("/api/auth/me"), "not.a.token")), 401, "UNAUTHORIZED");
    }

    @Test
    void adminLoginHasAdminRole() throws Exception {
        mvc.perform(postJson("/api/auth/login", null, "{\"email\":\"admin@merlette.local\",\"password\":\"Admin1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void publicRouteIgnoresInvalidToken() throws Exception {
        // optionalAuth : un token expiré ne bloque pas la lecture du fil
        mvc.perform(auth(get("/api/observations"), "expired.or.invalid"))
                .andExpect(status().isOk());
    }
}
