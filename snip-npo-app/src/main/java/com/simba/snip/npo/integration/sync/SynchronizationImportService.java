package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.config.EnmIntegrationProperties;
import com.simba.snip.npo.domain.ImportBusyException;
import com.simba.snip.npo.integration.CanonicalNormalizer;
import com.simba.snip.npo.integration.CanonicalSnapshotHasher;
import com.simba.snip.npo.integration.CanonicalValidator;
import com.simba.snip.npo.integration.FixtureKind;
import com.simba.snip.npo.integration.ImportAuditEventType;
import com.simba.snip.npo.integration.ImportExecutionGuard;
import com.simba.snip.npo.integration.ImportExecutionStatus;
import com.simba.snip.npo.integration.ImportExecutionType;
import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.ImportLease;
import com.simba.snip.npo.integration.ImportLeaseService;
import com.simba.snip.npo.integration.ImportPlan;
import com.simba.snip.npo.integration.ImportRuntimeException;
import com.simba.snip.npo.integration.IntegrationMetrics;
import com.simba.snip.npo.integration.IntegrationRuntimeIdentity;
import com.simba.snip.npo.integration.NetworkImportBatchService;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.integration.NetworkImportService;
import com.simba.snip.npo.integration.NetworkReconciliationService;
import com.simba.snip.npo.integration.ReconciliationResult;
import com.simba.snip.npo.integration.SourceSnapshot;
import com.simba.snip.npo.integration.enm.ConnectorCancellationToken;
import com.simba.snip.npo.integration.enm.EnmConnectorMetrics;
import com.simba.snip.npo.integration.enm.EnmImportTestHooks;
import com.simba.snip.npo.integration.enm.EricssonEnmConnector;
import com.simba.snip.npo.integration.enm.ImportExecutionContext;
import com.simba.snip.npo.integration.enm.SnapshotCompleteness;
import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.integration.enm.SynchronizationExecutionContext;
import com.simba.snip.npo.integration.enm.VendorSnapshot;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.integration.sync.VendorIncrementalBatch;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SynchronizationImportService {

    private final ConnectorRegistry connectorRegistry;
    private final EnmIntegrationProperties enmProperties;
    private final NetworkImportBatchService batchService;
    private final NetworkImportBatchRepository batchRepository;
    private final ImportLeaseService leaseService;
    private final ImportExecutionGuard executionGuard;
    private final IntegrationRuntimeIdentity identity;
    private final IntegrationMetrics metrics;
    private final CanonicalNormalizer normalizer;
    private final CanonicalValidator validator;
    private final CanonicalSnapshotHasher hasher;
    private final NetworkReconciliationService reconciliationService;
    private final EricssonEnmConnector enmConnector;
    private final EnmConnectorMetrics enmMetrics;
    private final EnmImportTestHooks enmImportTestHooks;
    private final SynchronizationModeSelector modeSelector;
    private final SynchronizationCheckpointService checkpointService;
    private final SynchronizationSourceStateService sourceStateService;
    private final NetworkDriftService driftService;
    private final SynchronizationSourceHealthEvaluator healthEvaluator;
    private final SynchronizationMetrics synchronizationMetrics;
    private final NetworkImportService networkImportService;
    private final VendorIncrementalRemoveApplier incrementalRemoveApplier;
    private final SimulatorEnmSyncState simulatorEnmSyncState;

    public SynchronizationImportService(
            ConnectorRegistry connectorRegistry,
            EnmIntegrationProperties enmProperties,
            NetworkImportBatchService batchService,
            NetworkImportBatchRepository batchRepository,
            ImportLeaseService leaseService,
            ImportExecutionGuard executionGuard,
            IntegrationRuntimeIdentity identity,
            IntegrationMetrics metrics,
            CanonicalNormalizer normalizer,
            CanonicalValidator validator,
            CanonicalSnapshotHasher hasher,
            NetworkReconciliationService reconciliationService,
            EricssonEnmConnector enmConnector,
            EnmConnectorMetrics enmMetrics,
            EnmImportTestHooks enmImportTestHooks,
            SynchronizationModeSelector modeSelector,
            SynchronizationCheckpointService checkpointService,
            SynchronizationSourceStateService sourceStateService,
            NetworkDriftService driftService,
            SynchronizationSourceHealthEvaluator healthEvaluator,
            SynchronizationMetrics synchronizationMetrics,
            NetworkImportService networkImportService,
            VendorIncrementalRemoveApplier incrementalRemoveApplier,
            SimulatorEnmSyncState simulatorEnmSyncState
    ) {
        this.connectorRegistry = connectorRegistry;
        this.enmProperties = enmProperties;
        this.batchService = batchService;
        this.batchRepository = batchRepository;
        this.leaseService = leaseService;
        this.executionGuard = executionGuard;
        this.identity = identity;
        this.metrics = metrics;
        this.normalizer = normalizer;
        this.validator = validator;
        this.hasher = hasher;
        this.reconciliationService = reconciliationService;
        this.enmConnector = enmConnector;
        this.enmMetrics = enmMetrics;
        this.enmImportTestHooks = enmImportTestHooks;
        this.modeSelector = modeSelector;
        this.checkpointService = checkpointService;
        this.sourceStateService = sourceStateService;
        this.driftService = driftService;
        this.healthEvaluator = healthEvaluator;
        this.synchronizationMetrics = synchronizationMetrics;
        this.networkImportService = networkImportService;
        this.incrementalRemoveApplier = incrementalRemoveApplier;
        this.simulatorEnmSyncState = simulatorEnmSyncState;
    }

    public SynchronizationExecutionResult execute(SynchronizationExecutionRequest request) {
        SynchronizationPolicy policy = request.policy();
        if (!policy.enabled() || !enmProperties.isEnabled()) {
            throw new SynchronizationDisabledException("synchronization source is disabled");
        }
        ConnectorDefinition definition = connectorRegistry.require(policy.connectorId());
        String sourceSystem = policy.sourceSystem();
        String sourceScope = policy.sourceScope();
        Instant now = Instant.now();
        synchronizationMetrics.incrementRuns();
        sourceStateService.recordStarted(policy, now, now);

        Optional<SynchronizationCheckpointEntity> checkpoint = checkpointService.find(sourceSystem, sourceScope);
        SynchronizationCheckpointEntity checkpointRow = checkpointService.require(sourceSystem, policy.connectorId(), sourceScope, now);
        modeSelector.detectCrashWindow(sourceSystem, sourceScope, checkpointRow).ifPresent(crash -> {
            checkpointService.markStatus(sourceSystem, sourceScope, SynchronizationCheckpointStatus.CHECKPOINT_UNCERTAIN, now);
            driftService.recordOpen(
                    sourceSystem,
                    policy.connectorId(),
                    sourceScope,
                    NetworkDriftType.SYNCHRONIZATION_DRIFT,
                    "CHECKPOINT",
                    sourceScope,
                    crash.getId(),
                    0L,
                    "completed execution without checkpoint confirmation",
                    now
            );
            synchronizationMetrics.incrementRecoveryRequired();
            batchService.appendAudit(crash.getId(), ImportAuditEventType.CHECKPOINT_UNCERTAIN, now,
                    "execution=" + crash.getId());
        });
        checkpoint = checkpointService.find(sourceSystem, sourceScope);
        if (checkpoint.isPresent() && modeSelector.requiresRecovery(checkpoint.get()) && !request.recoveryRequested()) {
            throw new SynchronizationRecoveryRequiredException("recovery authorization required");
        }
        SynchronizationMode mode;
        try {
            mode = request.recoveryRequested()
                    ? SynchronizationMode.RECOVERY_FULL
                    : modeSelector.select(
                            policy,
                            enmConnector.descriptor(definition),
                            checkpoint,
                            false
                    );
        } catch (SynchronizationRecoveryRequiredException ex) {
            checkpointService.markStatus(sourceSystem, sourceScope, SynchronizationCheckpointStatus.CHECKPOINT_UNCERTAIN, now);
            sourceStateService.recordOutcome(
                    policy, checkpoint, null, 0L, now, false,
                    SynchronizationSourceHealth.RECOVERING, true, now);
            synchronizationMetrics.incrementRecoveryRequired();
            throw new ImportRuntimeException(ImportFailureCode.CHECKPOINT_UNCERTAIN, ex.getMessage(), false, ex);
        } catch (SynchronizationUnsupportedModeException ex) {
            throw new ImportRuntimeException(ImportFailureCode.INCREMENTAL_NOT_SUPPORTED, ex.getMessage(), false, ex);
        }

        leaseService.recoverExpired(sourceSystem, sourceScope);
        Optional<NetworkImportBatchEntity> active = networkImportService.activeExecutionForTests(sourceSystem, sourceScope);
        if (active.isPresent()) {
            synchronizationMetrics.incrementOverlapSkips();
            sourceStateService.recordOverlapSkip(policy, now);
            batchService.appendAudit(active.get().getId(), ImportAuditEventType.OVERLAP_SKIPPED, now,
                    "sourceScope=" + sourceScope);
            return SynchronizationExecutionResult.skipped();
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
                now,
                identity.instanceId()
        );
        batchService.recordSynchronization(executionId, mode.name(), request.initiator().name());
        batchService.appendAudit(executionId, ImportAuditEventType.SYNCHRONIZATION_STARTED, now,
                "initiator=" + request.initiator() + " mode=" + mode);
        batchService.appendAudit(executionId, ImportAuditEventType.MODE_SELECTED, now, "mode=" + mode);
        String startingCheckpoint = modeSelector.normalizeStartingCheckpoint(checkpoint);
        batchService.appendAudit(executionId, ImportAuditEventType.CHECKPOINT_LOADED, now,
                "checkpoint=" + startingCheckpoint);

        Optional<ImportLease> lease = leaseService.acquire(sourceSystem, sourceScope, executionId, identity.instanceId());
        if (lease.isEmpty()) {
            synchronizationMetrics.incrementOverlapSkips();
            sourceStateService.recordOverlapSkip(policy, now);
            batchService.terminalize(
                    executionId,
                    ImportExecutionStatus.REJECTED.name(),
                    now,
                    ImportFailureCode.LEASE_UNAVAILABLE,
                    true,
                    "lease unavailable"
            );
            return SynchronizationExecutionResult.skipped();
        }

        batchService.markRunning(executionId, now, lease.get().fencingToken());
        batchService.appendAudit(executionId, ImportAuditEventType.LEASE_ACQUIRED, now,
                "fencingToken=" + lease.get().fencingToken());
        Closeable guard = executionGuard.start(executionId, lease.get());
        long startedNs = System.nanoTime();
        VendorSnapshot vendorSnapshot = null;
        SourceSnapshot snapshot = null;
        VendorIncrementalBatch incrementalBatch = null;
        try {
            ConnectorCancellationToken cancellationToken = new ConnectorCancellationToken();
            enmImportTestHooks.bind(cancellationToken);
            ImportExecutionContext importContext = new ImportExecutionContext(
                    executionId,
                    now.plus(policy.maxExecutionDuration()),
                    lease.get(),
                    leaseService,
                    cancellationToken,
                    policy.requestTimeout()
            );
            SynchronizationExecutionContext syncContext = new SynchronizationExecutionContext(
                    importContext, mode, startingCheckpoint, request.recoveryRequested());
            importContext.assertContinuing();
            EricssonEnmConnector.AcquisitionResult acquired = enmConnector.acquireSynchronized(definition, syncContext);
            vendorSnapshot = acquired.vendorSnapshot();
            snapshot = acquired.sourceSnapshot();
            incrementalBatch = acquired.incrementalBatch();
            networkImportService.persistVendorSnapshotForSync(vendorSnapshot);
            batchService.recordSnapshot(
                    executionId,
                    snapshot.sourceSnapshotId(),
                    snapshot.vendorSchemaVersion(),
                    hasher.hash(normalizer.normalize(snapshot).snapshot())
            );
            if (vendorSnapshot.completeness() != SnapshotCompleteness.COMPLETE) {
                return failClosed(request, executionId, sourceSystem, sourceScope, snapshot, vendorSnapshot,
                        lease.get(), mode, startedNs);
            }
            CanonicalNormalizer.NormalizeResult normalized = normalizer.normalize(snapshot);
            var issues = validator.validateAndFilter(normalized.snapshot(), normalized.issues());
            ImportPlan plan = reconciliationService.plan(normalized.snapshot(), issues, Instant.now());
            enmImportTestHooks.runBeforeReconcile();
            importContext.assertContinuing();
            leaseService.assertOwnership(lease.get());
            NetworkImportBatchEntity running = batchService.require(executionId);
            ReconciliationResult result = reconciliationService.apply(
                    running, normalized.snapshot(), plan, Instant.now(), lease.get());
            int explicitRemoves = incrementalRemoveApplier.applyRemoves(incrementalBatch, executionId, Instant.now());
            networkImportService.persistProvenanceForSync(executionId, vendorSnapshot, snapshot, Instant.now());
            Instant completedAt = Instant.now();
            ReconciliationResult finalResult = new ReconciliationResult(
                    result.entitiesRead(),
                    result.created(),
                    result.updated(),
                    result.unchanged(),
                    result.rejected(),
                    result.conflicts(),
                    result.missing() + explicitRemoves
            );
            batchService.complete(executionId, completedAt, finalResult);
            enmImportTestHooks.runAfterReconcileBeforeCheckpoint();
            String resultingCheckpoint = incrementalBatch == null
                    ? SimulatorEnmSyncState.CHECKPOINT_PREFIX + "1"
                    : incrementalBatch.resultingCheckpoint();
            if (mode == SynchronizationMode.FULL || mode == SynchronizationMode.RECOVERY_FULL) {
                resultingCheckpoint = SimulatorEnmSyncState.CHECKPOINT_PREFIX + "1";
                simulatorEnmSyncState.establishSequence(sourceScope, resultingCheckpoint);
            }
            checkpointService.advanceIfAuthoritative(
                    sourceSystem,
                    sourceScope,
                    executionId,
                    snapshot.sourceSnapshotId(),
                    now,
                    completedAt,
                    resultingCheckpoint,
                    vendorSnapshot.sourceVersion(),
                    mode,
                    "COMPLETE",
                    lease.get().fencingToken(),
                    completedAt,
                    completedAt
            );
            batchService.appendAudit(executionId, ImportAuditEventType.CHECKPOINT_ADVANCED, completedAt,
                    "checkpoint=" + resultingCheckpoint);
            batchService.appendAudit(executionId, ImportAuditEventType.SYNCHRONIZATION_COMPLETED, completedAt,
                    "mode=" + mode);
            Optional<SynchronizationCheckpointEntity> updatedCheckpoint = checkpointService.find(sourceSystem, sourceScope);
            sourceStateService.recordOutcome(
                    policy,
                    updatedCheckpoint,
                    executionId,
                    lease.get().fencingToken(),
                    completedAt,
                    true,
                    healthEvaluator.healthyAfterSuccess(),
                    false,
                    completedAt
            );
            if (incrementalBatch != null && !incrementalBatch.changes().isEmpty()) {
                driftService.recordOpen(
                        sourceSystem,
                        policy.connectorId(),
                        sourceScope,
                        NetworkDriftType.SOURCE_STATE_DRIFT,
                        "CELL",
                        "CELL-SIM-002",
                        executionId,
                        lease.get().fencingToken(),
                        "synthetic source-state change detected",
                        completedAt
                );
                synchronizationMetrics.incrementDriftDetected();
                batchService.appendAudit(executionId, ImportAuditEventType.DRIFT_DETECTED, completedAt,
                        "entity=CELL-SIM-002");
            }
            driftService.resolveApplicable(sourceSystem, sourceScope, executionId, lease.get().fencingToken(), completedAt);
            synchronizationMetrics.incrementSuccesses();
            return new SynchronizationExecutionResult(batchService.require(executionId), mode, false, executionId);
        } catch (RuntimeException ex) {
            synchronizationMetrics.incrementFailures();
            NetworkImportBatchEntity current = batchRepository.findById(executionId).orElseGet(() -> batchService.require(executionId));
            if ("COMPLETED".equals(current.getStatus())) {
                handleCompletedBeforeCheckpoint(request, executionId, sourceSystem, sourceScope, lease.get(), mode, now);
                return new SynchronizationExecutionResult(current, mode, false, executionId);
            }
            failFromException(request, executionId, sourceSystem, sourceScope, snapshot, vendorSnapshot, lease.get(),
                    mode, ex, startedNs);
            return new SynchronizationExecutionResult(batchService.require(executionId), mode, false, executionId);
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

    private SynchronizationExecutionResult failClosed(
            SynchronizationExecutionRequest request,
            UUID executionId,
            String sourceSystem,
            String sourceScope,
            SourceSnapshot snapshot,
            VendorSnapshot vendorSnapshot,
            ImportLease lease,
            SynchronizationMode mode,
            long startedNs
    ) {
        Instant failedAt = Instant.now();
        ImportFailureCode code = vendorSnapshot.completeness() == SnapshotCompleteness.PARTIAL
                ? ImportFailureCode.SNAPSHOT_PARTIAL
                : ImportFailureCode.SNAPSHOT_READ_FAILED;
        batchService.terminalize(executionId, ImportExecutionStatus.FAILED.name(), failedAt, code, false, code.name());
        batchService.appendAudit(executionId, ImportAuditEventType.SYNCHRONIZATION_FAILED, failedAt, code.name());
        sourceStateService.recordOutcome(
                request.policy(),
                checkpointService.find(sourceSystem, sourceScope),
                executionId,
                lease.fencingToken(),
                failedAt,
                false,
                healthEvaluator.mapFailure(code),
                false,
                failedAt
        );
        return new SynchronizationExecutionResult(batchService.require(executionId), mode, false, executionId        );
    }

    private void handleCompletedBeforeCheckpoint(
            SynchronizationExecutionRequest request,
            UUID executionId,
            String sourceSystem,
            String sourceScope,
            ImportLease lease,
            SynchronizationMode mode,
            Instant now
    ) {
        checkpointService.markStatus(sourceSystem, sourceScope, SynchronizationCheckpointStatus.CHECKPOINT_UNCERTAIN, now);
        driftService.recordOpen(
                sourceSystem,
                request.policy().connectorId(),
                sourceScope,
                NetworkDriftType.SYNCHRONIZATION_DRIFT,
                "CHECKPOINT",
                sourceScope,
                executionId,
                lease.fencingToken(),
                "reconciliation completed without checkpoint confirmation",
                now
        );
        batchService.appendAudit(executionId, ImportAuditEventType.CHECKPOINT_UNCERTAIN, now,
                "execution=" + executionId);
        sourceStateService.recordOutcome(
                request.policy(),
                checkpointService.find(sourceSystem, sourceScope),
                executionId,
                lease.fencingToken(),
                now,
                false,
                SynchronizationSourceHealth.RECOVERING,
                true,
                now
        );
        synchronizationMetrics.incrementRecoveryRequired();
    }

    private void failFromException(
            SynchronizationExecutionRequest request,
            UUID executionId,
            String sourceSystem,
            String sourceScope,
            SourceSnapshot snapshot,
            VendorSnapshot vendorSnapshot,
            ImportLease lease,
            SynchronizationMode mode,
            RuntimeException ex,
            long startedNs
    ) {
        ImportFailureCode code = ex instanceof ImportRuntimeException runtime
                ? runtime.failureCode()
                : ImportFailureCode.SNAPSHOT_READ_FAILED;
        Instant failedAt = Instant.now();
        batchService.terminalize(executionId, ImportExecutionStatus.FAILED.name(), failedAt, code, false, code.name());
        batchService.appendAudit(executionId, ImportAuditEventType.SYNCHRONIZATION_FAILED, failedAt, code.name());
        boolean recovery = code == ImportFailureCode.CHECKPOINT_REJECTED
                || code == ImportFailureCode.CHECKPOINT_EXPIRED
                || code == ImportFailureCode.SEQUENCE_GAP
                || code == ImportFailureCode.CHECKPOINT_UNCERTAIN;
        if (recovery) {
            checkpointService.markStatus(sourceSystem, sourceScope, SynchronizationCheckpointStatus.RECOVERY_REQUIRED, failedAt);
            synchronizationMetrics.incrementRecoveryRequired();
        }
        sourceStateService.recordOutcome(
                request.policy(),
                checkpointService.find(sourceSystem, sourceScope),
                executionId,
                lease.fencingToken(),
                failedAt,
                false,
                healthEvaluator.mapFailure(code),
                recovery,
                failedAt
        );
    }
}
