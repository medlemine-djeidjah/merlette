package com.merlette.backend.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Heure courante à la microseconde (précision de TIMESTAMPTZ en PostgreSQL). */
public final class TimeUtils {

    private TimeUtils() {}

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
