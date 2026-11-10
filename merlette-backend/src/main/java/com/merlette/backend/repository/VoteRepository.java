package com.merlette.backend.repository;

import com.merlette.backend.entity.Vote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Integer> {

    Optional<Vote> findByObservationIdAndUserId(Integer observationId, Integer userId);

    /** Tous les votes d'une observation, avec le votant. */
    @Query("SELECT v FROM Vote v JOIN FETCH v.user WHERE v.observationId = :observationId ORDER BY v.createdAt, v.id")
    List<Vote> findForObservation(@Param("observationId") Integer observationId);

    List<Vote> findByProposalId(Integer proposalId);
}
