package com.simba.snip.npo.productioncampaign;

import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.service.CampaignGenerationService;
import com.simba.snip.npo.productioncampaign.service.CampaignMutationSlotService;
import com.simba.snip.npo.productioncampaign.service.CampaignObservationService;
import com.simba.snip.npo.productioncampaign.service.CampaignRecoveryService;
import com.simba.snip.npo.productioncampaign.service.CampaignSafetyService;
import com.simba.snip.npo.productioncampaign.service.ForwardProgressionClosureService;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampaignMutationSlotConcurrencyIT extends ProductionCampaignITSupport {

    @Autowired CampaignMutationSlotService slotService;
    @Autowired CampaignSafetyService safetyService;
    @Autowired CampaignGenerationService generationService;
    @Autowired CampaignRecoveryService recoveryService;
    @Autowired ForwardProgressionClosureService closureService;
    @Autowired CampaignObservationService observationService;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void simultaneousForwardAcquisitions() throws Exception {
        var campaign = newCampaign("CELL-SLOT-1", "idem-slot-1");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        UUID itemA = null;
        UUID itemB = null;
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger losses = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> f1 = pool.submit(() -> raceAcquire(campaignId, revisionId, itemA, barrier, wins, losses));
            Future<?> f2 = pool.submit(() -> raceAcquire(campaignId, revisionId, itemB, barrier, wins, losses));
            f1.get(30, TimeUnit.SECONDS);
            f2.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, wins.get());
        assertEquals(1, losses.get());
        assertEquals(1, slotService.activeMutations(campaignId));
    }

    @Test
    void forwardVersusRecoveryAcquisition() throws Exception {
        var campaign = newCampaign("CELL-SLOT-2", "idem-slot-2");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        UUID forwardItem = null;
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger forwardWins = new AtomicInteger();
        AtomicInteger recoveryWins = new AtomicInteger();
        AtomicInteger recoveryClosureDenials = new AtomicInteger();
        AtomicInteger slotDenials = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> forward = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                try {
                    slotService.acquireForward(campaignId, revisionId, forwardItem, 1L, 1L);
                    forwardWins.incrementAndGet();
                } catch (CampaignException ex) {
                    assertEquals(ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE, ex.reasonCode());
                    slotDenials.incrementAndGet();
                }
                return null;
            });
            Future<?> recovery = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                try {
                    slotService.acquireRecovery(campaignId, revisionId, null, 1L, 1L);
                    recoveryWins.incrementAndGet();
                } catch (CampaignException ex) {
                    if (ex.reasonCode() == ProductionReasonCode.RECOVERY_REQUIRES_FORWARD_CLOSURE) {
                        recoveryClosureDenials.incrementAndGet();
                    } else if (ex.reasonCode() == ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE) {
                        slotDenials.incrementAndGet();
                    } else {
                        throw ex;
                    }
                }
                return null;
            });
            forward.get(30, TimeUnit.SECONDS);
            recovery.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(0, recoveryWins.get());
        assertTrue(recoveryClosureDenials.get() >= 1);
        assertEquals(1, slotService.activeMutations(campaignId));
        assertEquals(1, forwardWins.get());
    }

    @Test
    void crashBetweenReservationAndSlotPersistence() {
        var campaign = newCampaign("CELL-SLOT-4", "idem-slot-4");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        UUID reservation = safetyService.reserve(campaignId, revisionId, null, "FORWARD");
        assertEquals(0, slotService.activeMutations(campaignId));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            slotService.acquire(campaignId, "FORWARD", null, reservation, 1L, 1L);
            throw new IllegalStateException("injected crash after slot acquire, before caller commit");
        }));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_safety_budget_reservation WHERE campaign_id = ? AND state = 'RESERVED'",
                Integer.class,
                campaignId
        ));
        assertEquals(0, slotService.activeMutations(campaignId));
        slotService.acquire(campaignId, "FORWARD", null, reservation, 1L, 1L);
        assertEquals(1, slotService.activeMutations(campaignId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_mutation_slot WHERE campaign_id = ? AND reservation_id = ?",
                Integer.class,
                campaignId,
                reservation
        ));
    }

    @Test
    void duplicateMayHaveSentProcessing() throws Exception {
        var campaign = newCampaign("CELL-SLOT-3", "idem-slot-3");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        int revisionNumber = campaign.getCurrentRevisionNumber();
        UUID reservation = slotService.acquireForward(campaignId, revisionId, null, 1L, 1L);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger accounted = new AtomicInteger();
        AtomicInteger denied = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> raceConsume(campaignId, revisionNumber, reservation, barrier, accounted, denied));
            Future<?> b = pool.submit(() -> raceConsume(campaignId, revisionNumber, reservation, barrier, accounted, denied));
            a.get(30, TimeUnit.SECONDS);
            b.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, accounted.get());
        assertEquals(1, denied.get());
        assertEquals(1, safetyService.distinctCellsExposed(campaignId, revisionNumber));
    }

    @Test
    void crashDuringAtomicAcquireForwardRollsBackReservationAndSlot() {
        var campaign = newCampaign("CELL-SLOT-4B", "idem-slot-4b");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            slotService.acquireForward(campaignId, revisionId, null, 1L, 1L);
            throw new IllegalStateException("injected crash after atomic reserve+slot");
        }));
        Integer reservations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_safety_budget_reservation WHERE campaign_id = ?",
                Integer.class,
                campaignId
        );
        assertEquals(0, reservations);
        assertEquals(0, slotService.activeMutations(campaignId));
    }

    @Test
    void recoveryHolderRebind() {
        var campaign = newCampaign("CELL-SLOT-5", "idem-slot-5");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        slotService.acquireForward(campaignId, revisionId, itemId, 1L, 1L);
        jdbc.update("UPDATE campaign_execution_item SET state = 'MAY_HAVE_SENT' WHERE item_id = ?", itemId);
        closureService.determineRecoveryRequired(campaignId, itemId);
        recoveryService.request(campaignId, actor("req-r", CampaignPermission.CAMPAIGN_RECOVERY_REQUEST));
        recoveryService.startReview(campaignId, actor("rev-r", CampaignPermission.CAMPAIGN_RECOVERY_REVIEW));
        recoveryService.authorize(campaignId, actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE));
        insertP16Rollback(campaignId, "p16-rollback-auth");
        recoveryService.rebindHolderToRecovery(
                campaignId,
                revisionId,
                actor("auth-r", CampaignPermission.CAMPAIGN_RECOVERY_AUTHORIZE),
                1L,
                1L
        );
        var snap = slotService.snapshot(campaignId);
        assertEquals("RECOVERY", snap.holderType());
        assertEquals(1, slotService.activeMutations(campaignId));
        String closure = jdbc.queryForObject(
                "SELECT state FROM campaign_forward_progression_closure WHERE campaign_id = ?",
                String.class,
                campaignId
        );
        assertEquals("CLOSED", closure);
    }

    @Test
    void suspensionControlGenerationRace() throws Exception {
        var campaign = newCampaign("CELL-SLOT-6", "idem-slot-6");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger staleDenials = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> acquire = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                try {
                    slotService.acquireForward(campaignId, revisionId, null, 1L, 1L);
                    slotService.assertCurrentFenceAndGeneration(campaignId, 1L, 1L);
                } catch (CampaignException ex) {
                    assertTrue(ex.reasonCode() == ProductionReasonCode.MUTATION_SLOT_STALE
                            || ex.reasonCode() == ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE);
                    staleDenials.incrementAndGet();
                }
                return null;
            });
            Future<?> suspend = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                generationService.invalidateControlGeneration(campaignId);
                return null;
            });
            acquire.get(30, TimeUnit.SECONDS);
            suspend.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        long live = generationService.currentControlGeneration(campaignId);
        assertTrue(live >= 2);
        if (slotService.activeMutations(campaignId) == 1) {
            assertThrows(CampaignException.class, () -> slotService.assertCurrentFenceAndGeneration(campaignId, 1L, 1L));
        } else {
            assertTrue(staleDenials.get() >= 1);
        }
    }

    @Test
    void serializationReleaseRacingNewAcquisition() throws Exception {
        var campaign = newCampaign("CELL-SLOT-7", "idem-slot-7");
        UUID campaignId = campaign.getCampaignId();
        UUID revisionId = revisionId(campaignId);
        UUID itemId = jdbc.queryForObject("SELECT item_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        UUID cohortId = jdbc.queryForObject("SELECT cohort_id FROM campaign_execution_item WHERE campaign_id = ?", UUID.class, campaignId);
        slotService.acquireForward(campaignId, revisionId, itemId, 1L, 1L);
        slotService.markMayHaveSent(campaignId, "FORWARD");
        jdbc.update("UPDATE campaign_execution_item SET state = 'COMPLETED' WHERE item_id = ?", itemId);
        Instant verified = Instant.parse("2026-01-01T00:00:00Z");
        observationService.persistBoundary(
                campaignId, revisionId, cohortId, itemId, null, "CELL-SLOT-7", "txPower",
                new BigDecimal("43"), verified, verified.plusSeconds(1), verified.plusSeconds(60),
                "kpi-source", verified.plusSeconds(1), "PHASE12_CHECKPOINT", "ckpt-1", 1000L, "ckpt-1",
                "p18-health-v1", "p18-observation-v1",
                CampaignObservationService.NetworkObservationHealth.HEALTHY,
                CampaignObservationService.OperationalSafetyHealth.SAFE
        );
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicInteger acquireWins = new AtomicInteger();
        try {
            Future<?> release = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                slotService.releaseSerialization(campaignId, "FORWARD");
                return null;
            });
            Future<?> acquire = pool.submit(() -> {
                barrier.await(30, TimeUnit.SECONDS);
                try {
                    slotService.acquireForward(campaignId, revisionId, null, 1L, 1L);
                    acquireWins.incrementAndGet();
                } catch (CampaignException ex) {
                    assertEquals(ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE, ex.reasonCode());
                }
                return null;
            });
            release.get(30, TimeUnit.SECONDS);
            acquire.get(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        var snap = slotService.snapshot(campaignId);
        int active = slotService.activeMutations(campaignId);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_mutation_slot WHERE campaign_id = ?", Integer.class, campaignId));
        assertTrue(active <= 1);
        if (acquireWins.get() == 1) {
            assertEquals(1, active);
            assertEquals("ACQUIRED", snap.state());
            assertEquals("FORWARD", snap.holderType());
        } else {
            assertEquals(0, acquireWins.get());
            assertEquals(0, active);
            assertEquals("EMPTY", snap.state());
            assertEquals("NONE", snap.holderType());
        }
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_mutation_slot WHERE campaign_id = ? AND state = 'HELD_MAY_HAVE_SENT' AND holder_type = 'NONE'",
                Integer.class,
                campaignId
        ));
    }

    private void raceAcquire(
            UUID campaignId,
            UUID revisionId,
            UUID itemId,
            CyclicBarrier barrier,
            AtomicInteger wins,
            AtomicInteger losses
    ) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
            slotService.acquireForward(campaignId, revisionId, itemId, 1L, 1L);
            wins.incrementAndGet();
        } catch (CampaignException ex) {
            if (ex.reasonCode() == ProductionReasonCode.MUTATION_SLOT_UNAVAILABLE) {
                losses.incrementAndGet();
            } else {
                throw ex;
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void raceConsume(
            UUID campaignId,
            int revisionNumber,
            UUID reservation,
            CyclicBarrier barrier,
            AtomicInteger accounted,
            AtomicInteger denied
    ) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
            safetyService.consumeMayHaveSent(campaignId, revisionNumber, reservation, "FORWARD", "CELL-SLOT-3", true);
            accounted.incrementAndGet();
        } catch (CampaignException ex) {
            assertEquals(ProductionReasonCode.SAFETY_EXPOSURE_ALREADY_ACCOUNTED, ex.reasonCode());
            denied.incrementAndGet();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void insertP16Rollback(UUID campaignId, String authorizer) {
        UUID changeId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO production_network_change (
                    production_change_id, phase15_execution_id, production_target_id, change_control_reference,
                    status, production_fingerprint, authorization_generation, cell_id, parameter,
                    expected_value, desired_value, rollback_expected_value, rollback_desired_value,
                    requester_principal_id, authorizer_principal_id, executor_principal_id,
                    audit_chain_integrity, created_at, updated_at, version)
                SELECT ?, ?, production_target_id, 'CC-RB', 'AUTHORIZED', ?, 1, 'CELL-SLOT-5', 'txPower',
                       46, 43, 46, 43, 'p16-req', ?, 'p16-exec', 'VALID', NOW(), NOW(), 0
                  FROM production_change_campaign WHERE campaign_id = ?
                """,
                changeId, UUID.randomUUID(), "c".repeat(64), authorizer, campaignId
        );
        jdbc.update(
                "UPDATE campaign_execution_item SET production_change_id = ? WHERE campaign_id = ?",
                changeId, campaignId
        );
        jdbc.update(
                """
                INSERT INTO production_execution_rollback (
                    rollback_id, production_change_id, status, rollback_fingerprint, authorization_generation,
                    requester_principal_id, reviewer_principal_id, authorizer_principal_id, created_at, updated_at)
                VALUES (?, ?, 'AUTHORIZED', ?, 1, 'p16-req', 'p16-rev', ?, NOW(), NOW())
                """,
                UUID.randomUUID(), changeId, "d".repeat(64), authorizer
        );
    }

    private UUID revisionId(UUID campaignId) {
        return jdbc.queryForObject(
                "SELECT revision_id FROM campaign_revision WHERE campaign_id = ?",
                UUID.class,
                campaignId
        );
    }
}
