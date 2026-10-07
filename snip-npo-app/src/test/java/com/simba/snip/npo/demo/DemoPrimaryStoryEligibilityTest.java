package com.simba.snip.npo.demo;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.api.TwinDetailDto;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.assurance.CaseStatus;
import com.simba.snip.npo.assurance.CaseType;
import com.simba.snip.npo.assurance.Severity;
import com.simba.snip.npo.changeintelligence.api.ChangeProposalDetailDto;
import com.simba.snip.npo.changeintelligence.api.GenerateChangeProposalRequest;
import com.simba.snip.npo.changeintelligence.authorization.ChangeProposalAuthorizer;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.changeintelligence.model.GenerationInitiator;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.planning.api.CreatePlanningScenarioRequest;
import com.simba.snip.npo.planning.api.EvaluatePlanningScenarioRequest;
import com.simba.snip.npo.planning.api.PlanningAlternativeRequest;
import com.simba.snip.npo.planning.api.PlanningCellRequest;
import com.simba.snip.npo.planning.api.PlanningEvaluationDto;
import com.simba.snip.npo.planning.api.PlanningIntentRequest;
import com.simba.snip.npo.planning.api.PlanningScenarioDetailDto;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NpoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
class DemoPrimaryStoryEligibilityTest extends AbstractPostgresIT {

    @Autowired private TestRestTemplate http;
    @Autowired private AssuranceCaseService assuranceCaseService;
    @Autowired private RadioConfigurationRepository radioConfigurationRepository;
    @Autowired private com.simba.snip.npo.persist.CellRepository cellRepository;
    @Autowired private NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    @Autowired private KnowledgeGate knowledgeGate;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void cell001ReachesRecommendedThroughLivePaths() {
        var cell = cellRepository.findByCellId("CELL-001").orElseThrow();
        assertEquals("46", radioConfigurationRepository
                .findByCell_IdAndParameterName(cell.getId(), "txPower").orElseThrow().getParameterValue());

        Integer recent = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM kpi_observation k
                JOIN cell c ON c.id = k.cell_id
                WHERE c.cell_id = 'CELL-001' AND k.metric = 'BLER_DL'
                  AND k.observed_at > NOW() - INTERVAL '168 hours'
                """,
                Integer.class);
        assertTrue(recent != null && recent >= 2);

        var cases = assuranceCaseService.listForCell("CELL-001");
        assertEquals(1, cases.size());
        assertEquals(CaseType.DEGRADING_RADIO_QUALITY.name(), cases.get(0).getCaseType());
        assertEquals(Severity.CRITICAL.name(), cases.get(0).getSeverity());
        assertEquals(CaseStatus.OPEN.name(), cases.get(0).getStatus());

        var knowledge = knowledgeStatusRepository
                .findBySourceSystemAndSynchronizationScope("ERICSSON_ENM_SIMULATOR", "DEFAULT")
                .orElseThrow();
        assertTrue(knowledgeGate.evaluate(knowledge.getConfidence()).allowsRecommendation());

        TwinDetailDto twin = http.postForObject(
                "/api/v1/twins/cells/CELL-001/synchronize", null, TwinDetailDto.class);
        assertEquals("CURRENT", twin.freshness());

        PlanningScenarioDetailDto scenario = http.postForEntity(
                "/api/v1/planning/scenarios",
                new CreatePlanningScenarioRequest(
                        "CELL-001 demo eligibility",
                        "",
                        "demo-network-engineer",
                        List.of(new PlanningCellRequest("CELL-001"), new PlanningCellRequest("CELL-002")),
                        List.of(new PlanningAlternativeRequest(
                                "Alternative A",
                                1,
                                List.of(
                                        new PlanningIntentRequest("CELL-001", "txPower", BigDecimal.valueOf(44)),
                                        new PlanningIntentRequest("CELL-002", "txPower", BigDecimal.valueOf(42))
                                )
                        ))
                ),
                PlanningScenarioDetailDto.class
        ).getBody();
        assertNotNull(scenario);

        ResponseEntity<PlanningEvaluationDto> evaluated = http.postForEntity(
                "/api/v1/planning/scenarios/" + scenario.id() + "/evaluations",
                new EvaluatePlanningScenarioRequest("demo-network-engineer", scenario.rowVersion()),
                PlanningEvaluationDto.class
        );
        assertTrue(evaluated.getStatusCode().is2xxSuccessful());
        assertNotNull(evaluated.getBody());
        assertNotEquals("FAILED", evaluated.getBody().status());
        assertTrue(evaluated.getBody().items().stream()
                .anyMatch(item -> "CELL-001".equals(item.cellId()) && "SUCCEEDED".equals(item.outcome())));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set(ChangeProposalAuthorizer.HEADER, ChangeProposalAuthorizer.PERMISSION_GENERATE);
        ResponseEntity<ChangeProposalDetailDto> generated = http.exchange(
                "/api/v1/change-intelligence/proposals",
                HttpMethod.POST,
                new HttpEntity<>(new GenerateChangeProposalRequest(
                        "CELL",
                        "CELL-001",
                        "txPower",
                        cases.get(0).getId(),
                        null,
                        GenerationInitiator.MANUAL,
                        "generator"
                ), headers),
                ChangeProposalDetailDto.class
        );
        assertEquals(HttpStatus.OK, generated.getStatusCode());
        assertNotNull(generated.getBody());
        assertEquals(ProposalStatus.RECOMMENDED.name(), generated.getBody().proposal().status());
        assertNotEquals(ChangeProposalFailureCode.NETWORK_KNOWLEDGE_UNKNOWN.name(),
                generated.getBody().proposal().failureCode());
        assertNotEquals(ChangeProposalFailureCode.NETWORK_KNOWLEDGE_LOW.name(),
                generated.getBody().proposal().failureCode());
        assertNotEquals(ChangeProposalFailureCode.NO_BENEFICIAL_CANDIDATE.name(),
                generated.getBody().proposal().failureCode());
        assertFalse(generated.getBody().candidates().isEmpty());
    }
}
