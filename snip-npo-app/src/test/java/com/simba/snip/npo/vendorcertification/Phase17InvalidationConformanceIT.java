package com.simba.snip.npo.vendorcertification;

import com.simba.snip.npo.productionchange.ProductionChangeITSupport;
import com.simba.snip.npo.productionchange.api.ProductionChangeDto;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.service.ProductionTargetRegistry;
import com.simba.snip.npo.productionwritegateway.service.ConsumeCommand;
import com.simba.snip.npo.productionwritegateway.service.ConsumeResult;
import com.simba.snip.npo.productionwritegateway.service.ProductionGrantConsumeService;
import com.simba.snip.npo.productionwritegateway.vendortransport.DestinationTrustValidator;
import com.simba.snip.npo.vendorcertification.service.CertificationInvalidationService;
import com.simba.snip.npo.vendorcertification.service.Phase17CertificationExpiryScheduler;
import com.simba.snip.npo.vendorcertification.service.TestInvalidationTransactionHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17InvalidationConformanceIT extends ProductionChangeITSupport {

    static final String TARGET_A = "ERICSSON-ENM-PHASE17-A";
    static final String TARGET_B = "ERICSSON-ENM-PHASE17-B";

    @Autowired
    private CertificationInvalidationService invalidation;
    @Autowired
    private Phase17CertificationExpiryScheduler expiryScheduler;
    @Autowired
    private TestInvalidationTransactionHook hook;

    @AfterEach
    void resetHookAndPhase17() {
        hook.reset();
        cleanupPhase17Rows();
    }

    private void cleanupPhase17Rows() {
        Phase17GraphCleanup.deleteAll(jdbc);
    }

    @Test
    void c17i01CrossTargetInvalidationIsolation() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_B));
        var graphA = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_A, "iso-a");
        var graphB = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_B, "iso-b");
        issueGrant(TARGET_A);
        UUID grantB = issueGrant(TARGET_B);

        invalidation.invalidate(new CertificationInvalidationService.InvalidationCommand(
                CertificationInvalidationService.TriggerType.INTERFACE_REVOKED,
                "vendor_interface_definition",
                graphA.interfaceId().toString(),
                graphA.interfaceVersionId(),
                "REVOKED",
                Instant.now(),
                TARGET_A,
                ActorPrincipal.of("cert-revoker-1")
        ));

        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT state FROM transport_certification WHERE transport_certification_id = ?",
                String.class, graphA.certificationId()));
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT status FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, graphA.bundleVersionId()));
        assertEquals("PRODUCTION_REGISTERED", jdbc.queryForObject(
                "SELECT state FROM transport_certification WHERE transport_certification_id = ?",
                String.class, graphB.certificationId()));
        assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT status FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, graphB.bundleVersionId()));
        assertEquals("APPROVED", jdbc.queryForObject(
                "SELECT status FROM production_target_onboarding WHERE onboarding_id = ?",
                String.class, graphB.onboardingId()));
        assertEquals("CURRENT", jdbc.queryForObject(
                "SELECT status FROM production_target_certification WHERE target_certification_id = ?",
                String.class, graphB.targetCertificationId()));
        assertEquals("ISSUED", jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?", String.class, grantB));
    }

    @Test
    void c17i01TwoProfilesSameTargetOnlyBoundSourceAffected() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        var primary = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_A, "two-p");
        var second = Phase17CertificationGraphSeeder.seedSecondProfile(jdbc, primary, "two-p2");
        invalidation.invalidate(new CertificationInvalidationService.InvalidationCommand(
                CertificationInvalidationService.TriggerType.INTERFACE_REVOKED,
                "vendor_interface_definition",
                second.interfaceId().toString(),
                second.interfaceVersionId(),
                "REVOKED",
                Instant.now(),
                TARGET_A,
                ActorPrincipal.of("cert-revoker-1")
        ));
        assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT status FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, primary.bundleVersionId()));
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT status FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, second.bundleVersionId()));
        assertEquals("CURRENT", jdbc.queryForObject(
                "SELECT status FROM production_target_certification WHERE target_certification_id = ?",
                String.class, primary.targetCertificationId()));
    }

    @Test
    void c17i02LateFailureRollsBackEntireTransaction() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_A, "rb");
        UUID grantId = issueGrant(TARGET_A);
        String certBefore = jdbc.queryForObject(
                "SELECT state FROM transport_certification WHERE transport_certification_id = ?",
                String.class, graph.certificationId());
        String bundleBefore = jdbc.queryForObject(
                "SELECT status FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, graph.bundleVersionId());
        Integer eventsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM phase17_invalidation_event", Integer.class);
        Integer auditBefore = jdbc.queryForObject("SELECT COUNT(*) FROM phase17_certification_audit_event", Integer.class);
        Integer outboxBefore = jdbc.queryForObject("SELECT COUNT(*) FROM phase17_invalidation_outbox", Integer.class);
        hook.failAfterRequiredWrites();
        assertThrows(RuntimeException.class, () -> invalidation.invalidate(
                new CertificationInvalidationService.InvalidationCommand(
                        CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                        "transport_certification",
                        graph.certificationId().toString(),
                        graph.certificationId(),
                        "REVOKED",
                        Instant.now(),
                        TARGET_A,
                        ActorPrincipal.of("cert-revoker-1")
                )));
        assertEquals(certBefore, jdbc.queryForObject(
                "SELECT state FROM transport_certification WHERE transport_certification_id = ?",
                String.class, graph.certificationId()));
        assertEquals(bundleBefore, jdbc.queryForObject(
                "SELECT status FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, graph.bundleVersionId()));
        assertEquals("ISSUED", jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?", String.class, grantId));
        assertEquals(eventsBefore, jdbc.queryForObject("SELECT COUNT(*) FROM phase17_invalidation_event", Integer.class));
        assertEquals(auditBefore, jdbc.queryForObject("SELECT COUNT(*) FROM phase17_certification_audit_event", Integer.class));
        assertEquals(outboxBefore, jdbc.queryForObject("SELECT COUNT(*) FROM phase17_invalidation_outbox", Integer.class));
    }

    @Test
    void b17i03GrantStatePreservation() {
        ProductionChangeDto issuedChange = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        UUID issued = insertIssuedGrant(issuedChange);
        assertEquals("ISSUED", grantStatus(issuedChange.productionChangeId()));
        invalidation.invalidate(revokeCmd(TARGET_ID));
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?", String.class, issued));

        restoreGatewaySafetyFlags();
        ProductionChangeDto consumed = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(consumed);
        executeExpectingOk(consumed.productionChangeId());
        assertEquals("CONSUMED", grantStatus(consumed.productionChangeId()));
        invalidation.invalidate(revokeCmd(TARGET_ID));
        assertEquals("CONSUMED", grantStatus(consumed.productionChangeId()));

        ProductionChangeDto expiredChange = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        UUID expiredId = insertIssuedGrant(expiredChange);
        jdbc.update("UPDATE production_execution_grant SET status = 'EXPIRED' WHERE grant_id = ?", expiredId);
        ProductionChangeDto revokedChange = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        UUID alreadyRevoked = insertIssuedGrant(revokedChange);
        jdbc.update("UPDATE production_execution_grant SET status = 'REVOKED' WHERE grant_id = ?", alreadyRevoked);
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        UUID unrelated = issueGrant(TARGET_A);
        invalidation.invalidate(revokeCmd(TARGET_ID));
        assertEquals("EXPIRED", jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?", String.class, expiredId));
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?", String.class, alreadyRevoked));
        assertEquals("ISSUED", jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?", String.class, unrelated));
    }

    @Test
    void c17i03InvalidationWinsConsumeRejected() throws Exception {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        UUID grantId = insertIssuedGrant(change);
        ProductionGrantConsumeService consume = gatewayContext().getBean(ProductionGrantConsumeService.class);
        CyclicBarrier barrier = new CyclicBarrier(2);
        hook.setAfterLocksAction(() -> {
            try {
                barrier.await(15, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
        AtomicReference<ConsumeResult> consumeResult = new AtomicReference<>();
        Thread consumeThread = new Thread(() -> {
            try {
                barrier.await(15, TimeUnit.SECONDS);
                consumeResult.set(consume.consume(commandFor(change, grantId)));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
        Thread invalidateThread = new Thread(() -> invalidation.invalidate(revokeCmd(TARGET_ID)));
        consumeThread.start();
        invalidateThread.start();
        consumeThread.join(20_000);
        invalidateThread.join(20_000);
        assertEquals("REVOKED", grantStatus(change.productionChangeId()));
        assertTrue(consumeResult.get() != null && !consumeResult.get().succeeded());
        assertEquals(0, mutationCount());
        assertEquals(0, grantCount(change.productionChangeId(), "ISSUED"));
        Integer replacements = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ?",
                Integer.class, change.productionChangeId());
        assertEquals(1, replacements);
    }

    @Test
    void c17i03ConsumeWinsGrantStaysConsumed() throws Exception {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        UUID grantId = insertIssuedGrant(change);
        ProductionGrantConsumeService consume = gatewayContext().getBean(ProductionGrantConsumeService.class);
        CyclicBarrier started = new CyclicBarrier(2);
        CountDownLatch consumed = new CountDownLatch(1);
        Thread consumeThread = new Thread(() -> {
            try {
                started.await(15, TimeUnit.SECONDS);
                consume.consume(commandFor(change, grantId));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                consumed.countDown();
            }
        });
        Thread invalidateThread = new Thread(() -> {
            try {
                started.await(15, TimeUnit.SECONDS);
                assertTrue(consumed.await(15, TimeUnit.SECONDS));
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            invalidation.invalidate(revokeCmd(TARGET_ID));
        });
        consumeThread.start();
        invalidateThread.start();
        consumeThread.join(20_000);
        invalidateThread.join(20_000);
        assertEquals("CONSUMED", grantStatus(change.productionChangeId()));
        assertNotEquals("REVOKED", grantStatus(change.productionChangeId()));
        Integer replacements = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_execution_grant WHERE production_change_id = ?",
                Integer.class, change.productionChangeId());
        assertEquals(1, replacements);
    }

    @Test
    void b17i11ExpiryUsesInvalidationCascade() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_A, "exp");
        UUID issued = issueGrant(TARGET_A);
        jdbc.update("UPDATE vendor_write_transport_profile SET certification_expiry = NOW() - INTERVAL '1 hour' "
                + "WHERE transport_profile_version_id = ?", graph.transportProfileVersionId());
        expiryScheduler.markExpired();
        expiryScheduler.markExpired();
        assertEquals("EXPIRED", jdbc.queryForObject(
                "SELECT state FROM transport_certification WHERE transport_certification_id = ?",
                String.class, graph.certificationId()));
        assertEquals("EXPIRED", jdbc.queryForObject(
                "SELECT status FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, graph.bundleVersionId()));
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT status FROM production_execution_grant WHERE grant_id = ?", String.class, issued));
        ProductionChangeDto later = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_network_change SET production_target_id = ? WHERE production_change_id = ?",
                TARGET_A, later.productionChangeId());
        seedTransportFor(later);
        int before = mutationCount();
        http.exchange(
                "/api/v1/production-changes/" + later.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());
    }

    @Test
    void b17i05CertifiedImmutabilityRejected() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_A, "imm");
        String original = jdbc.queryForObject(
                "SELECT content_digest FROM vendor_write_transport_profile WHERE transport_profile_version_id = ?",
                String.class, graph.transportProfileVersionId());
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE vendor_write_transport_profile SET content_digest = ? WHERE transport_profile_version_id = ?",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", graph.transportProfileVersionId()));
        assertEquals(original, jdbc.queryForObject(
                "SELECT content_digest FROM vendor_write_transport_profile WHERE transport_profile_version_id = ?",
                String.class, graph.transportProfileVersionId()));
        String bundle = jdbc.queryForObject(
                "SELECT content_digest FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, graph.bundleVersionId());
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE transport_certification_bundle SET content_digest = ? WHERE bundle_version_id = ?",
                "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee", graph.bundleVersionId()));
        assertEquals(bundle, jdbc.queryForObject(
                "SELECT content_digest FROM transport_certification_bundle WHERE bundle_version_id = ?",
                String.class, graph.bundleVersionId()));
        String endpoint = jdbc.queryForObject(
                "SELECT content_digest FROM production_endpoint_profile WHERE endpoint_profile_version_id = ?",
                String.class, graph.endpointProfileVersionId());
        assertThrows(Exception.class, () -> jdbc.update(
                "UPDATE production_endpoint_profile SET content_digest = ? WHERE endpoint_profile_version_id = ?",
                "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd",
                graph.endpointProfileVersionId()));
        assertEquals(endpoint, jdbc.queryForObject(
                "SELECT content_digest FROM production_endpoint_profile WHERE endpoint_profile_version_id = ?",
                String.class, graph.endpointProfileVersionId()));
    }

    @Test
    void b17i10DestinationMismatchMutationCountZero() {
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_ID, "dest");
        assertDestinationMismatch(new DestinationTrustValidator.ObservedDestination(
                "evil.example", 443, "enm.lab.invalid", true, true, "LAB", "zone-a"));
        assertDestinationMismatch(new DestinationTrustValidator.ObservedDestination(
                "enm.lab.invalid", 8443, "enm.lab.invalid", true, true, "LAB", "zone-a"));
        assertDestinationMismatch(new DestinationTrustValidator.ObservedDestination(
                "enm.lab.invalid", 443, "other.tls", true, true, "LAB", "zone-a"));
        assertDestinationMismatch(new DestinationTrustValidator.ObservedDestination(
                "enm.lab.invalid", 443, "enm.lab.invalid", false, true, "LAB", "zone-a"));
        assertDestinationMismatch(new DestinationTrustValidator.ObservedDestination(
                "enm.lab.invalid", 443, "enm.lab.invalid", true, false, "LAB", "zone-a"));
        assertDestinationMismatch(new DestinationTrustValidator.ObservedDestination(
                "enm.lab.invalid", 443, "enm.lab.invalid", true, true, "PROD", "zone-a"));
        testTransport().setObservedDestination(null);
        ProductionChangeDto missing = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(missing);
        http.exchange(
                "/api/v1/production-changes/" + missing.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());
        jdbc.update("UPDATE production_target_certification SET status = 'REVOKED' WHERE target_certification_id = ?",
                graph.targetCertificationId());
    }

    private void assertDestinationMismatch(DestinationTrustValidator.ObservedDestination observed) {
        testTransport().reset();
        testTransport().setObservedDestination(observed);
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        int before = mutationCount();
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());
    }

    @Test
    void b17i09CrossTargetCredentialDenied() {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_ID, "cred-l0");
        jdbc.update("UPDATE production_credential_profile SET production_target_id = ? WHERE credential_profile_version_id = ?",
                TARGET_A, graph.credentialProfileVersionId());
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        int before = gatewayContext().getBean(
                com.simba.snip.npo.productionwritegateway.service.ProductionCredentialResolutionService.class)
                .getCredentialResolutionCount();
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(0, mutationCount());
        int after = gatewayContext().getBean(
                com.simba.snip.npo.productionwritegateway.service.ProductionCredentialResolutionService.class)
                .getCredentialResolutionCount();
        assertEquals(before, after);
        jdbc.update("UPDATE production_target_certification SET status = 'REVOKED' WHERE production_target_id = ?", TARGET_ID);
    }

    @Test
    void c17i06Phase16AuthorizationStaleFingerprintGenerationExpiryDeny() {
        Phase17CertificationGraphSeeder.seed(jdbc, TARGET_ID, "p16auth");
        ProductionChangeDto stale = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_change_authorization SET status = 'STALE' WHERE production_change_id = ?",
                stale.productionChangeId());
        seedTransportFor(stale);
        int before = mutationCount();
        http.exchange(
                "/api/v1/production-changes/" + stale.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());

        ProductionChangeDto fingerprint = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_change_authorization SET production_fingerprint = ? WHERE production_change_id = ?",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                fingerprint.productionChangeId());
        seedTransportFor(fingerprint);
        before = mutationCount();
        http.exchange(
                "/api/v1/production-changes/" + fingerprint.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());

        ProductionChangeDto generation = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_change_authorization SET authorization_generation = authorization_generation + 1 "
                + "WHERE production_change_id = ?", generation.productionChangeId());
        seedTransportFor(generation);
        before = mutationCount();
        http.exchange(
                "/api/v1/production-changes/" + generation.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());

        ProductionChangeDto expired = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        jdbc.update("UPDATE production_change_authorization SET expires_at = NOW() - INTERVAL '1 hour' "
                + "WHERE production_change_id = ?", expired.productionChangeId());
        seedTransportFor(expired);
        before = mutationCount();
        http.exchange(
                "/api/v1/production-changes/" + expired.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());
        jdbc.update("UPDATE production_target_certification SET status = 'REVOKED' WHERE production_target_id = ?", TARGET_ID);
    }

    @Test
    void b17i01ConcurrentInvalidationsDoNotDeadlock() throws Exception {
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_A));
        targetRegistry.register(ProductionTargetRegistry.TargetRegistration.l0Ericsson(TARGET_B));
        var graphA = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_A, "lk-a");
        var graphB = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_B, "lk-b");
        CyclicBarrier barrier = new CyclicBarrier(2);
        Thread t1 = new Thread(() -> {
            try {
                barrier.await(15, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            invalidation.invalidate(new CertificationInvalidationService.InvalidationCommand(
                    CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                    "transport_certification",
                    graphA.certificationId().toString(),
                    graphA.certificationId(),
                    "REVOKED",
                    Instant.now(),
                    TARGET_A,
                    ActorPrincipal.of("cert-revoker-1")
            ));
        });
        Thread t2 = new Thread(() -> {
            try {
                barrier.await(15, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            invalidation.invalidate(new CertificationInvalidationService.InvalidationCommand(
                    CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                    "transport_certification",
                    graphB.certificationId().toString(),
                    graphB.certificationId(),
                    "REVOKED",
                    Instant.now(),
                    TARGET_B,
                    ActorPrincipal.of("cert-revoker-2")
            ));
        });
        t1.start();
        t2.start();
        t1.join(20_000);
        t2.join(20_000);
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT state FROM transport_certification WHERE transport_certification_id = ?",
                String.class, graphA.certificationId()));
        assertEquals("REVOKED", jdbc.queryForObject(
                "SELECT state FROM transport_certification WHERE transport_certification_id = ?",
                String.class, graphB.certificationId()));
    }

    @Test
    void b17i09UnknownAndInactiveCredentialDenied() {
        var graph = Phase17CertificationGraphSeeder.seed(jdbc, TARGET_ID, "cred-unk");
        jdbc.update("UPDATE production_credential_profile SET status = 'SUPERSEDED' WHERE credential_profile_version_id = ?",
                graph.credentialProfileVersionId());
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        seedTransportFor(change);
        int before = mutationCount();
        http.exchange(
                "/api/v1/production-changes/" + change.productionChangeId() + "/execute",
                HttpMethod.POST,
                productionEntity(Map.of(), ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE, PRINCIPAL_EXECUTOR),
                String.class);
        assertEquals(before, mutationCount());
        jdbc.update("UPDATE production_target_certification SET status = 'REVOKED' WHERE production_target_id = ?", TARGET_ID);
    }

    private ConsumeCommand commandFor(ProductionChangeDto change, UUID grantId) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT * FROM production_execution_grant WHERE grant_id = ?", grantId);
        return new ConsumeCommand(
                grantId,
                change.productionChangeId(),
                (UUID) row.get("phase15_execution_id"),
                (String) row.get("target_id"),
                (String) row.get("production_fingerprint"),
                ((Number) row.get("authorization_generation")).intValue(),
                ((Number) row.get("fencing_token")).longValue(),
                (String) row.get("operation_binding_hash"),
                com.simba.snip.npo.productionchange.protocol.GrantType.FORWARD
        );
    }

    private CertificationInvalidationService.InvalidationCommand revokeCmd(String targetId) {
        return new CertificationInvalidationService.InvalidationCommand(
                CertificationInvalidationService.TriggerType.CERTIFICATION_REVOKED,
                "transport_certification",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                "REVOKED",
                Instant.now(),
                targetId,
                ActorPrincipal.of("cert-revoker-1")
        );
    }

    private UUID issueGrant(String targetId) {
        ProductionChangeDto change = reviewedAndAuthorized(verifiedPhase15ExecutionId());
        UUID grantId = insertIssuedGrant(change);
        if (!TARGET_ID.equals(targetId)) {
            jdbc.update("UPDATE production_network_change SET production_target_id = ? WHERE production_change_id = ?",
                    targetId, change.productionChangeId());
            jdbc.update("UPDATE production_execution_grant SET target_id = ? WHERE grant_id = ?",
                    targetId, grantId);
        }
        return grantId;
    }

    private UUID insertIssuedGrant(ProductionChangeDto change) {
        UUID grantId = UUID.randomUUID();
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
                """, grantId, change.productionChangeId());
        return grantId;
    }
}
