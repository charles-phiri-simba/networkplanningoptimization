package com.simba.snip.npo.changeintelligence;

import com.simba.snip.npo.persist.ProposedActionEntity;
import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.service.ChangeProposalGovernanceService;
import com.simba.snip.npo.changeintelligence.service.NetworkChangeProposalGenerationService;
import com.simba.snip.npo.integration.enm.EnmTransport;
import com.simba.snip.npo.twin.DigitalTwinSimulationService;
import com.simba.snip.npo.twin.SimulatableParameterRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeIntelligenceArchitectureIsolationTest {

    private static final String ROOT = "src/main/java/com/simba/snip/npo/changeintelligence";

    @Test
    void phase13DoesNotReferenceEnmTransportOrConnectors() throws IOException {
        assertNoForbiddenImport(ROOT);
    }

    @Test
    void phase13DoesNotReferenceKeyVaultOrAzure() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(ROOT))) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path).toLowerCase(Locale.ROOT);
                    return source.contains("keyvault") || source.contains("azure.identity")
                            || source.contains("secretclient");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }

    @Test
    void noDuplicateSimulatableParameterRegistry() throws IOException {
        String source = concatSources(ROOT);
        assertFalse(source.contains("class Phase13SimulatableParameterRegistry"));
        assertFalse(source.contains("class ChangeIntelligenceParameterRegistry"));
        assertTrue(source.contains("SimulatableParameterRegistry"));
    }

    @Test
    void noDuplicateSimulationService() throws IOException {
        String source = concatSources(ROOT);
        assertFalse(source.contains("class Phase13DigitalTwinSimulationService"));
        assertTrue(source.contains("DigitalTwinSimulationService"));
    }

    @Test
    void noDuplicateKnowledgeEvaluator() throws IOException {
        String source = concatSources(ROOT);
        assertFalse(source.contains("class Phase13NetworkKnowledgeConfidenceEvaluator"));
        assertFalse(source.contains("class ChangeIntelligenceKnowledgeEvaluator"));
    }

    @Test
    void noDuplicateDriftEngine() throws IOException {
        String source = concatSources(ROOT);
        assertFalse(source.contains("class Phase13NetworkDriftService"));
        assertFalse(source.contains("class ChangeIntelligenceDriftService"));
    }

    @Test
    void threeConfidenceDomainsNotCollapsed() throws IOException {
        String entity = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/persist/NetworkChangeProposalEntity.java"));
        assertTrue(entity.contains("networkKnowledgeConfidence"));
        assertTrue(entity.contains("assuranceConfidence"));
        assertTrue(entity.contains("simulationConfidence"));
        assertFalse(entity.contains("overallConfidence"));
    }

    @Test
    void networkChangeProposalIsDistinctFromProposedAction() {
        assertNotSame(NetworkChangeProposalEntity.class, ProposedActionEntity.class);
        assertNotEquals("ProposedActionEntity", NetworkChangeProposalEntity.class.getSimpleName());
    }

    @Test
    void noAutomaticProposedActionConversion() throws IOException {
        String governance = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalGovernanceService.java"));
        assertFalse(governance.contains("ProposedAction"));
        assertFalse(governance.contains("ActionService"));
        assertFalse(governance.contains("McpCapabilityGateway"));
    }

    @Test
    void agentPackagesDoNotApproveChangeProposals() throws IOException {
        assertNoChangeProposalGovernanceIn("src/main/java/com/simba/snip/npo/agent");
    }

    @Test
    void approvalPathDoesNotInvokeMcpOrExecution() throws IOException {
        String governance = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalGovernanceService.java"));
        assertFalse(governance.contains("Mcp"));
        assertFalse(governance.contains("executeFromMcp"));
        assertFalse(governance.contains("ActionExecution"));
    }

    @Test
    void approvalDoesNotMutateCanonicalState() throws IOException {
        String governance = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/ChangeProposalGovernanceService.java"));
        assertFalse(governance.contains("RadioConfigurationRepository"));
        assertFalse(governance.contains("NetworkReconciliationService"));
    }

    @Test
    void noVendorWriteCapabilityAdded() throws IOException {
        for (java.lang.reflect.Method method : EnmTransport.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            assertFalse(name.contains("write"));
            assertFalse(name.contains("mutate"));
        }
    }

    @Test
    void noExecutableVendorCommandPersisted() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V14__phase13_change_intelligence.sql"));
        assertFalse(migration.toLowerCase(Locale.ROOT).contains("vendor_command"));
        assertFalse(migration.toLowerCase(Locale.ROOT).contains("endpoint"));
    }

    @Test
    void noPhase14PackageIntroduced() throws IOException {
        assertFalse(Files.exists(Path.of("src/main/java/com/simba/snip/npo/phase14")));
    }

    @Test
    void generationUsesAuthoritativeSimulationService() throws IOException {
        String generation = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeintelligence/service/NetworkChangeProposalGenerationService.java"));
        assertTrue(generation.contains("DigitalTwinSimulationService"));
        assertTrue(generation.contains("executeFromMcp"));
    }

    @Test
    void registryRemainsAuthoritativeForTxPower() {
        assertTrue(SimulatableParameterRegistry.find("txPower").isPresent());
    }

    @Test
    void digitalTwinSimulationServiceUnchangedAuthority() {
        assertTrue(DigitalTwinSimulationService.class.getPackageName().contains("twin"));
    }

    private static void assertNoForbiddenImport(String root) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("integration.enm.EnmTransport")
                            || source.contains("integration.enm.EricssonEnmConnector")
                            || source.contains("EricssonEnmConnector");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }

    private static void assertNoChangeProposalGovernanceIn(String root) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("ChangeProposalGovernanceService")
                            || source.contains("ChangeProposalAuthorizer.PERMISSION_APPROVE");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }

    private static String concatSources(String root) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    builder.append(Files.readString(path));
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
        return builder.toString();
    }
}
