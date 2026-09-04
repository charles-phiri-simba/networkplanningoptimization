package com.simba.snip.npo.vendorcertification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase17GateTraceabilityValidationTest {

    @Test
    void all158GatesMappedWithZeroBrokenReferences() throws Exception {
        Path root = repoRoot();
        JsonNode rootNode = new ObjectMapper().readTree(Files.readString(
                root.resolve("docs/implementation/phase17-gate-evidence-map.json")));
        assertTrue(rootNode.isArray());
        assertEquals(158, rootNode.size());
        Set<String> gates = new TreeSet<>();
        int broken = 0;
        for (JsonNode node : rootNode) {
            String gate = node.get("gate").asText();
            assertTrue(gate.matches("G17-\\d{3}"), gate);
            assertTrue(gates.add(gate), "duplicate " + gate);
            JsonNode ids = node.get("evidenceIds");
            assertTrue(ids.isArray() && ids.size() >= 1, gate);
            for (JsonNode id : ids) {
                if (id.asText().isBlank() || id.asText().contains("PLACEHOLDER")) {
                    broken++;
                }
            }
            assertFalse(node.get("evidenceTypes").isEmpty(), gate);
        }
        assertEquals(158, gates.size());
        for (int i = 1; i <= 158; i++) {
            assertTrue(gates.contains("G17-" + String.format("%03d", i)));
        }
        assertEquals(0, broken);

        String spec = Files.readString(root.resolve(
                "docs/implementation/SNIP-PHASE-17-CERTIFIED-VENDOR-WRITE-TRANSPORT-INTEGRATION-TARGET-ONBOARDING-PRODUCTION-OPERATIONAL-READINESS-SPECIFICATION.md"));
        int section34 = spec.indexOf("## 34. Architecture gate → evidence map");
        assertTrue(section34 > 0);
        Map<String, List<String>> markdown = new LinkedHashMap<>();
        for (String line : spec.substring(section34).split("\n")) {
            if (!line.startsWith("| G17-")) {
                continue;
            }
            String[] cols = line.split("\\|", -1);
            if (cols.length < 7) {
                continue;
            }
            markdown.put(cols[1].trim(), expand(cols[5].trim()));
        }
        assertEquals(158, markdown.size());
        for (JsonNode node : rootNode) {
            String gate = node.get("gate").asText();
            List<String> jsonIds = new ArrayList<>();
            node.get("evidenceIds").forEach(id -> jsonIds.add(id.asText()));
            assertEquals(markdown.get(gate), jsonIds, gate);
        }
    }

    private static List<String> expand(String cell) {
        List<String> out = new ArrayList<>();
        for (String raw : cell.split(",")) {
            String t = raw.trim();
            if (t.matches("CS17-[A-Z]/[A-Z]")) {
                out.add("CS17-" + t.charAt(5));
                out.add("CS17-" + t.charAt(7));
            } else if (t.matches("CS17-[A-Z]–[A-Z]")) {
                for (char c = t.charAt(5); c <= t.charAt(7); c++) {
                    out.add("CS17-" + c);
                }
            } else if (t.contains("–") && t.matches(".*\\d+–\\d+$")) {
                int dash = t.lastIndexOf('–');
                String left = t.substring(0, dash);
                String right = t.substring(dash + 1);
                String prefix = left.replaceFirst("\\d+$", "");
                int start = Integer.parseInt(left.substring(prefix.length()));
                int end = Integer.parseInt(right);
                int width = left.length() - prefix.length();
                for (int i = start; i <= end; i++) {
                    out.add(prefix + String.format("%0" + width + "d", i));
                }
            } else if (t.contains("/") && t.matches(".*\\d+(/\\d+)+$")) {
                String prefix = t.replaceFirst("\\d+(/\\d+)+$", "");
                String rest = t.substring(prefix.length());
                int width = rest.split("/")[0].length();
                for (String n : rest.split("/")) {
                    out.add(prefix + String.format("%0" + width + "d", Integer.parseInt(n)));
                }
            } else {
                out.add(t);
            }
        }
        return out;
    }

    private static Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.exists(cwd.resolve("docs/implementation/phase17-gate-evidence-map.json"))) {
            return cwd;
        }
        return cwd.getParent();
    }
}
