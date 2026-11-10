package com.merlette.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * « optionalAuth » : sur une route publique en lecture, un token invalide ou expiré
 * est ignoré (l'utilisateur est traité comme visiteur) au lieu de renvoyer 401.
 * Sur les autres routes, le token est vérifié normalement.
 */
public class OptionalBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    private final JwtDecoder jwtDecoder;
    private final RequestMatcher publicRoutes;

    public OptionalBearerTokenResolver(JwtDecoder jwtDecoder, RequestMatcher publicRoutes) {
        this.jwtDecoder = jwtDecoder;
        this.publicRoutes = publicRoutes;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (!publicRoutes.matches(request)) {
            return delegate.resolve(request);
        }
        try {
            String token = delegate.resolve(request);
            if (token != null) {
                jwtDecoder.decode(token);
            }
            return token;
        } catch (JwtException | OAuth2AuthenticationException e) {
            return null; // token invalide → visiteur
        }
    }
}
