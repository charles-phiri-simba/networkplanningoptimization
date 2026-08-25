# ADR 049 — Vendor Telemetry Remains in Phase 2 Boundary

## Status

Accepted for Phase 7.

## Context

Mixing inventory import with KPI streaming would blur Phase 2 telemetry contracts and tempt vectorization of operational records.

## Decision

Phase 7 imports inventory, topology, and configuration only. Vendor telemetry adapters, Kafka vendor topics, and vectorization of imported operational records are prohibited. KPI/event intelligence remains the Phase 2 path.

## Consequences

Imported cells are visible to existing Context reads without a new telemetry pipeline.
