package com.simba.snip.npo.agent;

import com.simba.snip.npo.persist.AgentRunAuditEventEntity;
import com.simba.snip.npo.persist.AgentRunAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentAuditService {

    private final AgentRunAuditEventRepository repository;

    public AgentAuditService(AgentRunAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void append(UUID runId, AgentRunAuditEventType type, String agentId, String summary) {
        String clipped = summary == null ? "" : summary.replaceAll("\\s+", " ").trim();
        if (clipped.length() > 512) {
            clipped = clipped.substring(0, 512);
        }
        repository.save(AgentRunAuditEventEntity.create(
                UUID.randomUUID(),
                runId,
                type.name(),
                agentId,
                Instant.now(),
                clipped
        ));
    }

    @Transactional(readOnly = true)
    public List<AgentRunAuditEventEntity> list(UUID runId) {
        return repository.findByRunIdOrderByOccurredAtAsc(runId);
    }
}
