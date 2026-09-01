package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConnectorSecurityAuditEventRepository extends JpaRepository<ConnectorSecurityAuditEventEntity, UUID> {
    List<ConnectorSecurityAuditEventEntity> findByExecutionIdOrderByOccurredAtAsc(UUID executionId);

    List<ConnectorSecurityAuditEventEntity> findBySessionIdOrderByOccurredAtAsc(UUID sessionId);
}
