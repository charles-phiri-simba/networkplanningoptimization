# ADR 011 — PostgreSQL telemetry projection

## Status

Accepted for Phase 2.

## Context

Events are not themselves queryable operational state. Context Intelligence needs current and bounded historical KPI observations. A dedicated time-series database is out of scope.

## Decision

Project validated events into PostgreSQL `kpi_observation`. `observed_at` stores event time. `ingested_at` stores receive time. Current state is the latest row per metric by event time. No extra current-state table in Phase 2. Telemetry must not insert Site/gNB/Cell rows; unknown cells are rejected.

## Consequences

History is bounded by the existing recency window plus last-N in context assembly. Scale-out TSDB work is deferred.
