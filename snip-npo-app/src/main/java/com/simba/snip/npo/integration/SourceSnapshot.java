package com.simba.snip.npo.integration;

import java.time.Instant;
import java.util.List;

public record SourceSnapshot(
        String sourceSnapshotId,
        String sourceSystem,
        Vendor vendor,
        String vendorSchemaVersion,
        Instant capturedAt,
        boolean completeSnapshot,
        List<SourceSite> sites,
        List<SourceGnb> gnbs,
        List<SourceCell> cells,
        List<SourceConfiguration> configurations,
        List<SourceNeighbour> neighbours
) {
    public SourceSnapshot {
        sites = List.copyOf(sites);
        gnbs = List.copyOf(gnbs);
        cells = List.copyOf(cells);
        configurations = List.copyOf(configurations);
        neighbours = List.copyOf(neighbours);
    }

    public int entityCount() {
        return sites.size() + gnbs.size() + cells.size() + configurations.size() + neighbours.size();
    }
}
