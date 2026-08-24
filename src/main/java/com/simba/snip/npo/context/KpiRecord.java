package com.simba.snip.npo.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KpiRecord(
        String id,
        String site,
        String cell,
        String band,
        double bler,
        double dropRate,
        double latencyMs
) {
}
