package com.simba.snip.npo.changeplanning.api;

import com.simba.snip.npo.changeplanning.persist.ExecutionReadinessAssessmentEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanAuditEventEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanOperationEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanPreconditionEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanReviewEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.repository.ExecutionReadinessAssessmentRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanOperationRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanPreconditionRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanReviewRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.changeplanning.service.ChangePlanAuditService;
import com.simba.snip.npo.domain.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChangePlanQueryService {

    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanOperationRepository operationRepository;
    private final NetworkChangePlanRollbackOperationRepository rollbackRepository;
    private final NetworkChangePlanPreconditionRepository preconditionRepository;
    private final ExecutionReadinessAssessmentRepository assessmentRepository;
    private final NetworkChangePlanReviewRepository reviewRepository;
    private final ChangePlanAuditService auditService;

    public ChangePlanQueryService(
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanOperationRepository operationRepository,
            NetworkChangePlanRollbackOperationRepository rollbackRepository,
            NetworkChangePlanPreconditionRepository preconditionRepository,
            ExecutionReadinessAssessmentRepository assessmentRepository,
            NetworkChangePlanReviewRepository reviewRepository,
            ChangePlanAuditService auditService
    ) {
        this.planRepository = planRepository;
        this.operationRepository = operationRepository;
        this.rollbackRepository = rollbackRepository;
        this.preconditionRepository = preconditionRepository;
        this.assessmentRepository = assessmentRepository;
        this.reviewRepository = reviewRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ChangePlanSummaryDto> list() {
        return planRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ChangePlanDetailDto require(UUID planId) {
        NetworkChangePlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        return toDetail(plan);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> evidence(UUID planId) {
        NetworkChangePlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new DomainNotFoundException("changePlan", planId.toString()));
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("planId", plan.getId().toString());
        evidence.put("proposalId", plan.getProposalId().toString());
        evidence.put("fingerprint", plan.getFingerprint());
        evidence.put("operations", operationRepository.findByPlanIdOrderBySequenceNumberAsc(planId).stream()
                .map(op -> Map.of(
                        "sequenceNumber", op.getSequenceNumber(),
                        "operationType", op.getOperationType(),
                        "expectedCurrentValue", op.getExpectedCurrentValue(),
                        "desiredValue", op.getDesiredValue()
                )).toList());
        evidence.put("rollbackOperations", rollbackRepository.findByPlanIdOrderBySequenceNumberAsc(planId).stream()
                .map(op -> Map.of(
                        "sequenceNumber", op.getSequenceNumber(),
                        "expectedCurrentValue", op.getExpectedCurrentValue(),
                        "desiredValue", op.getDesiredValue()
                )).toList());
        evidence.put("preconditions", preconditionRepository.findByPlanIdOrderBySequenceNumberAsc(planId));
        evidence.put("reviews", reviewRepository.findByPlanIdOrderByReviewedAtAsc(planId).stream()
                .map(r -> Map.of("reviewer", r.getReviewer(), "reviewedAt", r.getReviewedAt().toString()))
                .toList());
        evidence.put("readinessAssessments", assessmentRepository.findByPlanIdOrderByAssessedAtAsc(planId));
        evidence.put("auditEvents", auditService.list(planId).stream()
                .map(this::auditMap)
                .toList());
        return evidence;
    }

    private ChangePlanDetailDto toDetail(NetworkChangePlanEntity plan) {
        UUID planId = plan.getId();
        List<ChangePlanOperationDto> operations = operationRepository.findByPlanIdOrderBySequenceNumberAsc(planId)
                .stream().map(this::toOperationDto).toList();
        List<ChangePlanRollbackDto> rollbacks = rollbackRepository.findByPlanIdOrderBySequenceNumberAsc(planId)
                .stream().map(this::toRollbackDto).toList();
        List<ChangePlanPreconditionDto> preconditions = preconditionRepository.findByPlanIdOrderBySequenceNumberAsc(planId)
                .stream().map(this::toPreconditionDto).toList();
        List<ChangePlanReadinessDto> assessments = assessmentRepository.findByPlanIdOrderByAssessedAtAsc(planId)
                .stream().map(this::toReadinessDto).toList();
        return new ChangePlanDetailDto(
                toSummary(plan),
                plan.getFingerprint(),
                plan.getAuthorizedFingerprint(),
                plan.getKnowledgeConfidenceAtCreation(),
                plan.getRiskLevel(),
                plan.getReviewedBy(),
                plan.getReviewedAt(),
                plan.getAuthorizedBy(),
                plan.getAuthorizedAt(),
                plan.getCancelledBy(),
                plan.getCancelledAt(),
                plan.getInvalidationReason(),
                plan.getInvalidatedAt(),
                operations,
                rollbacks,
                preconditions,
                assessments
        );
    }

    private ChangePlanSummaryDto toSummary(NetworkChangePlanEntity plan) {
        return new ChangePlanSummaryDto(
                plan.getId(),
                plan.getProposalId(),
                plan.getStatus(),
                plan.getTargetEntityType(),
                plan.getTargetEntityId(),
                plan.getParameterName(),
                plan.getExpectedCurrentValue(),
                plan.getDesiredValue(),
                plan.getImpactLevel(),
                plan.getCreatedAt(),
                plan.getExpiresAt()
        );
    }

    private ChangePlanOperationDto toOperationDto(NetworkChangePlanOperationEntity op) {
        return new ChangePlanOperationDto(
                op.getSequenceNumber(),
                op.getOperationType(),
                op.getTargetEntityType(),
                op.getTargetEntityId(),
                op.getParameterName(),
                op.getExpectedCurrentValue(),
                op.getDesiredValue()
        );
    }

    private ChangePlanRollbackDto toRollbackDto(NetworkChangePlanRollbackOperationEntity op) {
        return new ChangePlanRollbackDto(
                op.getSequenceNumber(),
                op.getOperationType(),
                op.getTargetEntityType(),
                op.getTargetEntityId(),
                op.getParameterName(),
                op.getExpectedCurrentValue(),
                op.getDesiredValue()
        );
    }

    private ChangePlanPreconditionDto toPreconditionDto(NetworkChangePlanPreconditionEntity p) {
        return new ChangePlanPreconditionDto(
                p.getPreconditionType(),
                p.getExpectedCondition(),
                p.getObservedValue(),
                p.getResult(),
                p.getReasonCode(),
                p.getCheckedAt()
        );
    }

    private ChangePlanReadinessDto toReadinessDto(ExecutionReadinessAssessmentEntity a) {
        return new ChangePlanReadinessDto(
                a.getAssessedAt(),
                a.getResult(),
                a.getAssessedFingerprint(),
                a.getReasonCodes()
        );
    }

    private Map<String, String> auditMap(NetworkChangePlanAuditEventEntity event) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("eventType", event.getEventType());
        map.put("actor", event.getActor());
        map.put("occurredAt", event.getOccurredAt().toString());
        if (event.getDetails() != null) {
            map.put("details", event.getDetails());
        }
        return map;
    }
}
