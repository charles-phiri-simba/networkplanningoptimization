package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.GrantStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionGrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ProductionGrantExpiryService {

    private final ProductionExecutionGrantRepository grantRepository;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionGrantExpiryService(
            ProductionExecutionGrantRepository grantRepository,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.grantRepository = grantRepository;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int expireDueGrants() {
        Instant now = clock.instant();
        List<ProductionExecutionGrantEntity> expired =
                grantRepository.findByStatusAndExpiresAtBefore(GrantStatus.ISSUED.name(), now);
        for (ProductionExecutionGrantEntity grant : expired) {
            grant.setStatus(GrantStatus.EXPIRED.name());
            metrics.incrementGrantExpired();
            auditService.append(
                    grant.getProductionChangeId(),
                    ProductionAuditEventType.PRODUCTION_GRANT_EXPIRED,
                    "system",
                    List.of(ProductionReasonCode.PRODUCTION_GRANT_EXPIRED.name()),
                    Map.of("grantType", grant.getGrantType())
            );
        }
        return expired.size();
    }
}
