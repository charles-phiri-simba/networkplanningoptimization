package com.simba.snip.npo.planning.repository;

import com.simba.snip.npo.planning.persist.PlanningScenarioCellEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanningScenarioCellRepository extends JpaRepository<PlanningScenarioCellEntity, UUID> {

    List<PlanningScenarioCellEntity> findByScenarioIdOrderByOrdinalAsc(UUID scenarioId);

    void deleteByScenarioId(UUID scenarioId);
}
