package com.simba.snip.npo.productionwritegateway.vendortransport;

import com.simba.snip.npo.productionchange.protocol.RuntimeArtifactIdentity;

public interface RuntimeTransportArtifactIdentityProvider {

    RuntimeArtifactIdentity currentIdentity();
}
