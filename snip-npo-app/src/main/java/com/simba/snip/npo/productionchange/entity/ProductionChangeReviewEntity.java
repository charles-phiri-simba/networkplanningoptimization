package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_change_review")
public class ProductionChangeReviewEntity {

    @Id
    @Column(name = "review_id")
    private UUID reviewId;

    @Column(name = "production_change_id", nullable = false)
    private UUID productionChangeId;

    @Column(name = "reviewer_principal_id", nullable = false, length = 128)
    private String reviewerPrincipalId;

    @Column(nullable = false, length = 16)
    private String decision;

    @Column(name = "reason_codes", length = 1024)
    private String reasonCodes;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "production_fingerprint_at_review", length = 64)
    private String productionFingerprintAtReview;

    public static ProductionChangeReviewEntity create(
            UUID reviewId,
            UUID productionChangeId,
            String reviewerPrincipalId,
            String decision,
            String reasonCodes,
            Instant reviewedAt,
            String productionFingerprintAtReview
    ) {
        ProductionChangeReviewEntity entity = new ProductionChangeReviewEntity();
        entity.reviewId = reviewId;
        entity.productionChangeId = productionChangeId;
        entity.reviewerPrincipalId = reviewerPrincipalId;
        entity.decision = decision;
        entity.reasonCodes = reasonCodes;
        entity.reviewedAt = reviewedAt;
        entity.productionFingerprintAtReview = productionFingerprintAtReview;
        return entity;
    }

    public UUID getReviewId() { return reviewId; }
    public UUID getProductionChangeId() { return productionChangeId; }
    public String getReviewerPrincipalId() { return reviewerPrincipalId; }
    public String getDecision() { return decision; }
    public String getReasonCodes() { return reasonCodes; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getProductionFingerprintAtReview() { return productionFingerprintAtReview; }
}
