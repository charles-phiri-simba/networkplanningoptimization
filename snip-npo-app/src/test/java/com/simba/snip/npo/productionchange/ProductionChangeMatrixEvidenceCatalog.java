package com.simba.snip.npo.productionchange;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionChangeMatrixEvidenceCatalog {

    private static final Pattern ROW = Pattern.compile(
            "^\\| (P16-E\\d{3}) \\| ([^|]+) \\| ([^|]+) \\| ([^|]+) \\| ([^|]+) \\| ([^|]+) \\| ([^|]+) \\| ([^|]+) \\| ([^|]+) \\|",
            Pattern.MULTILINE);

    @Test
    void allItemsHaveRequirements() throws IOException {
        String spec = Files.readString(ProductionChangeSourcePaths.repoRoot().resolve(
                "docs/implementation/SNIP-PHASE-16-VENDOR-WRITE-INTEGRATION-SECURITY-PRODUCTION-CHANGE-CONTROL-CONTROLLED-REAL-NETWORK-EXECUTION-SPECIFICATION.md"));
        int start = spec.indexOf("## 55. Implementation evidence catalog");
        int end = spec.indexOf("### 55.1 Evidence totals");
        Matcher matcher = ROW.matcher(spec.substring(start, end));
        Map<String, String> requirements = new LinkedHashMap<>();
        while (matcher.find()) {
            String id = matcher.group(1);
            String type = matcher.group(3).trim();
            String requirement = matcher.group(6).trim();
            assertFalse(requirement.isBlank(), id + " missing requirement");
            assertFalse(requirement.equalsIgnoreCase("TBD"));
            assertFalse(requirement.equalsIgnoreCase("placeholder"));
            assertFalse(type.equalsIgnoreCase("GENERIC"));
            requirements.put(id, requirement);
        }
        assertEquals(173, requirements.size());

        int threats = spec.indexOf("## 53. Threat model — T01–T48 traceability");
        int threatsEnd = spec.indexOf("## 54. Architecture gate traceability matrix");
        String threatTable = spec.substring(threats, threatsEnd);
        for (int i = 1; i <= 48; i++) {
            String threatId = "T" + String.format("%02d", i);
            assertTrue(threatTable.contains("| " + threatId + " |"), "missing threat " + threatId);
            assertTrue(threatTable.contains("P16-E"), threatId + " must reference evidence");
        }
    }
}
