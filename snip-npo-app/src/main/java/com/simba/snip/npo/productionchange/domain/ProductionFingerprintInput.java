package com.simba.snip.npo.productionchange.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductionFingerprintInput(
        String adapterProfileId,
        int authorizationGeneration,
        String capabilityProfileVersion,
        String cellId,
        String changeControlReference,
        Instant changeWindowEnd,
        String changeWindowId,
        Instant changeWindowStart,
        String credentialProfileId,
        BigDecimal desiredValue,
        String environment,
        BigDecimal expectedValue,
        String parameter,
        String phase14PlanFingerprint,
        UUID phase14PlanId,
        Integer phase14PlanVersion,
        String phase15ExecutionFingerprint,
        UUID phase15ExecutionId,
        String platform,
        String productionPolicyVersion,
        String productionTargetId,
        BigDecimal rollbackDesiredValue,
        BigDecimal rollbackExpectedValue,
        String rollbackPolicyVersion,
        String securityProfileId,
        String vendor,
        String verificationPolicyVersion
) {
}
