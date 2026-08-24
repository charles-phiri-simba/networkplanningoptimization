# SNIP Phase 3 — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-24  
**Architecture:** `docs/architecture/SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-ARCHITECTURE.md`  
**Contract:** `docs/implementation/SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-SPECIFICATION.md`  
**Baseline:** `8c70537bec048f2bf7e55c0ca626c8deec7b8670` on `main` (Phase 2 architecturally accepted, 46 tests). HEAD is still this commit; Phase 3 is uncommitted working-tree work.  
**Method:** Extend Phase 2; `mvn -B test` (PostgreSQL + Kafka Testcontainers); `go test ./...` in `simulator/`; single-JVM local-AI E2E (Kafka JSON event → current Java consumer → PostgreSQL projection + detection → vector RAG → host Ollama `qwen2.5:7b`). Phase 4 was not started. Git push / new baseline were not authorised.

Root copies of the architecture and specification also exist (as requested): `SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-ARCHITECTURE.md` and `SNIP-PHASE-3-ASSURANCE-DECISION-INTELLIGENCE-SPECIFICATION.md`. Architecture and specification do not conflict.

---

## 1. Executive Summary

Phase 3 converts Phase 2 temporal degradation into a persisted, evidence-backed **Assurance Case** and uses **Decision Intelligence** to explain and prioritise investigation without taking action.

The locked path that ran from current code:

```text
Kafka JSON CELL_KPI_OBSERVED (CELL-001, BLER_DL=0.12)
  → Java consumer on :18087 → PostgreSQL projection
  → DegradingRadioQualityDetector (BLER_DL >= 0.08 AND trend INCREASING)
  → AssuranceCase DEGRADING_RADIO_QUALITY + operational evidence
  → Decision Intelligence + existing RAG (query expanded with NR / n78 / case type / trending metric names)
  → vector retrieval (min-score 0.60) → Ollama qwen2.5:7b
  → advisory assessment (humanReviewRequired=true)
```

Detection is a pure Java function. The LLM writes the prose summary only; severity and confidence stay on the persisted case. `humanReviewRequired` is always `true`. No apply/execute/OSS write APIs exist.

Deterministic CI remains lexical + stub with Kafka **off** by default. Re-run `mvn -B test`: **60 tests, 0 failures** (Finished at 2026-08-24T14:53:55+02:00). `go test ./...`: **PASS**. No Ollama in CI.

Cursor rule `.cursor/rules/snip-architecture.mdc` now authorises **Phase 3 only** and still prohibits MCP, autonomous Agents, live OSS/NMS/EMS, network writes, ITSM, Schema Registry, Avro/Protobuf, Flink/Spark/Kafka Streams, dedicated time-series DB, Kubernetes/EKS, RL, ML anomaly detection, and Phase 4.

---

## 2. Baseline Verification

| Check | Result |
|-------|--------|
| Started from `8c70537bec048f2bf7e55c0ca626c8deec7b8670` | Yes (`git rev-parse HEAD` still this commit) |
| Phase 1A/1A.1/1B/2 tests still pass | PASS (46 baseline tests remain in the 60) |
| Lexical + stub default | PASS (`retrievalMode=lexical` in CI) |
| Semantic RAG / SimpleVectorStore / Ollama profile | PASS (not rewritten) |
| Kafka telemetry path remains functional | PASS (`TelemetryKafkaTest` + live E2E consume) |
| Kafka not inside `NetworkContextService` | PASS (detection reads context after projection) |
| Kafka default off | PASS (`snip.kafka-enabled=${SNIP_KAFKA_ENABLED:false}`) |
| Ordinary CI does not require Ollama | PASS (`.github/workflows/ci.yml`: Go + `mvn -B test`) |
| No network-write APIs | PASS (only `POST /api/v1/recommendations`) |
| Telemetry / assurance state not vectorized | PASS (query expansion uses names, not values) |

---

## 3. Scope Delivered

