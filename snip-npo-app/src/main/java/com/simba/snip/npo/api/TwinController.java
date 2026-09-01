package com.simba.snip.npo.api;

import com.simba.snip.npo.twin.SimulationQueryService;
import com.simba.snip.npo.twin.TwinScenarioService;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TwinController {

    private final TwinSynchronizationService synchronizationService;
    private final TwinScenarioService scenarioService;
    private final SimulationQueryService queryService;

    public TwinController(
            TwinSynchronizationService synchronizationService,
            TwinScenarioService scenarioService,
            SimulationQueryService queryService
    ) {
        this.synchronizationService = synchronizationService;
        this.scenarioService = scenarioService;
        this.queryService = queryService;
    }

    @PostMapping("/api/v1/twins/cells/{cellId}/synchronize")
    public TwinDetailDto synchronize(@PathVariable String cellId) {
        var twin = synchronizationService.synchronizeCell(cellId);
        return queryService.twin(twin.getId());
    }

    @GetMapping("/api/v1/twins/{twinId}")
    public TwinDetailDto getTwin(@PathVariable UUID twinId) {
        return queryService.twin(twinId);
    }

    @GetMapping("/api/v1/twins/{twinId}/versions")
    public List<TwinVersionSummaryDto> versions(@PathVariable UUID twinId) {
        return queryService.versions(twinId);
    }

    @GetMapping("/api/v1/twins/{twinId}/versions/{version}")
    public TwinVersionDetailDto version(@PathVariable UUID twinId, @PathVariable int version) {
        return queryService.version(twinId, version);
    }

    @PostMapping(path = "/api/v1/twins/{twinId}/scenarios", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ScenarioDetailDto createScenario(@PathVariable UUID twinId, @RequestBody CreateScenarioRequest request) {
        var stored = scenarioService.create(
                twinId,
                request.name(),
                request.description(),
                request.createdBy(),
                request.baselineTwinVersion(),
                request.change()
        );
        return queryService.scenario(stored.getId());
    }

    @GetMapping("/api/v1/twins/{twinId}/scenarios")
    public List<ScenarioDetailDto> scenarios(@PathVariable UUID twinId) {
        return queryService.scenarios(twinId);
    }

    @GetMapping("/api/v1/scenarios/{scenarioId}")
    public ScenarioDetailDto scenario(@PathVariable UUID scenarioId) {
        return queryService.scenario(scenarioId);
    }

    @GetMapping("/api/v1/simulations/{simulationId}")
    public SimulationDetailDto simulation(@PathVariable UUID simulationId) {
        return queryService.simulation(simulationId);
    }

    @GetMapping("/api/v1/simulation-comparisons")
    public SimulationComparisonDto compare(@RequestParam UUID left, @RequestParam UUID right) {
        return queryService.compare(left, right);
    }
}
