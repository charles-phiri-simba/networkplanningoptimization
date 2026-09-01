package com.simba.snip.npo.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CanonicalSnapshot {

    private final SourceSnapshot source;
    private final List<CanonicalSite> sites = new ArrayList<>();
    private final List<CanonicalGnb> gnbs = new ArrayList<>();
    private final List<CanonicalCell> cells = new ArrayList<>();
    private final List<CanonicalCellConfiguration> configurations = new ArrayList<>();
    private final List<CanonicalNeighbourRelation> neighbours = new ArrayList<>();

    public CanonicalSnapshot(SourceSnapshot source) {
        this.source = source;
    }

    public SourceSnapshot source() {
        return source;
    }

    public String sourceSnapshotId() {
        return source.sourceSnapshotId();
    }

    public String sourceSystem() {
        return source.sourceSystem();
    }

    public Vendor vendor() {
        return source.vendor();
    }

    public String vendorSchemaVersion() {
        return source.vendorSchemaVersion();
    }

    public Instant capturedAt() {
        return source.capturedAt();
    }

    public boolean completeSnapshot() {
        return source.completeSnapshot();
    }

    public List<CanonicalSite> sites() {
        return sites;
    }

    public List<CanonicalGnb> gnbs() {
        return gnbs;
    }

    public List<CanonicalCell> cells() {
        return cells;
    }

    public List<CanonicalCellConfiguration> configurations() {
        return configurations;
    }

    public List<CanonicalNeighbourRelation> neighbours() {
        return neighbours;
    }
}
