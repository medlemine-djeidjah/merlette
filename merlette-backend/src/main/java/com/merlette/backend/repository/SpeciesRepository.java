package com.merlette.backend.repository;

import com.merlette.backend.entity.Species;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpeciesRepository extends JpaRepository<Species, Integer> {

    boolean existsByScientificNameIgnoreCase(String scientificName);

    /** Recherche insensible à la casse et aux accents sur le nom commun ou scientifique. */
    @Query(value = """
            SELECT s.* FROM species s
            JOIN categories c ON c.id = s.category_id
            WHERE (CAST(:q AS TEXT) IS NULL
                   OR unaccent(LOWER(s.common_name)) LIKE '%' || unaccent(LOWER(CAST(:q AS TEXT))) || '%'
                   OR unaccent(LOWER(s.scientific_name)) LIKE '%' || unaccent(LOWER(CAST(:q AS TEXT))) || '%')
              AND (CAST(:category AS TEXT) IS NULL OR c.slug = CAST(:category AS TEXT))
            ORDER BY s.common_name
            LIMIT :limit
            """, nativeQuery = true)
    List<Species> search(@Param("q") String q, @Param("category") String category, @Param("limit") int limit);
}
