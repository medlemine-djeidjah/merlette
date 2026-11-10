package com.merlette.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "observations")
@Getter
@Setter
public class Observation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Auteur de l'observation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private String title;

    private String description;

    private LocalDate observedAt;

    private String locationName;

    private Double latitude;

    private Double longitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ObservationStatus status = ObservationStatus.NEEDS_ID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identified_species_id")
    private Species identifiedSpecies;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = TimeUtils.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = TimeUtils.now();
    }
}
