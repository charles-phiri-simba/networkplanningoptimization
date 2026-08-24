# SNIP Phase 2 — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-24 (reconciled re-run)  
**Architecture:** `docs/architecture/SNIP-PHASE-2-EVENT-TELEMETRY-INTELLIGENCE-ARCHITECTURE.md`  
**Contract:** `docs/implementation/SNIP-PHASE-2-EVENT-TELEMETRY-INTELLIGENCE-SPECIFICATION.md`  
**Baseline:** `d5d5f65f6aec01b77fde2d4ec1321f670c43dee6` on `main` (Phase 1B complete, 31 tests). HEAD is still this commit; Phase 2 is uncommitted working-tree work.  
**Method:** Inspect working tree; `mvn -B test` (PostgreSQL + Kafka Testcontainers); `go test ./...` and `go build` in `simulator/`; single-JVM local-AI E2E (Go simulator → Compose Kafka → current Java consumer → PostgreSQL → vector RAG → host Ollama `qwen2.5:7b`). Phase 3 was not started. Git push / new baseline were not authorised.

---

## 0. Reconciliation (read this first)

An earlier assistant message said Phase 2 was **blocked** by the old Phase 1B cursor rule, and a later message said Phase 2 implementation/tests/E2E had **already completed**. Both were describing the same working tree at different moments:

| Claim | What was actually true |
|-------|------------------------|
| “Phase 2 is blocked” | The always-on cursor rule still authorised Phase **1B only**, so Auto-review blocked some writes and some E2E commands. Implementation files were nonetheless already on disk. |
| “Phase 2 is complete (46 tests, E2E)” | The source, tests, simulator, V3, and a local-AI path **did** exist. The prior E2E was real but **split across two JVMs** and mixed two different POST failures into one sentence. |

This pass **approved the Phase 2 rule**, **did not delete** the existing implementation, checked it against the architecture/specification, corrected only documentation/status drift, re-ran verification, and re-executed E2E from **current** code on **one** JVM.

### 0.1 Checklist against the working tree (this pass)

| Question | Answer |
|----------|--------|
| Which Phase 2 files currently exist? | See section 3. All listed paths are on disk. |
| Which Phase 2 source changes are implemented? | Kafka consumer/DLQ/idempotent projection, Flyway V3, last-N temporal context, deterministic trends, telemetry read APIs, prompt section, Go simulator. |
| Are both architecture and specification documents in the repo? | **Yes.** `docs/architecture/SNIP-PHASE-2-EVENT-TELEMETRY-INTELLIGENCE-ARCHITECTURE.md` and `docs/implementation/SNIP-PHASE-2-EVENT-TELEMETRY-INTELLIGENCE-SPECIFICATION.md`. |
| Does the Go simulator exist and build? | **Yes.** `go test ./...` PASS; `go build ./cmd/simulator` exit 0 (`go1.26.4 windows/amd64`). |
| Kafka consumer / DLQ / idempotency? | **Yes.** `KafkaTelemetryConfig`, `TelemetryEventListener`, `TelemetryProjectionService`, unique `event_id`. |
| Flyway V3? | **Yes.** `src/main/resources/db/migration/V3__kpi_observation_event_telemetry.sql`. |
| Temporal context / trend? | **Yes.** `NetworkContextService.buildTelemetry`, `TrendClassifier`. |
| Do 46 tests really pass **now**? | **Yes.** `mvn -B test` at 13:21:22 +02:00: **Tests run: 46, Failures: 0, Errors: 0, Skipped: 0**. |
| Was local Kafka/Postgres/Ollama E2E executed from **current** code? | **Yes, this pass.** One JVM on `127.0.0.1:18086` with `local-ai` + `snip.kafka-enabled=true` against Compose Postgres `55432` and Kafka `19092`. Prior-session E2E used two JVMs (18084 consumer, 18085 recommendation) and is **not** the verification of record. |
| Did `SNIP-PHASE-2-COMPLETION-REPORT.md` already exist? | **Yes.** It existed before this pass but understated the canonical-POST sequence. This file replaces it. |

### 0.2 Canonical POST — previous session vs this re-run

The previous report collapsed three different outcomes into one line. They were **not** the same failure.

**Previous session (not the verification of record):**

1. **HTTP 400** — PowerShell JSON escaping produced invalid JSON (`HttpMessageNotReadableException`). Client error. The application did not evaluate retrieval or generation.
2. **HTTP 200, `retrievalEmpty=true`** — valid JSON posted to the **18084** JVM (Kafka consumer, **before** query expansion). Vector min-score `0.60`; the exact canonical question did not mention BLER, so no chunk cleared the threshold. Citations were correctly **not** fabricated.
3. **HTTP 200, retrieval succeeded** — query expansion was then added; the POST was repeated against **18085** (local-AI, Kafka **off**, same Postgres). Vector hits succeeded. That JVM was **not** the Kafka consumer.

