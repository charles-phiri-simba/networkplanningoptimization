package com.simba.snip.npo.changeplanning;

import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.service.ChangePlanGovernanceService;
import com.simba.snip.npo.changeplanning.service.NetworkChangePlanService;
import com.simba.snip.npo.integration.enm.EnmTransport;
import com.simba.snip.npo.persist.ProposedActionEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangePlanningArchitectureIsolationTest {

    private static final String ROOT = "src/main/java/com/simba/snip/npo/changeplanning";

    @Test
    void phase14DoesNotReferenceEnmTransportOrConnectors() throws IOException {
        assertNoForbiddenImport(ROOT);
    }

    @Test
    void phase14DoesNotReferenceKeyVaultOrAzure() throws IOException {
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
    void networkChangePlanIsDistinctFromProposedAction() {
        assertNotSame(NetworkChangePlanEntity.class, ProposedActionEntity.class);
        assertNotEquals("ProposedActionEntity", NetworkChangePlanEntity.class.getSimpleName());
    }

    @Test
    void networkChangePlanIsDistinctFromProposal() {
        assertNotSame(NetworkChangePlanEntity.class, NetworkChangeProposalEntity.class);
    }

    @Test
    void noAutomaticProposedActionConversion() throws IOException {
        String governance = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeplanning/service/ChangePlanGovernanceService.java"));
        assertFalse(governance.contains("ProposedAction"));
        assertFalse(governance.contains("ActionExecution"));
        assertFalse(governance.contains("McpCapabilityGateway"));
    }

    @Test
    void planCreationDoesNotMutateCanonicalState() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeplanning/service/NetworkChangePlanService.java"));
        assertFalse(service.contains("RadioConfigurationRepository.save"));
        assertFalse(service.contains("NetworkReconciliationService"));
    }

    @Test
    void noVendorWriteCapabilityAdded() {
        for (java.lang.reflect.Method method : EnmTransport.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            assertFalse(name.contains("write"));
            assertFalse(name.contains("mutate"));
        }
    }

    @Test
    void noExecutableVendorCommandPersisted() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V15__phase14_change_execution_planning.sql"));
        String lower = migration.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("vendor_command"));
        assertFalse(lower.contains("credential"));
        assertFalse(lower.matches("(?s).*\\bendpoint\\b.*"));
    }

    @Test
    void noForbiddenExecuteEndpoints() throws IOException {
        String controller = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeplanning/api/ChangePlanningController.java"));
        assertFalse(controller.contains("/execute"));
        assertFalse(controller.contains("/apply"));
        assertFalse(controller.contains("/rollback"));
        assertFalse(controller.contains("/vendor-command"));
    }

    @Test
    void readinessServiceHasNoExecutionSideEffects() throws IOException {
        String readiness = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeplanning/service/ChangePlanReadinessService.java"));
        assertFalse(readiness.contains("EnmTransport"));
        assertFalse(readiness.contains("Mcp"));
        assertFalse(readiness.contains("ActionExecution"));
    }

    @Test
    void invalidationUsesRequiresNewBoundary() throws IOException {
        String invalidation = Files.readString(Path.of(
                "src/main/java/com/simba/snip/npo/changeplanning/service/ChangePlanInvalidationPersistenceService.java"));
        assertTrue(invalidation.contains("REQUIRES_NEW"));
    }

    private static void assertNoForbiddenImport(String root) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("EnmTransport")
                            || source.contains("EricssonEnmConnector")
                            || source.contains("CredentialHandle")
                            || source.contains("ActionExecutionService")
                            || source.contains("McpCapabilityGateway");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }
}
