package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.RuntimeArtifactIdentity;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionwritegateway.transport.UnconfiguredProductionEricssonWriteTransport;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class CertificationSendBoundaryPreflight {

    private static final Set<String> CONTROLLED_NON_PRODUCTION_CLASSES = Set.of(
            "SIMULATOR", "CONTROLLED_SANDBOX", "LAB"
    );

    private final ProductionCertificationAuthority authority;
    private final RuntimeTransportArtifactIdentityProvider identityProvider;
    private final DestinationTrustValidator destinationTrustValidator;
    private final TransportHealthComposer healthComposer;
    private final Phase17CredentialResolutionBinder credentialBinder;
    private final CertifiedTransportResolver transportResolver;
    private final ProductionChangeGatewayProperties properties;
    private final ObservedVendorSessionIdentityProvider observedIdentityProvider;
    private final Phase16AuthorizationCurrentnessReader authorizationReader;

    public CertificationSendBoundaryPreflight(
            ProductionCertificationAuthority authority,
            RuntimeTransportArtifactIdentityProvider identityProvider,
            DestinationTrustValidator destinationTrustValidator,
            TransportHealthComposer healthComposer,
            Phase17CredentialResolutionBinder credentialBinder,
            CertifiedTransportResolver transportResolver,
            ProductionChangeGatewayProperties properties,
            ObservedVendorSessionIdentityProvider observedIdentityProvider,
            Phase16AuthorizationCurrentnessReader authorizationReader
    ) {
        this.authority = authority;
        this.identityProvider = identityProvider;
        this.destinationTrustValidator = destinationTrustValidator;
        this.healthComposer = healthComposer;
        this.credentialBinder = credentialBinder;
        this.transportResolver = transportResolver;
        this.properties = properties;
        this.observedIdentityProvider = observedIdentityProvider;
        this.authorizationReader = authorizationReader;
    }

    public void evaluate(
            ProductionExecutionGrantEntity grant,
            ProductionNetworkChangeEntity change,
            ProductionNetworkTargetEntity target
    ) {
        boolean atomicRequested = target != null
                && "ATOMIC".equals(target.getExpectedStateGuardStrength());
        boolean phase16AuthorizationCurrent = authorizationReader.isCurrent(
                grant.getProductionChangeId(),
                grant.getPhase15ExecutionId(),
                grant.getProductionFingerprint(),
                grant.getAuthorizationGeneration()
        );
        String targetClass = target == null ? null : target.getEnvironment();
        evaluate(
                grant,
                change,
                target == null ? null : target.getTargetState(),
                phase16AuthorizationCurrent,
                atomicRequested,
                targetClass
        );
    }

    public void evaluate(
            ProductionExecutionGrantEntity grant,
            ProductionNetworkChangeEntity change,
            String phase16TargetState,
            boolean phase16AuthorizationCurrent,
            boolean atomicRequested
    ) {
        evaluate(grant, change, phase16TargetState, phase16AuthorizationCurrent, atomicRequested, null);
    }

    public void evaluate(
            ProductionExecutionGrantEntity grant,
            ProductionNetworkChangeEntity change,
            String phase16TargetState,
            boolean phase16AuthorizationCurrent,
            boolean atomicRequested,
            String targetClass
    ) {
        RuntimeArtifactIdentity identity;
        try {
            identity = identityProvider.currentIdentity();
        } catch (RuntimeException ex) {
            throw deny(Phase17DenialCode.P17_ARTIFACT_MISMATCH, "runtime identity missing or malformed");
        }
        if (identity == null) {
            throw deny(Phase17DenialCode.P17_ARTIFACT_MISMATCH, "runtime identity missing");
        }

        Optional<ProductionCertificationAuthority.ResolvedCurrentness> current;
        try {
            current = authority.readCurrent(grant.getTargetId());
        } catch (Phase17AuthorityUnavailableException ex) {
            throw deny(Phase17DenialCode.P17_AUTHORITY_UNAVAILABLE, "durable authority unavailable");
        }

        if (current.isEmpty()) {
            if (allowUncertifiedControlledPath(targetClass)) {
                return;
            }
            throw deny(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN, "no current certification");
        }

        ProductionCertificationAuthority.ResolvedCurrentness resolved = current.get();
        if (!phase16AuthorizationCurrent) {
            throw deny(Phase17DenialCode.P17_LEVEL3_NOT_LEVEL4, "Level4Satisfied false");
        }
        evaluateResolved(resolved, identity, grant, change, phase16TargetState, atomicRequested);
    }

    public boolean level4Satisfied(
            boolean phase16AuthorizationCurrent,
            ProductionCertificationAuthority.ResolvedCurrentness resolved
    ) {
        return phase16AuthorizationCurrent
                && resolved != null
                && "CURRENT".equals(resolved.snapshot().targetCertificationStatus())
                && "ACTIVE".equals(resolved.bundleStatus())
                && "PRODUCTION_REGISTERED".equals(resolved.transportCertState());
    }

    private void evaluateResolved(
            ProductionCertificationAuthority.ResolvedCurrentness resolved,
            RuntimeArtifactIdentity identity,
            ProductionExecutionGrantEntity grant,
            ProductionNetworkChangeEntity change,
            String phase16TargetState,
            boolean atomicRequested
    ) {
        var snapshot = resolved.snapshot();
        if ("WITHDRAWN".equals(resolved.documentationStatus())) {
            throw deny(Phase17DenialCode.P17_DOCUMENTATION_WITHDRAWN, "documentation withdrawn");
        }
        if ("REVOKED".equals(snapshot.interfaceStatus())) {
            throw deny(Phase17DenialCode.P17_INTERFACE_REVOKED, "interface revoked");
        }
        if ("SUPERSEDED".equals(snapshot.interfaceStatus())) {
            throw deny(Phase17DenialCode.P17_INTERFACE_SUPERSEDED, "interface superseded");
        }
        if (!"APPROVED".equals(snapshot.approvalStatus())) {
            throw deny(Phase17DenialCode.P17_APPROVAL_REVOKED, "approval not current");
        }
        if ("REVOKED".equals(resolved.transportCertState())) {
            throw deny(Phase17DenialCode.P17_CERTIFICATION_REVOKED, "certification revoked");
        }
        if ("EXPIRED".equals(resolved.transportCertState())
                || (snapshot.expiresAt() != null && snapshot.expiresAt().isBefore(Instant.now()))) {
            throw deny(Phase17DenialCode.P17_CERTIFICATION_EXPIRED, "certification expired");
        }
        if (!"ACTIVE".equals(resolved.bundleStatus())) {
            throw deny(Phase17DenialCode.P17_BUNDLE_INVALID, "bundle not active");
        }
        if ("SUSPENDED".equals(snapshot.targetCertificationStatus())) {
            throw deny(Phase17DenialCode.P17_TARGET_SUSPENDED, "target suspended");
        }
        if (!"CURRENT".equals(snapshot.targetCertificationStatus())) {
            throw deny(Phase17DenialCode.P17_CERTIFICATION_STALE, "target certification not current");
        }
        if ("WITHDRAWN".equals(snapshot.interfaceStatus()) || "DOCUMENTATION_WITHDRAWN".equals(snapshot.interfaceStatus())) {
            throw deny(Phase17DenialCode.P17_DOCUMENTATION_WITHDRAWN, "documentation withdrawn");
        }
        if (!"APPROVED".equals(resolved.onboardingStatus()) && resolved.onboardingStatus() != null
                && !"CURRENT".equals(resolved.onboardingStatus())) {
            if ("SUSPENDED".equals(resolved.onboardingStatus())) {
                throw deny(Phase17DenialCode.P17_TARGET_SUSPENDED, "onboarding suspended");
            }
            if (!"APPROVED".equals(resolved.onboardingStatus())) {
                throw deny(Phase17DenialCode.P17_TARGET_NOT_ONBOARDED, "target not onboarded");
            }
        }
        if (snapshot.artifactDigest() == null || !snapshot.artifactDigest().equals(identity.artifactDigest())) {
            throw deny(Phase17DenialCode.P17_ARTIFACT_MISMATCH, "certified A / deployed B");
        }
        if (!credentialBinder.matches(grant.getTargetId(), resolved.credentialTargetId(), resolved.credentialStatus())) {
            throw deny(Phase17DenialCode.P17_CREDENTIAL_PROFILE_DENIED, "credential profile not target-bound");
        }
        if (!"ACTIVE".equals(resolved.tlsStatus()) || !resolved.hostnameVerificationRequired()) {
            throw deny(Phase17DenialCode.P17_TLS_IDENTITY_MISMATCH, "TLS profile not eligible");
        }
        if (!"ACTIVE".equals(resolved.networkStatus())) {
            throw deny(Phase17DenialCode.P17_NETWORK_POLICY_INACTIVE, "network profile inactive");
        }
        if (snapshot.vendorSoftwareVersion() == null || snapshot.vendorSoftwareVersion().isBlank()) {
            throw deny(Phase17DenialCode.P17_VENDOR_VERSION_UNKNOWN, "vendor version unknown");
        }
        if (resolved.compatibilityStatus() == null || "SUSPENDED".equals(resolved.compatibilityStatus())
                || "EXPIRED".equals(resolved.compatibilityStatus())
                || "REVOKED".equals(resolved.compatibilityStatus())) {
            throw deny(Phase17DenialCode.P17_VENDOR_VERSION_MISMATCH, "vendor version not certified");
        }
        if (!"CELL".equals(resolved.objectType()) || !"txPower".equals(resolved.parameter())) {
            throw deny(Phase17DenialCode.P17_CAPABILITY_NOT_CERTIFIED, "capability not CELL/txPower");
        }
        if (atomicRequested && !resolved.atomicCertified()) {
            throw deny(Phase17DenialCode.P17_ATOMIC_NOT_CERTIFIED, "ATOMIC not certified");
        }
        Phase17DenialCode health = healthComposer.blockingReason(
                phase16TargetState, snapshot.transportHealthState(), snapshot.targetCertificationStatus());
        if (health != null) {
            throw deny(health, "health composition deny");
        }
        Optional<DestinationTrustValidator.ObservedDestination> observed =
                observedIdentityProvider == null
                        ? Optional.empty()
                        : observedIdentityProvider.currentObserved();
        if (observed.isEmpty() || observed.get() == null) {
            throw deny(Phase17DenialCode.P17_ENDPOINT_IDENTITY_MISMATCH, "observed destination missing");
        }
        DestinationTrustValidator.Mismatch mismatch = destinationTrustValidator.compare(resolved, observed.get());
        if (mismatch == DestinationTrustValidator.Mismatch.MISSING
                || mismatch == DestinationTrustValidator.Mismatch.FQDN
                || mismatch == DestinationTrustValidator.Mismatch.PORT) {
            throw deny(Phase17DenialCode.P17_ENDPOINT_IDENTITY_MISMATCH, mismatch.name());
        }
        if (mismatch == DestinationTrustValidator.Mismatch.TLS_IDENTITY
                || mismatch == DestinationTrustValidator.Mismatch.HOSTNAME
                || mismatch == DestinationTrustValidator.Mismatch.TRUST_CHAIN) {
            throw deny(Phase17DenialCode.P17_TLS_IDENTITY_MISMATCH, mismatch.name());
        }
        if (mismatch == DestinationTrustValidator.Mismatch.NETWORK_PROFILE) {
            throw deny(Phase17DenialCode.P17_NETWORK_POLICY_INACTIVE, mismatch.name());
        }
        if (!level4Satisfied(true, resolved)) {
            throw deny(Phase17DenialCode.P17_LEVEL4_NOT_CURRENT, "Level4Satisfied false for execution");
        }
        if (transportResolver.isUnconfiguredProduction()
                && transportResolver.resolveProduction() instanceof UnconfiguredProductionEricssonWriteTransport
                && properties.isProductionRuntime()) {
            throw deny(Phase17DenialCode.P17_INTERFACE_UNRESOLVED, "production transport unconfigured");
        }
    }

    boolean allowUncertifiedControlledPath(String targetClass) {
        if (!properties.isTestTransportEnabled() || properties.isProductionRuntime()) {
            return false;
        }
        if (targetClass == null || targetClass.isBlank()) {
            return false;
        }
        String normalized = targetClass.strip().toUpperCase(Locale.ROOT);
        if ("PRODUCTION".equals(normalized) || "PROD".equals(normalized)
                || "PREPROD".equals(normalized) || "UNKNOWN".equals(normalized)) {
            return false;
        }
        return CONTROLLED_NON_PRODUCTION_CLASSES.contains(normalized);
    }

    private static Phase17SendDeniedException deny(Phase17DenialCode code, String message) {
        return new Phase17SendDeniedException(code, message);
    }
}
