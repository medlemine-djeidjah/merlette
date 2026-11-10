package com.merlette.backend.repository;

import com.merlette.backend.entity.Photo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    List<Photo> findByObservationIdOrderByPosition(Integer observationId);

    List<Photo> findByObservationIdInOrderByPosition(Collection<Integer> observationIds);
}