**This re-run (verification of record):**

- Single JVM **18086**, current working tree, Kafka **on**, `local-ai` **on**.
- Canonical question posted from a JSON file (no PowerShell escaping).
- HTTP 200, `retrievalEmpty=false`, `retrievalMode=vector`, 3 citations, Ollama `qwen2.5:7b`.

Query expansion remains an implementation choice for architectural review (sections 14 and 27). It does **not** vectorize telemetry values.

---

## 1. Executive Summary

Phase 2 adds event-driven synthetic telemetry on the accepted Phase 1B domain: a Go simulator publishes versioned JSON KPI events to Kafka; a Java consumer validates, deduplicates, and projects them into PostgreSQL; `NetworkContextService` exposes last-N history and deterministic trends; Knowledge Intelligence still retrieves engineering notes (not telemetry vectors); the local LLM reasons over structured temporal context plus citations.

The mandatory local-AI path **actually ran from current code**:

```text
Go high-bler-load → snip.telemetry.cell-kpi.v1 (key=CELL-001)
  → Java consumer on :18086 → PostgreSQL
  → temporal context (BLER_DL and PRB_UTILIZATION_DL INCREASING)
  → retrieval query expanded with NR / n78 / BLER_DL / PRB_UTILIZATION_DL
  → vector retrieval (min-score 0.60) → Ollama qwen2.5:7b
```

The model used precomputed INCREASING trends and retrieved BLER/interference notes, returned three valid citations, labelled `SNIP_SIMULATOR` / synthetic, and recommended investigation rather than a confirmed root cause.

Deterministic CI remains lexical + stub with Kafka **off** by default. Re-run `mvn -B test`: **46 tests, 0 failures**. `go test ./...`: **PASS**. No Ollama in CI.

Cursor rule `.cursor/rules/snip-architecture.mdc` now authorises **Phase 2 only** and still prohibits MCP, autonomous Agents, live OSS/NMS/EMS, network writes, Schema Registry, Avro/Protobuf, Flink/Spark/Kafka Streams, dedicated time-series DB, Kubernetes/EKS, RL, and Phase 3.

**PHASE 2 STATUS: ARCHITECTURALLY ACCEPTED**

---

## 2. Baseline Verification

| Check | Result |
|-------|--------|
| Started from `d5d5f65f6aec01b77fde2d4ec1321f670c43dee6` | Yes (`git rev-parse HEAD` still this commit) |
| Phase 1A/1A.1/1B tests still pass | PASS (31 baseline tests remain in the 46) |
| Lexical + stub default | PASS (`retrievalMode=lexical` in CI) |
| Semantic RAG / SimpleVectorStore / Ollama profile | PASS (not rewritten) |
| No network-write APIs | PASS |
| Kafka not inside `NetworkContextService` | PASS (no Kafka imports) |
| Ordinary CI does not require Ollama | PASS |
| Kafka default off | PASS (`snip.kafka-enabled=${SNIP_KAFKA_ENABLED:false}`) |

---

## 3. Files in the working tree

### New

- `docs/architecture/SNIP-PHASE-2-EVENT-TELEMETRY-INTELLIGENCE-ARCHITECTURE.md`
- `docs/implementation/SNIP-PHASE-2-EVENT-TELEMETRY-INTELLIGENCE-SPECIFICATION.md`
- `docs/implementation/SNIP-PHASE-2-COMPLETION-REPORT.md`
- `docs/architecture/adr/008-kafka-topic-strategy.md` through `013-go-simulator-boundary.md`
- `simulator/` (`cmd/simulator`, `internal/event`, `internal/scenario`, `Dockerfile`, `go.mod`)
- `src/main/java/com/simba/snip/npo/telemetry/` (event, validator, catalog, listener, projection, metrics, trend, DLQ exception)
- `src/main/java/com/simba/snip/npo/config/KafkaTelemetryConfig.java`
- `src/main/java/com/simba/snip/npo/api/KpiSeriesDto.java`
- `src/main/resources/db/migration/V3__kpi_observation_event_telemetry.sql`
- `src/test/java/com/simba/snip/npo/telemetry/` (`TelemetryEventContractTest`, `TelemetryProjectionServiceTest`, `TelemetryKafkaTest`, `TrendClassifierTest`)

### Extended (Phase 1B types, not rewritten)