- Assurance Case domain (`DEGRADING_RADIO_QUALITY`) with status/severity/confidence
- Operational evidence persistence (`THRESHOLD`, `TREND`, `CORRELATED_KPI`)
- Flyway V4 (`assurance_case`, `assurance_evidence`, partial unique active index)
- Deterministic `DegradingRadioQualityDetector` after successful projection
- Configurable BLER thresholds; deterministic severity and confidence
- Active-case upsert (no per-event case explosion)
- Read-only assurance and assessment APIs
- Decision Intelligence: deterministic contributor/check/missing lists + LLM/stub summary
- Prompt sections: ASSURANCE CASE, OPERATIONAL EVIDENCE; question at the end
- Retrieval query expansion: technology, band, case type, INCREASING/DECREASING metric names
- Observability logs for detection create/update and assessment latency
- Tests (detector, detection/healthy control, APIs, prompt)
- ADRs 014–019, README, CONTEXT/STATUS, cursor rule

Accepted implementation nuances (not treated as architectural drift unless review rejects them):

- Automatic resolution **deferred**; cases remain `OPEN`
- Matching detections **replace** the evidence snapshot rather than appending one row per Kafka event
- Duplicate telemetry projections **do not** re-evaluate detection (same Phase 2 duplicate ack)
- Consumer-owned Phase 2 `ingestedAt` unchanged
- Latest KPI still by `eventTime`; simulator rows preferred for trends (Phase 2 ACCEPT)

---

## 4. Assurance Domain Model

First-class condition object, distinct from raw telemetry and from a future incident ticket.

| Field | Phase 3 value |
|-------|----------------|
| `caseType` | `DEGRADING_RADIO_QUALITY` only |
| `affectedEntityType` | `CELL` |
| `status` | `OPEN` / `ACKNOWLEDGED` / `RESOLVED` (create always `OPEN`) |
| `severity` | `INFO` / `WARNING` / `MAJOR` / `CRITICAL` |
| `confidence` | `LOW` / `MEDIUM` / `HIGH` (evidence completeness, not a model probability) |
| `ruleId` | `RULE_DEGRADING_RADIO_QUALITY_BLER_DL_V1` |
| `synthetic` | true when provenance or triggering observation is synthetic |

Java types: `AssuranceCaseEntity`, `AssuranceEvidenceEntity`, `AssuranceCaseView`, enums `CaseType`, `CaseStatus`, `Severity`, `Confidence`. No ITSM / incident platform.

---

## 5. Condition Detection

`DegradingRadioQualityDetector.evaluate` is a pure function over `CellContext` last-N series. It is **not** LLM logic and is **not** inside the Kafka listener.

Trigger: `TelemetryProjectionService.project()` calls `AssuranceDetectionService.evaluateCell(cellId)` after a successful persist. Kafka remains a transport.

Canonical rule:

```text
BLER_DL >= snip.assurance-bler-dl-threshold (default 0.08)
AND BLER_DL trend == INCREASING
```

No case when BLER is below warning, trend is `DECREASING` / `STABLE` / `INSUFFICIENT_DATA`, or the BLER series is missing. Seed CELL-001 (no INCREASING window) produces no case (`AssuranceDetectionTest.seedCell001DoesNotCreateCaseWithoutIncreasingTrend`).

---

## 6. Thresholds

Configurable in `application.yml` / `SnipProperties` (ratios, not percents):

| Property | Default |
|----------|---------|
| `snip.assurance-bler-dl-threshold` | `0.08` (warning) |
| `snip.assurance-bler-dl-major-threshold` | `0.10` |
| `snip.assurance-bler-dl-critical-threshold` | `0.12` |

These are the only Phase 3 detector knobs. No adaptive thresholds.

---

## 7. Severity / Confidence

Deterministic mapping (ADR 016), copied onto the assessment DTO from the persisted case:

