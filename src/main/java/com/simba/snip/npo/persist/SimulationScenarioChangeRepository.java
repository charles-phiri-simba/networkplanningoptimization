package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SimulationScenarioChangeRepository extends JpaRepository<SimulationScenarioChangeEntity, UUID> {

    Optional<SimulationScenarioChangeEntity> findByScenario_Id(UUID scenarioId);
}
