# ADR 051 — Import Execution Runtime

## Status

Accepted for Phase 8.

## Context

Phase 7 treated an import as a synchronous method that created a `NetworkImportBatch`. That is insufficient once concurrent callers, retries, and crashes must be diagnosed without mutating reconciliation rules.

## Decision

Evolve `NetworkImportBatch` into the durable import execution record. Persist `NEW` / `RETRY` / `REPLAY`, attempt lineage, source scope, snapshot hash, failure code, retryability, owner instance id, fencing token, and requested-at. Statuses are `REQUESTED`, `RUNNING`, `COMPLETED`, `FAILED`, `TIMED_OUT`, and `REJECTED`. Phase 7 reconciliation remains unchanged.

## Consequences

Operators can query attempt history. Replay no longer masquerades as a mutating reimport. Phase 9 is not started.
