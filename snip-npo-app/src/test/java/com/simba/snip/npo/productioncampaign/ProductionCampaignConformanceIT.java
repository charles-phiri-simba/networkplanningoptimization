package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.api.ProductionCampaignDto;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.security.HeaderAuthenticatedActorProvider;
import com.simba.snip.npo.productioncampaign.service.CampaignFingerprintFactory;
import com.simba.snip.npo.productioncampaign.service.CampaignGovernedProgressionService;
import com.simba.snip.npo.productioncampaign.service.CampaignHandoffOrchestrationService;
import com.simba.snip.npo.productioncampaign.service.CampaignItemLifecycleService;
import com.simba.snip.npo.productioncampaign.service.CampaignMutationSlotService;
import com.simba.snip.npo.productioncampaign.service.CampaignObservationService;
import com.simba.snip.npo.productioncampaign.service.CampaignPreSendValidationService;
import com.simba.snip.npo.productioncampaign.service.CampaignResumptionService;
import com.simba.snip.npo.productioncampaign.service.CanonicalReconciliationReader;
import com.simba.snip.npo.productioncampaign.service.ExternalInterferenceService;
import com.simba.snip.npo.productioncampaign.service.ForwardProgressionClosureService;
import com.simba.snip.npo.productioncampaign.service.ProductionChangeCampaignService;
import com.simba.snip.npo.productioncampaign.service.VendorRejectedSemantics;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionCampaignConformanceIT extends ProductionCampaignITSupport {

    @Autowired TestRestTemplate http;
    @Autowired CampaignHandoffOrchestrationService orchestration;
    @Autowired CampaignItemLifecycleService itemLifecycle;
    @Autowired CampaignGovernedProgressionService governedProgression;
    @Autowired CampaignMutationSlotService slotService;
    @Autowired CampaignResumptionService resumptionService;
    @Autowired CampaignObservationService observationService;
    @Autowired CanonicalReconciliationReader reconciliationReader;
    @Autowired ExternalInterferenceService interferenceService;
    @Autowired CampaignPreSendValidationService preSendValidation;
    @Autowired ForwardProgressionClosureService closureService;
    @Autowired com.simba.snip.npo.productioncampaign.service.CampaignRecoveryService recoveryService;
    @Autowired VendorRejectedSemantics vendorRejectedSemantics;
    @Autowired PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @ValueSource(strings = {
            "DETERMINED_RECOVERY_REQUIRED",
            "UNRESOLVED_MUTATION_OUTCOME",
            "CANARY_HEALTH_AND_SAFETY_PASS",
            "COMPLETE_CAMPAIGN",
            "MATERIAL_INVALIDATION",
            "EVALUATE_READY"
    })
    void httpSystemTriggersDeniedUnauthenticatedAndAuthenticated(String trigger) {
        var created = newCampaign("CELL-HTTP-" + trigger.substring(0, 8), "idem-http-" + trigger);
        HttpHeaders anon = new HttpHeaders();
        anon.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        ResponseEntity<String> unauth = http.exchange(
                "/api/v1/production-campaigns/" + created.getCampaignId() + "/commands",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("trigger", trigger), anon),
                String.class
        );
        assertTrue(unauth.getStatusCode().is4xxClientError());
        HttpHeaders headers = commandHeaders("CAMPAIGN_AUTHORIZE", "human-auth");
        ResponseEntity<String> auth = http.exchange(
                "/api/v1/production-campaigns/" + created.getCampaignId() + "/commands",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("trigger", trigger), headers),
                String.class
        );
        assertTrue(auth.getStatusCode().is4xxClientError());
        assertTrue(auth.getBody().contains(ProductionReasonCode.SYSTEM_TRANSITION_DENIED.name()));
        CampaignException serviceDeny = assertThrows(
                CampaignException.class,
                () -> campaignService.transition(
                        created.getCampaignId(), trigger, actor("human-auth", CampaignPermission.CAMPAIGN_AUTHORIZE), "idem-sys-" + trigger)
        );
        assertEquals(ProductionReasonCode.SYSTEM_TRANSITION_DENIED, serviceDeny.reasonCode());
    }

    @Test
    void fromStateGuardsRejectIllegalSystemEffects() {
        var created = newCampaign("CELL-FROM-1", "idem-from-1");
        CampaignException unresolved = assertThrows(
                CampaignException.class,
                () -> campaignService.recordUnresolvedMutationOutcome(created.getCampaignId())
        );
        assertEquals(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, unresolved.reasonCode());
        CampaignException ready = assertThrows(
                CampaignException.class,
                () -> campaignService.evaluateReady(created.getCampaignId())
        );
        assertEquals(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, ready.reasonCode());
    }

    @Test
    void handoffCrashBoundariesAndDuplicateConflict() throws Exception {
        var released = authorizeReadyAndRelease("CELL-HAND-C", "idem-hand-c");
        UUID campaignId = released.getCampaignId();
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            orchestration.commitForwardHandoff(campaignId, itemId);
            throw new IllegalStateException("crash before campaign handoff commit");
        }));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_execution_handoff WHERE campaign_id = ?", Integer.class, campaignId));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_network_change WHERE execution_origin = 'PRODUCTION_CAMPAIGN'", Integer.class));

        String first = orchestration.commitForwardHandoff(campaignId, itemId);
        String retry = orchestration.commitForwardHandoff(campaignId, itemId);
        assertEquals(first, retry);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_execution_handoff WHERE campaign_id = ?", Integer.class, campaignId));

        UUID changeId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, requester_principal_id, audit_chain_integrity,
                    execution_origin, campaign_handoff_id, created_at, updated_at, version)
                VALUES (?, ?, ?, 'CC-H', 'REQUESTED', ?, 0, 'CELL-HAND-C', 'txPower', 46, 43, 'req', 'VALID',
                        'PRODUCTION_CAMPAIGN', ?, NOW(), NOW(), 0)
                """,
                changeId, UUID.randomUUID(), TARGET_ID, "e".repeat(64), first
        );
        assertEquals(changeId, jdbc.queryForObject(
                "SELECT production_change_id FROM production_network_change WHERE campaign_handoff_id = ?",
                UUID.class,
                first
        ));

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();
        var released2 = authorizeReadyAndRelease("CELL-HAND-D", "idem-hand-d");
        UUID campaign2 = released2.getCampaignId();
        UUID item2 = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaign2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> f1 = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                try {
                    a.set(orchestration.commitForwardHandoff(campaign2, item2));
                } catch (CampaignException ex) {
                    a.set(jdbc.queryForObject(
                            "SELECT campaign_handoff_id FROM campaign_execution_handoff WHERE campaign_id = ?",
                            String.class,
                            campaign2
                    ));
                }
                return null;
            });
            Future<?> f2 = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                try {
                    b.set(orchestration.commitForwardHandoff(campaign2, item2));
                } catch (CampaignException ex) {
                    b.set(jdbc.queryForObject(
                            "SELECT campaign_handoff_id FROM campaign_execution_handoff WHERE campaign_id = ?",
                            String.class,
                            campaign2
                    ));
                }
                return null;
            });
            f1.get(30, TimeUnit.SECONDS);
            f2.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(a.get(), b.get());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_execution_handoff WHERE campaign_id = ?", Integer.class, campaign2));
    }

    @Test
    void itemStateMatrixDeniesUnspecifiedTransition() {
        var campaign = newCampaign("CELL-ITEM-1", "idem-item-1");
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaign.getCampaignId());
        CampaignException ex = assertThrows(CampaignException.class, () -> itemLifecycle.transition(itemId, "PLANNED", "EXECUTE"));
        assertEquals(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, ex.reasonCode());
        assertEquals("RELEASED", itemLifecycle.transition(itemId, "PLANNED", "CURRENT_EXACT_COHORT_RELEASE"));
        assertEquals("ELIGIBLE", itemLifecycle.transition(itemId, "RELEASED", "INDIVIDUAL_ELIGIBILITY"));
        assertEquals("HANDOFF_PENDING", itemLifecycle.transition(itemId, "ELIGIBLE", "START_HANDOFF"));
        assertEquals("HANDED_OFF", itemLifecycle.transition(itemId, "HANDOFF_PENDING", "P16_LINEAGE_CORRELATED"));
        assertEquals("PRE_SEND", itemLifecycle.transition(itemId, "HANDED_OFF", "PRE_SEND_READY"));
        assertEquals("MAY_HAVE_SENT", itemLifecycle.transition(itemId, "PRE_SEND", "GATEWAY_SEND_BOUNDARY"));
        assertEquals("VENDOR_ACCEPTED", itemLifecycle.transition(itemId, "MAY_HAVE_SENT", "ACCEPTANCE_EVIDENCE"));
        assertEquals("VERIFYING", itemLifecycle.transition(itemId, "VENDOR_ACCEPTED", "START_VERIFY"));
        assertEquals("PRODUCTION_VERIFIED", itemLifecycle.transition(itemId, "VERIFYING", "DIRECT_DESIRED_READBACK"));
        assertEquals("RECONCILIATION_PENDING", itemLifecycle.transition(itemId, "PRODUCTION_VERIFIED", "BEGIN_RECONCILE"));
        assertEquals("CANONICAL_RECONCILED", itemLifecycle.transition(itemId, "RECONCILIATION_PENDING", "PROOF_FORM_A_OR_B"));
        assertEquals("OBSERVING", itemLifecycle.transition(itemId, "CANONICAL_RECONCILED", "OPEN_OBSERVATION"));
        assertEquals("OBSERVATION_HEALTHY", itemLifecycle.transition(itemId, "OBSERVING", "INTERVAL_HEALTHY_SAFE"));
        assertEquals("COMPLETED", itemLifecycle.transition(itemId, "OBSERVATION_HEALTHY", "COMPLETE_ITEM"));
        assertEquals("COMPLETED", itemLifecycle.currentState(itemId));
        assertEquals("NOT_SENT", CampaignItemLifecycleService.destinationFor("PRE_SEND", "AUTHORITATIVE_NO_SEND"));
    }

    @Test
    void persistedObservationRequiresHealthyAndSafeAndDeniesStale() {
        var campaign = newCampaign("CELL-OBS-1", "idem-obs-1");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        UUID cohortId = jdbc.queryForObject("SELECT cohort_id FROM campaign_cohort WHERE campaign_id = ?", UUID.class, campaignId);
        Instant verified = Instant.parse("2026-02-01T00:00:00Z");
        CampaignException startBefore = assertThrows(CampaignException.class, () -> observationService.persistBoundary(
                campaignId, revisionId, cohortId, itemId, null, "CELL-OBS-1", "txPower",
                new BigDecimal("43"), verified, verified.minusSeconds(1), null,
                "src", verified, "PHASE12_CHECKPOINT", "ckpt", 1L, "ckpt",
                "p18-health-v1", "p18-observation-v1",
                CampaignObservationService.NetworkObservationHealth.HEALTHY,
                CampaignObservationService.OperationalSafetyHealth.SAFE
        ));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, startBefore.reasonCode());
        observationService.persistBoundary(
                campaignId, revisionId, cohortId, itemId, null, "CELL-OBS-1", "txPower",
                new BigDecimal("43"), verified, verified.plusSeconds(1), verified.plusSeconds(30),
                "src", verified.plusSeconds(1), "SOURCE_NATIVE_WATERMARK", "wm-1", 1L, "ckpt",
                "p18-health-v1", "p18-observation-v1",
                CampaignObservationService.NetworkObservationHealth.HEALTHY,
                CampaignObservationService.OperationalSafetyHealth.SAFE
        );
        observationService.assertCurrentHealthyAndSafe(campaignId);
        jdbc.update("UPDATE campaign_observation_boundary SET network_observation_health = 'STALE' WHERE campaign_id = ?", campaignId);
        CampaignException stale = assertThrows(CampaignException.class, () -> observationService.assertCurrentHealthyAndSafe(campaignId));
        assertEquals(ProductionReasonCode.OBSERVATION_NOT_HEALTHY, stale.reasonCode());
        CampaignException healthPass = assertThrows(
                CampaignException.class,
                () -> campaignService.applyCanaryHealthAndSafetyPass(campaignId)
        );
        assertEquals(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, healthPass.reasonCode());
    }

    @Test
    void proofFormAReconciliationAndPreexistingSameValueDeny() {
        Instant vendorVerified = Instant.parse("2026-06-01T00:00:00Z");
        CampaignException preexisting = assertThrows(CampaignException.class, () -> reconciliationReader.assertCanonicalReconciled(
                "CELL-001", "txPower", new BigDecimal("46"), vendorVerified, null, UUID.randomUUID().toString()
        ));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, preexisting.reasonCode());
        assertTrue(preexisting.getMessage().toLowerCase().contains("before")
                || preexisting.getMessage().toLowerCase().contains("checkpoint")
                || preexisting.getMessage().toLowerCase().contains("unavailable"));

        CampaignException wrongCell = assertThrows(CampaignException.class, () -> reconciliationReader.assertCanonicalReconciled(
                "CELL-DOES-NOT-EXIST", "txPower", new BigDecimal("43"), vendorVerified, null, "ckpt"
        ));
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, wrongCell.reasonCode());

        CampaignException wrongParam = assertThrows(CampaignException.class, () -> reconciliationReader.assertCanonicalReconciled(
                "CELL-001", "electricalTilt", new BigDecimal("46"), vendorVerified, null, "ckpt"
        ));
        assertEquals(ProductionReasonCode.UNSUPPORTED_OBJECT_OR_PARAMETER, wrongParam.reasonCode());

        UUID gnb = jdbc.queryForObject("SELECT gnb_id FROM cell WHERE cell_id = 'CELL-001'", UUID.class);
        UUID cellPk = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO cell (id, cell_id, name, gnb_id, technology, band, status) VALUES (?, 'CELL-P18-RECON', 'p18', ?, 'NR', 'n78', 'ACTIVE')",
                cellPk, gnb
        );
        Instant after = Instant.parse("2026-07-01T00:00:00Z");
        jdbc.update(
                "INSERT INTO radio_configuration (id, cell_id, parameter_name, parameter_value, unit, effective_from) VALUES (?, ?, 'txPower', '43', 'dBm', ?)",
                UUID.randomUUID(), cellPk, java.sql.Timestamp.from(after)
        );
        UUID ckpt = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO synchronization_checkpoint (
                    id, source_system, connector_id, synchronization_scope, checkpoint_type, checkpoint_value,
                    fencing_token, status, created_at, updated_at, last_observed_at)
                VALUES (?, 'ERICSSON_ENM', 'p18', 'CELL-P18-RECON', 'INCREMENTAL', 'v1', 1, 'VALID', NOW(), NOW(), ?)
                """,
                ckpt, java.sql.Timestamp.from(after)
        );
        reconciliationReader.assertCanonicalReconciled(
                "CELL-P18-RECON", "txPower", new BigDecimal("43"), vendorVerified, null, ckpt.toString()
        );
        CampaignException staleCkpt = assertThrows(CampaignException.class, () -> {
            jdbc.update("UPDATE synchronization_checkpoint SET status = 'EXPIRED' WHERE id = ?", ckpt);
            reconciliationReader.assertCanonicalReconciled(
                    "CELL-P18-RECON", "txPower", new BigDecimal("43"), vendorVerified, null, ckpt.toString()
            );
        });
        assertEquals(ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE, staleCkpt.reasonCode());
        jdbc.update("DELETE FROM radio_configuration WHERE cell_id = ?", cellPk);
        jdbc.update("DELETE FROM synchronization_checkpoint WHERE id = ?", ckpt);
        jdbc.update("DELETE FROM cell WHERE id = ?", cellPk);
    }

    @Test
    void externalInterferenceSameTargetCellParameter() {
        ExternalInterferenceService svc = interferenceService;
        assertTrue(!svc.isInterference("T2", "C1", "txPower", "T1", "C1", "txPower"));
        assertTrue(!svc.isInterference("T1", "C2", "txPower", "T1", "C1", "txPower"));
        assertTrue(!svc.isInterference("T1", "C1", "tilt", "T1", "C1", "txPower"));
        assertTrue(svc.isInterference("T1", "C1", "txPower", "T1", "C1", "txPower"));
        var campaign = newCampaign("CELL-INT-1", "idem-int-1");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        UUID cohortId = jdbc.queryForObject("SELECT cohort_id FROM campaign_cohort WHERE campaign_id = ?", UUID.class, campaignId);
        Instant verified = Instant.parse("2026-03-01T00:00:00Z");
        observationService.persistBoundary(
                campaignId, revisionId, cohortId, itemId, null, "CELL-INT-1", "txPower",
                new BigDecimal("43"), verified, verified.plusSeconds(1), verified.plusSeconds(3600),
                "src", verified.plusSeconds(1), "PHASE12_CHECKPOINT", "ckpt", 1L, "ckpt",
                "p18-health-v1", "p18-observation-v1",
                CampaignObservationService.NetworkObservationHealth.HEALTHY,
                CampaignObservationService.OperationalSafetyHealth.SAFE
        );
        Instant conflictAt = verified.plusSeconds(30);
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, requester_principal_id, audit_chain_integrity,
                    created_at, updated_at, version)
                VALUES (?, ?, ?, 'CC-X', 'AUTHORIZED', ?, 1, 'CELL-INT-1', 'txPower', 43, 43, 'ext', 'VALID', ?, ?, 0)
                """,
                UUID.randomUUID(), UUID.randomUUID(), TARGET_ID, "f".repeat(64),
                java.sql.Timestamp.from(conflictAt), java.sql.Timestamp.from(conflictAt)
        );
        CampaignException ex = assertThrows(CampaignException.class, () ->
                interferenceService.detectForActiveBoundary(campaignId, itemId, "CELL-INT-1", "txPower"));
        assertEquals(ProductionReasonCode.EXTERNAL_INTERFERENCE_DETECTED, ex.reasonCode());
    }

    @Test
    void resumptionSection224FromDurableState() {
        var released = authorizeReadyAndRelease("CELL-RES-1", "idem-res-1");
        UUID campaignId = released.getCampaignId();
        campaignService.transition(campaignId, "PAUSE_CAMPAIGN", actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-p");
        governResumptionToAuthorized(campaignId);
        String dest = resumptionService.makeEffective(campaignId, systemResumptionActor());
        assertEquals("PAUSED_FOR_RELEASE", dest);
        assertEquals("PAUSED_FOR_RELEASE", jdbc.queryForObject(
                "SELECT state FROM production_change_campaign WHERE campaign_id = ?", String.class, campaignId));
        assertEquals("PAUSED_FOR_RELEASE", jdbc.queryForObject(
                "SELECT destination_state FROM campaign_resumption WHERE campaign_id = ?", String.class, campaignId));

        var created = authorizeReadyAndRelease("CELL-RES-2", "idem-res-2");
        jdbc.update("UPDATE campaign_mutation_slot SET state = 'HELD_MAY_HAVE_SENT', holder_type = 'FORWARD' WHERE campaign_id = ?", created.getCampaignId());
        campaignService.transition(created.getCampaignId(), "PAUSE_CAMPAIGN", actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-2-p");
        governResumptionToAuthorized(created.getCampaignId());
        CampaignException unresolved = assertThrows(CampaignException.class, () ->
                resumptionService.makeEffective(created.getCampaignId(), systemResumptionActor()));
        assertEquals(ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED, unresolved.reasonCode());

        var closed = authorizeReadyAndRelease("CELL-RES-3", "idem-res-3");
        UUID closedItem = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, closed.getCampaignId());
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", closedItem);
        closureService.determineRecoveryRequired(closed.getCampaignId(), closedItem);
        jdbc.update(
                """
                INSERT INTO campaign_suspension (suspension_id, campaign_id, revision_id, suspension_type, state, reason_code, pre_state, created_at)
                SELECT ?, campaign_id, revision_id, 'OPERATOR_PAUSE', 'ACTIVE', 'PAUSE', 'CANARY_ACTIVE', NOW()
                  FROM campaign_revision WHERE campaign_id = ?
                """,
                UUID.randomUUID(), closed.getCampaignId()
        );
        jdbc.update("UPDATE production_change_campaign SET state = 'SUSPENDED' WHERE campaign_id = ?", closed.getCampaignId());
        jdbc.update("UPDATE campaign_execution_item SET state = 'COMPLETED' WHERE item_id = ?", closedItem);
        jdbc.update("UPDATE campaign_mutation_slot SET state = 'EMPTY', holder_type = 'NONE' WHERE campaign_id = ?", closed.getCampaignId());
        governResumptionToAuthorized(closed.getCampaignId());
        CampaignException closedEx = assertThrows(CampaignException.class, () ->
                resumptionService.makeEffective(closed.getCampaignId(), systemResumptionActor()));
        assertEquals(ProductionReasonCode.FORWARD_PROGRESSION_CLOSED, closedEx.reasonCode());

        var generic = authorizeReadyAndRelease("CELL-RES-4", "idem-res-4");
        campaignService.transition(generic.getCampaignId(), "PAUSE_CAMPAIGN", actor("pauser", CampaignPermission.CAMPAIGN_PAUSE), "idem-res-4-p");
        jdbc.update("UPDATE campaign_suspension SET pre_state = 'SUSPENDED' WHERE campaign_id = ?", generic.getCampaignId());
        governResumptionToAuthorized(generic.getCampaignId());
        CampaignException genericEx = assertThrows(CampaignException.class, () ->
                resumptionService.makeEffective(generic.getCampaignId(), systemResumptionActor()));
        assertEquals(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, genericEx.reasonCode());
        assertTrue(genericEx.getMessage().contains("no generic resume"));
    }

    @Test
    void recoveryCrossLayerSodAndZeroRowFailClosed() {
        var campaign = newCampaign("CELL-REC-1", "idem-rec-1");
        UUID campaignId = campaign.getCampaignId();
        CampaignException zero = assertThrows(CampaignException.class, () ->
                recoveryService.request(campaignId, actor("req-r", CampaignPermission.CAMPAIGN_RECOVERY_REQUEST)));
        assertEquals(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, zero.reasonCode());
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", itemId);
        closureService.determineRecoveryRequired(campaignId, itemId);
        recoveryService.request(campaignId, actor("req-r", CampaignPermission.CAMPAIGN_RECOVERY_REQUEST));
        CampaignException second = assertThrows(CampaignException.class, () ->
                recoveryService.request(campaignId, actor("req-r2", CampaignPermission.CAMPAIGN_RECOVERY_REQUEST)));
        assertEquals(ProductionReasonCode.INVALID_RECOVERY_TRANSITION, second.reasonCode());
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, requester_principal_id, authorizer_principal_id, executor_principal_id,
                    cell_id, parameter, expected_value, desired_value, audit_chain_integrity,
                    created_at, updated_at, version)
                SELECT ?, ?, production_target_id, 'CC-SOD', 'AUTHORIZED', 'p16-req', 'auth-r', 'auth-r',
                       'CELL-REC-1', 'txPower', 46, 43, 'VALID', NOW(), NOW(), 0
                  FROM production_change_campaign WHERE campaign_id = ?
                """,
                UUID.randomUUID(), UUID.randomUUID(), campaignId
        );
        UUID changeId = jdbc.queryForObject(
                "SELECT production_change_id FROM production_network_change WHERE cell_id = 'CELL-REC-1'",
                UUID.class
        );
        jdbc.update("UPDATE campaign_execution_item SET production_change_id = ? WHERE item_id = ?", changeId, itemId);
        jdbc.update(
                """
                INSERT INTO production_execution_rollback (
                    rollback_id, production_change_id, status, authorizer_principal_id, created_at, updated_at)
                VALUES (?, ?, 'AUTHORIZED', 'auth-r', NOW(), NOW())
                """,
                UUID.randomUUID(), changeId
        );
        recoveryService.startReview(campaignId, actor("rev-r", CampaignPermission.CAMPAIGN_RECOVERY_REVIEW));
        CampaignException sod = assertThrows(CampaignException.class, () ->
                recoveryService.authorize(campaignId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE)));
        assertEquals(ProductionReasonCode.PRODUCTION_SOD_VIOLATION, sod.reasonCode());
    }

    @Test
    void vendorRejectedAfterMayHaveSentDoesNotRewrite() {
        var campaign = newCampaign("CELL-VR-1", "idem-vr-1");
        UUID campaignId = campaign.getCampaignId();
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", itemId);
        jdbc.update("UPDATE campaign_mutation_slot SET state = 'HELD_MAY_HAVE_SENT', holder_type = 'FORWARD' WHERE campaign_id = ?", campaignId);
        CampaignException ex = assertThrows(CampaignException.class, () ->
                vendorRejectedSemantics.applyDurable(campaignId, itemId, true));
        assertEquals(ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED, ex.reasonCode());
        assertEquals("MAY_HAVE_SENT", jdbc.queryForObject(
                "SELECT state FROM campaign_execution_item WHERE item_id = ?", String.class, itemId));
        assertEquals("HELD_MAY_HAVE_SENT", jdbc.queryForObject(
                "SELECT state FROM campaign_mutation_slot WHERE campaign_id = ?", String.class, campaignId));
    }

    @Test
    void completionRequiresFrozenSuccessfulForwardEvidence() {
        var campaign = newCampaign("CELL-CMP-1", "idem-cmp-1");
        UUID campaignId = campaign.getCampaignId();
        jdbc.update("UPDATE production_change_campaign SET state = 'FINAL_OBSERVATION' WHERE campaign_id = ?", campaignId);
        CampaignException missing = assertThrows(CampaignException.class, () -> campaignService.completeCampaign(campaignId));
        assertEquals(ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE, missing.reasonCode());
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        jdbc.update("UPDATE campaign_execution_item SET state = 'VENDOR_ACCEPTED' WHERE item_id = ?", itemId);
        CampaignException vendorOnly = assertThrows(CampaignException.class, () -> campaignService.completeCampaign(campaignId));
        assertEquals(ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE, vendorOnly.reasonCode());
        jdbc.update("UPDATE campaign_execution_item SET state = 'COMPLETED' WHERE item_id = ?", itemId);
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        UUID cohortId = jdbc.queryForObject("SELECT cohort_id FROM campaign_cohort WHERE campaign_id = ?", UUID.class, campaignId);
        Instant verified = Instant.parse("2026-04-01T00:00:00Z");
        observationService.persistBoundary(
                campaignId, revisionId, cohortId, itemId, null, "CELL-CMP-1", "txPower",
                new BigDecimal("43"), verified, verified.plusSeconds(1), verified.plusSeconds(3600),
                "src", verified.plusSeconds(1), "PHASE12_CHECKPOINT", "ckpt-missing", 1L, "ckpt-missing",
                "p18-health-v1", "p18-observation-v1",
                CampaignObservationService.NetworkObservationHealth.HEALTHY,
                CampaignObservationService.OperationalSafetyHealth.SAFE
        );
        CampaignException proof = assertThrows(CampaignException.class, () -> campaignService.completeCampaign(campaignId));
        assertTrue(proof.reasonCode() == ProductionReasonCode.RECONCILIATION_PROVENANCE_UNAVAILABLE
                || proof.reasonCode() == ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE);
    }

    @Test
    void bindPhase16IfPresentAdvancesHandoffPendingToHandedOff() {
        var released = authorizeReadyAndRelease("CELL-BIND-1", "idem-bind-1");
        UUID campaignId = released.getCampaignId();
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String handoffId = orchestration.commitForwardHandoff(campaignId, itemId);
        assertEquals("HANDOFF_PENDING", itemLifecycle.currentState(itemId));
        UUID changeId = UUID.randomUUID();
        UUID phase15 = jdbc.queryForObject("SELECT phase15_execution_id FROM campaign_execution_item WHERE item_id = ?", UUID.class, itemId);
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, audit_chain_integrity, created_at, updated_at, version,
                    execution_origin, campaign_handoff_id)
                SELECT ?, ?, production_target_id, 'CC-BIND', 'AUTHORIZED', ?, 1, 'CELL-BIND-1', 'txPower',
                       46, 43, 46, 46, 'p16-req', 'VALID', NOW(), NOW(), 0, 'PRODUCTION_CAMPAIGN', ?
                  FROM production_change_campaign WHERE campaign_id = ?
                """,
                changeId, phase15, "c".repeat(64), handoffId, campaignId
        );
        assertTrue(orchestration.bindPhase16IfPresent(handoffId));
        assertEquals("HANDED_OFF", itemLifecycle.currentState(itemId));
        assertEquals(changeId, jdbc.queryForObject(
                "SELECT production_change_id FROM campaign_execution_handoff WHERE campaign_handoff_id = ?",
                UUID.class,
                handoffId
        ));
    }

    @Test
    void governedItemMachineRefusesUnspecifiedPostHandoffJump() {
        var campaign = newCampaign("CELL-ITEM-1", "idem-item-1");
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaign.getCampaignId());
        jdbc.update("UPDATE campaign_execution_item SET state = 'HANDED_OFF' WHERE item_id = ?", itemId);
        CampaignException skip = assertThrows(CampaignException.class, () ->
                itemLifecycle.transition(itemId, "HANDED_OFF", "COMPLETE_ITEM"));
        assertEquals(ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION, skip.reasonCode());
        assertEquals("PRE_SEND", governedProgression.markPreSend(itemId));
        assertEquals("MAY_HAVE_SENT", governedProgression.recordGatewaySendBoundary(itemId));
    }

    @Test
    void recoveryAcquireWhileOpenDeniedAndSerializedRecoveryRequiresClosure() {
        var campaign = newCampaign("CELL-REC-OPEN-1", "idem-rec-open-1");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = jdbc.queryForObject("SELECT revision_id FROM campaign_revision WHERE campaign_id = ?", UUID.class, campaignId);
        CampaignException open = assertThrows(CampaignException.class, () ->
                slotService.acquireRecovery(campaignId, revisionId, null, 1L, 1L));
        assertEquals(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, open.reasonCode());
        CampaignException begin = assertThrows(CampaignException.class, () ->
                recoveryService.beginSerializedRecovery(
                        campaignId, revisionId, null,
                        actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE), 1L, 1L));
        assertEquals(ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE, begin.reasonCode());
    }

    @Test
    void resumptionFromCanaryActiveWithUnusedStaleReleaseDoesNotVerify() {
        var released = authorizeReadyAndRelease("CELL-RES-CA-1", "idem-res-ca");
        UUID campaignId = released.getCampaignId();
        jdbc.update("UPDATE campaign_cohort_release SET state = 'STALE' WHERE campaign_id = ?", campaignId);
        jdbc.update(
                """
                INSERT INTO campaign_suspension (suspension_id, campaign_id, revision_id, suspension_type, state, reason_code, pre_state, created_at)
                SELECT ?, campaign_id, revision_id, 'OPERATOR_PAUSE', 'ACTIVE', 'PAUSE', 'CANARY_ACTIVE', NOW()
                  FROM campaign_revision WHERE campaign_id = ?
                """,
                UUID.randomUUID(), campaignId
        );
        jdbc.update("UPDATE production_change_campaign SET state = 'SUSPENDED' WHERE campaign_id = ?", campaignId);
        governResumptionToAuthorized(campaignId);
        String dest = resumptionService.makeEffective(campaignId, systemResumptionActor());
        assertEquals("PAUSED_FOR_RELEASE", dest);
    }

    @Test
    void campaignFingerprintMutationSensitivity() {
        Map<String, Object> base = CampaignFingerprintFactory.material();
        base.put("objective", "o1");
        base.put("ticket", "t1");
        base.put("vendor", "ERICSSON");
        String original = CampaignFingerprintFactory.hash(base);
        for (String key : java.util.List.of("objective", "ticket", "vendor")) {
            Map<String, Object> mutated = CampaignFingerprintFactory.copy(base);
            mutated.put(key, String.valueOf(base.get(key)) + "-x");
            assertNotEquals(original, CampaignFingerprintFactory.hash(mutated), key);
        }
        var campaign = newCampaign("CELL-FP-1", "idem-fp-1");
        var other = campaignService.create(
                TARGET_ID, "different-objective", "CC-FP-DIFF", java.util.List.of(item("CELL-FP-2")),
                actor("creator-1", CampaignPermission.CAMPAIGN_CREATE), "idem-fp-2"
        );
        assertNotEquals(campaign.getFingerprint(), other.getFingerprint());
    }

    @Test
    void preSendValidationDeniesClosedAndWrongHolder() {
        var released = authorizeReadyAndRelease("CELL-PS-1", "idem-ps-1");
        UUID campaignId = released.getCampaignId();
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        String hid = orchestration.commitForwardHandoff(campaignId, itemId);
        jdbc.update("UPDATE campaign_forward_progression_closure SET state = 'CLOSED', closed_at = NOW() WHERE campaign_id = ?", campaignId);
        CampaignException closed = assertThrows(CampaignException.class, () -> preSendValidation.validateCampaignOriginSend(hid));
        assertEquals(ProductionReasonCode.FORWARD_PROGRESSION_CLOSED, closed.reasonCode());
    }

    private HttpHeaders commandHeaders(String permission, String actor) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.add(HeaderAuthenticatedActorProvider.PERMISSION_HEADER, permission);
        headers.add(HeaderAuthenticatedActorProvider.ACTOR_HEADER, actor);
        headers.add(HeaderAuthenticatedActorProvider.SOURCE_HEADER, SOURCE);
        headers.add(HeaderAuthenticatedActorProvider.ACTOR_TYPE_HEADER, "HUMAN");
        headers.add("Idempotency-Key", "idem-cmd-" + UUID.randomUUID());
        return headers;
    }
}
