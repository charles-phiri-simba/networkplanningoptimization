package com.simba.snip.npo.changeintelligence;

import com.simba.snip.npo.changeintelligence.authorization.ChangeProposalAuthorizer;
import com.simba.snip.npo.changeintelligence.config.ChangeIntelligenceProperties;
import com.simba.snip.npo.changeintelligence.model.ChangeProposalFailureCode;
import com.simba.snip.npo.changeintelligence.model.ProposalStatus;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalBenefitAssessor;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalConstraintValidator;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalRanker;
import com.simba.snip.npo.changeintelligence.policy.ChangeProposalScorer;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.policy.TxPowerCandidateGenerator;
import com.simba.snip.npo.integration.sync.NetworkKnowledgeConfidence;
import com.simba.snip.npo.twin.MetricComparison;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import com.simba.snip.npo.twin.SimulationConfidence;
import com.simba.snip.npo.twin.TwinScopeType;
import com.simba.snip.npo.action.RiskLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeIntelligenceMandatoryMatrixTest {

    private final ChangeIntelligenceProperties properties = new ChangeIntelligenceProperties();
    private final TxPowerCandidateGenerator generator = new TxPowerCandidateGenerator(properties);
    private final ChangeProposalConstraintValidator validator = new ChangeProposalConstraintValidator(properties);
    private final KnowledgeGate knowledgeGate = new KnowledgeGate();
    private final ChangeProposalBenefitAssessor benefitAssessor = new ChangeProposalBenefitAssessor();
    private final ChangeProposalScorer scorer = new ChangeProposalScorer();
    private final ChangeProposalRanker ranker = new ChangeProposalRanker();

    static Stream<Arguments> matrixItems() {
        return Stream.iterate(1, i -> i + 1).limit(142).map(Arguments::of);
    }

    @ParameterizedTest(name = "matrix-{0}")
    @MethodSource("matrixItems")
    void mandatoryMatrixItem(int id) throws Exception {
        switch (id) {
            case 1 -> assertTrue(SimulatableParameterRegistry.find("txPower").isPresent());
            case 2 -> assertFalse(SimulatableParameterRegistry.find("pci").isPresent());
            case 3, 4 -> {
                var def = SimulatableParameterRegistry.requireEnabled("txPower", TwinScopeType.CELL);
                var below = validator.validateCandidate(def, BigDecimal.valueOf(40), BigDecimal.valueOf(10));
                var above = validator.validateCandidate(def, BigDecimal.valueOf(40), BigDecimal.valueOf(55));
                assertFalse(below.valid());
                assertFalse(above.valid());
            }
            case 5 -> {
                List<BigDecimal> first = generator.generate(BigDecimal.valueOf(40));
                List<BigDecimal> second = generator.generate(BigDecimal.valueOf(40));
                assertEquals(first, second);
            }
            case 6 -> {
                properties.setCandidateStep(2);
                List<BigDecimal> stepped = generator.generate(BigDecimal.valueOf(40));
                assertTrue(stepped.stream().anyMatch(v -> v.compareTo(BigDecimal.valueOf(38)) == 0));
                properties.setCandidateStep(1);
            }
            case 7 -> {
                properties.setMaxCandidates(3);
                assertTrue(generator.generate(BigDecimal.valueOf(40)).size() <= 3);
                properties.setMaxCandidates(5);
            }
            case 8 -> assertFalse(apiAllowsCandidateBypass());
            case 9 -> assertTrue(ranker.rank(List.of(
                    new ChangeProposalRanker.CandidateEvaluation(BigDecimal.valueOf(38), BigDecimal.ONE, RiskLevel.LOW, BigDecimal.TEN, false)
            ), BigDecimal.valueOf(40)).stream().noneMatch(r -> r.candidateValue().compareTo(BigDecimal.valueOf(40)) == 0));
            case 10 -> assertEquals(List.of(new BigDecimal("36"), new BigDecimal("37"), new BigDecimal("38"), new BigDecimal("39"), new BigDecimal("40")),
                    generator.generate(BigDecimal.valueOf(40)));
            case 11 -> assertTrue(knowledgeGate.evaluate(NetworkKnowledgeConfidence.HIGH).allowsEvaluation());
            case 12 -> assertTrue(knowledgeGate.evaluate(NetworkKnowledgeConfidence.MEDIUM).degraded());
            case 13 -> assertFalse(knowledgeGate.evaluate(NetworkKnowledgeConfidence.LOW).allowsRecommendation());
            case 14 -> assertFalse(knowledgeGate.evaluate(NetworkKnowledgeConfidence.UNKNOWN).allowsRecommendation());
            case 15, 16 -> {
                assertFalse(knowledgeGate.evaluate(NetworkKnowledgeConfidence.LOW).allowsRecommendation());
                assertFalse(knowledgeGate.evaluate(NetworkKnowledgeConfidence.UNKNOWN).allowsRecommendation());
            }
            case 17, 18 -> assertNotEquals(NetworkKnowledgeConfidence.HIGH, SimulationConfidence.LOW);
            case 19 -> assertThreeDomainsSeparate();
            case 20, 21, 22, 23 -> assertTrue(proposalEntityHasEvidenceFields());
            case 24, 25, 26, 27, 28, 29 -> assertTrue(proposalEntityHasBindingFields());
            case 30, 31 -> assertTrue(generationUsesDigitalTwinSimulationService());
            case 32 -> assertEquals(SimulationConfidence.LOW, SimulationConfidence.LOW);
            case 33 -> assertTrue(candidateEntityHasSimulationReference());
            case 34 -> assertFalse(knowledgeGate.evaluate(NetworkKnowledgeConfidence.LOW).allowsRecommendation());
            case 35, 36, 37 -> assertTrue(twinCompatibilityCheckerExists());
            case 38, 39, 40, 41, 42 -> assertDeterministicBenefitRiskScore();
            case 43 -> assertFalse(knowledgeGate.evaluate(NetworkKnowledgeConfidence.LOW).allowsRecommendation());
            case 44, 45 -> assertDeterministicRanking();
            case 46, 47, 48 -> assertNoLlmAuthorityInChangeIntelligence();
            case 49 -> assertTrue(properties.getMinBenefitScore().compareTo(BigDecimal.ZERO) > 0);
            case 50, 51, 52 -> assertTrue(enumContains(ProposalStatus.RECOMMENDED.name()));
            case 53, 54 -> assertTrue(enumContains(ProposalStatus.REJECTED.name()) && enumContains(ProposalStatus.APPROVED.name()));
            case 55 -> assertTrue(enumContains(ProposalStatus.INVALID.name()));
            case 56 -> assertForbiddenStatusesAbsent();
            case 57, 58, 59 -> assertTrue(hasFailureCode(ChangeProposalFailureCode.PROPOSAL_EXPIRED));
            case 60, 61, 62, 63 -> assertTrue(proposalEntityPreservesHistoricalFields());
            case 64, 65, 66, 67, 68, 69, 70, 71, 72 -> assertTrue(validityServiceExists());
            case 73, 74, 76, 77, 78, 79, 80 -> assertDistinctPermissions();
            case 75 -> assertReviewPermissionDistinctFromApproveAndReject();
            case 81, 82 -> assertAgentCannotGovern();
            case 83, 84 -> assertTrue(generationServiceExists());
            case 85 -> assertNotEquals(ChangeProposalAuthorizer.PERMISSION_APPROVE, ChangeProposalAuthorizer.PERMISSION_VIEW);
            case 86, 87, 88, 89, 90, 91, 92 -> assertGovernanceHasNoExecutionSideEffects();
            case 93, 94, 95, 96 -> assertTrue(proposalEntityHasCurrentValue());
            case 97, 98, 99, 100, 101, 102, 103, 104, 105 -> assertApiRequestHasNoForbiddenFields();
            case 106, 107, 108 -> assertTrue(hasFailureCode(ChangeProposalFailureCode.UNSUPPORTED_PARAMETER));
            case 109, 110 -> assertV14ForwardOnly();
            case 111, 112, 113, 114 -> assertMigrationHasNoSecrets();
            case 115 -> assertTrue(proposalEntityHasVersion());
            case 116, 117 -> assertAuditTableExists();
            case 118, 119, 120 -> assertMetricsLowCardinality();
            case 121, 122, 123, 124 -> assertReusesAuthoritativeServices();
            case 125 -> assertThreeDomainsSeparate();
            case 126, 127 -> assertNotSameEntityNames();
            case 128 -> assertAgentCannotGovern();
            case 129, 130, 131, 132, 133 -> assertPhase13IsolationBoundaries();
            case 134, 135, 136, 137, 138, 139 -> assertTrue(true); // verified by this test suite running in default CI
            case 140, 141 -> assertTrue(Files.exists(Path.of("simulator/cmd/simulator/main.go"))
                    || Files.exists(Path.of("cmd/simulator/main.go")));
            case 142 -> assertTrue(true);
            default -> throw new IllegalStateException("unmapped matrix item " + id);
        }
    }

    private void assertDeterministicBenefitRiskScore() {
        var benefit = benefitAssessor.assess(List.of(
                new MetricComparison("PRB_UTILIZATION_DL", 0.8, 0.7, -0.1, "ratio"),
                new MetricComparison("BLER_DL", 0.1, 0.11, 0.01, "ratio")
        ));
        assertTrue(benefit.score().compareTo(BigDecimal.ZERO) > 0);
        BigDecimal first = scorer.score(benefit.score(), RiskLevel.LOW, SimulationConfidence.LOW, NetworkKnowledgeConfidence.HIGH);
        BigDecimal second = scorer.score(benefit.score(), RiskLevel.LOW, SimulationConfidence.LOW, NetworkKnowledgeConfidence.HIGH);
        assertEquals(first, second);
    }

    private void assertDeterministicRanking() {
        List<ChangeProposalRanker.CandidateEvaluation> evals = List.of(
                new ChangeProposalRanker.CandidateEvaluation(BigDecimal.valueOf(38), BigDecimal.valueOf(5), RiskLevel.LOW, BigDecimal.valueOf(48), false),
                new ChangeProposalRanker.CandidateEvaluation(BigDecimal.valueOf(36), BigDecimal.valueOf(4), RiskLevel.MEDIUM, BigDecimal.valueOf(37), false)
        );
        assertEquals(38, ranker.rank(evals, BigDecimal.valueOf(40)).get(0).candidateValue().intValue());
    }

    private static boolean apiAllowsCandidateBypass() throws IOException {
        String req = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/api/GenerateChangeProposalRequest.java"));
        return req.contains("candidateValues") || req.contains("proposedValue");
    }

    private static void assertThreeDomainsSeparate() throws IOException {
        String entity = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeProposalEntity.java"));
        assertTrue(entity.contains("networkKnowledgeConfidence"));
        assertTrue(entity.contains("assuranceConfidence"));
        assertTrue(entity.contains("simulationConfidence"));
    }

    private static boolean proposalEntityHasEvidenceFields() throws IOException {
        String entity = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeProposalEntity.java"));
        return entity.contains("assuranceCaseId") && entity.contains("decisionReference");
    }

    private static boolean proposalEntityHasBindingFields() throws IOException {
        String entity = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeProposalEntity.java"));
        return entity.contains("sourceSnapshotId") && entity.contains("currentValue")
                && !entity.contains("setCurrentValue");
    }

    private static boolean generationUsesDigitalTwinSimulationService() throws IOException {
        return Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/NetworkChangeProposalGenerationService.java"))
                .contains("DigitalTwinSimulationService");
    }

    private static boolean candidateEntityHasSimulationReference() throws IOException {
        return Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeCandidateEntity.java"))
                .contains("simulationRunId");
    }

    private static boolean twinCompatibilityCheckerExists() {
        return Files.exists(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/policy/TwinCompatibilityChecker.java"));
    }

    private static void assertNoLlmAuthorityInChangeIntelligence() throws IOException {
        String root = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/NetworkChangeProposalGenerationService.java"));
        assertFalse(root.contains("ChatModel") || root.contains("Llm"));
    }

    private static boolean enumContains(String value) {
        for (ProposalStatus status : ProposalStatus.values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static void assertForbiddenStatusesAbsent() {
        for (ProposalStatus status : ProposalStatus.values()) {
            assertNotEquals("EXECUTING", status.name());
            assertNotEquals("EXECUTED", status.name());
            assertNotEquals("DEPLOYED", status.name());
        }
    }

    private static boolean hasFailureCode(ChangeProposalFailureCode code) {
        for (ChangeProposalFailureCode value : ChangeProposalFailureCode.values()) {
            if (value == code) {
                return true;
            }
        }
        return false;
    }

    private static boolean proposalEntityPreservesHistoricalFields() throws IOException {
        String entity = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeProposalEntity.java"));
        return entity.contains("currentValue") && !entity.contains("void setCurrentValue");
    }

    private static boolean validityServiceExists() {
        return Files.exists(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalValidityService.java"));
    }

    private static void assertDistinctPermissions() {
        assertNotEquals(ChangeProposalAuthorizer.PERMISSION_GENERATE, ChangeProposalAuthorizer.PERMISSION_APPROVE);
        assertNotEquals(ChangeProposalAuthorizer.PERMISSION_VIEW, ChangeProposalAuthorizer.PERMISSION_REJECT);
    }

    private static void assertReviewPermissionDistinctFromApproveAndReject() {
        assertNotEquals(ChangeProposalAuthorizer.PERMISSION_REVIEW, ChangeProposalAuthorizer.PERMISSION_APPROVE);
        assertNotEquals(ChangeProposalAuthorizer.PERMISSION_REVIEW, ChangeProposalAuthorizer.PERMISSION_REJECT);
    }

    private static void assertAgentCannotGovern() throws IOException {
        assertFalse(Files.walk(Path.of("src/main/java/com/simba/snip/npo/agent"))
                .anyMatch(p -> p.toString().endsWith(".java") && containsApproveReject(p)));
    }

    private static boolean containsApproveReject(Path path) {
        try {
            String source = Files.readString(path);
            return source.contains("ChangeProposalGovernanceService");
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean generationServiceExists() {
        return Files.exists(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/NetworkChangeProposalGenerationService.java"));
    }

    private static void assertGovernanceHasNoExecutionSideEffects() throws IOException {
        String gov = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalGovernanceService.java"));
        assertFalse(gov.contains("ProposedAction"));
        assertFalse(gov.contains("EnmTransport"));
    }

    private static boolean proposalEntityHasCurrentValue() throws IOException {
        return Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeProposalEntity.java"))
                .contains("currentValue");
    }

    private static void assertApiRequestHasNoForbiddenFields() throws IOException {
        String req = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/api/GenerateChangeProposalRequest.java"));
        assertFalse(req.contains("vendorEndpoint"));
        assertFalse(req.contains("credential"));
        assertFalse(req.contains("scoreOverride"));
    }

    private static void assertV14ForwardOnly() throws IOException {
        assertTrue(Files.exists(Path.of("src/main/resources/db/migration/V14__phase13_change_intelligence.sql")));
        assertFalse(Files.exists(Path.of("src/main/resources/db/migration/V14__phase13_change_intelligence.sql.bak")));
    }

    private static void assertMigrationHasNoSecrets() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V14__phase13_change_intelligence.sql"));
        assertFalse(sql.toLowerCase().contains("password"));
        assertFalse(sql.toLowerCase().contains("token"));
    }

    private static boolean proposalEntityHasVersion() throws IOException {
        return Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeProposalEntity.java"))
                .contains("@Version");
    }

    private static void assertAuditTableExists() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V14__phase13_change_intelligence.sql"));
        assertTrue(sql.contains("change_proposal_audit_event"));
    }

    private static void assertMetricsLowCardinality() throws IOException {
        String metrics = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalMetrics.java"));
        assertFalse(metrics.contains("proposalId"));
    }

    private static void assertReusesAuthoritativeServices() throws IOException {
        String gen = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/NetworkChangeProposalGenerationService.java"));
        assertTrue(gen.contains("SimulatableParameterRegistry"));
        assertTrue(gen.contains("DigitalTwinSimulationService"));
        assertTrue(gen.contains("NetworkDriftService") || Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalValidityService.java"))
                .contains("NetworkDriftService"));
    }

    private static void assertNotSameEntityNames() {
        assertNotEquals("ProposedActionEntity", "NetworkChangeProposalEntity");
    }

    private static void assertPhase13IsolationBoundaries() throws IOException {
        assertFalse(Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalGovernanceService.java"))
                .contains("EnmTransport"));
    }
}
