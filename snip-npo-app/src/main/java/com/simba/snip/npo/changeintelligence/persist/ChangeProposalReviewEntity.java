package com.simba.snip.npo.changeintelligence.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "change_proposal_review")
public class ChangeProposalReviewEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    private NetworkChangeProposalEntity proposal;

    @Column(nullable = false, length = 16)
    private String decision;

    @Column(nullable = false, length = 128)
    private String reviewer;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(length = 512)
    private String comment;

    @Column(name = "proposal_version", nullable = false)
    private long proposalVersion;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    public static ChangeProposalReviewEntity create(
            UUID id,
            NetworkChangeProposalEntity proposal,
            String decision,
            String reviewer,
            String reasonCode,
            String comment,
            long proposalVersion,
            Instant reviewedAt
    ) {
        ChangeProposalReviewEntity entity = new ChangeProposalReviewEntity();
        entity.id = id;
        entity.proposal = proposal;
        entity.decision = decision;
        entity.reviewer = reviewer;
        entity.reasonCode = reasonCode;
        entity.comment = comment;
        entity.proposalVersion = proposalVersion;
        entity.reviewedAt = reviewedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public NetworkChangeProposalEntity getProposal() {
        return proposal;
    }

    public String getDecision() {
        return decision;
    }

    public String getReviewer() {
        return reviewer;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getComment() {
        return comment;
    }

    public long getProposalVersion() {
        return proposalVersion;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
