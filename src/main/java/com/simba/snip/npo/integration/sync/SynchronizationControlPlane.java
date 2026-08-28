package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.integration.enm.VendorImportAuthorizer;
import com.simba.snip.npo.integration.security.ConnectorRegistry;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import com.simba.snip.npo.persist.SynchronizationSourceStateEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class SynchronizationControlPlane {

    private final SynchronizationPolicyRegistry policyRegistry;
    private final SynchronizationImportService importService;
    private final SynchronizationCheckpointService checkpointService;
    private final SynchronizationSourceStateService sourceStateService;
    private final VendorImportAuthorizer authorizer;
    private final ConnectorRegistry connectorRegistry;
    private final SynchronizationMetrics metrics;

    public SynchronizationControlPlane(
            SynchronizationPolicyRegistry policyRegistry,
            SynchronizationImportService importService,
            SynchronizationCheckpointService checkpointService,
            SynchronizationSourceStateService sourceStateService,
            VendorImportAuthorizer authorizer,
            ConnectorRegistry connectorRegistry,
            SynchronizationMetrics metrics
    ) {
        this.policyRegistry = policyRegistry;
        this.importService = importService;
        this.checkpointService = checkpointService;
        this.sourceStateService = sourceStateService;
        this.authorizer = authorizer;
        this.connectorRegistry = connectorRegistry;
        this.metrics = metrics;
    }

    public SynchronizationExecutionResult triggerManual(String connectorId) {
        authorizer.requireTrigger();
        SynchronizationPolicy policy = policyRegistry.findByConnectorId(connectorId)
                .orElseThrow(() -> new IllegalArgumentException("no synchronization policy for connector " + connectorId));
        policyRegistry.validateConnectorBinding(policy);
        if (!policy.enabled()) {
            throw new SynchronizationDisabledException("synchronization source is disabled");
        }
        return importService.execute(new SynchronizationExecutionRequest(
                policy,
                SynchronizationInitiator.MANUAL,
                policy.preferredMode(),
                false
        ));
    }

    public SynchronizationExecutionResult triggerRecovery(String connectorId) {
        authorizer.requireRecovery();
        SynchronizationPolicy policy = policyRegistry.findByConnectorId(connectorId)
                .orElseThrow(() -> new IllegalArgumentException("no synchronization policy for connector " + connectorId));
        if (!policy.enabled()) {
            throw new SynchronizationDisabledException("synchronization source is disabled");
        }
        return importService.execute(new SynchronizationExecutionRequest(
                policy,
                SynchronizationInitiator.MANUAL,
                SynchronizationMode.RECOVERY_FULL,
                true
        ));
    }

    public void triggerScheduled(SynchronizationPolicy policy) {
        if (!policy.enabled()) {
            return;
        }
        if (!isDue(policy)) {
            return;
        }
        Optional<SynchronizationCheckpointEntity> checkpoint = checkpointService.find(
                policy.sourceSystem(), policy.sourceScope());
        if (checkpoint.map(SynchronizationCheckpointEntity::getStatus)
                .map(SynchronizationCheckpointStatus.RECOVERY_REQUIRED.name()::equals)
                .orElse(false)
                && !policy.allowRecoveryFullOnScheduled()) {
            return;
        }
        authorizer.runWith(VendorImportAuthorizer.SYSTEM_SCHEDULED_PERMISSION, () ->
                importService.execute(new SynchronizationExecutionRequest(
                        policy,
                        SynchronizationInitiator.SCHEDULED,
                        policy.preferredMode(),
                        false
                )));
    }

    public boolean isDue(SynchronizationPolicy policy) {
        Optional<SynchronizationCheckpointEntity> checkpoint = checkpointService.find(
                policy.sourceSystem(), policy.sourceScope());
        Instant anchor = checkpoint
                .map(SynchronizationCheckpointEntity::getLastSuccessfulCompletedAt)
                .orElse(null);
        if (anchor == null) {
            Optional<SynchronizationSourceStateEntity> state = Optional.ofNullable(
                    sourceStateService.require(policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), Instant.now()));
            anchor = state.map(SynchronizationSourceStateEntity::getLastCompletedAt).orElse(null);
        }
        if (anchor == null) {
            return true;
        }
        return Duration.between(anchor, Instant.now()).compareTo(policy.cadence()) >= 0;
    }

    public java.util.List<SynchronizationPolicy> configuredSources() {
        return policyRegistry.policies();
    }
}
