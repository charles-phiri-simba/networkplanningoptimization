package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionAuditEventRepository extends JpaRepository<ActionAuditEventEntity, UUID> {

    List<ActionAuditEventEntity> findByActionIdOrderByOccurredAtAsc(UUID actionId);
}
