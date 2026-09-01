package com.simba.snip.npo.integration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendorBoundarySourceTest {

    @Test
    void higherLayersDoNotImportVendorAdapterTypes() throws Exception {
        assertNoVendorLeak(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/agent"));
        assertNoVendorLeak(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/twin"));
        assertNoVendorLeak(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/assurance"));
        assertNoVendorLeak(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/action"));
        assertNoVendorLeak(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/mcp"));
        assertNoVendorLeak(Path.of("snip-npo-app/src/main/java/com/simba/snip/npo/retrieve"));
    }

    private static void assertNoVendorLeak(Path directory) throws Exception {
        assertTrue(Files.isDirectory(directory), "missing " + directory);
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("com.simba.snip.npo.integration.ericsson"), path.toString());
                assertFalse(source.contains("com.simba.snip.npo.integration.nokia"), path.toString());
                assertFalse(source.contains("EricssonFixtureAdapter"), path.toString());
                assertFalse(source.contains("NokiaFixtureAdapter"), path.toString());
                assertFalse(source.contains("NetworkSourceAdapterRegistry"), path.toString());
            }
        }
    }
}