| Condition | Severity | Confidence |
|-----------|----------|------------|
| BLER ≥ 0.08 INCREASING, no PRB series | WARNING | LOW |
| BLER ≥ 0.10 INCREASING, PRB present but not INCREASING | MAJOR | MEDIUM |
| BLER ≥ 0.12 INCREASING **and** PRB_UTILIZATION_DL INCREASING | CRITICAL | HIGH |

`high-bler-load` (BLER 0.12 INCREASING, PRB 0.84 INCREASING) is therefore CRITICAL / HIGH. The LLM is instructed not to override these values; the API does not take them from generated text.

---

## 8. Case Persistence

Flyway `src/main/resources/db/migration/V4__assurance_case.sql`. Live E2E migrated the existing Phase 2 Postgres (v3 → v4). Maven Testcontainers applies V1–V4 on every IT.

Partial unique index `assurance_case_active_uk` on `(affected_entity_id, case_type)` WHERE `status IN ('OPEN','ACKNOWLEDGED')`. Repository load uses `@Query` + `@EntityGraph` (`loadById`) so Surefire does not have to parse a derived `findWithEvidenceById` name.

---

## 9. Evidence Model

Operational facts only. Evidence types used by the canonical detector:

| Type | Meaning |
|------|---------|
| `THRESHOLD` | BLER crossed the warning threshold with INCREASING trend |
| `TREND` | Precomputed BLER trend (not recalculated) |
| `CORRELATED_KPI` | PRB co-occurrence, labelled as **not causal proof** |

Source and `synthetic` are retained. LLM prose is not stored as evidence.

---

## 10. Duplicate / Update Semantics

Active identity: `affectedEntityId + caseType + status in {OPEN, ACKNOWLEDGED}`.

A matching detection updates `lastObservedAt`, severity, confidence, and **replaces** the evidence collection (`orphanRemoval`). `detectedAt` and `firstObservedAt` are preserved. `AssuranceDetectionTest.highBlerLoadCreatesOneCaseAndRepeatedEventsUpdateIt` asserts one row after a second `high-bler-load`-shaped projection.

Duplicate `eventId` projections ack without calling detection, so republishing Phase 2 `high-bler-load` IDs on a DB that already has those rows does **not** create a case. Live E2E therefore used a **new** event id (section 16).

Automatic close-on-recovery is deferred.

---

## 11. APIs

Read-only:

```text
GET /api/v1/assurance/cases
GET /api/v1/assurance/cases/{caseId}
GET /api/v1/cells/{cellId}/assurance
GET /api/v1/assurance/cases/{caseId}/assessment
```

Empty list for a known cell with no cases (`CELL-002`). `404` for unknown cell and unknown case id. No POST/PATCH/DELETE on cases. No apply/execute.

---

## 12. Decision Intelligence

`DecisionIntelligenceService` is an application service (ADR 018). It loads the case, resolves cell context, retrieves engineering notes, and returns `DecisionAssessmentDto`.

Structured fields from `DecisionSupportComposer` (not the LLM):

- `likelyContributors` — labelled as inference
- `recommendedChecks` — investigation priorities, including human confirmation before any change
- `missingEvidence` — e.g. SINR, RSRP, neighbour KPI series not attached
- `urgency` — `IMMEDIATE` for CRITICAL

The generator writes `summary` only. `humanReviewRequired` is always `true`. Empty retrieval uses a fixed grounded-empty summary and does not fabricate citations.

Canonical question (also the default UI text):

> Why has SNIP raised a DEGRADING_RADIO_QUALITY assurance case for CELL-001, and what should I investigate first?

---

## 13. Prompt / RAG Integration

`AssembledPrompt` sections, in order: SAFETY, ASSURANCE CASE, OPERATIONAL EVIDENCE, STRUCTURED NETWORK CONTEXT, TEMPORAL KPI HISTORY / TRENDS, legacy synthetic KPI, RETRIEVED ENGINEERING KNOWLEDGE, USER QUESTION.

Safety text tells the model that TREND / case type / severity / confidence are deterministic, to distinguish evidence from inference, and never to claim a network action.

