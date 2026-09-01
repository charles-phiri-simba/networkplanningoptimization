package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.adapter.ProductionWriteGatewayClient;
import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.LeaseHandle;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionchange.entity.ProductionExecutionRollbackEntity;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.policy.ProductionSeparationOfDutiesPolicy;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteRequest;
import com.simba.snip.npo.productionchange.protocol.GatewayExecuteResponse;
import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionChangeStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionExecutionRollbackRepository;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductionRollbackOrchestrationService {

    private final ProductionChangeProperties properties;
    private final ProductionNetworkChangeRepository changeRepository;
    private final ProductionExecutionRollbackRepository rollbackRepository;
    private final ProductionSeparationOfDutiesPolicy sodPolicy;
    private final ProductionLeaseService leaseService;
    private final ProductionExecutionGrantService grantService;
    private final ProductionWriteGatewayClient gatewayClient;
    private final ProductionRollbackSyncService rollbackSyncService;

    public ProductionRollbackOrchestrationService(
            ProductionChangeProperties properties,
            ProductionNetworkChangeRepository changeRepository,
            ProductionExecutionRollbackRepository rollbackRepository,
            ProductionSeparationOfDutiesPolicy sodPolicy,
            ProductionLeaseService leaseService,
            ProductionExecutionGrantService grantService,
            ProductionWriteGatewayClient gatewayClient,
            ProductionRollbackSyncService rollbackSyncService
    ) {
        this.properties = properties;
        this.changeRepository = changeRepository;
        this.rollbackRepository = rollbackRepository;
        this.sodPolicy = sodPolicy;
        this.leaseService = leaseService;
        this.grantService = grantService;
        this.gatewayClient = gatewayClient;
        this.rollbackSyncService = rollbackSyncService;
    }

    public ProductionNetworkChangeEntity execute(UUID productionChangeId, ActorPrincipal executor) {
        if (properties.isAutomaticRollbackEnabled()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_ROLLBACK_BLOCKED,
                    "automatic rollback is not authorized"
            );
        }
        ProductionNetworkChangeEntity change = changeRepository.findById(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_INVALID_REQUEST,
                        "production change not found"
                ));
        if (!ProductionChangeStatus.ROLLBACK_AUTHORIZED.name().equals(change.getStatus())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_ROLLBACK_AUTHORIZATION_MISSING,
                    "rollback execute requires independent authorization"
            );
        }
        ProductionExecutionRollbackEntity rollback = rollbackRepository
                .findFirstByProductionChangeIdOrderByCreatedAtDesc(productionChangeId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_ROLLBACK_AUTHORIZATION_MISSING,
                        "rollback record not found"
                ));
        sodPolicy.authorizerMustNotExecute(rollback.getAuthorizerPrincipalId(), executor.actorPrincipalId());
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
            ProductionExecutionGrantEntity grant = grantService.issue(change, lease, GrantType.ROLLBACK, executor);
            GatewayExecuteResponse response = gatewayClient.executeRollback(new GatewayExecuteRequest(
                    grant.getGrantId(),
                    change.getProductionChangeId(),
                    change.getProductionChangeId().toString() + ":rollback"
            ));
            return rollbackSyncService.syncFromDurableEvidence(change.getProductionChangeId(), response, executor);
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
