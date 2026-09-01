package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.persist.NetworkDriftObservationEntity;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusEntity;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import com.simba.snip.npo.persist.SynchronizationSourceStateEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SynchronizationQueryService {

    private final SynchronizationPolicyRegistry policyRegistry;
    private final SynchronizationCheckpointService checkpointService;
    private final SynchronizationSourceStateService sourceStateService;
    private final NetworkDriftService driftService;

    public SynchronizationQueryService(
            SynchronizationPolicyRegistry policyRegistry,
            SynchronizationCheckpointService checkpointService,
            SynchronizationSourceStateService sourceStateService,
            NetworkDriftService driftService
    ) {
        this.policyRegistry = policyRegistry;
        this.checkpointService = checkpointService;
        this.sourceStateService = sourceStateService;
        this.driftService = driftService;
    }

    public List<Map<String, Object>> listSources() {
        return policyRegistry.policies().stream().map(this::sourceSummary).toList();
    }

    public Map<String, Object> sourceState(String sourceSystem, String sourceScope) {
        SynchronizationPolicy policy = policyRegistry.require(sourceSystem, sourceScope);
        SynchronizationSourceStateEntity state = sourceStateService.require(
                sourceSystem, policy.connectorId(), sourceScope, java.time.Instant.now());
        NetworkKnowledgeStatusEntity knowledge = sourceStateService.requireKnowledge(
                sourceSystem, policy.connectorId(), sourceScope, java.time.Instant.now());
        Optional<SynchronizationCheckpointEntity> checkpoint = checkpointService.find(sourceSystem, sourceScope);
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceSystem", sourceSystem);
        payload.put("sourceScope", sourceScope);
        payload.put("connectorId", policy.connectorId());
        payload.put("enabled", policy.enabled());
        payload.put("freshness", state.getFreshness());
        payload.put("sourceHealth", state.getSourceHealth());
        payload.put("recoveryRequired", state.isRecoveryRequired());
        payload.put("knowledgeConfidence", knowledge.getConfidence());
        payload.put("confidenceReasonCodes", knowledge.getReasonCodes());
        payload.put("lastTrustedSnapshotId", knowledge.getLastTrustedSnapshotId());
        payload.put("lastTrustedSynchronizationAt", knowledge.getLastTrustedSynchronizationAt());
        checkpoint.ifPresent(value -> {
            payload.put("checkpointStatus", value.getStatus());
            payload.put("checkpointType", value.getCheckpointType());
            payload.put("checkpointValueHash", Integer.toHexString(value.getCheckpointValue().hashCode()));
        });
        return payload;
    }

    public List<Map<String, Object>> drift(String sourceSystem, String sourceScope) {
        return driftService.list(sourceSystem, sourceScope).stream().map(this::driftDto).toList();
    }

    private Map<String, Object> sourceSummary(SynchronizationPolicy policy) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceSystem", policy.sourceSystem());
        payload.put("sourceScope", policy.sourceScope());
        payload.put("connectorId", policy.connectorId());
        payload.put("enabled", policy.enabled());
        payload.put("preferredMode", policy.preferredMode().name());
        payload.put("cadenceSeconds", policy.cadence().getSeconds());
        return payload;
    }

    private Map<String, Object> driftDto(NetworkDriftObservationEntity drift) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("driftId", drift.getId());
        payload.put("driftType", drift.getDriftType());
        payload.put("driftStatus", drift.getDriftStatus());
        payload.put("entityType", drift.getEntityType());
        payload.put("entityId", drift.getEntityId());
        payload.put("summary", drift.getSummary());
        payload.put("detectedAt", drift.getDetectedAt());
        payload.put("resolvedAt", drift.getResolvedAt());
        return payload;
    }
}
