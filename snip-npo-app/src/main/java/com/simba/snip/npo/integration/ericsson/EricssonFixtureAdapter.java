package com.simba.snip.npo.integration.ericsson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.integration.FixtureKind;
import com.simba.snip.npo.integration.IntegrationSnapshotException;
import com.simba.snip.npo.integration.NetworkSourceAdapter;
import com.simba.snip.npo.integration.PowerUnit;
import com.simba.snip.npo.integration.SourceCell;
import com.simba.snip.npo.integration.SourceConfiguration;
import com.simba.snip.npo.integration.SourceGnb;
import com.simba.snip.npo.integration.SourceNeighbour;
import com.simba.snip.npo.integration.SourceSite;
import com.simba.snip.npo.integration.SourceSnapshot;
import com.simba.snip.npo.integration.Vendor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class EricssonFixtureAdapter implements NetworkSourceAdapter {

    public static final String SOURCE_SYSTEM = "ERICSSON_FIXTURE";
    public static final String SCHEMA_VERSION = "ERICSSON_FIXTURE_V1";

    private final ObjectMapper objectMapper;

    public EricssonFixtureAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Vendor vendor() {
        return Vendor.ERICSSON;
    }

    @Override
    public String sourceSystem() {
        return SOURCE_SYSTEM;
    }

    @Override
    public String schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public SourceSnapshot readSnapshot(FixtureKind kind) {
        String resource = resourceFor(kind);
        try (InputStream in = EricssonFixtureAdapter.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IntegrationSnapshotException("Ericsson fixture missing: " + resource);
            }
            EricssonExport export = objectMapper.readValue(in, EricssonExport.class);
            return toSnapshot(export);
        } catch (IOException ex) {
            throw new IntegrationSnapshotException("Ericsson fixture is unreadable: " + resource, ex);
        }
    }

    private static String resourceFor(FixtureKind kind) {
        return switch (kind) {
            case NORMAL -> "/integration/ericsson/normal.json";
            case CONFLICT -> "/integration/ericsson/conflict.json";
            case CELL001_STALE -> "/integration/ericsson/cell001-stale.json";
            case UPDATE -> "/integration/ericsson/update.json";
            case MISSING_OMIT -> "/integration/ericsson/missing-omit.json";
            case REJECT -> "/integration/ericsson/reject.json";
            case PARTIAL -> "/integration/ericsson/partial.json";
            case REAPPEAR -> "/integration/ericsson/reappear.json";
            case CATASTROPHIC -> "/integration/ericsson/catastrophic.json";
            case DELAY -> "/integration/ericsson/delay.json";
            case SNAPSHOT_FAIL -> throw new IntegrationSnapshotException("Ericsson snapshot read failed");
            case CONTENT_MISMATCH -> "/integration/ericsson/content-mismatch.json";
            case COMMIT_FAIL -> "/integration/ericsson/commit-fail.json";
            case TIMEOUT -> "/integration/ericsson/timeout.json";
            case IDENTITY_BASE -> "/integration/ericsson/identity-base.json";
        };
    }

    private static SourceSnapshot toSnapshot(EricssonExport export) {
        if (export == null || export.exportId() == null || export.capturedAt() == null) {
            throw new IntegrationSnapshotException("Ericsson snapshot metadata is incomplete");
        }
        List<SourceSite> sites = new ArrayList<>();
        List<SourceGnb> gnbs = new ArrayList<>();
        List<SourceCell> cells = new ArrayList<>();
        List<SourceConfiguration> configurations = new ArrayList<>();
        List<SourceNeighbour> neighbours = new ArrayList<>();
        if (export.managedElements() != null) {
            for (EricssonExport.ManagedElement element : export.managedElements()) {
                Double lat = element.geo() == null ? null : element.geo().lat();
                Double lon = element.geo() == null ? null : element.geo().lon();
                sites.add(new SourceSite(
                        element.moId(),
                        element.dn(),
                        element.snipCanonicalId(),
                        element.userLabel(),
                        lat,
                        lon,
                        element.administrativeState()
                ));
            }
        }
        if (export.gnodeBs() != null) {
            for (EricssonExport.GnodeB gnb : export.gnodeBs()) {
                gnbs.add(new SourceGnb(
                        gnb.moId(),
                        gnb.dn(),
                        gnb.snipCanonicalId(),
                        gnb.parentSiteCanonicalId(),
                        gnb.userLabel(),
                        gnb.equipmentVendor() == null || gnb.equipmentVendor().isBlank()
                                ? "Ericsson"
                                : gnb.equipmentVendor(),
                        gnb.productName(),
                        gnb.administrativeState()
                ));
            }
        }
        if (export.nrCells() != null) {
            for (EricssonExport.NrCell cell : export.nrCells()) {
                cells.add(new SourceCell(
                        cell.moId(),
                        cell.ldn(),
                        cell.snipCanonicalId(),
                        cell.parentGnbCanonicalId(),
                        cell.userLabel(),
                        cell.rat(),
                        cell.freqBand(),
                        cell.earfcnDl(),
                        cell.physicalCellId(),
                        cell.dlBandwidth(),
                        cell.duplex(),
                        cell.administrativeState()
                ));
                if (cell.configuredMaxTxPower() != null) {
                    configurations.add(new SourceConfiguration(
                            cell.moId() + ":txPower",
                            cell.ldn(),
                            cell.snipCanonicalId(),
                            "txPower",
                            cell.configuredMaxTxPower().doubleValue(),
                            PowerUnit.TENTHS_DBM
                    ));
                }
            }
        }
        if (export.cellRelations() != null) {
            for (EricssonExport.CellRelation relation : export.cellRelations()) {
                neighbours.add(new SourceNeighbour(
                        relation.moId(),
                        relation.dn(),
                        relation.sourceCanonicalId(),
                        relation.targetCanonicalId(),
                        relation.relationType(),
                        relation.administrativeState()
                ));
            }
        }
        return new SourceSnapshot(
                export.exportId(),
                SOURCE_SYSTEM,
                Vendor.ERICSSON,
                export.schema() == null ? SCHEMA_VERSION : export.schema(),
                export.capturedAt(),
                export.complete(),
                sites,
                gnbs,
                cells,
                configurations,
                neighbours
        );
    }
}
