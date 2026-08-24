# ADR 013 — Go simulator boundary

## Status

Accepted for Phase 2.

## Context

Phase 2 needs a synthetic producer to prove the event path. Live OSS/NMS/EMS adapters are forbidden. The simulator must not become an AI or domain-decision component.

## Decision

Implement a small Go module (`simulator/`) that maps named scenarios to canonical JSON events and publishes them to Kafka keyed by `cellId`. Scenarios: `high-bler-load`, `healthy-stable`, `unknown-cell`. Go contains no retrieval, LLM, trend, or topology logic.

## Consequences

Java remains the consumer, validator, projector, and reasoning host. The simulator can be replaced later by a real producer that honours the same JSON v1 contract.
