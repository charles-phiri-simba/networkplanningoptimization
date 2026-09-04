package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.protocol.Phase17BundleDigest;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17BundleDigestAndSoDTest {

    private static final String DIGEST_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String BASELINE = "77fd24c0fd32c920c97ff5169f4bc8a93a77b208";
    private static final String EVIDENCE = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test
    void t17Impl069SameBundleSameDigest() {
        Phase17BundleDigest.BundleDigestInput input = sample(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        assertEquals(Phase17BundleDigest.digest(input), Phase17BundleDigest.digest(input));
    }

    @Test
    void t17Impl070NullVsEmptyDistinct() {
        UUID bundle = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Phase17BundleDigest.BundleDigestInput withNull = sample(bundle);
        Phase17BundleDigest.BundleDigestInput withEmptyEvidence = new Phase17BundleDigest.BundleDigestInput(
                withNull.bundleId(), withNull.versionNo(), withNull.vendor(), withNull.platform(),
                withNull.interfaceDefinitionVersionId(), withNull.interfaceApprovalId(),
                withNull.transportProfileVersionId(), withNull.artifactDigest(),
                withNull.transportImplementationVersion(), withNull.sourceBaselineSha(),
                withNull.vendorVersionPredicate(), withNull.capabilityCertVersionId(),
                withNull.securityCertVersionId(), withNull.credentialProfileVersionId(),
                withNull.tlsProfileVersionId(), withNull.networkPolicyProfileVersionId(),
                withNull.endpointProfileVersionId(), withNull.targetClass(),
                "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
        );
        assertNotEquals(Phase17BundleDigest.digest(withNull), Phase17BundleDigest.digest(withEmptyEvidence));
        assertNotEquals(
                Phase17BundleDigest.evidenceSetDigest(List.of()),
                Phase17BundleDigest.evidenceSetDigest(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")))
        );
    }

    @Test
    void t17Impl068UppercaseDigestRejected() {
        Phase17BundleDigest.BundleDigestInput bad = new Phase17BundleDigest.BundleDigestInput(
                UUID.randomUUID(), 1, "ERICSSON", "ENM", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                DIGEST_A.toUpperCase(), "unconfigured-0", BASELINE, "EXPLICIT",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "LAB", EVIDENCE
        );
        assertThrows(IllegalArgumentException.class, () -> Phase17BundleDigest.digest(bad));
    }

    @Test
    void t17Sec021NullBlankInvalidPrincipalsDenied() {
        Phase17SeparationOfDutiesPolicy sod = new Phase17SeparationOfDutiesPolicy();
        assertEquals(Phase17DenialCode.P17_SOD_VIOLATION,
                assertThrows(Phase17Exception.class, () -> sod.requirePrincipal(null, "actor")).denialCode());
        assertEquals(Phase17DenialCode.P17_SOD_VIOLATION,
                assertThrows(Phase17Exception.class, () -> sod.requirePrincipal("  ", "actor")).denialCode());
        assertEquals(Phase17DenialCode.P17_SOD_VIOLATION,
                assertThrows(Phase17Exception.class, () -> sod.requirePrincipal("", "actor")).denialCode());
        sod.requirePrincipal("certifier-1", "actor");
        assertThrows(Phase17Exception.class, () -> sod.requireDistinct("same", "same", "dup"));
        assertThrows(Phase17Exception.class, () -> sod.requireDistinct("\u0041lice", "alice", "dup"));
        sod.requireDistinct("alice", "alicé", "ok");
    }

    @Test
    void t17Sec003004AgentMcpDenied() {
        Phase17SeparationOfDutiesPolicy sod = new Phase17SeparationOfDutiesPolicy();
        assertEquals(Phase17DenialCode.P17_AGENT_DENIED,
                assertThrows(Phase17Exception.class, () -> sod.denyAgentOrMcp("agent:orchestrator")).denialCode());
        assertEquals(Phase17DenialCode.P17_MCP_DENIED,
                assertThrows(Phase17Exception.class, () -> sod.denyAgentOrMcp("mcp:tool")).denialCode());
        sod.denyAgentOrMcp("human-certifier");
    }

    @Test
    void boundFieldChangeChangesDigestAndSourceOrderIndependent() {
        Phase17BundleDigest.BundleDigestInput input = sample(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        String original = Phase17BundleDigest.digest(input);
        Phase17BundleDigest.BundleDigestInput changedVersion = new Phase17BundleDigest.BundleDigestInput(
                input.bundleId(), 2, input.vendor(), input.platform(),
                input.interfaceDefinitionVersionId(), input.interfaceApprovalId(),
                input.transportProfileVersionId(), input.artifactDigest(),
                input.transportImplementationVersion(), input.sourceBaselineSha(),
                input.vendorVersionPredicate(), input.capabilityCertVersionId(),
                input.securityCertVersionId(), input.credentialProfileVersionId(),
                input.tlsProfileVersionId(), input.networkPolicyProfileVersionId(),
                input.endpointProfileVersionId(), input.targetClass(),
                input.activeEvidenceSetDigest()
        );
        assertNotEquals(original, Phase17BundleDigest.digest(changedVersion));
        UUID a = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID b = UUID.fromString("22222222-2222-2222-2222-222222222222");
        assertEquals(Phase17BundleDigest.evidenceSetDigest(List.of(a, b)),
                Phase17BundleDigest.evidenceSetDigest(List.of(b, a)));
    }

    @Test
    void sodCanonicalPrincipalAliceEqualsAlice() {
        Phase17SeparationOfDutiesPolicy sod = new Phase17SeparationOfDutiesPolicy();
        assertThrows(Phase17Exception.class, () -> sod.requireDistinct("Alice", "alice", "dup"));
        assertThrows(Phase17Exception.class, () -> sod.requireDistinct(" alice ", "ALICE", "dup"));
        assertThrows(Phase17Exception.class, () -> sod.requirePrincipal("   ", "actor"));
        sod.requireDistinct("Alice", "Bob", "ok");
    }

    private static Phase17BundleDigest.BundleDigestInput sample(UUID bundleId) {
        return new Phase17BundleDigest.BundleDigestInput(
                bundleId, 1, "ERICSSON", "ENM",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                DIGEST_A, "unconfigured-0", BASELINE, "EXPLICIT",
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                "LAB", EVIDENCE
        );
    }
}
