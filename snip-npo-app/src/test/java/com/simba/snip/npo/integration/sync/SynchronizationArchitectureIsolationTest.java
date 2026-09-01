package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.enm.EnmTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynchronizationArchitectureIsolationTest {

    @Test
    void schedulerDoesNotReferenceTransportOrKeyVault() throws IOException {
        String scheduler = Files.readString(Path.of(
                "snip-npo-app/src/main/java/com/simba/snip/npo/integration/sync/SynchronizationScheduler.java"));
        String lower = scheduler.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("enmtransport"));
        assertFalse(lower.contains("ericssonenmconnector"));
        assertFalse(lower.contains("azure"));
        assertFalse(lower.contains("keyvault"));
    }

    @Test
    void schedulerAndControlPlaneDoNotInjectTransportDirectly() throws IOException {
        for (String file : List.of(
                "SynchronizationScheduler.java",
                "SynchronizationControlPlane.java"
        )) {
            String source = Files.readString(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/integration/sync/" + file));
            assertFalse(source.contains("EnmTransport"));
            assertFalse(source.contains("EricssonEnmConnector"));
        }
    }

    @Test
    void enmTransportHasNoNetworkMutationOperations() {
        for (java.lang.reflect.Method method : EnmTransport.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            assertFalse(name.contains("write"));
            assertFalse(name.contains("delete"));
            assertFalse(name.contains("command"));
            assertFalse(name.contains("mutate"));
        }
    }

    @Test
    void agentMcpAndPhase4PackagesDoNotImportSyncTransport() throws IOException {
        assertNoForbiddenImport("snip-npo-app/src/main/java/com/simba/snip/npo/agent");
        assertNoForbiddenImport("snip-npo-app/src/main/java/com/simba/snip/npo/mcp");
        assertNoForbiddenImport("snip-npo-app/src/main/java/com/simba/snip/npo/action");
    }

    private static void assertNoForbiddenImport(String root) throws IOException {
        try (Stream<Path> files = Files.walk(Path.of(root))) {
            boolean offender = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains("integration.enm.EnmTransport")
                                    || source.contains("integration.enm.EricssonEnmConnector");
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
            assertFalse(offender, root + " must not depend on ENM transport/connector");
        }
    }
}
