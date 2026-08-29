package com.simba.snip.npo.changeintelligence.service;

import com.simba.snip.npo.changeintelligence.ChangeProposalException;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalAuditEventType;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.persist.ChangeProposalAuditEventEntity;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.repository.ChangeProposalAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeProposalAuditService {

    private final ChangeProposalAuditEventRepository repository;

    public ChangeProposalAuditService(ChangeProposalAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void append(UUID proposalId, String eventType, String actor, String details) {
        if (proposalId == null) {
            return;
        }
        repository.save(ChangeProposalAuditEventEntity.create(
                UUID.randomUUID(),
                proposalId,
                eventType,
                actor == null ? "system" : actor,
                details,
                Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public List<ChangeProposalAuditEventEntity> list(UUID proposalId) {
        return repository.findByProposalIdOrderByOccurredAtAsc(proposalId);
    }
}
