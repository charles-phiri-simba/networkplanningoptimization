package com.simba.snip.npo.integration;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CanonicalNormalizer {

    public NormalizeResult normalize(SourceSnapshot source) {
        CanonicalSnapshot canonical = new CanonicalSnapshot(source);
        List<ValidationIssue> issues = new ArrayList<>();
        for (SourceSite site : source.sites()) {
            if (blank(site.sourceEntityId())) {
                issues.add(issue(RejectionReasonCode.MISSING_SOURCE_ID, CanonicalEntityType.SITE, site.sourceEntityId(),
                        "site source id is required"));
                continue;
            }
            if (blank(site.canonicalSiteId())) {
                issues.add(issue(RejectionReasonCode.MISSING_CANONICAL_ID, CanonicalEntityType.SITE, site.sourceEntityId(),
                        "canonical site id is required"));
                continue;
            }
            var status = CanonicalEnumerationNormalizer.operationalStatus(site.operationalStateRaw());
            if (status.isEmpty()) {
                issues.add(issue(RejectionReasonCode.UNSUPPORTED_TECHNOLOGY, CanonicalEntityType.SITE, site.sourceEntityId(),
                        "unsupported operational state: " + site.operationalStateRaw()));
                continue;
            }
            canonical.sites().add(new CanonicalSite(
                    site.sourceEntityId(),
                    site.sourceDn(),
                    site.canonicalSiteId(),
                    site.name(),
                    site.latitude(),
                    site.longitude(),
                    status.get()
            ));
        }
        for (SourceGnb gnb : source.gnbs()) {
            if (blank(gnb.sourceEntityId())) {
                issues.add(issue(RejectionReasonCode.MISSING_SOURCE_ID, CanonicalEntityType.GNB, gnb.sourceEntityId(),
                        "gnb source id is required"));
                continue;
            }
            if (blank(gnb.canonicalGnbId()) || blank(gnb.canonicalSiteId())) {
                issues.add(issue(RejectionReasonCode.MISSING_CANONICAL_ID, CanonicalEntityType.GNB, gnb.sourceEntityId(),
                        "canonical gnb/site id is required"));
                continue;
            }
            var status = CanonicalEnumerationNormalizer.operationalStatus(gnb.operationalStateRaw());
            if (status.isEmpty()) {
                issues.add(issue(RejectionReasonCode.UNSUPPORTED_TECHNOLOGY, CanonicalEntityType.GNB, gnb.sourceEntityId(),
                        "unsupported operational state: " + gnb.operationalStateRaw()));
                continue;
            }
            canonical.gnbs().add(new CanonicalGnb(
                    gnb.sourceEntityId(),
                    gnb.sourceDn(),
                    gnb.canonicalGnbId(),
                    gnb.canonicalSiteId(),
                    gnb.name(),
                    gnb.equipmentVendor(),
                    gnb.model(),
                    status.get()
            ));
        }
        for (SourceCell cell : source.cells()) {
            if (blank(cell.sourceEntityId())) {
                issues.add(issue(RejectionReasonCode.MISSING_SOURCE_ID, CanonicalEntityType.CELL, cell.sourceEntityId(),
                        "cell source id is required"));
                continue;
            }
            if (blank(cell.canonicalCellId()) || blank(cell.canonicalGnbId())) {
                issues.add(issue(RejectionReasonCode.MISSING_CANONICAL_ID, CanonicalEntityType.CELL, cell.sourceEntityId(),
                        "canonical cell/gnb id is required"));
                continue;
            }
            var technology = CanonicalEnumerationNormalizer.technology(cell.technologyRaw());
            if (technology.isEmpty()) {
                issues.add(issue(RejectionReasonCode.UNSUPPORTED_TECHNOLOGY, CanonicalEntityType.CELL, cell.sourceEntityId(),
                        "unsupported technology: " + cell.technologyRaw()));
                continue;
            }
            var duplex = CanonicalEnumerationNormalizer.duplex(cell.duplexRaw());
            if (cell.duplexRaw() != null && !cell.duplexRaw().isBlank() && duplex.isEmpty()) {
                issues.add(issue(RejectionReasonCode.UNSUPPORTED_DUPLEX, CanonicalEntityType.CELL, cell.sourceEntityId(),
                        "unsupported duplex: " + cell.duplexRaw()));
                continue;
            }
            var status = CanonicalEnumerationNormalizer.operationalStatus(cell.operationalStateRaw());
            if (status.isEmpty()) {
                issues.add(issue(RejectionReasonCode.UNSUPPORTED_TECHNOLOGY, CanonicalEntityType.CELL, cell.sourceEntityId(),
                        "unsupported operational state: " + cell.operationalStateRaw()));
                continue;
            }
            canonical.cells().add(new CanonicalCell(
                    cell.sourceEntityId(),
                    cell.sourceDn(),
                    cell.canonicalCellId(),
                    cell.canonicalGnbId(),
                    cell.name(),
                    technology.get(),
                    cell.band(),
                    cell.arfcn(),
                    cell.pci(),
                    cell.bandwidthMhz(),
                    duplex.orElse(null),
                    status.get()
            ));
        }
        for (SourceConfiguration configuration : source.configurations()) {
            if (blank(configuration.sourceEntityId())) {
                issues.add(issue(RejectionReasonCode.MISSING_SOURCE_ID, CanonicalEntityType.CELL_CONFIGURATION,
                        configuration.sourceEntityId(), "configuration source id is required"));
                continue;
            }
            if (blank(configuration.canonicalCellId())) {
                issues.add(issue(RejectionReasonCode.MISSING_CANONICAL_ID, CanonicalEntityType.CELL_CONFIGURATION,
                        configuration.sourceEntityId(), "canonical cell id is required"));
                continue;
            }
            if (configuration.sourceUnit() == null) {
                issues.add(issue(RejectionReasonCode.INVALID_UNIT, CanonicalEntityType.CELL_CONFIGURATION,
                        configuration.sourceEntityId(), "txPower unit is required"));
                continue;
            }
            if (configuration.sourceValue() == null || !Double.isFinite(configuration.sourceValue())) {
                issues.add(issue(RejectionReasonCode.INVALID_TX_POWER, CanonicalEntityType.CELL_CONFIGURATION,
                        configuration.sourceEntityId(), "txPower value is invalid"));
                continue;
            }
            double dbm = CanonicalUnitNormalizer.txPowerToDbm(configuration.sourceValue(), configuration.sourceUnit());
            if (!CanonicalUnitNormalizer.inOperationalRange(dbm)) {
                issues.add(issue(RejectionReasonCode.INVALID_TX_POWER, CanonicalEntityType.CELL_CONFIGURATION,
                        configuration.sourceEntityId(), "txPower out of range: " + dbm + " dBm"));
                continue;
            }
            canonical.configurations().add(new CanonicalCellConfiguration(
                    configuration.sourceEntityId(),
                    configuration.sourceDn(),
                    configuration.canonicalCellId(),
                    "txPower",
                    dbm,
                    "dBm"
            ));
        }
        for (SourceNeighbour neighbour : source.neighbours()) {
            if (blank(neighbour.sourceEntityId())) {
                issues.add(issue(RejectionReasonCode.MISSING_SOURCE_ID, CanonicalEntityType.NEIGHBOUR,
                        neighbour.sourceEntityId(), "neighbour source id is required"));
                continue;
            }
            if (blank(neighbour.canonicalSourceCellId()) || blank(neighbour.canonicalTargetCellId())) {
                issues.add(issue(RejectionReasonCode.MISSING_CANONICAL_ID, CanonicalEntityType.NEIGHBOUR,
                        neighbour.sourceEntityId(), "canonical neighbour cell ids are required"));
                continue;
            }
            if (neighbour.canonicalSourceCellId().equals(neighbour.canonicalTargetCellId())) {
                issues.add(issue(RejectionReasonCode.MALFORMED_RELATIONSHIP, CanonicalEntityType.NEIGHBOUR,
                        neighbour.sourceEntityId(), "self-neighbour is not allowed"));
                continue;
            }
            var status = CanonicalEnumerationNormalizer.operationalStatus(neighbour.operationalStateRaw());
            if (status.isEmpty()) {
                issues.add(issue(RejectionReasonCode.MALFORMED_RELATIONSHIP, CanonicalEntityType.NEIGHBOUR,
                        neighbour.sourceEntityId(), "unsupported neighbour state"));
                continue;
            }
            canonical.neighbours().add(new CanonicalNeighbourRelation(
                    neighbour.sourceEntityId(),
                    neighbour.sourceDn(),
                    neighbour.canonicalSourceCellId(),
                    neighbour.canonicalTargetCellId(),
                    neighbour.relationType() == null || neighbour.relationType().isBlank()
                            ? "INTRA_FREQUENCY"
                            : neighbour.relationType(),
                    status.get()
            ));
        }
        issues.addAll(duplicateSourceIds(canonical));
        return new NormalizeResult(canonical, issues);
    }

    private static List<ValidationIssue> duplicateSourceIds(CanonicalSnapshot canonical) {
        List<ValidationIssue> issues = new ArrayList<>();
        markDuplicates(canonical.sites().stream().map(CanonicalSite::sourceEntityId).toList(),
                CanonicalEntityType.SITE, issues);
        markDuplicates(canonical.gnbs().stream().map(CanonicalGnb::sourceEntityId).toList(),
                CanonicalEntityType.GNB, issues);
        markDuplicates(canonical.cells().stream().map(CanonicalCell::sourceEntityId).toList(),
                CanonicalEntityType.CELL, issues);
        return issues;
    }

    private static void markDuplicates(List<String> ids, CanonicalEntityType type, List<ValidationIssue> issues) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                duplicates.add(id);
            }
        }
        for (String id : duplicates) {
            issues.add(issue(RejectionReasonCode.DUPLICATE_SOURCE_IDENTITY, type, id,
                    "duplicate source identity in snapshot"));
        }
    }

    private static ValidationIssue issue(
            RejectionReasonCode code, CanonicalEntityType type, String sourceEntityId, String details
    ) {
        return new ValidationIssue(code, type, sourceEntityId, details);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record NormalizeResult(CanonicalSnapshot snapshot, List<ValidationIssue> issues) {
    }
}
