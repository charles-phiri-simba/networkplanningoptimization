# ADR 036 — Manual on-demand Twin synchronization

## Status

Accepted for Phase 6.

## Context

Kafka-triggered or continuous Twin updates would silently move the simulation baseline under an approved action.

## Decision

`TwinSynchronizationService` runs only on explicit `POST /api/v1/twins/cells/{cellId}/synchronize`. There is no Kafka subscription and no automatic resynchronization during governed execution.

## Consequences

Operators must resynchronize after operational drift. Automatic telemetry synchronization remains deferred.
