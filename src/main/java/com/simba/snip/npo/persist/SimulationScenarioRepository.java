package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationScenarioRepository extends JpaRepository<SimulationScenarioEntity, UUID> {

    List<SimulationScenarioEntity> findByTwin_IdOrderByCreatedAtDesc(UUID twinId);
}
