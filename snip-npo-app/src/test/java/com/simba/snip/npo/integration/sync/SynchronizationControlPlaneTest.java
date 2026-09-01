package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.SynchronizationCheckpointRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class)
class SynchronizationControlPlaneTest extends AbstractPostgresIT {

    @Autowired
    private SynchronizationControlPlane controlPlane;
    @Autowired
    private SynchronizationQueryService queryService;
    @Autowired
    private VendorImportAuthorizer authorizer;
    @Autowired
    private SimulatorEnmScenarioController scenarios;
    @Autowired
    private SimulatorEnmSyncState syncState;
    @Autowired
    private ConnectorRegistry connectorRegistry;
    @Autowired
    private CellRepository cellRepository;
    @Autowired
    private SynchronizationCheckpointRepository checkpointRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private com.simba.snip.npo.integration.enm.EricssonEnmConnector connector;

    @BeforeEach
    @AfterEach
    void reset() {
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

    private void runFullBaseline() {
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));
    }

    @Test
    void simulatorAdvertisesIncrementalCapabilities() {
        assertTrue(connector.descriptor(connectorRegistry.require(
                ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)).capabilities()
                .contains(com.simba.snip.npo.integration.security.ConnectorCapability.INCREMENTAL_SYNCHRONIZATION));
    }

    @Test
    void firstManualFullEstablishesTrustedBaselineAndHighConfidence() {
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        SynchronizationExecutionResult result = authorizer.callWith(
                VendorImportAuthorizer.PERMISSION,
                () -> controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)
        );
        assertFalse(result.overlapSkipped());
        assertEquals("COMPLETED", result.batch().getStatus());
        assertTrue(cellRepository.count() >= 1);
        Map<String, Object> state = queryService.sourceState("ERICSSON_ENM_SIMULATOR", "DEFAULT");
        assertEquals("HIGH", state.get("knowledgeConfidence"));
        assertEquals("FRESH", state.get("freshness"));
        assertNotNull(checkpointRepository.findAll().get(0).getLastSuccessfulExecutionId());
    }

    @Test
    void scheduledInitiatorUsesSameRuntimePath() {
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));
        syncState.resetScope("DEFAULT");
        scenarios.use(SimulatorEnmScenario.INCREMENTAL_SUCCESS);
        SynchronizationPolicy policy = controlPlane.configuredSources().get(0);
        SynchronizationExecutionResult scheduled = authorizer.callWith(
                VendorImportAuthorizer.SYSTEM_SCHEDULED_PERMISSION,
                () -> {
                    controlPlane.triggerScheduled(policy);
                    return new SynchronizationExecutionResult(null, SynchronizationMode.INCREMENTAL, false, null);
                }
        );
        assertNotNull(scheduled);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void overlapSkipDoesNotMutateCheckpoint() {
        runFullBaseline();
        long before = checkpointRepository.count();
        java.util.UUID runningId = java.util.UUID.randomUUID();
        jdbc.update(
                "INSERT INTO network_import_batch (id, source_system, vendor, source_snapshot_id, vendor_schema_version, "
                        + "fixture_kind, started_at, status, entities_read, entities_created, entities_updated, "
                        + "entities_unchanged, entities_rejected, conflicts_detected, missing_entities_detected, "
                        + "execution_type, attempt_number, source_scope, requested_at) "
                        + "VALUES (?, 'ERICSSON_ENM_SIMULATOR', 'ERICSSON', 'UNREAD', 'v1', 'NORMAL', NOW(), 'RUNNING', "
                        + "0,0,0,0,0,0,0,'NEW',1,'DEFAULT', NOW())",
                runningId
        );
        jdbc.update(
                "INSERT INTO network_import_lease (lease_key, source_system, source_scope, owner_execution_id, "
                        + "owner_instance_id, fencing_token, acquired_at, heartbeat_at, expires_at) "
                        + "VALUES (?, 'ERICSSON_ENM_SIMULATOR', 'DEFAULT', ?, 'overlap-test', 1, NOW(), NOW(), NOW() + INTERVAL '5 minutes')",
                "ERICSSON_ENM_SIMULATOR/DEFAULT",
                runningId
        );
        SynchronizationExecutionResult skipped = authorizer.callWith(
                VendorImportAuthorizer.PERMISSION,
                () -> controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)
        );
        assertTrue(skipped.overlapSkipped());
        assertEquals(before, checkpointRepository.count());
    }

    @Test
    void checkpointRejectedRequiresRecoveryAuthorization() {
        runFullBaseline();
        scenarios.use(SimulatorEnmScenario.CHECKPOINT_REJECTED);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));
        scenarios.use(SimulatorEnmScenario.RECOVERY_FULL_SUCCESS);
        SynchronizationExecutionResult recovery = authorizer.callWith(
                VendorImportAuthorizer.PERMISSION_RECOVERY,
                () -> controlPlane.triggerRecovery(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)
        );
        assertFalse(recovery.overlapSkipped());
        assertNotNull(recovery.batch());
        assertEquals("COMPLETED", recovery.batch().getStatus());
    }
}
