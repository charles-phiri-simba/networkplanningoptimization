package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.CertificationCurrentnessSnapshot;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.productionchange.protocol.RuntimeArtifactIdentity;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionwritegateway.transport.UnconfiguredProductionEricssonWriteTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CertificationSendBoundaryPreflightTest {

    private static final String DIGEST = "40680c7f2f9d70952b419abe1b4c9c943bab42c86cb0361e15ae5efc84f87b6d";
    private static final String BASELINE = "77fd24c0fd32c920c97ff5169f4bc8a93a77b208";

    private ProductionCertificationAuthority authority;
    private RuntimeTransportArtifactIdentityProvider identityProvider;
    private CertificationSendBoundaryPreflight preflight;
    private ProductionChangeGatewayProperties properties;
    private ObservedVendorSessionIdentityProvider observedIdentity;
    private Phase16AuthorizationCurrentnessReader authorizationReader;
    private ProductionExecutionGrantEntity grant;
    private ProductionNetworkChangeEntity change;

    @BeforeEach
    void setup() {
        authority = mock(ProductionCertificationAuthority.class);
        identityProvider = () -> new RuntimeArtifactIdentity(DIGEST, "unconfigured-0", BASELINE, null);
        properties = new ProductionChangeGatewayProperties();
        properties.setTestTransportEnabled(false);
        properties.setProductionRuntime(true);
        observedIdentity = mock(ObservedVendorSessionIdentityProvider.class);
        when(observedIdentity.currentObserved()).thenReturn(Optional.empty());
        authorizationReader = mock(Phase16AuthorizationCurrentnessReader.class);
        when(authorizationReader.isCurrent(any(), any(), any(), anyInt())).thenReturn(true);
        preflight = new CertificationSendBoundaryPreflight(
                authority,
                identityProvider,
                new DestinationTrustValidator(),
                new TransportHealthComposer(),
                new Phase17CredentialResolutionBinder(),
                new CertifiedTransportResolver(new UnconfiguredProductionEricssonWriteTransport()),
                properties,
                observedIdentity,
                authorizationReader
        );
        grant = mock(ProductionExecutionGrantEntity.class);
        when(grant.getTargetId()).thenReturn("target-1");
        when(grant.getProductionChangeId()).thenReturn(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(grant.getPhase15ExecutionId()).thenReturn(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        when(grant.getProductionFingerprint()).thenReturn(DIGEST);
        when(grant.getAuthorizationGeneration()).thenReturn(1);
        change = mock(ProductionNetworkChangeEntity.class);
    }

    @Test
    void cs17ALevel3NotLevel4() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", false, false));
        assertEquals(Phase17DenialCode.P17_LEVEL3_NOT_LEVEL4, ex.denialCode());
    }

    @Test
    void cs17BCertificationRevoked() {
        ProductionCertificationAuthority.ResolvedCurrentness resolved = eligible();
        resolved = withTransportState(resolved, "REVOKED");
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(resolved));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_REVOKED, ex.denialCode());
    }

    @Test
    void cs17HAuthorityUnavailable() {
        when(authority.readCurrent("target-1")).thenThrow(new Phase17AuthorityUnavailableException("down", null));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_AUTHORITY_UNAVAILABLE, ex.denialCode());
    }

    @Test
    void cs17XArtifactMismatch() {
        ProductionCertificationAuthority.ResolvedCurrentness resolved = withArtifact(eligible(),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(resolved));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_ARTIFACT_MISMATCH, ex.denialCode());
    }

    @Test
    void cs17GTargetSuspended() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withTargetStatus(eligible(), "SUSPENDED")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_TARGET_SUSPENDED, ex.denialCode());
    }

    @Test
    void cs17VExpired() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withTransportState(eligible(), "EXPIRED")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_EXPIRED, ex.denialCode());
    }

    @Test
    void cs17LCapability() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withCapability(eligible(), "NODE", "retT")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_CAPABILITY_NOT_CERTIFIED, ex.denialCode());
    }

    @Test
    void cs17MAtomicNotCertified() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, true));
        assertEquals(Phase17DenialCode.P17_ATOMIC_NOT_CERTIFIED, ex.denialCode());
    }

    @Test
    void cs17KVendorVersion() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withCompatibility(eligible(), "SUSPENDED")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_VENDOR_VERSION_MISMATCH, ex.denialCode());
    }

    @Test
    void cs17YHealthBlocking() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withHealth(eligible(), "DEGRADED")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_HEALTH_BLOCKING, ex.denialCode());
    }

    @Test
    void cs17DEndpointMismatch() {
        when(observedIdentity.currentObserved()).thenReturn(Optional.of(
                TestDestinationIdentityDouble.wrongFqdn("evil.example", 443, "enm.example.invalid", "LAB", "zone-a")));
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB"));
        assertEquals(Phase17DenialCode.P17_ENDPOINT_IDENTITY_MISMATCH, ex.denialCode());
    }

    @Test
    void cs17EFi17_011ArtifactMismatch() {
        ProductionCertificationAuthority.ResolvedCurrentness resolved = withArtifact(eligible(),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(resolved));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_ARTIFACT_MISMATCH, ex.denialCode());
    }

    @Test
    void cs17FCredentialDenied() {
        ProductionCertificationAuthority.ResolvedCurrentness resolved = eligible();
        resolved = new ProductionCertificationAuthority.ResolvedCurrentness(
                resolved.snapshot(), resolved.bundleStatus(), resolved.transportCertState(),
                resolved.approvedFqdn(), resolved.approvedPort(), resolved.tlsServerIdentity(),
                resolved.networkDomain(), resolved.routeZoneId(), resolved.endpointVendor(),
                resolved.endpointPlatform(), resolved.endpointVersionNo(),
                resolved.hostnameVerificationRequired(), resolved.tlsStatus(), resolved.networkStatus(),
                "other-target", resolved.credentialStatus(), resolved.objectType(), resolved.parameter(),
                resolved.atomicCertified(), resolved.expectedStateStrategy(), resolved.transportProfileStatus(),
                resolved.compatibilityStatus(), resolved.onboardingStatus(), resolved.documentationStatus());
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(resolved));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_CREDENTIAL_PROFILE_DENIED, ex.denialCode());
    }

    @Test
    void fi17_013BundleInvalid() {
        ProductionCertificationAuthority.ResolvedCurrentness resolved = copy(
                eligible(), eligible().snapshot(), "INVALID", eligible().transportCertState(),
                eligible().snapshot().targetCertificationStatus(), eligible().snapshot().artifactDigest(),
                eligible().objectType(), eligible().parameter(), eligible().compatibilityStatus(),
                eligible().snapshot().transportHealthState(), eligible().snapshot().interfaceStatus());
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(resolved));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_BUNDLE_INVALID, ex.denialCode());
    }

    @Test
    void t17Sec015NetworkProfileInactive() {
        ProductionCertificationAuthority.ResolvedCurrentness resolved = withNetworkStatus(eligible(), "INACTIVE");
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(resolved));
        when(observedIdentity.currentObserved()).thenReturn(Optional.of(
                new DestinationTrustValidator.ObservedDestination(
                        "enm.example.invalid", 443, "enm.example.invalid", true, true, "LAB", "zone-a")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_NETWORK_POLICY_INACTIVE, ex.denialCode());
    }

    @Test
    void fi17_008TlsMismatch() {
        when(observedIdentity.currentObserved()).thenReturn(Optional.of(
                new DestinationTrustValidator.ObservedDestination(
                        "enm.example.invalid", 443, "other", true, true, "LAB", "zone-a")));
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB"));
        assertEquals(Phase17DenialCode.P17_TLS_IDENTITY_MISMATCH, ex.denialCode());
    }

    @Test
    void cs17CInterfaceRevoked() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withInterface(eligible(), "REVOKED")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_INTERFACE_REVOKED, ex.denialCode());
    }

    @Test
    void fi17UnknownCertOnProductionRuntime() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.empty());
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN, ex.denialCode());
    }

    @Test
    void uncertifiedTestTransportAllowedForLab() {
        properties.setTestTransportEnabled(true);
        properties.setProductionRuntime(false);
        when(authority.readCurrent("target-1")).thenReturn(Optional.empty());
        preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB");
    }

    @Test
    void c17i09SimulatorAndSandboxAllowUncertifiedBypass() {
        properties.setTestTransportEnabled(true);
        properties.setProductionRuntime(false);
        when(authority.readCurrent("target-1")).thenReturn(Optional.empty());
        preflight.evaluate(grant, change, "ACTIVE", true, false, "SIMULATOR");
        preflight.evaluate(grant, change, "ACTIVE", true, false, "CONTROLLED_SANDBOX");
    }

    @Test
    void c17i09PreprodAndProductionRuntimeDenyBypass() {
        properties.setTestTransportEnabled(true);
        properties.setProductionRuntime(false);
        when(authority.readCurrent("target-1")).thenReturn(Optional.empty());
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN,
                assertThrows(Phase17SendDeniedException.class,
                        () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "PREPROD")).denialCode());
        properties.setProductionRuntime(true);
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN,
                assertThrows(Phase17SendDeniedException.class,
                        () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB")).denialCode());
        properties.setProductionRuntime(false);
        properties.setTestTransportEnabled(false);
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN,
                assertThrows(Phase17SendDeniedException.class,
                        () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB")).denialCode());
    }

    @Test
    void fi17_015Phase16AuthorizationRevokedDeniesBeforeSend() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        when(authorizationReader.isCurrent(any(), any(), any(), anyInt())).thenReturn(false);
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, mockTarget("LAB", "READ_THEN_WRITE")));
        assertEquals(Phase17DenialCode.P17_LEVEL3_NOT_LEVEL4, ex.denialCode());
    }

    @Test
    void c17i06AtomicRequestedFromTargetDeniesWhenUncertified() {
        properties.setProductionRuntime(false);
        when(observedIdentity.currentObserved()).thenReturn(Optional.of(
                TestDestinationIdentityDouble.approved("enm.example.invalid", 443, "enm.example.invalid", "LAB", "zone-a")));
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        when(authorizationReader.isCurrent(any(), any(), any(), anyInt())).thenReturn(true);
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, mockTarget("LAB", "ATOMIC")));
        assertEquals(Phase17DenialCode.P17_ATOMIC_NOT_CERTIFIED, ex.denialCode());
    }

    @Test
    void c17i09ProductionTargetClassDeniesUncertifiedBypass() {
        properties.setTestTransportEnabled(true);
        properties.setProductionRuntime(false);
        when(authority.readCurrent("target-1")).thenReturn(Optional.empty());
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "PRODUCTION"));
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN, ex.denialCode());
    }

    @Test
    void c17i09UnknownAndNullTargetClassDenyBypass() {
        properties.setTestTransportEnabled(true);
        properties.setProductionRuntime(false);
        when(authority.readCurrent("target-1")).thenReturn(Optional.empty());
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN,
                assertThrows(Phase17SendDeniedException.class,
                        () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "UNKNOWN")).denialCode());
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_UNKNOWN,
                assertThrows(Phase17SendDeniedException.class,
                        () -> preflight.evaluate(grant, change, "ACTIVE", true, false, null)).denialCode());
    }

    @Test
    void c17i08MissingObservedDestinationDenies() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB"));
        assertEquals(Phase17DenialCode.P17_ENDPOINT_IDENTITY_MISMATCH, ex.denialCode());
    }

    @Test
    void c17i06Phase16AuthorizationCurrentAllowsWhenObservedMatches() {
        properties.setProductionRuntime(false);
        when(observedIdentity.currentObserved()).thenReturn(Optional.of(
                TestDestinationIdentityDouble.approved("enm.example.invalid", 443, "enm.example.invalid", "LAB", "zone-a")));
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB");
    }

    @Test
    void c17i06Phase16AuthorizationRevokedDeniesBeforeSend() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", false, false, "LAB"));
        assertEquals(Phase17DenialCode.P17_LEVEL3_NOT_LEVEL4, ex.denialCode());
    }

    @Test
    void cs17WInterfaceSuperseded() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withInterface(eligible(), "SUPERSEDED")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_INTERFACE_SUPERSEDED, ex.denialCode());
    }

    @Test
    void cs17ZDocumentationWithdrawn() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withDocumentation(eligible(), "WITHDRAWN")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_DOCUMENTATION_WITHDRAWN, ex.denialCode());
    }

    @Test
    void cs17IDurableRevokedWinsOverStalePositive() {
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(withTransportState(eligible(), "REVOKED")));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_CERTIFICATION_REVOKED, ex.denialCode());
    }

    @Test
    void cs17JFi17_012PartitionDeniesAuthorityUnavailable() {
        when(authority.readCurrent("target-1")).thenThrow(new Phase17AuthorityUnavailableException("partition", null));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_AUTHORITY_UNAVAILABLE, ex.denialCode());
    }

    @Test
    void fi17_002AuthorityTimeout() {
        when(authority.readCurrent("target-1")).thenThrow(new Phase17AuthorityUnavailableException("timeout", null));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false));
        assertEquals(Phase17DenialCode.P17_AUTHORITY_UNAVAILABLE, ex.denialCode());
    }

    @Test
    void destinationFqdnMismatchDenies() {
        when(observedIdentity.currentObserved()).thenReturn(Optional.of(
                TestDestinationIdentityDouble.wrongFqdn("evil.example", 443, "enm.example.invalid", "LAB", "zone-a")));
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB"));
        assertEquals(Phase17DenialCode.P17_ENDPOINT_IDENTITY_MISMATCH, ex.denialCode());
    }

    @Test
    void destinationNullObservedFqdnDenies() {
        when(observedIdentity.currentObserved()).thenReturn(Optional.of(
                new DestinationTrustValidator.ObservedDestination(null, 443, "enm.example.invalid", true, true, "LAB", "zone-a")));
        when(authority.readCurrent("target-1")).thenReturn(Optional.of(eligible()));
        Phase17SendDeniedException ex = assertThrows(Phase17SendDeniedException.class,
                () -> preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB"));
        assertEquals(Phase17DenialCode.P17_ENDPOINT_IDENTITY_MISMATCH, ex.denialCode());
    }

    @Test
    void uncertifiedTestTransportAllowed() {
        properties.setTestTransportEnabled(true);
        properties.setProductionRuntime(false);
        when(authority.readCurrent("target-1")).thenReturn(Optional.empty());
        preflight.evaluate(grant, change, "ACTIVE", true, false, "LAB");
    }

    private ProductionCertificationAuthority.ResolvedCurrentness eligible() {
        CertificationCurrentnessSnapshot snapshot = new CertificationCurrentnessSnapshot(
                "target-1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "INTERFACE_VERIFIED", "APPROVED", UUID.randomUUID(), DIGEST,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "ENM-22", "EXPLICIT:ENM-22",
                "HEALTHY", "CURRENT", Instant.now().plusSeconds(3600), Instant.now(), 1L
        );
        return new ProductionCertificationAuthority.ResolvedCurrentness(
                snapshot, "ACTIVE", "PRODUCTION_REGISTERED",
                "enm.example.invalid", 443, "enm.example.invalid", "LAB", "zone-a",
                "ERICSSON", "ENM", 1, true, "ACTIVE", "ACTIVE", "target-1", "ACTIVE",
                "CELL", "txPower", false, "READ_THEN_WRITE", "ACTIVE", "ACTIVE", "APPROVED", "ACTIVE"
        );
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withTransportState(
            ProductionCertificationAuthority.ResolvedCurrentness base, String state
    ) {
        return copy(base, base.snapshot(), base.bundleStatus(), state, base.snapshot().targetCertificationStatus(),
                base.snapshot().artifactDigest(), base.objectType(), base.parameter(),
                base.compatibilityStatus(), base.snapshot().transportHealthState(), base.snapshot().interfaceStatus());
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withTargetStatus(
            ProductionCertificationAuthority.ResolvedCurrentness base, String status
    ) {
        return copy(base, base.snapshot(), base.bundleStatus(), base.transportCertState(), status,
                base.snapshot().artifactDigest(), base.objectType(), base.parameter(),
                base.compatibilityStatus(), base.snapshot().transportHealthState(), base.snapshot().interfaceStatus());
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withArtifact(
            ProductionCertificationAuthority.ResolvedCurrentness base, String digest
    ) {
        return copy(base, base.snapshot(), base.bundleStatus(), base.transportCertState(),
                base.snapshot().targetCertificationStatus(), digest, base.objectType(), base.parameter(),
                base.compatibilityStatus(), base.snapshot().transportHealthState(), base.snapshot().interfaceStatus());
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withCapability(
            ProductionCertificationAuthority.ResolvedCurrentness base, String object, String parameter
    ) {
        return copy(base, base.snapshot(), base.bundleStatus(), base.transportCertState(),
                base.snapshot().targetCertificationStatus(), base.snapshot().artifactDigest(), object, parameter,
                base.compatibilityStatus(), base.snapshot().transportHealthState(), base.snapshot().interfaceStatus());
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withCompatibility(
            ProductionCertificationAuthority.ResolvedCurrentness base, String compatibility
    ) {
        return copy(base, base.snapshot(), base.bundleStatus(), base.transportCertState(),
                base.snapshot().targetCertificationStatus(), base.snapshot().artifactDigest(),
                base.objectType(), base.parameter(), compatibility, base.snapshot().transportHealthState(),
                base.snapshot().interfaceStatus());
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withHealth(
            ProductionCertificationAuthority.ResolvedCurrentness base, String health
    ) {
        return copy(base, base.snapshot(), base.bundleStatus(), base.transportCertState(),
                base.snapshot().targetCertificationStatus(), base.snapshot().artifactDigest(),
                base.objectType(), base.parameter(), base.compatibilityStatus(), health,
                base.snapshot().interfaceStatus());
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withInterface(
            ProductionCertificationAuthority.ResolvedCurrentness base, String iface
    ) {
        return copy(base, base.snapshot(), base.bundleStatus(), base.transportCertState(),
                base.snapshot().targetCertificationStatus(), base.snapshot().artifactDigest(),
                base.objectType(), base.parameter(), base.compatibilityStatus(),
                base.snapshot().transportHealthState(), iface);
    }

    private ProductionCertificationAuthority.ResolvedCurrentness copy(
            ProductionCertificationAuthority.ResolvedCurrentness base,
            CertificationCurrentnessSnapshot snapshot,
            String bundle,
            String transportState,
            String targetStatus,
            String artifact,
            String objectType,
            String parameter,
            String compatibility,
            String health,
            String iface
    ) {
        CertificationCurrentnessSnapshot next = new CertificationCurrentnessSnapshot(
                snapshot.productionTargetId(), snapshot.onboardingVersionId(), snapshot.bundleVersionId(),
                snapshot.interfaceDefinitionVersionId(), iface, snapshot.approvalStatus(),
                snapshot.transportProfileVersionId(), artifact, snapshot.capabilityCertVersionId(),
                snapshot.securityCertVersionId(), snapshot.credentialProfileVersionId(),
                snapshot.tlsProfileVersionId(), snapshot.networkPolicyProfileVersionId(),
                snapshot.endpointProfileVersionId(), snapshot.vendorSoftwareVersion(),
                snapshot.vendorVersionPredicate(), health, targetStatus, snapshot.expiresAt(),
                snapshot.authorityReadAt(), snapshot.authorityRowVersion()
        );
        return new ProductionCertificationAuthority.ResolvedCurrentness(
                next, bundle, transportState, base.approvedFqdn(), base.approvedPort(),
                base.tlsServerIdentity(), base.networkDomain(), base.routeZoneId(),
                base.endpointVendor(), base.endpointPlatform(), base.endpointVersionNo(),
                base.hostnameVerificationRequired(), base.tlsStatus(), base.networkStatus(),
                base.credentialTargetId(), base.credentialStatus(), objectType, parameter,
                base.atomicCertified(), base.expectedStateStrategy(), base.transportProfileStatus(),
                compatibility, base.onboardingStatus(), base.documentationStatus()
        );
    }

    private static com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkTargetEntity mockTarget(
            String environment, String expectedStateGuardStrength
    ) {
        var target = mock(com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkTargetEntity.class);
        when(target.getExpectedStateGuardStrength()).thenReturn(expectedStateGuardStrength);
        when(target.getEnvironment()).thenReturn(environment);
        when(target.getTargetState()).thenReturn("ACTIVE");
        return target;
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withNetworkStatus(
            ProductionCertificationAuthority.ResolvedCurrentness base, String networkStatus
    ) {
        ProductionCertificationAuthority.ResolvedCurrentness copied = copy(
                base, base.snapshot(), base.bundleStatus(), base.transportCertState(),
                base.snapshot().targetCertificationStatus(), base.snapshot().artifactDigest(),
                base.objectType(), base.parameter(), base.compatibilityStatus(),
                base.snapshot().transportHealthState(), base.snapshot().interfaceStatus());
        return new ProductionCertificationAuthority.ResolvedCurrentness(
                copied.snapshot(), copied.bundleStatus(), copied.transportCertState(), copied.approvedFqdn(),
                copied.approvedPort(), copied.tlsServerIdentity(), copied.networkDomain(), copied.routeZoneId(),
                copied.endpointVendor(), copied.endpointPlatform(), copied.endpointVersionNo(),
                copied.hostnameVerificationRequired(), copied.tlsStatus(), networkStatus,
                copied.credentialTargetId(), copied.credentialStatus(), copied.objectType(), copied.parameter(),
                copied.atomicCertified(), copied.expectedStateStrategy(), copied.transportProfileStatus(),
                copied.compatibilityStatus(), copied.onboardingStatus(), copied.documentationStatus()
        );
    }

    private ProductionCertificationAuthority.ResolvedCurrentness withDocumentation(
            ProductionCertificationAuthority.ResolvedCurrentness base, String documentation
    ) {
        ProductionCertificationAuthority.ResolvedCurrentness copied = copy(
                base, base.snapshot(), base.bundleStatus(), base.transportCertState(),
                base.snapshot().targetCertificationStatus(), base.snapshot().artifactDigest(),
                base.objectType(), base.parameter(), base.compatibilityStatus(),
                base.snapshot().transportHealthState(), base.snapshot().interfaceStatus());
        return new ProductionCertificationAuthority.ResolvedCurrentness(
                copied.snapshot(), copied.bundleStatus(), copied.transportCertState(), copied.approvedFqdn(),
                copied.approvedPort(), copied.tlsServerIdentity(), copied.networkDomain(), copied.routeZoneId(),
                copied.endpointVendor(), copied.endpointPlatform(), copied.endpointVersionNo(),
                copied.hostnameVerificationRequired(), copied.tlsStatus(), copied.networkStatus(),
                copied.credentialTargetId(), copied.credentialStatus(), copied.objectType(), copied.parameter(),
                copied.atomicCertified(), copied.expectedStateStrategy(), copied.transportProfileStatus(),
                copied.compatibilityStatus(), copied.onboardingStatus(), documentation
        );
    }
}
