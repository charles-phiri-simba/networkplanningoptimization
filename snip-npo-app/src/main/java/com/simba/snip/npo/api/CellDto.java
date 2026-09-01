package com.simba.snip.npo.api;

public record CellDto(
        String cellId,
        String name,
        String gnbId,
        String siteId,
        String technology,
        String band,
        Integer arfcn,
        Integer pci,
        Integer bandwidthMhz,
        String duplexMode,
        String status
) {
}
