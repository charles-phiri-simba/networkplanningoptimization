package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.CertificationLevel;
import com.simba.snip.npo.productionchange.domain.ExpectedStateGuardStrength;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.domain.ProductionTargetState;
import com.simba.snip.npo.productionchange.service.ProductionTargetRegistry;
import com.simba.snip.npo.productionwritegateway.vendortransport.DestinationTrustValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17DenyPathMutationCountIT extends ProductionChangeITSupport {

    enum DenyCase {
        CS17_A_P16_STALE,
        CS17_B_CERT_REVOKED,
        CS17_C_INTERFACE_REVOKED,
        CS17_D_DEST_FQDN,
        CS17_E_ARTIFACT,
        CS17_F_CREDENTIAL_CROSS,
        CS17_G_TARGET_SUSPENDED,
        CS17_H_AUTHORITY_UNAVAILABLE,
        CS17_I_DURABLE_REVOKE,
        CS17_K_VENDOR_VERSION,
        CS17_M_ATOMIC,
        CS17_V_EXPIRED,
        CS17_W_SUPERSEDED,
        CS17_Y_HEALTH,
        CS17_Z_DOCUMENTATION,
        FI17_008_TLS,
        FI17_013_BUNDLE_INVALID,
        FI17_016_KILL_SWITCH,
        NULL_DESTINATION
    }

    @AfterEach
    void cleanup() {
        restoreAuthorityColumn();
        gatewayProperties().setEnabled(true);
        testTransport().setObservedDestination(new DestinationTrustValidator.ObservedDestination(
                "enm.lab.invalid", 443, "enm.lab.invalid", true, true, "LAB", "zone-a"));
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @ParameterizedTest(name = "{0} mutationCount==0")
    @EnumSource(DenyCase.class)
    void denyBeforeSendMutationExactlyZero(DenyCase denyCase) {
        String targetId = "ERICSSON-ENM-DENY-" + denyCase.name();
        ExpectedStateGuardStrength strength = denyCase == DenyCase.CS17_M_ATOMIC
                ? ExpectedStateGuardStrength.ATOMIC
                : ExpectedStateGuardStrength.READ_THEN_WRITE;
        targetRegistry.register(new ProductionTargetRegistry.TargetRegistration(
                targetId, "ERICSSON", "ENM", "PRODUCTION", "test", "RAN",
                "ericsson-enm-write-l0-deny", "1", "security-l0", "credential-profile-ref-l0-deny",
                "CELL", "txPower", "MANUAL", "p16-rollback-v1", "p16-verification-v1",
                CertificationLevel.L0, true, ProductionTargetState.ACTIVE, strength));
        ensureTargetHealth(targetId);
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, targetId, denyCase.name().toLowerCase());
        ProductionChangeDto change = createProductionChange(
                verifiedPhase15ExecutionId(), targetId, PRINCIPAL_CC_VALIDATOR,
                Instant.now().plus(2, ChronoUnit.HOURS));
        reviewProductionChange(change.productionChangeId());
        authorizeProductionChange(change.productionChangeId());
        seedTransportFor(change);
        applyFault(denyCase, graph, targetId, change);
        testTransport().reset();
        testTransport().seedCell(change.cellId(), change.expectedValue());
        if (denyCase == DenyCase.CS17_D_DEST_FQDN) {
            testTransport().setObservedDestination(new DestinationTrustValidator.ObservedDestination(
                    "evil.example", 443, "enm.lab.invalid", true, true, "LAB", "zone-a"));
        } else if (denyCase == DenyCase.FI17_008_TLS) {
            testTransport().setObservedDestination(new DestinationTrustValidator.ObservedDestination(
                    "enm.lab.invalid", 443, "other-tls", true, true, "LAB", "zone-a"));
        } else if (denyCase == DenyCase.NULL_DESTINATION) {
            testTransport().setObservedDestination(null);
        } else if (denyCase != DenyCase.CS17_H_AUTHORITY_UNAVAILABLE) {
            testTransport().setObservedDestination(new DestinationTrustValidator.ObservedDestination(
                    "enm.lab.invalid", 443, "enm.lab.invalid", true, true, "LAB", "zone-a"));
        }
        int before = mutationCount();
        ResponseEntity<String> response = http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(java.util.Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());
        assertEquals(0, mutationCount() - before);
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()
                || (response.getBody() != null && (response.getBody().contains("P17_")
                || response.getBody().contains("PREFLIGHT")
                || response.getBody().contains("DENIED"))));
    }

    private void applyFault(
            DenyCase denyCase,
            Phase17CertificationGraphSeeder.Graph graph,
            String targetId,
            ProductionChangeDto change
    ) {
        switch (denyCase) {
            case CS17_A_P16_STALE -> jdbc.update(
                    "UPDATE production_change_authorization SET status = 'STALE' WHERE production_change_id = ?",
                    change.productionChangeId());
            case CS17_B_CERT_REVOKED, CS17_I_DURABLE_REVOKE -> jdbc.update(
                    "UPDATE transport_certification SET state = 'REVOKED' WHERE transport_certification_id = ?",
                    graph.certificationId());
            case CS17_C_INTERFACE_REVOKED -> jdbc.update(
                    "UPDATE vendor_interface_definition SET status = 'REVOKED' WHERE interface_definition_id = ?",
                    graph.interfaceId());
            case CS17_D_DEST_FQDN, FI17_008_TLS, NULL_DESTINATION -> {
            }
            case CS17_E_ARTIFACT -> jdbc.update(
                    "UPDATE transport_certification_bundle SET artifact_digest = ? WHERE bundle_version_id = ?",
                    "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", graph.bundleVersionId());
            case CS17_F_CREDENTIAL_CROSS -> {
                targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson("ERICSSON-ENM-DENY-OTHER"));
                jdbc.update(
                        "UPDATE production_credential_profile SET production_target_id = ? WHERE credential_profile_version_id = ?",
                        "ERICSSON-ENM-DENY-OTHER", graph.credentialProfileVersionId());
            }
            case CS17_G_TARGET_SUSPENDED -> jdbc.update(
                    "UPDATE production_target_onboarding SET status = 'SUSPENDED' WHERE onboarding_id = ?",
                    graph.onboardingId());
            case CS17_H_AUTHORITY_UNAVAILABLE -> jdbc.update(
                    "ALTER TABLE production_target_certification RENAME COLUMN status TO status_hidden");
            case CS17_K_VENDOR_VERSION -> jdbc.update(
                    "UPDATE vendor_version_compatibility SET status = 'SUSPENDED' WHERE transport_profile_version_id = ?",
                    graph.transportProfileVersionId());
            case CS17_M_ATOMIC -> {
            }
            case CS17_V_EXPIRED -> jdbc.update(
                    "UPDATE transport_certification SET state = 'EXPIRED' WHERE transport_certification_id = ?",
                    graph.certificationId());
            case CS17_W_SUPERSEDED -> jdbc.update(
                    "UPDATE vendor_interface_definition SET status = 'SUPERSEDED' WHERE interface_definition_id = ?",
                    graph.interfaceId());
            case CS17_Y_HEALTH -> jdbc.update(
                    "UPDATE vendor_transport_health SET health_state = 'DEGRADED' WHERE production_target_id = ?",
                    targetId);
            case CS17_Z_DOCUMENTATION -> jdbc.update(
                    "UPDATE vendor_interface_definition SET documentation_status = 'WITHDRAWN' WHERE interface_definition_id = ?",
                    graph.interfaceId());
            case FI17_013_BUNDLE_INVALID -> jdbc.update(
                    "UPDATE transport_certification_bundle SET status = 'INVALID' WHERE bundle_version_id = ?",
                    graph.bundleVersionId());
            case FI17_016_KILL_SWITCH -> gatewayProperties().setEnabled(false);
            default -> throw new IllegalStateException(denyCase.name());
        }
    }

    private void restoreAuthorityColumn() {
        Integer hidden = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_name = 'production_target_certification' AND column_name = 'status_hidden'",
                Integer.class);
        if (hidden != null && hidden > 0) {
            jdbc.update("ALTER TABLE production_target_certification RENAME COLUMN status_hidden TO status");
        }
    }
}
