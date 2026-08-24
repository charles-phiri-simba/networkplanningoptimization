# SNIP Phase 2 — Event & Telemetry Intelligence Implementation Specification

## 1. Authority and Baseline

This is the authorised implementation specification for Phase 2.

Start from:

```text
Branch: main
Commit: d5d5f65f6aec01b77fde2d4ec1321f670c43dee6
Phase 1B: COMPLETE
CI: PASS
Tests: 31 tests, 0 failures
```

Read the Phase 2 architecture document before implementation. Do not start later phases.

## 2. Objective

Implement the minimum event-driven telemetry foundation that proves:

```text
Synthetic Telemetry
      |
      v
Kafka
      |
      v
Projection
      |
      v
PostgreSQL Current + Historical State
      |
      v
Temporal Context
      |
      +------ Semantic Knowledge
      |
      v
Actual LLM
      |
      v
Grounded Engineering Insight
```

## 3. Preserve Existing Capabilities

Do not rewrite Site/gNB/Cell, PostgreSQL domain persistence, Flyway foundation, NetworkContextService boundary, semantic retrieval, SimpleVectorStore, Ollama integration, stub/lexical CI path, citations/provenance, recommendation API, observability or existing tests/ADRs.

## 4. Go Telemetry Simulator

Create a small Go component.

Support:
- `high-bler-load`
- `healthy-stable`
- `unknown-cell`

Responsibilities:

```text
Scenario -> TelemetryEvent -> Kafka Publish
```

No AI or domain reasoning in Go.

## 5. Kafka

Introduce local Kafka through Docker Compose.

Topics:

```text
snip.telemetry.cell-kpi.v1
snip.telemetry.cell-kpi.dlq.v1
```

Kafka key: `cellId`.

Do not build a full event mesh.

## 6. Event Contract

Implement the versioned JSON contract:

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

Validate required fields and supported schema version.

## 7. Delivery and Idempotency

Assume at-least-once delivery.

Duplicate `eventId` values must not create duplicate KPI observations. Add a uniqueness constraint where appropriate and test duplicate delivery explicitly.

## 8. Unknown Cell Handling

Unknown cells must not be created from telemetry.

Reject and route to the DLQ.

## 9. Retry / DLQ

Use bounded retries. After exhaustion, send unrecoverable events to the DLQ.

Cover unknown cell, invalid payload, invalid metric/value and processing failure.

## 10. Java Consumer

Implement a Spring Kafka consumer:

```text
Kafka Consumer
 -> TelemetryEventValidator
 -> TelemetryProjectionService
 -> PostgreSQL
```

Keep transport out of `NetworkContextService`.

## 11. TelemetryProjectionService

Implement deterministic responsibilities:

1. validate event;
2. resolve Cell;
3. deduplicate `eventId`;
4. persist KPI observation;
5. update current state if separately represented;
6. preserve event/ingestion times;
7. preserve source/synthetic provenance.

No LLM logic.

## 12. Persistence Evolution

Use Flyway to evolve KPI persistence with `eventId`, `eventTime`, `ingestedAt`, source and provenance as needed.

PostgreSQL remains the only telemetry projection/history store in Phase 2.

Do not add a time-series DB.

## 13. Temporal History

Expose the last N observations per KPI, recommended default N=5.

Order by event time appropriately.

## 14. Trend Model

Implement:

```text
INCREASING
DECREASING
STABLE
INSUFFICIENT_DATA
```

Trend calculation is deterministic Java, not LLM logic.

## 15. Context Evolution

Extend `CELL-001` context with current values, last-N history, trends, timestamps and provenance while preserving topology/config/neighbours.

## 16. Read APIs

Add conceptually:

```text
GET /api/v1/cells/{cellId}/telemetry
GET /api/v1/cells/{cellId}/telemetry/{metric}
```

Extend:

```text
GET /api/v1/cells/{cellId}/context
```

No telemetry write API.

## 17. Recommendation Integration

Preserve the existing recommendation endpoint.

The reasoning context must distinguish:

```text
USER QUESTION
STRUCTURED NETWORK CONTEXT
TEMPORAL KPI HISTORY / TRENDS
RETRIEVED ENGINEERING KNOWLEDGE
SAFETY INSTRUCTIONS
```

Do not vectorize operational telemetry merely to expose it to the LLM.

## 18. Canonical Scenario

`high-bler-load` must deterministically produce for `CELL-001`:

```text
BLER_DL: 0.04 -> 0.06 -> 0.09 -> 0.12
PRB_UTILIZATION_DL: 0.60 -> 0.68 -> 0.77 -> 0.84
```

Also implement stable healthy and unknown-cell scenarios.

## 19. Canonical Validation Question

> **What is happening on CELL-001, and what should I investigate?**

The real local-AI path must use Go-produced Kafka telemetry, Java consumption, PostgreSQL projection, temporal context, semantic RAG and actual Ollama LLM reasoning.

## 20. Observability

Preserve current telemetry and add useful event/projection indicators such as:

```text
telemetryEventsConsumed
telemetryEventsProjected
telemetryDuplicatesIgnored
telemetryEventsDlq
telemetryProjectionLatencyMs
lastEventTime
historyObservationCount
```

Do not overbuild observability infrastructure.

## 21. Go Tests

Test event construction, scenario determinism, required fields and validation rules.

## 22. Kafka Contract Tests

Test JSON serialization/deserialization, schema version, required fields, invalid payload handling and key selection where practical.

