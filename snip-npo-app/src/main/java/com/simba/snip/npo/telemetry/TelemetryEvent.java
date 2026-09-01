package com.simba.snip.npo.telemetry;

import java.time.Instant;

public record TelemetryEvent(
        String eventId,
        String eventType,
        String schemaVersion,
        String source,
        String cellId,
        String metric,
        Double value,
        String unit,
        Instant eventTime,
        Instant ingestedAt,
        Boolean synthetic
) {
    public static final String TYPE_CELL_KPI_OBSERVED = "CELL_KPI_OBSERVED";
    public static final String SCHEMA_V1 = "1.0";
    public static final String SOURCE_SIMULATOR = "SNIP_SIMULATOR";

    public String kafkaKey() {
        return cellId;
    }
}
