package com.simba.snip.npo.integration;

import com.simba.snip.npo.config.EnmIntegrationProperties;
import com.simba.snip.npo.config.IntegrationRuntimeProperties;
import com.simba.snip.npo.domain.DomainValidationException;
import com.simba.snip.npo.domain.ImportBusyException;
import com.simba.snip.npo.integration.enm.ConnectorCancellationToken;
import com.simba.snip.npo.integration.enm.EnmConnectorMetrics;
import com.simba.snip.npo.integration.enm.EnmImportTestHooks;
import com.simba.snip.npo.integration.enm.EricssonEnmConnector;
import com.simba.snip.npo.integration.enm.ImportExecutionContext;
import com.simba.snip.npo.integration.enm.SnapshotCompleteness;
import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.enm.VendorSnapshot;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorMode;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import com.simba.snip.npo.integration.security.ConnectorSecurityException;
import com.simba.snip.npo.integration.security.ConnectorSecurityMetrics;
import com.simba.snip.npo.integration.security.SecureConnectorClientFactory;
import com.simba.snip.npo.integration.security.SecureVendorSnapshotParser;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.persist.SourceProvenanceEntity;
import com.simba.snip.npo.persist.VendorSnapshotEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NetworkImportService {

    public static final String DEFAULT_SCOPE = IntegrationRuntimeProperties.DEFAULT_SCOPE;

    private final NetworkSourceAdapterRegistry adapterRegistry;
    private final CanonicalNormalizer normalizer;
    private final CanonicalValidator validator;
    private final CanonicalSnapshotHasher hasher;
    private final NetworkImportBatchService batchService;
    private final NetworkImportBatchRepository batchRepository;
    private final NetworkReconciliationService reconciliationService;
    private final ImportLeaseService leaseService;
    private final ImportExecutionGuard executionGuard;
    private final ImportFaultInjector faultInjector;
    private final IntegrationRuntimeIdentity identity;
    private final IntegrationRuntimeProperties properties;
    private final IntegrationMetrics metrics;
    private final ConnectorRegistry connectorRegistry;
    private final SecureConnectorClientFactory connectorClientFactory;
    private final SecureVendorSnapshotParser snapshotParser;
    private final ConnectorSecurityMetrics connectorSecurityMetrics;
    private final EricssonEnmConnector enmConnector;
    private final VendorImportAuthorizer vendorImportAuthorizer;
    private final EnmIntegrationProperties enmProperties;
    private final EnmConnectorMetrics enmMetrics;
    private final EnmImportTestHooks enmImportTestHooks;

    public NetworkImportService(
            NetworkSourceAdapterRegistry adapterRegistry,
            CanonicalNormalizer normalizer,
            CanonicalValidator validator,
            CanonicalSnapshotHasher hasher,
            NetworkImportBatchService batchService,
            NetworkImportBatchRepository batchRepository,
            NetworkReconciliationService reconciliationService,
            ImportLeaseService leaseService,
            ImportExecutionGuard executionGuard,
            ImportFaultInjector faultInjector,
            IntegrationRuntimeIdentity identity,
            IntegrationRuntimeProperties properties,
            IntegrationMetrics metrics,
            ConnectorRegistry connectorRegistry,
            SecureConnectorClientFactory connectorClientFactory,
            SecureVendorSnapshotParser snapshotParser,
            ConnectorSecurityMetrics connectorSecurityMetrics,
            EricssonEnmConnector enmConnector,
            VendorImportAuthorizer vendorImportAuthorizer,
            EnmIntegrationProperties enmProperties,
            EnmConnectorMetrics enmMetrics,
            EnmImportTestHooks enmImportTestHooks
    ) {
        this.adapterRegistry = adapterRegistry;
        this.normalizer = normalizer;
        this.validator = validator;
        this.hasher = hasher;
        this.batchService = batchService;
        this.batchRepository = batchRepository;
        this.reconciliationService = reconciliationService;
        this.leaseService = leaseService;
        this.executionGuard = executionGuard;
        this.faultInjector = faultInjector;
        this.identity = identity;
        this.properties = properties;
        this.metrics = metrics;
        this.connectorRegistry = connectorRegistry;
        this.connectorClientFactory = connectorClientFactory;
        this.snapshotParser = snapshotParser;
        this.connectorSecurityMetrics = connectorSecurityMetrics;
        this.enmConnector = enmConnector;
        this.vendorImportAuthorizer = vendorImportAuthorizer;
        this.enmProperties = enmProperties;
        this.enmMetrics = enmMetrics;
        this.enmImportTestHooks = enmImportTestHooks;
    }

    public NetworkImportBatchEntity importEricsson(FixtureKind kind) {
        return importVendor(Vendor.ERICSSON, kind, false);
    }

    public NetworkImportBatchEntity importNokia(FixtureKind kind) {
        return importVendor(Vendor.NOKIA, kind, false);
    }

    public NetworkImportBatchEntity importSecure(String connectorId) {
        ConnectorDefinition definition = connectorRegistry.require(connectorId);
        if (definition.mode() == ConnectorMode.SIMULATOR || definition.mode() == ConnectorMode.REAL) {
            return importEnm(definition);
        }
        String sourceSystem = definition.sourceSystem();
        String sourceScope = definition.sourceScope();
        String schemaVersion = definition.vendor() == Vendor.ERICSSON
                ? "ERICSSON_SECURE_MOCK_V1"
                : "NOKIA_SECURE_MOCK_V1";
        Instant requestedAt = Instant.now();
        leaseService.recoverExpired(sourceSystem, sourceScope);
        Optional<NetworkImportBatchEntity> active = activeExecution(sourceSystem, sourceScope);
        if (active.isPresent()) {
            metrics.incrementConcurrentRejected();
            throw new ImportBusyException(
                    "import already RUNNING for " + sourceSystem + "/" + sourceScope,
                    active.get().getId(),
                    ImportFailureCode.LEASE_UNAVAILABLE.name()
            );
        }
        UUID executionId = UUID.randomUUID();
        batchService.requested(
                executionId,
                sourceSystem,
                definition.vendor().name(),
                schemaVersion,
                FixtureKind.NORMAL,
                sourceScope,
                ImportExecutionType.NEW,
                1,
                null,
                requestedAt,
                identity.instanceId()
        );
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_STARTED, requestedAt,
                "connectorId=" + definition.connectorId() + " sourceScope=" + sourceScope);
        metrics.incrementStarted();
        Optional<ImportLease> lease = leaseService.acquire(sourceSystem, sourceScope, executionId, identity.instanceId());
        if (lease.isEmpty()) {
            UUID owner = leaseService.find(sourceSystem, sourceScope).map(ImportLease::ownerExecutionId).orElse(executionId);
            batchService.terminalize(
                    executionId,
                    ImportExecutionStatus.REJECTED.name(),
                    Instant.now(),
                    ImportFailureCode.LEASE_UNAVAILABLE,
                    true,
                    "lease unavailable for " + sourceSystem + "/" + sourceScope
            );
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REJECTED, Instant.now(),
                    "failureCode=LEASE_UNAVAILABLE");
            metrics.incrementConcurrentRejected();
            connectorSecurityMetrics.incrementMultiReplicaLeaseContention();
            throw new ImportBusyException(
                    "import lease unavailable for " + sourceSystem + "/" + sourceScope,
                    owner,
                    ImportFailureCode.LEASE_UNAVAILABLE.name()
            );
        }
        batchService.markRunning(executionId, Instant.now(), lease.get().fencingToken());
        batchService.appendAudit(executionId, ImportAuditEventType.LEASE_ACQUIRED, Instant.now(),
                "fencingToken=" + lease.get().fencingToken() + " ownerInstanceId=" + identity.instanceId());
        Closeable guard = executionGuard.start(executionId, lease.get());
        long startedNs = System.nanoTime();
        SecureConnectorClientFactory.OpenSession session = null;
        SourceSnapshot snapshot = null;
        try {
            session = connectorClientFactory.open(definition, executionId);
            byte[] body = session.client().readInventory();
            snapshot = snapshotParser.parse(definition, body);
            CanonicalNormalizer.NormalizeResult normalized = normalizer.normalize(snapshot);
            String hash = hasher.hash(normalized.snapshot());
            SnapshotClassification classification = classify(sourceSystem, sourceScope, snapshot.sourceSnapshotId(), hash);
            batchService.recordSnapshot(executionId, snapshot.sourceSnapshotId(), snapshot.vendorSchemaVersion(), hash);
            if (classification.type() == ImportExecutionType.REPLAY) {
                connectorClientFactory.complete(session, true, null);
                leaseService.release(lease.get());
                metrics.incrementReplays();
                NetworkImportBatchEntity replayed = batchService.completeReplay(
                        executionId,
                        Instant.now(),
                        classification.originalSuccessfulExecutionId(),
                        snapshot.sourceSnapshotId(),
                        snapshot.vendorSchemaVersion(),
                        hash
                );
                batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REPLAYED, Instant.now(),
                        "originalSuccessfulExecutionId=" + classification.originalSuccessfulExecutionId());
                return replayed;
            }
            if (classification.rejected()) {
                connectorClientFactory.complete(session, true, null);
                batchService.terminalize(
                        executionId,
                        ImportExecutionStatus.REJECTED.name(),
                        Instant.now(),
                        classification.failureCode(),
                        false,
                        classification.detail()
                );
                batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REJECTED, Instant.now(),
                        "failureCode=" + classification.failureCode());
                return batchService.require(executionId);
            }
            NetworkImportBatchEntity running = batchService.require(executionId);
            if (!"RUNNING".equals(running.getStatus())) {
                throw new ImportRuntimeException(ImportFailureCode.EXECUTION_TIMEOUT, "execution is no longer RUNNING");
            }
            Instant now = Instant.now();
            batchService.appendCheckpoint(executionId, ImportCheckpointType.SNAPSHOT_READ, now,
                    "sourceSnapshotId=" + snapshot.sourceSnapshotId()
                            + " complete=" + snapshot.completeSnapshot()
                            + " entitiesRead=" + snapshot.entityCount());
            batchService.appendAudit(executionId, ImportAuditEventType.SNAPSHOT_READ, now,
                    "sourceSnapshotId=" + snapshot.sourceSnapshotId()
                            + " hash=" + hash);
            batchService.appendCheckpoint(executionId, ImportCheckpointType.NORMALIZATION_COMPLETED, Instant.now(),
                    "canonicalHash=" + hash);
            var issues = validator.validateAndFilter(normalized.snapshot(), normalized.issues());
            batchService.appendCheckpoint(executionId, ImportCheckpointType.VALIDATION_COMPLETED, Instant.now(),
                    "issues=" + issues.size());
            ImportPlan plan = reconciliationService.plan(normalized.snapshot(), issues, Instant.now());
            batchService.appendCheckpoint(executionId, ImportCheckpointType.RECONCILIATION_COMPLETED, Instant.now(),
                    "created=" + plan.toResult().created() + " updated=" + plan.toResult().updated());
            leaseService.assertOwnership(lease.get());
            running = batchService.require(executionId);
            ReconciliationResult result = reconciliationService.apply(
                    running, normalized.snapshot(), plan, Instant.now(), lease.get());
            batchService.appendCheckpoint(executionId, ImportCheckpointType.CANONICAL_COMMIT_COMPLETED, Instant.now(),
                    "created=" + result.created() + " updated=" + result.updated());
            Instant completedAt = Instant.now();
            boolean completed = batchService.complete(executionId, completedAt, result);
            if (!completed) {
                throw new ImportRuntimeException(ImportFailureCode.EXECUTION_TIMEOUT, "execution left RUNNING before complete");
            }
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_COMPLETED, completedAt, "status=COMPLETED");
            connectorClientFactory.complete(session, true, null);
            NetworkImportBatchEntity done = batchService.require(executionId);
            metrics.recordSuccess(
                    result,
                    (System.nanoTime() - startedNs) / 1_000_000L,
                    new IntegrationMetrics.UUIDLike(
                            done.getId().toString(),
                            done.getSourceSystem(),
                            done.getSourceSnapshotId(),
                            done.getSourceScope(),
                            done.getLeaseFencingToken()
                    )
            );
            return done;
        } catch (RuntimeException ex) {
            ImportFailureCode code = failureCode(ex);
            if (session != null) {
                connectorClientFactory.complete(session, false, code);
            }
            boolean retryable = ImportRuntimeException.retryableDefault(code);
            Instant failedAt = Instant.now();
            String status = code == ImportFailureCode.EXECUTION_TIMEOUT
                    ? ImportExecutionStatus.TIMED_OUT.name()
                    : ImportExecutionStatus.FAILED.name();
            String safeMessage = ex instanceof ConnectorSecurityException
                    ? ex.getMessage()
                    : code.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
            boolean updated = batchService.terminalize(executionId, status, failedAt, code, retryable, safeMessage);
            if (updated) {
                batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_FAILED, failedAt, code.name());
                metrics.recordFailure(executionId.toString(), sourceSystem, sourceScope,
                        snapshot == null ? "UNREAD" : snapshot.sourceSnapshotId(),
                        code, (System.nanoTime() - startedNs) / 1_000_000L);
            }
            return batchService.require(executionId);
        } finally {
            try {
                guard.close();
            } catch (Exception ignored) {
                // already cancelled
            }
            leaseService.release(lease.get());
            batchService.appendAudit(executionId, ImportAuditEventType.LEASE_RELEASED, Instant.now(),
                    "fencingToken=" + lease.get().fencingToken());
        }
    }

    public NetworkImportBatchEntity importEnm(ConnectorDefinition definition) {
        vendorImportAuthorizer.requireTrigger();
        if (!definition.enabled() || !enmProperties.isEnabled()) {
            throw new ConnectorSecurityException(ImportFailureCode.CONNECTOR_DISABLED, "ENM connector is disabled");
        }
        if (enmConnector.descriptor(definition).capabilities().stream()
                .anyMatch(com.simba.snip.npo.integration.security.ConnectorCapability::mutatesNetwork)) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED, "ENM connector is not read-only");
        }
        String sourceSystem = definition.sourceSystem();
        String sourceScope = definition.sourceScope();
        Instant requestedAt = Instant.now();
        leaseService.recoverExpired(sourceSystem, sourceScope);
        Optional<NetworkImportBatchEntity> active = activeExecution(sourceSystem, sourceScope);
        if (active.isPresent()) {
            metrics.incrementConcurrentRejected();
            throw new ImportBusyException(
                    "import already RUNNING for " + sourceSystem + "/" + sourceScope,
                    active.get().getId(),
                    ImportFailureCode.LEASE_UNAVAILABLE.name()
            );
        }
        UUID executionId = UUID.randomUUID();
        batchService.requested(
                executionId,
                sourceSystem,
                definition.vendor().name(),
                "ENM_SIMULATOR_V1",
                FixtureKind.NORMAL,
                sourceScope,
                ImportExecutionType.NEW,
                1,
                null,
                requestedAt,
                identity.instanceId()
        );
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REQUESTED, requestedAt,
                "connectorId=" + definition.connectorId());
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_STARTED, requestedAt,
                "connectorId=" + definition.connectorId() + " sourceScope=" + sourceScope);
        metrics.incrementStarted();
        Optional<ImportLease> lease = leaseService.acquire(sourceSystem, sourceScope, executionId, identity.instanceId());
        if (lease.isEmpty()) {
            UUID owner = leaseService.find(sourceSystem, sourceScope).map(ImportLease::ownerExecutionId).orElse(executionId);
            batchService.terminalize(
                    executionId,
                    ImportExecutionStatus.REJECTED.name(),
                    Instant.now(),
                    ImportFailureCode.LEASE_UNAVAILABLE,
                    true,
                    "lease unavailable for " + sourceSystem + "/" + sourceScope
            );
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REJECTED, Instant.now(),
                    "failureCode=LEASE_UNAVAILABLE");
            metrics.incrementConcurrentRejected();
            throw new ImportBusyException(
                    "import lease unavailable for " + sourceSystem + "/" + sourceScope,
                    owner,
                    ImportFailureCode.LEASE_UNAVAILABLE.name()
            );
        }
        batchService.markRunning(executionId, Instant.now(), lease.get().fencingToken());
        batchService.appendAudit(executionId, ImportAuditEventType.LEASE_ACQUIRED, Instant.now(),
                "fencingToken=" + lease.get().fencingToken() + " ownerInstanceId=" + identity.instanceId());
        Closeable guard = executionGuard.start(executionId, lease.get());
        long startedNs = System.nanoTime();
        VendorSnapshot vendorSnapshot = null;
        SourceSnapshot snapshot = null;
        try {
            ConnectorCancellationToken cancellationToken = new ConnectorCancellationToken();
            enmImportTestHooks.bind(cancellationToken);
            ImportExecutionContext context = new ImportExecutionContext(
                    executionId,
                    Instant.now().plus(enmProperties.getOverallExecutionTimeout()),
                    lease.get(),
                    leaseService,
                    cancellationToken,
                    enmProperties.getRequestTimeout()
            );
            context.assertContinuing();
            batchService.appendAudit(executionId, ImportAuditEventType.SNAPSHOT_STARTED, Instant.now(),
                    "connectorId=" + definition.connectorId());
            EricssonEnmConnector.AcquisitionResult acquired = enmConnector.acquire(definition, context);
            vendorSnapshot = acquired.vendorSnapshot();
            snapshot = acquired.sourceSnapshot();
            persistVendorSnapshot(vendorSnapshot);
            batchService.recordSnapshot(
                    executionId,
                    snapshot.sourceSnapshotId(),
                    snapshot.vendorSchemaVersion(),
                    hasher.hash(normalizer.normalize(snapshot).snapshot())
            );
            if (vendorSnapshot.completeness() != SnapshotCompleteness.COMPLETE) {
                return failClosedWithoutReconcile(
                        executionId, sourceSystem, sourceScope, snapshot, vendorSnapshot, startedNs);
            }
            CanonicalNormalizer.NormalizeResult normalized = normalizer.normalize(snapshot);
            String hash = hasher.hash(normalized.snapshot());
            SnapshotClassification classification = classify(
                    sourceSystem, sourceScope, snapshot.sourceSnapshotId(), hash);
            if (classification.type() == ImportExecutionType.REPLAY) {
                leaseService.release(lease.get());
                metrics.incrementReplays();
                NetworkImportBatchEntity replayed = batchService.completeReplay(
                        executionId,
                        Instant.now(),
                        classification.originalSuccessfulExecutionId(),
                        snapshot.sourceSnapshotId(),
                        snapshot.vendorSchemaVersion(),
                        hash
                );
                batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REPLAYED, Instant.now(),
                        "originalSuccessfulExecutionId=" + classification.originalSuccessfulExecutionId());
                return replayed;
            }
            if (classification.rejected()) {
                batchService.terminalize(
                        executionId,
                        ImportExecutionStatus.REJECTED.name(),
                        Instant.now(),
                        classification.failureCode(),
                        false,
                        classification.detail()
                );
                batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REJECTED, Instant.now(),
                        "failureCode=" + classification.failureCode());
                return batchService.require(executionId);
            }
            var issues = validator.validateAndFilter(normalized.snapshot(), normalized.issues());
            ImportPlan plan = reconciliationService.plan(normalized.snapshot(), issues, Instant.now());
            enmImportTestHooks.runBeforeReconcile();
            context.assertContinuing();
            leaseService.assertOwnership(lease.get());
            NetworkImportBatchEntity running = batchService.require(executionId);
            if (!"RUNNING".equals(running.getStatus())) {
                throw new ImportRuntimeException(
                        ImportFailureCode.LEASE_LOST, "execution is no longer RUNNING", false, null);
            }
            ReconciliationResult result = reconciliationService.apply(
                    running, normalized.snapshot(), plan, Instant.now(), lease.get());
            persistProvenance(executionId, vendorSnapshot, snapshot, Instant.now());
            Instant completedAt = Instant.now();
            boolean completed = batchService.complete(executionId, completedAt, result);
            if (!completed) {
                throw new ImportRuntimeException(
                        ImportFailureCode.EXECUTION_TIMEOUT, "execution left RUNNING before complete");
            }
            batchService.appendAudit(executionId, ImportAuditEventType.SNAPSHOT_COMPLETED, completedAt,
                    "completeness=COMPLETE pagesReceived=" + vendorSnapshot.pagesReceived()
                            + " entitiesRead=" + vendorSnapshot.entitiesRead());
            batchService.appendAudit(executionId, ImportAuditEventType.RECONCILIATION_COMPLETED, completedAt,
                    "created=" + result.created() + " updated=" + result.updated());
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_COMPLETED, completedAt, "status=COMPLETED");
            NetworkImportBatchEntity done = batchService.require(executionId);
            metrics.recordSuccess(
                    result,
                    (System.nanoTime() - startedNs) / 1_000_000L,
                    new IntegrationMetrics.UUIDLike(
                            done.getId().toString(),
                            done.getSourceSystem(),
                            done.getSourceSnapshotId(),
                            done.getSourceScope(),
                            done.getLeaseFencingToken()
                    )
            );
            return done;
        } catch (ImportBusyException ex) {
            throw ex;
        } catch (EricssonEnmConnector.AcquisitionFailedException ex) {
            vendorSnapshot = ex.vendorSnapshot();
            snapshot = ex.sourceSnapshot();
            return failClosedFromException(executionId, sourceSystem, sourceScope, snapshot, vendorSnapshot, ex, startedNs);
        } catch (RuntimeException ex) {
            return failClosedFromException(executionId, sourceSystem, sourceScope, snapshot, vendorSnapshot, ex, startedNs);
        } finally {
            try {
                guard.close();
            } catch (Exception ignored) {
                // already cancelled
            }
            leaseService.release(lease.get());
            batchService.appendAudit(executionId, ImportAuditEventType.LEASE_RELEASED, Instant.now(),
                    "fencingToken=" + lease.get().fencingToken());
            enmImportTestHooks.clear();
        }
    }

    private NetworkImportBatchEntity failClosedWithoutReconcile(
            UUID executionId,
            String sourceSystem,
            String sourceScope,
            SourceSnapshot snapshot,
            VendorSnapshot vendorSnapshot,
            long startedNs
    ) {
        ImportFailureCode code = vendorSnapshot.completeness() == SnapshotCompleteness.PARTIAL
                ? ImportFailureCode.SNAPSHOT_PARTIAL
                : ImportFailureCode.SNAPSHOT_READ_FAILED;
        if (vendorSnapshot.completeness() == SnapshotCompleteness.PARTIAL) {
            enmMetrics.incrementSnapshotPartial();
            batchService.appendAudit(executionId, ImportAuditEventType.SNAPSHOT_PARTIAL, Instant.now(),
                    "completeness=PARTIAL pagesReceived=" + vendorSnapshot.pagesReceived());
        } else {
            enmMetrics.incrementSnapshotFailed();
        }
        Instant failedAt = Instant.now();
        batchService.terminalize(
                executionId,
                ImportExecutionStatus.FAILED.name(),
                failedAt,
                code,
                false,
                safeFailureMessage(code)
        );
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_FAILED, failedAt, code.name());
        metrics.recordFailure(
                executionId.toString(),
                sourceSystem,
                sourceScope,
                snapshot == null ? "UNREAD" : snapshot.sourceSnapshotId(),
                code,
                (System.nanoTime() - startedNs) / 1_000_000L
        );
        return batchService.require(executionId);
    }

    private NetworkImportBatchEntity failClosedFromException(
            UUID executionId,
            String sourceSystem,
            String sourceScope,
            SourceSnapshot snapshot,
            VendorSnapshot vendorSnapshot,
            RuntimeException ex,
            long startedNs
    ) {
        ImportFailureCode code = failureCode(ex);
        if (vendorSnapshot != null && vendorSnapshot.completeness() == SnapshotCompleteness.PARTIAL) {
            code = ImportFailureCode.SNAPSHOT_PARTIAL;
        }
        if (vendorSnapshot != null) {
            try {
                persistVendorSnapshot(vendorSnapshot);
            } catch (DataAccessException ignored) {
                // snapshot metadata is diagnostic; do not replace the original failure code
            }
        }
        boolean retryable = vendorRetryable(code, ex);
        Instant failedAt = Instant.now();
        String status = code == ImportFailureCode.EXECUTION_TIMEOUT
                ? ImportExecutionStatus.TIMED_OUT.name()
                : ImportExecutionStatus.FAILED.name();
        try {
            appendVendorFailureAudit(executionId, code);
        } catch (DataAccessException ignored) {
            // audit is diagnostic
        }
        boolean updated = batchService.terminalize(
                executionId, status, failedAt, code, retryable, safeFailureMessage(code));
        if (updated) {
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_FAILED, failedAt, code.name());
            metrics.recordFailure(
                    executionId.toString(),
                    sourceSystem,
                    sourceScope,
                    snapshot == null ? "UNREAD" : snapshot.sourceSnapshotId(),
                    code,
                    (System.nanoTime() - startedNs) / 1_000_000L
            );
        }
        return batchService.require(executionId);
    }

    private void persistVendorSnapshot(VendorSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        String warnings = snapshot.warnings().stream().collect(Collectors.joining(","));
        if (warnings.length() > 256) {
            warnings = warnings.substring(0, 256);
        }
        batchService.persistVendorSnapshot(VendorSnapshotEntity.create(
                UUID.randomUUID(),
                snapshot.executionId(),
                snapshot.snapshotId(),
                snapshot.connectorId(),
                snapshot.sourceVendor(),
                snapshot.sourceSystem(),
                snapshot.startedAt(),
                snapshot.completedAt(),
                snapshot.completeness().name(),
                snapshot.pagesReceived(),
                snapshot.entitiesRead(),
                snapshot.sourceVersion(),
                warnings.isBlank() ? null : warnings
        ));
    }

    private void persistProvenance(
            UUID executionId,
            VendorSnapshot snapshot,
            SourceSnapshot source,
            Instant observedAt
    ) {
        java.util.ArrayList<SourceProvenanceEntity> rows = new java.util.ArrayList<>();
        for (SourceSite site : source.sites()) {
            rows.add(SourceProvenanceEntity.create(
                    UUID.randomUUID(),
                    "SITE",
                    site.canonicalSiteId(),
                    snapshot.sourceVendor(),
                    snapshot.sourceSystem(),
                    "ManagedElement",
                    site.sourceEntityId(),
                    snapshot.snapshotId(),
                    observedAt,
                    executionId
            ));
        }
        for (SourceGnb gnb : source.gnbs()) {
            rows.add(SourceProvenanceEntity.create(
                    UUID.randomUUID(),
                    "GNB",
                    gnb.canonicalGnbId(),
                    snapshot.sourceVendor(),
                    snapshot.sourceSystem(),
                    "RadioFunction",
                    gnb.sourceEntityId(),
                    snapshot.snapshotId(),
                    observedAt,
                    executionId
            ));
        }
        for (SourceCell cell : source.cells()) {
            rows.add(SourceProvenanceEntity.create(
                    UUID.randomUUID(),
                    "CELL",
                    cell.canonicalCellId(),
                    snapshot.sourceVendor(),
                    snapshot.sourceSystem(),
                    "Cell",
                    cell.sourceEntityId(),
                    snapshot.snapshotId(),
                    observedAt,
                    executionId
            ));
        }
        batchService.persistProvenance(rows);
    }

    private void appendVendorFailureAudit(UUID executionId, ImportFailureCode code) {
        ImportAuditEventType type = switch (code) {
            case VENDOR_RATE_LIMITED -> ImportAuditEventType.VENDOR_RATE_LIMITED;
            case VENDOR_TIMEOUT -> ImportAuditEventType.VENDOR_TIMEOUT;
            case VENDOR_AUTHENTICATION_FAILED -> ImportAuditEventType.VENDOR_AUTHENTICATION_FAILED;
            case VENDOR_AUTHORIZATION_DENIED -> ImportAuditEventType.VENDOR_AUTHORIZATION_DENIED;
            case VENDOR_PROTOCOL_ERROR, VENDOR_RESPONSE_INVALID, VENDOR_PAGINATION_INVALID ->
                    ImportAuditEventType.VENDOR_PROTOCOL_ERROR;
            case LEASE_LOST -> ImportAuditEventType.LEASE_LOST;
            case CONNECTOR_CANCELLED -> ImportAuditEventType.CONNECTOR_CANCELLED;
            case SNAPSHOT_PARTIAL -> ImportAuditEventType.SNAPSHOT_PARTIAL;
            default -> ImportAuditEventType.IMPORT_FAILED;
        };
        if (type != ImportAuditEventType.IMPORT_FAILED) {
            batchService.appendAudit(executionId, type, Instant.now(), "failureCode=" + code.name());
        }
    }

    private static boolean vendorRetryable(ImportFailureCode code, RuntimeException ex) {
        if (ex instanceof ImportRuntimeException runtime) {
            if (code == ImportFailureCode.LEASE_LOST
                    || code == ImportFailureCode.CONNECTOR_CANCELLED
                    || code == ImportFailureCode.SNAPSHOT_PARTIAL
                    || code == ImportFailureCode.PRODUCTION_TRANSPORT_NOT_CONFIGURED) {
                return false;
            }
            return runtime.retryable();
        }
        return ImportRuntimeException.retryableDefault(code);
    }

    private static String safeFailureMessage(ImportFailureCode code) {
        return code.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public NetworkImportBatchEntity importVendor(Vendor vendor, FixtureKind kind, boolean allowCatastrophicKind) {
        FixtureKind resolved = kind == null ? FixtureKind.NORMAL : kind;
        if (!allowCatastrophicKind && isTestOnlyKind(resolved)) {
            throw new DomainValidationException("fixture kind is not importable via API: " + resolved);
        }
        if (vendor == Vendor.NOKIA && resolved != FixtureKind.NORMAL && resolved != FixtureKind.CONFLICT) {
            throw new DomainValidationException("Nokia fixture kind is not configured: " + resolved);
        }
        NetworkSourceAdapter adapter = adapterRegistry.require(vendor);
        String sourceSystem = adapter.sourceSystem();
        String sourceScope = DEFAULT_SCOPE;
        Instant requestedAt = Instant.now();
        leaseService.recoverExpired(sourceSystem, sourceScope);

        Optional<NetworkImportBatchEntity> active = activeExecution(sourceSystem, sourceScope);
        if (active.isPresent()) {
            metrics.incrementConcurrentRejected();
            throw new ImportBusyException(
                    "import already RUNNING for " + sourceSystem + "/" + sourceScope,
                    active.get().getId(),
                    ImportFailureCode.LEASE_UNAVAILABLE.name()
            );
        }

        SourceSnapshot snapshot;
        try {
            snapshot = adapter.readSnapshot(resolved);
        } catch (RuntimeException ex) {
            return persistReadFailure(adapter, vendor, resolved, sourceScope, requestedAt, ex);
        }
        if (snapshot.sourceSnapshotId() == null || snapshot.sourceSnapshotId().isBlank() || snapshot.capturedAt() == null) {
            return persistReadFailure(adapter, vendor, resolved, sourceScope, requestedAt,
                    new IntegrationSnapshotException("snapshot metadata is incomplete"));
        }

        CanonicalNormalizer.NormalizeResult normalized = normalizer.normalize(snapshot);
        String hash = hasher.hash(normalized.snapshot());
        SnapshotClassification classification = classify(sourceSystem, sourceScope, snapshot.sourceSnapshotId(), hash);

        if (classification.type() == ImportExecutionType.REPLAY) {
            return persistReplay(adapter, vendor, resolved, sourceScope, requestedAt, snapshot, hash, classification);
        }
        if (classification.rejected()) {
            return persistRejected(adapter, vendor, resolved, sourceScope, requestedAt, snapshot, hash, classification);
        }

        UUID executionId = UUID.randomUUID();
        NetworkImportBatchEntity execution = batchService.requested(
                executionId,
                sourceSystem,
                vendor.name(),
                adapter.schemaVersion(),
                resolved,
                sourceScope,
                classification.type(),
                classification.attemptNumber(),
                classification.previousExecutionId(),
                requestedAt,
                identity.instanceId()
        );
        batchService.recordSnapshot(executionId, snapshot.sourceSnapshotId(), snapshot.vendorSchemaVersion(), hash);
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_STARTED, requestedAt,
                "vendor=" + vendor + " fixtureKind=" + resolved
                        + " executionType=" + classification.type()
                        + " sourceScope=" + sourceScope);
        metrics.incrementStarted();
        if (classification.type() == ImportExecutionType.RETRY) {
            metrics.incrementRetries();
        }

        Optional<ImportLease> lease = leaseService.acquire(sourceSystem, sourceScope, executionId, identity.instanceId());
        if (lease.isEmpty()) {
            UUID owner = leaseService.find(sourceSystem, sourceScope).map(ImportLease::ownerExecutionId).orElse(executionId);
            batchService.terminalize(
                    executionId,
                    ImportExecutionStatus.REJECTED.name(),
                    Instant.now(),
                    ImportFailureCode.LEASE_UNAVAILABLE,
                    true,
                    "lease unavailable for " + sourceSystem + "/" + sourceScope
            );
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REJECTED, Instant.now(),
                    "failureCode=LEASE_UNAVAILABLE");
            metrics.incrementConcurrentRejected();
            throw new ImportBusyException(
                    "import lease unavailable for " + sourceSystem + "/" + sourceScope,
                    owner,
                    ImportFailureCode.LEASE_UNAVAILABLE.name()
            );
        }

        SnapshotClassification afterLease = classify(sourceSystem, sourceScope, snapshot.sourceSnapshotId(), hash);
        if (afterLease.type() == ImportExecutionType.REPLAY && afterLease.originalSuccessfulExecutionId() != null
                && !afterLease.originalSuccessfulExecutionId().equals(executionId)) {
            leaseService.release(lease.get());
            metrics.incrementReplays();
            NetworkImportBatchEntity replayed = batchService.completeReplay(
                    executionId,
                    Instant.now(),
                    afterLease.originalSuccessfulExecutionId(),
                    snapshot.sourceSnapshotId(),
                    snapshot.vendorSchemaVersion(),
                    hash
            );
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REPLAYED, Instant.now(),
                    "originalSuccessfulExecutionId=" + afterLease.originalSuccessfulExecutionId());
            return replayed;
        }

        batchService.markRunning(executionId, Instant.now(), lease.get().fencingToken());
        batchService.appendAudit(executionId, ImportAuditEventType.LEASE_ACQUIRED, Instant.now(),
                "fencingToken=" + lease.get().fencingToken() + " ownerInstanceId=" + identity.instanceId());
        faultInjector.signalLeaseHeld();
        faultInjector.awaitHold();
        Closeable guard = executionGuard.start(executionId, lease.get());
        long startedNs = System.nanoTime();
        try {
            maybeDelay(resolved);
            NetworkImportBatchEntity running = batchService.require(executionId);
            if (!"RUNNING".equals(running.getStatus())) {
                throw new ImportRuntimeException(ImportFailureCode.EXECUTION_TIMEOUT, "execution is no longer RUNNING");
            }
            Instant now = Instant.now();
            batchService.appendCheckpoint(executionId, ImportCheckpointType.SNAPSHOT_READ, now,
                    "sourceSnapshotId=" + snapshot.sourceSnapshotId()
                            + " complete=" + snapshot.completeSnapshot()
                            + " entitiesRead=" + snapshot.entityCount());
            batchService.appendAudit(executionId, ImportAuditEventType.SNAPSHOT_READ, now,
                    "sourceSnapshotId=" + snapshot.sourceSnapshotId()
                            + " complete=" + snapshot.completeSnapshot()
                            + " entitiesRead=" + snapshot.entityCount()
                            + " hash=" + hash);
            batchService.appendCheckpoint(executionId, ImportCheckpointType.NORMALIZATION_COMPLETED, Instant.now(),
                    "canonicalHash=" + hash);
            var issues = validator.validateAndFilter(normalized.snapshot(), normalized.issues());
            batchService.appendCheckpoint(executionId, ImportCheckpointType.VALIDATION_COMPLETED, Instant.now(),
                    "issues=" + issues.size());
            batchService.appendAudit(executionId, ImportAuditEventType.VALIDATION_COMPLETED, Instant.now(),
                    "issues=" + issues.size());
            ImportPlan plan = reconciliationService.plan(normalized.snapshot(), issues, Instant.now());
            batchService.appendCheckpoint(executionId, ImportCheckpointType.RECONCILIATION_COMPLETED, Instant.now(),
                    "created=" + plan.toResult().created() + " updated=" + plan.toResult().updated()
                            + " unchanged=" + plan.toResult().unchanged() + " conflicts=" + plan.toResult().conflicts());
            batchService.appendAudit(executionId, ImportAuditEventType.RECONCILIATION_COMPLETED, Instant.now(),
                    "created=" + plan.toResult().created() + " updated=" + plan.toResult().updated()
                            + " unchanged=" + plan.toResult().unchanged() + " conflicts=" + plan.toResult().conflicts());
            leaseService.assertOwnership(lease.get());
            running = batchService.require(executionId);
            ReconciliationResult result = reconciliationService.apply(
                    running, normalized.snapshot(), plan, Instant.now(), lease.get());
            batchService.appendCheckpoint(executionId, ImportCheckpointType.CANONICAL_COMMIT_COMPLETED, Instant.now(),
                    "created=" + result.created() + " updated=" + result.updated());
            Instant completedAt = Instant.now();
            boolean completed = batchService.complete(executionId, completedAt, result);
            if (!completed) {
                throw new ImportRuntimeException(ImportFailureCode.EXECUTION_TIMEOUT, "execution left RUNNING before complete");
            }
            batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_COMPLETED, completedAt, "status=COMPLETED");
            NetworkImportBatchEntity done = batchService.require(executionId);
            metrics.recordSuccess(
                    result,
                    (System.nanoTime() - startedNs) / 1_000_000L,
                    new IntegrationMetrics.UUIDLike(
                            done.getId().toString(),
                            done.getSourceSystem(),
                            done.getSourceSnapshotId(),
                            done.getSourceScope(),
                            done.getLeaseFencingToken()
                    )
            );
            return done;
        } catch (RuntimeException ex) {
            ImportFailureCode code = failureCode(ex);
            boolean retryable = ImportRuntimeException.retryableDefault(code);
            Instant failedAt = Instant.now();
            String status = code == ImportFailureCode.EXECUTION_TIMEOUT
                    ? ImportExecutionStatus.TIMED_OUT.name()
                    : ImportExecutionStatus.FAILED.name();
            boolean updated = batchService.terminalize(executionId, status, failedAt, code, retryable, ex.getMessage());
            if (updated) {
                batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_FAILED, failedAt,
                        code.name() + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                metrics.recordFailure(executionId.toString(), sourceSystem, sourceScope, snapshot.sourceSnapshotId(),
                        code, (System.nanoTime() - startedNs) / 1_000_000L);
            }
            return batchService.require(executionId);
        } finally {
            try {
                guard.close();
            } catch (Exception ignored) {
                // already cancelled
            }
            leaseService.release(lease.get());
            batchService.appendAudit(executionId, ImportAuditEventType.LEASE_RELEASED, Instant.now(),
                    "fencingToken=" + lease.get().fencingToken());
        }
    }

    private NetworkImportBatchEntity persistReplay(
            NetworkSourceAdapter adapter,
            Vendor vendor,
            FixtureKind kind,
            String sourceScope,
            Instant requestedAt,
            SourceSnapshot snapshot,
            String hash,
            SnapshotClassification classification
    ) {
        UUID executionId = UUID.randomUUID();
        batchService.requested(
                executionId,
                adapter.sourceSystem(),
                vendor.name(),
                adapter.schemaVersion(),
                kind,
                sourceScope,
                ImportExecutionType.REPLAY,
                1,
                null,
                requestedAt,
                identity.instanceId()
        );
        NetworkImportBatchEntity replayed = batchService.completeReplay(
                executionId,
                Instant.now(),
                classification.originalSuccessfulExecutionId(),
                snapshot.sourceSnapshotId(),
                snapshot.vendorSchemaVersion(),
                hash
        );
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REPLAYED, Instant.now(),
                "originalSuccessfulExecutionId=" + classification.originalSuccessfulExecutionId()
                        + " canonicalMutation=false");
        metrics.incrementReplays();
        return replayed;
    }

    private NetworkImportBatchEntity persistRejected(
            NetworkSourceAdapter adapter,
            Vendor vendor,
            FixtureKind kind,
            String sourceScope,
            Instant requestedAt,
            SourceSnapshot snapshot,
            String hash,
            SnapshotClassification classification
    ) {
        UUID executionId = UUID.randomUUID();
        batchService.requested(
                executionId,
                adapter.sourceSystem(),
                vendor.name(),
                adapter.schemaVersion(),
                kind,
                sourceScope,
                classification.type(),
                classification.attemptNumber(),
                classification.previousExecutionId(),
                requestedAt,
                identity.instanceId()
        );
        batchService.recordSnapshot(executionId, snapshot.sourceSnapshotId(), snapshot.vendorSchemaVersion(), hash);
        batchService.terminalize(
                executionId,
                ImportExecutionStatus.REJECTED.name(),
                Instant.now(),
                classification.failureCode(),
                false,
                classification.detail()
        );
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_REJECTED, Instant.now(),
                "failureCode=" + classification.failureCode());
        metrics.recordFailure(executionId.toString(), adapter.sourceSystem(), sourceScope, snapshot.sourceSnapshotId(),
                classification.failureCode(), 0L);
        return batchService.require(executionId);
    }

    private NetworkImportBatchEntity persistReadFailure(
            NetworkSourceAdapter adapter,
            Vendor vendor,
            FixtureKind kind,
            String sourceScope,
            Instant requestedAt,
            RuntimeException ex
    ) {
        ImportFailureCode code = failureCode(ex);
        UUID executionId = UUID.randomUUID();
        batchService.requested(
                executionId,
                adapter.sourceSystem(),
                vendor.name(),
                adapter.schemaVersion(),
                kind,
                sourceScope,
                ImportExecutionType.NEW,
                1,
                null,
                requestedAt,
                identity.instanceId()
        );
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_STARTED, requestedAt,
                "vendor=" + vendor + " fixtureKind=" + kind);
        metrics.incrementStarted();
        batchService.terminalize(
                executionId,
                ImportExecutionStatus.FAILED.name(),
                Instant.now(),
                code,
                ImportRuntimeException.retryableDefault(code),
                ex.getMessage()
        );
        batchService.appendAudit(executionId, ImportAuditEventType.IMPORT_FAILED, Instant.now(),
                code.name() + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        metrics.recordFailure(executionId.toString(), adapter.sourceSystem(), sourceScope, "UNREAD", code, 0L);
        return batchService.require(executionId);
    }

    private SnapshotClassification classify(String sourceSystem, String sourceScope, String snapshotId, String hash) {
        List<NetworkImportBatchEntity> history = batchRepository
                .findBySourceSystemAndSourceScopeAndSourceSnapshotIdOrderByAttemptNumberAsc(
                        sourceSystem, sourceScope, snapshotId);
        Optional<String> establishedHash = history.stream()
                .filter(item -> item.getCanonicalSnapshotHash() != null && !item.getCanonicalSnapshotHash().isBlank())
                .filter(item -> !ImportFailureCode.SNAPSHOT_ID_CONTENT_MISMATCH.name().equals(item.getFailureCode()))
                .map(NetworkImportBatchEntity::getCanonicalSnapshotHash)
                .findFirst();
        if (establishedHash.isPresent() && !establishedHash.get().equals(hash)) {
            return SnapshotClassification.reject(
                    ImportExecutionType.NEW,
                    1,
                    null,
                    ImportFailureCode.SNAPSHOT_ID_CONTENT_MISMATCH,
                    "sourceSnapshotId content mismatch"
            );
        }
        Optional<NetworkImportBatchEntity> successful = history.stream()
                .filter(item -> "COMPLETED".equals(item.getStatus()))
                .filter(item -> !"REPLAY".equals(item.getExecutionType()))
                .min(Comparator.comparing(NetworkImportBatchEntity::getAttemptNumber)
                        .thenComparing(NetworkImportBatchEntity::getRequestedAt));
        if (successful.isPresent()) {
            return SnapshotClassification.replay(successful.get().getId());
        }
        Optional<NetworkImportBatchEntity> latest = history.stream()
                .max(Comparator.comparing(NetworkImportBatchEntity::getAttemptNumber)
                        .thenComparing(NetworkImportBatchEntity::getRequestedAt));
        if (latest.isPresent()) {
            NetworkImportBatchEntity prior = latest.get();
            boolean retryableFailure = ("FAILED".equals(prior.getStatus()) || "TIMED_OUT".equals(prior.getStatus()))
                    && Boolean.TRUE.equals(prior.getRetryable());
            if (retryableFailure) {
                return SnapshotClassification.retry(prior.getAttemptNumber() + 1, prior.getId());
            }
            if ("REJECTED".equals(prior.getStatus()) || "FAILED".equals(prior.getStatus())
                    || "TIMED_OUT".equals(prior.getStatus())) {
                ImportFailureCode code = prior.getFailureCode() == null
                        ? ImportFailureCode.VALIDATION_FATAL
                        : ImportFailureCode.valueOf(prior.getFailureCode());
                return SnapshotClassification.reject(
                        ImportExecutionType.NEW,
                        1,
                        prior.getId(),
                        code,
                        "prior execution is not retryable"
                );
            }
        }
        return SnapshotClassification.fresh();
    }

    private Optional<NetworkImportBatchEntity> activeExecution(String sourceSystem, String sourceScope) {
        return batchRepository.findBySourceSystemAndSourceScopeAndStatus(sourceSystem, sourceScope, "RUNNING")
                .stream()
                .findFirst();
    }

    private void maybeDelay(FixtureKind kind) {
        if (kind != FixtureKind.DELAY && kind != FixtureKind.TIMEOUT) {
            return;
        }
        long delayMs = properties.getFixtureReadDelay().toMillis();
        if (delayMs <= 0L) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ImportRuntimeException(ImportFailureCode.EXECUTION_TIMEOUT, "import delay interrupted", ex);
        }
    }

    private static boolean isTestOnlyKind(FixtureKind kind) {
        return kind == FixtureKind.CATASTROPHIC
                || kind == FixtureKind.DELAY
                || kind == FixtureKind.SNAPSHOT_FAIL
                || kind == FixtureKind.CONTENT_MISMATCH
                || kind == FixtureKind.COMMIT_FAIL
                || kind == FixtureKind.TIMEOUT
                || kind == FixtureKind.IDENTITY_BASE;
    }

    private static ImportFailureCode failureCode(RuntimeException ex) {
        if (ex instanceof ImportRuntimeException runtime) {
            return runtime.failureCode();
        }
        if (ex instanceof IntegrationSnapshotException) {
            return ImportFailureCode.SNAPSHOT_READ_FAILED;
        }
        if (ex instanceof DomainValidationException) {
            return ImportFailureCode.SCHEMA_UNSUPPORTED;
        }
        if (ex instanceof DataAccessException) {
            return ImportFailureCode.DATABASE_COMMIT_FAILED;
        }
        return ImportFailureCode.RECONCILIATION_FAILED;
    }

    private record SnapshotClassification(
            ImportExecutionType type,
            int attemptNumber,
            UUID previousExecutionId,
            UUID originalSuccessfulExecutionId,
            boolean rejected,
            ImportFailureCode failureCode,
            String detail
    ) {
        static SnapshotClassification fresh() {
            return new SnapshotClassification(ImportExecutionType.NEW, 1, null, null, false, null, null);
        }

        static SnapshotClassification retry(int attemptNumber, UUID previousExecutionId) {
            return new SnapshotClassification(
                    ImportExecutionType.RETRY, attemptNumber, previousExecutionId, null, false, null, null);
        }

        static SnapshotClassification replay(UUID originalSuccessfulExecutionId) {
            return new SnapshotClassification(
                    ImportExecutionType.REPLAY, 1, null, originalSuccessfulExecutionId, false, null, null);
        }

        static SnapshotClassification reject(
                ImportExecutionType type,
                int attemptNumber,
                UUID previousExecutionId,
                ImportFailureCode failureCode,
                String detail
        ) {
            return new SnapshotClassification(type, attemptNumber, previousExecutionId, null, true, failureCode, detail);
        }
    }
}
