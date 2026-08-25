package com.simba.snip.npo.mcp;

import com.simba.snip.npo.action.ActionRules;
import com.simba.snip.npo.twin.DigitalTwinSimulationService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SimulationCellParameterTool implements McpTool {

    private final DigitalTwinSimulationService digitalTwinSimulationService;

    public SimulationCellParameterTool(DigitalTwinSimulationService digitalTwinSimulationService) {
        this.digitalTwinSimulationService = digitalTwinSimulationService;
    }

    @Override
    public String name() {
        return ActionRules.CAPABILITY_SIMULATION;
    }

    @Override
    public String description() {
        return "Delegates to the Phase 6 Digital Twin synthetic cell-parameter model. Not vendor-calibrated RF physics.";
    }

    @Override
    public Map<String, Object> call(Map<String, Object> arguments) {
        return digitalTwinSimulationService.executeFromMcp(arguments);
    }
}
