package com.simba.snip.npo.productioncampaign.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_change_campaign")
public class ProductionChangeCampaignEntity {

    @Id
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "production_target_id", nullable = false, length = 128)
    private String productionTargetId;

    @Column(nullable = false, length = 32)
    private String vendor;

    @Column(nullable = false, length = 32)
    private String platform;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(name = "network_domain", length = 64)
    private String networkDomain;

    @Column(nullable = false, length = 1024)
    private String objective;

    @Column(name = "change_control_reference", nullable = false, length = 256)
    private String changeControlReference;

    @Column(nullable = false, length = 64)
    private String state;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "creator_principal_id", nullable = false, length = 128)
    private String creatorPrincipalId;

    @Column(name = "authorizer_principal_id", length = 128)
    private String authorizerPrincipalId;

    @Column(name = "current_revision_number", nullable = false)
    private int currentRevisionNumber;

    @Column(length = 64)
    private String fingerprint;

    @Column(name = "abort_state", nullable = false, length = 32)
    private String abortState;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public String getProductionTargetId() {
        return productionTargetId;
    }

    public void setProductionTargetId(String productionTargetId) {
        this.productionTargetId = productionTargetId;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getNetworkDomain() {
        return networkDomain;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getChangeControlReference() {
        return changeControlReference;
    }

    public void setChangeControlReference(String changeControlReference) {
        this.changeControlReference = changeControlReference;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreatorPrincipalId() {
        return creatorPrincipalId;
    }

    public void setCreatorPrincipalId(String creatorPrincipalId) {
        this.creatorPrincipalId = creatorPrincipalId;
    }

    public String getAuthorizerPrincipalId() {
        return authorizerPrincipalId;
    }

    public void setAuthorizerPrincipalId(String authorizerPrincipalId) {
        this.authorizerPrincipalId = authorizerPrincipalId;
    }

    public int getCurrentRevisionNumber() {
        return currentRevisionNumber;
    }

    public void setCurrentRevisionNumber(int currentRevisionNumber) {
        this.currentRevisionNumber = currentRevisionNumber;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getAbortState() {
        return abortState;
    }

    public void setAbortState(String abortState) {
        this.abortState = abortState;
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

    public long getVersion() {
        return version;
    }
}
