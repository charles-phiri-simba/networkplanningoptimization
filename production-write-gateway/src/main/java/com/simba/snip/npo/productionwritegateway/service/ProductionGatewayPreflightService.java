package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.adapter.ExpectedStateGuardStrength;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.entity.ProductionChangeControlEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionLeaseEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.repository.ProductionChangeControlRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionExecutionLeaseRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionNetworkChangeRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionNetworkTargetRepository;
import com.simba.snip.npo.productionwritegateway.transport.EricssonWriteTransport;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Live currentness after consume. Fingerprint and authorization generation are
 * compared to production_network_change (not only the grant row). Target state,
 * profiles, change-control, window, lease fencing, kill switch, and rate limits
 * are read from current durable rows / configuration.
 */
@Service
public class ProductionGatewayPreflightService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionNetworkTargetRepository targetRepository;
    private final ProductionChangeControlRepository controlRepository;
    private final ProductionExecutionLeaseRepository leaseRepository;
    private final ProductionKillSwitchEnforcementService killSwitch;
    private final ProductionGatewayRateLimitEnforcementService rateLimits;
    private final ProductionChangeGatewayProperties properties;
    private final EricssonWriteTransport transport;

    public ProductionGatewayPreflightService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionNetworkTargetRepository targetRepository,
            ProductionChangeControlRepository controlRepository,
            ProductionExecutionLeaseRepository leaseRepository,
            ProductionKillSwitchEnforcementService killSwitch,
            ProductionGatewayRateLimitEnforcementService rateLimits,
            ProductionChangeGatewayProperties properties,
            EricssonWriteTransport transport
    ) {
        this.changeRepository = changeRepository;
        this.targetRepository = targetRepository;
        this.controlRepository = controlRepository;
        this.leaseRepository = leaseRepository;
        this.killSwitch = killSwitch;
        this.rateLimits = rateLimits;
        this.properties = properties;
        this.transport = transport;
    }

    public PreflightSnapshot run(
            ProductionExecutionGrantEntity consumedGrant,
            GrantType grantType,
            UUID attemptId
    ) {
        UUID changeId = consumedGrant.getProductionChangeId();
        UUID grantId = consumedGrant.getGrantId();
        if (!"CONSUMED".equals(consumedGrant.getStatus())) {
            throw deny(ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH, grantId, changeId);
        }
        killSwitch.assertEnabled(grantId, changeId);

        ProductionNetworkChangeEntity change = changeRepository.findById(changeId)
                .orElseThrow(() -> deny(ProductionReasonCode.PRODUCTION_INVALID_REQUEST, grantId, changeId));
        if ("INVALID".equals(change.getAuditChainIntegrity())) {
            throw deny(ProductionReasonCode.PRODUCTION_AUDIT_CHAIN_INVALID, grantId, changeId);
        }
        if (consumedGrant.getProductionFingerprint() == null
                || !consumedGrant.getProductionFingerprint().equals(change.getProductionFingerprint())) {
            throw deny(ProductionReasonCode.PRODUCTION_FINGERPRINT_MISMATCH, grantId, changeId);
        }
        if (consumedGrant.getAuthorizationGeneration() != change.getAuthorizationGeneration()) {
            throw deny(ProductionReasonCode.PRODUCTION_AUTHORIZATION_STALE, grantId, changeId);
        }

        ProductionNetworkTargetEntity target = targetRepository.findById(consumedGrant.getTargetId())
                .orElseThrow(() -> deny(ProductionReasonCode.PRODUCTION_TARGET_NOT_FOUND, grantId, changeId));
        if (!target.isEnabled()) {
            throw deny(ProductionReasonCode.PRODUCTION_TARGET_DISABLED, grantId, changeId);
        }
        if ("SUSPENDED".equals(target.getTargetState())) {
            throw deny(ProductionReasonCode.PRODUCTION_TARGET_SUSPENDED, grantId, changeId);
        }
        if (!"ACTIVE".equals(target.getTargetState())) {
            throw deny(ProductionReasonCode.PRODUCTION_TARGET_NOT_ACTIVE, grantId, changeId);
        }
        if (!properties.getPermittedVendors().contains(target.getVendor())
                || !properties.getPermittedPlatforms().contains(target.getPlatform())) {
            throw deny(ProductionReasonCode.PRODUCTION_VENDOR_UNSUPPORTED, grantId, changeId);
        }
        if (isBlank(target.getAdapterProfileId())
                || isBlank(target.getCapabilityProfileVersion())
                || isBlank(target.getSecurityProfileId())
                || isBlank(target.getCredentialProfileId())) {
            throw deny(ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, grantId, changeId);
        }
        if (target.getSecurityProfileId().toUpperCase(Locale.ROOT).contains("TRUST_ALL")) {
            throw deny(ProductionReasonCode.PRODUCTION_TLS_FAILURE, grantId, changeId);
        }
        if (!properties.getSsl().isHostnameVerification() || properties.getSsl().isTrustAll()) {
            throw deny(ProductionReasonCode.PRODUCTION_TLS_FAILURE, grantId, changeId);
        }

        ProductionChangeControlEntity control = controlRepository
                .findFirstByProductionChangeIdOrderByValidatedAtDesc(changeId)
                .orElseThrow(() -> deny(ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID, grantId, changeId));
        if (!"VALID".equals(control.getStatus())) {
            throw deny(ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID, grantId, changeId);
        }
        if (control.getValidUntil() != null && !control.getValidUntil().isAfter(Instant.now())) {
            throw deny(ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_EXPIRED, grantId, changeId);
        }
        assertChangeWindowOpen(target, grantId, changeId);

        if (!"CELL".equals(target.getAllowedObjectTypes()) && !target.getAllowedObjectTypes().contains("CELL")) {
            throw deny(ProductionReasonCode.PRODUCTION_SCOPE_DENIED, grantId, changeId);
        }
        if (!"txPower".equals(change.getParameter())
                || (target.getAllowedParameters() != null && !target.getAllowedParameters().contains("txPower"))) {
            throw deny(ProductionReasonCode.PRODUCTION_SCOPE_DENIED, grantId, changeId);
        }
        if (properties.getMaximumCellsPerExecution() != 1
                || properties.getMaximumParametersPerExecution() != 1
                || properties.getMaximumOperationsPerExecution() != 1) {
            throw deny(ProductionReasonCode.PRODUCTION_SCOPE_DENIED, grantId, changeId);
        }

        ProductionExecutionLeaseEntity lease = leaseRepository
                .findFirstByProductionTargetIdAndCellIdAndParameterAndStatus(
                        target.getTargetId(), change.getCellId(), change.getParameter(), "ACTIVE")
                .orElseThrow(() -> deny(ProductionReasonCode.PRODUCTION_LEASE_REQUIRED, grantId, changeId));
        if (lease.getFencingToken() != consumedGrant.getFencingToken()) {
            throw deny(ProductionReasonCode.PRODUCTION_FENCING_TOKEN_STALE, grantId, changeId);
        }
        if (!lease.getExpiresAt().isAfter(Instant.now())) {
            throw deny(ProductionReasonCode.PRODUCTION_LEASE_UNAVAILABLE, grantId, changeId);
        }

        ExpectedStateGuardStrength strength = ExpectedStateGuardStrength.valueOf(
                target.getExpectedStateGuardStrength());
        if (strength == ExpectedStateGuardStrength.ATOMIC && !transport.supportsAtomicCompareAndSet()) {
            throw deny(ProductionReasonCode.PRODUCTION_ATOMIC_UNSUPPORTED, grantId, changeId);
        }
        String verificationPolicy = target.getVerificationPolicy() == null ? "" : target.getVerificationPolicy();
        if (strength == ExpectedStateGuardStrength.READ_THEN_WRITE
                && verificationPolicy.toUpperCase(Locale.ROOT).contains("FORBID_READ_THEN_WRITE")) {
            throw deny(ProductionReasonCode.PRODUCTION_POLICY_DENY, grantId, changeId);
        }

        rateLimits.enforce(target.getTargetId(), change.getCellId(), grantId, changeId);

        return new PreflightSnapshot(change, target, grantType, attemptId);
    }

    private void assertChangeWindowOpen(
            ProductionNetworkTargetEntity target,
            UUID grantId,
            UUID changeId
    ) {
        String policy = target.getChangeWindowPolicy();
        if (policy == null || policy.isBlank() || "ALWAYS_OPEN".equalsIgnoreCase(policy)) {
            return;
        }
        if ("CLOSED".equalsIgnoreCase(policy) || policy.toUpperCase(Locale.ROOT).contains("CLOSED")) {
            throw deny(ProductionReasonCode.PRODUCTION_CHANGE_WINDOW_CLOSED, grantId, changeId);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static GatewayDeniedException deny(ProductionReasonCode code, UUID grantId, UUID changeId) {
        return GatewayDeniedException.deny(code, grantId, changeId);
    }

    public record PreflightSnapshot(
            ProductionNetworkChangeEntity change,
            ProductionNetworkTargetEntity target,
            GrantType grantType,
            UUID attemptId
    ) {
    }
}
