package com.simba.snip.npo.changeintelligence.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_candidate")
public class NetworkChangeCandidateEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    private NetworkChangeProposalEntity proposal;

    @Column(name = "candidate_value", nullable = false, length = 32)
    private String candidateValue;

    @Column(name = "baseline_candidate", nullable = false)
    private boolean baselineCandidate;

    @Column(name = "validation_outcome", nullable = false, length = 32)
    private String validationOutcome;

    @Column(name = "validation_reason", length = 64)
    private String validationReason;

    @Column(name = "simulation_run_id")
    private UUID simulationRunId;

    @Column(name = "simulation_confidence", length = 16)
    private String simulationConfidence;

    @Column(name = "benefit_score", precision = 12, scale = 6)
    private BigDecimal benefitScore;

    @Column(name = "benefit_reason_codes", length = 512)
    private String benefitReasonCodes;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "risk_reason_codes", length = 512)
    private String riskReasonCodes;

    @Column(name = "proposal_score", precision = 12, scale = 6)
    private BigDecimal proposalScore;

    @Column(name = "rank_order")
    private Integer rankOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static NetworkChangeCandidateEntity create(
            UUID id,
            NetworkChangeProposalEntity proposal,
            String candidateValue,
            boolean baselineCandidate,
            String validationOutcome,
            String validationReason,
            UUID simulationRunId,
            String simulationConfidence,
            BigDecimal benefitScore,
            String benefitReasonCodes,
            String riskLevel,
            String riskReasonCodes,
            BigDecimal proposalScore,
            Integer rankOrder,
            Instant createdAt
    ) {
        NetworkChangeCandidateEntity entity = new NetworkChangeCandidateEntity();
        entity.id = id;
        entity.proposal = proposal;
        entity.candidateValue = candidateValue;
        entity.baselineCandidate = baselineCandidate;
        entity.validationOutcome = validationOutcome;
        entity.validationReason = validationReason;
        entity.simulationRunId = simulationRunId;
        entity.simulationConfidence = simulationConfidence;
        entity.benefitScore = benefitScore;
        entity.benefitReasonCodes = benefitReasonCodes;
        entity.riskLevel = riskLevel;
        entity.riskReasonCodes = riskReasonCodes;
        entity.proposalScore = proposalScore;
        entity.rankOrder = rankOrder;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public NetworkChangeProposalEntity getProposal() {
        return proposal;
    }

    public String getCandidateValue() {
        return candidateValue;
    }

    public boolean isBaselineCandidate() {
        return baselineCandidate;
    }

    public String getValidationOutcome() {
        return validationOutcome;
    }

    public String getValidationReason() {
        return validationReason;
    }

    public UUID getSimulationRunId() {
        return simulationRunId;
    }

    public String getSimulationConfidence() {
        return simulationConfidence;
    }

    public BigDecimal getBenefitScore() {
        return benefitScore;
    }

    public String getBenefitReasonCodes() {
        return benefitReasonCodes;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getRiskReasonCodes() {
        return riskReasonCodes;
    }

    public BigDecimal getProposalScore() {
        return proposalScore;
    }

    public Integer getRankOrder() {
        return rankOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
