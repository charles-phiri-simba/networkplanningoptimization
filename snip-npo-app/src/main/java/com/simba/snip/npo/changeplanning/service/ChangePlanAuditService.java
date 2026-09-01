package com.simba.snip.npo.changeplanning.service;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanAuditEventEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangePlanAuditService {

    private final NetworkChangePlanAuditEventRepository repository;

    public ChangePlanAuditService(NetworkChangePlanAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void append(UUID planId, String eventType, String actor, String details) {
        if (planId == null) {
            return;
        }
        repository.save(NetworkChangePlanAuditEventEntity.create(
                UUID.randomUUID(),
                planId,
                eventType,
                actor == null ? "system" : actor,
                details,
                Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public List<NetworkChangePlanAuditEventEntity> list(UUID planId) {
        return repository.findByPlanIdOrderByOccurredAtAsc(planId);
    }
}
