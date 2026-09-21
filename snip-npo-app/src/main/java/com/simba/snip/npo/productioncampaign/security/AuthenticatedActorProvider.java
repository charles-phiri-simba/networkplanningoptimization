package com.simba.snip.npo.productioncampaign.security;

import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;

public interface AuthenticatedActorProvider {

    AuthenticatedActor requireHuman(CampaignPermission required);
}
