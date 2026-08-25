# ADR 053 — NEW / RETRY / REPLAY Semantics

## Status

Accepted for Phase 8.

## Context

Phase 7 replay created an ordinary new batch while canonical state stayed idempotent. Failed attempts were not a first-class lineage. Automatic retry would hide operator control.

## Decision

Classify each request as `NEW`, `RETRY`, or `REPLAY`. `REPLAY` is a new immutable completed record that references the original successful execution and performs zero canonical mutation. `RETRY` is an explicit resubmission of a retryable failure and creates a new attempt. Automatic retry, queues, and cancellation APIs are prohibited.

## Consequences

Successful duplicate snapshots no longer re-apply MISSING transitions. Reappearance requires a new snapshot identity. Callers cannot override retryability.
