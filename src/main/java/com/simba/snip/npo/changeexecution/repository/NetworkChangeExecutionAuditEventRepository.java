package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangeExecutionAuditEventRepository extends JpaRepository<NetworkChangeExecutionAuditEventEntity, UUID> {

    List<NetworkChangeExecutionAuditEventEntity> findByExecutionIdOrderByOccurredAtAsc(UUID executionId);
}
