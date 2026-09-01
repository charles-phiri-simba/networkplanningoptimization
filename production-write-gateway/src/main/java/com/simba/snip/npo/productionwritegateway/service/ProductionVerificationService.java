package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.adapter.ObservationStatus;
import com.simba.snip.npo.productionwritegateway.adapter.PostMutationObservation;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionVerificationEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionwritegateway.repository.ProductionExecutionVerificationRepository;
import com.simba.snip.npo.productionwritegateway.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProductionVerificationService {

    private final ExpectedStateObservationService observationService;
    private final ProductionExecutionVerificationRepository verificationRepository;
    private final ProductionNetworkChangeRepository changeRepository;

    public ProductionVerificationService(
            ExpectedStateObservationService observationService,
            ProductionExecutionVerificationRepository verificationRepository,
            ProductionNetworkChangeRepository changeRepository
    ) {
        this.observationService = observationService;
        this.verificationRepository = verificationRepository;
        this.changeRepository = changeRepository;
    }

    public VerificationDecision verify(ProductionNetworkChangeEntity change, GrantType grantType) {
        BigDecimal expected = grantType == GrantType.ROLLBACK
                ? change.getRollbackExpectedValue()
                : change.getExpectedValue();
        BigDecimal desired = grantType == GrantType.ROLLBACK
                ? change.getRollbackDesiredValue()
                : change.getDesiredValue();
        PostMutationObservation observation = observationService.observeDesired(change, grantType);
        if (observation.status() == ObservationStatus.TIMEOUT
                || observation.status() == ObservationStatus.SOURCE_UNAVAILABLE
                || observation.status() == ObservationStatus.UNKNOWN
                || observation.status() == ObservationStatus.STALE
                || observation.observedValue() == null) {
            return new VerificationDecision(
                    GatewayAttemptStatus.RECOVERY_REQUIRED,
                    ProductionChangeStatus.PRODUCTION_OUTCOME_UNRESOLVED,
                    ProductionReasonCode.PRODUCTION_OUTCOME_UNRESOLVED,
                    observation
            );
        }
        BigDecimal observed = observation.observedValue();
        if (desired != null && observed.compareTo(desired) == 0) {
            return new VerificationDecision(
                    GatewayAttemptStatus.VERIFIED,
                    ProductionChangeStatus.NETWORK_SYNCHRONIZATION_REQUIRED,
                    null,
                    observation
            );
        }
        if (expected != null && observed.compareTo(expected) == 0) {
            return new VerificationDecision(
                    GatewayAttemptStatus.RECOVERY_REQUIRED,
                    ProductionChangeStatus.RECOVERY_REQUIRED,
                    ProductionReasonCode.PRODUCTION_VERIFICATION_MISMATCH,
                    observation
            );
        }
        return new VerificationDecision(
                GatewayAttemptStatus.MANUAL_INTERVENTION_REQUIRED,
                ProductionChangeStatus.MANUAL_INTERVENTION_REQUIRED,
                ProductionReasonCode.PRODUCTION_MANUAL_INTERVENTION_REQUIRED,
                observation
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(
            UUID productionChangeId,
            UUID attemptId,
            VerificationDecision decision,
            BigDecimal desiredValue
    ) {
        ProductionExecutionVerificationEntity entity = new ProductionExecutionVerificationEntity();
        entity.setVerificationId(UUID.randomUUID());
        entity.setProductionChangeId(productionChangeId);
        entity.setAttemptId(attemptId);
        entity.setResult(decision.attemptStatus().name());
        entity.setObservedValue(
                decision.observation() == null ? null : decision.observation().observedValue());
        entity.setDesiredValue(desiredValue);
        entity.setVerifiedAt(Instant.now());
        verificationRepository.saveAndFlush(entity);

        ProductionNetworkChangeEntity change = changeRepository.findById(productionChangeId).orElseThrow();
        change.setStatus(decision.productionChangeStatus().name());
        change.setReasonCode(decision.reasonCode() == null ? null : decision.reasonCode().name());
        change.setUpdatedAt(Instant.now());
        change.setVersion(change.getVersion() + 1);
        changeRepository.saveAndFlush(change);
    }

    public record VerificationDecision(
            GatewayAttemptStatus attemptStatus,
            ProductionChangeStatus productionChangeStatus,
            ProductionReasonCode reasonCode,
            PostMutationObservation observation
    ) {
    }
}
