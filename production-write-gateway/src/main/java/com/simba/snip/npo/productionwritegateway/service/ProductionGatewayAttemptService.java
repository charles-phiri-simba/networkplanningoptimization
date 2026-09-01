package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.GatewayAttemptStatus;
import com.simba.snip.npo.productionchange.protocol.MutationOutcome;
import com.simba.snip.npo.productionchange.protocol.SendPhase;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionwritegateway.entity.ProductionGatewayAttemptEntity;
import com.simba.snip.npo.productionwritegateway.repository.ProductionGatewayAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProductionGatewayAttemptService {

    private final ProductionGatewayAttemptRepository attemptRepository;
    private final ProductionChangeGatewayProperties properties;

    public ProductionGatewayAttemptService(
            ProductionGatewayAttemptRepository attemptRepository,
            ProductionChangeGatewayProperties properties
    ) {
        this.attemptRepository = attemptRepository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionGatewayAttemptEntity insertPreSend(ProductionExecutionGrantEntity consumedGrant) {
        if (!"CONSUMED".equals(consumedGrant.getStatus())) {
            throw new IllegalStateException("attempt insert requires CONSUMED grant");
        }
        ProductionGatewayAttemptEntity attempt = new ProductionGatewayAttemptEntity();
        attempt.setAttemptId(UUID.randomUUID());
        attempt.setGrantId(consumedGrant.getGrantId());
        attempt.setProductionChangeId(consumedGrant.getProductionChangeId());
        attempt.setProductionTargetId(consumedGrant.getTargetId());
        attempt.setStatus(GatewayAttemptStatus.PRE_SEND.name());
        attempt.setSendPhase(SendPhase.PRE_SEND.name());
        attempt.setMutationOutcome(MutationOutcome.NOT_SENT.name());
        attempt.setOperationBindingHash(consumedGrant.getOperationBindingHash());
        attempt.setFencingToken(consumedGrant.getFencingToken());
        attempt.setProductionFingerprint(consumedGrant.getProductionFingerprint());
        attempt.setGatewayInstanceId(properties.getGateway().getInstanceId());
        attempt.setStartedAt(Instant.now());
        attempt.setVersion(0);
        return attemptRepository.saveAndFlush(attempt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductionGatewayAttemptEntity updateStatus(
            UUID attemptId,
            GatewayAttemptStatus status,
            SendPhase sendPhase,
            MutationOutcome mutationOutcome
    ) {
        ProductionGatewayAttemptEntity attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.setStatus(status.name());
        if (sendPhase != null) {
            attempt.setSendPhase(sendPhase.name());
        }
        if (mutationOutcome != null) {
            attempt.setMutationOutcome(mutationOutcome.name());
        }
        if (status == GatewayAttemptStatus.VERIFIED
                || status == GatewayAttemptStatus.VENDOR_REJECTED
                || status == GatewayAttemptStatus.VERIFICATION_FAILED
                || status == GatewayAttemptStatus.RECOVERY_REQUIRED
                || status == GatewayAttemptStatus.MANUAL_INTERVENTION_REQUIRED) {
            attempt.setCompletedAt(Instant.now());
        }
        attempt.setVersion(attempt.getVersion() + 1);
        return attemptRepository.saveAndFlush(attempt);
    }

    public long countByGrantId(UUID grantId) {
        return attemptRepository.countByGrantId(grantId);
    }
}
