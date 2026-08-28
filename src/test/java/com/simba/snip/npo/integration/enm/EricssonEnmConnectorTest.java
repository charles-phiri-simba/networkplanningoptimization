package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.api.ImportBatchDto;
import com.simba.snip.npo.config.EnmIntegrationProperties;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportLease;
import com.simba.snip.npo.integration.NetworkImportQueryService;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.integration.security.ConnectorAccessMode;
import com.simba.snip.npo.integration.security.ConnectorCapability;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorDescriptor;
import com.simba.snip.npo.integration.security.ConnectorMode;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import com.simba.snip.npo.integration.security.ConnectorSecurityException;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkImportAuditEventRepository;
import com.simba.snip.npo.persist.SourceProvenanceRepository;
import com.simba.snip.npo.persist.VendorSnapshotEntity;
import com.simba.snip.npo.persist.VendorSnapshotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EricssonEnmConnectorTest extends AbstractPostgresIT {

    @Autowired
    private NetworkImportService importService;
    @Autowired
    private NetworkImportQueryService queryService;
    @Autowired
    private VendorImportAuthorizer authorizer;
    @Autowired
    private SimulatorEnmScenarioController scenarios;
    @Autowired
    private EnmImportTestHooks hooks;
    @Autowired
    private EnmIntegrationProperties enmProperties;
    @Autowired
    private EricssonEnmConnector connector;
    @Autowired
    private ConnectorRegistry connectorRegistry;
    @Autowired
    private CellRepository cellRepository;
    @Autowired
    private VendorSnapshotRepository vendorSnapshotRepository;
    @Autowired
    private SourceProvenanceRepository sourceProvenanceRepository;
    @Autowired
    private NetworkImportAuditEventRepository auditRepository;
    @Autowired
    private EnmConnectorMetrics metrics;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TestRestTemplate http;

    @AfterEach
    void reset() {
        scenarios.use(SimulatorEnmScenario.SUCCESS_SINGLE_PAGE);
        hooks.clear();
        enmProperties.setMaxPages(8);
        enmProperties.setMaxEntities(32);
        enmProperties.setMaxAttempts(3);
        enmProperties.setInitialBackoff(Duration.ofMillis(10));
        enmProperties.setMaxBackoff(Duration.ofMillis(100));
        enmProperties.setOverallExecutionTimeout(Duration.ofSeconds(30));
        enmProperties.setRequestTimeout(Duration.ofSeconds(2));
        enmProperties.setImplementationType("SIMULATOR");
        ConnectorDefinition current = connectorRegistry.require(EricssonEnmConnector.CONNECTOR_ID);
        if (current.mode() != ConnectorMode.SIMULATOR) {
            connectorRegistry.replace(copy(current, ConnectorMode.SIMULATOR, true));
        }
        jdbc.update("DELETE FROM network_import_lease WHERE source_system = ?", "ERICSSON_ENM_SIMULATOR");
        jdbc.update(
                "UPDATE network_import_batch SET status = 'FAILED' WHERE source_system = ? AND status = 'RUNNING'",
                "ERICSSON_ENM_SIMULATOR"
        );
    }

    @Test
    void connectorClassificationIsEricssonEnmReadOnly() {
        ConnectorDefinition definition = connectorRegistry.require(EricssonEnmConnector.CONNECTOR_ID);
        ConnectorDescriptor descriptor = connector.descriptor(definition);
        assertEquals(com.simba.snip.npo.integration.Vendor.ERICSSON, descriptor.vendor());
        assertEquals("ENM", descriptor.platform());
        assertEquals(ConnectorAccessMode.READ_ONLY, descriptor.accessMode());
        assertTrue(descriptor.readOnly());
        assertTrue(descriptor.capabilities().contains(ConnectorCapability.INVENTORY_READ));
        assertTrue(descriptor.capabilities().stream().noneMatch(ConnectorCapability::mutatesNetwork));
        assertFalse(descriptor.capabilities().contains(ConnectorCapability.WRITE_CONFIGURATION));
        assertFalse(descriptor.capabilities().contains(ConnectorCapability.NETWORK_MUTATION));
        assertFalse(descriptor.capabilities().contains(ConnectorCapability.PARAMETER_CHANGE));
        assertFalse(descriptor.capabilities().contains(ConnectorCapability.EXECUTE_COMMAND));
    }

    @Test
    void singlePageSuccessfulSnapshotMapsAndPersistsProvenance() {
        ImportBatchDto batch = importEnm();
        assertEquals("COMPLETED", batch.status());
        assertEquals("COMPLETE", batch.completeness());
        assertEquals(1, batch.pagesReceived());
        assertEquals("READ_ONLY", batch.accessMode());
        assertEquals("ENM", batch.platform());
        assertTrue(cellRepository.findByCellId("CELL-SIM-001").isPresent());
        List<com.simba.snip.npo.persist.SourceProvenanceEntity> provenance =
                sourceProvenanceRepository.findByImportExecutionId(batch.importId());
        assertTrue(provenance.stream().anyMatch(row -> "CELL-SIM-001".equals(row.getCanonicalEntityId())));
        com.simba.snip.npo.persist.SourceProvenanceEntity cell =
                provenance.stream().filter(row -> "CELL-SIM-001".equals(row.getCanonicalEntityId())).findFirst().orElseThrow();
        assertEquals("ERICSSON", cell.getSourceVendor());
        assertEquals("ERICSSON_ENM_SIMULATOR", cell.getSourceSystem());
        assertEquals("Cell", cell.getSourceObjectType());
        assertEquals("CELL-001", cell.getSourceObjectId());
        assertEquals(batch.importId(), cell.getImportExecutionId());
        assertNoSecrets(batch);
    }

    @Test
    void multiPageSuccessfulSnapshotMapsBothCells() {
        scenarios.use(SimulatorEnmScenario.SUCCESS_MULTI_PAGE);
        ImportBatchDto batch = importEnm();
        assertEquals("COMPLETED", batch.status());
        assertEquals("COMPLETE", batch.completeness());
        assertEquals(2, batch.pagesReceived());
        assertTrue(cellRepository.findByCellId("CELL-SIM-001").isPresent());
        assertTrue(cellRepository.findByCellId("CELL-SIM-002").isPresent());
    }

    @Test
    void failedLaterPageProducesZeroCanonicalMutation() {
        boolean cellTwoBefore = cellRepository.findByCellId("CELL-SIM-002").isPresent();
        scenarios.use(SimulatorEnmScenario.FAIL_AFTER_FIRST_PAGE);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_UNAVAILABLE.name(), batch.failureCode());
        assertEquals("FAILED", batch.completeness());
        assertEquals(0, batch.entitiesCreated());
        assertEquals(0, batch.entitiesUpdated());
        assertEquals(cellTwoBefore, cellRepository.findByCellId("CELL-SIM-002").isPresent());
        assertEquals(0, batch.missingEntitiesDetected());
    }

    @Test
    void partialSnapshotNeverInfersDeletion() {
        boolean cellOneBefore = cellRepository.findByCellId("CELL-SIM-001").isPresent();
        scenarios.use(SimulatorEnmScenario.PARTIAL_AFTER_FIRST_PAGE);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.SNAPSHOT_PARTIAL.name(), batch.failureCode());
        assertEquals("PARTIAL", batch.completeness());
        assertEquals(Boolean.FALSE, batch.retryable());
        assertEquals(0, batch.entitiesCreated());
        assertEquals(0, batch.entitiesUpdated());
        assertEquals(0, batch.missingEntitiesDetected());
        assertEquals(cellOneBefore, cellRepository.findByCellId("CELL-SIM-001").isPresent());
    }

    @Test
    void authentication401IsNonRetryable() {
        scenarios.use(SimulatorEnmScenario.AUTH_401);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_AUTHENTICATION_FAILED.name(), batch.failureCode());
        assertEquals(Boolean.FALSE, batch.retryable());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void authorization403IsNonRetryable() {
        scenarios.use(SimulatorEnmScenario.AUTH_403);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_AUTHORIZATION_DENIED.name(), batch.failureCode());
        assertEquals(Boolean.FALSE, batch.retryable());
    }

    @Test
    void rateLimit429RetriesWithinBudget() {
        scenarios.use(SimulatorEnmScenario.RATE_LIMIT_429);
        long retriesBefore = metrics.vendorRetries();
        ImportBatchDto batch = importEnm();
        assertEquals("COMPLETED", batch.status());
        assertTrue(metrics.vendorRetries() > retriesBefore);
        assertTrue(metrics.vendorThrottles() > 0);
    }

    @Test
    void unavailable503RetriesWithinBudget() {
        scenarios.use(SimulatorEnmScenario.UNAVAILABLE_503);
        ImportBatchDto batch = importEnm();
        assertEquals("COMPLETED", batch.status());
    }

    @Test
    void retryExhaustionFailsSafely() {
        scenarios.use(SimulatorEnmScenario.RATE_LIMIT_429);
        scenarios.setRateLimitHits(10);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_RATE_LIMITED.name(), batch.failureCode());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void retryAfterIsHonored() {
        scenarios.use(SimulatorEnmScenario.RATE_LIMIT_429);
        long started = System.nanoTime();
        ImportBatchDto batch = importEnm();
        assertEquals("COMPLETED", batch.status());
        assertTrue((System.nanoTime() - started) / 1_000_000L >= 20L);
    }

    @Test
    void requestTimeoutMapsToVendorTimeout() {
        scenarios.use(SimulatorEnmScenario.TIMEOUT);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_TIMEOUT.name(), batch.failureCode());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void overallDeadlinePreventsRetryOutsideBudget() {
        scenarios.use(SimulatorEnmScenario.RATE_LIMIT_429);
        scenarios.setRateLimitHits(10);
        enmProperties.setOverallExecutionTimeout(Duration.ofMillis(40));
        enmProperties.setInitialBackoff(Duration.ofMillis(200));
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_TIMEOUT.name(), batch.failureCode());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void cancellationBeforeFirstPage() {
        hooks.onBind(ConnectorCancellationToken::cancel);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.CONNECTOR_CANCELLED.name(), batch.failureCode());
        assertEquals(Boolean.FALSE, batch.retryable());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void cancellationBetweenPages() {
        scenarios.use(SimulatorEnmScenario.SUCCESS_MULTI_PAGE);
        hooks.afterFirstPage(() -> {
            ConnectorCancellationToken token = hooks.token();
            assertNotNull(token);
            token.cancel();
        });
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.CONNECTOR_CANCELLED.name(), batch.failureCode());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void cancellationDuringRetryBackoff() throws Exception {
        scenarios.use(SimulatorEnmScenario.RATE_LIMIT_429);
        scenarios.setRateLimitHits(10);
        enmProperties.setInitialBackoff(Duration.ofSeconds(2));
        CountDownLatch started = new CountDownLatch(1);
        hooks.onBind(token -> {
            Thread worker = new Thread(() -> {
                started.countDown();
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                token.cancel();
            });
            worker.setDaemon(true);
            worker.start();
        });
        ImportBatchDto batch = importEnm();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.CONNECTOR_CANCELLED.name(), batch.failureCode());
    }

    @Test
    void leaseLossBetweenPagesDoesNotReconcile() {
        scenarios.use(SimulatorEnmScenario.SUCCESS_MULTI_PAGE);
        hooks.afterFirstPage(this::stealLease);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.LEASE_LOST.name(), batch.failureCode());
        assertEquals(Boolean.FALSE, batch.retryable());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void leaseLossImmediatelyBeforeReconciliationDoesNotReconcile() {
        boolean cellBefore = cellRepository.findByCellId("CELL-SIM-001").isPresent();
        hooks.beforeReconcile(this::stealLease);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.LEASE_LOST.name(), batch.failureCode());
        assertEquals(0, batch.entitiesCreated());
        assertEquals(cellBefore, cellRepository.findByCellId("CELL-SIM-001").isPresent());
    }

    @Test
    void staleFencingTokenCannotReconcile() {
        boolean cellBefore = cellRepository.findByCellId("CELL-SIM-001").isPresent();
        hooks.beforeReconcile(this::bumpFencingToken);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.LEASE_LOST.name(), batch.failureCode());
        assertEquals(cellBefore, cellRepository.findByCellId("CELL-SIM-001").isPresent());
    }

    @Test
    void repeatedContinuationTokenRejected() {
        scenarios.use(SimulatorEnmScenario.REPEATED_CONTINUATION);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_PAGINATION_INVALID.name(), batch.failureCode());
        assertEquals(Boolean.FALSE, batch.retryable());
    }

    @Test
    void continuationCycleRejected() {
        scenarios.use(SimulatorEnmScenario.CONTINUATION_CYCLE);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_PAGINATION_INVALID.name(), batch.failureCode());
    }

    @Test
    void emptyPageWithInvalidContinuationRejected() {
        scenarios.use(SimulatorEnmScenario.EMPTY_INVALID_CONTINUATION);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_PAGINATION_INVALID.name(), batch.failureCode());
    }

    @Test
    void pageLimitEnforced() {
        scenarios.use(SimulatorEnmScenario.PAGE_LIMIT);
        enmProperties.setMaxPages(2);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.SNAPSHOT_LIMIT_EXCEEDED.name(), batch.failureCode());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void entityLimitEnforced() {
        scenarios.use(SimulatorEnmScenario.ENTITY_LIMIT);
        enmProperties.setMaxEntities(32);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.SNAPSHOT_LIMIT_EXCEEDED.name(), batch.failureCode());
    }

    @Test
    void malformedResponseRejected() {
        scenarios.use(SimulatorEnmScenario.MALFORMED);
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.VENDOR_RESPONSE_INVALID.name(), batch.failureCode());
        assertEquals(Boolean.FALSE, batch.retryable());
    }

    @Test
    void rawPayloadAndSecretsAreNotPersistedOrReturned() {
        ImportBatchDto batch = importEnm();
        List<VendorSnapshotEntity> snapshots = vendorSnapshotRepository.findByExecutionIdOrderByStartedAtAsc(batch.importId());
        assertFalse(snapshots.isEmpty());
        VendorSnapshotEntity snapshot = snapshots.get(snapshots.size() - 1);
        assertFalse(String.valueOf(snapshot.getWarnings()).toLowerCase().contains("password"));
        assertFalse(String.valueOf(snapshot.getWarnings()).contains("Bearer"));
        auditRepository.findByImportIdOrderByOccurredAtAsc(batch.importId()).forEach(event -> {
            String details = event.getDetails() == null ? "" : event.getDetails();
            assertFalse(details.contains("password"));
            assertFalse(details.contains("Bearer"));
            assertFalse(details.contains("sim-token-page"));
            assertFalse(details.contains("BEGIN PRIVATE KEY"));
        });
        assertNoSecrets(batch);
    }

    @Test
    void unauthorizedImportIsRejectedBeforeConnectorUse() {
        long sessions = metrics.connectorSessions();
        assertThrows(ConnectorSecurityException.class, () ->
                importService.importSecure(EricssonEnmConnector.CONNECTOR_ID));
        assertEquals(sessions, metrics.connectorSessions());
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Map> response = http.postForEntity(
                "/api/v1/integration/imports/connectors/" + EricssonEnmConnector.CONNECTOR_ID,
                new HttpEntity<>(Map.of(), headers),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED.name(), response.getBody().get("failureCode"));
        assertFalse(String.valueOf(response.getBody()).contains("password"));
    }

    @Test
    void productionTransportFailsClosed() {
        ConnectorDefinition current = connectorRegistry.require(EricssonEnmConnector.CONNECTOR_ID);
        connectorRegistry.replace(copy(current, ConnectorMode.REAL, true));
        ImportBatchDto batch = importEnm();
        assertEquals("FAILED", batch.status());
        assertEquals(ImportFailureCode.PRODUCTION_TRANSPORT_NOT_CONFIGURED.name(), batch.failureCode());
        assertEquals(Boolean.FALSE, batch.retryable());
        assertEquals(0, batch.entitiesCreated());
    }

    @Test
    void authorizedHttpImportSucceeds() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(VendorImportAuthorizer.HEADER, VendorImportAuthorizer.PERMISSION);
        ResponseEntity<ImportBatchDto> response = http.postForEntity(
                "/api/v1/integration/imports/connectors/" + EricssonEnmConnector.CONNECTOR_ID,
                new HttpEntity<>(Map.of(), headers),
                ImportBatchDto.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("COMPLETED", response.getBody().status());
    }

    @Test
    void healthDoesNotProbeEnm() {
        ResponseEntity<Map> response = http.getForEntity("/api/v1/integration/health", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().get("enmLiveInventoryProbed"));
    }

    @Test
    void readinessDoesNotProbeLiveInventory() {
        ResponseEntity<String> response = http.getForEntity("/api/v1/integration/connectors/security", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"liveInventoryProbed\":false"));
        assertTrue(response.getBody().contains(EricssonEnmConnector.CONNECTOR_ID));
        assertFalse(response.getBody().contains("password"));
        assertFalse(response.getBody().contains("Bearer"));
    }

    private ImportBatchDto importEnm() {
        return authorizer.callWith(VendorImportAuthorizer.PERMISSION, () ->
                queryService.importDetail(importService.importSecure(EricssonEnmConnector.CONNECTOR_ID).getId()));
    }

    private void stealLease() {
        jdbc.update(
                "UPDATE network_import_lease SET expires_at = NOW() - INTERVAL '1 hour' WHERE lease_key = ?",
                ImportLease.key("ERICSSON_ENM_SIMULATOR", "DEFAULT")
        );
    }

    private void bumpFencingToken() {
        jdbc.update(
                "UPDATE network_import_lease SET fencing_token = fencing_token + 1 WHERE lease_key = ?",
                ImportLease.key("ERICSSON_ENM_SIMULATOR", "DEFAULT")
        );
    }

    private static ConnectorDefinition copy(ConnectorDefinition current, ConnectorMode mode, boolean enabled) {
        return new ConnectorDefinition(
                current.connectorId(),
                current.vendor(),
                current.sourceSystem(),
                current.sourceScope(),
                current.endpointRef(),
                current.inventoryPath(),
                current.credentialRef(),
                current.trustProfileId(),
                current.authorizationProfileId(),
                current.networkPolicyId(),
                current.authenticationMethod(),
                current.credentialProvider(),
                current.requiredCapabilities(),
                enabled,
                mode
        );
    }

    private static void assertNoSecrets(ImportBatchDto batch) {
        String body = batch.toString();
        assertFalse(body.contains("password"));
        assertFalse(body.contains("passwd"));
        assertFalse(body.contains("Bearer"));
        assertFalse(body.contains("client_secret"));
        assertFalse(body.contains("BEGIN PRIVATE KEY"));
        assertFalse(body.contains("Authorization:"));
        if (batch.error() != null) {
            assertFalse(batch.error().contains("sim-token"));
        }
    }
}
