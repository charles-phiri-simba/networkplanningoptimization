package com.simba.snip.npo.api;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.action.ActionPolicyEvaluator;
import com.simba.snip.npo.action.ActionType;
import com.simba.snip.npo.action.CapabilityRegistry;
import com.simba.snip.npo.action.PolicyOutcome;
import com.simba.snip.npo.agent.AgentRegistry;
import com.simba.snip.npo.agent.AgentRole;
import com.simba.snip.npo.integration.FixtureKind;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.integration.Vendor;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkImportAuditEventRepository;
import com.simba.snip.npo.persist.NetworkSourceReferenceRepository;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultiVendorIntegrationApiTest extends AbstractPostgresIT {

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private NetworkImportService importService;

    @Autowired
    private CellRepository cellRepository;

    @Autowired
    private RadioConfigurationRepository radioConfigurationRepository;

    @Autowired
    private NetworkSourceReferenceRepository sourceReferenceRepository;

    @Autowired
    private NetworkImportAuditEventRepository auditRepository;

    @Autowired
    private ActionPolicyEvaluator policyEvaluator;

    @Autowired
    private AgentRegistry agentRegistry;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @Order(1)
    void ericssonNormalImportCreatesCanonicalStateAndAudit() {
        ImportBatchDto batch = importEricsson(FixtureKind.NORMAL);
        assertEquals("COMPLETED", batch.status());
        assertEquals("NEW", batch.executionType());
        assertEquals(1, batch.attemptNumber());
        assertEquals("DEFAULT", batch.sourceScope());
        assertNotNull(batch.canonicalSnapshotHash());
        assertEquals("ERICSSON_FIXTURE", batch.sourceSystem());
        assertEquals("ERICSSON_FIXTURE_V1", batch.vendorSchemaVersion());
        assertEquals("er-snap-normal-001", batch.sourceSnapshotId());
        assertTrue(batch.entitiesCreated() > 0);
        assertEquals(0, batch.conflictsDetected());
        assertNotNull(http.getForObject("/api/v1/sites/SITE-E001", SiteDto.class));
        CellDto cell = http.getForObject("/api/v1/cells/CELL-E001", CellDto.class);
        assertEquals("NR", cell.technology());
        assertEquals("TDD", cell.duplexMode());
        assertEquals("GNB-E001", cell.gnbId());
        assertEquals("SITE-E001", cell.siteId());
        assertEquals("46", txPower("CELL-E001"));
        assertEquals("43", txPower("CELL-E002"));
        assertTrue(http.getForObject("/api/v1/cells/CELL-E001/neighbours", NeighbourDto[].class).length >= 1);
        assertEquals("46", txPower("CELL-001"));
        assertTrue(sourceReferenceRepository.findByCanonicalEntityIdOrderByCanonicalEntityTypeAsc("CELL-E001")
                .stream().anyMatch(ref -> "ERICSSON_FIXTURE".equals(ref.getSourceSystem()) && ref.isAuthoritative()));
        List<String> types = auditRepository.findByImportIdOrderByOccurredAtAsc(batch.importId()).stream()
                .map(event -> event.getEventType()).toList();
        assertEquals(List.of(
                "IMPORT_STARTED",
                "LEASE_ACQUIRED",
                "SNAPSHOT_READ",
                "VALIDATION_COMPLETED",
                "RECONCILIATION_COMPLETED",
                "IMPORT_COMPLETED",
                "LEASE_RELEASED"
        ), types);
        ImportCheckpointDto[] checkpoints = http.getForObject(
                "/api/v1/integration/imports/" + batch.importId() + "/checkpoints", ImportCheckpointDto[].class);
        assertEquals(5, checkpoints.length);
        assertEquals("SNAPSHOT_READ", checkpoints[0].checkpointType());
        assertEquals("CANONICAL_COMMIT_COMPLETED", checkpoints[4].checkpointType());
    }

    @Test
    @Order(2)
    void identicalEricssonReimportIsIdempotent() {
        ImportBatchDto batch = importEricsson(FixtureKind.NORMAL);
        assertEquals("COMPLETED", batch.status());
        assertEquals("REPLAY", batch.executionType());
        assertEquals("DEFAULT", batch.sourceScope());
        assertNotNull(batch.originalSuccessfulExecutionId());
        assertNotEquals(batch.importId(), batch.originalSuccessfulExecutionId());
        assertEquals(0, batch.entitiesCreated());
        assertEquals(0, batch.entitiesUpdated());
        assertEquals(0, batch.entitiesUnchanged());
        assertEquals(1, cellRepository.findByCellId("CELL-E001").stream().count());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM neighbour_relationship n JOIN cell s ON s.id = n.source_cell_id JOIN cell t ON t.id = n.target_cell_id WHERE s.cell_id='CELL-E001' AND t.cell_id='CELL-E002'",
                Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_source_reference WHERE canonical_entity_id='CELL-E001' AND source_system='ERICSSON_FIXTURE'",
                Integer.class));
        ImportBatchDto original = http.getForObject(
                "/api/v1/integration/imports/" + batch.originalSuccessfulExecutionId(), ImportBatchDto.class);
        assertEquals("NEW", original.executionType());
        assertEquals("COMPLETED", original.status());
        assertEquals(original.canonicalSnapshotHash(), batch.canonicalSnapshotHash());
    }

    @Test
    @Order(3)
    void nokiaNormalImportUsesTheSameCanonicalLayer() {
        ImportBatchDto batch = importNokia(FixtureKind.NORMAL);
        assertEquals("COMPLETED", batch.status());
        assertEquals("NOKIA_FIXTURE", batch.sourceSystem());
        assertEquals("nk-snap-normal-001", batch.sourceSnapshotId());
        assertEquals("NOKIA_FIXTURE_V1", batch.vendorSchemaVersion());
        CellDto cell = http.getForObject("/api/v1/cells/CELL-N001", CellDto.class);
        assertEquals("NR", cell.technology());
        assertEquals("TDD", cell.duplexMode());
        assertEquals("46", txPower("CELL-N001"));
        assertEquals("43", txPower("CELL-N002"));
        assertEquals("46", txPower("CELL-001"));
        assertNotNull(http.getForObject("/api/v1/gnbs/GNB-N001", GnbDto.class));
    }

    @Test
    @Order(4)
    void sameSourceChangedSnapshotUpdatesTxPower() {
        assertEquals("46", txPower("CELL-E001"));
        ImportBatchDto batch = importEricsson(FixtureKind.UPDATE);
        assertEquals("COMPLETED", batch.status());
        assertEquals(0, batch.entitiesCreated());
        assertTrue(batch.entitiesUpdated() >= 1);
        assertEquals(0, batch.conflictsDetected());
        assertEquals("44", txPower("CELL-E001"));
    }

    @Test
    @Order(5)
    void partialSnapshotDoesNotMarkOmittedEntityMissing() {
        importEricsson(FixtureKind.NORMAL);
        ImportBatchDto batch = importEricsson(FixtureKind.PARTIAL);
        assertEquals("COMPLETED", batch.status());
        assertEquals(0, batch.missingEntitiesDetected());
        assertEquals("ACTIVE", sourceStatus("CELL", "CELL-E002"));
        assertNotNull(cellRepository.findByCellId("CELL-E002").orElseThrow());
    }

    @Test
    @Order(6)
    void completeSnapshotMarksAbsentSourceEntityMissingWithoutDeleting() {
        ImportBatchDto batch = importEricsson(FixtureKind.MISSING_OMIT);
        assertEquals("COMPLETED", batch.status());
        assertTrue(batch.missingEntitiesDetected() >= 1);
        assertEquals("MISSING", sourceStatus("CELL", "CELL-E002"));
        assertTrue(cellRepository.findByCellId("CELL-E002").isPresent());
        importEricsson(FixtureKind.REAPPEAR);
        assertEquals("ACTIVE", sourceStatus("CELL", "CELL-E002"));
    }

    @Test
    @Order(7)
    void invalidRecordsArePersistedAsRejections() {
        ImportBatchDto batch = importEricsson(FixtureKind.REJECT);
        assertEquals("COMPLETED", batch.status());
        assertTrue(batch.entitiesRejected() >= 5);
        assertNotNull(http.getForObject("/api/v1/cells/CELL-R001", CellDto.class));
        ResponseEntity<ImportRejectionDto[]> rejections =
                http.getForEntity("/api/v1/integration/rejections", ImportRejectionDto[].class);
        assertEquals(HttpStatus.OK, rejections.getStatusCode());
        List<String> codes = Arrays.stream(rejections.getBody()).map(ImportRejectionDto::reasonCode).toList();
        assertTrue(codes.contains("UNSUPPORTED_TECHNOLOGY"));
        assertTrue(codes.contains("INVALID_TX_POWER"));
        assertTrue(codes.contains("MISSING_PARENT"));
        assertTrue(codes.contains("MISSING_SOURCE_ID"));
        assertTrue(codes.contains("DUPLICATE_SOURCE_IDENTITY"));
        assertTrue(codes.contains("INVALID_NEIGHBOUR") || codes.contains("MALFORMED_RELATIONSHIP"));
        assertTrue(cellRepository.findByCellId("CELL-R-BADTECH").isEmpty());
    }

    @Test
    @Order(8)
    void secondConflictingSourcePersistsConflictAndDoesNotOverwrite() {
        ImportBatchDto ericsson = importEricsson(FixtureKind.CONFLICT);
        assertEquals("COMPLETED", ericsson.status());
        assertEquals("46", txPower("CELL-CONFLICT-001"));
        ImportBatchDto nokia = importNokia(FixtureKind.CONFLICT);
        assertEquals("COMPLETED", nokia.status());
        assertTrue(nokia.conflictsDetected() >= 1);
        assertEquals("46", txPower("CELL-CONFLICT-001"));
        assertEquals(1, cellRepository.findByCellId("CELL-CONFLICT-001").stream().count());
        ResponseEntity<ImportConflictDto[]> conflicts =
                http.getForEntity("/api/v1/integration/conflicts", ImportConflictDto[].class);
        ImportConflictDto tx = Arrays.stream(conflicts.getBody())
                .filter(c -> c.canonicalEntityId().contains("CELL-CONFLICT-001"))
                .filter(c -> "txPower".equals(c.conflictScope()) || c.canonicalEntityId().endsWith(":txPower"))
                .findFirst()
                .orElseThrow();
        assertEquals("OPEN", tx.status());
        assertEquals("ERICSSON_FIXTURE", tx.authoritativeSource());
        assertEquals("NOKIA_FIXTURE", tx.incomingSource());
        assertTrue(tx.incomingValue().contains("43"));
        assertTrue(tx.currentValue().contains("46"));
        ImportConflictDto fetched = http.getForObject("/api/v1/integration/conflicts/" + tx.conflictId(), ImportConflictDto.class);
        assertEquals(tx.conflictId(), fetched.conflictId());
    }

    @Test
    @Order(9)
    void catastrophicSnapshotFailsBatchWithoutCanonicalMutation() {
        String before = txPower("CELL-E001");
        var failed = importService.importVendor(Vendor.ERICSSON, FixtureKind.CATASTROPHIC, true);
        assertEquals("FAILED", failed.getStatus());
        assertNotEquals("STARTED", failed.getStatus());
        assertEquals(before, txPower("CELL-E001"));
        List<String> types = auditRepository.findByImportIdOrderByOccurredAtAsc(failed.getId()).stream()
                .map(event -> event.getEventType()).toList();
        assertTrue(types.contains("IMPORT_STARTED"));
        assertTrue(types.contains("IMPORT_FAILED"));
        ResponseEntity<ImportBatchDto> api = http.getForEntity(
                "/api/v1/integration/imports/" + failed.getId(), ImportBatchDto.class);
        assertEquals(HttpStatus.OK, api.getStatusCode());
        assertEquals("FAILED", api.getBody().status());
    }

    @Test
    @Order(10)
    void controlledCell001ImportMakesExistingTwinStaleWithoutAutoSync() {
        TwinDetailDto twin = synchronize("CELL-001");
        assertEquals("CURRENT", twin.freshness());
        int version = twin.latestVersion();
        Integer versionsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM network_twin_version WHERE twin_id = ?", Integer.class, twin.id());
        try {
            ImportBatchDto batch = importEricsson(FixtureKind.CELL001_STALE);
            assertEquals("COMPLETED", batch.status());
            assertEquals("44", txPower("CELL-001"));
            TwinDetailDto after = http.getForObject("/api/v1/twins/" + twin.id(), TwinDetailDto.class);
            assertEquals("STALE", after.freshness());
            assertEquals(version, after.latestVersion());
            Integer versionsAfter = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM network_twin_version WHERE twin_id = ?", Integer.class, twin.id());
            assertEquals(versionsBefore, versionsAfter);
            ImportBatchDto replay = importEricsson(FixtureKind.CELL001_STALE);
            assertEquals("REPLAY", replay.executionType());
            assertEquals(0, replay.entitiesCreated());
            assertEquals(0, replay.entitiesUpdated());
            TwinDetailDto afterReplay = http.getForObject("/api/v1/twins/" + twin.id(), TwinDetailDto.class);
            assertEquals("STALE", afterReplay.freshness());
            assertEquals(version, afterReplay.latestVersion());
            Integer versionsReplay = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM network_twin_version WHERE twin_id = ?", Integer.class, twin.id());
            assertEquals(versionsBefore, versionsReplay);
        } finally {
            jdbc.update(
                    "UPDATE radio_configuration SET parameter_value = '46' WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = 'CELL-001')");
            jdbc.update("DELETE FROM network_source_reference WHERE canonical_entity_id IN ('SITE-001','GNB-001','CELL-001','CELL-001:txPower')");
            assertEquals("46", txPower("CELL-001"));
        }
    }

    @Test
    @Order(11)
    void phase4AndPhase5BoundariesRemainClosed() {
        assertTrue(CapabilityRegistry.all().stream().noneMatch(c -> c.capabilityId().contains("vendor")));
        assertEquals(PolicyOutcome.DENY, policyEvaluator.evaluate(ActionType.APPLY_CELL_PARAMETER_CHANGE).decision());
        assertEquals(5, agentRegistry.list().size());
        assertTrue(agentRegistry.list().stream().noneMatch(a -> a.role().name().contains("VENDOR")));
        assertNotNull(agentRegistry.requireRole(AgentRole.CHIEF_ORCHESTRATOR));
        ResponseEntity<String> missing = http.getForEntity(
                "/api/v1/integration/imports/" + UUID.randomUUID(), String.class);
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        ResponseEntity<String> badKind = http.postForEntity(
                "/api/v1/integration/imports/ericsson",
                new CreateImportRequest("NOT_A_KIND"),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, badKind.getStatusCode());
        ResponseEntity<String> delayKind = http.postForEntity(
                "/api/v1/integration/imports/ericsson",
                new CreateImportRequest("DELAY"),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, delayKind.getStatusCode());
        ResponseEntity<Map> health = http.getForEntity("/api/v1/integration/health", Map.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertTrue(((Number) health.getBody().get("activeImports")).intValue() >= 0);
        ResponseEntity<ImportBatchDto[]> listed = http.getForEntity("/api/v1/integration/imports", ImportBatchDto[].class);
        assertEquals(HttpStatus.OK, listed.getStatusCode());
        assertTrue(listed.getBody().length >= 1);
    }

    private ImportBatchDto importEricsson(FixtureKind kind) {
        ResponseEntity<ImportBatchDto> response = http.postForEntity(
                "/api/v1/integration/imports/ericsson",
                new CreateImportRequest(kind.name()),
                ImportBatchDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private ImportBatchDto importNokia(FixtureKind kind) {
        ResponseEntity<ImportBatchDto> response = http.postForEntity(
                "/api/v1/integration/imports/nokia",
                new CreateImportRequest(kind.name()),
                ImportBatchDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private TwinDetailDto synchronize(String cellId) {
        ResponseEntity<TwinDetailDto> response = http.postForEntity(
                "/api/v1/twins/cells/" + cellId + "/synchronize", null, TwinDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private String txPower(String cellId) {
        var cell = cellRepository.findByCellId(cellId).orElseThrow();
        return radioConfigurationRepository.findByCell_IdAndParameterName(cell.getId(), "txPower")
                .orElseThrow()
                .getParameterValue();
    }

    private String sourceStatus(String type, String canonicalId) {
        return sourceReferenceRepository
                .findByCanonicalEntityTypeAndCanonicalEntityIdAndAuthoritativeTrue(type, canonicalId)
                .orElseThrow()
                .getSourceStatus();
    }
}
