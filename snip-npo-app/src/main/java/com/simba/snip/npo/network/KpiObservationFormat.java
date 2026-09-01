package com.simba.snip.npo.network;

import java.math.BigDecimal;

/**
 * Formats KPI observations for reasoning context. Does not convert stored values.
 * Ratio units include an explicit percentage reading so 0.12 ratio is not read as 0.12%.
 */
public final class KpiObservationFormat {

    private KpiObservationFormat() {
    }

    public static String format(String metric, Double value, String unit) {
        StringBuilder sb = new StringBuilder();
        sb.append(metric).append(": ").append(value);
        if (unit != null && !unit.isBlank()) {
            sb.append(' ').append(unit);
        }
        if (isRatio(unit) && value != null) {
            sb.append(" (").append(percentFromRatio(value)).append(')');
        }
        return sb.toString();
    }

    static String percentFromRatio(double ratio) {
        BigDecimal percent = BigDecimal.valueOf(ratio)
                .multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros();
        return percent.toPlainString() + "%";
    }

    private static boolean isRatio(String unit) {
        return unit != null && "ratio".equalsIgnoreCase(unit.trim());
    }
}
