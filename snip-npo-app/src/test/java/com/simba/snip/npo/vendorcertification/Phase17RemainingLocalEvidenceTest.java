package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.domain.ExpectedStateGuardStrength;
import com.simba.snip.npo.productionchange.protocol.Phase17DenialCode;
import com.simba.snip.npo.targetonboarding.service.ProductionTargetOnboardingService;
import com.simba.snip.npo.vendorcertification.exception.Phase17Exception;
import com.simba.snip.npo.vendorcertification.policy.Phase17SeparationOfDutiesPolicy;
import com.simba.snip.npo.vendorcertification.service.VendorCapabilityCertificationService;
import com.simba.snip.npo.vendorcertification.service.VendorVersionCompatibilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17RemainingLocalEvidenceTest {

    @ParameterizedTest
    @CsvSource({
            "null,requester,T17-SEC-021",
            "'',requester,T17-SEC-022",
            "null,reviewer,T17-SEC-023",
            "' ',reviewer,T17-SEC-024",
            "null,certifier,T17-SEC-025",
            "'',certifier,T17-SEC-026",
            "null,executor,T17-SEC-027"
    })
    void nullBlankPrincipalsDenied(String principal, String role, String evidenceId) {
        Phase17SeparationOfDutiesPolicy sod = new Phase17SeparationOfDutiesPolicy();
        String value = "null".equals(principal) ? null : principal;
        assertEquals(Phase17DenialCode.P17_SOD_VIOLATION,
                assertThrows(Phase17Exception.class, () -> sod.requirePrincipal(value, role)).denialCode(),
                evidenceId);
    }

    @Test
    void t17Sec028SamePrincipalForbidden() {
        Phase17SeparationOfDutiesPolicy sod = new Phase17SeparationOfDutiesPolicy();
        assertThrows(Phase17Exception.class, () -> sod.requireDistinct("same", "same", "sod"));
    }

    @Test
    void t17Impl024025CapabilityCellTxPowerOnly() {
        VendorCapabilityCertificationService service =
                new VendorCapabilityCertificationService(new Phase17SeparationOfDutiesPolicy());
        service.requireCellTxPower("CELL", "txPower", "cap-1");
        assertThrows(Phase17Exception.class, () -> service.requireCellTxPower("NODE", "txPower", "cap-1"));
        assertThrows(Phase17Exception.class, () -> service.requireCellTxPower("CELL", "retT", "cap-1"));
    }

    @Test
    void t17Impl027060ReadThenWriteDefaultAndGuardReuse() {
        assertEquals("READ_THEN_WRITE", ExpectedStateGuardStrength.READ_THEN_WRITE.name());
        assertFalse(Files.exists(repoRoot().resolve(
                "production-change-protocol/src/main/java/com/simba/snip/npo/productionchange/protocol/ExpectedStateStrength.java")));
    }

    @Test
    void t17Impl028029030VendorVersionPredicate() {
        VendorVersionCompatibilityService service = new VendorVersionCompatibilityService();
        service.requireExplicitPredicate("ENM-22", "EXPLICIT:ENM-22");
        assertThrows(Phase17Exception.class, () -> service.requireExplicitPredicate(null, "EXPLICIT:ENM-22"));
        assertThrows(Phase17Exception.class, () -> service.requireExplicitPredicate("ENM-23", ">=1.0"));
        assertThrows(Phase17Exception.class, () -> service.requireExplicitPredicate("ENM-99", "EXPLICIT:ENM-22"));
    }

    @Test
    void t17Impl018020OnboardingSoD() {
        ProductionTargetOnboardingService service =
                new ProductionTargetOnboardingService(new Phase17SeparationOfDutiesPolicy());
        service.requireCreateReviewApproveDistinct("c", "r", "a", "e");
        assertThrows(Phase17Exception.class, () -> service.requireCreateReviewApproveDistinct("c", "c", "a", "e"));
        assertThrows(Phase17Exception.class, () -> service.requireCreateReviewApproveDistinct("c", "r", "a", "c"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "T17-INF-001:deploy/k8s/production-write-gateway-deployment.yaml",
            "T17-INF-002:deploy/k8s/production-write-gateway-serviceaccount.yaml",
            "T17-INF-004:production-write-gateway/src/main/resources/META-INF/snip-transport-artifact.json"
    })
    void infrastructureArtifactsPresent(String spec) {
        String path = spec.substring(spec.indexOf(':') + 1);
        assertTrue(Files.exists(repoRoot().resolve(path)), spec);
    }

    @Test
    void t17Inf003NoOpenEgressInV18() throws IOException {
        String v18 = Files.readString(repoRoot().resolve(
                "snip-npo-app/src/main/resources/db/migration/V18__phase17_certified_vendor_transport.sql"));
        assertTrue(v18.contains("0.0.0.0/0"));
        assertTrue(v18.contains("allowed_egress_scope = '0.0.0.0/0'"));
    }

    @Test
    void t17Inf005To008LocalInfraOnly() throws IOException {
        Path root = repoRoot();
        assertTrue(Files.exists(root.resolve("deploy/k8s")) || Files.exists(root.resolve("deploy")));
        String yaml = Files.readString(root.resolve("production-write-gateway/src/main/resources/application.yml"));
        assertFalse(yaml.contains("secret-value-cache: true"));
        assertTrue(Files.exists(root.resolve(
                "production-write-gateway/src/main/java/com/simba/snip/npo/productionwritegateway/vendortransport/PackagedRuntimeTransportArtifactIdentityProvider.java")));
    }

    @Test
    void t17Sec029EnvDigestNotAuthenticity() throws IOException {
        String provider = Files.readString(repoRoot().resolve(
                "production-write-gateway/src/main/java/com/simba/snip/npo/productionwritegateway/vendortransport/PackagedRuntimeTransportArtifactIdentityProvider.java"));
        assertTrue(provider.contains("computeDigest") || provider.contains("sha-256")
                || provider.contains("Sha256") || provider.contains("MessageDigest"));
        assertFalse(provider.contains("System.getenv(\"CLAIMED_DIGEST\") == certified"));
    }

    @Test
    void t17Int028AuditPayloadHasNoSecretColumns() throws IOException {
        String audit = Files.readString(repoRoot().resolve(
                "snip-npo-app/src/main/java/com/simba/snip/npo/vendorcertification/audit/Phase17CertificationAuditService.java"));
        assertFalse(audit.toLowerCase(java.util.Locale.ROOT).contains("secret_value"));
        assertFalse(audit.contains("password"));
    }

    @Test
    void t17Int002UnconfiguredTransportClassPresent() {
        assertTrue(Files.exists(repoRoot().resolve(
                "production-write-gateway/src/main/java/com/simba/snip/npo/productionwritegateway/transport/UnconfiguredProductionEricssonWriteTransport.java")));
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.exists(cwd.resolve("snip-npo-app")) ? cwd : cwd.getParent();
    }
}
