package com.merlette.backend.security;

import com.merlette.backend.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Accès à l'utilisateur connecté (lu depuis le JWT). */
@Component
public class CurrentUser {

    /** Id de l'utilisateur connecté, ou null pour un visiteur. */
    public Integer idOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Integer.valueOf(jwt.getSubject());
        }
        return null;
    }

    /** Id de l'utilisateur connecté ; 401 si visiteur. */
    public Integer id() {
        Integer id = idOrNull();
        if (id == null) {
            throw ApiException.unauthorized();
        }
        return id;
    }

    /** 403 si l'utilisateur connecté n'est pas le propriétaire du contenu. */
    public void requireOwner(Integer ownerId) {
        if (!id().equals(ownerId)) {
            throw ApiException.forbidden();
        }
    }

    /** 403 si l'utilisateur connecté n'est ni le propriétaire ni un admin. */
    public void requireOwnerOrAdmin(Integer ownerId) {
        if (!id().equals(ownerId) && !isAdmin()) {
            throw ApiException.forbidden();
        }
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
