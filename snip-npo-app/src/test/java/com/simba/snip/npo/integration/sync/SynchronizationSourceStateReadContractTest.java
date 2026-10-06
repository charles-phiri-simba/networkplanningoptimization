package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SynchronizationSourceStateReadContractTest extends AbstractPostgresIT {

    private static final String SOURCE = "ERICSSON_ENM_SIMULATOR";
    private static final String SCOPE = "DEFAULT";
    private static final String CONNECTOR = ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER;

    @Autowired private SynchronizationQueryService queryService;
    @Autowired private SynchronizationControlPlane controlPlane;
    @Autowired private VendorImportAuthorizer authorizer;
    @Autowired private SimulatorEnmScenarioController scenarios;
    @Autowired private SimulatorEnmSyncState syncState;
    @Autowired private SynchronizationSourceStateRepository sourceStateRepository;
    @Autowired private NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    @Autowired private SynchronizationCheckpointRepository checkpointRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TestRestTemplate http;

    @BeforeEach
    @AfterEach
    void reset() {
        scenarios.use(SimulatorEnmScenario.SUCCESS_SINGLE_PAGE);
        syncState.resetAll();
        jdbc.update("DELETE FROM network_import_lease");
        jdbc.update("DELETE FROM network_knowledge_status");
        jdbc.update("DELETE FROM synchronization_source_state");
        jdbc.update("DELETE FROM synchronization_checkpoint");
    }

    @Test
    void missingStateReturnsExplicitAbsenceAndCreatesNoRows() {
        long batchesBefore = count("network_import_batch");
        assertEquals(0, sourceStateRepository.count());
        assertEquals(0, knowledgeStatusRepository.count());
        assertEquals(0, checkpointRepository.count());

        HttpHeaders headers = new HttpHeaders();
        headers.set(VendorImportAuthorizer.HEADER, VendorImportAuthorizer.PERMISSION_VIEW);
        ResponseEntity<Map> response = http.exchange(
                "/api/v1/integration/sync/sources/" + SOURCE + "/" + SCOPE,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("present"));
        assertEquals(SOURCE, body.get("sourceSystem"));
        assertEquals(SCOPE, body.get("sourceScope"));
        assertEquals(CONNECTOR, body.get("connectorId"));
        assertNull(body.get("knowledgeConfidence"));
        assertNull(body.get("freshness"));
        assertNull(body.get("sourceHealth"));
        assertNull(body.get("recoveryRequired"));
        assertNull(body.get("lastTrustedSnapshotId"));
        assertNull(body.get("checkpointStatus"));

        assertEquals(0, sourceStateRepository.count());
        assertEquals(0, knowledgeStatusRepository.count());
        assertEquals(0, checkpointRepository.count());
        assertEquals(batchesBefore, count("network_import_batch"));
    }

    @Test
    void existingStateIsReturnedWithoutWriteSideEffects() {
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION, () -> controlPlane.triggerManual(CONNECTOR));
        long sourceRows = sourceStateRepository.count();
        long knowledgeRows = knowledgeStatusRepository.count();
        long checkpointRows = checkpointRepository.count();
        long batches = count("network_import_batch");
        assertEquals(1, sourceRows);
        assertEquals(1, knowledgeRows);

        Map<String, Object> state = queryService.sourceState(SOURCE, SCOPE);
        assertEquals(true, state.get("present"));
        assertEquals("HIGH", state.get("knowledgeConfidence"));
        assertNotNull(state.get("freshness"));
        assertNotNull(state.get("sourceHealth"));
        assertEquals(false, state.get("recoveryRequired"));

        assertEquals(sourceRows, sourceStateRepository.count());
        assertEquals(knowledgeRows, knowledgeStatusRepository.count());
        assertEquals(checkpointRows, checkpointRepository.count());
        assertEquals(batches, count("network_import_batch"));
    }

    @Test
    void writePathRequireStillMaterializesState() {
        assertEquals(0, sourceStateRepository.count());
        assertEquals(0, knowledgeStatusRepository.count());
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        authorizer.runWith(VendorImportAuthorizer.PERMISSION, () -> controlPlane.triggerManual(CONNECTOR));
        assertEquals(1, sourceStateRepository.count());
        assertEquals(1, knowledgeStatusRepository.count());
        assertFalse(checkpointRepository.findAll().isEmpty());
        Map<String, Object> state = queryService.sourceState(SOURCE, SCOPE);
        assertEquals(true, state.get("present"));
        assertEquals("HIGH", state.get("knowledgeConfidence"));
    }

    private long count(String table) {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0L : value;
    }
}
