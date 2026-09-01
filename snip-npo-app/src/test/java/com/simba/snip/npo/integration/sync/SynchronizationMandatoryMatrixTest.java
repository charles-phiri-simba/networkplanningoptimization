package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.api.ImportBatchDto;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportRuntimeException;
import com.simba.snip.npo.integration.NetworkImportQueryService;
import com.simba.snip.npo.integration.enm.EnmConnectorMetrics;
import com.simba.snip.npo.integration.enm.EnmImportTestHooks;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.enm.EricssonEnmConnector;
import com.simba.snip.npo.integration.security.ConnectorAccessMode;
import com.simba.snip.npo.integration.security.ConnectorDescriptor;
import com.simba.snip.npo.integration.security.ConnectorImplementationType;
import com.simba.snip.npo.integration.Vendor;
import com.simba.snip.npo.integration.security.ConnectorCapability;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import com.simba.snip.npo.integration.security.ConnectorSecurityException;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkDriftObservationRepository;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.NetworkSourceReferenceRepository;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import com.simba.snip.npo.persist.SynchronizationCheckpointRepository;
import com.simba.snip.npo.persist.SynchronizationSourceStateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SynchronizationMandatoryMatrixTest extends AbstractPostgresIT {

    private static final String CONNECTOR = ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER;
    private static final String SOURCE = "ERICSSON_ENM_SIMULATOR";
    private static final String SCOPE = "DEFAULT";

    @Autowired private SynchronizationControlPlane controlPlane;
    @Autowired private SynchronizationImportService importService;
    @Autowired private SynchronizationQueryService queryService;
    @Autowired private SynchronizationCheckpointService checkpointService;
    @Autowired private SynchronizationModeSelector modeSelector;
    @Autowired private NetworkDriftService driftService;
    @Autowired private VendorImportAuthorizer authorizer;
    @Autowired private SimulatorEnmScenarioController scenarios;
    @Autowired private SimulatorEnmSyncState syncState;
    @Autowired private ConnectorRegistry connectorRegistry;
    @Autowired private EricssonEnmConnector enmConnector;
    @Autowired private CellRepository cellRepository;
    @Autowired private NetworkImportBatchRepository batchRepository;
    @Autowired private SynchronizationCheckpointRepository checkpointRepository;
    @Autowired private SynchronizationSourceStateRepository sourceStateRepository;
    @Autowired private NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    @Autowired private NetworkDriftObservationRepository driftRepository;
    @Autowired private NetworkSourceReferenceRepository sourceReferenceRepository;
    @Autowired private EnmImportTestHooks hooks;
    @Autowired private EnmConnectorMetrics enmMetrics;
    @Autowired private NetworkImportQueryService importQueryService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TestRestTemplate http;

    @BeforeEach
    @AfterEach
    void reset() {
        hooks.clear();
        scenarios.use(SimulatorEnmScenario.SUCCESS_SINGLE_PAGE);
        syncState.resetAll();
        jdbc.update("DELETE FROM network_drift_observation");
        jdbc.update("DELETE FROM network_knowledge_status");
        jdbc.update("DELETE FROM synchronization_source_state");
        jdbc.update("DELETE FROM synchronization_checkpoint");
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update("UPDATE network_import_batch SET status = 'FAILED', completed_at = NOW() WHERE status = 'RUNNING'");
        jdbc.update("UPDATE network_import_batch SET status = 'FAILED' WHERE synchronization_mode IS NOT NULL AND status = 'COMPLETED'");
    }

    private SynchronizationPolicy policy() {
        return controlPlane.configuredSources().get(0);
    }

    private void runFullBaseline() {
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION, () -> controlPlane.triggerManual(CONNECTOR));
    }

    private SynchronizationExecutionResult runManual(SimulatorEnmScenario scenario) {
        scenarios.use(scenario);
        return authorizer.callWith(VendorImportAuthorizer.PERMISSION,
                () -> controlPlane.triggerManual(CONNECTOR));
    }

    @Test
    void matrix01_scheduledDueSourceEntersControlPlane() {
        runFullBaseline();
        jdbc.update(
                "UPDATE synchronization_checkpoint SET last_successful_completed_at = ? WHERE source_system = ?",
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofHours(1))), SOURCE);
        SynchronizationPolicy duePolicy = policy();
        assertTrue(controlPlane.isDue(duePolicy));
        scenarios.use(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        authorizer.runWith(VendorImportAuthorizer.SYSTEM_SCHEDULED_PERMISSION,
                () -> controlPlane.triggerScheduled(duePolicy));
        assertTrue(batchRepository.findAll().stream()
                .anyMatch(batch -> "SCHEDULED".equals(batch.getSynchronizationInitiator())));
    }

    @Test
    void matrix04_disabledSourceDoesNotExecute() {
        SynchronizationPolicy disabled = new SynchronizationPolicy(
                policy().sourceSystem(), policy().connectorId(), policy().sourceScope(),
                false, policy().preferredMode(), policy().cadence(), policy().requestTimeout(),
                policy().maxExecutionDuration(), policy().maxConsecutiveFailures(),
                policy().agingAfter(), policy().staleAfter(), policy().overlapPolicy(),
                policy().maxRetryAttempts(), policy().retryBackoff(), policy().allowRecoveryFullOnScheduled());
        assertThrows(SynchronizationDisabledException.class, () -> importService.execute(
                new SynchronizationExecutionRequest(disabled, SynchronizationInitiator.MANUAL, SynchronizationMode.FULL, false)));
    }

    @Test
    void matrix05_disabledSourceDoesNotOpenConnectorSession() {
        long before = enmMetrics.connectorSessions();
        matrix04_disabledSourceDoesNotExecute();
        assertEquals(before, enmMetrics.connectorSessions());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void matrix06_overlappingScheduledTriggerIsSkipped() {
        runFullBaseline();
        UUID runningId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO network_import_batch (id, source_system, vendor, source_snapshot_id, vendor_schema_version, "
                        + "fixture_kind, started_at, status, entities_read, entities_created, entities_updated, "
                        + "entities_unchanged, entities_rejected, conflicts_detected, missing_entities_detected, "
                        + "execution_type, attempt_number, source_scope, requested_at) "
                        + "VALUES (?, ?, 'ERICSSON', 'UNREAD', 'v1', 'NORMAL', NOW(), 'RUNNING', "
                        + "0,0,0,0,0,0,0,'NEW',1,?, NOW())",
                runningId, SOURCE, SCOPE);
        jdbc.update(
                "INSERT INTO network_import_lease (lease_key, source_system, source_scope, owner_execution_id, "
                        + "owner_instance_id, fencing_token, acquired_at, heartbeat_at, expires_at) "
                        + "VALUES (?, ?, ?, ?, 'overlap-test', 1, NOW(), NOW(), NOW() + INTERVAL '5 minutes')",
                SOURCE + "/" + SCOPE, SOURCE, SCOPE, runningId);
        authorizer.runWith(VendorImportAuthorizer.SYSTEM_SCHEDULED_PERMISSION,
                () -> controlPlane.triggerScheduled(policy()));
        assertTrue(batchRepository.findAll().stream()
                .filter(batch -> batch.getSynchronizationInitiator() != null)
                .noneMatch(batch -> "SCHEDULED".equals(batch.getSynchronizationInitiator())
                        && "COMPLETED".equals(batch.getStatus())));
    }

    @Test
    void matrix13_staleHolderCannotOverwriteKnowledgeStatus() {
        runFullBaseline();
        var knowledge = knowledgeStatusRepository.findAll().get(0);
        long authoritative = knowledge.getFencingToken();
        knowledge.update(
                authoritative - 1,
                NetworkKnowledgeConfidence.LOW.name(),
                KnowledgeConfidenceReason.RECOVERY_REQUIRED.name(),
                SynchronizationFreshness.DEGRADED.name(),
                SynchronizationSourceHealth.DEGRADED.name(),
                "stale",
                Instant.now(),
                Instant.now()
        );
        knowledgeStatusRepository.saveAndFlush(knowledge);
        assertEquals(NetworkKnowledgeConfidence.HIGH.name(), knowledgeStatusRepository.findAll().get(0).getConfidence());
        assertTrue(authoritative <= knowledgeStatusRepository.findAll().get(0).getFencingToken());
    }

    @Test
    void matrix15_firstSourceWithNoCheckpointSelectsFull() {
        SynchronizationMode mode = modeSelector.select(
                policy(),
                enmConnector.descriptor(connectorRegistry.require(CONNECTOR)),
                checkpointService.find(SOURCE, SCOPE),
                false);
        assertEquals(SynchronizationMode.FULL, mode);
    }

    @Test
    void matrix17_failedFullDoesNotAdvanceCheckpoint() {
        scenarios.use(SimulatorEnmScenario.FAIL_AFTER_FIRST_PAGE);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION, () -> controlPlane.triggerManual(CONNECTOR));
        SynchronizationCheckpointEntity checkpoint = checkpointRepository.findAll().get(0);
        assertEquals(SynchronizationCheckpointStatus.UNVERIFIED.name(), checkpoint.getStatus());
        assertEquals(SimulatorEnmSyncState.CHECKPOINT_ZERO, checkpoint.getCheckpointValue());
    }

    @Test
    void matrix19_validIncrementalCapabilitySelectsIncremental() {
        runFullBaseline();
        SynchronizationMode mode = modeSelector.select(
                policy(),
                enmConnector.descriptor(connectorRegistry.require(CONNECTOR)),
                checkpointService.find(SOURCE, SCOPE),
                false);
        assertEquals(SynchronizationMode.INCREMENTAL, mode);
    }

    @Test
    void matrix20_unsupportedIncrementalFailsClosedForRealConnector() {
        runFullBaseline();
        ConnectorDescriptor realDescriptor = new ConnectorDescriptor(
                "REAL-ENM-TEST",
                Vendor.ERICSSON,
                "ENM",
                "INT",
                ConnectorImplementationType.REAL,
                ConnectorAccessMode.READ_ONLY,
                EricssonEnmConnector.READ_CAPABILITIES
        );
        assertThrows(SynchronizationUnsupportedModeException.class, () -> modeSelector.select(
                policy(),
                realDescriptor,
                checkpointService.find(SOURCE, SCOPE),
                false));
    }

    @Test
    void matrix21_incrementalAdvancesOnlyAfterReconciliation() {
        runFullBaseline();
        String before = checkpointRepository.findAll().get(0).getCheckpointValue();
        scenarios.use(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        SynchronizationExecutionResult result = runManual(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        assertEquals("COMPLETED", result.batch().getStatus());
        assertEquals("INCREMENTAL", result.batch().getSynchronizationMode());
        assertNotEquals(before, checkpointRepository.findAll().get(0).getCheckpointValue());
    }

    @Test
    void matrix22_incrementalOmissionDoesNotRemoveEntity() {
        runFullBaseline();
        long before = cellRepository.count();
        runManual(SimulatorEnmScenario.NO_CHANGES);
        assertEquals(before, cellRepository.count());
    }

    @Test
    void matrix23_explicitSyntheticRemoveMarksMissingConservatively() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.EXPLICIT_REMOVE);
        assertTrue(sourceReferenceRepository.findBySourceSystemAndSourceStatus(SOURCE, "MISSING")
                .stream().anyMatch(ref -> "CELL-SIM-001".equals(ref.getCanonicalEntityId())));
        assertTrue(cellRepository.findByCellId("CELL-SIM-001").isPresent());
    }

    @Test
    void matrix24_sameIncrementalBatchReplayIsIdempotent() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        long cellsAfterFirst = cellRepository.count();
        runManual(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        assertEquals(cellsAfterFirst, cellRepository.count());
    }

    @Test
    void matrix25_sameFullSnapshotReplayIsIdempotent() {
        runFullBaseline();
        long cells = cellRepository.count();
        scenarios.use(SimulatorEnmScenario.RECOVERY_FULL_SUCCESS);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION_RECOVERY,
                () -> controlPlane.triggerRecovery(CONNECTOR));
        assertEquals(cells, cellRepository.count());
    }

    @Test
    void matrix27_checkpointExpiredRequiresRecovery() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.CHECKPOINT_EXPIRED);
        assertEquals(SynchronizationCheckpointStatus.RECOVERY_REQUIRED.name(),
                checkpointRepository.findAll().get(0).getStatus());
    }

    @Test
    void matrix28_sequenceGapRequiresRecovery() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.SEQUENCE_GAP);
        assertEquals(SynchronizationCheckpointStatus.RECOVERY_REQUIRED.name(),
                checkpointRepository.findAll().get(0).getStatus());
    }

    @Test
    void matrix29_recoveryRequiredCannotContinueOrdinaryIncremental() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.CHECKPOINT_REJECTED);
        assertThrows(SynchronizationRecoveryRequiredException.class,
                () -> authorizer.callWith(VendorImportAuthorizer.PERMISSION,
                        () -> controlPlane.triggerManual(CONNECTOR)));
    }

    @Test
    void matrix32_failedRecoveryDoesNotLaunchNewJobs() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.CHECKPOINT_REJECTED);
        long before = batchRepository.count();
        scenarios.use(SimulatorEnmScenario.RECOVERY_FULL_FAILURE);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION_RECOVERY,
                () -> controlPlane.triggerRecovery(CONNECTOR));
        assertEquals(before + 1, batchRepository.count());
    }

    @Test
    void matrix39_crashWindowForcesRecoveryWithoutSilentSkip() {
        runFullBaseline();
        UUID baselineExecution = checkpointRepository.findAll().get(0).getLastSuccessfulExecutionId();
        hooks.afterReconcileBeforeCheckpoint(() -> {
            throw new ImportRuntimeException(ImportFailureCode.CHECKPOINT_UNCERTAIN, "simulated crash");
        });
        SynchronizationExecutionResult crashed = runManual(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        assertEquals("COMPLETED", crashed.batch().getStatus());
        assertNotEquals(baselineExecution, crashed.batch().getId());
        assertTrue(crashed.batch().getEntitiesRead() > 0 || crashed.batch().getEntitiesUpdated() > 0
                || crashed.batch().getEntitiesCreated() > 0);
        assertEquals(SynchronizationCheckpointStatus.CHECKPOINT_UNCERTAIN.name(),
                checkpointRepository.findAll().get(0).getStatus());
        assertTrue(driftRepository.findAll().stream()
                .anyMatch(d -> NetworkDriftType.SYNCHRONIZATION_DRIFT.name().equals(d.getDriftType())));
        assertThrows(SynchronizationRecoveryRequiredException.class,
                () -> authorizer.callWith(VendorImportAuthorizer.PERMISSION,
                        () -> controlPlane.triggerManual(CONNECTOR)));
        scenarios.use(SimulatorEnmScenario.RECOVERY_FULL_SUCCESS);
        SynchronizationExecutionResult recovery = authorizer.callWith(VendorImportAuthorizer.PERMISSION_RECOVERY,
                () -> controlPlane.triggerRecovery(CONNECTOR));
        assertEquals("COMPLETED", recovery.batch().getStatus());
        assertEquals("RECOVERY_FULL", recovery.batch().getSynchronizationMode());
        assertEquals(SynchronizationCheckpointStatus.VALID.name(),
                checkpointRepository.findAll().get(0).getStatus());
        assertNotEquals(crashed.batch().getId(), checkpointRepository.findAll().get(0).getLastSuccessfulExecutionId());
    }

    @Test
    void matrix40_sourceStateSurvivesPersistenceReload() {
        runFullBaseline();
        var persisted = sourceStateRepository.findAll();
        assertFalse(persisted.isEmpty());
        Map<String, Object> reloaded = queryService.sourceState(SOURCE, SCOPE);
        assertEquals("HIGH", reloaded.get("knowledgeConfidence"));
    }

    @Test
    void matrix41_checkpointSurvivesPersistenceReload() {
        runFullBaseline();
        String value = checkpointRepository.findAll().get(0).getCheckpointValue();
        assertNotNull(checkpointRepository.findBySourceSystemAndSynchronizationScope(SOURCE, SCOPE)
                .orElseThrow().getLastSuccessfulExecutionId());
        assertEquals(value, checkpointRepository.findBySourceSystemAndSynchronizationScope(SOURCE, SCOPE)
                .orElseThrow().getCheckpointValue());
    }

    @Test
    void matrix47_readinessRemainsReadyDuringVendorOutage() {
        scenarios.use(SimulatorEnmScenario.UNAVAILABLE_503);
        Map<String, Object> health = importQueryService.runtimeHealth();
        assertFalse("DOWN".equals(health.get("status")));
    }

    @Test
    void matrix51_sourceStateDriftDetectedOnIncrementalChange() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.DRIFT_DETECTED);
        assertTrue(driftRepository.findAll().stream()
                .anyMatch(d -> NetworkDriftType.SOURCE_STATE_DRIFT.name().equals(d.getDriftType())));
    }

    @Test
    void matrix53_driftRecordContainsNoRawVendorPayload() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.DRIFT_DETECTED);
        driftRepository.findAll().forEach(drift -> {
            assertFalse(drift.getSummary().contains("password"));
            assertFalse(drift.getSummary().contains("{"));
        });
    }

    @Test
    void matrix54_laterTrustedStateResolvesDrift() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.DRIFT_DETECTED);
        runManual(SimulatorEnmScenario.DRIFT_RESOLVED);
        assertTrue(driftRepository.findAll().stream()
                .allMatch(d -> NetworkDriftStatus.RESOLVED.name().equals(d.getDriftStatus())));
    }

    @Test
    void matrix64_manualRecoveryRequiresAuthorization() {
        runFullBaseline();
        runManual(SimulatorEnmScenario.CHECKPOINT_REJECTED);
        assertThrows(ConnectorSecurityException.class, () -> authorizer.callWith(null,
                () -> controlPlane.triggerRecovery(CONNECTOR)));
    }

    @Test
    void matrix65_viewSynchronizationStatusRequiresAuthorization() {
        ResponseEntity<String> denied = http.exchange(
                "/api/v1/integration/sync/sources",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, denied.getStatusCode());
    }

    @Test
    void matrix66_apiCannotSupplyCredentialValues() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(VendorImportAuthorizer.HEADER, VendorImportAuthorizer.PERMISSION);
        ResponseEntity<ImportBatchDto> response = http.postForEntity(
                "/api/v1/integration/imports/connectors/" + CONNECTOR,
                new HttpEntity<>(Map.of("credential", "secret-value", "password", "secret"), headers),
                ImportBatchDto.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
    }

    @Test
    void matrix69_apiCannotMutateCheckpointDirectly() {
        runFullBaseline();
        HttpHeaders headers = new HttpHeaders();
        headers.set(VendorImportAuthorizer.HEADER, VendorImportAuthorizer.PERMISSION_VIEW);
        ResponseEntity<String> response = http.exchange(
                "/api/v1/integration/sync/sources/" + SOURCE + "/" + SCOPE,
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(Map.of("checkpointValue", "hacked"), headers),
                String.class);
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    void matrixModeAndInitiatorPopulatedForPhase12Executions() {
        runFullBaseline();
        NetworkImportBatchEntity full = batchRepository.findAll().stream()
                .filter(batch -> "FULL".equals(batch.getSynchronizationMode())
                        && "MANUAL".equals(batch.getSynchronizationInitiator()))
                .findFirst().orElseThrow();
        assertEquals("FULL", full.getSynchronizationMode());
        assertEquals("MANUAL", full.getSynchronizationInitiator());
        runManual(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        assertTrue(batchRepository.findAll().stream()
                .anyMatch(batch -> "INCREMENTAL".equals(batch.getSynchronizationMode())));
        runManual(SimulatorEnmScenario.CHECKPOINT_REJECTED);
        SynchronizationExecutionResult recovery = authorizer.callWith(VendorImportAuthorizer.PERMISSION_RECOVERY,
                () -> controlPlane.triggerRecovery(CONNECTOR));
        assertEquals("RECOVERY_FULL", recovery.batch().getSynchronizationMode());
        jdbc.update(
                "UPDATE synchronization_checkpoint SET last_successful_completed_at = ? WHERE source_system = ?",
                java.sql.Timestamp.from(Instant.now().minus(Duration.ofHours(1))), SOURCE);
        authorizer.runWith(VendorImportAuthorizer.SYSTEM_SCHEDULED_PERMISSION,
                () -> controlPlane.triggerScheduled(policy()));
        assertTrue(batchRepository.findAll().stream()
                .anyMatch(batch -> "SCHEDULED".equals(batch.getSynchronizationInitiator())));
    }

    @Test
    void matrix11_staleHolderCannotAdvanceCheckpoint() {
        runFullBaseline();
        SynchronizationCheckpointEntity checkpoint = checkpointRepository.findAll().get(0);
        long authoritative = checkpoint.getFencingToken();
        boolean advanced = checkpointService.advanceIfAuthoritative(
                SOURCE, SCOPE, UUID.randomUUID(), "snap-stale", Instant.now(), Instant.now(),
                "sim-seq:99", "sim-v99", SynchronizationMode.INCREMENTAL, "COMPLETE",
                authoritative - 1, Instant.now(), Instant.now());
        assertFalse(advanced);
        assertNotEquals("sim-seq:99", checkpointRepository.findAll().get(0).getCheckpointValue());
    }

    private HttpEntity<Void> authHeaders(String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(VendorImportAuthorizer.HEADER, permission);
        return new HttpEntity<>(headers);
    }
}
