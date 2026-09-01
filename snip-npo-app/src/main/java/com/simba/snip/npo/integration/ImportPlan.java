package com.simba.snip.npo.integration;

import com.simba.snip.npo.persist.NetworkSourceReferenceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ImportPlan {

    public record ConflictItem(
            CanonicalEntityType type,
            String canonicalId,
            String scope,
            String currentValue,
            String incomingValue,
            String sourceEntityId,
            String sourceDn
    ) {
    }

    public record ReferenceItem(
            CanonicalEntityType type,
            String canonicalId,
            String sourceEntityId,
            String sourceDn,
            boolean authoritative
    ) {
    }

    private final int entitiesRead;
    private final List<ValidationIssue> rejections;
    private final List<CanonicalSite> siteCreates = new ArrayList<>();
    private final List<CanonicalSite> siteUpdates = new ArrayList<>();
    private final List<CanonicalGnb> gnbCreates = new ArrayList<>();
    private final List<CanonicalGnb> gnbUpdates = new ArrayList<>();
    private final List<CanonicalCell> cellCreates = new ArrayList<>();
    private final List<CanonicalCell> cellUpdates = new ArrayList<>();
    private final List<CanonicalCellConfiguration> configurationCreates = new ArrayList<>();
    private final List<CanonicalCellConfiguration> configurationUpdates = new ArrayList<>();
    private final List<CanonicalNeighbourRelation> neighbourCreates = new ArrayList<>();
    private final List<ConflictItem> conflicts = new ArrayList<>();
    private final List<ReferenceItem> references = new ArrayList<>();
    private final List<UUID> missingReferenceIds = new ArrayList<>();
    private int unchanged;
    private int conflictCount;

    public ImportPlan(int entitiesRead, List<ValidationIssue> rejections) {
        this.entitiesRead = entitiesRead;
        this.rejections = List.copyOf(rejections);
    }

    public void addSiteCreate(CanonicalSite site, ReferenceItem reference) {
        siteCreates.add(site);
        references.add(reference);
    }

    public void addSiteUpdate(CanonicalSite site, ReferenceItem reference) {
        siteUpdates.add(site);
        references.add(reference);
    }

    public void addGnbCreate(CanonicalGnb gnb, ReferenceItem reference) {
        gnbCreates.add(gnb);
        references.add(reference);
    }

    public void addGnbUpdate(CanonicalGnb gnb, ReferenceItem reference) {
        gnbUpdates.add(gnb);
        references.add(reference);
    }

    public void addCellCreate(CanonicalCell cell, ReferenceItem reference) {
        cellCreates.add(cell);
        references.add(reference);
    }

    public void addCellUpdate(CanonicalCell cell, ReferenceItem reference) {
        cellUpdates.add(cell);
        references.add(reference);
    }

    public void addConfigurationCreate(CanonicalCellConfiguration configuration, ReferenceItem reference) {
        configurationCreates.add(configuration);
        references.add(reference);
    }

    public void addConfigurationUpdate(CanonicalCellConfiguration configuration, ReferenceItem reference) {
        configurationUpdates.add(configuration);
        references.add(reference);
    }

    public void addNeighbourCreate(CanonicalNeighbourRelation neighbour, ReferenceItem reference) {
        neighbourCreates.add(neighbour);
        references.add(reference);
    }

    public void addUnchanged(ReferenceItem reference) {
        unchanged++;
        references.add(reference);
    }

    public void addConflict(ConflictItem conflict, ReferenceItem reference) {
        conflicts.add(conflict);
        references.add(reference);
        conflictCount++;
    }

    public void addMissing(NetworkSourceReferenceEntity reference) {
        missingReferenceIds.add(reference.getId());
    }

    public ReconciliationResult toResult() {
        return new ReconciliationResult(
                entitiesRead,
                siteCreates.size() + gnbCreates.size() + cellCreates.size()
                        + configurationCreates.size() + neighbourCreates.size(),
                siteUpdates.size() + gnbUpdates.size() + cellUpdates.size() + configurationUpdates.size(),
                unchanged,
                rejections.size(),
                conflictCount,
                missingReferenceIds.size()
        );
    }

    public List<ValidationIssue> rejections() {
        return rejections;
    }

    public List<CanonicalSite> siteCreates() {
        return siteCreates;
    }

    public List<CanonicalSite> siteUpdates() {
        return siteUpdates;
    }

    public List<CanonicalGnb> gnbCreates() {
        return gnbCreates;
    }

    public List<CanonicalGnb> gnbUpdates() {
        return gnbUpdates;
    }

    public List<CanonicalCell> cellCreates() {
        return cellCreates;
    }

    public List<CanonicalCell> cellUpdates() {
        return cellUpdates;
    }

    public List<CanonicalCellConfiguration> configurationCreates() {
        return configurationCreates;
    }

    public List<CanonicalCellConfiguration> configurationUpdates() {
        return configurationUpdates;
    }

    public List<CanonicalNeighbourRelation> neighbourCreates() {
        return neighbourCreates;
    }

    public List<ConflictItem> conflicts() {
        return conflicts;
    }

    public List<ReferenceItem> references() {
        return references;
    }

    public List<UUID> missingReferenceIds() {
        return missingReferenceIds;
    }
}
