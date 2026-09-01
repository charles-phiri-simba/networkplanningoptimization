package com.simba.snip.npo.changeintelligence.service;

import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChangeProposalInvalidationPersistenceService {

    private final NetworkChangeProposalRepository proposalRepository;
    private final ChangeProposalAuditService auditService;
    private final ChangeProposalMetrics metrics;

    public ChangeProposalInvalidationPersistenceService(
            NetworkChangeProposalRepository proposalRepository,
            ChangeProposalAuditService auditService,
            ChangeProposalMetrics metrics
    ) {
        this.proposalRepository = proposalRepository;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistInvalidation(
            UUID proposalId,
            String invalidationReason,
            Instant invalidatedAt,
            String auditDetail,
            String knowledgeConfidence,
            String knowledgeReasonCodes
    ) {
        NetworkChangeProposalEntity proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new DomainNotFoundException("changeProposal", proposalId.toString()));
        if (ProposalStatus.INVALIDATED.name().equals(proposal.getStatus())) {
            return;
        }
        proposal.markInvalidated(invalidationReason, invalidatedAt);
        if (knowledgeConfidence != null) {
            proposal.refreshKnowledgeSnapshot(knowledgeConfidence, knowledgeReasonCodes);
        }
        proposalRepository.save(proposal);
        auditService.append(proposalId, "PROPOSAL_INVALIDATED", "system", auditDetail);
        metrics.incrementInvalidations();
    }
}
