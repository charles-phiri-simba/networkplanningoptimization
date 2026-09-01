package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationLimitationRepository extends JpaRepository<SimulationLimitationEntity, UUID> {

    List<SimulationLimitationEntity> findBySimulationIdOrderByCodeAsc(UUID simulationId);
}
