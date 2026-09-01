package com.simba.snip.npo.twin;

import java.time.Instant;

public record TwinProvenance(
        String source,
        String sourceCellId,
        String sourceContextVersion,
        Instant sourceTelemetryTimestamp,
        Instant capturedAt,
        boolean synthetic
) {
    public static final String SOURCE_SNIP_OPERATIONAL_STATE = "SNIP_OPERATIONAL_STATE";
}