Query expansion (Decision Intelligence) appends `technology`, `band`, `caseType`, and INCREASING/DECREASING metric **names**. Operational telemetry values and assurance state are not stored in `SimpleVectorStore`. Vector min-score remains `0.60`. The 4-arg `AssembledPrompt` constructor remains for Phase 2 tests.

---

## 14. Tests

| Suite | Result |
|-------|--------|
| `mvn -B test` (full, including `TelemetryKafkaTest`) | **60 tests, 0 failures** (BUILD SUCCESS, 24.885 s, 2026-08-24T14:53:55+02:00) |
| `go test ./...` in `simulator/` | PASS (event + scenario; `cmd/simulator` has no unit tests) |
| Phase 1A/1B/2 regressions | PASS |

Phase 3 additions (14 tests):

- `DegradingRadioQualityDetectorTest` (6) — CRITICAL/HIGH, MAJOR/MEDIUM, WARNING/LOW, no-case controls
- `AssuranceDetectionTest` (3) — create+update identity, seed CELL-001 no case, `healthy-stable` CELL-002 no case
- `AssuranceApiTest` (4) — empty list, 404s, case+assessment stub path (`humanReviewRequired=true`, citations present)
- `AssembledPromptTest` +1 — assurance sections rendered

`AssuranceDetectionTest` is named `*Test` not `*IT` because default Surefire skips `*IT.java` (same lesson as Phase 2 Kafka).

---

## 15. PostgreSQL Results

V4 applied on Testcontainers in Maven and on the live Compose database used for E2E (`127.0.0.1:55432`). Tables `assurance_case` and `assurance_evidence` exist. Live E2E stored one OPEN case for CELL-001 (`d6c6bbcd-c0cc-4ce9-b0fd-a0a3b31c86e2`) with three evidence rows. CELL-002 had none.

---

## 16. Canonical Assurance Scenario

Phase 2 `high-bler-load` history was already on the E2E database from the Phase 2 run (BLER 0.04→0.12, PRB 0.60→0.84, INCREASING). Republishing those event IDs would have been duplicates and would **not** have invoked detection.

Verification of record therefore published a **new** keyed event on `snip.telemetry.cell-kpi.v1`:

| Item | Value |
|------|--------|
| Event id | `p3-e2e-bler-trigger-01` |
| Key / cell | `CELL-001` |
| Metric / value | `BLER_DL` / `0.12` |
| Event time | `2026-08-24T10:20:00Z` |
| JVM | `127.0.0.1:18087`, current working tree, `local-ai` + `snip.kafka-enabled=true` |
| Postgres / Kafka | Compose `55432` / `19092` |

Consumer log:

```text
telemetryEventsProjected=1 eventId=p3-e2e-bler-trigger-01 ... telemetryProjectionLatencyMs=296
assuranceCasesDetected=1 cellId=CELL-001 caseId=d6c6bbcd-c0cc-4ce9-b0fd-a0a3b31c86e2
  caseType=DEGRADING_RADIO_QUALITY severity=CRITICAL confidence=HIGH status=OPEN
  ruleId=RULE_DEGRADING_RADIO_QUALITY_BLER_DL_V1 synthetic=true
  assuranceDetectionLatencyMs=73
```

`GET /api/v1/cells/CELL-001/assurance` returned that single case with THRESHOLD, TREND, and CORRELATED_KPI evidence (PRB 0.84 INCREASING). Maven ITs independently create the same case from a full 4-step high-BLER series without relying on leftover E2E rows.

---

## 17. Healthy Control Scenario

`healthy-stable` (CELL-002, BLER 0.008 / PRB 0.41) must not create `DEGRADING_RADIO_QUALITY`.

- Maven: `AssuranceDetectionTest.healthyStableDoesNotCreateDegradingCase` projects four BLER+PRB points; assertion is empty.
- API: `AssuranceApiTest.emptyAssuranceForHealthyLookupIsEmptyList`.
- Live E2E: `GET /api/v1/cells/CELL-002/assurance` → HTTP 200 body `[]`.

