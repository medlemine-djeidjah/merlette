package com.merlette.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Vote d'un utilisateur pour une proposition (un seul par observation). */
@Entity
@Table(name = "votes")
@Getter
@Setter
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Copie de proposal.observation (clé étrangère composite en base). */
    private Integer observationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = TimeUtils.now();
    }
}
