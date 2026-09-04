package com.simba.snip.npo.vendorcertification.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class Phase17CertificationMetrics {

    private final MeterRegistry registry;

    public Phase17CertificationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incrementRevocation(String trigger) {
        registry.counter("snip.p17.certification.revocation", "trigger", sanitize(trigger)).increment();
    }

    public void incrementDeny(String code) {
        registry.counter("snip.p17.gateway.cert.deny", "code", sanitize(code)).increment();
    }

    public void incrementArtifactMismatch() {
        registry.counter("snip.p17.artifact.mismatch").increment();
    }

    public void incrementTlsFailure() {
        registry.counter("snip.p17.tls.failure").increment();
    }

    public void incrementEndpointMismatch() {
        registry.counter("snip.p17.endpoint.mismatch").increment();
    }

    public void incrementVendorVersionMismatch() {
        registry.counter("snip.p17.vendor.version.mismatch").increment();
    }

    public void incrementCredentialFailure() {
        registry.counter("snip.p17.credential.profile.failure").increment();
    }

    public void incrementOutcomeUnknown() {
        registry.counter("snip.p17.outcome.unknown").increment();
    }

    public void incrementVerificationMismatch() {
        registry.counter("snip.p17.verification.mismatch").increment();
    }

    public void incrementRollbackFailure() {
        registry.counter("snip.p17.rollback.failure").increment();
    }

    public void incrementExpiry() {
        registry.counter("snip.p17.certification.expiry").increment();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replaceAll("[^A-Z0-9_]", "_");
    }
}
