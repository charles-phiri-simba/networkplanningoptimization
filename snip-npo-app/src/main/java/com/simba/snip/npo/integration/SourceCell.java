package com.simba.snip.npo.integration;

public record SourceCell(
        String sourceEntityId,
        String sourceDn,
        String canonicalCellId,
        String canonicalGnbId,
        String name,
        String technologyRaw,
        String band,
        Integer arfcn,
        Integer pci,
        Integer bandwidthMhz,
        String duplexRaw,
        String operationalStateRaw
) {
}
