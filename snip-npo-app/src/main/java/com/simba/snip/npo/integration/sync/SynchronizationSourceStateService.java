package com.simba.snip.npo.integration.sync;

import com.simba.snip.npo.persist.NetworkKnowledgeStatusEntity;
import com.simba.snip.npo.persist.NetworkKnowledgeStatusRepository;
import com.simba.snip.npo.persist.SynchronizationCheckpointEntity;
import com.simba.snip.npo.persist.SynchronizationSourceStateEntity;
import com.simba.snip.npo.persist.SynchronizationSourceStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SynchronizationSourceStateService {

    private final SynchronizationSourceStateRepository sourceStateRepository;
    private final NetworkKnowledgeStatusRepository knowledgeStatusRepository;
    private final SynchronizationFreshnessEvaluator freshnessEvaluator;
    private final NetworkKnowledgeConfidenceEvaluator confidenceEvaluator;

    public SynchronizationSourceStateService(
            SynchronizationSourceStateRepository sourceStateRepository,
            NetworkKnowledgeStatusRepository knowledgeStatusRepository,
            SynchronizationFreshnessEvaluator freshnessEvaluator,
            NetworkKnowledgeConfidenceEvaluator confidenceEvaluator
    ) {
        this.sourceStateRepository = sourceStateRepository;
        this.knowledgeStatusRepository = knowledgeStatusRepository;
        this.freshnessEvaluator = freshnessEvaluator;
        this.confidenceEvaluator = confidenceEvaluator;
    }

    @Transactional
    public SynchronizationSourceStateEntity require(String sourceSystem, String connectorId, String scope, Instant now) {
        return sourceStateRepository.findBySourceSystemAndSynchronizationScope(sourceSystem, scope)
                .orElseGet(() -> sourceStateRepository.save(
                        SynchronizationSourceStateEntity.initial(UUID.randomUUID(), sourceSystem, connectorId, scope, now)));
    }

    @Transactional
    public NetworkKnowledgeStatusEntity requireKnowledge(
            String sourceSystem,
            String connectorId,
            String scope,
            Instant now
    ) {
        return knowledgeStatusRepository.findBySourceSystemAndSynchronizationScope(sourceSystem, scope)
                .orElseGet(() -> knowledgeStatusRepository.save(
                        NetworkKnowledgeStatusEntity.initial(UUID.randomUUID(), sourceSystem, connectorId, scope, now)));
    }

    @Transactional
    public void recordOverlapSkip(SynchronizationPolicy policy, Instant now) {
        SynchronizationSourceStateEntity state = require(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        state.recordOverlapSkip(now);
        sourceStateRepository.save(state);
    }

    @Transactional
    public void recordStarted(SynchronizationPolicy policy, Instant startedAt, Instant now) {
        SynchronizationSourceStateEntity state = require(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        state.recordStarted(startedAt, now);
        sourceStateRepository.save(state);
    }

    @Transactional
    public void recordOutcome(
            SynchronizationPolicy policy,
            Optional<SynchronizationCheckpointEntity> checkpoint,
            UUID executionId,
            long fencingToken,
            Instant completedAt,
            boolean success,
            SynchronizationSourceHealth sourceHealth,
            boolean recoveryRequired,
            Instant now
    ) {
        SynchronizationSourceStateEntity state = require(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        SynchronizationFreshness freshness = freshnessEvaluator.evaluate(
                policy, checkpoint, now, recoveryRequired, !success);
        if (success) {
            state.recordSuccess(
                    executionId,
                    fencingToken,
                    completedAt,
                    freshness.name(),
                    sourceHealth.name(),
                    now
            );
        } else {
            state.recordFailure(
                    fencingToken,
                    completedAt,
                    freshness.name(),
                    sourceHealth.name(),
                    policy.maxConsecutiveFailures(),
                    recoveryRequired,
                    now
            );
        }
        sourceStateRepository.save(state);

        NetworkKnowledgeStatusEntity knowledge = requireKnowledge(
                policy.sourceSystem(), policy.connectorId(), policy.sourceScope(), now);
        NetworkKnowledgeConfidenceEvaluator.Evaluation evaluation = confidenceEvaluator.evaluate(
                freshness, sourceHealth, recoveryRequired, checkpoint);
        knowledge.update(
                fencingToken,
                evaluation.confidence().name(),
                evaluation.reasonCodes(),
                freshness.name(),
                sourceHealth.name(),
                checkpoint.map(SynchronizationCheckpointEntity::getLastSuccessfulSnapshotId).orElse(null),
                checkpoint.map(SynchronizationCheckpointEntity::getLastSuccessfulCompletedAt).orElse(null),
                now
        );
        knowledgeStatusRepository.save(knowledge);
    }
}