## 23. Projection Integration Tests

Verify:
- valid event persists;
- duplicate ignored;
- eventTime retained;
- ingestedAt retained;
- unknown cell does not create topology;
- invalid event reaches DLQ path;
- history ordering is correct.

Use PostgreSQL/Kafka-capable integration infrastructure where practical.

## 24. Trend Tests

Verify:
- `4,6,9,12 -> INCREASING`
- `12,9,6,4 -> DECREASING`
- stable -> `STABLE`
- insufficient observations -> `INSUFFICIENT_DATA`

## 25. Context Tests

Verify `CELL-001` includes current KPI, last-N history, trend, timestamps, provenance and existing topology/config/neighbours.

## 26. Recommendation Regression

Preserve all earlier tests. Add a deterministic test proving temporal evidence is assembled into the recommendation reasoning structure.

Ordinary CI must not require Ollama.

## 27. Local End-to-End Validation

Run:

```text
Go Simulator
 -> Kafka
 -> Java Consumer
 -> PostgreSQL
 -> NetworkContextService
 -> Vector Retrieval
 -> Ollama
 -> Recommendation
```

Record scenario, event IDs, topic/key, persisted observations, trends, context evidence, citations, answer, projection/context/retrieval/generation/total latency and DLQ evidence.

## 28. CI

CI should run Maven tests, PostgreSQL/Testcontainers tests, Kafka integration tests where feasible, Go tests and prior regressions.

CI must not require Ollama.

## 29. Docker Compose

Extend local Compose with Kafka and simulator support as appropriate. Keep ports configurable. Do not add Kubernetes.

## 30. ADRs

Create concise ADRs for:
1. Kafka/topic strategy
2. Telemetry event contract
3. At-least-once/idempotency
4. PostgreSQL telemetry projection
5. Temporal context/trend strategy
6. Go simulator boundary

## 31. Safety

Phase 2 remains read-only.

No network commands, configuration writes, vendor API actions or autonomous remediation.

## 32. Explicitly Out of Scope

Do NOT implement MCP, autonomous Agents, network writes, live OSS/NMS/EMS integration, vendor adapters, Digital Twin platform, Flink, Spark, Kafka Streams, Schema Registry, Avro, Protobuf, dedicated time-series DB, production Kubernetes/EKS, Agent Factory, RL or Phase 3 functionality.

## 33. Acceptance Criteria

### Baseline
- [ ] Existing 31-test baseline remains passing.
- [ ] Semantic RAG still works.
- [ ] Local-AI path remains available.
- [ ] No network writes exist.

### Go Simulator
- [ ] Go component builds.
- [ ] Three scenarios exist.
- [ ] Scenarios are deterministic.
- [ ] Events conform to canonical JSON.

### Kafka
- [ ] Local Kafka runs.
- [ ] Primary topic works.
- [ ] DLQ works.
- [ ] Records keyed by cellId.
- [ ] Consumer processes events.

### Idempotency / Validation
- [ ] Duplicate eventId does not duplicate observations.
- [ ] Unknown cell rejected.
- [ ] Invalid payload handled.
- [ ] Retry/DLQ behaviour works.

### Persistence
- [ ] Event-driven KPI observations persist.
- [ ] eventTime/ingestedAt retained.
- [ ] source/synthetic retained.
- [ ] Flyway migrations pass.

### Temporal Context
- [ ] Last-N history available.
- [ ] Current value identified.
- [ ] Trend calculated deterministically.
- [ ] CELL-001 exposes temporal context.

### Knowledge + AI
- [ ] Temporal context remains structured.
- [ ] Operational data is not unnecessarily vectorized.
- [ ] Canonical question runs through real local-AI path.
- [ ] Answer uses trends + retrieved knowledge.
- [ ] Citations remain valid.
- [ ] No autonomous root-cause claim.

### Observability / CI / Docs
- [ ] Event processing visible.
- [ ] Duplicate/DLQ behaviour observable.
- [ ] Projection/context/retrieval/generation latency observable.
- [ ] Java tests pass.
- [ ] Go tests pass.
- [ ] Integration path tested.
- [ ] CI does not require Ollama.
- [ ] README/docs updated.
- [ ] ADRs created.

### Scope Control
- [ ] No MCP.
- [ ] No Agents.
- [ ] No live network integration.
- [ ] No time-series DB.
- [ ] No Phase 3 implementation.

## 34. Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-2-COMPLETION-REPORT.md
```

Include Executive Summary, Baseline Verification, Scope Delivered, Go Simulator, Kafka Architecture, Event Contract, Delivery/Idempotency, DLQ/Retry, PostgreSQL Projection, Temporal Context, Trend Computation, APIs, Knowledge + Temporal Context Integration, Tests, Go Results, Kafka/PostgreSQL Integration, Canonical E2E Run, Unknown-Cell DLQ Run, Observability, Safety Review, ADRs, Performance, Acceptance PASS/FAIL, Limitations, Technical Debt, Lessons, Recommended Next Phase and Architectural Questions.

End with exactly one:

```text
PHASE 2 STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 2 STATUS: ACCEPTANCE NOT RECOMMENDED
```

## 35. Final Instruction to Cursor

Treat this as the authorised scope for **Phase 2 only**.

> **Events report what happened. Projections maintain state. Context Intelligence explains state. AI reasons over context.**

Do not broaden the phase. Do not start Phase 3.

When all criteria have been evaluated, STOP and produce the completion report for architectural review.
