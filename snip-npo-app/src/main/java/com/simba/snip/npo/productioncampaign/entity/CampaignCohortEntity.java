package com.simba.snip.npo.productioncampaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_cohort")
public class CampaignCohortEntity {

    @Id
    @Column(name = "cohort_id")
    private UUID cohortId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "revision_id", nullable = false)
    private UUID revisionId;

    @Column(name = "cohort_sequence", nullable = false)
    private int cohortSequence;

    @Column(name = "cohort_type", nullable = false, length = 16)
    private String cohortType;

    @Column(nullable = false, length = 64)
    private String state;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public UUID getCohortId() {
        return cohortId;
    }

    public void setCohortId(UUID cohortId) {
        this.cohortId = cohortId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public int getCohortSequence() {
        return cohortSequence;
    }

    public void setCohortSequence(int cohortSequence) {
        this.cohortSequence = cohortSequence;
    }

    public String getCohortType() {
        return cohortType;
    }

    public void setCohortType(String cohortType) {
        this.cohortType = cohortType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
