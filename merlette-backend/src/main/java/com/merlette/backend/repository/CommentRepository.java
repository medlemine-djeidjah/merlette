package com.merlette.backend.repository;

import com.merlette.backend.entity.Comment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @Query("""
            SELECT c FROM Comment c JOIN FETCH c.author
            WHERE c.observation.id = :observationId
            ORDER BY c.createdAt, c.id
            """)
    List<Comment> findForObservation(@Param("observationId") Integer observationId);

    long countByObservationId(Integer observationId);

    /** Nombre de commentaires par observation : lignes [observationId, count]. */
    @Query("""
            SELECT c.observation.id, COUNT(c) FROM Comment c
            WHERE c.observation.id IN :ids GROUP BY c.observation.id
            """)
    List<Object[]> countByObservationIds(@Param("ids") Collection<Integer> ids);

    /** Identifiants des utilisateurs ayant déjà commenté l'observation. */
    @Query("SELECT DISTINCT c.author.id FROM Comment c WHERE c.observation.id = :observationId")
    List<Integer> findCommenterIds(@Param("observationId") Integer observationId);
}
