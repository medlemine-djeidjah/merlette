package com.merlette.backend.dto;

import java.time.Instant;

/** Profil public : pas d'email. */
public record UserProfileDto(Integer id, String username, String bio, String avatarUrl,
                             Instant createdAt, Stats stats) {

    public record Stats(long observations, long proposals, long identifiedObservations) {}
}
