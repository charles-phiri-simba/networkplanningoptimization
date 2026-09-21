package com.simba.snip.npo.productioncampaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_revision")
public class CampaignRevisionEntity {

    @Id
    @Column(name = "revision_id")
    private UUID revisionId;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "authorization_generation", nullable = false)
    private int authorizationGeneration;

    @Column(nullable = false, length = 64)
    private String state;

    @Column(name = "policy_versions", nullable = false, length = 1024)
    private String policyVersions;

    @Column(length = 1024)
    private String windows;

    @Column(name = "external_ticket_currentness_required", nullable = false)
    private boolean externalTicketCurrentnessRequired;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public int getAuthorizationGeneration() {
        return authorizationGeneration;
    }

    public void setAuthorizationGeneration(int authorizationGeneration) {
        this.authorizationGeneration = authorizationGeneration;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPolicyVersions() {
        return policyVersions;
    }

    public void setPolicyVersions(String policyVersions) {
        this.policyVersions = policyVersions;
    }

    public String getWindows() {
        return windows;
    }

    public void setWindows(String windows) {
        this.windows = windows;
    }

    public boolean isExternalTicketCurrentnessRequired() {
        return externalTicketCurrentnessRequired;
    }

    public void setExternalTicketCurrentnessRequired(boolean externalTicketCurrentnessRequired) {
        this.externalTicketCurrentnessRequired = externalTicketCurrentnessRequired;
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
