package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionWorkloadIdentityBindingTest {

    @Test
    void gatewayServiceAccountSeparate() throws IOException {
        new ProductionChangeInfraValidationTest().gatewayServiceAccountSeparate();
        String sa = Files.readString(ProductionChangeSourcePaths.repoRoot()
                .resolve("deploy/k8s/production-write-gateway-serviceaccount.yaml"));
        assertTrue(sa.contains("azure.workload.identity/client-id")
                || sa.contains("SNIP_PRODUCTION_WRITE_UAMI_CLIENT_ID"));
    }
}
