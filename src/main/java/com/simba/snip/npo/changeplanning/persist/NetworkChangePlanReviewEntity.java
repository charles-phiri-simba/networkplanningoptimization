package com.simba.snip.npo.changeplanning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_change_plan_review")
public class NetworkChangePlanReviewEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(nullable = false, length = 128)
    private String reviewer;

    @Column(length = 512)
    private String comment;

    @Column(name = "plan_version", nullable = false)
    private long planVersion;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    public static NetworkChangePlanReviewEntity create(
            UUID id,
            UUID planId,
            String reviewer,
            String comment,
            long planVersion,
            Instant reviewedAt
    ) {
        NetworkChangePlanReviewEntity entity = new NetworkChangePlanReviewEntity();
        entity.id = id;
        entity.planId = planId;
        entity.reviewer = reviewer;
        entity.comment = comment;
        entity.planVersion = planVersion;
        entity.reviewedAt = reviewedAt;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public String getReviewer() { return reviewer; }
    public String getComment() { return comment; }
    public long getPlanVersion() { return planVersion; }
    public Instant getReviewedAt() { return reviewedAt; }
}
