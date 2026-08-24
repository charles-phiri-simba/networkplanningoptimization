# ADR 010 — At-least-once delivery and idempotency

## Status

Accepted for Phase 2.

## Context

Kafka delivery is at-least-once. Duplicate publishes must not create duplicate KPI observations.

## Decision

Treat `eventId` as the idempotency key. `kpi_observation.event_id` is unique. The projection service looks up `eventId` before insert and treats unique-constraint races as duplicates (ack, do not DLQ).

## Consequences

Retransmitted events are safe. `eventId` must be stable for a given simulated observation. Exactly-once Kafka processing is not claimed.
