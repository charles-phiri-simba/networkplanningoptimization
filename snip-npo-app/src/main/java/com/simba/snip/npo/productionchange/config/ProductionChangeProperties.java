package com.simba.snip.npo.productionchange.config;

import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.productionchange.domain.CertificationLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "snip.production-change")
public class ProductionChangeProperties {

    private boolean enabled = false;
    private boolean globalExecutionEnabled = false;
    private int maximumCellsPerExecution = 1;
    private int maximumParametersPerExecution = 1;
    private int maximumOperationsPerExecution = 1;
    private Duration maximumForwardGrantTtl = Duration.ofMinutes(5);
    private Duration maximumRollbackGrantTtl = Duration.ofMinutes(5);
    private Duration minimumGrantTtl = Duration.ofSeconds(60);
    private Duration maximumGrantTtl = Duration.ofMinutes(15);
    private Duration leaseDuration = Duration.ofMinutes(5);
    private int maximumActiveForwardGrantsPerChange = 1;
    private int maximumActiveRollbackGrantsPerChange = 1;
    private int maximumConcurrentIssuedGrantsPerTarget = 10;
    private int maximumChangesPerTargetPerHour = 6;
    private int maximumChangesPerCellPerDay = 3;
    private int maximumGrantsPerActorPerHour = 10;
    private int maximumOutcomeUnknownBeforeSuspend = 3;
    private int maximumVerificationFailuresBeforeSuspend = 3;
    private boolean requireProductionReview = true;
    private boolean requireProductionAuthorization = true;
    private boolean requireChangeControlValidation = true;
    private boolean requireCurrentValueMatch = true;
    private boolean requireIndependentVerification = true;
    private boolean requireRollbackReview = true;
    private boolean requireRollbackAuthorization = true;
    private boolean automaticRollbackEnabled = false;
    private String gatewayBaseUrl = "";
    private List<String> permittedVendors = new ArrayList<>(List.of("ERICSSON"));
    private List<String> permittedPlatforms = new ArrayList<>(List.of("ENM"));
    private CertificationLevel minimumCertificationLevelForExecution = CertificationLevel.L0;
    private String productionPolicyVersion = "p16-v1";
    private String instanceId = "snip-npo-app";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isGlobalExecutionEnabled() {
        return globalExecutionEnabled;
    }

    public void setGlobalExecutionEnabled(boolean globalExecutionEnabled) {
        this.globalExecutionEnabled = globalExecutionEnabled;
    }

    public int getMaximumCellsPerExecution() {
        return maximumCellsPerExecution;
    }

    public void setMaximumCellsPerExecution(int maximumCellsPerExecution) {
        this.maximumCellsPerExecution = maximumCellsPerExecution;
    }

    public int getMaximumParametersPerExecution() {
        return maximumParametersPerExecution;
    }

    public void setMaximumParametersPerExecution(int maximumParametersPerExecution) {
        this.maximumParametersPerExecution = maximumParametersPerExecution;
    }

    public int getMaximumOperationsPerExecution() {
        return maximumOperationsPerExecution;
    }

    public void setMaximumOperationsPerExecution(int maximumOperationsPerExecution) {
        this.maximumOperationsPerExecution = maximumOperationsPerExecution;
    }

    public Duration getMaximumForwardGrantTtl() {
        return maximumForwardGrantTtl;
    }

    public void setMaximumForwardGrantTtl(Duration maximumForwardGrantTtl) {
        this.maximumForwardGrantTtl = maximumForwardGrantTtl;
    }

    public Duration getMaximumRollbackGrantTtl() {
        return maximumRollbackGrantTtl;
    }

    public void setMaximumRollbackGrantTtl(Duration maximumRollbackGrantTtl) {
        this.maximumRollbackGrantTtl = maximumRollbackGrantTtl;
    }

    public Duration getMinimumGrantTtl() {
        return minimumGrantTtl;
    }

    public void setMinimumGrantTtl(Duration minimumGrantTtl) {
        this.minimumGrantTtl = minimumGrantTtl;
    }

    public Duration getMaximumGrantTtl() {
        return maximumGrantTtl;
    }

    public void setMaximumGrantTtl(Duration maximumGrantTtl) {
        this.maximumGrantTtl = maximumGrantTtl;
    }

    public Duration getLeaseDuration() {
        return leaseDuration;
    }

