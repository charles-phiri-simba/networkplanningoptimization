package com.simba.snip.npo.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.assurance.AssuranceCaseService;
import com.simba.snip.npo.config.SnipProperties;
import com.simba.snip.npo.domain.DomainNotFoundException;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.AgentCaseMemoryEntity;
import com.simba.snip.npo.persist.AgentCaseMemoryRepository;
import com.simba.snip.npo.persist.AgentPlanEntity;
import com.simba.snip.npo.persist.AgentPlanRepository;
import com.simba.snip.npo.persist.AgentPlanStepEntity;
import com.simba.snip.npo.persist.AgentPlanStepRepository;
import com.simba.snip.npo.persist.AgentRunEntity;
import com.simba.snip.npo.persist.AgentRunRepository;
import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.ProposedActionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrationService.class);

    private final AgentRunRepository runRepository;
    private final AgentPlanRepository planRepository;
    private final AgentPlanStepRepository stepRepository;
    private final AgentCaseMemoryRepository caseMemoryRepository;
    private final AssuranceCaseService assuranceCaseService;
    private final ChiefOrchestrationAgent chief;
    private final ContextAgent contextAgent;
    private final AssuranceAgent assuranceAgent;
    private final KnowledgeAgent knowledgeAgent;
    private final DecisionAgent decisionAgent;
    private final AgentProposalAdapter proposalAdapter;
    private final AgentAuditService auditService;
    private final AgentMetrics metrics;
    private final SnipProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<UUID, AgentRunMemory> memories = new ConcurrentHashMap<>();

    public AgentOrchestrationService(
            AgentRunRepository runRepository,
            AgentPlanRepository planRepository,
            AgentPlanStepRepository stepRepository,
            AgentCaseMemoryRepository caseMemoryRepository,
            AssuranceCaseService assuranceCaseService,
            ChiefOrchestrationAgent chief,
            ContextAgent contextAgent,
            AssuranceAgent assuranceAgent,
            KnowledgeAgent knowledgeAgent,
            DecisionAgent decisionAgent,
            AgentProposalAdapter proposalAdapter,
            AgentAuditService auditService,
            AgentMetrics metrics,
            SnipProperties properties,
            ObjectMapper objectMapper
    ) {
        this.runRepository = runRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.caseMemoryRepository = caseMemoryRepository;
        this.assuranceCaseService = assuranceCaseService;
        this.chief = chief;
        this.contextAgent = contextAgent;
        this.assuranceAgent = assuranceAgent;
        this.knowledgeAgent = knowledgeAgent;
        this.decisionAgent = decisionAgent;
        this.proposalAdapter = proposalAdapter;
        this.auditService = auditService;
        this.metrics = metrics;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID start(AgentRunCommand command) {
        if (command.objective() == null || command.objective().isBlank()) {
            throw new DomainValidationException("objective is required");
        }
        AssuranceCaseEntity assuranceCase = assuranceCaseService.findById(command.assuranceCaseId())
                .orElseThrow(() -> new DomainNotFoundException("assurance case", String.valueOf(command.assuranceCaseId())));
        String cellId = assuranceCase.getAffectedEntityId();
        Instant started = Instant.now();
        int maxSteps = valueOr(command.maxSteps(), properties.getAgentMaxSteps());
        int maxAgentCalls = valueOr(command.maxAgentCalls(), properties.getAgentMaxAgentCalls());
        int maxRetries = valueOr(command.maxRetries(), properties.getAgentMaxRetriesPerStep());
        long timeoutMs = command.timeoutMs() == null ? properties.getAgentOverallRunTimeoutMs() : command.timeoutMs();
        UUID runId = UUID.randomUUID();
        AgentRunEntity run = AgentRunEntity.create(
                runId,
                command.objective().trim(),
                AgentRunStatus.RUNNING.name(),
                command.assuranceCaseId(),
                command.initiatedBy() == null || command.initiatedBy().isBlank() ? "demo-user" : command.initiatedBy().trim(),
                started,
                maxSteps,
                maxAgentCalls,
                maxRetries,
                timeoutMs
        );
        runRepository.save(run);
        metrics.incrementRunsStarted();
        auditService.append(runId, AgentRunAuditEventType.RUN_STARTED, AgentRegistry.CHIEF, "objective stored");
        log.info("agentRunsStarted=1 runId={} assuranceCaseId={}", runId, command.assuranceCaseId());

        AgentRunMemory memory = new AgentRunMemory();
        memories.put(runId, memory);
        int agentCalls = 0;
        int modelCalls = 0;
        try {
            List<AgentOutputs.PlannedStep> planned = chief.createCanonicalPlan(run.getObjective(), cellId);
            modelCalls++;
            agentCalls++;
            AgentPlanEntity plan = planRepository.save(AgentPlanEntity.create(UUID.randomUUID(), runId, run.getObjective()));
            for (AgentOutputs.PlannedStep item : planned) {
                stepRepository.save(AgentPlanStepEntity.create(
                        UUID.randomUUID(),
                        plan.getId(),
                        item.stepNumber(),
                        item.agentRole().name(),
                        item.task(),
                        item.requiredInputs(),
                        item.expectedOutput(),
                        PlanStepStatus.PENDING.name()
                ));
            }
            auditService.append(runId, AgentRunAuditEventType.PLAN_CREATED, AgentRegistry.CHIEF, "steps=" + planned.size());

            boolean failed = false;
            String failReason = null;
            List<AgentPlanStepEntity> steps = stepRepository.findByPlanIdOrderByStepNumberAsc(plan.getId());
            for (AgentPlanStepEntity step : steps) {
                if (failed) {
                    step.setStatus(PlanStepStatus.SKIPPED.name());
                    stepRepository.save(step);
                    continue;
                }
                if (Duration.between(started, Instant.now()).toMillis() > timeoutMs
                        || step.getStepNumber() > maxSteps
                        || agentCalls >= maxAgentCalls
                        || modelCalls >= properties.getAgentMaxTotalModelCalls()) {
                    metrics.incrementLimitReached();
                    auditService.append(runId, AgentRunAuditEventType.LIMIT_REACHED, AgentRegistry.CHIEF,
                            "step=" + step.getStepNumber() + " agentCalls=" + agentCalls + " modelCalls=" + modelCalls);
                    step.setStatus(PlanStepStatus.SKIPPED.name());
                    stepRepository.save(step);
                    failed = true;
                    failReason = "LIMIT_REACHED";
                    continue;
                }
                run.setCurrentStep(step.getStepNumber());
                runRepository.save(run);
                step.setStatus(PlanStepStatus.RUNNING.name());
                stepRepository.save(step);
                metrics.incrementStepsStarted();
                String agentId = registryAgent(step.getAgentRole());
                auditService.append(runId, AgentRunAuditEventType.STEP_STARTED, agentId, "step=" + step.getStepNumber());
                boolean stepOk = executeWithRetry(run, step, memory, cellId, maxRetries);
                agentCalls++;
                modelCalls++;
                if (!stepOk) {
                    metrics.incrementStepsFailed();
                    auditService.append(runId, AgentRunAuditEventType.STEP_FAILED, agentId, "step=" + step.getStepNumber());
                    if (isCritical(step.getAgentRole())) {
                        failed = true;
                        failReason = "STEP_FAILED";
                    }
                } else {
                    metrics.incrementStepsCompleted();
                    auditService.append(runId, AgentRunAuditEventType.STEP_COMPLETED, agentId, "step=" + step.getStepNumber());
                }
            }

            if (!failed && memory.decision() != null && memory.decision().candidateAction() != null) {
                if (memory.proposedActionIds().size() >= properties.getAgentMaxProposedActions()) {
                    metrics.incrementLimitReached();
                    auditService.append(runId, AgentRunAuditEventType.LIMIT_REACHED, AgentRegistry.CHIEF, "maxProposedActions");
                    failed = true;
                    failReason = "LIMIT_REACHED";
                } else {
                    ProposedActionEntity proposed = proposalAdapter.propose(
                            runId, run.getAssuranceCaseId(), memory.decision().candidateAction());
                    memory.addProposedAction(proposed.getId());
                    metrics.incrementActionsProposed();
                    auditService.append(runId, AgentRunAuditEventType.ACTION_PROPOSED, AgentRegistry.DECISION,
                            "actionId=" + proposed.getId() + " type=" + proposed.getActionType());
                }
            }

            Instant completed = Instant.now();
            run.setCompletedAt(completed);
            if (failed) {
                run.setStatus(AgentRunStatus.FAILED.name());
                metrics.incrementRunsFailed();
                auditService.append(runId, AgentRunAuditEventType.RUN_FAILED, AgentRegistry.CHIEF,
                        failReason == null ? "FAILED" : failReason);
            } else {
                run.setStatus(AgentRunStatus.COMPLETED.name());
                metrics.incrementRunsCompleted();
                auditService.append(runId, AgentRunAuditEventType.RUN_COMPLETED, AgentRegistry.CHIEF, "completed");
            }
            persistCaseMemory(run, memory);
            runRepository.save(run);
            metrics.recordRunLatencyMs(Duration.between(started, completed).toMillis());
            log.info(
                    "agentRunFinished runId={} status={} agentCalls={} modelCalls={} actionsProposed={}",
                    runId, run.getStatus(), agentCalls, modelCalls, memory.proposedActionIds().size()
            );
            return runId;
        } finally {
            memories.remove(runId);
        }
    }

    private boolean executeWithRetry(
            AgentRunEntity run,
            AgentPlanStepEntity step,
            AgentRunMemory memory,
            String cellId,
            int maxRetries
    ) {
        AgentStepException last = null;
        int attempts = 0;
        while (attempts <= maxRetries) {
            try {
                Instant stepStarted = Instant.now();
                String summary = invoke(step, memory, run, cellId);
                if (Duration.between(stepStarted, Instant.now()).toMillis() > properties.getAgentPerAgentTimeoutMs()) {
                    throw new AgentStepException("agent timeout");
                }
                step.setStatus(PlanStepStatus.COMPLETED.name());
                step.setOutputSummary(clip(summary, 2000));
                stepRepository.save(step);
                return true;
            } catch (RuntimeException ex) {
                last = ex instanceof AgentStepException stepEx ? stepEx : new AgentStepException(ex.getMessage());
                attempts++;
                if (attempts <= maxRetries) {
                    metrics.incrementRetries();
                }
            }
        }
        step.setStatus(PlanStepStatus.FAILED.name());
        step.setOutputSummary(clip(last == null ? "step failed" : last.getMessage(), 512));
        stepRepository.save(step);
        return false;
    }

    private String invoke(AgentPlanStepEntity step, AgentRunMemory memory, AgentRunEntity run, String cellId) {
        AgentRole role = AgentRole.valueOf(step.getAgentRole());
        return switch (role) {
            case CONTEXT -> {
                AgentOutputs.ContextResult result = contextAgent.invoke(cellId);
                memory.setContext(result);
                yield "cell=" + result.cellId() + " provenance=" + result.provenance();
            }
            case ASSURANCE -> {
                AgentOutputs.AssuranceResult result = assuranceAgent.invoke(run.getAssuranceCaseId());
                memory.setAssurance(result);
                yield "caseType=" + result.caseType() + " severity=" + result.severity() + " confidence=" + result.confidence();
            }
            case KNOWLEDGE -> {
                AgentOutputs.KnowledgeResult result = knowledgeAgent.invoke(run.getId(), cellId, run.getObjective());
                memory.setKnowledge(result);
                yield "insufficientEvidence=" + result.insufficientEvidence() + " sources=" + result.retrievedSources().size();
            }
            case DECISION -> {
                AgentOutputs.DecisionResult result = decisionAgent.invoke(
                        run.getObjective(), cellId, memory.context(), memory.assurance(), memory.knowledge());
                memory.setDecision(result);
                yield "candidate=" + (result.candidateAction() == null ? "none" : result.candidateAction().actionType());
            }
            case CHIEF_ORCHESTRATOR -> throw new AgentStepException("chief does not execute as a specialist step");
        };
    }

    private void persistCaseMemory(AgentRunEntity run, AgentRunMemory memory) {
        Map<String, Object> findings = new LinkedHashMap<>();
        findings.put("contextCellId", memory.context() == null ? null : memory.context().cellId());
        findings.put("assuranceSeverity", memory.assurance() == null ? null : memory.assurance().severity());
        findings.put("knowledgeInsufficient", memory.knowledge() == null || memory.knowledge().insufficientEvidence());
        findings.put("candidateAction", memory.decision() == null || memory.decision().candidateAction() == null
                ? null
                : memory.decision().candidateAction().actionType().name());
        String summary = "AgentRun " + run.getId() + " status=" + run.getStatus();
        if (memory.decision() != null && memory.decision().summary() != null) {
            summary = clip(memory.decision().summary(), 1024);
        }
        caseMemoryRepository.save(AgentCaseMemoryEntity.create(
                UUID.randomUUID(),
                run.getAssuranceCaseId(),
                run.getId(),
                summary,
                writeJson(findings),
                writeJson(memory.proposedActionIds()),
                Instant.now()
        ));
    }

    private static boolean isCritical(String role) {
        return AgentRole.CONTEXT.name().equals(role)
                || AgentRole.ASSURANCE.name().equals(role)
                || AgentRole.DECISION.name().equals(role);
    }

    private static String registryAgent(String role) {
        return switch (AgentRole.valueOf(role)) {
            case CHIEF_ORCHESTRATOR -> AgentRegistry.CHIEF;
            case KNOWLEDGE -> AgentRegistry.KNOWLEDGE;
            case CONTEXT -> AgentRegistry.CONTEXT;
            case ASSURANCE -> AgentRegistry.ASSURANCE;
            case DECISION -> AgentRegistry.DECISION;
        };
    }

    private static int valueOr(Integer override, int fallback) {
        return override == null ? fallback : override;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
