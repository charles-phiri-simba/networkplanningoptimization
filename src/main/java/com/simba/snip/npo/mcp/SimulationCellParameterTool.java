package com.simba.snip.npo.mcp;

import com.simba.snip.npo.action.ActionRules;
import com.simba.snip.npo.domain.DomainValidationException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimulationCellParameterTool implements McpTool {

    @Override
    public String name() {
        return ActionRules.CAPABILITY_SIMULATION;
    }

    @Override
    public String description() {
        return "Synthetic dry-run simulation of a cell parameter change. Not a production RF digital twin.";
    }

    @Override
    public Map<String, Object> call(Map<String, Object> arguments) {
        if (!truthy(arguments.get("dryRun"))) {
            throw new DomainValidationException("simulation requires dryRun=true");
        }
        String cellId = string(arguments.get("cellId"));
        String parameter = arguments.get("parameter") == null ? "pci" : String.valueOf(arguments.get("parameter"));
        Object currentValue = arguments.getOrDefault("currentValue", "unknown");
        Object proposedValue = arguments.getOrDefault("proposedValue", "unknown");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionId", String.valueOf(arguments.get("actionId")));
        result.put("cellId", cellId);
        result.put("parameter", parameter);
        result.put("currentValue", currentValue);
        result.put("proposedValue", proposedValue);
        result.put("predictedImpact",
                "SYNTHETIC: changing " + parameter + " from " + currentValue + " to " + proposedValue
                        + " on " + cellId + " is modelled as a dry-run only. This is not a vendor-certified RF prediction.");
        result.put("warnings", List.of(
                "synthetic=true",
                "dryRun=true is mandatory",
                "No live network write is performed"
        ));
        result.put("constraints", List.of(
                "dryRun must remain true",
                "APPLY_CELL_PARAMETER_CHANGE remains DENY"
        ));
        result.put("synthetic", true);
        return result;
    }

    private static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String string(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new DomainValidationException("cellId is required");
        }
        return String.valueOf(value);
    }
}
