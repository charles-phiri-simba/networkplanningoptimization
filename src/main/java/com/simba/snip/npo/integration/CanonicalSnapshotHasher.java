package com.simba.snip.npo.integration;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class CanonicalSnapshotHasher {

    public String hash(CanonicalSnapshot snapshot) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(canonicalPayload(snapshot).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }

    String canonicalPayload(CanonicalSnapshot snapshot) {
        StringBuilder out = new StringBuilder();
        out.append("complete=").append(snapshot.completeSnapshot()).append('\n');
        out.append("sites=").append(sites(snapshot.sites())).append('\n');
        out.append("gnbs=").append(gnbs(snapshot.gnbs())).append('\n');
        out.append("cells=").append(cells(snapshot.cells())).append('\n');
        out.append("configs=").append(configs(snapshot.configurations())).append('\n');
        out.append("neighbours=").append(neighbours(snapshot.neighbours())).append('\n');
        return out.toString();
    }

    private static String sites(List<CanonicalSite> sites) {
        return sites.stream()
                .sorted(Comparator.comparing(CanonicalSite::canonicalSiteId))
                .map(site -> String.join("|",
                        n(site.canonicalSiteId()),
                        n(site.sourceEntityId()),
                        n(site.name()),
                        n(site.latitude()),
                        n(site.longitude()),
                        n(site.status())))
                .collect(Collectors.joining(";"));
    }

    private static String gnbs(List<CanonicalGnb> gnbs) {
        return gnbs.stream()
                .sorted(Comparator.comparing(CanonicalGnb::canonicalGnbId))
                .map(gnb -> String.join("|",
                        n(gnb.canonicalGnbId()),
                        n(gnb.canonicalSiteId()),
                        n(gnb.sourceEntityId()),
                        n(gnb.name()),
                        n(gnb.equipmentVendor()),
                        n(gnb.model()),
                        n(gnb.status())))
                .collect(Collectors.joining(";"));
    }

    private static String cells(List<CanonicalCell> cells) {
        return cells.stream()
                .sorted(Comparator.comparing(CanonicalCell::canonicalCellId))
                .map(cell -> String.join("|",
                        n(cell.canonicalCellId()),
                        n(cell.canonicalGnbId()),
                        n(cell.sourceEntityId()),
                        n(cell.name()),
                        n(cell.technology()),
                        n(cell.band()),
                        n(cell.arfcn()),
                        n(cell.pci()),
                        n(cell.bandwidthMhz()),
                        n(cell.duplexMode()),
                        n(cell.status())))
                .collect(Collectors.joining(";"));
    }

    private static String configs(List<CanonicalCellConfiguration> configurations) {
        return configurations.stream()
                .sorted(Comparator.comparing(CanonicalCellConfiguration::canonicalCellId)
                        .thenComparing(CanonicalCellConfiguration::parameterName))
                .map(config -> String.join("|",
                        n(config.canonicalCellId()),
                        n(config.parameterName()),
                        String.format(Locale.ROOT, "%.9f", config.txPowerDbm()),
                        n(config.unit()),
                        n(config.sourceEntityId())))
                .collect(Collectors.joining(";"));
    }

    private static String neighbours(List<CanonicalNeighbourRelation> neighbours) {
        return neighbours.stream()
                .sorted(Comparator.comparing(CanonicalNeighbourRelation::canonicalSourceCellId)
                        .thenComparing(CanonicalNeighbourRelation::canonicalTargetCellId))
                .map(neighbour -> String.join("|",
                        n(neighbour.canonicalSourceCellId()),
                        n(neighbour.canonicalTargetCellId()),
                        n(neighbour.relationType()),
                        n(neighbour.status()),
                        n(neighbour.sourceEntityId())))
                .collect(Collectors.joining(";"));
    }

    private static String n(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
