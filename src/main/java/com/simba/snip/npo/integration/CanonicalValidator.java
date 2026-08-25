package com.simba.snip.npo.integration;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CanonicalValidator {

    public List<ValidationIssue> validateAndFilter(CanonicalSnapshot snapshot, List<ValidationIssue> priorIssues) {
        List<ValidationIssue> issues = new ArrayList<>(priorIssues);
        Set<String> duplicateSiteSources = duplicateIds(priorIssues, CanonicalEntityType.SITE);
        Set<String> duplicateGnbSources = duplicateIds(priorIssues, CanonicalEntityType.GNB);
        Set<String> duplicateCellSources = duplicateIds(priorIssues, CanonicalEntityType.CELL);
        snapshot.sites().removeIf(site -> duplicateSiteSources.contains(site.sourceEntityId()));
        snapshot.gnbs().removeIf(gnb -> duplicateGnbSources.contains(gnb.sourceEntityId()));
        snapshot.cells().removeIf(cell -> duplicateCellSources.contains(cell.sourceEntityId()));

        Set<String> siteIds = snapshot.sites().stream().map(CanonicalSite::canonicalSiteId).collect(Collectors.toSet());
        Set<String> gnbIds = snapshot.gnbs().stream().map(CanonicalGnb::canonicalGnbId).collect(Collectors.toSet());
        Set<String> cellIds = snapshot.cells().stream().map(CanonicalCell::canonicalCellId).collect(Collectors.toSet());

        Iterator<CanonicalGnb> gnbs = snapshot.gnbs().iterator();
        while (gnbs.hasNext()) {
            CanonicalGnb gnb = gnbs.next();
            if (!siteIds.contains(gnb.canonicalSiteId())) {
                issues.add(new ValidationIssue(
                        RejectionReasonCode.MISSING_PARENT,
                        CanonicalEntityType.GNB,
                        gnb.sourceEntityId(),
                        "parent site missing from snapshot: " + gnb.canonicalSiteId()
                ));
                gnbs.remove();
            }
        }
        gnbIds = snapshot.gnbs().stream().map(CanonicalGnb::canonicalGnbId).collect(Collectors.toSet());

        Iterator<CanonicalCell> cells = snapshot.cells().iterator();
        while (cells.hasNext()) {
            CanonicalCell cell = cells.next();
            if (!gnbIds.contains(cell.canonicalGnbId())) {
                issues.add(new ValidationIssue(
                        RejectionReasonCode.MISSING_PARENT,
                        CanonicalEntityType.CELL,
                        cell.sourceEntityId(),
                        "parent gNB missing from snapshot: " + cell.canonicalGnbId()
                ));
                cells.remove();
            }
        }
        cellIds = snapshot.cells().stream().map(CanonicalCell::canonicalCellId).collect(Collectors.toSet());

        Iterator<CanonicalCellConfiguration> configs = snapshot.configurations().iterator();
        while (configs.hasNext()) {
            CanonicalCellConfiguration configuration = configs.next();
            if (!cellIds.contains(configuration.canonicalCellId())) {
                issues.add(new ValidationIssue(
                        RejectionReasonCode.MISSING_PARENT,
                        CanonicalEntityType.CELL_CONFIGURATION,
                        configuration.sourceEntityId(),
                        "parent cell missing from snapshot: " + configuration.canonicalCellId()
                ));
                configs.remove();
            }
        }

        Iterator<CanonicalNeighbourRelation> neighbours = snapshot.neighbours().iterator();
        while (neighbours.hasNext()) {
            CanonicalNeighbourRelation neighbour = neighbours.next();
            if (!cellIds.contains(neighbour.canonicalSourceCellId())
                    || !cellIds.contains(neighbour.canonicalTargetCellId())) {
                issues.add(new ValidationIssue(
                        RejectionReasonCode.INVALID_NEIGHBOUR,
                        CanonicalEntityType.NEIGHBOUR,
                        neighbour.sourceEntityId(),
                        "neighbour endpoints missing from snapshot"
                ));
                neighbours.remove();
            }
        }
        return List.copyOf(issues);
    }

    private static Set<String> duplicateIds(List<ValidationIssue> issues, CanonicalEntityType type) {
        Set<String> ids = new HashSet<>();
        for (ValidationIssue issue : issues) {
            if (issue.reasonCode() == RejectionReasonCode.DUPLICATE_SOURCE_IDENTITY && issue.entityType() == type) {
                ids.add(issue.sourceEntityId());
            }
        }
        return ids;
    }
}
