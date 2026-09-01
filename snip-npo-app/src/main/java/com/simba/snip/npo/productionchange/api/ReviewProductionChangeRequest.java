package com.simba.snip.npo.productionchange.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ReviewProductionChangeRequest(
        String decision,
        List<String> reasonCodes
) {
}
