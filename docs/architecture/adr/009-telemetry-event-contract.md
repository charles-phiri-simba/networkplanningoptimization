# ADR 009 — Telemetry event contract

## Status

Accepted for Phase 2.

## Context

Producers and consumers must share a stable, versioned payload. Avro, Protobuf, and Schema Registry are deferred.

## Decision

Use JSON schema version `1.0` with event type `CELL_KPI_OBSERVED`. Required fields: `eventId`, `eventType`, `schemaVersion`, `source`, `cellId`, `metric`, `value`, `unit`, `eventTime`, `synthetic`. `ingestedAt` may be present on the wire; the Java consumer overwrites it with receive time. Supported metrics are the Phase 1B KPI names. Ratio values must be in `[0, 1]`.

## Consequences

Unknown versions, types, metrics, or invalid values are unrecoverable and go to the DLQ. A later schema version can be introduced without replacing v1.
