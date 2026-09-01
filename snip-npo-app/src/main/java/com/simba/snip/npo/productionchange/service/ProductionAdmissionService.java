package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.policy.ProductionBlastRadiusPolicy;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionAdmissionService {

    private final ProductionChangeProperties properties;
    private final ProductionTargetRegistry targetRegistry;
    private final ProductionBindingAssembler bindingAssembler;
    private final ProductionFingerprintService fingerprintService;
    private final ProductionChangeControlService changeControlService;
    private final ProductionBlastRadiusPolicy blastRadiusPolicy;
    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final ProductionFailurePersistenceService failurePersistenceService;
    private final Clock clock;

    public ProductionAdmissionService(
            ProductionChangeProperties properties,
            ProductionTargetRegistry targetRegistry,
            ProductionBindingAssembler bindingAssembler,
            ProductionFingerprintService fingerprintService,
            ProductionChangeControlService changeControlService,
            ProductionBlastRadiusPolicy blastRadiusPolicy,
            ProductionNetworkChangeRepository changeRepository,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            ProductionFailurePersistenceService failurePersistenceService,
            Clock clock
    ) {
        this.properties = properties;
        this.targetRegistry = targetRegistry;
        this.bindingAssembler = bindingAssembler;
        this.fingerprintService = fingerprintService;
        this.changeControlService = changeControlService;
        this.blastRadiusPolicy = blastRadiusPolicy;
        this.changeRepository = changeRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.failurePersistenceService = failurePersistenceService;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkChangeEntity admit(
            UUID phase15ExecutionId,
            String productionTargetId,
            ProductionChangeControlService.ChangeControlReference changeControl,
            ActorPrincipal requester
    ) {
        if (!properties.isEnabled()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_DISABLED,
                    "production-change is disabled"
            );
        }
        metrics.incrementRequests("REQUESTED");
        Instant now = clock.instant();
        changeControlService.validate(changeControl, requester, now);
        ProductionBindingAssembler.UpstreamBinding binding = bindingAssembler.requireVerifiedPhase15(phase15ExecutionId);
        NetworkChangeExecutionEntity execution = binding.execution();
        NetworkChangePlanEntity plan = binding.plan();
        blastRadiusPolicy.requireSingleCellParameterOperation(1, 1, 1);
        blastRadiusPolicy.requireTxPower(binding.operation().getParameterName());
        ProductionNetworkTargetEntity target = targetRegistry.require(productionTargetId);
        try {
            bindingAssembler.requireTargetEligible(target);
        } catch (ProductionChangeException ex) {
            metrics.incrementRequests("ADMISSION_REJECTED");
            throw ex;
        }
        String fingerprint = fingerprintService.compute(bindingAssembler.fingerprintInput(
                null,
                target,
                binding,
                changeControl.reference(),
                0
        ));
        ProductionNetworkChangeEntity change = ProductionNetworkChangeEntity.createRequested(
                UUID.randomUUID(),
                execution.getId(),
                target.getTargetId(),
                changeControl.reference(),
                plan.getId(),
                plan.getFingerprint(),
                execution.getExecutionFingerprint(),
                binding.operation().getTargetEntityId(),
                binding.operation().getParameterName(),
                bindingAssembler.parseDecimal(binding.operation().getExpectedCurrentValue()),
                bindingAssembler.parseDecimal(binding.operation().getDesiredValue()),
                bindingAssembler.parseDecimal(binding.rollback().getExpectedCurrentValue()),
                bindingAssembler.parseDecimal(binding.rollback().getDesiredValue()),
                requester.actorPrincipalId(),
                fingerprint,
                now
        );
        change = changeRepository.save(change);
        changeControlService.persist(change, changeControl, requester);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_CHANGE_REQUESTED,
                requester.actorPrincipalId(),
                List.of(),
                Map.of("phase15ExecutionId", execution.getId().toString())
        );
        change.setStatus(ProductionChangeStatus.READY_FOR_REVIEW.name());
        change.setUpdatedAt(now);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_ADMISSION_PASSED,
                requester.actorPrincipalId(),
                List.of(),
                Map.of("status", ProductionChangeStatus.READY_FOR_REVIEW.name())
        );
        metrics.incrementRequests("ADMITTED");
        return change;
    }

    public void reject(ProductionNetworkChangeEntity change, ProductionReasonCode reasonCode, ActorPrincipal actor) {
        Instant now = clock.instant();
        failurePersistenceService.apply(change, ProductionChangeStatus.ADMISSION_REJECTED, reasonCode, now);
        auditService.append(
                change.getProductionChangeId(),
                ProductionAuditEventType.PRODUCTION_ADMISSION_REJECTED,
                actor.actorPrincipalId(),
                List.of(reasonCode.name()),
                Map.of()
        );
        metrics.incrementRequests("ADMISSION_REJECTED");
    }
}
