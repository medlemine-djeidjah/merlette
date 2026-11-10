package com.merlette.backend.repository;

import com.merlette.backend.entity.Proposal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposalRepository extends JpaRepository<Proposal, Integer> {

    /** Propositions d'une observation, de la plus ancienne à la plus récente. */
    @Query("""
            SELECT p FROM Proposal p JOIN FETCH p.species JOIN FETCH p.author
            WHERE p.observation.id = :observationId
            ORDER BY p.createdAt, p.id
            """)
    List<Proposal> findForObservation(@Param("observationId") Integer observationId);

    boolean existsByObservationIdAndSpeciesId(Integer observationId, Integer speciesId);

    long countByAuthorId(Integer userId);

    /** Nombre de propositions par observation : lignes [observationId, count]. */
    @Query("""
            SELECT p.observation.id, COUNT(p) FROM Proposal p
            WHERE p.observation.id IN :ids GROUP BY p.observation.id
            """)
    List<Object[]> countByObservationIds(@Param("ids") Collection<Integer> ids);
}
