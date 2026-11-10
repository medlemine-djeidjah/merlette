package com.merlette.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Espèce proposée pour une observation. */
@Entity
@Table(name = "proposals")
@Getter
@Setter
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_id")
    private Observation observation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id")
    private Species species;

    /** Utilisateur qui a fait la proposition. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = TimeUtils.now();
    }
}
