package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionGatewayKubernetesManifestTest {

    @Test
    void gatewayDeploymentManifestSeparate() throws IOException {
        new ProductionChangeInfraValidationTest().gatewayDeploymentManifestSeparate();
        String deployment = Files.readString(ProductionChangeSourcePaths.repoRoot()
                .resolve("deploy/k8s/production-write-gateway-deployment.yaml"));
        assertTrue(deployment.contains("serviceAccountName: production-write-gateway"));
        assertFalse(deployment.contains("snip-npo"));
    }
}
