# ADR 056 — Import Checkpoints and Recovery

## Status

Accepted for Phase 8.

## Context

Detailed progress must be diagnosable without proliferating execution statuses or implementing record-level resume.

## Decision

Append-only phase checkpoints are `SNAPSHOT_READ`, `NORMALIZATION_COMPLETED`, `VALIDATION_COMPLETED`, `RECONCILIATION_COMPLETED`, and `CANONICAL_COMMIT_COMPLETED`. A retry restarts from snapshot read. Abandoned `RUNNING` executions whose leases are expired or missing are terminalized as `FAILED` / `LEASE_EXPIRED` on startup and before lease acquisition. No scheduler or resume-from-checkpoint logic is added.

## Consequences

Checkpoints exist for diagnosis, timing, and future recovery evolution. Record-level resume remains deferred.
