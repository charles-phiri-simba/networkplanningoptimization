package com.simba.snip.npo.planning.service;

import com.simba.snip.npo.domain.DomainRules;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.NetworkTwinEntity;
import com.simba.snip.npo.persist.NetworkTwinRepository;
import com.simba.snip.npo.persist.NetworkTwinVersionEntity;
import com.simba.snip.npo.planning.PlanningException;
import com.simba.snip.npo.planning.PlanningFailureCode;
import com.simba.snip.npo.planning.api.EvaluatePlanningScenarioRequest;
import com.simba.snip.npo.planning.api.PlanningEvaluationDto;
import com.simba.snip.npo.planning.persist.PlanningAlternativeEntity;
import com.simba.snip.npo.planning.persist.PlanningCellIntentEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationEntity;
import com.simba.snip.npo.planning.persist.PlanningEvaluationItemEntity;
import com.simba.snip.npo.planning.persist.PlanningScenarioEntity;
import com.simba.snip.npo.planning.repository.PlanningEvaluationItemRepository;
import com.simba.snip.npo.planning.repository.PlanningEvaluationRepository;
import com.simba.snip.npo.planning.repository.PlanningScenarioRepository;
import com.simba.snip.npo.twin.CellParameterSimulationModel;
import com.simba.snip.npo.twin.TwinFreshness;
import com.simba.snip.npo.twin.TwinScenarioService;
import com.simba.snip.npo.twin.TwinScopeType;
import com.simba.snip.npo.twin.TwinSynchronizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanningEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(PlanningEvaluationService.class);

    private final PlanningScenarioRepository scenarioRepository;
    private final PlanningScenarioService scenarioService;
    private final PlanningEvaluationRepository evaluationRepository;
    private final PlanningEvaluationItemRepository itemRepository;
    private final PlanningFingerprintService fingerprintService;
    private final PlanningEvaluationItemService itemService;
    private final NetworkTwinRepository twinRepository;
    private final TwinSynchronizationService synchronizationService;
    private final TwinScenarioService twinScenarioService;
    private final PlanningMapper mapper;
    private final PlanningMetrics metrics;
    private final PlanningEvaluationService self;

    public PlanningEvaluationService(
            PlanningScenarioRepository scenarioRepository,
            PlanningScenarioService scenarioService,
            PlanningEvaluationRepository evaluationRepository,
            PlanningEvaluationItemRepository itemRepository,
            PlanningFingerprintService fingerprintService,
            PlanningEvaluationItemService itemService,
            NetworkTwinRepository twinRepository,
            TwinSynchronizationService synchronizationService,
            TwinScenarioService twinScenarioService,
            PlanningMapper mapper,
            PlanningMetrics metrics,
            @Lazy PlanningEvaluationService self
    ) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioService = scenarioService;
        this.evaluationRepository = evaluationRepository;
        this.itemRepository = itemRepository;
        this.fingerprintService = fingerprintService;
        this.itemService = itemService;
        this.twinRepository = twinRepository;
        this.synchronizationService = synchronizationService;
        this.twinScenarioService = twinScenarioService;
        this.mapper = mapper;
        this.metrics = metrics;
        this.self = self;
    }

    public EvaluateResult evaluate(UUID scenarioId, EvaluatePlanningScenarioRequest request) {
        String createdBy = DomainRules.requireDomainId(request.createdBy(), "createdBy");
        StartResult start = self.start(scenarioId, createdBy, request.rowVersion());
        if (start.complete()) {
            return new EvaluateResult(self.toDto(start.evaluationId()), false);
        }
        for (PlanningIntentWork work : start.remaining()) {
            itemService.process(scenarioId, start.evaluationId(), work);
        }
        self.finalize(start.evaluationId());
        return new EvaluateResult(self.toDto(start.evaluationId()), !start.resumed());
    }

    @Transactional
    public StartResult start(UUID scenarioId, String createdBy, Integer rowVersion) {
        PlanningScenarioEntity scenario = scenarioRepository.lockById(scenarioId)
                .orElseThrow(() -> new PlanningException(PlanningFailureCode.SCENARIO_NOT_FOUND, "scenario not found"));
        if (rowVersion == null || rowVersion != scenario.getRowVersion()) {
            throw new PlanningException(PlanningFailureCode.SCENARIO_VERSION_CONFLICT, "scenario version conflict");
        }
        PlanningScenarioGraph graph = scenarioService.loadGraph(scenarioId);
        if (graph.intents().isEmpty()) {
            throw new PlanningException(PlanningFailureCode.EMPTY_INTENTS, "at least one intent is required");
        }
        PlanningEvaluationEntity inProgress = evaluationRepository.findByScenarioIdAndStatus(scenarioId, "IN_PROGRESS").orElse(null);
        if (inProgress != null) {
            return new StartResult(inProgress.getId(), remainingWork(graph, inProgress.getId()), false, true);
        }
        String intentFp = fingerprintService.intentFingerprint(scenarioId, graph.alternatives(), graph.intents());
        Map<String, Pin> currentPins = resolveCurrentPins(graph);
        String wouldAdmit = fingerprintService.admissionFingerprint(intentFp, admissionPreview(graph, currentPins));
        boolean allCurrent = graph.intents().stream().allMatch(intent -> currentPins.containsKey(intent.getCellId()));
        List<PlanningEvaluationEntity> terminals = graph.evaluations().stream()
                .filter(e -> !"IN_PROGRESS".equals(e.getStatus()))
                .filter(e -> intentFp.equals(e.getIntentFingerprint()))
                .sorted(Comparator.comparing(PlanningEvaluationEntity::getCreatedAt).reversed())
                .toList();
        PlanningEvaluationEntity matching = terminals.isEmpty() ? null : terminals.get(0);

        if (matching != null && wouldAdmit.equals(matching.getAdmissionFingerprint())) {
            if ("SUCCEEDED".equals(matching.getStatus())) {
                return new StartResult(matching.getId(), List.of(), true, false);
            }
        } else if (matching != null && "SUCCEEDED".equals(matching.getStatus()) && !allCurrent) {
            return new StartResult(matching.getId(), List.of(), true, false);
        }
        try {
            PlanningEvaluationEntity created = evaluationRepository.save(PlanningEvaluationEntity.start(
                    UUID.randomUUID(),
                    scenarioId,
                    intentFp,
                    wouldAdmit,
                    createdBy,
                    Instant.now(),
                    graph.intents().size()
            ));
            metrics.incrementEvaluationsStarted();
            log.info("planningEvaluationCreated evaluationId={} scenarioId={}", created.getId(), scenarioId);
            return new StartResult(created.getId(), works(graph), false, false);
        } catch (DataIntegrityViolationException ex) {
            metrics.incrementInProgressConflicts();
            PlanningEvaluationEntity raced = evaluationRepository.findByScenarioIdAndStatus(scenarioId, "IN_PROGRESS")
                    .orElseThrow(() -> new PlanningException(PlanningFailureCode.EVALUATION_IN_PROGRESS, "evaluation in progress"));
            return new StartResult(raced.getId(), remainingWork(graph, raced.getId()), false, true);
        }
    }

    @Transactional
    public void finalize(UUID evaluationId) {
        PlanningEvaluationEntity evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new PlanningException(PlanningFailureCode.EVALUATION_NOT_FOUND, "evaluation not found"));
        if (!"IN_PROGRESS".equals(evaluation.getStatus())) {
            return;
        }
        List<PlanningEvaluationItemEntity> items = itemRepository.findByEvaluationId(evaluationId);
        int succeeded = (int) items.stream().filter(i -> "SUCCEEDED".equals(i.getOutcome())).count();
        int failed = (int) items.stream().filter(i -> "FAILED".equals(i.getOutcome())).count();
        String status = failed == 0 && succeeded > 0 ? "SUCCEEDED"
                : succeeded > 0 ? "PARTIAL"
                : "FAILED";
        evaluation.finalizeStatus(status, succeeded, failed, Instant.now());
        switch (status) {
            case "SUCCEEDED" -> metrics.incrementEvaluationsSucceeded();
            case "PARTIAL" -> metrics.incrementEvaluationsPartial();
            default -> metrics.incrementEvaluationsFailed();
        }
        log.info(
                "planningEvaluationFinalized evaluationId={} status={} succeeded={} failed={}",
                evaluationId, status, succeeded, failed
        );
    }

    @Transactional(readOnly = true)
    public PlanningEvaluationDto get(UUID scenarioId, UUID evaluationId) {
        PlanningEvaluationEntity evaluation = evaluationRepository.findByIdAndScenarioId(evaluationId, scenarioId)
                .orElseThrow(() -> new PlanningException(PlanningFailureCode.EVALUATION_NOT_FOUND, "evaluation not found"));
        return toDto(evaluation.getId());
    }

    @Transactional(readOnly = true)
    public PlanningEvaluationDto toDto(UUID evaluationId) {
        PlanningEvaluationEntity evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new PlanningException(PlanningFailureCode.EVALUATION_NOT_FOUND, "evaluation not found"));
        PlanningScenarioGraph graph = scenarioService.loadGraph(evaluation.getScenarioId());
        Map<UUID, PlanningAlternativeEntity> alts = graph.alternatives().stream()
                .collect(Collectors.toMap(PlanningAlternativeEntity::getId, a -> a));
        List<PlanningEvaluationItemEntity> items = itemRepository.findByEvaluationId(evaluationId);
        boolean historical = items.stream()
                .filter(i -> "SUCCEEDED".equals(i.getOutcome()))
                .anyMatch(i -> !stillCurrent(i));
        return mapper.toEvaluation(
                evaluation,
                items.stream().map(item -> mapper.toItem(item, alts.get(item.getAlternativeId()))).toList(),
                historical
        );
    }

    private boolean stillCurrent(PlanningEvaluationItemEntity item) {
        if (item.getTwinId() == null || item.getTwinVersion() == null) {
            return false;
        }
        try {
            NetworkTwinVersionEntity version = synchronizationService.requireVersion(item.getTwinId(), item.getTwinVersion());
            return synchronizationService.freshness(version) == TwinFreshness.CURRENT;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private List<PlanningFingerprintService.AdmissionLine> admissionPreview(
            PlanningScenarioGraph graph,
            Map<String, Pin> currentPins
    ) {
        Map<UUID, PlanningAlternativeEntity> alts = graph.alternatives().stream()
                .collect(Collectors.toMap(PlanningAlternativeEntity::getId, a -> a));
        List<PlanningFingerprintService.AdmissionLine> lines = new ArrayList<>();
        for (PlanningCellIntentEntity intent : graph.intents()) {
            PlanningAlternativeEntity alternative = alts.get(intent.getAlternativeId());
            Pin pin = currentPins.get(intent.getCellId());
            lines.add(new PlanningFingerprintService.AdmissionLine(
                    alternative == null ? 0 : alternative.getOrdinal(),
                    intent.getCellId(),
                    pin == null ? null : pin.twinId(),
                    pin == null ? null : pin.version(),
                    pin == null ? null : pin.baseline(),
                    pin == null ? null : pin.config(),
                    pin == null ? null : CellParameterSimulationModel.MODEL_ID,
                    pin == null ? null : CellParameterSimulationModel.MODEL_VERSION
            ));
        }
        return lines;
    }

    private Map<String, Pin> resolveCurrentPins(PlanningScenarioGraph graph) {
        Map<String, Pin> pins = new HashMap<>();
        for (String cellId : graph.intents().stream().map(PlanningCellIntentEntity::getCellId).distinct().toList()) {
            NetworkTwinEntity twin = twinRepository.findByScopeTypeAndScopeId(TwinScopeType.CELL.name(), cellId).orElse(null);
            if (twin == null || twin.getLatestVersion() < 1) {
                continue;
            }
            NetworkTwinVersionEntity version = synchronizationService.requireVersion(twin.getId(), twin.getLatestVersion());
            if (synchronizationService.freshness(version) != TwinFreshness.CURRENT) {
                continue;
            }
            try {
                pins.put(cellId, new Pin(
                        twin.getId(),
                        version.getVersion(),
                        twinScenarioService.baselineTxPower(version),
                        PlanningFingerprintService.configurationToken(version.getSourceContextVersion())
                ));
            } catch (DomainValidationException ignored) {
                // Missing baseline is not a CURRENT pin for pre-execution admission identity.
            }
        }
        return pins;
    }

    private List<PlanningIntentWork> works(PlanningScenarioGraph graph) {
        Map<UUID, PlanningAlternativeEntity> alts = graph.alternatives().stream()
                .collect(Collectors.toMap(PlanningAlternativeEntity::getId, a -> a));
        List<PlanningIntentWork> works = new ArrayList<>();
        for (PlanningCellIntentEntity intent : graph.intents()) {
            PlanningAlternativeEntity alt = alts.get(intent.getAlternativeId());
            works.add(new PlanningIntentWork(
                    intent.getAlternativeId(),
                    alt.getOrdinal(),
                    alt.getName(),
                    intent.getCellId(),
                    intent.getParameterId(),
                    intent.getIntendedValue()
            ));
        }
        works.sort(Comparator.comparingInt(PlanningIntentWork::alternativeOrdinal).thenComparing(PlanningIntentWork::cellId));
        return works;
    }

    private List<PlanningIntentWork> remainingWork(PlanningScenarioGraph graph, UUID evaluationId) {
        List<PlanningEvaluationItemEntity> items = itemRepository.findByEvaluationId(evaluationId);
        return works(graph).stream()
                .filter(work -> items.stream().noneMatch(item ->
                        item.getAlternativeId().equals(work.alternativeId())
                                && item.getCellId().equals(work.cellId())
                                && "SUCCEEDED".equals(item.getOutcome())))
                .toList();
    }

    public record StartResult(UUID evaluationId, List<PlanningIntentWork> remaining, boolean complete, boolean resumed) {
    }

    public record EvaluateResult(PlanningEvaluationDto body, boolean created) {
    }

    private record Pin(UUID twinId, int version, BigDecimal baseline, String config) {
    }
}
