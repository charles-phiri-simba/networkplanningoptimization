package com.simba.snip.npo.integration.ericsson.enm;

import com.simba.snip.npo.integration.PowerUnit;
import com.simba.snip.npo.integration.SourceCell;
import com.simba.snip.npo.integration.SourceConfiguration;
import com.simba.snip.npo.integration.SourceGnb;
import com.simba.snip.npo.integration.SourceNeighbour;
import com.simba.snip.npo.integration.SourceSite;
import com.simba.snip.npo.integration.SourceSnapshot;
import com.simba.snip.npo.integration.Vendor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class EricssonEnmSnapshotMapper {

    public static final String SCHEMA_VERSION = "ENM_SIMULATOR_V1";

    public SourceSnapshot toNeutral(
            String snapshotId,
            String sourceSystem,
            Instant capturedAt,
            boolean complete,
            List<EnmInventoryPage> pages
    ) {
        List<SourceSite> sites = new ArrayList<>();
        List<SourceGnb> gnbs = new ArrayList<>();
        List<SourceCell> cells = new ArrayList<>();
        List<SourceConfiguration> configurations = new ArrayList<>();
        for (EnmInventoryPage page : pages) {
            for (EnmManagedElement element : page.managedElements()) {
                sites.add(new SourceSite(
                        element.moId(),
                        element.dn(),
                        element.snipCanonicalId(),
                        element.userLabel(),
                        null,
                        null,
                        "UNLOCKED"
                ));
            }
            for (EnmRadioFunction radio : page.radioFunctions()) {
                gnbs.add(new SourceGnb(
                        radio.moId(),
                        radio.dn(),
                        radio.snipCanonicalId(),
                        radio.parentSiteCanonicalId(),
                        radio.userLabel(),
                        "Ericsson",
                        "SIM-GNB",
                        "UNLOCKED"
                ));
            }
            for (EnmCell cell : page.cells()) {
                cells.add(new SourceCell(
                        cell.moId(),
                        cell.ldn(),
                        cell.snipCanonicalId(),
                        cell.parentGnbCanonicalId(),
                        cell.userLabel(),
                        "NR",
                        "n78",
                        630000,
                        1,
                        20,
                        "TDD",
                        "UNLOCKED"
                ));
                if (cell.configuredMaxTxPowerTenthsDbm() != null) {
                    configurations.add(new SourceConfiguration(
                            cell.moId() + ":txPower",
                            cell.ldn(),
                            cell.snipCanonicalId(),
                            "txPower",
                            cell.configuredMaxTxPowerTenthsDbm().doubleValue(),
                            PowerUnit.TENTHS_DBM
                    ));
                }
            }
        }
        return new SourceSnapshot(
                snapshotId,
                sourceSystem,
                Vendor.ERICSSON,
                SCHEMA_VERSION,
                capturedAt,
                complete,
                sites,
                gnbs,
                cells,
                configurations,
                List.of()
        );
    }

    public SourceSnapshot toNeutralIncremental(
            String snapshotId,
            String sourceSystem,
            Instant capturedAt,
            com.simba.snip.npo.integration.sync.VendorIncrementalBatch batch
    ) {
        List<SourceSite> sites = new ArrayList<>();
        List<SourceGnb> gnbs = new ArrayList<>();
        List<SourceCell> cells = new ArrayList<>();
        List<SourceConfiguration> configurations = new ArrayList<>();
        for (com.simba.snip.npo.integration.sync.VendorIncrementalChange change : batch.changes()) {
            if (change.changeType() != com.simba.snip.npo.integration.sync.VendorIncrementalChangeType.UPSERT) {
                continue;
            }
            if (change.entityType() == com.simba.snip.npo.integration.CanonicalEntityType.CELL) {
                cells.add(new SourceCell(
                        change.sourceEntityId(),
                        "GNBDUFunction=1,NRCellDU=2",
                        change.canonicalEntityId(),
                        "GNB-SIM-001",
                        "Sim Cell 2",
                        "NR",
                        "n78",
                        630000,
                        1,
                        20,
                        "TDD",
                        "UNLOCKED"
                ));
                configurations.add(new SourceConfiguration(
                        change.sourceEntityId() + ":txPower",
                        "GNBDUFunction=1,NRCellDU=2",
                        change.canonicalEntityId(),
                        "txPower",
                        460.0,
                        PowerUnit.TENTHS_DBM
                ));
            }
        }
        return new SourceSnapshot(
                snapshotId,
                sourceSystem,
                Vendor.ERICSSON,
                SCHEMA_VERSION,
                capturedAt,
                false,
                sites,
                gnbs,
                cells,
                configurations,
                List.of()
        );
    }
}
