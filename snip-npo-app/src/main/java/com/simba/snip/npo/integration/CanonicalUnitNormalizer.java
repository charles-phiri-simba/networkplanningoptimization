package com.simba.snip.npo.integration;

import com.simba.snip.npo.twin.SimulatableParameterRegistry;

import java.math.BigDecimal;

/**
 * Canonical txPower is always dBm. Ericsson fixtures use tenths of a dBm
 * ({@code configuredMaxTxPower = 460} → {@code 46.0 dBm}). Nokia fixtures use direct dBm.
 */
public final class CanonicalUnitNormalizer {

    private CanonicalUnitNormalizer() {
    }

    public static double txPowerToDbm(double value, PowerUnit unit) {
        if (unit == null) {
            throw new IntegrationSnapshotException("txPower unit is required");
        }
        return switch (unit) {
            case DBM -> value;
            case TENTHS_DBM -> value / 10.0d;
        };
    }

    public static String formatDbm(double dbm) {
        if (dbm == Math.rint(dbm)) {
            return Long.toString((long) Math.rint(dbm));
        }
        return BigDecimal.valueOf(dbm).stripTrailingZeros().toPlainString();
    }

    public static boolean inOperationalRange(double dbm) {
        BigDecimal value = BigDecimal.valueOf(dbm);
        return value.compareTo(SimulatableParameterRegistry.find(SimulatableParameterRegistry.TX_POWER)
                .orElseThrow()
                .minValue()) >= 0
                && value.compareTo(SimulatableParameterRegistry.find(SimulatableParameterRegistry.TX_POWER)
                .orElseThrow()
                .maxValue()) <= 0;
    }
}
