package com.merlette.backend.dto;

import com.merlette.backend.entity.User;

/** Forme courte d'un utilisateur : auteur, votant, acteur… */
public record UserSummaryDto(Integer id, String username) {

    public static UserSummaryDto from(User u) {
        return u == null ? null : new UserSummaryDto(u.getId(), u.getUsername());
    }
}
