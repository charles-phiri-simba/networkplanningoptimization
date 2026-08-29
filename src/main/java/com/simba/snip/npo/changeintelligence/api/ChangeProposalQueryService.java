package com.simba.snip.npo.changeintelligence.api;

import com.simba.snip.npo.changeintelligence.persist.ChangeProposalAuditEventEntity;
import com.simba.snip.npo.changeintelligence.persist.ChangeProposalReviewEntity;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeCandidateEntity;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.repository.ChangeProposalAuditEventRepository;
import com.simba.snip.npo.changeintelligence.repository.ChangeProposalReviewRepository;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeCandidateRepository;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChangeProposalQueryService {

    private final NetworkChangeProposalRepository proposalRepository;
    private final NetworkChangeCandidateRepository candidateRepository;
    private final ChangeProposalReviewRepository reviewRepository;
    private final ChangeProposalAuditEventRepository auditEventRepository;

    public ChangeProposalQueryService(
            NetworkChangeProposalRepository proposalRepository,
            NetworkChangeCandidateRepository candidateRepository,
            ChangeProposalReviewRepository reviewRepository,
            ChangeProposalAuditEventRepository auditEventRepository
    ) {
        this.proposalRepository = proposalRepository;
        this.candidateRepository = candidateRepository;
        this.reviewRepository = reviewRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ChangeProposalSummaryDto> list() {
        return proposalRepository.findAllByOrderByCreatedAtDesc().stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public ChangeProposalDetailDto require(UUID proposalId) {
        return detail(requireEntity(proposalId));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> evidence(UUID proposalId) {
        NetworkChangeProposalEntity proposal = requireEntity(proposalId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("proposalId", proposal.getId());
        payload.put("assuranceCaseId", proposal.getAssuranceCaseId());
        payload.put("decisionReference", proposal.getDecisionReference());
        payload.put("sourceSystem", proposal.getSourceSystem());
        payload.put("sourceSnapshotId", proposal.getSourceSnapshotId());
        payload.put("networkKnowledgeConfidence", proposal.getNetworkKnowledgeConfidence());
        payload.put("knowledgeReasonCodes", proposal.getKnowledgeReasonCodes());
        payload.put("assuranceConfidence", proposal.getAssuranceConfidence());
        payload.put("simulationConfidence", proposal.getSimulationConfidence());
        payload.put("candidates", candidateRepository.findByProposal_IdOrderByRankOrderAscCandidateValueAsc(proposalId)
                .stream()
                .map(this::candidateDto)
                .toList());
        payload.put("reviews", reviewRepository.findByProposal_IdOrderByReviewedAtAsc(proposalId)
                .stream()
                .map(this::reviewDto)
                .toList());
        payload.put("auditEvents", auditEventRepository.findByProposalIdOrderByOccurredAtAsc(proposalId)
                .stream()
                .map(this::auditDto)
                .toList());
        return payload;
    }

    public NetworkChangeProposalEntity requireEntity(UUID proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new DomainNotFoundException("changeProposal", proposalId.toString()));
    }

    private ChangeProposalDetailDto detail(NetworkChangeProposalEntity proposal) {
        return new ChangeProposalDetailDto(
                summary(proposal),
                candidateRepository.findByProposal_IdOrderByRankOrderAscCandidateValueAsc(proposal.getId())
                        .stream()
                        .map(this::candidateDto)
                        .toList()
        );
    }

    private ChangeProposalSummaryDto summary(NetworkChangeProposalEntity proposal) {
        return new ChangeProposalSummaryDto(
                proposal.getId(),
                proposal.getProposalType(),
                proposal.getStatus(),
                proposal.getTargetEntityType(),
                proposal.getTargetEntityId(),
                proposal.getParameterName(),
                proposal.getCurrentValue(),
                proposal.getProposedValue(),
                proposal.getUnit(),
                proposal.getNetworkKnowledgeConfidence(),
                proposal.getAssuranceConfidence(),
                proposal.getSimulationConfidence(),
                proposal.getRiskLevel(),
                proposal.getBenefitSummary(),
                proposal.getProposalScore(),
                proposal.getFailureCode(),
                proposal.getFailureReason(),
                proposal.getCreatedAt(),
                proposal.getEvaluatedAt(),
                proposal.getExpiresAt(),
                proposal.getInvalidationReason(),
                proposal.getVersion()
        );
    }

    private CandidateEvidenceDto candidateDto(NetworkChangeCandidateEntity candidate) {
        return new CandidateEvidenceDto(
                candidate.getCandidateValue(),
                candidate.isBaselineCandidate(),
                candidate.getValidationOutcome(),
                candidate.getValidationReason(),
                candidate.getSimulationRunId(),
                candidate.getSimulationConfidence(),
                candidate.getBenefitScore(),
                candidate.getRiskLevel(),
                candidate.getProposalScore(),
                candidate.getRankOrder()
        );
    }

    private Map<String, Object> reviewDto(ChangeProposalReviewEntity review) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("decision", review.getDecision());
        dto.put("reviewer", review.getReviewer());
        dto.put("reasonCode", review.getReasonCode());
        dto.put("comment", review.getComment());
        dto.put("proposalVersion", review.getProposalVersion());
        dto.put("reviewedAt", review.getReviewedAt());
        return dto;
    }

    private Map<String, Object> auditDto(ChangeProposalAuditEventEntity event) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("eventType", event.getEventType());
        dto.put("actor", event.getActor());
        dto.put("details", event.getDetails());
        dto.put("occurredAt", event.getOccurredAt());
        return dto;
    }
}
