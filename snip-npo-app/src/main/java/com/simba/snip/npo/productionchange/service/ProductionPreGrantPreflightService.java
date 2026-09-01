package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import com.simba.snip.npo.changeintelligence.policy.KnowledgeGate;
import com.simba.snip.npo.changeintelligence.repository.NetworkChangeProposalRepository;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.NetworkDriftObservationRepository;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusEntity;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.LeaseHandle;
import com.simba.snip.npo.productionchange.domain.PreflightCheckResult;
import com.simba.snip.npo.productionchange.domain.PreflightOutcome;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionChangeAuthorizationEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.policy.ProductionBlastRadiusPolicy;
import com.simba.snip.npo.productionchange.policy.ProductionChangeWindowPolicy;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProductionPreGrantPreflightService {

    private final ProductionChangeProperties properties;
    private final ProductionTargetRegistry targetRegistry;
    private final ProductionBindingAssembler bindingAssembler;
    private final ProductionFingerprintService fingerprintService;
    private final ProductionAuthorizationService authorizationService;
    private final ProductionChangeControlService changeControlService;
    private final ProductionChangeWindowPolicy windowPolicy;
    private final ProductionBlastRadiusPolicy blastRadiusPolicy;
    private final ProductionLeaseService leaseService;
    private final CellRepository cellRepository;
    private final NetworkChangeProposalRepository proposalRepository;
    private final NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    private final NetworkDriftObservationRepository driftObservationRepository;
    private final KnowledgeGate knowledgeGate;
    private final ProductionChangeAuditService auditService;
    private final ProductionFailurePersistenceService failurePersistenceService;
    private final Clock clock;

    public ProductionPreGrantPreflightService(
            ProductionChangeProperties properties,
            ProductionTargetRegistry targetRegistry,
            ProductionBindingAssembler bindingAssembler,
            ProductionFingerprintService fingerprintService,
            ProductionAuthorizationService authorizationService,
            ProductionChangeControlService changeControlService,
            ProductionChangeWindowPolicy windowPolicy,
            ProductionBlastRadiusPolicy blastRadiusPolicy,
            ProductionLeaseService leaseService,
            CellRepository cellRepository,
            NetworkChangeProposalRepository proposalRepository,
            NetworkKnowledgeStatusRepository knowledgeStatusRepository,
            NetworkDriftObservationRepository driftObservationRepository,
            KnowledgeGate knowledgeGate,
            ProductionChangeAuditService auditService,
            ProductionFailurePersistenceService failurePersistenceService,
            Clock clock
    ) {
        this.properties = properties;
        this.targetRegistry = targetRegistry;
        this.bindingAssembler = bindingAssembler;
        this.fingerprintService = fingerprintService;
        this.authorizationService = authorizationService;
        this.changeControlService = changeControlService;
        this.windowPolicy = windowPolicy;
        this.blastRadiusPolicy = blastRadiusPolicy;
        this.leaseService = leaseService;
        this.cellRepository = cellRepository;
        this.proposalRepository = proposalRepository;
        this.knowledgeStatusRepository = knowledgeStatusRepository;
        this.driftObservationRepository = driftObservationRepository;
        this.knowledgeGate = knowledgeGate;
        this.auditService = auditService;
        this.failurePersistenceService = failurePersistenceService;
        this.clock = clock;
    }

    public List<PreflightCheckResult> evaluate(
            ProductionNetworkChangeEntity change,
            LeaseHandle lease,
            ActorPrincipal actor
    ) {
        Instant now = clock.instant();
        List<PreflightCheckResult> checks = new ArrayList<>();
        try {
            if (!properties.isEnabled() || !properties.isGlobalExecutionEnabled()) {
                checks.add(PreflightCheckResult.deny(
                        "killSwitch",
                        ProductionReasonCode.PRODUCTION_KILL_SWITCH_DENY,
                        "global execution is disabled"
                ));
                return deny(change, actor, checks);
            }
            ProductionNetworkTargetEntity target = targetRegistry.require(change.getProductionTargetId());
            bindingAssembler.requireTargetEligible(target);
            checks.add(PreflightCheckResult.pass("target"));
            ProductionBindingAssembler.UpstreamBinding binding =
                    bindingAssembler.requireVerifiedPhase15(change.getPhase15ExecutionId());
            checks.add(PreflightCheckResult.pass("phase15"));
            if (!binding.plan().getFingerprint().equals(change.getPhase14PlanFingerprint())) {
                checks.add(PreflightCheckResult.deny("phase14", ProductionReasonCode.PRODUCTION_PHASE14_PLAN_STALE, "plan fingerprint changed"));
                return deny(change, actor, checks);
            }
            checks.add(PreflightCheckResult.pass("phase14"));
            requireProposal(binding.plan().getProposalId(), checks);
            changeControlService.requireCurrent(change.getProductionChangeId(), now);
            checks.add(PreflightCheckResult.pass("changeControl"));
            windowPolicy.requireOpen(now, binding.execution().getExecutionWindowOpensAt(), binding.execution().getExecutionWindowClosesAt());
            checks.add(PreflightCheckResult.pass("window"));
            blastRadiusPolicy.requireSingleCellParameterOperation(1, 1, 1);
            blastRadiusPolicy.requireTxPower(change.getParameter());
            checks.add(PreflightCheckResult.pass("scope"));
            if (cellRepository.findByCellId(change.getCellId()).isEmpty()) {
                checks.add(PreflightCheckResult.unknown("cell", ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "cell not found"));
                return deny(change, actor, checks);
            }
            checks.add(PreflightCheckResult.pass("cell"));
            requireKnowledge(binding.plan().getSourceSystem(), checks);
            requireNoOpenDrift(binding.plan().getSourceSystem(), change.getCellId(), checks);
            String currentFingerprint = fingerprintService.compute(bindingAssembler.fingerprintInput(
                    change,
                    target,
                    binding,
                    change.getChangeControlReference(),
                    change.getAuthorizationGeneration()
            ));
            if (!currentFingerprint.equals(change.getProductionFingerprint())) {
                checks.add(PreflightCheckResult.deny("fingerprint", ProductionReasonCode.PRODUCTION_FINGERPRINT_STALE, "fingerprint changed"));
                return deny(change, actor, checks);
            }
            ProductionChangeAuthorizationEntity authorization =
                    authorizationService.requireActive(change.getProductionChangeId(), currentFingerprint);
            if (authorization.getExpiresAt() != null && !authorization.getExpiresAt().isAfter(now)) {
                checks.add(PreflightCheckResult.deny("authorization", ProductionReasonCode.PRODUCTION_AUTHORIZATION_STALE, "authorization expired"));
                return deny(change, actor, checks);
            }
            checks.add(PreflightCheckResult.pass("authorization"));
            LeaseHandle current = leaseService.requireCurrent(
                    change.getProductionTargetId(),
                    change.getCellId(),
                    change.getParameter(),
                    lease.holderId()
            );
            if (current.fencingToken() != lease.fencingToken()) {
                checks.add(PreflightCheckResult.deny("lease", ProductionReasonCode.PRODUCTION_FENCING_TOKEN_STALE, "fencing token changed"));
                return deny(change, actor, checks);
            }
            checks.add(PreflightCheckResult.pass("lease"));
            if (change.getRollbackDesiredValue() == null || change.getRollbackExpectedValue() == null) {
                checks.add(PreflightCheckResult.deny("rollback", ProductionReasonCode.PRODUCTION_PHASE14_PLAN_STALE, "rollback bindings missing"));
                return deny(change, actor, checks);
            }
            checks.add(PreflightCheckResult.pass("rollback"));
            auditService.append(
                    change.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_PREGRANT_PREFLIGHT_PASSED,
                    actor.actorPrincipalId(),
                    List.of(),
                    Map.of("checks", checks.size())
            );
            return checks;
        } catch (ProductionChangeException ex) {
            checks.add(PreflightCheckResult.deny("exception", ex.reasonCode(), ex.getMessage()));
            return deny(change, actor, checks);
        }
    }

    private void requireProposal(java.util.UUID proposalId, List<PreflightCheckResult> checks) {
        NetworkChangeProposalEntity proposal = proposalRepository.findById(proposalId).orElse(null);
        if (proposal == null) {
            checks.add(PreflightCheckResult.unknown("proposal", ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "proposal unavailable"));
            throw new ProductionChangeException(ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "proposal unavailable");
        }
        checks.add(PreflightCheckResult.pass("proposal"));
    }

    private void requireKnowledge(String sourceSystem, List<PreflightCheckResult> checks) {
        NetworkKnowledgeStatusEntity knowledge = knowledgeStatusRepository
                .findBySourceSystemAndSynchronizationScope(sourceSystem, "DEFAULT")
                .or(() -> knowledgeStatusRepository.findAll().stream().filter(k -> sourceSystem.equals(k.getSourceSystem())).findFirst())
                .orElse(null);
        if (knowledge == null) {
            checks.add(PreflightCheckResult.unknown("knowledge", ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "knowledge unavailable"));
            throw new ProductionChangeException(ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "knowledge unavailable");
        }
        KnowledgeGate.GateResult gate = knowledgeGate.evaluate(knowledge.getConfidence());
        if (!gate.allowsRecommendation() || "UNKNOWN".equals(knowledge.getFreshness()) || "UNKNOWN".equals(knowledge.getConfidence())) {
            checks.add(PreflightCheckResult.unknown("knowledge", ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "knowledge not trustworthy"));
            throw new ProductionChangeException(ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "knowledge not trustworthy");
        }
        checks.add(PreflightCheckResult.pass("knowledge"));
    }

    private void requireNoOpenDrift(String sourceSystem, String cellId, List<PreflightCheckResult> checks) {
        boolean open = driftObservationRepository
                .findBySourceSystemAndSynchronizationScopeAndDriftStatusOrderByDetectedAtDesc(sourceSystem, "DEFAULT", "OPEN")
                .stream()
                .anyMatch(drift -> cellId.equals(drift.getEntityId()));
        if (open) {
            checks.add(PreflightCheckResult.deny("drift", ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "unresolved drift"));
            throw new ProductionChangeException(ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "unresolved drift");
        }
        checks.add(PreflightCheckResult.pass("drift"));
    }

    private List<PreflightCheckResult> deny(
            ProductionNetworkChangeEntity change,
            ActorPrincipal actor,
            List<PreflightCheckResult> checks
    ) {
        PreflightCheckResult failing = checks.stream()
                .filter(check -> check.outcome() != PreflightOutcome.PASS)
                .findFirst()
                .orElse(PreflightCheckResult.unknown("preflight", ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED, "unknown"));
        ProductionReasonCode reason = failing.reasonCode() == null
                ? ProductionReasonCode.PRODUCTION_PREFLIGHT_DENIED
                : failing.reasonCode();
        failurePersistenceService.persist(
                change.getProductionChangeId(),
                ProductionChangeStatus.PREFLIGHT_DENIED,
                reason
        );
        change.setStatus(ProductionChangeStatus.PREFLIGHT_DENIED.name());
        change.setReasonCode(reason.name());
        change.setUpdatedAt(clock.instant());
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_PREGRANT_PREFLIGHT_DENIED,
                actor.actorPrincipalId(),
                List.of(reason.name()),
                Map.of("check", failing.checkName())
        );
        throw new ProductionChangeException(reason, "pre-grant preflight denied: " + failing.checkName());
    }
}
