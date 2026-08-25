package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentCaseMemoryRepository extends JpaRepository<AgentCaseMemoryEntity, UUID> {

    Optional<AgentCaseMemoryEntity> findByRunId(UUID runId);
}
