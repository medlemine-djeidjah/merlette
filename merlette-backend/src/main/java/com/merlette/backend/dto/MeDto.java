package com.merlette.backend.dto;

import com.merlette.backend.entity.User;
import java.time.Instant;

/** Utilisateur connecté (avec son email, jamais le mot de passe). */
public record MeDto(Integer id, String username, String email, String role,
                    String bio, String avatarUrl, Instant createdAt) {

    public static MeDto from(User u) {
        return new MeDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(),
                u.getBio(), u.getAvatarUrl(), u.getCreatedAt());
    }
}
