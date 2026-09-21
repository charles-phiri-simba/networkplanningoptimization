package com.simba.snip.npo.productioncampaign.api;

import java.time.Instant;
import java.util.UUID;

public record ProductionCampaignDto(
        UUID campaignId,
        String productionTargetId,
        String vendor,
        String platform,
        String environment,
        String objective,
        String changeControlReference,
        String state,
        String abortState,
        String fingerprint,
        int currentRevisionNumber,
        String creatorPrincipalId,
        String authorizerPrincipalId,
        Instant createdAt,
        Instant updatedAt
) {
}
