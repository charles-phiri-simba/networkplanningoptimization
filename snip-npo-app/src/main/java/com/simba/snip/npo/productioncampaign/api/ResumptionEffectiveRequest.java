package com.simba.snip.npo.productioncampaign.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * HTTP body for resumption EFFECTIVE. Safety predicates are read from durable
 * state; clients cannot supply them.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ResumptionEffectiveRequest {
}
