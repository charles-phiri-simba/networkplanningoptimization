package com.simba.snip.npo.changeexecution;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeexecution.adapter.simulator.SimulatorExecutionAdapter;
import com.simba.snip.npo.changeexecution.domain.ExecutionStatus;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.service.ExecutionTargetRegistry;
import com.simba.snip.npo.integration.enm.EnmTransport;
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

class ChangeExecutionArchitectureIsolationTest {

    private static final String ROOT = "src/main/java/com/simba/snip/npo/changeexecution";

    @Test
    void phase15DoesNotReferenceEnmTransportOrConnectors() throws IOException {
        assertNoForbiddenImport(ROOT);
    }

    @Test
    void phase15DoesNotReferenceKeyVaultOrAzure() throws IOException {
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
    void networkChangeExecutionIsDistinctFromPlan() {
        assertNotSame(NetworkChangeExecutionEntity.class, NetworkChangePlanEntity.class);
        assertNotEquals("NetworkChangePlanEntity", NetworkChangeExecutionEntity.class.getSimpleName());
    }

    @Test
    void appliedAndVerifiedAreDistinctLifecycleStates() {
        assertNotEquals(ExecutionStatus.APPLIED, ExecutionStatus.VERIFIED);
        assertTrue(ExecutionStatus.APPLIED.allowsVerify());
        assertFalse(ExecutionStatus.VERIFIED.allowsVerify());
    }

    @Test
    void v16ContainsAllExecutionEvidenceTables() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V16__phase15_governed_change_execution.sql"));
        for (String table : java.util.List.of(
                "network_change_execution",
                "network_change_execution_operation",
                "network_change_execution_attempt",
                "network_change_execution_authorization",
                "network_change_execution_verification",
                "network_change_execution_recovery",
                "network_change_execution_rollback",
                "network_change_execution_audit_event")) {
            assertTrue(migration.contains("CREATE TABLE " + table));
        }
    }

    @Test
    void noProductionWriteAdapter() throws IOException {
        String registry = Files.readString(Path.of(ROOT, "service/ExecutionTargetRegistry.java"));
        assertFalse(registry.contains("EnmTransport"));
        assertFalse(registry.contains("EricssonEnmConnector"));
        assertTrue(registry.contains("SIMULATOR_TARGET_ID"));
    }

    @Test
    void noForbiddenExecuteEndpoints() throws IOException {
        String controller = Files.readString(Path.of(ROOT, "api/ChangeExecutionController.java"));
        assertFalse(controller.contains("/vendor-command"));
        assertFalse(controller.contains("credential"));
        assertTrue(controller.contains("/execute"));
    }

    @Test
    void noSecretsInMigration() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V16__phase15_governed_change_execution.sql"));
        String lower = migration.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("credential"));
        assertFalse(lower.contains("password"));
        assertFalse(lower.contains("private_key"));
    }

    @Test
    void executionDoesNotMutateCanonicalState() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(ROOT))) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("RadioConfigurationRepository.save")
                            || source.contains("NetworkReconciliationService");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }

    @Test
    void agentCannotExecuteOrAuthorize() throws IOException {
        Path agentRoot = Path.of("src/main/java/com/simba/snip/npo/agent");
        if (!Files.exists(agentRoot)) {
            return;
        }
        try (Stream<Path> files = Files.walk(agentRoot)) {
            assertFalse(files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("NetworkChangeExecutionService")
                            || source.contains("ChangeExecutionController");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }));
        }
    }

    @Test
    void mcpCannotExecuteOrRollback() throws IOException {
        Path mcpRoot = Path.of("src/main/java/com/simba/snip/npo/mcp");
        if (!Files.exists(mcpRoot)) {
            return;
        }
        try (Stream<Path> files = Files.walk(mcpRoot)) {
            assertFalse(files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("ChangeExecutionController")
                            || source.contains("NetworkChangeExecutionService");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }));
        }
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
    void simulatorAdapterDistinctFromEnmTransport() {
        assertNotSame(SimulatorExecutionAdapter.class, EnmTransport.class);
        assertTrue(SimulatorExecutionAdapter.TARGET_ID.equals(ExecutionTargetRegistry.SIMULATOR_TARGET_ID));
    }

    private static void assertNoForbiddenImport(String root) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("EnmTransport")
                            || source.contains("EricssonEnmConnector")
                            || source.contains("CredentialHandle")
                            || source.contains("Ericsson")
                            || source.contains("KeyVault")
                            || source.contains("SecretClient");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }
}
