package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.ExecutionReadinessAssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExecutionReadinessAssessmentRepository extends JpaRepository<ExecutionReadinessAssessmentEntity, UUID> {

    List<ExecutionReadinessAssessmentEntity> findByPlanIdOrderByAssessedAtAsc(UUID planId);
}
