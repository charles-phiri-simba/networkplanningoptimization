# SNIP Phase 2 — Event & Telemetry Intelligence Architecture

## 1. Purpose

Phase 2 introduces the event-driven and temporal intelligence foundation of the Simba Network Intelligence Platform (SNIP).

Phase 1B established a persistent structured network domain and reasoning-ready context. Phase 2 extends that foundation so SNIP can ingest changing network observations, project them into current and historical operational state, derive deterministic trends, and expose time-aware context to Knowledge Intelligence and AI reasoning.

> **Architectural question:** Can SNIP ingest changing network telemetry, maintain a time-aware operational view of a cell, and combine that temporal context with engineering knowledge for AI reasoning?

## 2. Architectural Evolution

```text
Synthetic / Future Network Sources
              |
              v
        Telemetry Events
              |
              v
       Ingestion Boundary
              |
              v
            Kafka
              |
              v
       Context Projection
              |
        +-----+------+
        |            |
        v            v
 Current State   Historical State
        |            |
        +-----+------+
              |
              v
      Temporal Context
              |
              +------ Engineering Knowledge
              |
              v
          AI Reasoning
              |
              v
       Engineering Insight
```

The major architectural addition is **time-aware operational context**.

## 3. Core Principle

> **Events report what happened. Projections maintain state. Context Intelligence explains state. AI reasons over context.**

```text
Event != State != Context != Reasoning
```

## 4. Telemetry Source

Phase 2 uses a synthetic telemetry simulator. No live OSS, EMS, NMS, vendor API, gNB, AMF or UDM integration is introduced.

The simulator validates event contracts, ordering, duplicate handling, ingestion, projection, temporal context, trend reasoning and failure handling.

## 5. Go Telemetry Simulator

Go is introduced in Phase 2 as the implementation language for the synthetic telemetry producer.

```text
Scenario Generator
        |
        v
Canonical Telemetry Event
        |
        v
Kafka Producer
```

The Go component must not contain AI reasoning or domain decision logic.

## 6. Event Backbone

Kafka is introduced as a minimal event backbone.

Primary topic:

```text
snip.telemetry.cell-kpi.v1
```

Dead-letter topic:

```text
snip.telemetry.cell-kpi.dlq.v1
```

This is not a full enterprise event mesh.

## 7. Canonical Telemetry Event

```json
{
  "eventId": "evt-123",
  "eventType": "CELL_KPI_OBSERVED",
  "schemaVersion": "1.0",
  "source": "SNIP_SIMULATOR",
  "cellId": "CELL-001",
  "metric": "BLER_DL",
  "value": 0.12,
  "unit": "ratio",
  "eventTime": "2026-08-24T10:15:00Z",
  "ingestedAt": "2026-08-24T10:15:01Z",
  "synthetic": true
}
```

Semantics:
- `eventId` — unique event identity for idempotency
- `eventType` — semantic classification
- `schemaVersion` — contract version
- `source` — origin
- `cellId` — stable SNIP domain identifier
- `metric` — canonical KPI name
- `value` / `unit` — observation
- `eventTime` — when the measurement occurred
- `ingestedAt` — when SNIP received it
- `synthetic` — provenance flag

## 8. Time Semantics

SNIP distinguishes event time, ingestion time and processing time. Advanced windowing is deferred, but the semantics must be correct from the start.

## 9. Kafka Key Strategy

Kafka records are keyed by `cellId`, preserving per-cell ordering within a partition.

## 10. Delivery Semantics

Assume **at-least-once delivery**. Consumers must be idempotent using `eventId`.

```text
Telemetry Event
       |
       v
eventId lookup
       |
  +----+----+
  |         |
seen      unseen
  |         |
drop      process
```

## 11. Unknown Cell Policy

Telemetry must not create topology.

Unknown cells are rejected and routed to the DLQ.

## 12. Retry and DLQ

Use bounded retries. Unrecoverable events go to `snip.telemetry.cell-kpi.dlq.v1`.

DLQ cases include invalid schema, unknown cell, invalid metric/value and unrecoverable processing failure.

## 13. Serialization

Use versioned JSON in Phase 2. Avro, Protobuf and Schema Registry are deferred.

## 14. Java/Spring Consumer

```text
Kafka Consumer
      |
      v
TelemetryEventValidator
      |
      v
TelemetryProjectionService
      |
      v
PostgreSQL
```

Kafka transport logic must not be embedded inside `NetworkContextService`.

## 15. TelemetryProjectionService

