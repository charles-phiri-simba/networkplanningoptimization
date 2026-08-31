package com.simba.snip.npo.changeexecution.audit;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionAuditEventEntity;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExecutionAuditService {

    private final NetworkChangeExecutionAuditEventRepository repository;

    public ExecutionAuditService(NetworkChangeExecutionAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void append(UUID executionId, String eventType, String actor, String details) {
        if (executionId == null) {
            return;
        }
        repository.save(NetworkChangeExecutionAuditEventEntity.create(
                UUID.randomUUID(),
                executionId,
                eventType,
                actor == null ? "system" : actor,
                details,
                Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public List<NetworkChangeExecutionAuditEventEntity> list(UUID executionId) {
        return repository.findByExecutionIdOrderByOccurredAtAsc(executionId);
    }
}
