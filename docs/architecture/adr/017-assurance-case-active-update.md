# ADR 017 — Active assurance-case update semantics

## Status

Accepted for Phase 3.

## Context

Each telemetry event must not create a new case.

## Decision

Active identity is `affectedEntityId + caseType + status in {OPEN, ACKNOWLEDGED}`, enforced with a partial unique index. Matching detections update `lastObservedAt`, severity/confidence, and replace the operational evidence snapshot. `detectedAt` / `firstObservedAt` are preserved. Automatic close-on-recovery is deferred.

## Consequences

Repeated `high-bler-load` publications update one case. No per-event case explosion.