    public void setLeaseDuration(Duration leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    public int getMaximumActiveForwardGrantsPerChange() {
        return maximumActiveForwardGrantsPerChange;
    }

    public void setMaximumActiveForwardGrantsPerChange(int maximumActiveForwardGrantsPerChange) {
        this.maximumActiveForwardGrantsPerChange = maximumActiveForwardGrantsPerChange;
    }

    public int getMaximumActiveRollbackGrantsPerChange() {
        return maximumActiveRollbackGrantsPerChange;
    }

    public void setMaximumActiveRollbackGrantsPerChange(int maximumActiveRollbackGrantsPerChange) {
        this.maximumActiveRollbackGrantsPerChange = maximumActiveRollbackGrantsPerChange;
    }

    public int getMaximumConcurrentIssuedGrantsPerTarget() {
        return maximumConcurrentIssuedGrantsPerTarget;
    }

    public void setMaximumConcurrentIssuedGrantsPerTarget(int maximumConcurrentIssuedGrantsPerTarget) {
        this.maximumConcurrentIssuedGrantsPerTarget = maximumConcurrentIssuedGrantsPerTarget;
    }

    public int getMaximumChangesPerTargetPerHour() {
        return maximumChangesPerTargetPerHour;
    }

    public void setMaximumChangesPerTargetPerHour(int maximumChangesPerTargetPerHour) {
        this.maximumChangesPerTargetPerHour = maximumChangesPerTargetPerHour;
    }

    public int getMaximumChangesPerCellPerDay() {
        return maximumChangesPerCellPerDay;
    }

    public void setMaximumChangesPerCellPerDay(int maximumChangesPerCellPerDay) {
        this.maximumChangesPerCellPerDay = maximumChangesPerCellPerDay;
    }

    public int getMaximumGrantsPerActorPerHour() {
        return maximumGrantsPerActorPerHour;
    }

    public void setMaximumGrantsPerActorPerHour(int maximumGrantsPerActorPerHour) {
        this.maximumGrantsPerActorPerHour = maximumGrantsPerActorPerHour;
    }

    public int getMaximumOutcomeUnknownBeforeSuspend() {
        return maximumOutcomeUnknownBeforeSuspend;
    }

    public void setMaximumOutcomeUnknownBeforeSuspend(int maximumOutcomeUnknownBeforeSuspend) {
        this.maximumOutcomeUnknownBeforeSuspend = maximumOutcomeUnknownBeforeSuspend;
    }

    public int getMaximumVerificationFailuresBeforeSuspend() {
        return maximumVerificationFailuresBeforeSuspend;
    }

    public void setMaximumVerificationFailuresBeforeSuspend(int maximumVerificationFailuresBeforeSuspend) {
        this.maximumVerificationFailuresBeforeSuspend = maximumVerificationFailuresBeforeSuspend;
    }

    public boolean isRequireProductionReview() {
        return requireProductionReview;
    }

    public void setRequireProductionReview(boolean requireProductionReview) {
        this.requireProductionReview = requireProductionReview;
    }

    public boolean isRequireProductionAuthorization() {
        return requireProductionAuthorization;
    }

    public void setRequireProductionAuthorization(boolean requireProductionAuthorization) {
        this.requireProductionAuthorization = requireProductionAuthorization;
    }

    public boolean isRequireChangeControlValidation() {
        return requireChangeControlValidation;
    }

    public void setRequireChangeControlValidation(boolean requireChangeControlValidation) {
        this.requireChangeControlValidation = requireChangeControlValidation;
    }

    public boolean isRequireCurrentValueMatch() {
        return requireCurrentValueMatch;
    }

    public void setRequireCurrentValueMatch(boolean requireCurrentValueMatch) {
        this.requireCurrentValueMatch = requireCurrentValueMatch;
    }

    public boolean isRequireIndependentVerification() {
        return requireIndependentVerification;
    }

    public void setRequireIndependentVerification(boolean requireIndependentVerification) {
        this.requireIndependentVerification = requireIndependentVerification;
    }

    public boolean isRequireRollbackReview() {
        return requireRollbackReview;
    }

    public void setRequireRollbackReview(boolean requireRollbackReview) {
        this.requireRollbackReview = requireRollbackReview;
    }

    public boolean isRequireRollbackAuthorization() {
        return requireRollbackAuthorization;
    }

    public void setRequireRollbackAuthorization(boolean requireRollbackAuthorization) {
        this.requireRollbackAuthorization = requireRollbackAuthorization;
    }

    public boolean isAutomaticRollbackEnabled() {
        return automaticRollbackEnabled;
    }

    public void setAutomaticRollbackEnabled(boolean automaticRollbackEnabled) {
        this.automaticRollbackEnabled = automaticRollbackEnabled;
    }

    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public List<String> getPermittedVendors() {
        return permittedVendors;
    }

    public void setPermittedVendors(List<String> permittedVendors) {
        this.permittedVendors = permittedVendors;
    }

    public List<String> getPermittedPlatforms() {
        return permittedPlatforms;
    }

    public void setPermittedPlatforms(List<String> permittedPlatforms) {
        this.permittedPlatforms = permittedPlatforms;
    }

    public CertificationLevel getMinimumCertificationLevelForExecution() {
        return minimumCertificationLevelForExecution;
    }

    public void setMinimumCertificationLevelForExecution(CertificationLevel minimumCertificationLevelForExecution) {
        this.minimumCertificationLevelForExecution = minimumCertificationLevelForExecution;
    }

    public String getProductionPolicyVersion() {
        return productionPolicyVersion;
    }

    public void setProductionPolicyVersion(String productionPolicyVersion) {
        this.productionPolicyVersion = productionPolicyVersion;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public boolean gatewayUrlConfigured() {
        return gatewayBaseUrl != null && !gatewayBaseUrl.isBlank();
    }

    public void validate() {
        if (maximumCellsPerExecution != 1) {
            throw new DomainValidationException("snip.production-change.maximum-cells-per-execution must equal 1");
        }
        if (maximumParametersPerExecution != 1) {
            throw new DomainValidationException("snip.production-change.maximum-parameters-per-execution must equal 1");
        }
        if (maximumOperationsPerExecution != 1) {
            throw new DomainValidationException("snip.production-change.maximum-operations-per-execution must equal 1");
        }
        if (automaticRollbackEnabled) {
            throw new DomainValidationException("snip.production-change.automatic-rollback-enabled must be false");
        }
        if (!requireProductionReview
                || !requireProductionAuthorization
                || !requireChangeControlValidation
                || !requireCurrentValueMatch
                || !requireIndependentVerification
                || !requireRollbackReview
                || !requireRollbackAuthorization) {
            throw new DomainValidationException("mandatory production-change safety gates cannot be disabled");
        }
        if (maximumActiveForwardGrantsPerChange != 1) {
            throw new DomainValidationException("snip.production-change.maximum-active-forward-grants-per-change must equal 1");
        }
        if (maximumActiveRollbackGrantsPerChange != 1) {
            throw new DomainValidationException("snip.production-change.maximum-active-rollback-grants-per-change must equal 1");
        }
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new DomainValidationException("snip.production-change.lease-duration must be positive");
        }
        if (maximumForwardGrantTtl == null || maximumForwardGrantTtl.compareTo(minimumGrantTtl) < 0) {
            throw new DomainValidationException("snip.production-change.maximum-forward-grant-ttl must be at least the minimum TTL");
        }
        if (maximumForwardGrantTtl.compareTo(maximumGrantTtl) > 0) {
            throw new DomainValidationException("snip.production-change.maximum-forward-grant-ttl exceeds maximum grant TTL");
        }
        if (maximumRollbackGrantTtl == null || maximumRollbackGrantTtl.compareTo(minimumGrantTtl) < 0) {
            throw new DomainValidationException("snip.production-change.maximum-rollback-grant-ttl must be at least the minimum TTL");
        }
        if (maximumRollbackGrantTtl.compareTo(maximumGrantTtl) > 0) {
            throw new DomainValidationException("snip.production-change.maximum-rollback-grant-ttl exceeds maximum grant TTL");
        }
        if (permittedVendors == null || permittedVendors.isEmpty()) {
            throw new DomainValidationException("snip.production-change.permitted-vendors must not be empty");
        }
        if (permittedPlatforms == null || permittedPlatforms.isEmpty()) {
            throw new DomainValidationException("snip.production-change.permitted-platforms must not be empty");
        }
        if (instanceId == null || instanceId.isBlank()) {
            throw new DomainValidationException("snip.production-change.instance-id must not be blank");
        }
        if (minimumCertificationLevelForExecution == null) {
            throw new DomainValidationException("snip.production-change.minimum-certification-level-for-execution is required");
        }
        if (maximumChangesPerTargetPerHour < 0
                || maximumChangesPerCellPerDay < 0
                || maximumGrantsPerActorPerHour < 0) {
            throw new DomainValidationException(
                    "snip.production-change rate-limit maxima must not be negative; zero fails closed");
        }
    }
}
