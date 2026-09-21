package com.simba.snip.npo.productioncampaign.domain;

import java.util.Set;

public record AuthenticatedActor(
        String actorId,
        ActorType actorType,
        Set<CampaignPermission> authorities,
        String authenticationSource,
        boolean authenticated
) {
}
