package com.simba.snip.npo.changeintelligence;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.changeintelligence.api.ChangeProposalDetailDto;
import com.simba.snip.npo.changeintelligence.api.GenerateChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.api.ReviewChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.authorization.ChangeProposalAuthorizer;
import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenario;
import com.simba.snip.npo.integration.enm.SimulatorEnmScenarioController;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.sync.SynchronizationControlPlane;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.ProposedActionEntity;
import com.simba.snip.npo.persist.ProposedActionRepository;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.telemetry.TelemetryEvent;
import com.simba.snip.npo.telemetry.TelemetryProjectionService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChangeIntelligenceApiTest extends AbstractPostgresIT {

    private static final String CELL = "CELL-001";
    private static final String SEED_TX_POWER = "46";

    @Autowired private TestRestTemplate http;
    @Autowired private TelemetryProjectionService projectionService;
    @Autowired private AssuranceCaseService assuranceCaseService;
    @Autowired private SynchronizationControlPlane controlPlane;
    @Autowired private VendorImportAuthorizer vendorImportAuthorizer;
    @Autowired private SimulatorEnmScenarioController scenarios;
    @Autowired private CellRepository cellRepository;
    @Autowired private RadioConfigurationRepository radioConfigurationRepository;
    @Autowired private NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    @Autowired private ProposedActionRepository proposedActionRepository;
    @Autowired private ChangeIntelligenceProperties changeIntelligenceProperties;
    @Autowired private JdbcTemplate jdbc;

    private UUID assuranceCaseId;
    private UUID phase13OwnedAssuranceCaseId;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM change_proposal_audit_event");
        jdbc.update("DELETE FROM change_proposal_review");
        jdbc.update("DELETE FROM network_change_candidate");
        jdbc.update("DELETE FROM network_change_proposal");
        jdbc.update("DELETE FROM network_drift_observation WHERE summary = 'phase13-test-drift'");
        cleanupPhase13AssuranceForCell001();
        jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p13-%'");
        restoreSharedPriorPhaseState();
    }

    @BeforeEach
    void fixtures() {
        seedTelemetry();
        runTrustedBaseline();
        List<AssuranceCaseEntity> cases = assuranceCaseService.listForCell(CELL);
        if (!cases.isEmpty()) {
            assuranceCaseId = cases.get(0).getId();
            phase13OwnedAssuranceCaseId = assuranceCaseId;
        } else {
            phase13OwnedAssuranceCaseId = null;
        }
        http.postForEntity("/api/v1/twins/cells/" + CELL + "/synchronize", null, Map.class);
    }

    @Test
    void highConfidenceRecommendationScenario() {
        setKnowledge("HIGH");
        long actionsBefore = proposedActionRepository.count();
        ChangeProposalDetailDto proposal = generate();
        assertEquals(ProposalStatus.RECOMMENDED.name(), proposal.proposal().status());
        assertNotNull(proposal.proposal().proposedValue());
        assertEquals("LOW", proposal.proposal().simulationConfidence());
        assertEquals(txPower(), proposal.proposal().currentValue());
        assertEquals(txPower(), canonicalTxPower());
        assertEquals(actionsBefore, proposedActionRepository.count());
        assertFalse(proposal.candidates().isEmpty());
    }

    @Test
    void approvalWithoutExecutionSideEffects() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        assertEquals(ProposalStatus.RECOMMENDED.name(), recommended.proposal().status());
        long actionsBefore = proposedActionRepository.count();
        String canonicalBefore = canonicalTxPower();

        ChangeProposalDetailDto approved = approve(recommended.proposal().id());
        assertEquals(ProposalStatus.APPROVED.name(), approved.proposal().status());
        assertEquals(actionsBefore, proposedActionRepository.count());
        assertEquals(canonicalBefore, canonicalTxPower());
        List<ProposedActionEntity> actions = proposedActionRepository.findAll();
        assertTrue(actions.stream().noneMatch(a -> recommended.proposal().id().equals(a.getId())));
    }

    @Test
    void staleCurrentValueBlocksApproval() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        String expectedCurrent = recommended.proposal().currentValue();
        String mutatedTxPower = String.valueOf(Integer.parseInt(expectedCurrent) - 1);
        try {
            jdbc.update(
                    "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                    mutatedTxPower, CELL);

            ResponseEntity<String> response = http.exchange(
                    "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/approve",
                    HttpMethod.POST,
                    entity(new ReviewChangeProposalRequest("approver", null, "approve"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                    String.class);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotEquals(expectedCurrent, canonicalTxPower());
            ChangeProposalDetailDto reloaded = get(recommended.proposal().id());
            assertEquals(expectedCurrent, reloaded.proposal().currentValue());
        } finally {
            restoreCell001TxPower(SEED_TX_POWER);
        }
    }

    @Test
    void sharedPriorPhaseStateRestoredAfterStaleCurrentValueScenario() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        String expectedCurrent = recommended.proposal().currentValue();
        assertEquals(SEED_TX_POWER, expectedCurrent);
        try {
            jdbc.update(
                    "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                    String.valueOf(Integer.parseInt(expectedCurrent) - 1), CELL);
            assertNotEquals(SEED_TX_POWER, canonicalTxPower());
            assertFalse(assuranceCaseService.listForCell(CELL).isEmpty());
        } finally {
            restoreCell001TxPower(SEED_TX_POWER);
            cleanupPhase13AssuranceForCell001();
            jdbc.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p13-%'");
        }
        assertEquals(SEED_TX_POWER, canonicalTxPower());
        assertTrue(assuranceCaseService.listForCell(CELL).isEmpty());
        Integer p13KpiCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM kpi_observation WHERE event_id LIKE 'p13-%'",
                Integer.class);
        assertEquals(0, p13KpiCount);
    }

    @Test
    void knowledgeDegradationBlocksApproval() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        setKnowledge("LOW");
        ResponseEntity<String> low = http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/approve",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("approver", null, "approve"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, low.getStatusCode());

        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended2 = generate();
        setKnowledge("UNKNOWN");
        ResponseEntity<String> unknown = http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended2.proposal().id() + "/approve",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("approver", null, "approve"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                String.class);
        assertEquals(HttpStatus.CONFLICT, unknown.getStatusCode());
    }

    @Test
    void mediumConfidenceAllowsRecommendationWithDegradation() {
        setKnowledge("MEDIUM");
        ChangeProposalDetailDto proposal = generate();
        if (ProposalStatus.RECOMMENDED.name().equals(proposal.proposal().status())) {
            assertEquals("MEDIUM", proposal.proposal().networkKnowledgeConfidence());
        } else {
            assertEquals(ProposalStatus.EVALUATED.name(), proposal.proposal().status());
        }
    }

    @Test
    void generatorCannotApproveWithoutPermission() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/approve",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("generator", null, "self approve"), ChangeProposalAuthorizer.PERMISSION_GENERATE),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void viewerCannotGenerate() {
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-intelligence/proposals",
                HttpMethod.POST,
                entity(new GenerateChangeProposalRequest("CELL", CELL, "txPower", assuranceCaseId, null,
                        GenerationInitiator.MANUAL, "viewer"), ChangeProposalAuthorizer.PERMISSION_VIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    }

    @Test
    void reviewerCannotApproveWithReviewPermissionOnly() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/approve",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("reviewer", null, "approve"), ChangeProposalAuthorizer.PERMISSION_REVIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertTrue(forbidden.getBody().contains(ChangeProposalFailureCode.PROPOSAL_APPROVAL_FORBIDDEN.name()));
    }

    @Test
    void reviewerCannotRejectWithReviewPermissionOnly() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/reject",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("reviewer", "NOT_SUITABLE", "reject"), ChangeProposalAuthorizer.PERMISSION_REVIEW),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertTrue(forbidden.getBody().contains(ChangeProposalFailureCode.PROPOSAL_REJECTION_FORBIDDEN.name()));
    }

    @Test
    void reviewerCanAccessGovernanceEvidence() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        ResponseEntity<Map> evidence = http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/evidence",
                HttpMethod.GET,
                entity(null, ChangeProposalAuthorizer.PERMISSION_REVIEW),
                Map.class);
        assertEquals(HttpStatus.OK, evidence.getStatusCode());
        assertNotNull(evidence.getBody());
        assertEquals(recommended.proposal().id().toString(), String.valueOf(evidence.getBody().get("proposalId")));
        assertFalse(((List<?>) evidence.getBody().get("candidates")).isEmpty());
    }

    @Test
    void vendorImportPermissionDoesNotGrantProposalApproval() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        ResponseEntity<String> forbidden = http.exchange(
                "/api/v1/change-intelligence/proposals/" + recommended.proposal().id() + "/approve",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("importer", null, "approve"), VendorImportAuthorizer.PERMISSION),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertTrue(forbidden.getBody().contains(ChangeProposalFailureCode.PROPOSAL_APPROVAL_FORBIDDEN.name()));
    }

    @Test
    void currentValueChangePersistsInvalidated() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        int candidateCount = recommended.candidates().size();
        String expectedCurrent = recommended.proposal().currentValue();
        try {
            jdbc.update(
                    "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                    String.valueOf(Integer.parseInt(expectedCurrent) - 1), CELL);
            assertPersistedInvalidation(
                    recommended.proposal().id(),
                    ChangeProposalFailureCode.CURRENT_VALUE_CHANGED.name(),
                    ChangeProposalFailureCode.CURRENT_VALUE_CHANGED,
                    candidateCount);
        } finally {
            restoreCell001TxPower(SEED_TX_POWER);
        }
    }

    @Test
    void knowledgeLowPersistsInvalidated() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        int candidateCount = recommended.candidates().size();
        setKnowledge("LOW");
        assertPersistedInvalidation(
                recommended.proposal().id(),
                ChangeProposalFailureCode.KNOWLEDGE_CONFIDENCE_DEGRADED.name(),
                ChangeProposalFailureCode.KNOWLEDGE_CONFIDENCE_DEGRADED,
                candidateCount);
    }

    @Test
    void knowledgeUnknownPersistsInvalidated() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        int candidateCount = recommended.candidates().size();
        setKnowledge("UNKNOWN");
        assertPersistedInvalidation(
                recommended.proposal().id(),
                ChangeProposalFailureCode.KNOWLEDGE_CONFIDENCE_DEGRADED.name(),
                ChangeProposalFailureCode.KNOWLEDGE_CONFIDENCE_DEGRADED,
                candidateCount);
    }

    @Test
    void driftPersistsInvalidated() {
        setKnowledge("HIGH");
        ChangeProposalDetailDto recommended = generate();
        int candidateCount = recommended.candidates().size();
        insertOpenDriftForProposal(recommended.proposal().id());
        assertPersistedInvalidation(
                recommended.proposal().id(),
                ChangeProposalFailureCode.PROPOSAL_INVALIDATED.name(),
                ChangeProposalFailureCode.PROPOSAL_INVALIDATED,
                candidateCount);
    }

    @Test
    void generationTwinStaleResultsInInvalid() {
        setKnowledge("HIGH");
        jdbc.update(
                "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                "45", CELL);
        try {
            ChangeProposalDetailDto proposal = generate();
            assertEquals(ProposalStatus.INVALID.name(), proposal.proposal().status());
            assertEquals(ChangeProposalFailureCode.TWIN_STATE_STALE.name(), proposal.proposal().failureCode());
        } finally {
            restoreCell001TxPower(SEED_TX_POWER);
            http.postForEntity("/api/v1/twins/cells/" + CELL + "/synchronize", null, Map.class);
        }
    }

    @Test
    void generationSimulationFailureResultsInSimulationFailed() {
        setKnowledge("HIGH");
        int originalMaxCandidates = changeIntelligenceProperties.getMaxCandidates();
        int originalMaxDelta = changeIntelligenceProperties.getMaxDelta();
        changeIntelligenceProperties.setMaxCandidates(1);
        changeIntelligenceProperties.setMaxDelta(0);
        try {
            ChangeProposalDetailDto proposal = generate();
            assertEquals(ProposalStatus.SIMULATION_FAILED.name(), proposal.proposal().status());
            assertEquals(ChangeProposalFailureCode.SIMULATION_FAILED.name(), proposal.proposal().failureCode());
        } finally {
            changeIntelligenceProperties.setMaxCandidates(originalMaxCandidates);
            changeIntelligenceProperties.setMaxDelta(originalMaxDelta);
        }
    }

    @Test
    void generationNoBeneficialCandidateResultsInEvaluated() {
        setKnowledge("HIGH");
        BigDecimal originalMinBenefit = changeIntelligenceProperties.getMinBenefitScore();
        changeIntelligenceProperties.setMinBenefitScore(new BigDecimal("999999"));
        try {
            ChangeProposalDetailDto proposal = generate();
            assertEquals(ProposalStatus.EVALUATED.name(), proposal.proposal().status());
            assertEquals(ChangeProposalFailureCode.NO_BENEFICIAL_CANDIDATE.name(), proposal.proposal().failureCode());
        } finally {
            changeIntelligenceProperties.setMinBenefitScore(originalMinBenefit);
        }
    }

    @Test
    void generationLowKnowledgeNeverBecomesRecommended() {
        setKnowledge("LOW");
        ChangeProposalDetailDto proposal = generate();
        assertEquals(ProposalStatus.EVALUATED.name(), proposal.proposal().status());
        assertEquals(ChangeProposalFailureCode.NETWORK_KNOWLEDGE_LOW.name(), proposal.proposal().failureCode());
    }

    @Test
    void generationUnknownKnowledgeNeverBecomesRecommended() {
        setKnowledge("UNKNOWN");
        ChangeProposalDetailDto proposal = generate();
        assertEquals(ProposalStatus.EVALUATED.name(), proposal.proposal().status());
        assertEquals(ChangeProposalFailureCode.NETWORK_KNOWLEDGE_UNKNOWN.name(), proposal.proposal().failureCode());
    }

    private ChangeProposalDetailDto generate() {
        ResponseEntity<ChangeProposalDetailDto> response = http.exchange(
                "/api/v1/change-intelligence/proposals",
                HttpMethod.POST,
                entity(new GenerateChangeProposalRequest("CELL", CELL, "txPower", assuranceCaseId, null,
                        GenerationInitiator.MANUAL, "generator"), ChangeProposalAuthorizer.PERMISSION_GENERATE),
                ChangeProposalDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ChangeProposalDetailDto approve(UUID proposalId) {
        ResponseEntity<ChangeProposalDetailDto> response = http.exchange(
                "/api/v1/change-intelligence/proposals/" + proposalId + "/approve",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("approver", null, "approved"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                ChangeProposalDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ChangeProposalDetailDto get(UUID proposalId) {
        return get(proposalId, ChangeProposalAuthorizer.PERMISSION_VIEW);
    }

    private ChangeProposalDetailDto get(UUID proposalId, String permission) {
        ResponseEntity<ChangeProposalDetailDto> response = http.exchange(
                "/api/v1/change-intelligence/proposals/" + proposalId,
                HttpMethod.GET,
                entity(null, permission),
                ChangeProposalDetailDto.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<String> approveExpectingConflict(UUID proposalId) {
        return http.exchange(
                "/api/v1/change-intelligence/proposals/" + proposalId + "/approve",
                HttpMethod.POST,
                entity(new ReviewChangeProposalRequest("approver", null, "approve"), ChangeProposalAuthorizer.PERMISSION_APPROVE),
                String.class);
    }

    private void assertPersistedInvalidation(
            UUID proposalId,
            String expectedInvalidationReason,
            ChangeProposalFailureCode expectedFailureCode,
            int expectedCandidateCount
    ) {
        ResponseEntity<String> conflict = approveExpectingConflict(proposalId);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        assertTrue(conflict.getBody().contains(expectedFailureCode.name()));

        ChangeProposalDetailDto reloaded = get(proposalId);
        assertEquals(ProposalStatus.INVALIDATED.name(), reloaded.proposal().status());
        assertEquals(expectedInvalidationReason, reloaded.proposal().invalidationReason());
        Instant invalidatedAt = jdbc.queryForObject(
                "SELECT invalidated_at FROM network_change_proposal WHERE id = ?",
                Instant.class,
                proposalId);
        assertNotNull(invalidatedAt);

        ResponseEntity<Map> evidence = http.exchange(
                "/api/v1/change-intelligence/proposals/" + proposalId + "/evidence",
                HttpMethod.GET,
                entity(null, ChangeProposalAuthorizer.PERMISSION_REVIEW),
                Map.class);
        assertEquals(HttpStatus.OK, evidence.getStatusCode());
        assertEquals(expectedCandidateCount, ((List<?>) evidence.getBody().get("candidates")).size());
        List<?> auditEvents = (List<?>) evidence.getBody().get("auditEvents");
        assertTrue(auditEvents.stream()
                .map(event -> ((Map<?, ?>) event).get("eventType"))
                .anyMatch("PROPOSAL_INVALIDATED"::equals));

        ResponseEntity<String> blockedAgain = approveExpectingConflict(proposalId);
        assertEquals(HttpStatus.CONFLICT, blockedAgain.getStatusCode());
        assertTrue(blockedAgain.getBody().contains(ChangeProposalFailureCode.INVALID_PROPOSAL_STATE.name()));
    }

    private void insertOpenDriftForProposal(UUID proposalId) {
        Map<String, Object> source = jdbc.queryForMap(
                """
                SELECT source_system, source_snapshot_id
                FROM network_change_proposal
                WHERE id = ?
                """,
                proposalId);
        jdbc.update(
                """
                INSERT INTO network_drift_observation (
                    id, source_system, connector_id, synchronization_scope, drift_type, drift_status,
                    entity_type, entity_id, summary, detected_at
                ) VALUES (?, ?, ?, ?, ?, 'OPEN', 'CELL', ?, 'phase13-test-drift', NOW())
                """,
                UUID.randomUUID(),
                source.get("source_system"),
                ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER,
                "DEFAULT",
                "CONFIGURATION",
                CELL);
    }

    private <T> HttpEntity<T> entity(T body, String permission) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ChangeProposalAuthorizer.HEADER, permission);
        return new HttpEntity<>(body, headers);
    }

    private void seedTelemetry() {
        Instant t0 = Instant.parse("2026-08-25T08:00:00Z");
        double[] bler = {0.04, 0.06, 0.09, 0.12};
        double[] prb = {0.60, 0.68, 0.77, 0.84};
        for (int i = 0; i < 4; i++) {
            Instant ts = t0.plusSeconds(i * 300L);
            String prefix = "p13-" + UUID.randomUUID();
            projectionService.project(event(prefix + "-bler", CELL, "BLER_DL", bler[i], ts));
            projectionService.project(event(prefix + "-prb", CELL, "PRB_UTILIZATION_DL", prb[i], ts));
        }
    }

    private void runTrustedBaseline() {
        scenarios.use(SimulatorEnmScenario.FULL_SUCCESS);
        vendorImportAuthorizer.runWith(VendorImportAuthorizer.PERMISSION, () ->
                controlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER));
    }

    private void setKnowledge(String confidence) {
        jdbc.update("UPDATE network_knowledge_status SET confidence = ?, reason_codes = ?, freshness = 'FRESH', source_health = 'HEALTHY'",
                confidence, confidence.equals("HIGH") ? "TRUSTED_BASELINE" : "DEGRADED");
    }

    private String txPower() {
        return radioConfigurationRepository.findByCell_IdAndParameterName(
                cellRepository.findByCellId(CELL).orElseThrow().getId(), "txPower")
                .orElseThrow()
                .getParameterValue();
    }

    private String canonicalTxPower() {
        return txPower();
    }

    private void restoreSharedPriorPhaseState() {
        restoreCell001TxPower(SEED_TX_POWER);
    }

    private void restoreCell001TxPower(String txPower) {
        jdbc.update(
                "UPDATE radio_configuration SET parameter_value = ? WHERE parameter_name = 'txPower' AND cell_id = (SELECT id FROM cell WHERE cell_id = ?)",
                txPower, CELL);
    }

    private void cleanupPhase13AssuranceForCell001() {
        if (phase13OwnedAssuranceCaseId != null) {
            jdbc.update("DELETE FROM assurance_evidence WHERE assurance_case_id = ?", phase13OwnedAssuranceCaseId);
            jdbc.update("DELETE FROM assurance_case WHERE id = ?", phase13OwnedAssuranceCaseId);
            phase13OwnedAssuranceCaseId = null;
        }
        jdbc.update(
                """
                DELETE FROM assurance_evidence WHERE assurance_case_id IN (
                    SELECT id FROM assurance_case
                    WHERE affected_entity_id = ? AND synthetic = TRUE AND rule_id = 'RULE_DEGRADING_RADIO_QUALITY_BLER_DL_V1'
                )
                """,
                CELL);
        jdbc.update(
                """
                DELETE FROM assurance_case
                WHERE affected_entity_id = ? AND synthetic = TRUE AND rule_id = 'RULE_DEGRADING_RADIO_QUALITY_BLER_DL_V1'
                """,
                CELL);
    }

    private static TelemetryEvent event(String eventId, String cellId, String metric, double value, Instant eventTime) {
        return new TelemetryEvent(
                eventId,
                TelemetryEvent.TYPE_CELL_KPI_OBSERVED,
                TelemetryEvent.SCHEMA_V1,
                TelemetryEvent.SOURCE_SIMULATOR,
                cellId,
                metric,
                value,
                "ratio",
                eventTime,
                null,
                true
        );
    }
}
