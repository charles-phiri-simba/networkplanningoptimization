package com.simba.snip.npo.productionchange.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record AuthorizeProductionChangeRequest() {
}
