package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionEntity;
import com.simba.snip.npo.changeexecution.entity.NetworkChangeExecutionOperationEntity;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionOperationRepository;
import com.simba.snip.npo.changeexecution.repository.NetworkChangeExecutionRepository;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanRollbackOperationEntity;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRepository;
import com.simba.snip.npo.changeplanning.repository.NetworkChangePlanRollbackOperationRepository;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.CertificationLevel;
import com.simba.snip.npo.productionchange.domain.ProductionFingerprintInput;
import com.simba.snip.npo.productionchange.domain.ProductionTargetState;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class ProductionBindingAssembler {

    private final NetworkChangeExecutionRepository executionRepository;
    private final NetworkChangeExecutionOperationRepository operationRepository;
    private final NetworkChangePlanRepository planRepository;
    private final NetworkChangePlanRollbackOperationRepository rollbackRepository;
    private final ProductionChangeProperties properties;

    public ProductionBindingAssembler(
            NetworkChangeExecutionRepository executionRepository,
            NetworkChangeExecutionOperationRepository operationRepository,
            NetworkChangePlanRepository planRepository,
            NetworkChangePlanRollbackOperationRepository rollbackRepository,
            ProductionChangeProperties properties
    ) {
        this.executionRepository = executionRepository;
        this.operationRepository = operationRepository;
        this.planRepository = planRepository;
        this.rollbackRepository = rollbackRepository;
        this.properties = properties;
    }

    public record UpstreamBinding(
            NetworkChangeExecutionEntity execution,
            NetworkChangePlanEntity plan,
            NetworkChangeExecutionOperationEntity operation,
            NetworkChangePlanRollbackOperationEntity rollback
    ) {
    }

    public UpstreamBinding requireVerifiedPhase15(java.util.UUID phase15ExecutionId) {
        NetworkChangeExecutionEntity execution = executionRepository.findById(phase15ExecutionId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_PHASE15_EXECUTION_INELIGIBLE,
                        "phase 15 execution not found"
                ));
        if (!"VERIFIED".equals(execution.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_PHASE15_EXECUTION_NOT_VERIFIED,
                    "phase 15 execution must be VERIFIED"
            );
        }
        NetworkChangePlanEntity plan = planRepository.findById(execution.getPlanId())
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_PHASE14_PLAN_STALE,
                        "phase 14 plan not found"
                ));
        if ("INVALIDATED".equals(plan.getStatus())
                || "EXPIRED".equals(plan.getStatus())
                || "CANCELLED".equals(plan.getStatus())
                || "SUPERSEDED".equals(plan.getStatus())
                || "INVALID".equals(plan.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_PHASE14_PLAN_STALE,
                    "phase 14 plan is not current"
            );
        }
        if (!execution.getPlanFingerprint().equals(plan.getFingerprint())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_PHASE14_PLAN_STALE,
                    "phase 14 plan fingerprint is stale relative to phase 15"
            );
        }
        List<NetworkChangeExecutionOperationEntity> operations =
                operationRepository.findByExecutionIdOrderBySequenceNumberAsc(execution.getId());
        if (operations.size() != 1) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_SCOPE_DENIED,
                    "exactly one governed operation is required"
            );
        }
        NetworkChangePlanRollbackOperationEntity rollback = rollbackRepository
                .findByPlanIdOrderBySequenceNumberAsc(plan.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_PHASE14_PLAN_STALE,
                        "phase 14 rollback operation is required"
                ));
        return new UpstreamBinding(execution, plan, operations.get(0), rollback);
    }

    public ProductionFingerprintInput fingerprintInput(
            ProductionNetworkChangeEntity change,
            ProductionNetworkTargetEntity target,
            UpstreamBinding binding,
            String changeControlReference,
            int authorizationGeneration
    ) {
        NetworkChangeExecutionEntity execution = binding.execution();
        NetworkChangePlanEntity plan = binding.plan();
        NetworkChangeExecutionOperationEntity operation = binding.operation();
        NetworkChangePlanRollbackOperationEntity rollback = binding.rollback();
        return new ProductionFingerprintInput(
                target.getAdapterProfileId(),
                authorizationGeneration,
                target.getCapabilityProfileVersion(),
                operation.getTargetEntityId(),
                changeControlReference,
                execution.getExecutionWindowClosesAt(),
                execution.getId().toString(),
                execution.getExecutionWindowOpensAt(),
                target.getCredentialProfileId(),
                parseDecimal(operation.getDesiredValue()),
                target.getEnvironment(),
                parseDecimal(operation.getExpectedCurrentValue()),
                operation.getParameterName(),
                plan.getFingerprint(),
                plan.getId(),
                plan.getPlanVersion(),
                execution.getExecutionFingerprint(),
                execution.getId(),
                target.getPlatform(),
                properties.getProductionPolicyVersion(),
                target.getTargetId(),
                parseDecimal(rollback.getDesiredValue()),
                parseDecimal(rollback.getExpectedCurrentValue()),
                target.getRollbackPolicy(),
                target.getSecurityProfileId(),
                target.getVendor(),
                target.getVerificationPolicy()
        );
    }

    public void requireTargetEligible(ProductionNetworkTargetEntity target) {
        if (!target.isEnabled()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_TARGET_NOT_ACTIVE,
                    "production target is not enabled"
            );
        }
        ProductionTargetState state = ProductionTargetState.valueOf(target.getTargetState());
        if (state == ProductionTargetState.SUSPENDED) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_TARGET_SUSPENDED,
                    "production target is suspended"
            );
        }
        if (state == ProductionTargetState.DISABLED) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_TARGET_DISABLED,
                    "production target is disabled"
            );
        }
        if (state != ProductionTargetState.ACTIVE) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_TARGET_NOT_ACTIVE,
                    "production target is not ACTIVE"
            );
        }
        if (!properties.getPermittedVendors().contains(target.getVendor())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_VENDOR_UNSUPPORTED,
                    "vendor is not permitted"
            );
        }
        if (!properties.getPermittedPlatforms().contains(target.getPlatform())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_VENDOR_UNSUPPORTED,
                    "platform is not permitted"
            );
        }
        CertificationLevel level = CertificationLevel.valueOf(target.getCertificationLevel());
        if (!level.meets(properties.getMinimumCertificationLevelForExecution())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_TARGET_CERTIFICATION_INSUFFICIENT,
                    "target certification level is insufficient"
            );
        }
    }

    public BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value.strip());
        } catch (RuntimeException ex) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                    "numeric binding is invalid"
            );
        }
    }
}
