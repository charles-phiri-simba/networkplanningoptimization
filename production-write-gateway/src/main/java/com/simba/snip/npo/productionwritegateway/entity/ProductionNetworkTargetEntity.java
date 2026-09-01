package com.simba.snip.npo.productionwritegateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "production_network_target")
public class ProductionNetworkTargetEntity {

    @Id
    @Column(name = "target_id")
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

    @Column(nullable = false)
    private long version;

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
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

    public String getAdapterProfileId() {
        return adapterProfileId;
    }

    public void setAdapterProfileId(String adapterProfileId) {
        this.adapterProfileId = adapterProfileId;
    }

    public String getCapabilityProfileVersion() {
        return capabilityProfileVersion;
    }

    public void setCapabilityProfileVersion(String capabilityProfileVersion) {
        this.capabilityProfileVersion = capabilityProfileVersion;
    }

    public String getSecurityProfileId() {
        return securityProfileId;
    }

    public void setSecurityProfileId(String securityProfileId) {
        this.securityProfileId = securityProfileId;
    }

    public String getCredentialProfileId() {
        return credentialProfileId;
    }

    public void setCredentialProfileId(String credentialProfileId) {
        this.credentialProfileId = credentialProfileId;
    }

    public String getAllowedObjectTypes() {
        return allowedObjectTypes;
    }

    public void setAllowedObjectTypes(String allowedObjectTypes) {
        this.allowedObjectTypes = allowedObjectTypes;
    }

    public String getAllowedParameters() {
        return allowedParameters;
    }

    public void setAllowedParameters(String allowedParameters) {
        this.allowedParameters = allowedParameters;
    }

    public String getChangeWindowPolicy() {
        return changeWindowPolicy;
    }

    public void setChangeWindowPolicy(String changeWindowPolicy) {
        this.changeWindowPolicy = changeWindowPolicy;
    }

    public String getVerificationPolicy() {
        return verificationPolicy;
    }

    public void setVerificationPolicy(String verificationPolicy) {
        this.verificationPolicy = verificationPolicy;
    }

    public String getCertificationLevel() {
        return certificationLevel;
    }

    public void setCertificationLevel(String certificationLevel) {
        this.certificationLevel = certificationLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTargetState() {
        return targetState;
    }

    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }

    public String getTargetFingerprint() {
        return targetFingerprint;
    }

    public void setTargetFingerprint(String targetFingerprint) {
        this.targetFingerprint = targetFingerprint;
    }

    public String getExpectedStateGuardStrength() {
        return expectedStateGuardStrength;
    }

    public void setExpectedStateGuardStrength(String expectedStateGuardStrength) {
        this.expectedStateGuardStrength = expectedStateGuardStrength;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
