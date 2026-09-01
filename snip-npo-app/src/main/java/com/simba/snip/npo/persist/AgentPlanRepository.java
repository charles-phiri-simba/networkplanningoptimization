package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentPlanRepository extends JpaRepository<AgentPlanEntity, UUID> {

    Optional<AgentPlanEntity> findByRunId(UUID runId);
}
