package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationResultMetricRepository extends JpaRepository<SimulationResultMetricEntity, UUID> {

    List<SimulationResultMetricEntity> findBySimulationIdOrderByMetricAsc(UUID simulationId);
}
