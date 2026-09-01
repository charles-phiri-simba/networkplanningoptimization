package com.simba.snip.npo.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.integration.PowerUnit;
import com.simba.snip.npo.integration.SourceCell;
import com.simba.snip.npo.integration.SourceConfiguration;
import com.simba.snip.npo.integration.SourceGnb;
import com.simba.snip.npo.integration.SourceNeighbour;
import com.simba.snip.npo.integration.SourceSite;
import com.simba.snip.npo.integration.SourceSnapshot;
import com.simba.snip.npo.integration.Vendor;
import com.simba.snip.npo.integration.ericsson.EricssonExport;
import com.simba.snip.npo.integration.nokia.NokiaSnapshot;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SecureVendorSnapshotParser {

    private final ObjectMapper objectMapper;

    public SecureVendorSnapshotParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SourceSnapshot parse(ConnectorDefinition definition, byte[] body) {
        try {
            if (definition.vendor() == Vendor.ERICSSON) {
                return ericsson(definition, objectMapper.readValue(body, EricssonExport.class));
            }
            return nokia(definition, objectMapper.readValue(body, NokiaSnapshot.class));
        } catch (IOException ex) {
            throw new ConnectorSecurityException(
                    com.simba.snip.npo.integration.ImportFailureCode.SNAPSHOT_READ_FAILED,
                    "connector inventory payload is unreadable",
                    ex
            );
        }
    }

    private static SourceSnapshot ericsson(ConnectorDefinition definition, EricssonExport export) {
        if (export == null || export.exportId() == null || export.capturedAt() == null) {
            throw new ConnectorSecurityException(
                    com.simba.snip.npo.integration.ImportFailureCode.SNAPSHOT_READ_FAILED,
                    "snapshot metadata is incomplete");
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
                        element.moId(), element.dn(), element.snipCanonicalId(), element.userLabel(),
                        lat, lon, element.administrativeState()));
            }
        }
        if (export.gnodeBs() != null) {
            for (EricssonExport.GnodeB gnb : export.gnodeBs()) {
                gnbs.add(new SourceGnb(
                        gnb.moId(), gnb.dn(), gnb.snipCanonicalId(), gnb.parentSiteCanonicalId(), gnb.userLabel(),
                        gnb.equipmentVendor() == null || gnb.equipmentVendor().isBlank() ? "Ericsson" : gnb.equipmentVendor(),
                        gnb.productName(), gnb.administrativeState()));
            }
        }
        if (export.nrCells() != null) {
            for (EricssonExport.NrCell cell : export.nrCells()) {
                cells.add(new SourceCell(
                        cell.moId(), cell.ldn(), cell.snipCanonicalId(), cell.parentGnbCanonicalId(), cell.userLabel(),
                        cell.rat(), cell.freqBand(), cell.earfcnDl(), cell.physicalCellId(), cell.dlBandwidth(),
                        cell.duplex(), cell.administrativeState()));
                if (cell.configuredMaxTxPower() != null) {
                    configurations.add(new SourceConfiguration(
                            cell.moId() + ":txPower", cell.ldn(), cell.snipCanonicalId(), "txPower",
                            cell.configuredMaxTxPower().doubleValue(), PowerUnit.TENTHS_DBM));
                }
            }
        }
        if (export.cellRelations() != null) {
            for (EricssonExport.CellRelation relation : export.cellRelations()) {
                neighbours.add(new SourceNeighbour(
                        relation.moId(), relation.dn(), relation.sourceCanonicalId(), relation.targetCanonicalId(),
                        relation.relationType(), relation.administrativeState()));
            }
        }
        return new SourceSnapshot(
                export.exportId(),
                definition.sourceSystem(),
                Vendor.ERICSSON,
                export.schema() == null ? "ERICSSON_SECURE_MOCK_V1" : export.schema(),
                export.capturedAt(),
                export.complete(),
                sites, gnbs, cells, configurations, neighbours
        );
    }

    private static SourceSnapshot nokia(ConnectorDefinition definition, NokiaSnapshot snapshot) {
        if (snapshot == null || snapshot.snapshotUid() == null || snapshot.timeStamp() == null) {
            throw new ConnectorSecurityException(
                    com.simba.snip.npo.integration.ImportFailureCode.SNAPSHOT_READ_FAILED,
                    "snapshot metadata is incomplete");
        }
        List<SourceSite> sites = new ArrayList<>();
        List<SourceGnb> gnbs = new ArrayList<>();
        List<SourceCell> cells = new ArrayList<>();
        List<SourceConfiguration> configurations = new ArrayList<>();
        List<SourceNeighbour> neighbours = new ArrayList<>();
        if (snapshot.btsSites() != null) {
            for (NokiaSnapshot.BtsSite site : snapshot.btsSites()) {
                sites.add(new SourceSite(
                        site.distName(), site.distName(), site.snipId(), site.siteName(),
                        site.latitude(), site.longitude(), site.operationalState()));
            }
        }
        if (snapshot.nrbts() != null) {
            for (NokiaSnapshot.NrBts gnb : snapshot.nrbts()) {
                gnbs.add(new SourceGnb(
                        gnb.distName(), gnb.distName(), gnb.snipId(), gnb.parentSite(), gnb.btsName(),
                        gnb.vendorName() == null ? "Nokia" : gnb.vendorName(),
                        gnb.hardware(), gnb.operationalState()));
            }
        }
        if (snapshot.lcells() != null) {
            for (NokiaSnapshot.LteCell cell : snapshot.lcells()) {
                cells.add(new SourceCell(
                        cell.distName(), cell.distName(), cell.snipId(), cell.parentGnb(), cell.cellName(),
                        cell.technology(), cell.operatingBand(), cell.nrarfcn(), cell.pci(), cell.channelBw(),
                        cell.duplexMode(), cell.operationalState()));
                if (cell.pMax() != null) {
                    configurations.add(new SourceConfiguration(
                            cell.distName() + ":pMax", cell.distName(), cell.snipId(), "txPower",
                            cell.pMax(), PowerUnit.DBM));
                }
            }
        }
        if (snapshot.adjs() != null) {
            for (NokiaSnapshot.Adj adj : snapshot.adjs()) {
                neighbours.add(new SourceNeighbour(
                        adj.distName(), adj.distName(), adj.sourceSnipId(), adj.targetSnipId(),
                        adj.adjType(), adj.operationalState()));
            }
        }
        return new SourceSnapshot(
                snapshot.snapshotUid(),
                definition.sourceSystem(),
                Vendor.NOKIA,
                snapshot.schemaVersion() == null ? "NOKIA_SECURE_MOCK_V1" : snapshot.schemaVersion(),
                snapshot.timeStamp(),
                snapshot.fullExport(),
                sites, gnbs, cells, configurations, neighbours
        );
    }
}
