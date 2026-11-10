package com.merlette.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentation OpenAPI affichée par Swagger UI sur /docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI merletteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Merlette API")
                        .version("1.0")
                        .description("""
                                API REST de Merlette : observations, identification communautaire, commentaires, notifications.

                                **Authentification** : appeler `POST /api/auth/login` (ou `register`), copier le `token`,
                                puis cliquer sur **Authorize** et le coller (sans le préfixe `Bearer`).

                                **Erreurs** : toujours au format `{ "error": { "code", "message", "details" } }`.

                                **Images** : servies sur `/uploads/<fichier>` (URL renvoyée dans `photos[].url`).

                                Compte admin de démo : `admin@merlette.local` / `Admin1234`."""))
                .components(new Components().addSecuritySchemes("bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
