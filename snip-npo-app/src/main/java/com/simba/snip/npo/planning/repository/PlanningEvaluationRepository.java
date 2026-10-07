package com.simba.snip.npo.planning.repository;

import com.simba.snip.npo.planning.persist.PlanningEvaluationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanningEvaluationRepository extends JpaRepository<PlanningEvaluationEntity, UUID> {

    List<PlanningEvaluationEntity> findByScenarioIdOrderByCreatedAtDesc(UUID scenarioId);

    Optional<PlanningEvaluationEntity> findByScenarioIdAndStatus(UUID scenarioId, String status);

    Optional<PlanningEvaluationEntity> findByIdAndScenarioId(UUID id, UUID scenarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PlanningEvaluationEntity e where e.id = :id")
    Optional<PlanningEvaluationEntity> lockById(@Param("id") UUID id);
}
