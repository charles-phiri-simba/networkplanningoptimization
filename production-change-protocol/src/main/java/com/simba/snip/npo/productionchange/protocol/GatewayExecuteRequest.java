package com.simba.snip.npo.productionchange.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record GatewayExecuteRequest(
        UUID grantId,
        UUID productionChangeId,
        String correlationId
) {
}
