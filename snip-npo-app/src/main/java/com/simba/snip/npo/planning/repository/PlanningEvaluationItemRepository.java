package com.simba.snip.npo.planning.repository;

import com.simba.snip.npo.planning.persist.PlanningEvaluationItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanningEvaluationItemRepository extends JpaRepository<PlanningEvaluationItemEntity, UUID> {

    List<PlanningEvaluationItemEntity> findByEvaluationId(UUID evaluationId);

    Optional<PlanningEvaluationItemEntity> findByEvaluationIdAndAlternativeIdAndCellId(
            UUID evaluationId,
            UUID alternativeId,
            String cellId
    );

    List<PlanningEvaluationItemEntity> findByAlternativeIdIn(Collection<UUID> alternativeIds);

    @Query("""
            select i from PlanningEvaluationItemEntity i, PlanningEvaluationEntity e
            where i.evaluationId = e.id
              and e.scenarioId = :scenarioId
              and i.alternativeId = :alternativeId
              and i.cellId = :cellId
              and i.parameterId = :parameterId
              and i.intendedValue = :intendedValue
              and i.twinId = :twinId
              and i.twinVersion = :twinVersion
              and i.pinnedBaselineTxPower = :baseline
              and (
                    (:configurationFingerprint is null and i.configurationFingerprint is null)
                    or i.configurationFingerprint = :configurationFingerprint
              )
              and i.modelId = :modelId
              and i.modelVersion = :modelVersion
              and i.outcome = 'SUCCEEDED'
              and i.simulationRunId is not null
            order by e.createdAt desc
            """)
    List<PlanningEvaluationItemEntity> findReusableSucceeded(
            @Param("scenarioId") UUID scenarioId,
            @Param("alternativeId") UUID alternativeId,
            @Param("cellId") String cellId,
            @Param("parameterId") String parameterId,
            @Param("intendedValue") BigDecimal intendedValue,
            @Param("twinId") UUID twinId,
            @Param("twinVersion") int twinVersion,
            @Param("baseline") BigDecimal baseline,
            @Param("configurationFingerprint") String configurationFingerprint,
            @Param("modelId") String modelId,
            @Param("modelVersion") String modelVersion
    );
}
