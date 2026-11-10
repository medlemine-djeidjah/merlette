package com.merlette.backend.observation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PhotoRepository extends JpaRepository<Photo, Integer> {
    List<Photo> findByObservationIdOrderByPosition(Integer observationId);
}