Responsibilities:
1. validate event;
2. resolve Cell;
3. deduplicate by `eventId`;
4. persist KPI observation;
5. maintain latest/current state where applicable;
6. preserve time and provenance;
7. emit operational telemetry.

This component is deterministic infrastructure, not AI.

## 16. PostgreSQL Role

PostgreSQL remains the projection store for current operational state and bounded telemetry history.

No dedicated time-series database is introduced in Phase 2.

## 17. KPI Observation Evolution

Conceptually evolve KPI observations to include:

```text
id
eventId
cell reference
metric
value
unit
eventTime
ingestedAt
source
synthetic
```

`eventId` should be unique.

## 18. Temporal Context Intelligence

`NetworkContextService` evolves to expose current state, recent history, deterministic trends, neighbour context, configuration and provenance.

Example:

```text
BLER_DL:
  current: 12%
  previous: 9%
  trend: INCREASING

PRB_UTILIZATION_DL:
  current: 84%
  previous: 77%
  trend: INCREASING
```

## 19. Deterministic Trend Computation

Trend classification is deterministic Java logic:

```text
INCREASING
DECREASING
STABLE
INSUFFICIENT_DATA
```

> **Use deterministic computation for deterministic facts. Use AI for reasoning.**

## 20. Temporal Window

Use a bounded last-N-observations model, recommended default: last 5 observations per KPI.

## 21. Simulator Scenarios

Implement at minimum:

- `high-bler-load`
- `healthy-stable`
- `unknown-cell`

Canonical scenario:

```text
BLER_DL:
4% -> 6% -> 9% -> 12%

PRB_UTILIZATION_DL:
60% -> 68% -> 77% -> 84%
```

## 22. Telemetry APIs

Read-only APIs conceptually:

```text
GET /api/v1/cells/{cellId}/telemetry
GET /api/v1/cells/{cellId}/telemetry/{metric}
GET /api/v1/cells/{cellId}/context
```

The context endpoint now includes temporal/trend information.

## 23. AI Reasoning Context

The LLM receives structured current state, temporal history/trends, neighbours/configuration and retrieved engineering knowledge.

The model does not calculate deterministic trends itself.

## 24. Canonical Phase 2 Question

> **What is happening on CELL-001, and what should I investigate?**

Expected reasoning connects temporal KPI change, load, neighbour/configuration context and engineering knowledge without claiming a confirmed root cause.

## 25. Runtime Architecture

```text
                    Go Telemetry Simulator
                             |
                             v
                snip.telemetry.cell-kpi.v1
                             |
                           Kafka
                             |
                             v
                    Java Kafka Consumer
                             |
                             v
                TelemetryProjectionService
                             |
                             v
                       PostgreSQL
                             |
              +--------------+--------------+
              |                             |
              v                             v
        Latest Context                KPI History
              |                             |
              +--------------+--------------+
                             |
                             v
                  NetworkContextService
                             |
                 +-----------+-----------+
                 |                       |
                 v                       v
        Knowledge Intelligence       Temporal Context
                 |                       |
                 +-----------+-----------+
                             |
                             v
                            LLM
                             |
                             v
                  Engineering Insight
```

## 26. Explicitly Deferred

Do not introduce MCP, autonomous Agents, network writes, live OSS/NMS/EMS integration, vendor adapters, full Digital Twin, Flink, Spark, Kafka Streams, Schema Registry, Avro, Protobuf, a dedicated time-series DB, production Kubernetes/EKS, Agent Factory or RL.

## 27. Locked Decisions

- Telemetry source: synthetic simulator
- Simulator language: Go
- Event backbone: Kafka
- Serialization: JSON v1
- Topic: `snip.telemetry.cell-kpi.v1`
- Kafka key: `cellId`
- Delivery: at least once
- Deduplication: `eventId`
- Unknown cells: reject to DLQ
- DLQ: `snip.telemetry.cell-kpi.dlq.v1`
- Consumer: Java/Spring
- Projection store: PostgreSQL
- Time-series DB: deferred
- Trend computation: deterministic Java
- Temporal window: last N observations
- AI input: structured state + trend/history + RAG
- MCP: deferred
- Network actions: none

## 28. Architectural Outcome

At the end of Phase 2, SNIP should progress from **Knowledge + Static Context** to:

```text
Knowledge
   +
Structured Network State
   +
Event-Driven Operational State
   +
Temporal History
   +
Deterministic Trends
   |
   v
AI Reasoning
```

This becomes the foundation for later Digital Twin evolution, assurance intelligence, anomaly detection, streaming analytics and governed action intelligence.
