package com.simba.snip.npo.action;

import com.simba.snip.npo.persist.ActionAuditEventEntity;
import com.simba.snip.npo.persist.ActionAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ActionAuditService {

    private final ActionAuditEventRepository repository;

    public ActionAuditService(ActionAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void append(UUID actionId, AuditEventType type, String actor, String details) {
        repository.save(ActionAuditEventEntity.create(
                UUID.randomUUID(),
                actionId,
                type.name(),
                actor,
                Instant.now(),
                details
        ));
    }

    @Transactional(readOnly = true)
    public List<ActionAuditEventEntity> list(UUID actionId) {
        return repository.findByActionIdOrderByOccurredAtAsc(actionId);
    }
}
