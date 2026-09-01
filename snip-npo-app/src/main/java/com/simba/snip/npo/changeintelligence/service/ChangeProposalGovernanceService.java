package com.simba.snip.npo.changeintelligence.service;

import com.simba.snip.npo.changeintelligence.ChangeProposalException;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.model.ReviewDecision;
import com.simba.snip.npo.changeintelligence.persist.ChangeProposalReviewEntity;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.repository.ChangeProposalReviewRepository;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ChangeProposalGovernanceService {

    private final NetworkChangeProposalRepository proposalRepository;
    private final ChangeProposalReviewRepository reviewRepository;
    private final ChangeProposalValidityService validityService;
    private final ChangeProposalAuditService auditService;
    private final ChangeProposalMetrics metrics;
    private final Clock clock;

    public ChangeProposalGovernanceService(
            NetworkChangeProposalRepository proposalRepository,
            ChangeProposalReviewRepository reviewRepository,
            ChangeProposalValidityService validityService,
            ChangeProposalAuditService auditService,
            ChangeProposalMetrics metrics,
            Clock clock
    ) {
        this.proposalRepository = proposalRepository;
        this.reviewRepository = reviewRepository;
        this.validityService = validityService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NetworkChangeProposalEntity approve(UUID proposalId, String reviewer, String comment) {
        Instant now = clock.instant();
        requireApprovable(proposalId);
        auditService.append(proposalId, "APPROVAL_ATTEMPTED", reviewer, "approve");
        ChangeProposalValidityService.ValidityResult validity = validityService.revalidate(
                proposalRepository.findById(proposalId).orElseThrow());
        if (!validity.valid()) {
            throw new ChangeProposalException(validity.failureCode(), validity.reason());
        }
        try {
            NetworkChangeProposalEntity proposal = proposalRepository.findById(proposalId).orElseThrow();
            proposal.markApproved(now);
            proposalRepository.save(proposal);
            reviewRepository.save(ChangeProposalReviewEntity.create(
                    UUID.randomUUID(),
                    proposal,
                    ReviewDecision.APPROVED.name(),
                    reviewer,
                    null,
                    comment,
                    proposal.getVersion(),
                    now
            ));
            metrics.incrementApprovals();
            auditService.append(proposalId, "PROPOSAL_APPROVED", reviewer, comment);
            return proposal;
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangeProposalException(
                    ChangeProposalFailureCode.CONCURRENT_REVIEW_CONFLICT, "concurrent review conflict");
        }
    }

    @Transactional
    public NetworkChangeProposalEntity reject(UUID proposalId, String reviewer, String reasonCode, String comment) {
        Instant now = clock.instant();
        NetworkChangeProposalEntity proposal = requireReviewable(proposalId);
        if (!ProposalStatus.RECOMMENDED.name().equals(proposal.getStatus())) {
            throw new ChangeProposalException(ChangeProposalFailureCode.INVALID_PROPOSAL_STATE, proposal.getStatus());
        }
        try {
            proposal.markRejected(now);
            proposalRepository.save(proposal);
            reviewRepository.save(ChangeProposalReviewEntity.create(
                    UUID.randomUUID(),
                    proposal,
                    ReviewDecision.REJECTED.name(),
                    reviewer,
                    reasonCode,
                    comment,
                    proposal.getVersion(),
                    now
            ));
            metrics.incrementRejections();
            auditService.append(proposalId, "PROPOSAL_REJECTED", reviewer, reasonCode);
            return proposal;
        } catch (OptimisticLockingFailureException ex) {
            throw new ChangeProposalException(
                    ChangeProposalFailureCode.CONCURRENT_REVIEW_CONFLICT, "concurrent review conflict");
        }
    }

    private void requireApprovable(UUID proposalId) {
        NetworkChangeProposalEntity proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new DomainNotFoundException("changeProposal", proposalId.toString()));
        if (!ProposalStatus.RECOMMENDED.name().equals(proposal.getStatus())) {
            throw new ChangeProposalException(ChangeProposalFailureCode.INVALID_PROPOSAL_STATE, proposal.getStatus());
        }
    }

    private NetworkChangeProposalEntity requireReviewable(UUID proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new DomainNotFoundException("changeProposal", proposalId.toString()));
    }
}
