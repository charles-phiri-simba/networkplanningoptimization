package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.enm.SimulatorEnmSyncState;
import com.simba.snip.npo.integration.security.ConnectorCapability;
import com.simba.snip.npo.integration.security.ConnectorDefinition;
import com.simba.snip.npo.integration.security.ConnectorDescriptor;
import com.simba.snip.npo.integration.security.ConnectorMode;
import com.simba.snip.npo.persist.NetworkImportBatchEntity;
import com.simba.snip.npo.persist.NetworkImportBatchRepository;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class SynchronizationModeSelector {

    private final NetworkImportBatchRepository batchRepository;

    public SynchronizationModeSelector(NetworkImportBatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public SynchronizationMode select(
            SynchronizationPolicy policy,
            ConnectorDescriptor descriptor,
            Optional<SynchronizationCheckpointEntity> checkpoint,
            boolean recoveryRequested
    ) {
        if (recoveryRequested) {
            return SynchronizationMode.RECOVERY_FULL;
        }
        if (checkpoint.isEmpty()) {
            return SynchronizationMode.FULL;
        }
        SynchronizationCheckpointEntity current = checkpoint.get();
        SynchronizationCheckpointStatus status = SynchronizationCheckpointStatus.valueOf(current.getStatus());
        if (status == SynchronizationCheckpointStatus.RECOVERY_REQUIRED
                || status == SynchronizationCheckpointStatus.CHECKPOINT_UNCERTAIN
                || status == SynchronizationCheckpointStatus.INVALID
                || status == SynchronizationCheckpointStatus.EXPIRED) {
            return SynchronizationMode.RECOVERY_FULL;
        }
        if (policy.preferredMode() == SynchronizationMode.INCREMENTAL
                && status == SynchronizationCheckpointStatus.VALID
                && supportsIncremental(descriptor)) {
            return SynchronizationMode.INCREMENTAL;
        }
        if (policy.preferredMode() == SynchronizationMode.INCREMENTAL && !supportsIncremental(descriptor)) {
            throw new SynchronizationUnsupportedModeException("incremental requested but connector does not support it");
        }
        return SynchronizationMode.FULL;
    }

    public Optional<NetworkImportBatchEntity> detectCrashWindow(
            String sourceSystem,
            String sourceScope,
            SynchronizationCheckpointEntity checkpoint
    ) {
        if (checkpoint.getLastSuccessfulExecutionId() == null) {
            return Optional.empty();
        }
        return batchRepository.findFirstBySourceSystemAndSourceScopeAndSynchronizationModeNotNullAndStatusOrderByCompletedAtDesc(
                        sourceSystem, sourceScope, "COMPLETED")
                .filter(latest -> !latest.getId().equals(checkpoint.getLastSuccessfulExecutionId()));
    }

    public boolean requiresRecovery(SynchronizationCheckpointEntity checkpoint) {
        SynchronizationCheckpointStatus status = SynchronizationCheckpointStatus.valueOf(checkpoint.getStatus());
        return status == SynchronizationCheckpointStatus.RECOVERY_REQUIRED
                || status == SynchronizationCheckpointStatus.CHECKPOINT_UNCERTAIN
                || status == SynchronizationCheckpointStatus.INVALID
                || status == SynchronizationCheckpointStatus.EXPIRED;
    }

    public boolean supportsIncremental(ConnectorDescriptor descriptor) {
        Set<ConnectorCapability> capabilities = descriptor.capabilities();
        return capabilities.contains(ConnectorCapability.INCREMENTAL_SYNCHRONIZATION)
                && capabilities.contains(ConnectorCapability.DURABLE_CHECKPOINT)
                && capabilities.contains(ConnectorCapability.RESUMABLE_CHECKPOINT);
    }

    public boolean simulatorAdvertisesIncremental(ConnectorDefinition definition) {
        return definition.mode() != ConnectorMode.REAL;
    }

    public String normalizeStartingCheckpoint(Optional<SynchronizationCheckpointEntity> checkpoint) {
        if (checkpoint.isEmpty()) {
            return SimulatorEnmSyncState.CHECKPOINT_ZERO;
        }
        return checkpoint.get().getCheckpointValue();
    }
}
