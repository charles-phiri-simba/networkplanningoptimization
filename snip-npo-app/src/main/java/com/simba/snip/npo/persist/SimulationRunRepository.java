package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, UUID> {

    List<SimulationRunEntity> findByScenario_IdOrderByStartedAtAsc(UUID scenarioId);

    List<SimulationRunEntity> findByTwin_IdAndBaselineTwinVersionOrderByStartedAtAsc(UUID twinId, int baselineTwinVersion);
}
