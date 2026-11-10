package com.merlette.backend.repository;

import com.merlette.backend.entity.Observation;
import com.merlette.backend.entity.ObservationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObservationRepository extends JpaRepository<Observation, Integer> {

    /** Fil des observations ; chaque filtre est ignoré s'il vaut null. */
    @Query(value = """
            SELECT o FROM Observation o
            JOIN FETCH o.category c
            JOIN FETCH o.author
            LEFT JOIN FETCH o.identifiedSpecies
            WHERE (:category IS NULL OR c.slug = :category)
              AND (:status IS NULL OR CAST(o.status AS String) = :status)
              AND (:userId IS NULL OR o.author.id = :userId)
            """,
            countQuery = """
            SELECT COUNT(o) FROM Observation o
            WHERE (:category IS NULL OR o.category.slug = :category)
              AND (:status IS NULL OR CAST(o.status AS String) = :status)
              AND (:userId IS NULL OR o.author.id = :userId)
            """)
    Page<Observation> search(@Param("category") String category,
                             @Param("status") String status,
                             @Param("userId") Integer userId,
                             Pageable pageable);

    @Query("""
            SELECT o FROM Observation o
            JOIN FETCH o.category JOIN FETCH o.author LEFT JOIN FETCH o.identifiedSpecies
            WHERE o.id = :id
            """)
    Optional<Observation> findWithDetails(@Param("id") Integer id);

    /** Suppression directe en base : ON DELETE CASCADE supprime photos, propositions, votes, commentaires, notifications. */
    @Modifying
    @Query("DELETE FROM Observation o WHERE o.id = :id")
    void deleteWithCascade(@Param("id") Integer id);

    long countByAuthorId(Integer userId);

    long countByAuthorIdAndStatus(Integer userId, ObservationStatus status);
}
