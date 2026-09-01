package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.LeaseHandle;
import com.simba.snip.npo.productionchange.domain.LeaseStatus;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionLeaseEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionLeaseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductionLeaseService {

    private final ProductionExecutionLeaseRepository leaseRepository;
    private final ProductionChangeProperties properties;
    private final ProductionChangeAuditService auditService;
    private final Clock clock;

    public ProductionLeaseService(
            ProductionExecutionLeaseRepository leaseRepository,
            ProductionChangeProperties properties,
            ProductionChangeAuditService auditService,
            Clock clock
    ) {
        this.leaseRepository = leaseRepository;
        this.properties = properties;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LeaseHandle acquire(
            String productionTargetId,
            String cellId,
            String parameter,
            String holderId,
            UUID productionChangeId,
            String actorPrincipalId
    ) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.getLeaseDuration());
        Optional<ProductionExecutionLeaseEntity> existing = leaseRepository
                .findByProductionTargetIdAndCellIdAndParameterAndStatus(
                        productionTargetId, cellId, parameter, LeaseStatus.ACTIVE.name()
                );
        if (existing.isPresent()) {
            ProductionExecutionLeaseEntity lease = existing.get();
            if (lease.getExpiresAt().isAfter(now) && !holderId.equals(lease.getHolderId())) {
                auditService.append(
                        productionChangeId,
                        ProductionAuditEventType.PRODUCTION_LEASE_CONFLICT,
                        actorPrincipalId,
                        List.of(ProductionReasonCode.PRODUCTION_LEASE_CONFLICT.name()),
                        Map.of("cellIdOmitted", true)
                );
                throw new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_LEASE_CONFLICT,
                        "active production lease exists for target/cell/parameter"
                );
            }
            if (holderId.equals(lease.getHolderId()) && lease.getExpiresAt().isAfter(now)) {
                return toHandle(lease);
            }
            lease.setStatus(LeaseStatus.EXPIRED.name());
        }
        try {
            ProductionExecutionLeaseEntity created = leaseRepository.save(ProductionExecutionLeaseEntity.create(
                    UUID.randomUUID(),
                    productionTargetId,
                    cellId,
                    parameter,
                    holderId,
                    nextFencingToken(existing),
                    LeaseStatus.ACTIVE.name(),
                    now,
                    expiresAt
            ));
            auditService.append(
                    productionChangeId,
                    ProductionAuditEventType.PRODUCTION_LEASE_ACQUIRED,
                    actorPrincipalId,
                    List.of(),
                    Map.of("fencingToken", created.getFencingToken())
            );
            return toHandle(created);
        } catch (DataIntegrityViolationException ex) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_LEASE_UNAVAILABLE,
                    "production lease could not be acquired",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public LeaseHandle requireCurrent(String productionTargetId, String cellId, String parameter, String holderId) {
        ProductionExecutionLeaseEntity lease = leaseRepository
                .findByProductionTargetIdAndCellIdAndParameterAndStatus(
                        productionTargetId, cellId, parameter, LeaseStatus.ACTIVE.name()
                )
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_LEASE_REQUIRED,
                        "production lease is required"
                ));
        Instant now = clock.instant();
        if (!lease.getExpiresAt().isAfter(now)) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_LEASE_UNAVAILABLE,
                    "production lease has expired"
            );
        }
        if (!holderId.equals(lease.getHolderId())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_FENCING_MISMATCH,
                    "lease holder mismatch"
            );
        }
        return toHandle(lease);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String productionTargetId, String cellId, String parameter, String holderId, UUID productionChangeId, String actorPrincipalId) {
        leaseRepository.findByProductionTargetIdAndCellIdAndParameterAndStatus(
                productionTargetId, cellId, parameter, LeaseStatus.ACTIVE.name()
        ).ifPresent(lease -> {
            if (holderId.equals(lease.getHolderId())) {
                lease.setStatus(LeaseStatus.RELEASED.name());
                auditService.append(
                        productionChangeId,
                        ProductionAuditEventType.PRODUCTION_LEASE_RELEASED,
                        actorPrincipalId,
                        List.of(),
                        Map.of("fencingToken", lease.getFencingToken())
                );
            }
        });
    }

    private long nextFencingToken(Optional<ProductionExecutionLeaseEntity> existing) {
        return existing.map(lease -> lease.getFencingToken() + 1).orElse(1L);
    }

    private LeaseHandle toHandle(ProductionExecutionLeaseEntity lease) {
        return new LeaseHandle(
                lease.getLeaseId(),
                lease.getProductionTargetId(),
                lease.getCellId(),
                lease.getParameter(),
                lease.getHolderId(),
                lease.getFencingToken(),
                lease.getAcquiredAt(),
                lease.getExpiresAt()
        );
    }
}
