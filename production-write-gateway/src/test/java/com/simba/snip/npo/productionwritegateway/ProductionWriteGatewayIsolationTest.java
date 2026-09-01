package com.simba.snip.npo.productionwritegateway;

import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionwritegateway.service.ProductionGrantConsumeService;
import com.simba.snip.npo.productionwritegateway.transport.UnconfiguredProductionEricssonWriteTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductionWriteGatewayIsolationTest {

    @Test
    void independentMainClass() {
        assertEquals(
                "com.simba.snip.npo.productionwritegateway.ProductionWriteGatewayApplication",
                ProductionWriteGatewayApplication.class.getName()
        );
    }

    @Test
    void productionTransportFailClosed() throws IOException {
        Path source = gatewayMain().resolve(
                "java/com/simba/snip/npo/productionwritegateway/transport/UnconfiguredProductionEricssonWriteTransport.java");
        String text = Files.readString(source);
        assertTrue(text.contains("PRODUCTION_WRITE_TRANSPORT_NOT_CONFIGURED"));
        assertFalse(text.toLowerCase(Locale.ROOT).contains("http://"));
        String yaml = Files.readString(gatewayMain().resolve("resources/application.yml"));
        assertTrue(yaml.contains("enabled: false"));
        assertTrue(yaml.contains("test-transport-enabled: false"));
        assertTrue(UnconfiguredProductionEricssonWriteTransport.class.getPackageName()
                .startsWith("com.simba.snip.npo.productionwritegateway"));
    }

    @Test
    void gatewayNotRegisteredInsideAppMainSources() throws IOException {
        Path appMain = repoRoot().resolve("snip-npo-app/src/main/java");
        if (!Files.exists(appMain)) {
            return;
        }
        try (Stream<Path> files = Files.walk(appMain)) {
            boolean offender = files.filter(p -> p.toString().endsWith(".java")).anyMatch(path -> {
                try {
                    String source = Files.readString(path);
                    return source.contains("productionwritegateway.service")
                            || source.contains("ProductionCredentialResolutionService")
                            || source.contains("EricssonEnmWriteAdapter")
                            || source.contains("EricssonWriteTransport");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
            assertFalse(offender);
        }
    }

    @Test
    void unconfiguredTransportHasNoGuessedVendorUrls() throws IOException {
        Path source = gatewayMain().resolve(
                "java/com/simba/snip/npo/productionwritegateway/transport/UnconfiguredProductionEricssonWriteTransport.java");
        String text = Files.readString(source);
        String lower = text.toLowerCase(Locale.ROOT);
        assertFalse(lower.contains("http://"));
        assertFalse(lower.contains("https://"));
        assertFalse(lower.contains("/bulkcm"));
        assertFalse(lower.contains("netconf"));
        assertTrue(text.contains("PRODUCTION_WRITE_TRANSPORT_NOT_CONFIGURED"));
        assertTrue(UnconfiguredProductionEricssonWriteTransport.class.getPackageName()
                .startsWith("com.simba.snip.npo.productionwritegateway"));
    }

    @Test
    void productionYamlFailClosed() throws IOException {
        String yaml = Files.readString(gatewayMain().resolve("resources/application.yml"));
        assertTrue(yaml.contains("enabled: false"));
        assertTrue(yaml.contains("global-execution-enabled: false"));
        assertTrue(yaml.contains("failure-injection:") && yaml.contains("enabled: false"));
        assertTrue(yaml.contains("test-transport-enabled: false"));
        assertTrue(yaml.contains("flyway:") && yaml.contains("enabled: false"));
        assertTrue(yaml.contains("trust-all: false"));
        assertTrue(yaml.contains("hostname-verification: true"));
        assertTrue(yaml.contains("port: 8081"));
    }

    @Test
    void consumeSqlIsExactBindingPredicate() throws IOException {
        String source = Files.readString(gatewayMain().resolve(
                "java/com/simba/snip/npo/productionwritegateway/service/ProductionGrantConsumeService.java"));
        String normalized = source.replaceAll("\\s+", " ");
        assertTrue(normalized.contains("SET status = 'CONSUMED', consumed_at = :now, version = version + 1"));
        assertTrue(normalized.contains("AND status = 'ISSUED'"));
        assertTrue(normalized.contains("AND expires_at > :now"));
        assertTrue(normalized.contains("AND production_change_id = :productionChangeId"));
        assertTrue(normalized.contains("AND phase15_execution_id = :phase15ExecutionId"));
        assertTrue(normalized.contains("AND target_id = :targetId"));
        assertTrue(normalized.contains("AND production_fingerprint = :productionFingerprint"));
        assertTrue(normalized.contains("AND authorization_generation = :authorizationGeneration"));
        assertTrue(normalized.contains("AND fencing_token = :fencingToken"));
        assertTrue(normalized.contains("AND operation_binding_hash = :operationBindingHash"));
        assertTrue(normalized.contains("AND grant_type = :grantType"));
        assertFalse(source.contains("synchronized"));
        assertEquals(ProductionGrantConsumeService.CONSUME_SQL.replaceAll("\\s+", " ").trim(),
                extractSql(source).replaceAll("\\s+", " ").trim());
    }

    @Test
    void attemptStatusesAreExact() {
        assertEquals(List.of(
                "PRE_SEND",
                "SEND_ELIGIBLE",
                "MAY_HAVE_SENT",
                "VENDOR_REJECTED",
                "VENDOR_ACCEPTED",
                "OUTCOME_UNKNOWN",
                "VERIFYING",
                "VERIFIED",
                "VERIFICATION_FAILED",
                "RECOVERY_REQUIRED",
                "MANUAL_INTERVENTION_REQUIRED"
        ), Stream.of(GatewayAttemptStatus.values()).map(Enum::name).toList());
    }

    @Test
    void restClientHasNoRetryInterceptor() throws IOException {
        Path config = gatewayMain().resolve(
                "java/com/simba/snip/npo/productionwritegateway/config/GatewayTransportConfig.java");
        String source = Files.readString(config);
        assertFalse(source.toLowerCase(Locale.ROOT).contains("retry"));
        assertFalse(source.contains("RestClient"), "unused mutation RestClient must not remain");
    }

    @Test
    void gatewayNetworkPolicyHasNoOpenEgress() throws IOException {
        Path np = repoRoot().resolve("deploy/k8s/production-write-gateway-networkpolicy.yaml");
        assertTrue(Files.exists(np));
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        boolean sawDeny = false;
        boolean sawVendorFqdn = false;
        for (Object raw : yaml.loadAll(Files.readString(np))) {
            if (!(raw instanceof java.util.Map<?, ?> doc) || doc.isEmpty()) {
                continue;
            }
            assertFalse(containsCidr(doc, "0.0.0.0/0"));
            assertFalse(containsCidr(doc, "::/0"));
            Object metadata = doc.get("metadata");
            String name = metadata instanceof java.util.Map<?, ?> meta ? String.valueOf(meta.get("name")) : "";
            if ("production-write-gateway-default-deny".equals(name)) {
                java.util.Map<?, ?> spec = (java.util.Map<?, ?>) doc.get("spec");
                assertTrue(spec.get("ingress") instanceof java.util.List<?> ingress && ingress.isEmpty());
                assertTrue(spec.get("egress") instanceof java.util.List<?> egress && egress.isEmpty());
                sawDeny = true;
            }
            if (containsFqdn(doc, "enm.example.invalid")) {
                sawVendorFqdn = true;
            }
        }
        assertTrue(sawDeny);
        assertTrue(sawVendorFqdn);
    }

    @Test
    void appGatewayEgressDoesNotOpenEnmWrite() throws IOException {
        Path egress = repoRoot().resolve("deploy/k8s/snip-npo-allow-gateway-egress.yaml");
        assertTrue(Files.exists(egress));
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        Object raw = yaml.load(Files.readString(egress));
        java.util.Map<?, ?> doc = (java.util.Map<?, ?>) raw;
        assertFalse(containsCidr(doc, "0.0.0.0/0"));
        assertFalse(containsCidr(doc, "::/0"));
        assertFalse(containsFqdn(doc, "enm.example.invalid"));
        assertTrue(containsLabel(doc, "production-write-gateway"));
        assertTrue(containsPort(doc, 8081) || containsPort(doc, "8081"));
    }

    private static boolean containsCidr(Object node, String cidr) {
        if (node instanceof java.util.Map<?, ?> map) {
            if (cidr.equals(String.valueOf(map.get("cidr")))) {
                return true;
            }
            Object toCidr = map.get("toCIDR");
            if (toCidr instanceof java.util.Collection<?> collection && collection.stream().map(String::valueOf).anyMatch(cidr::equals)) {
                return true;
            }
            return map.values().stream().anyMatch(v -> containsCidr(v, cidr));
        }
        if (node instanceof java.util.Collection<?> collection) {
            return collection.stream().anyMatch(v -> containsCidr(v, cidr));
        }
        return false;
    }

    private static boolean containsFqdn(Object node, String fqdn) {
        if (node instanceof java.util.Map<?, ?> map) {
            if (fqdn.equals(String.valueOf(map.get("matchName"))) || fqdn.equals(String.valueOf(map.get("matchPattern")))) {
                return true;
            }
            return map.values().stream().anyMatch(v -> containsFqdn(v, fqdn));
        }
        if (node instanceof java.util.Collection<?> collection) {
            return collection.stream().anyMatch(v -> containsFqdn(v, fqdn));
        }
        return false;
    }

    private static boolean containsLabel(Object node, String app) {
        if (node instanceof java.util.Map<?, ?> map) {
            Object labels = map.get("matchLabels");
            if (labels instanceof java.util.Map<?, ?> lbl && app.equals(String.valueOf(lbl.get("app")))) {
                return true;
            }
            return map.values().stream().anyMatch(v -> containsLabel(v, app));
        }
        if (node instanceof java.util.Collection<?> collection) {
            return collection.stream().anyMatch(v -> containsLabel(v, app));
        }
        return false;
    }

    private static boolean containsPort(Object node, Object port) {
        if (node instanceof java.util.Map<?, ?> map) {
            if (String.valueOf(port).equals(String.valueOf(map.get("port")))) {
                return true;
            }
            return map.values().stream().anyMatch(v -> containsPort(v, port));
        }
        if (node instanceof java.util.Collection<?> collection) {
            return collection.stream().anyMatch(v -> containsPort(v, port));
        }
        return false;
    }

    @Test
    void distinctServiceAccountAndUamiPlaceholder() throws IOException {
        Path sa = repoRoot().resolve("deploy/k8s/production-write-gateway-serviceaccount.yaml");
        String text = Files.readString(sa);
        assertTrue(text.contains("name: production-write-gateway"));
        assertTrue(text.contains("${SNIP_PRODUCTION_WRITE_UAMI_CLIENT_ID}"));
        assertFalse(text.contains("snip-connector-runtime"));
    }

    @Test
    void credentialServiceLivesInGatewayOnly() throws IOException {
        Path gatewayService = gatewayMain().resolve(
                "java/com/simba/snip/npo/productionwritegateway/service/ProductionCredentialResolutionService.java");
        assertTrue(Files.exists(gatewayService));
        Path appMain = repoRoot().resolve("snip-npo-app/src/main/java");
        if (Files.exists(appMain)) {
            try (Stream<Path> files = Files.walk(appMain)) {
                assertFalse(files.anyMatch(p -> p.getFileName().toString()
                        .equals("ProductionCredentialResolutionService.java")));
            }
        }
    }

    private static String extractSql(String source) {
        int start = source.indexOf("UPDATE production_execution_grant");
        int end = source.indexOf("AND grant_type = :grantType");
        return source.substring(start, end) + "AND grant_type = :grantType";
    }

    private static Path gatewayMain() {
        return repoRoot().resolve("production-write-gateway/src/main");
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("production-write-gateway"))) {
            return cwd;
        }
        if (cwd.getFileName() != null && "production-write-gateway".equals(cwd.getFileName().toString())) {
            return cwd.getParent();
        }
        return cwd;
    }
}
