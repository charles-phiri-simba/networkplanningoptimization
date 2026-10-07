package com.simba.snip.npo.planning.repository;

import com.simba.snip.npo.planning.persist.PlanningAlternativeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanningAlternativeRepository extends JpaRepository<PlanningAlternativeEntity, UUID> {

    List<PlanningAlternativeEntity> findByScenarioIdOrderByOrdinalAsc(UUID scenarioId);
}
