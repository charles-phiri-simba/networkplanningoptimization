package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangePlanAuditEventRepository extends JpaRepository<NetworkChangePlanAuditEventEntity, UUID> {

    List<NetworkChangePlanAuditEventEntity> findByPlanIdOrderByOccurredAtAsc(UUID planId);
}
