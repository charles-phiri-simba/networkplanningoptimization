package com.simba.snip.npo.integration.ericsson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EricssonExport(
        String exportId,
        String schema,
        Instant capturedAt,
        boolean complete,
        List<ManagedElement> managedElements,
        List<GnodeB> gnodeBs,
        List<NrCell> nrCells,
        List<CellRelation> cellRelations
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ManagedElement(
            String moId,
            String dn,
            String userLabel,
            String snipCanonicalId,
            Geo geo,
            String administrativeState
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geo(Double lat, Double lon) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GnodeB(
            String moId,
            String dn,
            String userLabel,
            String snipCanonicalId,
            String parentSiteCanonicalId,
            String equipmentVendor,
            String productName,
            String administrativeState
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NrCell(
            String moId,
            String ldn,
            String userLabel,
            String snipCanonicalId,
            String parentGnbCanonicalId,
            String rat,
            String freqBand,
            Integer earfcnDl,
            Integer physicalCellId,
            Integer dlBandwidth,
            String duplex,
            String administrativeState,
            Integer configuredMaxTxPower
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CellRelation(
            String moId,
            String dn,
            String sourceCanonicalId,
            String targetCanonicalId,
            String relationType,
            String administrativeState
    ) {
    }
}
