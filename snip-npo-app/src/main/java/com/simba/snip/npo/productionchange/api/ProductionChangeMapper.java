package com.simba.snip.npo.productionchange.api;

import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;

public final class ProductionChangeMapper {

    private ProductionChangeMapper() {
    }

    public static ProductionChangeDto toDto(ProductionNetworkChangeEntity entity) {
        return new ProductionChangeDto(
                entity.getProductionChangeId(),
                entity.getPhase15ExecutionId(),
                entity.getProductionTargetId(),
                entity.getStatus(),
                entity.getReasonCode(),
                entity.getCellId(),
                entity.getParameter(),
                entity.getExpectedValue(),
                entity.getDesiredValue(),
                entity.getProductionFingerprint(),
                entity.getAuthorizationGeneration(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
