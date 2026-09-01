package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.adapter.ProductionWriteGatewayClient;
import com.simba.snip.npo.productionchange.audit.ProductionChangeAuditService;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.LeaseHandle;
import com.simba.snip.npo.productionchange.domain.ProductionAuditEventType;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.policy.ProductionSeparationOfDutiesPolicy;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteRequest;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductionExecutionOrchestrationService {

    private final ProductionChangeProperties properties;
    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionSeparationOfDutiesPolicy sodPolicy;
    private final ProductionPreGrantPreflightService preflightService;
    private final ProductionLeaseService leaseService;
    private final ProductionExecutionGrantService grantService;
    private final ProductionWriteGatewayClient gatewayClient;
    private final ProductionExecutionSyncService syncService;
    private final ProductionFailurePersistenceService failurePersistenceService;
    private final ProductionChangeAuditService auditService;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionExecutionOrchestrationService(
            ProductionChangeProperties properties,
            ProductionNetworkChangeRepository changeRepository,
            ProductionSeparationOfDutiesPolicy sodPolicy,
            ProductionPreGrantPreflightService preflightService,
            ProductionLeaseService leaseService,
            ProductionExecutionGrantService grantService,
            ProductionWriteGatewayClient gatewayClient,
            ProductionExecutionSyncService syncService,
            ProductionFailurePersistenceService failurePersistenceService,
            ProductionChangeAuditService auditService,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.properties = properties;
        this.changeRepository = changeRepository;
        this.sodPolicy = sodPolicy;
        this.preflightService = preflightService;
        this.leaseService = leaseService;
        this.grantService = grantService;
        this.gatewayClient = gatewayClient;
        this.syncService = syncService;
        this.failurePersistenceService = failurePersistenceService;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    public ProductionNetworkChangeEntity execute(UUID productionChangeId, ActorPrincipal executor) {
        long started = System.nanoTime();
        ProductionNetworkChangeEntity change = changeRepository.findById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
        auditService.requireMutable(change);
        if (!properties.isEnabled() || !properties.isGlobalExecutionEnabled()) {
            metrics.incrementKillSwitchDenials();
            failurePersistenceService.persist(
                    productionChangeId,
                    ProductionChangeStatus.EXECUTE_DENIED,
                    ProductionReasonCode.PRODUCTION_KILL_SWITCH_DENY
            );
            auditService.append(
                    productionChangeId,
                    ProductionAuditEventType.PRODUCTION_KILL_SWITCH_DENY,
                    executor.actorPrincipalId(),
                    List.of(ProductionReasonCode.PRODUCTION_KILL_SWITCH_DENY.name()),
                    Map.of()
            );
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_KILL_SWITCH_DENY,
                    "global execution is disabled"
            );
        }
        if (ProductionChangeStatus.VERIFIED.name().equals(change.getStatus())) {
            return change;
        }
        if (!ProductionChangeStatus.AUTHORIZED.name().equals(change.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_AUTHORIZATION_MISSING,
                    "production change is not AUTHORIZED"
            );
        }
        sodPolicy.authorizerMustNotExecute(change.getAuthorizerPrincipalId(), executor.actorPrincipalId());
        change.setExecutorPrincipalId(executor.actorPrincipalId());
        change.setUpdatedAt(clock.instant());
        change = changeRepository.saveAndFlush(change);
        String holderId = change.getProductionChangeId().toString();
        LeaseHandle lease = leaseService.acquire(
                change.getProductionTargetId(),
                change.getCellId(),
                change.getParameter(),
                holderId,
                change.getProductionChangeId(),
                executor.actorPrincipalId()
        );
        try {
            preflightService.evaluate(change, lease, executor);
            ProductionExecutionGrantEntity grant = grantService.issue(change, lease, GrantType.FORWARD, executor);
            GatewayExecuteResponse response;
            try {
                response = gatewayClient.execute(new GatewayExecuteRequest(
                        grant.getGrantId(),
                        change.getProductionChangeId(),
                        change.getProductionChangeId().toString()
                ));
            } catch (ProductionChangeException ex) {
                metrics.incrementAttempts("GATEWAY_UNAVAILABLE");
                throw ex;
            } finally {
                metrics.recordExecuteDuration(System.nanoTime() - started);
            }
            return syncService.syncFromDurableEvidence(change.getProductionChangeId(), response, executor);
        } finally {
            leaseService.release(
                    change.getProductionTargetId(),
                    change.getCellId(),
                    change.getParameter(),
                    holderId,
                    change.getProductionChangeId(),
                    executor.actorPrincipalId()
            );
        }
    }
}
