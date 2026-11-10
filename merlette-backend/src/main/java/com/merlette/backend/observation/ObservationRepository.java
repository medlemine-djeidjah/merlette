package com.merlette.backend.observation;

import org.springframework.data.jpa.repository.*;

public interface ObservationRepository extends JpaRepository<Observation, Integer>, JpaSpecificationExecutor<Observation> {}
