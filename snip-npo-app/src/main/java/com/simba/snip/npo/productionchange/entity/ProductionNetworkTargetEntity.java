package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "production_network_target")
public class ProductionNetworkTargetEntity {

    @Id
    @Column(name = "target_id", length = 128)
    private String targetId;

    @Column(nullable = false, length = 32)
    private String vendor;

    @Column(nullable = false, length = 32)
    private String platform;

    @Column(nullable = false, length = 32)
    private String environment;

    @Column(length = 64)
    private String region;

    @Column(name = "network_domain", length = 64)
    private String networkDomain;

    @Column(name = "adapter_profile_id", nullable = false, length = 64)
    private String adapterProfileId;

    @Column(name = "capability_profile_version", nullable = false, length = 32)
    private String capabilityProfileVersion;

    @Column(name = "security_profile_id", nullable = false, length = 64)
    private String securityProfileId;

    @Column(name = "credential_profile_id", nullable = false, length = 128)
    private String credentialProfileId;

    @Column(name = "allowed_object_types", nullable = false, length = 256)
    private String allowedObjectTypes;

    @Column(name = "allowed_parameters", nullable = false, length = 256)
    private String allowedParameters;

    @Column(name = "change_window_policy", length = 1024)
    private String changeWindowPolicy;

    @Column(name = "rollback_policy", length = 1024)
    private String rollbackPolicy;

    @Column(name = "verification_policy", length = 1024)
    private String verificationPolicy;

    @Column(name = "certification_level", nullable = false, length = 8)
    private String certificationLevel;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "target_state", nullable = false, length = 16)
    private String targetState;

    @Column(name = "target_fingerprint", nullable = false, length = 64)
    private String targetFingerprint;

    @Column(name = "expected_state_guard_strength", nullable = false, length = 32)
    private String expectedStateGuardStrength;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static ProductionNetworkTargetEntity create(
            String targetId,
            String vendor,
            String platform,
            String environment,
            String region,
            String networkDomain,
            String adapterProfileId,
            String capabilityProfileVersion,
            String securityProfileId,
            String credentialProfileId,
            String allowedObjectTypes,
            String allowedParameters,
            String changeWindowPolicy,
            String rollbackPolicy,
            String verificationPolicy,
            String certificationLevel,
            boolean enabled,
            String targetState,
            String targetFingerprint,
            String expectedStateGuardStrength,
            Instant now
    ) {
        ProductionNetworkTargetEntity entity = new ProductionNetworkTargetEntity();
        entity.targetId = targetId;
        entity.vendor = vendor;
        entity.platform = platform;
        entity.environment = environment;
        entity.region = region;
        entity.networkDomain = networkDomain;
        entity.adapterProfileId = adapterProfileId;
        entity.capabilityProfileVersion = capabilityProfileVersion;
        entity.securityProfileId = securityProfileId;
        entity.credentialProfileId = credentialProfileId;
        entity.allowedObjectTypes = allowedObjectTypes;
        entity.allowedParameters = allowedParameters;
        entity.changeWindowPolicy = changeWindowPolicy;
        entity.rollbackPolicy = rollbackPolicy;
        entity.verificationPolicy = verificationPolicy;
        entity.certificationLevel = certificationLevel;
        entity.enabled = enabled;
        entity.targetState = targetState;
        entity.targetFingerprint = targetFingerprint;
        entity.expectedStateGuardStrength = expectedStateGuardStrength;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public String getTargetId() { return targetId; }
    public String getVendor() { return vendor; }
    public String getPlatform() { return platform; }
    public String getEnvironment() { return environment; }
    public String getRegion() { return region; }
    public String getNetworkDomain() { return networkDomain; }
    public String getAdapterProfileId() { return adapterProfileId; }
    public String getCapabilityProfileVersion() { return capabilityProfileVersion; }
    public String getSecurityProfileId() { return securityProfileId; }
    public String getCredentialProfileId() { return credentialProfileId; }
    public String getAllowedObjectTypes() { return allowedObjectTypes; }
    public String getAllowedParameters() { return allowedParameters; }
    public String getChangeWindowPolicy() { return changeWindowPolicy; }
    public String getRollbackPolicy() { return rollbackPolicy; }
    public String getVerificationPolicy() { return verificationPolicy; }
    public String getCertificationLevel() { return certificationLevel; }
    public boolean isEnabled() { return enabled; }
    public String getTargetState() { return targetState; }
    public String getTargetFingerprint() { return targetFingerprint; }
    public String getExpectedStateGuardStrength() { return expectedStateGuardStrength; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public void setAdapterProfileId(String adapterProfileId) { this.adapterProfileId = adapterProfileId; }
    public void setCapabilityProfileVersion(String capabilityProfileVersion) { this.capabilityProfileVersion = capabilityProfileVersion; }
    public void setSecurityProfileId(String securityProfileId) { this.securityProfileId = securityProfileId; }
    public void setCredentialProfileId(String credentialProfileId) { this.credentialProfileId = credentialProfileId; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setTargetState(String targetState) { this.targetState = targetState; }
    public void setTargetFingerprint(String targetFingerprint) { this.targetFingerprint = targetFingerprint; }
    public void setExpectedStateGuardStrength(String expectedStateGuardStrength) { this.expectedStateGuardStrength = expectedStateGuardStrength; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