No false case on CELL-002.

---

## 18. Local-AI Assessment Run

| Item | Value |
|------|--------|
| JVM | `127.0.0.1:18087` (same process that consumed Kafka) |
| Case | `d6c6bbcd-c0cc-4ce9-b0fd-a0a3b31c86e2` |
| Request | `GET /api/v1/assurance/cases/{caseId}/assessment` (canonical question inside the service) |
| HTTP | 200 |
| `retrievalMode` | `vector` |
| `retrievalEmpty` | `false` |
| Hits | 3 |
| Citations | `sample-bler-midband` (~0.680), `sample-mid-band-context` (~0.669), `sample-interference` (~0.663) |
| Generation | Ollama `qwen2.5:7b` |
| Latencies | retrieval 116 ms, generation 18055 ms, total 18230 ms (HTTP filter 18241 ms) |
| `severity` / `confidence` | CRITICAL / HIGH (from the case, not rewritten) |
| `urgency` | IMMEDIATE |
| `humanReviewRequired` | `true` |

The model used the precomputed CRITICAL/HIGH case and INCREASING BLER/PRB evidence, recommended investigation rather than confirmed RCA, and did not claim a network action. Citations were real corpus chunks; none were fabricated.

The E2E JVM was stopped after this GET (`127.0.0.1:18087`).

---

## 19. Observability

Application logs (no extra platform):

- `assuranceCasesDetected`, `assuranceEvaluationsNoMatch`, `assuranceDetectionLatencyMs`
- `assuranceCaseSeverity`, `ruleId`, `synthetic`
- create vs update via `AssuranceMetrics` counters
- `decisionAssessment` with `retrievalEmpty`, hits, `retrievalMode`, `humanReviewRequired`, severity/confidence, retrieval/generation/total latency

Phase 2 telemetry counters remain.

---

## 20. Safety Review

Read-only APIs only. Detection and severity are deterministic. Assessments always set `humanReviewRequired=true`. Stub and LLM instructions forbid claiming a network action or a definitive autonomous root cause. Contributor strings are labelled inference. Operational evidence is separate from citations. No MCP, no agents, no OSS/NMS writes, no ITSM.

---

## 21. ADRs

| ADR | Topic |
|-----|--------|
| 014 | Assurance Case domain model |
| 015 | Deterministic condition detection |
| 016 | Severity / confidence model |
| 017 | Active assurance-case update semantics |
| 018 | Decision Intelligence boundary |
| 019 | Evidence versus inference separation |

Phase 2 ADRs 008–013 remain in force.

---

## 22. Performance

| Path | Observation |
|------|-------------|
| Telemetry projection (E2E trigger) | 296 ms |
| Detection (E2E) | 73 ms |
| Detection (Maven, after warm context) | ~9 ms |
| Vector retrieval (E2E assessment) | 116 ms |
| Local LLM generation | ~18 s (`qwen2.5:7b`) |
| Assessment total (E2E) | 18230 ms |
| Full Maven suite | 24.885 s (Kafka IT ~8.3 s of that) |

---

## 23. Acceptance PASS/FAIL

### Baseline
- [x] Phase 2 regression suite remains passing
- [x] Kafka telemetry path remains functional
- [x] Semantic RAG remains functional
- [x] Local-AI path remains available
- [x] No network writes exist

### Assurance Domain
- [x] AssuranceCase implemented
- [x] AssuranceEvidence implemented
- [x] PostgreSQL persistence implemented
- [x] Flyway migrations pass
- [x] Status/type/severity/confidence modeled

### Detection
- [x] DEGRADING_RADIO_QUALITY detector implemented
- [x] Thresholds configurable
- [x] Severity deterministic
- [x] Confidence deterministic
- [x] Healthy scenario produces no false case

### Case Management
- [x] Repeated matching detection updates active case
- [x] Evidence remains traceable
- [x] No event-by-event duplicate case explosion

