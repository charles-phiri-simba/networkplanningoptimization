package com.simba.snip.npo.integration;

import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NetworkImportService {

    private final NetworkSourceAdapterRegistry adapterRegistry;
    private final CanonicalNormalizer normalizer;
    private final CanonicalValidator validator;
    private final NetworkImportBatchService batchService;
    private final NetworkReconciliationService reconciliationService;
    private final IntegrationMetrics metrics;

    public NetworkImportService(
            NetworkSourceAdapterRegistry adapterRegistry,
            CanonicalNormalizer normalizer,
            CanonicalValidator validator,
            NetworkImportBatchService batchService,
            NetworkReconciliationService reconciliationService,
            IntegrationMetrics metrics
    ) {
        this.adapterRegistry = adapterRegistry;
        this.normalizer = normalizer;
        this.validator = validator;
        this.batchService = batchService;
        this.reconciliationService = reconciliationService;
        this.metrics = metrics;
    }

    public NetworkImportBatchEntity importEricsson(FixtureKind kind) {
        return importVendor(Vendor.ERICSSON, kind, false);
    }

    public NetworkImportBatchEntity importNokia(FixtureKind kind) {
        return importVendor(Vendor.NOKIA, kind, false);
    }

    public NetworkImportBatchEntity importVendor(Vendor vendor, FixtureKind kind, boolean allowCatastrophicKind) {
        FixtureKind resolved = kind == null ? FixtureKind.NORMAL : kind;
        if (resolved == FixtureKind.CATASTROPHIC && !allowCatastrophicKind) {
            throw new DomainValidationException("catastrophic fixture kind is not importable via API");
        }
        if (vendor == Vendor.NOKIA && resolved != FixtureKind.NORMAL && resolved != FixtureKind.CONFLICT) {
            throw new DomainValidationException("Nokia fixture kind is not configured: " + resolved);
        }
        NetworkSourceAdapter adapter = adapterRegistry.require(vendor);
        Instant startedAt = Instant.now();
        NetworkImportBatchEntity batch = batchService.start(
                adapter.sourceSystem(), vendor.name(), adapter.schemaVersion(), resolved, startedAt);
        batchService.appendAudit(batch.getId(), ImportAuditEventType.IMPORT_STARTED, startedAt,
                "vendor=" + vendor + " fixtureKind=" + resolved);
        metrics.incrementStarted();
        long startedNs = System.nanoTime();
        try {
            SourceSnapshot snapshot = adapter.readSnapshot(resolved);
            if (snapshot.sourceSnapshotId() == null || snapshot.sourceSnapshotId().isBlank()
                    || snapshot.capturedAt() == null) {
                throw new IntegrationSnapshotException("snapshot metadata is incomplete");
            }
            batchService.recordSnapshot(batch.getId(), snapshot.sourceSnapshotId(), snapshot.vendorSchemaVersion());
            batchService.appendAudit(batch.getId(), ImportAuditEventType.SNAPSHOT_READ, Instant.now(),
                    "sourceSnapshotId=" + snapshot.sourceSnapshotId()
                            + " complete=" + snapshot.completeSnapshot()
                            + " entitiesRead=" + snapshot.entityCount());
            CanonicalNormalizer.NormalizeResult normalized = normalizer.normalize(snapshot);
            var issues = validator.validateAndFilter(normalized.snapshot(), normalized.issues());
            batchService.appendAudit(batch.getId(), ImportAuditEventType.VALIDATION_COMPLETED, Instant.now(),
                    "issues=" + issues.size());
            ReconciliationResult result = reconciliationService.reconcile(
                    batchService.require(batch.getId()), normalized.snapshot(), issues, Instant.now());
            batchService.appendAudit(batch.getId(), ImportAuditEventType.RECONCILIATION_COMPLETED, Instant.now(),
                    "created=" + result.created() + " updated=" + result.updated()
                            + " unchanged=" + result.unchanged() + " conflicts=" + result.conflicts());
            Instant completedAt = Instant.now();
            batchService.complete(batch.getId(), completedAt, result);
            batchService.appendAudit(batch.getId(), ImportAuditEventType.IMPORT_COMPLETED, completedAt, "status=COMPLETED");
            NetworkImportBatchEntity completed = batchService.require(batch.getId());
            metrics.recordSuccess(
                    result,
                    (System.nanoTime() - startedNs) / 1_000_000L,
                    new IntegrationMetrics.UUIDLike(
                            completed.getId().toString(),
                            completed.getSourceSystem(),
                            completed.getSourceSnapshotId()
                    )
            );
            return completed;
        } catch (RuntimeException ex) {
            Instant failedAt = Instant.now();
            batchService.fail(batch.getId(), failedAt, ex.getMessage());
            batchService.appendAudit(batch.getId(), ImportAuditEventType.IMPORT_FAILED, failedAt,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            metrics.recordFailure(batch.getId().toString(), adapter.sourceSystem(), ex.getMessage(),
                    (System.nanoTime() - startedNs) / 1_000_000L);
            return batchService.require(batch.getId());
        }
    }
}
