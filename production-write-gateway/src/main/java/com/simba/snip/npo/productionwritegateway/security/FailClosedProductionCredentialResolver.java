package com.simba.snip.npo.productionwritegateway.security;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(ProductionWriteCredentialResolver.class)
public class FailClosedProductionCredentialResolver implements ProductionWriteCredentialResolver {

    @Override
    public WriteCredentialHandle resolveLatest(String credentialProfileId) {
        throw GatewayDeniedException.deny(ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, null, null);
    }

    @Override
    public WriteCredentialHandle resolveVersion(String credentialProfileId, String version) {
        throw GatewayDeniedException.deny(ProductionReasonCode.PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE, null, null);
    }
}
