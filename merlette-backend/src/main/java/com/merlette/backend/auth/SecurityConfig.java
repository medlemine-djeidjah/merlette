package com.merlette.backend.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); }
    @Bean SecretKeySpec jwtKey(@Value("${merlette.jwt-secret}") String secret) {
        byte[] bytes;
        if (secret.isBlank()) {
            bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("JWT_SECRET absent : clé temporaire, sessions invalidées au redémarrage.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET doit contenir au moins 32 octets UTF-8.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKeySpec key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKeySpec key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("merlette"));
        return decoder;
    }
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${merlette.cors-origin}") String origin) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        var unauthorized = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, exception) -> {
            response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Connexion requise ou session expirée.\",\"details\":[]}}");
        };
        return http.cors(c -> c.configurationSource(corsConfigurationSource)).csrf(c -> c.disable())
            .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(c -> c
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/categories", "/api/observations", "/api/observations/*", "/uploads/*").permitAll()
                .requestMatchers("/api/auth/me", "/api/users/me/observations", "/api/observations").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(c -> c.authenticationEntryPoint(unauthorized).accessDeniedHandler((req, res, ex) -> {
                res.setStatus(403); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"Accès interdit.\",\"details\":[]}}");
            }))
            .oauth2ResourceServer(c -> c.jwt(j -> {}).authenticationEntryPoint(unauthorized)).build();
    }
}
