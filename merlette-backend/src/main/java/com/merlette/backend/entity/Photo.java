package com.merlette.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "photos")
@Getter
@Setter
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_id")
    private Observation observation;

    /** Chemin public, ex. /uploads/abc.jpg */
    private String url;

    /** Ordre d'affichage, de 0 à 4. */
    private Integer position;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = TimeUtils.now();
    }
}
