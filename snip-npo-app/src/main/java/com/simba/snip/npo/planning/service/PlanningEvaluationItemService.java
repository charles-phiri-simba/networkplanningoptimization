package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.api.ScenarioChangeRequest;
import com.simba.snip.npo.domain.DomainConflictException;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.NetworkTwinRepository;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.persist.SimulationScenarioEntity;
import com.simba.snip.npo.planning.PlanningFailureCode;
import com.simba.snip.npo.planning.persist.PlanningEvaluationItemEntity;
import com.simba.snip.npo.planning.repository.PlanningEvaluationItemRepository;
import com.simba.snip.npo.planning.repository.PlanningEvaluationRepository;
import com.simba.snip.npo.twin.CellParameterSimulationModel;
import com.simba.snip.npo.twin.DigitalTwinSimulationService;
import com.simba.snip.npo.twin.TwinScopeType;
import com.simba.snip.npo.twin.TwinScenarioService;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PlanningEvaluationItemService {

    private static final Logger log = LoggerFactory.getLogger(PlanningEvaluationItemService.class);

    private final NetworkTwinRepository twinRepository;
    private final TwinSynchronizationService synchronizationService;
    private final TwinScenarioService twinScenarioService;
    private final DigitalTwinSimulationService simulationService;
    private final PlanningEvaluationItemRepository itemRepository;
    private final PlanningEvaluationRepository evaluationRepository;
    private final PlanningMetrics metrics;

    public PlanningEvaluationItemService(
            NetworkTwinRepository twinRepository,
            TwinSynchronizationService synchronizationService,
            TwinScenarioService twinScenarioService,
            DigitalTwinSimulationService simulationService,
            PlanningEvaluationItemRepository itemRepository,
            PlanningEvaluationRepository evaluationRepository,
            PlanningMetrics metrics
    ) {
        this.twinRepository = twinRepository;
        this.synchronizationService = synchronizationService;
        this.twinScenarioService = twinScenarioService;
        this.simulationService = simulationService;
        this.itemRepository = itemRepository;
        this.evaluationRepository = evaluationRepository;
        this.metrics = metrics;
    }

    @Transactional
    public void process(UUID scenarioId, UUID evaluationId, PlanningIntentWork work) {
        evaluationRepository.lockById(evaluationId)
                .orElseThrow(() -> new IllegalStateException("evaluation missing: " + evaluationId));
        if (itemRepository.findByEvaluationIdAndAlternativeIdAndCellId(evaluationId, work.alternativeId(), work.cellId())
                .filter(item -> "SUCCEEDED".equals(item.getOutcome()))
                .isPresent()) {
            return;
        }
        itemRepository.findByEvaluationIdAndAlternativeIdAndCellId(evaluationId, work.alternativeId(), work.cellId())
                .ifPresent(itemRepository::delete);

        NetworkTwinEntity twin = twinRepository
                .findByScopeTypeAndScopeId(TwinScopeType.CELL.name(), work.cellId())
                .orElse(null);
        if (twin == null || twin.getLatestVersion() < 1) {
            fail(evaluationId, work, PlanningFailureCode.ADMISSION_TWIN_MISSING, "CELL Digital Twin is missing");
            return;
        }
        NetworkTwinVersionEntity version = synchronizationService.requireVersion(twin.getId(), twin.getLatestVersion());
        try {
            synchronizationService.requireCurrentForSimulation(version);
        } catch (DomainConflictException ex) {
            fail(evaluationId, work, PlanningFailureCode.ADMISSION_TWIN_NOT_CURRENT, ex.getMessage());
            return;
        }
        BigDecimal baseline;
        try {
            baseline = twinScenarioService.baselineTxPower(version);
        } catch (DomainValidationException ex) {
            fail(evaluationId, work, PlanningFailureCode.ADMISSION_BASELINE_MISSING, ex.getMessage());
            return;
        }

        String configurationFingerprint = PlanningFingerprintService.configurationToken(version.getSourceContextVersion());
        List<PlanningEvaluationItemEntity> reusable = itemRepository.findReusableSucceeded(
                scenarioId,
                work.alternativeId(),
                work.cellId(),
                work.parameterId(),
                work.intendedValue(),
                twin.getId(),
                version.getVersion(),
                baseline,
                configurationFingerprint,
                CellParameterSimulationModel.MODEL_ID,
                CellParameterSimulationModel.MODEL_VERSION
        );
        if (!reusable.isEmpty()) {
            PlanningEvaluationItemEntity prior = reusable.get(0);
            itemRepository.save(PlanningEvaluationItemEntity.succeeded(
                    UUID.randomUUID(),
                    evaluationId,
                    work.alternativeId(),
                    work.cellId(),
                    work.parameterId(),
                    work.intendedValue(),
                    prior.getTwinId(),
                    prior.getTwinVersion(),
                    prior.getPinnedBaselineTxPower(),
                    prior.getConfigurationFingerprint(),
                    prior.getSimulationScenarioId(),
                    prior.getSimulationRunId(),
                    prior.getModelId(),
                    prior.getModelVersion(),
                    true,
                    prior.getConfidence()
            ));
            metrics.incrementRunsReused();
            metrics.incrementItemsSucceeded();
            log.info(
                    "planningEvaluationItemReused evaluationId={} cellId={} simulationRunId={}",
                    evaluationId, work.cellId(), prior.getSimulationRunId()
            );
            return;
        }

        String shortEval = evaluationId.toString().substring(0, 8);
        String name = ("planning-" + shortEval + "-" + work.alternativeOrdinal() + "-" + work.cellId());
        if (name.length() > 128) {
            name = name.substring(0, 128);
        }
        try {
            SimulationScenarioEntity child = twinScenarioService.create(
                    twin.getId(),
                    name,
                    "PI7 independent cell-local what-if",
                    "planning",
                    version.getVersion(),
                    new ScenarioChangeRequest(
                            work.parameterId(),
                            baseline.doubleValue(),
                            work.intendedValue().doubleValue()
                    )
            );
            Map<String, Object> result = simulationService.executeAdmittedCellLocalDryRun(child.getId());
            UUID runId = UUID.fromString(String.valueOf(result.get("simulationId")));
            itemRepository.save(PlanningEvaluationItemEntity.succeeded(
                    UUID.randomUUID(),
                    evaluationId,
                    work.alternativeId(),
                    work.cellId(),
                    work.parameterId(),
                    work.intendedValue(),
                    twin.getId(),
                    version.getVersion(),
                    baseline,
                    configurationFingerprint,
                    child.getId(),
                    runId,
                    CellParameterSimulationModel.MODEL_ID,
                    CellParameterSimulationModel.MODEL_VERSION,
                    false,
                    String.valueOf(result.getOrDefault("confidence", "LOW"))
            ));
            metrics.incrementItemsSucceeded();
            log.info(
                    "planningEvaluationItemSucceeded evaluationId={} cellId={} twinId={} version={} simulationRunId={}",
                    evaluationId, work.cellId(), twin.getId(), version.getVersion(), runId
            );
        } catch (DomainConflictException ex) {
            fail(evaluationId, work, PlanningFailureCode.ADMISSION_TWIN_NOT_CURRENT, ex.getMessage());
        } catch (DomainValidationException ex) {
            fail(evaluationId, work, PlanningFailureCode.SIMULATION_REJECTED, ex.getMessage());
        } catch (RuntimeException ex) {
            fail(evaluationId, work, PlanningFailureCode.SIMULATION_FAILED, ex.getMessage());
        }
    }

    private void fail(UUID evaluationId, PlanningIntentWork work, PlanningFailureCode code, String message) {
        itemRepository.save(PlanningEvaluationItemEntity.failed(
                UUID.randomUUID(),
                evaluationId,
                work.alternativeId(),
                work.cellId(),
                work.parameterId(),
                work.intendedValue(),
                code.name(),
                message
        ));
        metrics.incrementItemsFailed();
        log.info(
                "planningEvaluationItemFailed evaluationId={} cellId={} failureCode={}",
                evaluationId, work.cellId(), code
        );
    }

}
