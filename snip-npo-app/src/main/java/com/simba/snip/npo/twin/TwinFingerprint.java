package com.simba.snip.npo.twin;

import com.simba.snip.npo.network.CellContext;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class TwinFingerprint {

    private TwinFingerprint() {
    }

    public static String of(CellContext context) {
        String txPower = context.radioConfiguration().stream()
                .filter(p -> SimulatableParameterRegistry.TX_POWER.equals(p.parameterName()))
                .map(p -> p.parameterValue() + "@" + p.effectiveFrom())
                .findFirst()
                .orElse("missing");
        String metrics = context.telemetry().stream()
                .sorted(Comparator.comparing(CellContext.KpiSeriesView::metric))
                .map(series -> series.metric() + "=" + series.current().value() + "@" + series.current().observedAt())
                .collect(Collectors.joining("|"));
        String body = metrics.isBlank() ? "" : "|" + metrics;
        return "txPower=" + txPower + body;
    }
}
