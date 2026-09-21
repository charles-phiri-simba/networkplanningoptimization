package com.simba.snip.npo.productioncampaign.evidence;

import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses frozen Phase 18 architecture catalogues and threat/invariant mappings.
 */
public final class Phase18ArchitectureCatalog {

    public static final int GATE_COUNT = 254;
    public static final int THREAT_COUNT = 64;
    public static final int INVARIANT_COUNT = 32;

    private static final Pattern GATE_DEF = Pattern.compile("\\*\\*G18-(\\d{3})");
    private static final Pattern THREAT_DEF = Pattern.compile("\\*\\*T18-(\\d{2})");
    private static final Pattern INVARIANT_DEF = Pattern.compile("\\*\\*I18-(\\d{2})");
    private static final Pattern TABLE_ROW = Pattern.compile("^\\|\\s*(T18-\\d{2}|I18-\\d{2})\\s*\\|\\s*([^|]+)\\|");

    private Phase18ArchitectureCatalog() {
    }

    public static Catalog parse(Path architecturePath) {
        try {
            String text = Files.readString(architecturePath);
            Set<String> gates = new TreeSet<>();
            Matcher gm = GATE_DEF.matcher(text);
            while (gm.find()) {
                gates.add("G18-" + gm.group(1));
            }
            Set<String> threats = new TreeSet<>();
            Matcher tm = THREAT_DEF.matcher(text);
            while (tm.find()) {
                threats.add("T18-" + tm.group(1));
            }
            Set<String> invariants = new TreeSet<>();
            Matcher im = INVARIANT_DEF.matcher(text);
            while (im.find()) {
                invariants.add("I18-" + im.group(1));
            }
            Map<String, List<String>> threatGates = parseMappingTable(text, "## 56.2 Threat");
            Map<String, List<String>> invariantGates = parseMappingTable(text, "## 56.3 Invariant");
            return new Catalog(gates, threats, invariants, threatGates, invariantGates);
        } catch (CampaignException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CampaignException(
                    ProductionReasonCode.EVIDENCE_MAP_INVALID,
                    "unable to parse frozen Phase 18 architecture",
                    ex
            );
        }
    }

    private static Map<String, List<String>> parseMappingTable(String text, String heading) {
        int idx = text.indexOf(heading);
        if (idx < 0) {
            throw new CampaignException(ProductionReasonCode.EVIDENCE_MAP_INVALID, "architecture mapping heading missing: " + heading);
        }
        String section = text.substring(idx);
        int next = section.indexOf("\n## ", 10);
        if (next > 0) {
            section = section.substring(0, next);
        }
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (String line : section.split("\n")) {
            Matcher m = TABLE_ROW.matcher(line.trim());
            if (!m.find()) {
                continue;
            }
            map.put(m.group(1), expandGates(m.group(2).trim()));
        }
        return map;
    }

    static List<String> expandGates(String cell) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String raw : cell.split(",")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            token = token.replace('–', '-').replace('—', '-');
            if (token.matches("G18-\\d{3}-G18-\\d{3}")) {
                int start = Integer.parseInt(token.substring(4, 7));
                int end = Integer.parseInt(token.substring(12, 15));
                for (int i = start; i <= end; i++) {
                    out.add(String.format("G18-%03d", i));
                }
            } else if (token.matches("G18-\\d{3}")) {
                out.add(token);
            } else {
                throw new CampaignException(
                        ProductionReasonCode.EVIDENCE_MAP_INVALID,
                        "unparseable gate token in architecture mapping: " + token
                );
            }
        }
        return new ArrayList<>(out);
    }

    public record Catalog(
            Set<String> gates,
            Set<String> threats,
            Set<String> invariants,
            Map<String, List<String>> threatToGates,
            Map<String, List<String>> invariantToGates
    ) {
    }
}