- `KpiObservationEntity` / repository, `NetworkContextService`, `CellContext`, `CellController`, `AssembledPrompt`, `RecommendationService`, `RecommendationResponse`, `StubRecommendationGenerator`, `application.yml`, `docker-compose.yml`, `pom.xml`, `.github/workflows/ci.yml`, `README.md`, CONTEXT/STATUS, cursor rule

---

## 4. Scope Delivered

- Go simulator with `high-bler-load`, `healthy-stable`, `unknown-cell`
- Local Kafka via Compose profile `telemetry` (topics `snip.telemetry.cell-kpi.v1` and `.dlq.v1`)
- JSON v1 event contract and validator
- Spring Kafka consumer (opt-in `snip.kafka-enabled=true`)
- `TelemetryProjectionService` (validate → resolve cell → idempotent persist)
- Flyway V3: unique `event_id`, `ingested_at`; `observed_at` remains event time
- Last-N=5 temporal series + deterministic trends
- Read APIs `GET .../telemetry` and `GET .../telemetry/{metric}`; context includes telemetry
- Prompt section `TEMPORAL KPI HISTORY / TRENDS`
- Retrieval **query** expansion from technology, band, and trending metric **names** (telemetry is **not** vectorized)
- Observability counters/logs
- Tests, CI Go job, ADRs 008–013, README

Accepted implementation nuances (not treated as architectural drift):

- Consumer-owned `ingestedAt` (`Instant.now()`; producer timestamp ignored)
- Latest state derived by `eventTime` (`observed_at`)
- Ratios stored raw; formatted as `0.12 ratio (12%)` for humans/LLMs
- Simulator observations take precedence over seed rows for trend calculations
- Kafka disabled by default for ordinary CI; Kafka ITs use Testcontainers

---

## 5. Go Simulator

Module: `simulator/` (Go 1.22 module; host toolchain `go1.26.4`). Path: scenario → `TelemetryEvent` JSON → Kafka produce keyed by `cellId`. No AI or domain decision logic.

| Scenario | Cell | Behaviour |
|----------|------|-----------|
| `high-bler-load` | CELL-001 | BLER 0.04→0.12, PRB 0.60→0.84, deterministic event IDs |
| `healthy-stable` | CELL-002 | Stable BLER 0.008 / PRB 0.41 |
| `unknown-cell` | CELL-MISSING | Valid schema, unknown topology |

`go test ./...`: event + scenario PASS. `go build ./cmd/simulator`: success.

---

## 6. Kafka Architecture

Minimal backbone only. Compose service `kafka` (`apache/kafka:3.8.1`, KRaft) under profile `telemetry`. Host port `SNIP_KAFKA_PORT` (default 9092; E2E used 19092). Dual listeners: `kafka:29092` in Compose, `127.0.0.1:<host-port>` on the host.

Java consumer is **off** unless `SNIP_KAFKA_ENABLED=true`. Default Spring Boot tests exclude `KafkaAutoConfiguration` so the original 31 tests need no broker.

---

## 7. Event Contract

JSON schemaVersion `1.0`, eventType `CELL_KPI_OBSERVED`. Required: `eventId`, `eventType`, `schemaVersion`, `source`, `cellId`, `metric`, `value`, `unit`, `eventTime`, `synthetic`. `ingestedAt` on the wire is ignored; the Java consumer sets receive time.

Supported metrics match Phase 1B KPI names. Ratio values must be in `[0, 1]`.

---

## 8. Delivery / Idempotency

At-least-once. Unique `kpi_observation.event_id`. Lookup-before-insert plus unique-constraint race → `DUPLICATE` (ack, not DLQ). Unit-tested in `TelemetryProjectionServiceTest`. This E2E re-run republished `high-bler-load`; current consumer logged `telemetryDuplicatesIgnored=true` for all eight event IDs.

---

## 9. DLQ / Retry

Bounded retries (`FixedBackOff` 200 ms × 2). `UnrecoverableTelemetryException` (invalid JSON/schema/metric/value, unknown cell) is not retried and is published to `snip.telemetry.cell-kpi.dlq.v1`.

`TelemetryKafkaTest` (Testcontainers Confluent 7.6.1): keyed persist + unknown-cell/invalid JSON reach the DLQ; no `CELL-MISSING` topology row.

Live E2E (this pass): `unknown-cell` consumed; DeadLetter producer started; `GET /api/v1/cells/CELL-MISSING` → **404**. No topology create.

---

## 10. PostgreSQL Projection

No time-series DB. Current state = latest row per metric by event time. `observed_at` = eventTime; `ingestedAt` = consumer clock.

---

## 11. Temporal Context

