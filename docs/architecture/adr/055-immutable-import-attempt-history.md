# ADR 055 — Immutable Import Attempt History

## Status

Accepted for Phase 8.

## Context

Failed-result row replacement remains deferred technical debt elsewhere in SNIP. Import attempts must not inherit that weakness.

## Decision

Terminal import executions are immutable historical records. A retry creates a new row with incremented `attemptNumber` and `previousExecutionId`. Failed and timed-out attempts are not updated, deleted, or reused. Replay is also a new immutable record.

## Consequences

Operators can reconstruct lineage. Phase 8 does not redesign older subsystems that still replace failed rows.
