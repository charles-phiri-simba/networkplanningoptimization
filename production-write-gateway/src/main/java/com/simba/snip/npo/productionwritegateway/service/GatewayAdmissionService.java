package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.GrantType;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.repository.ProductionExecutionGrantRepository;
import com.simba.snip.npo.productionwritegateway.security.GatewayCallerAuthenticator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GatewayAdmissionService {

    private final GatewayCallerAuthenticator authenticator;
    private final ProductionExecutionGrantRepository grantRepository;

    public GatewayAdmissionService(
            GatewayCallerAuthenticator authenticator,
            ProductionExecutionGrantRepository grantRepository
    ) {
        this.authenticator = authenticator;
        this.grantRepository = grantRepository;
    }

    public String authenticate(String callerId, String authorizationHeader) {
        return authenticator.authenticate(callerId, authorizationHeader);
    }

    public ProductionExecutionGrantEntity loadGrant(UUID grantId) {
        return grantRepository.findById(grantId)
                .orElseThrow(() -> GatewayDeniedException.deny(
                        ProductionReasonCode.PRODUCTION_GRANT_NOT_FOUND, grantId, null));
    }

    public void validateBindings(
            ProductionExecutionGrantEntity grant,
            UUID productionChangeId,
            GrantType expectedGrantType
    ) {
        if (grant.getProductionChangeId() == null || !grant.getProductionChangeId().equals(productionChangeId)) {
            throw GatewayDeniedException.deny(
                    ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH,
                    grant.getGrantId(),
                    productionChangeId
            );
        }
        if (expectedGrantType != null && !expectedGrantType.name().equals(grant.getGrantType())) {
            throw GatewayDeniedException.deny(
                    ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH,
                    grant.getGrantId(),
                    productionChangeId
            );
        }
    }
}
