package com.simba.snip.npo.productionchange;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Structural NetworkPolicy evidence. YAML comments must not affect PASS/FAIL.
 */
final class ProductionChangeNetworkPolicySemantics {

    private ProductionChangeNetworkPolicySemantics() {
    }

    static List<Map<String, Object>> loadDocuments(Path path) throws IOException {
        Yaml yaml = new Yaml();
        List<Map<String, Object>> docs = new ArrayList<>();
        for (Object raw : yaml.loadAll(Files.readString(path))) {
            if (raw instanceof Map<?, ?> map && !map.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                docs.add(typed);
            }
        }
        assertFalse(docs.isEmpty(), "no YAML documents in " + path);
        return docs;
    }

    static Map<String, Object> requireNamed(List<Map<String, Object>> docs, String name) {
        return docs.stream()
                .filter(doc -> name.equals(metadataName(doc)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing document " + name));
    }

    static String metadataName(Map<String, Object> doc) {
        Object metadata = doc.get("metadata");
        if (metadata instanceof Map<?, ?> map) {
            Object name = map.get("name");
            return name == null ? null : String.valueOf(name);
        }
        return null;
    }

    static void assertNoWildcardInternetCidrs(Map<String, Object> doc) {
        collectCidrs(doc).forEach(cidr -> {
            assertFalse("0.0.0.0/0".equals(cidr), () -> "wildcard IPv4 in " + metadataName(doc));
            assertFalse("::/0".equals(cidr), () -> "wildcard IPv6 in " + metadataName(doc));
        });
    }

    static void assertDefaultDeny(Map<String, Object> doc) {
        Map<String, Object> spec = spec(doc);
        assertEquals(List.of(), asList(spec.get("ingress")), metadataName(doc) + " ingress must be empty");
        assertEquals(List.of(), asList(spec.get("egress")), metadataName(doc) + " egress must be empty");
    }

    static void assertGatewayAllowRequiredEgress(Map<String, Object> doc) {
        assertNoWildcardInternetCidrs(doc);
        assertFalse(hasIpBlock(doc), "gateway allow-required must not use ipBlock");
        List<Map<String, Object>> egress = egressRules(spec(doc));
        assertFalse(egress.isEmpty(), "gateway allow-required must declare egress");
        boolean dnsUdp = false;
        boolean dnsTcp = false;
        boolean postgres = false;
        for (Map<String, Object> rule : egress) {
            assertFalse(isUnrestrictedEgress(rule), "unrestricted gateway egress rule");
            List<Map<String, Object>> ports = portMaps(rule.get("ports"));
            for (Map<String, Object> port : ports) {
                String proto = String.valueOf(port.getOrDefault("protocol", "")).toUpperCase();
                String number = String.valueOf(port.get("port"));
                if ("53".equals(number) && "UDP".equals(proto)) {
                    dnsUdp = true;
                }
                if ("53".equals(number) && "TCP".equals(proto)) {
                    dnsTcp = true;
                }
                if ("5432".equals(number) && "TCP".equals(proto)) {
                    postgres = true;
                }
            }
        }
        assertTrue(dnsUdp && dnsTcp, "gateway DNS 53 UDP+TCP required");
        assertTrue(postgres, "gateway PostgreSQL 5432 required");
    }

    static void assertGatewayCiliumVendorEgress(Map<String, Object> doc) {
        assertNoWildcardInternetCidrs(doc);
        Map<String, Object> spec = spec(doc);
        List<Map<String, Object>> egress = egressRules(spec);
        assertFalse(egress.isEmpty());
        boolean dns = false;
        boolean vendorFqdn = false;
        for (Map<String, Object> rule : egress) {
            assertFalse(isUnrestrictedCiliumEgress(rule), "unrestricted Cilium egress");
            if (rule.get("toEndpoints") != null && hasPort(rule, "53")) {
                dns = true;
            }
            Object fqdns = rule.get("toFQDNs");
            if (fqdns instanceof Collection<?> names) {
                for (Object name : names) {
                    String match = fqdnMatch(name);
                    assertEquals("enm.example.invalid", match, "vendor FQDN must be the accepted placeholder");
                    assertTrue(hasPort(rule, "443"), "vendor FQDN must be TCP 443");
                    vendorFqdn = true;
                }
            }
        }
        assertTrue(dns, "Cilium DNS to kube-dns required");
        assertTrue(vendorFqdn, "Cilium toFQDNs enm.example.invalid:443 required");
    }

    static void assertAppGatewayOnlyEgress(Map<String, Object> doc) {
        assertNoWildcardInternetCidrs(doc);
        assertFalse(hasIpBlock(doc), "app→gateway egress must not use ipBlock");
        assertFalse(collectFqdns(doc).stream().anyMatch(f -> f.contains("enm")), "app must not receive ENM FQDN");
        List<Map<String, Object>> egress = egressRules(spec(doc));
        assertEquals(1, egress.size(), "app gateway egress must be a single rule");
        Map<String, Object> rule = egress.get(0);
        assertFalse(isUnrestrictedEgress(rule), "unrestricted app→gateway egress");
        boolean gatewayPod = false;
        for (Object peer : asList(rule.get("to"))) {
            if (peer instanceof Map<?, ?> map) {
                Object selector = map.get("podSelector");
                if (selector instanceof Map<?, ?> sel) {
                    Object labels = sel.get("matchLabels");
                    if (labels instanceof Map<?, ?> lbl && "production-write-gateway".equals(String.valueOf(lbl.get("app")))) {
                        gatewayPod = true;
                    }
                }
            }
        }
        assertTrue(gatewayPod, "app egress must target production-write-gateway");
        List<Map<String, Object>> ports = portMaps(rule.get("ports"));
        assertEquals(1, ports.size());
        assertEquals("TCP", String.valueOf(ports.get(0).get("protocol")).toUpperCase());
        assertEquals("8081", String.valueOf(ports.get(0).get("port")));
    }

    static void assertAppHasNoVendorWriteFqdn(Map<String, Object> doc) {
        assertNoWildcardInternetCidrs(doc);
        for (String fqdn : collectFqdns(doc)) {
            assertFalse(fqdn.contains("enm.example"), "app policy must not grant ENM write FQDN: " + fqdn);
        }
    }

    static boolean hasIpBlock(Object node) {
        if (node instanceof Map<?, ?> map) {
            if (map.containsKey("ipBlock")) {
                return true;
            }
            for (Object value : map.values()) {
                if (hasIpBlock(value)) {
                    return true;
                }
            }
        } else if (node instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (hasIpBlock(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, Object> spec(Map<String, Object> doc) {
        Object spec = doc.get("spec");
        assertNotNull(spec, metadataName(doc) + " missing spec");
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) spec;
        return typed;
    }

    private static List<Map<String, Object>> egressRules(Map<String, Object> spec) {
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Object rule : asList(spec.get("egress"))) {
            if (rule instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                rules.add(typed);
            }
        }
        return rules;
    }

    private static boolean isUnrestrictedEgress(Map<String, Object> rule) {
        List<?> to = asList(rule.get("to"));
        List<?> ports = asList(rule.get("ports"));
        return to.isEmpty() || ports.isEmpty();
    }

    private static boolean isUnrestrictedCiliumEgress(Map<String, Object> rule) {
        boolean destination = rule.get("toFQDNs") != null
                || rule.get("toEndpoints") != null
                || rule.get("toCIDR") != null
                || rule.get("toCIDRSet") != null;
        return !destination || !hasAnyPort(rule);
    }

    private static boolean hasAnyPort(Map<String, Object> rule) {
        Object toPorts = rule.get("toPorts");
        if (toPorts instanceof Collection<?> collection && !collection.isEmpty()) {
            return true;
        }
        return !asList(rule.get("ports")).isEmpty();
    }

    private static boolean hasPort(Map<String, Object> rule, String port) {
        Object toPorts = rule.get("toPorts");
        if (toPorts instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry instanceof Map<?, ?> map) {
                    Object ports = map.get("ports");
                    if (ports instanceof Collection<?> portList) {
                        for (Object p : portList) {
                            if (p instanceof Map<?, ?> portMap && port.equals(String.valueOf(portMap.get("port")))) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        for (Map<String, Object> p : portMaps(rule.get("ports"))) {
            if (port.equals(String.valueOf(p.get("port")))) {
                return true;
            }
        }
        return false;
    }

    private static String fqdnMatch(Object name) {
        if (name instanceof Map<?, ?> map) {
            Object matchName = map.get("matchName");
            if (matchName != null) {
                return String.valueOf(matchName);
            }
            Object pattern = map.get("matchPattern");
            if (pattern != null) {
                return String.valueOf(pattern);
            }
        }
        return String.valueOf(name);
    }

    private static List<String> collectFqdns(Object node) {
        List<String> names = new ArrayList<>();
        walk(node, names);
        return names;
    }

    private static void walk(Object node, List<String> names) {
        if (node instanceof Map<?, ?> map) {
            if (map.containsKey("matchName")) {
                names.add(String.valueOf(map.get("matchName")));
            }
            if (map.containsKey("matchPattern")) {
                names.add(String.valueOf(map.get("matchPattern")));
            }
            for (Object value : map.values()) {
                walk(value, names);
            }
        } else if (node instanceof Collection<?> collection) {
            for (Object value : collection) {
                walk(value, names);
            }
        }
    }

    private static List<String> collectCidrs(Object node) {
        List<String> cidrs = new ArrayList<>();
        collectCidrs(node, cidrs);
        return cidrs;
    }

    private static void collectCidrs(Object node, List<String> cidrs) {
        if (node instanceof Map<?, ?> map) {
            Object cidr = map.get("cidr");
            if (cidr != null) {
                cidrs.add(String.valueOf(cidr));
            }
            Object except = map.get("except");
            if (except instanceof Collection<?> collection) {
                for (Object item : collection) {
                    cidrs.add(String.valueOf(item));
                }
            }
            Object toCidr = map.get("toCIDR");
            if (toCidr instanceof Collection<?> collection) {
                for (Object item : collection) {
                    cidrs.add(String.valueOf(item));
                }
            }
            for (Object value : map.values()) {
                collectCidrs(value, cidrs);
            }
        } else if (node instanceof Collection<?> collection) {
            for (Object value : collection) {
                collectCidrs(value, cidrs);
            }
        }
    }

    private static List<Map<String, Object>> portMaps(Object ports) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object port : asList(ports)) {
            if (port instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                result.add(typed);
            }
        }
        return result;
    }

    private static List<?> asList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }
        fail("expected list, got " + value.getClass());
        return List.of();
    }

    static void assertKind(Map<String, Object> doc, String kind) {
        assertEquals(kind, doc.get("kind"), () -> "kind mismatch for " + metadataName(doc));
    }
}
