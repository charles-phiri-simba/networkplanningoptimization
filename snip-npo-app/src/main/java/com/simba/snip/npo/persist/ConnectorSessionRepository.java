package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConnectorSessionRepository extends JpaRepository<ConnectorSessionEntity, UUID> {
    List<ConnectorSessionEntity> findByExecutionIdOrderByStartedAtAsc(UUID executionId);
}
