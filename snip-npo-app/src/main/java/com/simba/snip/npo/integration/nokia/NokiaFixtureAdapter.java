package com.simba.snip.npo.integration.nokia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.domain.DomainValidationException;
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
public class NokiaFixtureAdapter implements NetworkSourceAdapter {

    public static final String SOURCE_SYSTEM = "NOKIA_FIXTURE";
    public static final String SCHEMA_VERSION = "NOKIA_FIXTURE_V1";

    private final ObjectMapper objectMapper;

    public NokiaFixtureAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Vendor vendor() {
        return Vendor.NOKIA;
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
        try (InputStream in = NokiaFixtureAdapter.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IntegrationSnapshotException("Nokia fixture missing: " + resource);
            }
            NokiaSnapshot snapshot = objectMapper.readValue(in, NokiaSnapshot.class);
            return toSnapshot(snapshot);
        } catch (IOException ex) {
            throw new IntegrationSnapshotException("Nokia fixture is unreadable: " + resource, ex);
        }
    }

    private static String resourceFor(FixtureKind kind) {
        return switch (kind) {
            case NORMAL -> "/integration/nokia/normal.json";
            case CONFLICT -> "/integration/nokia/conflict.json";
            default -> throw new DomainValidationException("Nokia fixture kind is not configured: " + kind);
        };
    }

    private static SourceSnapshot toSnapshot(NokiaSnapshot snapshot) {
        if (snapshot == null || snapshot.snapshotUid() == null || snapshot.timeStamp() == null) {
            throw new IntegrationSnapshotException("Nokia snapshot metadata is incomplete");
        }
        List<SourceSite> sites = new ArrayList<>();
        List<SourceGnb> gnbs = new ArrayList<>();
        List<SourceCell> cells = new ArrayList<>();
        List<SourceConfiguration> configurations = new ArrayList<>();
        List<SourceNeighbour> neighbours = new ArrayList<>();
        if (snapshot.btsSites() != null) {
            for (NokiaSnapshot.BtsSite site : snapshot.btsSites()) {
                sites.add(new SourceSite(
                        site.distName(),
                        site.distName(),
                        site.snipId(),
                        site.siteName(),
                        site.latitude(),
                        site.longitude(),
                        site.operationalState()
                ));
            }
        }
        if (snapshot.nrbts() != null) {
            for (NokiaSnapshot.NrBts gnb : snapshot.nrbts()) {
                gnbs.add(new SourceGnb(
                        gnb.distName(),
                        gnb.distName(),
                        gnb.snipId(),
                        gnb.parentSite(),
                        gnb.btsName(),
                        gnb.vendorName() == null ? "Nokia" : gnb.vendorName(),
                        gnb.hardware(),
                        gnb.operationalState()
                ));
            }
        }
        if (snapshot.lcells() != null) {
            for (NokiaSnapshot.LteCell cell : snapshot.lcells()) {
                cells.add(new SourceCell(
                        cell.distName(),
                        cell.distName(),
                        cell.snipId(),
                        cell.parentGnb(),
                        cell.cellName(),
                        cell.technology(),
                        cell.operatingBand(),
                        cell.nrarfcn(),
                        cell.pci(),
                        cell.channelBw(),
                        cell.duplexMode(),
                        cell.operationalState()
                ));
                if (cell.pMax() != null) {
                    configurations.add(new SourceConfiguration(
                            cell.distName() + ":pMax",
                            cell.distName(),
                            cell.snipId(),
                            "txPower",
                            cell.pMax(),
                            PowerUnit.DBM
                    ));
                }
            }
        }
        if (snapshot.adjs() != null) {
            for (NokiaSnapshot.Adj adj : snapshot.adjs()) {
                neighbours.add(new SourceNeighbour(
                        adj.distName(),
                        adj.distName(),
                        adj.sourceSnipId(),
                        adj.targetSnipId(),
                        adj.adjType(),
                        adj.operationalState()
                ));
            }
        }
        return new SourceSnapshot(
                snapshot.snapshotUid(),
                SOURCE_SYSTEM,
                Vendor.NOKIA,
                snapshot.schemaVersion() == null ? SCHEMA_VERSION : snapshot.schemaVersion(),
                snapshot.timeStamp(),
                snapshot.fullExport(),
                sites,
                gnbs,
                cells,
                configurations,
                neighbours
        );
    }
}
