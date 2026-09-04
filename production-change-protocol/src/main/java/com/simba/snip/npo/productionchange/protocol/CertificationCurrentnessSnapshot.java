package com.simba.snip.npo.productionchange.protocol;

import java.time.Instant;
import java.util.UUID;

public record CertificationCurrentnessSnapshot(
        String productionTargetId,
        UUID onboardingVersionId,
        UUID bundleVersionId,
        UUID interfaceDefinitionVersionId,
        String interfaceStatus,
        String approvalStatus,
        UUID transportProfileVersionId,
        String artifactDigest,
        UUID capabilityCertVersionId,
        UUID securityCertVersionId,
        UUID credentialProfileVersionId,
        UUID tlsProfileVersionId,
        UUID networkPolicyProfileVersionId,
        UUID endpointProfileVersionId,
        String vendorSoftwareVersion,
        String vendorVersionPredicate,
        String transportHealthState,
        String targetCertificationStatus,
        Instant expiresAt,
        Instant authorityReadAt,
        long authorityRowVersion
) {
}
