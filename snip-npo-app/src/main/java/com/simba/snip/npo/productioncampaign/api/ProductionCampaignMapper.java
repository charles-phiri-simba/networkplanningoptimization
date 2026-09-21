package com.simba.snip.npo.productioncampaign.api;

import com.simba.snip.npo.productioncampaign.entity.ProductionChangeCampaignEntity;

public final class ProductionCampaignMapper {

    private ProductionCampaignMapper() {
    }

    public static ProductionCampaignDto toDto(ProductionChangeCampaignEntity entity) {
        return new ProductionCampaignDto(
                entity.getCampaignId(),
                entity.getProductionTargetId(),
                entity.getVendor(),
                entity.getPlatform(),
                entity.getEnvironment(),
                entity.getObjective(),
                entity.getChangeControlReference(),
                entity.getState(),
                entity.getAbortState(),
                entity.getFingerprint(),
                entity.getCurrentRevisionNumber(),
                entity.getCreatorPrincipalId(),
                entity.getAuthorizerPrincipalId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
