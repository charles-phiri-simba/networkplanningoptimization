package com.simba.snip.npo.integration;

import com.simba.snip.npo.persist.CellEntity;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.GnbEntity;
import com.simba.snip.npo.persist.GnbRepository;
import com.simba.snip.npo.persist.NeighbourRelationshipEntity;
import com.simba.snip.npo.persist.NeighbourRelationshipRepository;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportRejectionEntity;
import com.simba.snip.npo.persist.NetworkImportRejectionRepository;
import com.simba.snip.npo.persist.NetworkIntegrationConflictEntity;
import com.simba.snip.npo.persist.NetworkIntegrationConflictRepository;
import com.simba.snip.npo.persist.NetworkSourceReferenceEntity;
import com.simba.snip.npo.persist.NetworkSourceReferenceRepository;
import com.simba.snip.npo.persist.RadioConfigurationEntity;
import com.simba.snip.npo.persist.RadioConfigurationRepository;
import com.simba.snip.npo.persist.SiteEntity;
import com.simba.snip.npo.persist.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class NetworkReconciliationService {

    private final SiteRepository siteRepository;
    private final GnbRepository gnbRepository;
    private final CellRepository cellRepository;
    private final RadioConfigurationRepository radioConfigurationRepository;
    private final NeighbourRelationshipRepository neighbourRelationshipRepository;
    private final NetworkSourceReferenceRepository sourceReferenceRepository;
    private final NetworkImportRejectionRepository rejectionRepository;
    private final NetworkIntegrationConflictRepository conflictRepository;

    public NetworkReconciliationService(
            SiteRepository siteRepository,
            GnbRepository gnbRepository,
            CellRepository cellRepository,
            RadioConfigurationRepository radioConfigurationRepository,
            NeighbourRelationshipRepository neighbourRelationshipRepository,
            NetworkSourceReferenceRepository sourceReferenceRepository,
            NetworkImportRejectionRepository rejectionRepository,
            NetworkIntegrationConflictRepository conflictRepository
    ) {
        this.siteRepository = siteRepository;
        this.gnbRepository = gnbRepository;
        this.cellRepository = cellRepository;
        this.radioConfigurationRepository = radioConfigurationRepository;
        this.neighbourRelationshipRepository = neighbourRelationshipRepository;
        this.sourceReferenceRepository = sourceReferenceRepository;
        this.rejectionRepository = rejectionRepository;
        this.conflictRepository = conflictRepository;
    }

    @Transactional
    public ReconciliationResult reconcile(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            List<ValidationIssue> issues,
            Instant now
    ) {
        Counters counters = new Counters(snapshot.source().entityCount(), issues.size());
        for (ValidationIssue issue : issues) {
            rejectionRepository.save(NetworkImportRejectionEntity.create(
                    UUID.randomUUID(),
                    batch.getId(),
                    issue.sourceEntityId(),
                    issue.entityType().name(),
                    issue.reasonCode().name(),
                    issue.details(),
                    now
            ));
        }
        Set<String> seen = new HashSet<>();
        for (CanonicalSite site : snapshot.sites()) {
            applySite(batch, snapshot, site, now, counters);
            seen.add(key(CanonicalEntityType.SITE, site.sourceEntityId()));
        }
        for (CanonicalGnb gnb : snapshot.gnbs()) {
            applyGnb(batch, snapshot, gnb, now, counters);
            seen.add(key(CanonicalEntityType.GNB, gnb.sourceEntityId()));
        }
        for (CanonicalCell cell : snapshot.cells()) {
            applyCell(batch, snapshot, cell, now, counters);
            seen.add(key(CanonicalEntityType.CELL, cell.sourceEntityId()));
        }
        for (CanonicalCellConfiguration configuration : snapshot.configurations()) {
            applyConfiguration(batch, snapshot, configuration, now, counters);
            seen.add(key(CanonicalEntityType.CELL_CONFIGURATION, configuration.sourceEntityId()));
        }
        for (CanonicalNeighbourRelation neighbour : snapshot.neighbours()) {
            applyNeighbour(batch, snapshot, neighbour, now, counters);
            seen.add(key(CanonicalEntityType.NEIGHBOUR, neighbour.sourceEntityId()));
        }
        if (snapshot.completeSnapshot()) {
            for (NetworkSourceReferenceEntity reference
                    : sourceReferenceRepository.findBySourceSystemAndSourceStatus(snapshot.sourceSystem(), "ACTIVE")) {
                if (!seen.contains(key(reference.getSourceEntityType(), reference.getSourceEntityId()))) {
                    reference.markMissing(now, batch.getId());
                    counters.missing++;
                }
            }
        }
        return new ReconciliationResult(
                counters.read,
                counters.created,
                counters.updated,
                counters.unchanged,
                counters.rejected,
                counters.conflicts,
                counters.missing
        );
    }

    private void applySite(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            CanonicalSite incoming,
            Instant now,
            Counters counters
    ) {
        Optional<SiteEntity> existing = siteRepository.findBySiteId(incoming.canonicalSiteId());
        NetworkSourceReferenceEntity authority = authority(CanonicalEntityType.SITE, incoming.canonicalSiteId());
        if (existing.isEmpty()) {
            siteRepository.saveAndFlush(SiteEntity.create(
                    UUID.randomUUID(),
                    incoming.canonicalSiteId(),
                    incoming.name(),
                    incoming.latitude(),
                    incoming.longitude(),
                    incoming.status()
            ));
            upsertReference(batch, snapshot, CanonicalEntityType.SITE, incoming.canonicalSiteId(),
                    incoming.sourceEntityId(), incoming.sourceDn(), true, now);
            counters.created++;
            return;
        }
        if (canWrite(authority, snapshot.sourceSystem())) {
            if (sameSite(existing.get(), incoming)) {
                counters.unchanged++;
            } else {
                existing.get().applyInventory(incoming.name(), incoming.latitude(), incoming.longitude(), incoming.status());
                counters.updated++;
            }
            upsertReference(batch, snapshot, CanonicalEntityType.SITE, incoming.canonicalSiteId(),
                    incoming.sourceEntityId(), incoming.sourceDn(), authority == null, now);
            return;
        }
        recordSecondSource(batch, snapshot, CanonicalEntityType.SITE, incoming.canonicalSiteId(),
                incoming.sourceEntityId(), incoming.sourceDn(),
                "inventory", describeSite(existing.get()), describeSite(incoming),
                sameSite(existing.get(), incoming), now, counters);
    }

    private void applyGnb(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            CanonicalGnb incoming,
            Instant now,
            Counters counters
    ) {
        Optional<GnbEntity> existing = gnbRepository.findByGnbId(incoming.canonicalGnbId());
        NetworkSourceReferenceEntity authority = authority(CanonicalEntityType.GNB, incoming.canonicalGnbId());
        if (existing.isEmpty()) {
            SiteEntity site = siteRepository.findBySiteId(incoming.canonicalSiteId()).orElseThrow();
            gnbRepository.saveAndFlush(GnbEntity.create(
                    UUID.randomUUID(),
                    incoming.canonicalGnbId(),
                    incoming.name(),
                    site,
                    incoming.equipmentVendor(),
                    incoming.model(),
                    incoming.status()
            ));
            upsertReference(batch, snapshot, CanonicalEntityType.GNB, incoming.canonicalGnbId(),
                    incoming.sourceEntityId(), incoming.sourceDn(), true, now);
            counters.created++;
            return;
        }
        if (canWrite(authority, snapshot.sourceSystem())) {
            if (sameGnb(existing.get(), incoming)) {
                counters.unchanged++;
            } else {
                existing.get().applyInventory(incoming.name(), incoming.equipmentVendor(), incoming.model(), incoming.status());
                counters.updated++;
            }
            upsertReference(batch, snapshot, CanonicalEntityType.GNB, incoming.canonicalGnbId(),
                    incoming.sourceEntityId(), incoming.sourceDn(), authority == null, now);
            return;
        }
        recordSecondSource(batch, snapshot, CanonicalEntityType.GNB, incoming.canonicalGnbId(),
                incoming.sourceEntityId(), incoming.sourceDn(),
                "inventory", describeGnb(existing.get()), describeGnb(incoming),
                sameGnb(existing.get(), incoming), now, counters);
    }

    private void applyCell(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            CanonicalCell incoming,
            Instant now,
            Counters counters
    ) {
        Optional<CellEntity> existing = cellRepository.findByCellId(incoming.canonicalCellId());
        NetworkSourceReferenceEntity authority = authority(CanonicalEntityType.CELL, incoming.canonicalCellId());
        if (existing.isEmpty()) {
            GnbEntity gnb = gnbRepository.findByGnbId(incoming.canonicalGnbId()).orElseThrow();
            cellRepository.saveAndFlush(CellEntity.create(
                    UUID.randomUUID(),
                    incoming.canonicalCellId(),
                    incoming.name(),
                    gnb,
                    incoming.technology(),
                    incoming.band(),
                    incoming.arfcn(),
                    incoming.pci(),
                    incoming.bandwidthMhz(),
                    incoming.duplexMode(),
                    incoming.status()
            ));
            upsertReference(batch, snapshot, CanonicalEntityType.CELL, incoming.canonicalCellId(),
                    incoming.sourceEntityId(), incoming.sourceDn(), true, now);
            counters.created++;
            return;
        }
        if (canWrite(authority, snapshot.sourceSystem())) {
            if (sameCell(existing.get(), incoming)) {
                counters.unchanged++;
            } else {
                existing.get().applyInventory(
                        incoming.name(),
                        incoming.technology(),
                        incoming.band(),
                        incoming.arfcn(),
                        incoming.pci(),
                        incoming.bandwidthMhz(),
                        incoming.duplexMode(),
                        incoming.status()
                );
                counters.updated++;
            }
            upsertReference(batch, snapshot, CanonicalEntityType.CELL, incoming.canonicalCellId(),
                    incoming.sourceEntityId(), incoming.sourceDn(), authority == null, now);
            return;
        }
        recordSecondSource(batch, snapshot, CanonicalEntityType.CELL, incoming.canonicalCellId(),
                incoming.sourceEntityId(), incoming.sourceDn(),
                "inventory", describeCell(existing.get()), describeCell(incoming),
                sameCell(existing.get(), incoming), now, counters);
    }

    private void applyConfiguration(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            CanonicalCellConfiguration incoming,
            Instant now,
            Counters counters
    ) {
        CellEntity cell = cellRepository.findByCellId(incoming.canonicalCellId()).orElseThrow();
        Optional<RadioConfigurationEntity> existing =
                radioConfigurationRepository.findByCell_IdAndParameterName(cell.getId(), incoming.parameterName());
        String canonicalConfigId = incoming.canonicalCellId() + ":" + incoming.parameterName();
        NetworkSourceReferenceEntity authority = authority(CanonicalEntityType.CELL_CONFIGURATION, canonicalConfigId);
        String incomingValue = CanonicalUnitNormalizer.formatDbm(incoming.txPowerDbm());
        if (existing.isEmpty()) {
            radioConfigurationRepository.saveAndFlush(RadioConfigurationEntity.create(
                    UUID.randomUUID(),
                    cell,
                    incoming.parameterName(),
                    incomingValue,
                    incoming.unit(),
                    snapshot.capturedAt()
            ));
            upsertReference(batch, snapshot, CanonicalEntityType.CELL_CONFIGURATION, canonicalConfigId,
                    incoming.sourceEntityId(), incoming.sourceDn(), true, now);
            counters.created++;
            return;
        }
        boolean same = sameConfig(existing.get(), incomingValue, incoming.unit());
        if (canWrite(authority, snapshot.sourceSystem())) {
            if (same) {
                counters.unchanged++;
            } else {
                existing.get().applyValue(incomingValue, snapshot.capturedAt());
                counters.updated++;
            }
            upsertReference(batch, snapshot, CanonicalEntityType.CELL_CONFIGURATION, canonicalConfigId,
                    incoming.sourceEntityId(), incoming.sourceDn(), authority == null, now);
            return;
        }
        recordSecondSource(batch, snapshot, CanonicalEntityType.CELL_CONFIGURATION, canonicalConfigId,
                incoming.sourceEntityId(), incoming.sourceDn(),
                incoming.parameterName(),
                existing.get().getParameterValue() + " " + existing.get().getUnit(),
                incomingValue + " " + incoming.unit(),
                same, now, counters);
    }

    private void applyNeighbour(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            CanonicalNeighbourRelation incoming,
            Instant now,
            Counters counters
    ) {
        String canonicalId = incoming.canonicalSourceCellId() + "->" + incoming.canonicalTargetCellId();
        Optional<NeighbourRelationshipEntity> existing =
                neighbourRelationshipRepository.findBySourceCell_CellIdAndTargetCell_CellId(
                        incoming.canonicalSourceCellId(), incoming.canonicalTargetCellId());
        NetworkSourceReferenceEntity authority = authority(CanonicalEntityType.NEIGHBOUR, canonicalId);
        if (existing.isEmpty()) {
            CellEntity source = cellRepository.findByCellId(incoming.canonicalSourceCellId()).orElseThrow();
            CellEntity target = cellRepository.findByCellId(incoming.canonicalTargetCellId()).orElseThrow();
            neighbourRelationshipRepository.saveAndFlush(NeighbourRelationshipEntity.create(
                    UUID.randomUUID(), source, target, incoming.relationType(), incoming.status()));
            upsertReference(batch, snapshot, CanonicalEntityType.NEIGHBOUR, canonicalId,
                    incoming.sourceEntityId(), incoming.sourceDn(), true, now);
            counters.created++;
            return;
        }
        if (canWrite(authority, snapshot.sourceSystem())) {
            counters.unchanged++;
            upsertReference(batch, snapshot, CanonicalEntityType.NEIGHBOUR, canonicalId,
                    incoming.sourceEntityId(), incoming.sourceDn(), authority == null, now);
            return;
        }
        recordSecondSource(batch, snapshot, CanonicalEntityType.NEIGHBOUR, canonicalId,
                incoming.sourceEntityId(), incoming.sourceDn(),
                "relation",
                existing.get().getRelationType() + "/" + existing.get().getStatus(),
                incoming.relationType() + "/" + incoming.status(),
                true, now, counters);
    }

    private void recordSecondSource(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            CanonicalEntityType type,
            String canonicalId,
            String sourceEntityId,
            String sourceDn,
            String scope,
            String currentValue,
            String incomingValue,
            boolean equivalent,
            Instant now,
            Counters counters
    ) {
        NetworkSourceReferenceEntity authority = authority(type, canonicalId);
        upsertReference(batch, snapshot, type, canonicalId, sourceEntityId, sourceDn, false, now);
        if (equivalent) {
            counters.unchanged++;
            return;
        }
        conflictRepository.save(NetworkIntegrationConflictEntity.create(
                UUID.randomUUID(),
                batch.getId(),
                type.name(),
                canonicalId,
                scope,
                currentValue,
                incomingValue,
                authority == null ? "UNKNOWN" : authority.getSourceSystem(),
                snapshot.sourceSystem(),
                "SECOND_SOURCE_VALUE_MISMATCH",
                now
        ));
        counters.conflicts++;
    }

    private void upsertReference(
            NetworkImportBatchEntity batch,
            CanonicalSnapshot snapshot,
            CanonicalEntityType type,
            String canonicalId,
            String sourceEntityId,
            String sourceDn,
            boolean authoritative,
            Instant now
    ) {
        Optional<NetworkSourceReferenceEntity> existing =
                sourceReferenceRepository.findByCanonicalEntityTypeAndCanonicalEntityIdAndSourceSystemAndSourceEntityId(
                        type.name(), canonicalId, snapshot.sourceSystem(), sourceEntityId);
        if (existing.isPresent()) {
            existing.get().markSeen(
                    now,
                    snapshot.sourceSnapshotId(),
                    snapshot.vendorSchemaVersion(),
                    snapshot.capturedAt(),
                    now,
                    batch.getId(),
                    "ACTIVE"
            );
            return;
        }
        sourceReferenceRepository.saveAndFlush(NetworkSourceReferenceEntity.create(
                UUID.randomUUID(),
                type.name(),
                canonicalId,
                snapshot.sourceSystem(),
                snapshot.vendor().name(),
                type.name(),
                sourceEntityId,
                sourceDn,
                authoritative,
                now,
                snapshot.sourceSnapshotId(),
                snapshot.vendorSchemaVersion(),
                snapshot.capturedAt(),
                now,
                batch.getId(),
                "ACTIVE"
        ));
    }

    private NetworkSourceReferenceEntity authority(CanonicalEntityType type, String canonicalId) {
        return sourceReferenceRepository
                .findByCanonicalEntityTypeAndCanonicalEntityIdAndAuthoritativeTrue(type.name(), canonicalId)
                .orElse(null);
    }

    private static boolean canWrite(NetworkSourceReferenceEntity authority, String incomingSource) {
        return authority == null || authority.getSourceSystem().equals(incomingSource);
    }

    private static boolean sameSite(SiteEntity existing, CanonicalSite incoming) {
        return Objects.equals(existing.getName(), incoming.name())
                && sameDouble(existing.getLatitude(), incoming.latitude())
                && sameDouble(existing.getLongitude(), incoming.longitude())
                && Objects.equals(existing.getStatus(), incoming.status());
    }

    private static boolean sameGnb(GnbEntity existing, CanonicalGnb incoming) {
        return Objects.equals(existing.getName(), incoming.name())
                && Objects.equals(existing.getVendor(), incoming.equipmentVendor())
                && Objects.equals(existing.getModel(), incoming.model())
                && Objects.equals(existing.getStatus(), incoming.status())
                && Objects.equals(existing.getSite().getSiteId(), incoming.canonicalSiteId());
    }

    private static boolean sameCell(CellEntity existing, CanonicalCell incoming) {
        return Objects.equals(existing.getName(), incoming.name())
                && Objects.equals(existing.getTechnology(), incoming.technology())
                && Objects.equals(existing.getBand(), incoming.band())
                && Objects.equals(existing.getArfcn(), incoming.arfcn())
                && Objects.equals(existing.getPci(), incoming.pci())
                && Objects.equals(existing.getBandwidthMhz(), incoming.bandwidthMhz())
                && Objects.equals(existing.getDuplexMode(), incoming.duplexMode())
                && Objects.equals(existing.getStatus(), incoming.status())
                && Objects.equals(existing.getGnb().getGnbId(), incoming.canonicalGnbId());
    }

    private static boolean sameConfig(RadioConfigurationEntity existing, String incomingValue, String unit) {
        return sameDouble(parseDouble(existing.getParameterValue()), parseDouble(incomingValue))
                && Objects.equals(existing.getUnit(), unit);
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private static boolean sameDouble(Double left, Double right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Math.abs(left - right) < 1.0e-9d;
    }

    private static String describeSite(SiteEntity site) {
        return site.getName() + "@" + site.getLatitude() + "," + site.getLongitude() + "/" + site.getStatus();
    }

    private static String describeSite(CanonicalSite site) {
        return site.name() + "@" + site.latitude() + "," + site.longitude() + "/" + site.status();
    }

    private static String describeGnb(GnbEntity gnb) {
        return gnb.getName() + "/" + gnb.getVendor() + "/" + gnb.getModel() + "/" + gnb.getStatus();
    }

    private static String describeGnb(CanonicalGnb gnb) {
        return gnb.name() + "/" + gnb.equipmentVendor() + "/" + gnb.model() + "/" + gnb.status();
    }

    private static String describeCell(CellEntity cell) {
        return cell.getName() + "/" + cell.getTechnology() + "/" + cell.getPci() + "/" + cell.getStatus();
    }

    private static String describeCell(CanonicalCell cell) {
        return cell.name() + "/" + cell.technology() + "/" + cell.pci() + "/" + cell.status();
    }

    private static String key(CanonicalEntityType type, String sourceEntityId) {
        return key(type.name(), sourceEntityId);
    }

    private static String key(String type, String sourceEntityId) {
        return type + ":" + sourceEntityId;
    }

    private static final class Counters {
        private final int read;
        private final int rejected;
        private int created;
        private int updated;
        private int unchanged;
        private int conflicts;
        private int missing;

        private Counters(int read, int rejected) {
            this.read = read;
            this.rejected = rejected;
        }
    }
}