Last N=5 per metric, chronological by event time. If any `SNIP_SIMULATOR` rows exist for that metric, seed rows are excluded from that series so demo `BLER_DL=0.12` does not invert `0.04→0.12`. Topology/config/neighbours unchanged.

---

## 12. Trend Computation

Deterministic Java first-versus-last in the window: `INCREASING` / `DECREASING` / `STABLE` / `INSUFFICIENT_DATA`. Tests: `4,6,9,12` INCREASING; reverse DECREASING; equals STABLE; fewer than 2 points INSUFFICIENT_DATA. LLM is instructed not to recalculate trends.

---

## 13. APIs

Read-only additions:

```text
GET /api/v1/cells/{cellId}/telemetry
GET /api/v1/cells/{cellId}/telemetry/{metric}
```

`GET /api/v1/cells/{cellId}/context` now includes `telemetry[]`. Recommendation response adds `historyObservationCount` and `lastEventTime`. No telemetry write API.

---

## 14. Knowledge + Temporal Context Integration

Prompt sections remain distinct: SAFETY, USER QUESTION, STRUCTURED NETWORK CONTEXT, TEMPORAL KPI HISTORY / TRENDS, RETRIEVED ENGINEERING KNOWLEDGE.

Operational telemetry is not stored in `SimpleVectorStore`. When cell context is present, `RecommendationService.retrievalQuery` appends `technology`, `band`, and INCREASING/DECREASING metric **names** to the user question. For CELL-001 that yields a query along the lines of:

```text
What is happening on CELL-001, and what should I investigate? NR n78 BLER_DL PRB_UTILIZATION_DL
```

That is query expansion, not embedding of KPI time series. It exists because the **exact** specification question, with `retrieve-min-score=0.60`, did not retrieve on the 18084 JVM. CI lexical case5 still uses a slightly richer question (`with high BLER`) so the stub path does not depend on expansion.

---

## 15. Tests (re-run this pass)

| Suite | Result |
|-------|--------|
| `mvn -B test` (full, including `TelemetryKafkaTest`) | **46 tests, 0 failures** (BUILD SUCCESS, 22.150 s after warm Kafka image) |
| `go test ./...` in `simulator/` | PASS (event + scenario; `cmd/simulator` has no unit tests) |
| `go build ./cmd/simulator` | PASS |
| Phase 1A/1B regressions | PASS |

---

## 16. Canonical E2E Run (verification of record — 18086)

| Item | Value |
|------|--------|
| JVM | `127.0.0.1:18086`, current working tree, `local-ai` + Kafka on |
| Postgres / Kafka | Compose `55432` / `19092` |
| Scenario | `high-bler-load` republished; 8/8 `telemetryDuplicatesIgnored` |
| Topic / key | `snip.telemetry.cell-kpi.v1` / `CELL-001` |
| Trends (GET telemetry) | BLER_DL INCREASING 0.04→0.12; PRB_UTILIZATION_DL INCREASING 0.60→0.84; seed-only metrics INSUFFICIENT_DATA |
| Question | What is happening on CELL-001, and what should I investigate? |
| Request | JSON file (`Content-Type: application/json`), not PowerShell-escaped body |
| HTTP | 200 |
| Context evidence | CELL-001 / GNB-001 / SITE-001 / source=`SNIP_SIMULATOR` / synthetic=true |
| Retrieval | vector, 3 hits, scores ≈ 0.658 / 0.636 / 0.634 |
| Citations | `sample-bler-midband`, `sample-interference`, `sample-mid-band-context` |
| Generation | Ollama `qwen2.5:7b` |
| Latencies | context 12 ms, retrieval 113 ms, generation 15777 ms, total 15905 ms |
| History | `historyObservationCount=12`, `lastEventTime=2026-08-24T10:15:00Z` |
| Answer | Used INCREASING BLER/PRB, investigation checklist, not confirmed RCA, read-only |

Without query expansion, the same canonical question previously scored below 0.60 on this corpus. Expansion made RAG usable without vectorizing telemetry. That choice is called out in section 27.

---

## 17. Unknown-Cell DLQ Run (this pass)

`unknown-cell` published `unknown-cell-bler-dl-01` key=`CELL-MISSING`. Consumer treated it as unrecoverable (DeadLetter producer started). `GET /api/v1/cells/CELL-MISSING` → 404. Kafka IT independently asserted DLQ payload and unchanged cell count.

---

## 18. Observability

Logs/counters: `telemetryEventsConsumed`, `telemetryEventsProjected`, `telemetryDuplicatesIgnored`, `telemetryEventsDlq`, `telemetryProjectionLatencyMs`. Recommendation logs include `historyObservationCount` and `lastEventTime`. No extra observability platform.

