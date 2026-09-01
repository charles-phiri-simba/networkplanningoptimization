package com.simba.snip.npo.integration;

public record CanonicalCell(
        String sourceEntityId,
        String sourceDn,
        String canonicalCellId,
        String canonicalGnbId,
        String name,
        String technology,
        String band,
        Integer arfcn,
        Integer pci,
        Integer bandwidthMhz,
        String duplexMode,
        String status
) {
}
