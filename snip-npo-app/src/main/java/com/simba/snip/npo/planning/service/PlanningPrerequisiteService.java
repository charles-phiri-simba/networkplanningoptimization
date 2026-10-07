package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.NetworkTwinRepository;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.planning.api.PlanningPrerequisiteDto;
import com.simba.snip.npo.planning.persist.PlanningScenarioCellEntity;
import com.simba.snip.npo.twin.TwinFreshness;
import com.simba.snip.npo.twin.TwinScenarioService;
import com.simba.snip.npo.twin.TwinScopeType;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PlanningPrerequisiteService {

    private final PlanningScenarioService scenarioService;
    private final NetworkTwinRepository twinRepository;
    private final TwinSynchronizationService synchronizationService;
    private final TwinScenarioService twinScenarioService;

    public PlanningPrerequisiteService(
            PlanningScenarioService scenarioService,
            NetworkTwinRepository twinRepository,
            TwinSynchronizationService synchronizationService,
            TwinScenarioService twinScenarioService
    ) {
        this.scenarioService = scenarioService;
        this.twinRepository = twinRepository;
        this.synchronizationService = synchronizationService;
        this.twinScenarioService = twinScenarioService;
    }

    @Transactional(readOnly = true)
    public List<PlanningPrerequisiteDto> prerequisites(UUID scenarioId) {
        PlanningScenarioGraph graph = scenarioService.requireGraph(scenarioId);
        List<PlanningPrerequisiteDto> result = new ArrayList<>();
        for (PlanningScenarioCellEntity cell : graph.cells()) {
            result.add(forCell(cell.getCellId()));
        }
        return result;
    }

    public PlanningPrerequisiteDto forCell(String cellId) {
        return twinRepository.findByScopeTypeAndScopeId(TwinScopeType.CELL.name(), cellId)
                .map(this::fromTwin)
                .orElseGet(() -> new PlanningPrerequisiteDto(cellId, false, null, null, "MISSING", null, false));
    }

    private PlanningPrerequisiteDto fromTwin(NetworkTwinEntity twin) {
        if (twin.getLatestVersion() < 1) {
            return new PlanningPrerequisiteDto(twin.getScopeId(), true, twin.getId(), twin.getLatestVersion(), "MISSING", null, false);
        }
        NetworkTwinVersionEntity version = synchronizationService.requireVersion(twin.getId(), twin.getLatestVersion());
        TwinFreshness freshness = synchronizationService.freshness(version);
        BigDecimal observed = null;
        try {
            observed = twinScenarioService.baselineTxPower(version);
        } catch (RuntimeException ignored) {
            // observed remains null
        }
        boolean canEvaluate = freshness == TwinFreshness.CURRENT && observed != null;
        return new PlanningPrerequisiteDto(
                twin.getScopeId(),
                true,
                twin.getId(),
                version.getVersion(),
                freshness.name(),
                observed,
                canEvaluate
        );
    }
}
