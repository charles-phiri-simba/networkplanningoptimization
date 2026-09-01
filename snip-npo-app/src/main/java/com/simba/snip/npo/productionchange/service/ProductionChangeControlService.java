package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ChangeControlSystem;
import com.simba.snip.npo.productionchange.entity.ProductionChangeControlEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.policy.ProductionSeparationOfDutiesPolicy;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionChangeControlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProductionChangeControlService {

    public record ChangeControlReference(
            String system,
            String reference,
            String status,
            String validatedByPrincipalId,
            Instant validatedAt,
            Instant validUntil
    ) {
    }

    private final ProductionChangeControlRepository controlRepository;
    private final ProductionSeparationOfDutiesPolicy sodPolicy;
    private final Clock clock;

    public ProductionChangeControlService(
            ProductionChangeControlRepository controlRepository,
            ProductionSeparationOfDutiesPolicy sodPolicy,
            Clock clock
    ) {
        this.controlRepository = controlRepository;
        this.sodPolicy = sodPolicy;
        this.clock = clock;
    }

    @Transactional
    public ProductionChangeControlEntity persist(
            ProductionNetworkChangeEntity change,
            ChangeControlReference reference,
            ActorPrincipal requester
    ) {
        validate(reference, requester, clock.instant());
        ProductionChangeControlEntity entity = ProductionChangeControlEntity.create(
                UUID.randomUUID(),
                change.getProductionChangeId(),
                reference.system(),
                reference.reference(),
                reference.status(),
                reference.validatedByPrincipalId(),
                reference.validatedAt(),
                reference.validUntil()
        );
        return controlRepository.save(entity);
    }

    public void validate(ChangeControlReference reference, ActorPrincipal requester, Instant now) {
        if (reference == null
                || reference.system() == null
                || reference.reference() == null
                || reference.reference().isBlank()
                || reference.status() == null
                || reference.status().isBlank()
                || reference.validatedByPrincipalId() == null
                || reference.validatedByPrincipalId().isBlank()
                || reference.validatedAt() == null) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID,
                    "change-control reference is required"
            );
        }
        if (!ChangeControlSystem.MANUAL.name().equals(reference.system())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID,
                    "only MANUAL change-control is supported"
            );
        }
        sodPolicy.changeControlValidatorMustNotBeRequester(reference.validatedByPrincipalId(), requester.actorPrincipalId());
        if (reference.validUntil() != null && !reference.validUntil().isAfter(now)) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_EXPIRED,
                    "change-control reference has expired"
            );
        }
        if (!"VALIDATED".equalsIgnoreCase(reference.status()) && !"VALID".equalsIgnoreCase(reference.status())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID,
                    "change-control status is not validated"
            );
        }
    }

    public ProductionChangeControlEntity requireCurrent(UUID productionChangeId, Instant now) {
        ProductionChangeControlEntity control = controlRepository
                .findFirstByProductionChangeIdOrderByValidatedAtDesc(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID,
                        "change-control reference is missing"
                ));
        if (control.getValidUntil() != null && !control.getValidUntil().isAfter(now)) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_EXPIRED,
                    "change-control reference has expired"
            );
        }
        if (!ChangeControlSystem.MANUAL.name().equals(control.getSystem())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_CHANGE_CONTROL_INVALID,
                    "only MANUAL change-control is supported"
            );
        }
        return control;
    }
}
