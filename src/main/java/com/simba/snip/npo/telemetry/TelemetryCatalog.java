package com.simba.snip.npo.telemetry;

import java.util.Set;

public final class TelemetryCatalog {

    public static final Set<String> SUPPORTED_METRICS = Set.of(
            "BLER_DL",
            "BLER_UL",
            "DROP_RATE",
            "THROUGHPUT_DL",
            "THROUGHPUT_UL",
            "LATENCY",
            "PRB_UTILIZATION_DL",
            "PRB_UTILIZATION_UL"
    );

    private TelemetryCatalog() {
    }

    public static boolean isRatioUnit(String unit) {
        return unit != null && "ratio".equalsIgnoreCase(unit.trim());
    }
}
