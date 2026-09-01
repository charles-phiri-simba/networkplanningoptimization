package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeBaselineTest {

    @Test
    void failedPhase15CandidateDocumented() throws IOException {
        Path docs = ProductionChangeSourcePaths.repoRoot().resolve("docs");
        String architecture = Files.readString(docs.resolve(
                "architecture/SNIP-PHASE-16-VENDOR-WRITE-INTEGRATION-SECURITY-PRODUCTION-CHANGE-CONTROL-CONTROLLED-REAL-NETWORK-EXECUTION-ARCHITECTURE.md"));
        String spec = Files.readString(docs.resolve(
                "implementation/SNIP-PHASE-16-VENDOR-WRITE-INTEGRATION-SECURITY-PRODUCTION-CHANGE-CONTROL-CONTROLLED-REAL-NETWORK-EXECUTION-SPECIFICATION.md"));
        assertTrue(architecture.contains("0cb1223e41ced5462ad552f993e6001a028ddb96")
                || architecture.contains("0cb1223"));
        assertTrue(spec.contains("0cb1223e41ced5462ad552f993e6001a028ddb96")
                || spec.contains("0cb1223"));
        assertTrue(architecture.contains("ae9c13d55b444fa50090813495b32b82f97c2ec3")
                || architecture.contains("ae9c13d"));
    }

    @Test
    void phase16ImplementationAbsent() {
        // Specification-time evidence required absence. Implementation is now authorized:
        // packages must exist. Baseline 8c0791b lacked them.
        Path appPkg = ProductionChangeSourcePaths.appMainJava()
                .resolve("com/simba/snip/npo/productionchange");
        Path gatewayPkg = ProductionChangeSourcePaths.gatewayMainJava()
                .resolve("com/simba/snip/npo/productionwritegateway");
        Path v17 = ProductionChangeSourcePaths.appMainResources()
                .resolve("db/migration/V17__phase16_production_change_execution.sql");
        assertTrue(Files.isDirectory(appPkg), "productionchange package must exist after authorization");
        assertTrue(Files.isDirectory(gatewayPkg), "productionwritegateway package must exist after authorization");
        assertTrue(Files.exists(v17), "V17 must exist after authorization");
    }
}