---

## 19. Safety Review

Read-only APIs only. No apply/execute/OSS/NMS writes. Stub and LLM instructions forbid claiming a network action or a definitive autonomous root cause.

---

## 20. ADRs

| ADR | Topic |
|-----|--------|
| 008 | Kafka / topic strategy |
| 009 | Telemetry event contract |
| 010 | At-least-once / idempotency |
| 011 | PostgreSQL telemetry projection |
| 012 | Temporal context / trend |
| 013 | Go simulator boundary |

---

## 21. Performance (this pass)

| Path | Observation |
|------|-------------|
| Context resolution (E2E) | 12 ms |
| Vector retrieval (E2E) | 113 ms |
| Local LLM generation | ~16 s (`qwen2.5:7b`) |
| Kafka IT (warm image) | ~8 s of the 22 s Maven run |
| Duplicate projection (E2E republish) | ~5–14 ms per event |

---

## 22. Acceptance PASS/FAIL

### Baseline
- [x] Existing 31-test baseline remains passing
- [x] Semantic RAG still works
- [x] Local-AI path remains available
- [x] No network writes exist

### Go / Kafka / idempotency / persistence / temporal / AI
- [x] Go builds and three scenarios exist
- [x] Local Kafka + primary/DLQ topics + `cellId` key
- [x] Duplicate `eventId` ignored; unknown cell rejected; invalid payload DLQ
- [x] Flyway V3; eventTime/ingestedAt/source/synthetic retained
- [x] Last-N, current, deterministic trend on CELL-001
- [x] Canonical question through real local-AI path with citations (this pass, one JVM)
- [x] CI: Maven + Go; no Ollama required
- [x] README + ADRs

### Scope control
- [x] No MCP, Agents, live OSS, TSDB, Phase 3

---

## 23. Limitations

- Trend uses first-versus-last in the window (no MIXED class).
- Simulator-preferred series when `SNIP_SIMULATOR` data exists for a metric (documented in ADR 012).
- Canonical question needs query expansion to clear `retrieve-min-score=0.60` on this corpus.
- Compose Kafka uses Apache 3.8.1; Testcontainers Kafka uses Confluent 7.6.1 (both Kafka protocol, not a mesh).
- `ingestedAt` in Go JSON is a placeholder; Java overwrites it.
- The LLM may paraphrase history order even when told not to recalculate trends (this run: it still used INCREASING correctly).

---

## 24. Technical Debt

- Hard-coded listener `groupId=snip-npo-telemetry` (should bind to `spring.kafka.consumer.group-id`; values currently match).
- DLQ recoverer logging is Kafka-framework verbose; application counter is sufficient but not a dedicated metric backend.
- CI lexical case5 does not POST the exact specification wording (adds “with high BLER”) so stub retrieval does not depend on query expansion.

---

## 25. Lessons

- Default Surefire does not run `*IT.java`; Kafka integration was named `TelemetryKafkaTest`.
- Canonical natural-language questions can miss a 0.60 vector threshold unless the retrieval query includes structured KPI names.
- Host ports 8080/18083/5432/9092 collide on this workstation; `SNIP_*_PORT` remains necessary.
- A 400 from a malformed client body is not an empty-retrieval result; keep those failures separate in reports.

---

## 26. Recommended Next Phase

Phase 3 (Governed Action + MCP) only when explicitly authorised. Do not start it from this report.

---

## 27. Architectural Questions — resolved

Phase 2 was architecturally accepted on 2026-08-24. The four questions are closed as follows:

1. **Simulator-preferred temporal series — ACCEPT for Phase 2.** When `SNIP_SIMULATOR` observations exist for a metric, they may form the temporal series instead of Phase 1B seed observations. Do not introduce a generic source-precedence framework yet.
2. **Context-aware retrieval query expansion — ACCEPT.** Preserve enrichment using structured descriptors such as technology, band, and relevant trending metric names. Do not lower the `0.60` vector threshold merely to make the canonical scenario pass. Do not inject diagnoses, inferred root causes, or generated conclusions into the retrieval query. Operational telemetry values/history must remain outside the vector store.
3. **Java-owned `ingestedAt` — ACCEPT.** SNIP ingestion time remains authoritative. Do not redesign the event contract in this phase solely to remove the producer placeholder.
4. **MIXED trend classification — DEFER.** Preserve the current deterministic Phase 2 trend model (`INCREASING` / `DECREASING` / `STABLE` / `INSUFFICIENT_DATA`; first-versus-last in the last-N window).

---

PHASE 2 STATUS: ARCHITECTURALLY ACCEPTED
