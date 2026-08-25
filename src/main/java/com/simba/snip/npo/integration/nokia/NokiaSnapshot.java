package com.simba.snip.npo.integration.nokia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NokiaSnapshot(
        String snapshotUid,
        String schemaVersion,
        Instant timeStamp,
        boolean fullExport,
        List<BtsSite> btsSites,
        List<NrBts> nrbts,
        List<LteCell> lcells,
        List<Adj> adjs
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BtsSite(
            String distName,
            String siteName,
            String snipId,
            Double latitude,
            Double longitude,
            String operationalState
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NrBts(
            String distName,
            String btsName,
            String snipId,
            String parentSite,
            String vendorName,
            String hardware,
            String operationalState
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LteCell(
            String distName,
            String cellName,
            String snipId,
            String parentGnb,
            String technology,
            String operatingBand,
            Integer nrarfcn,
            Integer pci,
            Integer channelBw,
            String duplexMode,
            String operationalState,
            Double pMax
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Adj(
            String distName,
            String sourceSnipId,
            String targetSnipId,
            String adjType,
            String operationalState
    ) {
    }
}
