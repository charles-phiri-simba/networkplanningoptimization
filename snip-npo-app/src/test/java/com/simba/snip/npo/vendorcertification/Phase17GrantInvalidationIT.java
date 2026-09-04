package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.vendorcertification.service.CertificationInvalidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Phase17GrantInvalidationIT extends ProductionChangeITSupport {

    @Autowired
    private CertificationInvalidationService invalidation;

    @AfterEach
    void cleanupPhase17() {
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void issuedRevokedConsumedPreservedUnrelatedUnchanged() {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("""
                INSERT INTO production_execution_grant (
                    grant_id, production_change_id, phase15_execution_id, target_id, grant_type, status,
                    production_fingerprint, authorization_generation, fencing_token, operation_binding_hash,
                    issued_at, expires_at, version)
                SELECT ?, production_change_id, phase15_execution_id, production_target_id, 'FORWARD', 'ISSUED',
                    production_fingerprint, authorization_generation, 1,
                    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
                    NOW(), NOW() + INTERVAL '5 minutes', 0
                FROM production_network_change WHERE production_change_id = ?
                """, UUID.randomUUID(), change.productionChangeId());
        assertEquals("ISSUED", grantStatus(change.productionChangeId()));

        invalidation.invalidate(new CertificationInvalidationService.InvalidationCommand(
                CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                "transport_certification",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                "REVOKED",
                Instant.now(),
                TARGET_ID,
                ActorPrincipal.of("cert-revoker-1")
        ));
        assertEquals("REVOKED", grantStatus(change.productionChangeId()));

        restoreGatewaySafetyFlags();
        ProductionChangeDto consumedChange = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(consumedChange);
        executeExpectingOk(consumedChange.productionChangeId());
        assertEquals("CONSUMED", grantStatus(consumedChange.productionChangeId()));
        invalidation.invalidate(new CertificationInvalidationService.InvalidationCommand(
                CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                "transport_certification",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                "REVOKED",
                Instant.now(),
                TARGET_ID,
                ActorPrincipal.of("cert-revoker-1")
        ));
        assertEquals("CONSUMED", grantStatus(consumedChange.productionChangeId()));

        CertificationInvalidationService.InvalidationResult first = invalidation.invalidate(
                new CertificationInvalidationService.InvalidationCommand(
                        CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                        "transport_certification",
                        "same-logical",
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "REVOKED",
                        Instant.parse("2026-01-01T00:00:00.000Z"),
                        TARGET_ID,
                        ActorPrincipal.of("cert-revoker-1")
                ));
        CertificationInvalidationService.InvalidationResult second = invalidation.invalidate(
                new CertificationInvalidationService.InvalidationCommand(
                        CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                        "transport_certification",
                        "same-logical",
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        "REVOKED",
                        Instant.parse("2026-01-01T00:00:00.000Z"),
                        TARGET_ID,
                        ActorPrincipal.of("cert-revoker-1")
                ));
        assertEquals(true, first.applied());
        assertEquals(true, second.idempotentReplay());
    }
}
