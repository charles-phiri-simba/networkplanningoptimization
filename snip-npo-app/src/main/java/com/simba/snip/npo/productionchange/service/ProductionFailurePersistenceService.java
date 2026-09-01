package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProductionFailurePersistenceService {

    private final ProductionNetworkChangeRepository changeRepository;
    private final Clock clock;

    public ProductionFailurePersistenceService(ProductionNetworkChangeRepository changeRepository, Clock clock) {
        this.changeRepository = changeRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(UUID productionChangeId, ProductionChangeStatus status, ProductionReasonCode reasonCode) {
        Instant now = clock.instant();
        changeRepository.findById(productionChangeId).ifPresent(change -> apply(change, status, reasonCode, now));
    }

    public void apply(
            ProductionNetworkChangeEntity change,
            ProductionChangeStatus status,
            ProductionReasonCode reasonCode,
            Instant now
    ) {
        change.setStatus(status.name());
        change.setReasonCode(reasonCode == null ? null : reasonCode.name());
        change.setUpdatedAt(now);
    }
}
