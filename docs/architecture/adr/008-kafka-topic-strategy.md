# ADR 008 — Kafka topic strategy

## Status

Accepted for Phase 2.

## Context

Phase 2 needs a minimal event backbone for synthetic cell KPI telemetry. A full event mesh, Kafka Streams, Schema Registry, and MSK/EKS are out of scope.

## Decision

Use a single local Kafka cluster (Docker Compose / Testcontainers). Primary topic `snip.telemetry.cell-kpi.v1`. Dead-letter topic `snip.telemetry.cell-kpi.dlq.v1`. Records are keyed by `cellId` so a cell’s events stay ordered in one partition. The Java consumer is opt-in (`snip.kafka-enabled=true`) so CI’s default lexical/stub path does not require a broker.

## Consequences

This is not an enterprise event platform. Later phases may add more topics without changing this v1 contract.
