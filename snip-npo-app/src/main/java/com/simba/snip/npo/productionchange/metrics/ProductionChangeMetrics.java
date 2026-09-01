package com.simba.snip.npo.productionchange.metrics;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Micrometer counters/timers. Tags are limited to vendor, result, and reasonCode group.
 * Never tags cellId, grantId, productionChangeId, userId, fingerprint, or endpoint.
 */
@Component
public class ProductionChangeMetrics {

    private static final String VENDOR = "ERICSSON";

    private final MeterRegistry registry;
    private final Timer executeTimer;

    public ProductionChangeMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.executeTimer = Timer.builder("production_execution_duration")
                .tag("vendor", VENDOR)
                .register(registry);
    }

    public void incrementRequests(String result) {
        counter("production_change_requests_total", result, null);
    }

    public void incrementAuthorizations(String result) {
        counter("production_change_authorizations_total", result, null);
    }

    public void incrementAttempts(String result) {
        counter("production_execution_attempts_total", result, null);
    }

    public void incrementVerified() {
        counter("production_execution_verified_total", "VERIFIED", null);
    }

    public void incrementOutcomeUnknown() {
        counter("production_execution_outcome_unknown_total", "OUTCOME_UNKNOWN", null);
    }

    public void incrementVerificationFailures() {
        counter("production_verification_failures_total", "FAILED", null);
    }

    public void incrementRollbacks(String result) {
        counter("production_rollbacks_total", result, null);
    }

    public void incrementManualIntervention() {
        counter("production_manual_intervention_total", "REQUIRED", null);
    }

    public void incrementTargetSuspensions() {
        counter("production_target_suspensions_total", "SUSPENDED", null);
    }

    public void incrementGrantIssuance(String result) {
        counter("production_grant_issuance_total", result, null);
    }

    public void incrementGrantIssuanceDenied(ProductionReasonCode reasonCode) {
        counter("production_grant_issuance_denied_total", "DENIED", group(reasonCode));
    }

    public void incrementGrantConsumeConflicts() {
        counter("production_grant_consume_conflicts_total", "CONFLICT", null);
    }

    public void incrementGrantExpired() {
        counter("production_grant_expired_total", "EXPIRED", null);
    }

    public void incrementGrantRevoked() {
        counter("production_grant_revoked_total", "REVOKED", null);
    }

    public void incrementKillSwitchDenials() {
        counter("production_kill_switch_denials_total", "DENY", null);
    }

    public void incrementRateLimitDenials() {
        counter("production_rate_limit_denials_total", "DENY", null);
    }

    public void incrementAuditChainInvalid() {
        counter("production_audit_chain_invalid_total", "INVALID", null);
    }

    public void recordExecuteDuration(long nanos) {
        executeTimer.record(nanos, TimeUnit.NANOSECONDS);
    }

    private void counter(String name, String result, String reasonCategory) {
        Counter.Builder builder = Counter.builder(name).tag("vendor", VENDOR);
        if (result != null) {
            builder.tag("result", result);
        }
        if (reasonCategory != null) {
            builder.tag("reasonCategory", reasonCategory);
        }
        builder.register(registry).increment();
    }

    private String group(ProductionReasonCode reasonCode) {
        if (reasonCode == null) {
            return "UNKNOWN";
        }
        String name = reasonCode.name();
        if (name.contains("GRANT")) {
            return "GRANT";
        }
        if (name.contains("RATE")) {
            return "RATE_LIMIT";
        }
        if (name.contains("LEASE")) {
            return "LEASE";
        }
        if (name.contains("AUTH")) {
            return "AUTHORIZATION";
        }
        if (name.contains("KILL") || name.contains("DISABLED")) {
            return "KILL_SWITCH";
        }
        return "POLICY";
    }
}
