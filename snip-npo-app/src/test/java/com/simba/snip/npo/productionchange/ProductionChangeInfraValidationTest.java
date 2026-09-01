package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeInfraValidationTest {

    @Test
    void gatewayDeploymentManifestSeparate() throws IOException {
        Path gateway = ProductionChangeSourcePaths.repoRoot()
                .resolve("deploy/k8s/production-write-gateway-deployment.yaml");
        Path app = ProductionChangeSourcePaths.repoRoot().resolve("deploy/k8s/deployment.yaml");
        assertTrue(Files.exists(gateway));
        assertTrue(Files.exists(app));
        String gw = Files.readString(gateway);
        String appText = Files.readString(app);
        assertTrue(gw.contains("name: production-write-gateway"));
        assertFalse(appText.contains("name: production-write-gateway"));
        assertTrue(gw.contains("kind: Deployment"));
    }

    @Test
    void gatewayServiceAccountSeparate() throws IOException {
        Path sa = ProductionChangeSourcePaths.repoRoot()
                .resolve("deploy/k8s/production-write-gateway-serviceaccount.yaml");
        Path appSa = ProductionChangeSourcePaths.repoRoot().resolve("deploy/k8s/serviceaccount.yaml");
        assertTrue(Files.exists(sa));
        String text = Files.readString(sa);
        assertTrue(text.contains("name: production-write-gateway"));
        assertFalse(text.contains("snip-connector-runtime"));
        if (Files.exists(appSa)) {
            assertFalse(Files.readString(appSa).contains("name: production-write-gateway"));
        }
    }

    @Test
    void appNetworkPolicyNoVendorEgress() throws IOException {
        Path appNp = ProductionChangeSourcePaths.repoRoot().resolve("deploy/k8s/networkpolicy.yaml");
        Path gatewayEgress = ProductionChangeSourcePaths.repoRoot()
                .resolve("deploy/k8s/snip-npo-allow-gateway-egress.yaml");
        var appDocs = ProductionChangeNetworkPolicySemantics.loadDocuments(appNp);
        for (var doc : appDocs) {
            ProductionChangeNetworkPolicySemantics.assertNoWildcardInternetCidrs(doc);
            ProductionChangeNetworkPolicySemantics.assertAppHasNoVendorWriteFqdn(doc);
        }
        ProductionChangeNetworkPolicySemantics.assertDefaultDeny(
                ProductionChangeNetworkPolicySemantics.requireNamed(appDocs, "snip-npo-default-deny-egress"));
        var extraDocs = ProductionChangeNetworkPolicySemantics.loadDocuments(gatewayEgress);
        ProductionChangeNetworkPolicySemantics.assertAppGatewayOnlyEgress(
                ProductionChangeNetworkPolicySemantics.requireNamed(extraDocs, "snip-npo-allow-gateway-egress"));
    }

    @Test
    void gatewayNetworkPolicyRestrictedEgress() throws IOException {
        Path path = ProductionChangeSourcePaths.repoRoot()
                .resolve("deploy/k8s/production-write-gateway-networkpolicy.yaml");
        String raw = Files.readString(path);
        assertTrue(raw.contains("0.0.0.0/0"), "comment fixture must mention 0.0.0.0/0 so string scans are not the evidence");
        var docs = ProductionChangeNetworkPolicySemantics.loadDocuments(path);
        var deny = ProductionChangeNetworkPolicySemantics.requireNamed(docs, "production-write-gateway-default-deny");
        ProductionChangeNetworkPolicySemantics.assertKind(deny, "NetworkPolicy");
        ProductionChangeNetworkPolicySemantics.assertDefaultDeny(deny);
        var allow = ProductionChangeNetworkPolicySemantics.requireNamed(docs, "production-write-gateway-allow-required");
        ProductionChangeNetworkPolicySemantics.assertGatewayAllowRequiredEgress(allow);
        var cilium = ProductionChangeNetworkPolicySemantics.requireNamed(
                docs, "production-write-gateway-enm-placeholder-egress");
        ProductionChangeNetworkPolicySemantics.assertKind(cilium, "CiliumNetworkPolicy");
        ProductionChangeNetworkPolicySemantics.assertGatewayCiliumVendorEgress(cilium);
        for (var doc : docs) {
            ProductionChangeNetworkPolicySemantics.assertNoWildcardInternetCidrs(doc);
        }
    }

    @Test
    void tlsVerificationEnabled() throws IOException {
        String yaml = Files.readString(ProductionChangeSourcePaths.repoRoot()
                .resolve("production-write-gateway/src/main/resources/application.yml"));
        assertTrue(yaml.contains("hostname-verification: true"));
        assertTrue(yaml.contains("trust-all: false"));
    }

    @Test
    void noTrustAllSsl() throws IOException {
        String yaml = Files.readString(ProductionChangeSourcePaths.repoRoot()
                .resolve("production-write-gateway/src/main/resources/application.yml"));
        assertTrue(yaml.contains("trust-all: false"));
        assertFalse(yaml.contains("trust-all: true"));
    }

    @Test
    void defaultEnabledFalseInYaml() throws IOException {
        String app = Files.readString(ProductionChangeSourcePaths.appMainResources().resolve("application.yml"));
        String gateway = Files.readString(ProductionChangeSourcePaths.repoRoot()
                .resolve("production-write-gateway/src/main/resources/application.yml"));
        assertTrue(app.contains("production-change:"));
        assertTrue(app.contains("enabled: false"));
        assertTrue(gateway.contains("enabled: false"));
        assertTrue(gateway.contains("global-execution-enabled: false"));
    }

    @Test
    void ciAzureIndependent() throws IOException {
        Path workflows = ProductionChangeSourcePaths.repoRoot().resolve(".github/workflows");
        assertTrue(Files.isDirectory(workflows));
        StringBuilder all = new StringBuilder();
        try (var stream = Files.list(workflows)) {
            stream.filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                    .forEach(p -> {
                        try {
                            all.append(Files.readString(p));
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    });
        }
        String text = all.toString();
        assertTrue(text.contains("mvn") || text.contains("maven"));
        assertFalse(text.toLowerCase(Locale.ROOT).contains("azure/login@")
                && text.toLowerCase(Locale.ROOT).contains("required: true"));
    }
}
