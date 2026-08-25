package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.domain.ImportBusyException;
import com.simba.snip.npo.integration.FixtureKind;
import com.simba.snip.npo.integration.ImportFaultInjector;
import com.simba.snip.npo.integration.ImportLease;
import com.simba.snip.npo.integration.ImportLeaseService;
import com.simba.snip.npo.integration.IntegrationRuntimeIdentity;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.integration.Vendor;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.persist.SiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
class IntegrationRuntimeHardeningTest extends AbstractPostgresIT {

    @Autowired
    private NetworkImportService importService;

    @Autowired
    private ImportLeaseService leaseService;

    @Autowired
    private ImportFaultInjector faultInjector;

    @Autowired
    private IntegrationRuntimeIdentity identity;

    @Autowired
    private NetworkImportBatchRepository batchRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void independentScopesCanOwnLeasesConcurrently() throws Exception {
        UUID ericssonExecution = UUID.randomUUID();
        persistRequested(ericssonExecution, "ERICSSON_FIXTURE", "ERICSSON");
        UUID nokiaExecution = UUID.randomUUID();
        persistRequested(nokiaExecution, "NOKIA_FIXTURE", "NOKIA");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ImportLease> ericsson = pool.submit(() -> leaseService.acquire(
                    "ERICSSON_FIXTURE", "DEFAULT", ericssonExecution, identity.instanceId()).orElseThrow());
            Future<ImportLease> nokia = pool.submit(() -> leaseService.acquire(
                    "NOKIA_FIXTURE", "DEFAULT", nokiaExecution, identity.instanceId()).orElseThrow());
            ImportLease left = ericsson.get(10, TimeUnit.SECONDS);
            ImportLease right = nokia.get(10, TimeUnit.SECONDS);
            assertNotEquals(left.leaseKey(), right.leaseKey());
            assertEquals(ericssonExecution, left.ownerExecutionId());
            assertEquals(nokiaExecution, right.ownerExecutionId());
        } finally {
            leaseService.find("ERICSSON_FIXTURE", "DEFAULT").ifPresent(leaseService::release);
            leaseService.find("NOKIA_FIXTURE", "DEFAULT").ifPresent(leaseService::release);
            pool.shutdownNow();
        }
    }

    @Test
    void sameScopeHasOneMutatingOwnerAndStaleTokenCannotCommit() {
        UUID firstId = UUID.randomUUID();
        persistRequested(firstId, "ERICSSON_FIXTURE", "ERICSSON");
        ImportLease first = leaseService.acquire("ERICSSON_FIXTURE", "DEFAULT", firstId, identity.instanceId()).orElseThrow();
        try {
            UUID secondId = UUID.randomUUID();
            persistRequested(secondId, "ERICSSON_FIXTURE", "ERICSSON");
            assertTrue(leaseService.acquire("ERICSSON_FIXTURE", "DEFAULT", secondId, identity.instanceId()).isEmpty());
            jdbc.update("UPDATE network_import_lease SET expires_at = ? WHERE lease_key = ?",
                    java.sql.Timestamp.from(Instant.now().minusSeconds(5)), first.leaseKey());
            leaseService.recoverExpired("ERICSSON_FIXTURE", "DEFAULT");
            UUID successorId = UUID.randomUUID();
            persistRequested(successorId, "ERICSSON_FIXTURE", "ERICSSON");
            ImportLease successor = leaseService.acquire(
                    "ERICSSON_FIXTURE", "DEFAULT", successorId, identity.instanceId()).orElseThrow();
            assertTrue(successor.fencingToken() > first.fencingToken());
            var lost = assertThrows(RuntimeException.class, () -> leaseService.assertOwnership(first));
            assertTrue(lost.getMessage().toLowerCase().contains("lease"));
            leaseService.release(first);
            assertTrue(leaseService.find("ERICSSON_FIXTURE", "DEFAULT").isPresent());
            leaseService.release(successor);
        } finally {
            leaseService.find("ERICSSON_FIXTURE", "DEFAULT").ifPresent(leaseService::release);
        }
    }

    @Test
    void snapshotIdContentMismatchIsRejectedWithoutCanonicalMutation() {
        importService.importVendor(Vendor.ERICSSON, FixtureKind.IDENTITY_BASE, true);
        String before = siteRepository.findBySiteId("SITE-E-IDENT").orElseThrow().getName();
        String txBefore = jdbc.queryForObject(
                "SELECT parameter_value FROM radio_configuration r JOIN cell c ON c.id = r.cell_id WHERE c.cell_id='CELL-E-IDENT' AND r.parameter_name='txPower'",
                String.class);
        NetworkImportBatchEntity rejected = importService.importVendor(
                Vendor.ERICSSON, FixtureKind.CONTENT_MISMATCH, true);
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("SNAPSHOT_ID_CONTENT_MISMATCH", rejected.getFailureCode());
        assertEquals(Boolean.FALSE, rejected.getRetryable());
        assertEquals(before, siteRepository.findBySiteId("SITE-E-IDENT").orElseThrow().getName());
        assertEquals(txBefore, jdbc.queryForObject(
                "SELECT parameter_value FROM radio_configuration r JOIN cell c ON c.id = r.cell_id WHERE c.cell_id='CELL-E-IDENT' AND r.parameter_name='txPower'",
                String.class));
    }

    @Test
    void retryCreatesNewAttemptAndForcedCommitFailureRollsBackCanonicalState() {
        faultInjector.failNextCanonicalCommit();
        NetworkImportBatchEntity first = importService.importVendor(Vendor.ERICSSON, FixtureKind.COMMIT_FAIL, true);
        assertEquals("FAILED", first.getStatus());
        assertEquals("DATABASE_COMMIT_FAILED", first.getFailureCode());
        assertEquals(Boolean.TRUE, first.getRetryable());
        assertEquals(1, first.getAttemptNumber());
        assertTrue(siteRepository.findBySiteId("SITE-E-RETRY").isEmpty());
        NetworkImportBatchEntity retry = importService.importVendor(Vendor.ERICSSON, FixtureKind.COMMIT_FAIL, true);
        assertEquals("RETRY", retry.getExecutionType());
        assertEquals(2, retry.getAttemptNumber());
        assertEquals(first.getId(), retry.getPreviousExecutionId());
        assertEquals("COMPLETED", retry.getStatus());
        assertEquals(first.getStatus(), "FAILED");
        assertTrue(siteRepository.findBySiteId("SITE-E-RETRY").isPresent());
        NetworkImportBatchEntity persistedFirst = batchRepository.findById(first.getId()).orElseThrow();
        assertEquals("FAILED", persistedFirst.getStatus());
        assertEquals(1, persistedFirst.getAttemptNumber());
    }

    @Test
    void abandonedRunningExecutionIsRecoveredOnDemand() {
        UUID abandonedId = UUID.randomUUID();
        persistRequested(abandonedId, "ERICSSON_FIXTURE", "ERICSSON");
        jdbc.update("UPDATE network_import_batch SET status='RUNNING', lease_fencing_token=99 WHERE id = ?", abandonedId);
        int recovered = leaseService.recoverExpired("ERICSSON_FIXTURE", "DEFAULT");
        assertTrue(recovered >= 1);
        NetworkImportBatchEntity recoveredExecution = batchRepository.findById(abandonedId).orElseThrow();
        assertEquals("FAILED", recoveredExecution.getStatus());
        assertEquals("LEASE_EXPIRED", recoveredExecution.getFailureCode());
    }

    @Test
    void sameScopeConcurrentImportRejectsTheSecondCaller() throws Exception {
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        faultInjector.armLeaseHeld(held);
        faultInjector.armHoldUntil(releaseFirst);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<NetworkImportBatchEntity> first = pool.submit(
                    () -> importService.importVendor(Vendor.ERICSSON, FixtureKind.DELAY, true));
            assertTrue(held.await(10, TimeUnit.SECONDS));
            Future<NetworkImportBatchEntity> second = pool.submit(
                    () -> importService.importVendor(Vendor.ERICSSON, FixtureKind.DELAY, true));
            Exception secondError = assertThrows(Exception.class, () -> second.get(10, TimeUnit.SECONDS));
            assertTrue(secondError.getCause() instanceof ImportBusyException);
            releaseFirst.countDown();
            NetworkImportBatchEntity completed = first.get(15, TimeUnit.SECONDS);
            assertEquals("COMPLETED", completed.getStatus());
            assertEquals("NEW", completed.getExecutionType());
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    private UUID persistRequested(UUID id, String sourceSystem, String vendor) {
        jdbc.update(
                """
                INSERT INTO network_import_batch (
                    id, source_system, vendor, source_snapshot_id, vendor_schema_version, fixture_kind,
                    started_at, status, entities_read, entities_created, entities_updated, entities_unchanged,
                    entities_rejected, conflicts_detected, missing_entities_detected, execution_type,
                    attempt_number, source_scope, requested_at, owner_instance_id
                ) VALUES (?, ?, ?, 'UNREAD', 'TEST', 'NORMAL', ?, 'REQUESTED', 0, 0, 0, 0, 0, 0, 0, 'NEW', 1, 'DEFAULT', ?, ?)
                """,
                id,
                sourceSystem,
                vendor,
                java.sql.Timestamp.from(Instant.now()),
                java.sql.Timestamp.from(Instant.now()),
                identity.instanceId()
        );
        return id;
    }
}
