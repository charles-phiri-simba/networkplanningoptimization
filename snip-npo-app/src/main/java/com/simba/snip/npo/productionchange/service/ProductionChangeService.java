package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ReviewDecision;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayAttemptEntity;
import com.simba.snip.npo.productionchange.entity.ProductionGatewayEvidenceEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionGatewayAttemptRepository;
import com.simba.snip.npo.productionchange.repository.ProductionGatewayEvidenceRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionChangeService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionAdmissionService admissionService;
    private final ProductionReviewService reviewService;
    private final ProductionAuthorizationService authorizationService;
    private final ProductionExecutionOrchestrationService executionOrchestrationService;
    private final ProductionRollbackRequestService rollbackRequestService;
    private final ProductionRollbackReviewService rollbackReviewService;
    private final ProductionRollbackAuthorizationService rollbackAuthorizationService;
    private final ProductionRollbackOrchestrationService rollbackOrchestrationService;
    private final ProductionGatewayAttemptRepository attemptRepository;
    private final ProductionGatewayEvidenceRepository evidenceRepository;

    public ProductionChangeService(
            ProductionNetworkChangeRepository changeRepository,
            ProductionAdmissionService admissionService,
            ProductionReviewService reviewService,
            ProductionAuthorizationService authorizationService,
            ProductionExecutionOrchestrationService executionOrchestrationService,
            ProductionRollbackRequestService rollbackRequestService,
            ProductionRollbackReviewService rollbackReviewService,
            ProductionRollbackAuthorizationService rollbackAuthorizationService,
            ProductionRollbackOrchestrationService rollbackOrchestrationService,
            ProductionGatewayAttemptRepository attemptRepository,
            ProductionGatewayEvidenceRepository evidenceRepository
    ) {
        this.changeRepository = changeRepository;
        this.admissionService = admissionService;
        this.reviewService = reviewService;
        this.authorizationService = authorizationService;
        this.executionOrchestrationService = executionOrchestrationService;
        this.rollbackRequestService = rollbackRequestService;
        this.rollbackReviewService = rollbackReviewService;
        this.rollbackAuthorizationService = rollbackAuthorizationService;
        this.rollbackOrchestrationService = rollbackOrchestrationService;
        this.attemptRepository = attemptRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional
    public ProductionNetworkChangeEntity create(
            UUID phase15ExecutionId,
            String productionTargetId,
            ProductionChangeControlService.ChangeControlReference changeControl,
            ActorPrincipal requester
    ) {
        return admissionService.admit(phase15ExecutionId, productionTargetId, changeControl, requester);
    }

    @Transactional(readOnly = true)
    public List<ProductionNetworkChangeEntity> list() {
        return changeRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public ProductionNetworkChangeEntity require(UUID productionChangeId) {
        return changeRepository.findById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
    }

    public ProductionNetworkChangeEntity review(
            UUID productionChangeId,
            ReviewDecision decision,
            List<String> reasonCodes,
            ActorPrincipal reviewer
    ) {
        return reviewService.review(productionChangeId, decision, reasonCodes, reviewer);
    }

    public ProductionNetworkChangeEntity authorize(UUID productionChangeId, ActorPrincipal authorizer) {
        return authorizationService.authorize(productionChangeId, authorizer);
    }

    public ProductionNetworkChangeEntity execute(UUID productionChangeId, ActorPrincipal executor) {
        return executionOrchestrationService.execute(productionChangeId, executor);
    }

    public ProductionNetworkChangeEntity requestRollback(UUID productionChangeId, ActorPrincipal requester) {
        return rollbackRequestService.request(productionChangeId, requester);
    }

    public ProductionNetworkChangeEntity reviewRollback(
            UUID productionChangeId,
            ReviewDecision decision,
            ActorPrincipal reviewer
    ) {
        return rollbackReviewService.review(productionChangeId, decision, reviewer);
    }

    public ProductionNetworkChangeEntity authorizeRollback(UUID productionChangeId, ActorPrincipal authorizer) {
        return rollbackAuthorizationService.authorize(productionChangeId, authorizer);
    }

    public ProductionNetworkChangeEntity executeRollback(UUID productionChangeId, ActorPrincipal executor) {
        return rollbackOrchestrationService.execute(productionChangeId, executor);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> evidence(UUID productionChangeId) {
        require(productionChangeId);
        Map<String, Object> body = new LinkedHashMap<>();
        List<ProductionGatewayAttemptEntity> attempts =
                attemptRepository.findByProductionChangeIdOrderByStartedAtAsc(productionChangeId);
        body.put("attempts", attempts.stream().map(attempt -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("attemptId", attempt.getAttemptId());
            row.put("status", attempt.getStatus());
            row.put("sendPhase", attempt.getSendPhase());
            row.put("mutationOutcome", attempt.getMutationOutcome());
            List<ProductionGatewayEvidenceEntity> evidence =
                    evidenceRepository.findByAttemptIdOrderByProducedAtAsc(attempt.getAttemptId());
            row.put("evidence", evidence.stream().map(item -> Map.of(
                    "evidenceType", item.getEvidenceType(),
                    "producer", item.getProducer(),
                    "producedAt", item.getProducedAt().toString()
            )).toList());
            return row;
        }).toList());
        return body;
    }
}