### APIs
- [x] Case list works
- [x] Case detail works
- [x] Cell assurance lookup works
- [x] Assessment endpoint works
- [x] Errors/empty results handled

### Decision Intelligence
- [x] Case + evidence + context + RAG are combined
- [x] Severity/confidence are not overridden by LLM
- [x] Canonical question runs through actual local-AI path
- [x] Assessment distinguishes evidence from inference
- [x] Investigation priorities are produced
- [x] `humanReviewRequired=true`
- [x] Citations remain valid

### Observability
- [x] Detection visible
- [x] Create/update visible
- [x] Detection/assessment latency visible

### CI / Docs
- [x] Maven tests pass
- [x] Go tests/build pass
- [x] CI does not require Ollama
- [x] README/docs updated
- [x] ADRs created

### Scope Control
- [x] No MCP
- [x] No Agents
- [x] No network writes
- [x] No anomaly-ML platform
- [x] No Phase 4 implementation

---

## 24. Known Limitations

- Automatic resolution is deferred; recovered cells keep an OPEN case until a later phase defines a recovery window.
- Evidence is a replaced snapshot, not an append-only log of every Kafka event.
- Live E2E detection used a unique trigger event because duplicate Phase 2 `high-bler-load` IDs skip detection. Maven ITs still project a full high-BLER series.
- `ACKNOWLEDGED` / `RESOLVED` exist in the schema; Phase 3 has no API to set them.
- Default Surefire does not run `*IT.java`.
- Listener `groupId` remains hardcoded `snip-npo-telemetry` (Phase 2 debt).
- Hibernate warns `firstResult/maxResults specified with collection fetch; applying in memory` on some entity-graph list queries.
- PowerShell `Measure-Object` on JSON arrays is a poor case-count tool; E2E CELL-002 emptiness was confirmed with raw HTTP body `[]`.

---

## 25. Technical Debt

- Bind Kafka listener `groupId` to `spring.kafka.consumer.group-id`.
- Consider a dedicated evidence history table if reviewers want event-level audit without snapshot replace.
- List queries that fetch evidence collections may need pagination that does not combine `LIMIT` with a collection `@EntityGraph`.
- CI lexical assessment uses the stub generator; the local-AI assessment is a workstation run, not GitHub Actions.

---

## 26. Lessons Learned

- Detection must run after a **new** projection. Idempotent telemetry replay will not re-open or refresh a case.
- Naming persistence tests `*IT` silently drops them from Surefire; keep `*Test`.
- Severity/confidence belong on the case row, then copied to the DTO — never parsed back from LLM text.
- Query expansion with case type and metric **names** is enough for the canonical question to clear `retrieve-min-score=0.60` without vectorizing telemetry.

---

## 27. Recommended Next Phase

Phase 3 is **frozen**. Do not add functionality, resolve deferred technical debt, perform unrelated refactoring, or start Phase 4 from this report. MCP, governed network action, and Agent runtime remain closed until explicitly authorised.

---

## 28. Architectural Questions — resolved

Phase 3 was architecturally accepted on 2026-08-24. The four questions are closed as follows:

1. **Automatic resolution deferred — ACCEPT.** Keep Phase 3 Assurance Cases `OPEN`. Do not introduce a recovery window in this phase.
2. **Evidence snapshot replacement — ACCEPT for Phase 3.** The active Assurance Case represents the current operational evidence snapshot. Do not introduce append-only assurance evidence history yet.
3. **Duplicate event IDs skipping assurance detection — ACCEPT.** Preserve Phase 2 telemetry idempotency semantics. A duplicate telemetry event is not new assurance evidence.
4. **`caseType` in retrieval query expansion — ACCEPT.** Deterministically established case types and semantic descriptors may enrich retrieval. Do not inject inferred diagnoses, root causes, generated conclusions, or telemetry values into the vector store or retrieval query.

---

PHASE 3 STATUS: ARCHITECTURALLY ACCEPTED
