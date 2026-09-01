package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.action.CapabilityRegistry;
import com.simba.snip.npo.agent.AgentRegistry;
import com.simba.snip.npo.integration.security.ConnectorCapability;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EricssonEnmArchitectureIsolationTest {

    @Test
    void domainAgentMcpAndPhase4DoNotDependOnEnm() throws IOException {
        assertNoForbiddenImport("snip-npo-app/src/main/java/com/simba/snip/npo/domain");
        assertNoForbiddenImport("snip-npo-app/src/main/java/com/simba/snip/npo/agent");
        assertNoForbiddenImport("snip-npo-app/src/main/java/com/simba/snip/npo/mcp");
        assertNoForbiddenImport("snip-npo-app/src/main/java/com/simba/snip/npo/action");
    }

    @Test
    void agentsHaveNoEnmConnector() {
        AgentRegistry registry = new AgentRegistry();
        assertTrue(registry.list().stream().noneMatch(agent ->
                agent.agentId().toLowerCase(Locale.ROOT).contains("enm")
                        || agent.agentId().toLowerCase(Locale.ROOT).contains("vendor")));
    }

    @Test
    void mcpHasNoEnmCapability() {
        assertTrue(CapabilityRegistry.all().stream().noneMatch(capability ->
                capability.capabilityId().toLowerCase(Locale.ROOT).contains("enm")
                        || capability.capabilityId().toLowerCase(Locale.ROOT).contains("vendor")
                        || capability.capabilityId().toLowerCase(Locale.ROOT).contains("import")));
    }

    @Test
    void enmTransportHasNoNetworkMutationOperations() {
        for (Method method : EnmTransport.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            assertFalse(name.contains("write"));
            assertFalse(name.contains("delete"));
            assertFalse(name.contains("command"));
            assertFalse(name.contains("mutate"));
            assertFalse(name.contains("parameter"));
            assertFalse(name.contains("postarbitrary"));
            assertFalse(name.contains("exchange"));
        }
    }

    @Test
    void ericssonConnectorDoesNotUseAzureOrKeyVault() throws IOException {
        String source = Files.readString(Path.of(
                "snip-npo-app/src/main/java/com/simba/snip/npo/integration/enm/EricssonEnmConnector.java"));
        String lower = source.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("azure"));
        assertFalse(lower.contains("secretclient"));
        assertFalse(lower.contains("defaultazurecredential"));
        assertFalse(lower.contains("keyvault"));
        assertFalse(lower.contains("workloadidentity"));
    }

    @Test
    void noSchedulerOnEnmImportPath() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/integration/enm"))) {
            List<Path> scheduled = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("@Scheduled");
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .toList();
            assertTrue(scheduled.isEmpty(), "ENM package must not register @Scheduled methods: " + scheduled);
        }
        try (Stream<Path> files = Files.walk(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/integration/ericsson"))) {
            List<Path> scheduled = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("@Scheduled");
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .toList();
            assertTrue(scheduled.isEmpty(), scheduled.toString());
        }
    }

    @Test
    void writeCapabilitiesAreNotAdvertisedOnEnmConnector() {
        assertTrue(EricssonEnmConnector.READ_CAPABILITIES.stream().noneMatch(ConnectorCapability::mutatesNetwork));
    }

    private static void assertNoForbiddenImport(String directory) throws IOException {
        Path root = Path.of(directory);
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains("com.simba.snip.npo.integration.enm")
                                    || source.contains("com.simba.snip.npo.integration.ericsson.enm");
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .toList();
            assertTrue(offenders.isEmpty(), "forbidden ENM dependency: " + offenders);
        }
    }
}
