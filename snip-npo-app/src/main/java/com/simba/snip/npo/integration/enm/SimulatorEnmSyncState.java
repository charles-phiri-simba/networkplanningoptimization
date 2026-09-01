package com.simba.snip.npo.integration.enm;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Deterministic synthetic checkpoint/sequence state for simulator-only incremental contract.
 * Checkpoint tokens use {@code sim-seq:N} where N is monotonically advanced after trusted commits.
 */
@Component
public class SimulatorEnmSyncState {

    public static final String CHECKPOINT_PREFIX = "sim-seq:";
    public static final String CHECKPOINT_ZERO = CHECKPOINT_PREFIX + "0";

    private final ConcurrentHashMap<String, AtomicInteger> scopeSequence = new ConcurrentHashMap<>();
    private final AtomicReference<String> lastBatchId = new AtomicReference<>();

    public int currentSequence(String scope) {
        return scopeSequence.computeIfAbsent(scope, ignored -> new AtomicInteger(0)).get();
    }

    public String checkpointFor(String scope) {
        return CHECKPOINT_PREFIX + currentSequence(scope);
    }

    public int parseSequence(String checkpoint) {
        if (checkpoint == null || checkpoint.isBlank()) {
            return 0;
        }
        if (!checkpoint.startsWith(CHECKPOINT_PREFIX)) {
            return -1;
        }
        try {
            return Integer.parseInt(checkpoint.substring(CHECKPOINT_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public String advanceCheckpoint(String scope) {
        int next = scopeSequence.computeIfAbsent(scope, ignored -> new AtomicInteger(0)).incrementAndGet();
        return CHECKPOINT_PREFIX + next;
    }

    public void resetScope(String scope) {
        scopeSequence.put(scope, new AtomicInteger(0));
        lastBatchId.set(null);
    }

    public void resetAll() {
        scopeSequence.clear();
        lastBatchId.set(null);
    }

    public String lastBatchId() {
        return lastBatchId.get();
    }

    public void rememberBatchId(String batchId) {
        lastBatchId.set(batchId);
    }

    public void establishSequence(String scope, String checkpointValue) {
        int sequence = parseSequence(checkpointValue);
        if (sequence >= 0) {
            scopeSequence.put(scope, new AtomicInteger(sequence));
        }
    }
}
