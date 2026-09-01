package com.simba.snip.npo.productionwritegateway.security;

import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GatewayCallerAuthenticator {

    public static final String CALLER_HEADER = "X-SNIP-GATEWAY-CALLER-ID";

    private final ProductionChangeGatewayProperties properties;

    public GatewayCallerAuthenticator(ProductionChangeGatewayProperties properties) {
        this.properties = properties;
    }

    public String authenticate(String callerId, String authorizationHeader) {
        if (callerId == null || callerId.isBlank()) {
            if (authorizationHeader != null && authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                throw GatewayDeniedException.deny(
                        ProductionReasonCode.PRODUCTION_UNAUTHORIZED, null, null);
            }
            throw GatewayDeniedException.deny(ProductionReasonCode.PRODUCTION_UNAUTHORIZED, null, null);
        }
        if (!properties.getGateway().getAllowedCallerIds().contains(callerId)) {
            throw GatewayDeniedException.deny(ProductionReasonCode.PRODUCTION_UNAUTHORIZED, null, null);
        }
        return callerId;
    }

    public void rejectJwtSoleAuthority(String authorizationHeader, String callerId) {
        if (callerId == null || callerId.isBlank()) {
            authenticate(null, authorizationHeader);
        }
    }

    public UUID unused() {
        return null;
    }
}
