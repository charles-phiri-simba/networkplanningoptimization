# SNIP Phase 3 — Assurance & Decision Intelligence Implementation Specification

## 1. Authority and Baseline

This document is the authorised implementation specification for Phase 3.

Start from:

```text
Branch: main
Commit: 8c70537bec048f2bf7e55c0ca626c8deec7b8670
Phase 2: COMPLETE
CI: PASS
Java tests: 46 tests, 0 failures
Go simulator: PASS
```

Read the Phase 3 architecture document completely before changing code.

Do not start Phase 4.

## 2. Objective

Implement the minimum Assurance & Decision Intelligence foundation that proves:

```text
Telemetry
   |
   v
Temporal Context
   |
   v
Deterministic Condition Detection
   |
   v
Persisted Assurance Case
   |
   v
Evidence + Severity + Confidence
   |
   +------ Semantic Knowledge
   |
   v
Actual LLM
   |
   v
Engineering Decision Support
```

Phase 3 remains read-only with respect to network operations.

## 3. Preserve Existing Capabilities

Do not rewrite:

- Site/gNB/Cell domain
- PostgreSQL domain model
- Flyway foundation
- Kafka telemetry architecture
- Go simulator
- telemetry event contract
- `TelemetryProjectionService`
- `NetworkContextService`
- trend computation
- semantic RAG
- accepted query expansion
- Ollama profile
- deterministic CI path
- citations/provenance
- existing APIs unless compatibly extended
- existing tests/ADRs

## 4. Assurance Domain Model

Implement:

```text
AssuranceCase
AssuranceEvidence
```

Minimum `AssuranceCase` fields:

```text
id
caseType
affectedEntityType
affectedEntityId
severity
confidence
status
detectedAt
firstObservedAt
lastObservedAt
ruleId
synthetic
```

Minimum values:

```text
caseType: DEGRADING_RADIO_QUALITY

status:
OPEN
ACKNOWLEDGED
RESOLVED

severity:
INFO
WARNING
MAJOR
CRITICAL

confidence:
LOW
MEDIUM
HIGH
```

## 5. Evidence Model

Minimum `AssuranceEvidence` fields:

```text
id
assuranceCase reference
evidenceType
metric
value
unit
trend
observedAt
source
synthetic
description
```

Operational evidence must originate from deterministic facts.

Do not persist LLM-generated prose as operational evidence.

## 6. Persistence

Use PostgreSQL.

Add versioned Flyway migrations for assurance tables, indexes and integrity constraints.

Do not add another database.

## 7. Condition Detection Service

Create a clear deterministic boundary, conceptually:

```text
AssuranceDetectionService
```

It receives reasoning-ready temporal context, not raw Kafka records.

Canonical rule:

```text
IF
  BLER_DL >= configured threshold
AND
  BLER_DL trend = INCREASING
THEN
  detect DEGRADING_RADIO_QUALITY
```

Additional evidence such as PRB utilisation may affect severity/confidence.

The LLM does not decide whether the rule matched.

## 8. Threshold Configuration

Externalise assurance thresholds.

Document defaults.

Do not hide thresholds in controllers, prompts or generated text.

## 9. Severity Mapping

Implement deterministic severity mapping.

Use:

```text
INFO
WARNING
MAJOR
CRITICAL
```

Document the initial rule.

The LLM must not override severity.

## 10. Confidence Mapping

Implement deterministic evidence-completeness categories:

```text
LOW
MEDIUM
HIGH
```

Do not emit pseudo-precise confidence percentages.

## 11. Case Create / Update Semantics

Do not create a new Assurance Case for each telemetry event.

For an existing active case matching:

```text
affectedEntityId + caseType + active status
```

update:

- `lastObservedAt`
- new evidence
- severity/confidence if deterministic rules justify changes

Preserve first detection time.

Test repeated detection explicitly.

## 12. Resolution

Keep resolution conservative.

Do not resolve after one healthy observation.

If a deterministic recovery window is straightforward, implement and test it. Otherwise keep automatic resolution deferred and document it.

Do not build incident-management workflow.

## 13. Triggering Assurance Evaluation

Preferred flow:

```text
TelemetryProjectionService
   |
   v
Updated Temporal Context
   |
   v
AssuranceDetectionService
   |
   v
AssuranceCaseService
```

Do not couple business assurance logic directly to Kafka APIs if avoidable.

## 14. AssuranceCaseService

Create a clear persistence/application boundary for:

- finding active equivalent case
- creating case
- updating case
- appending evidence
- loading case details
- querying cases by Cell

Keep controllers thin.

## 15. Decision Intelligence Service

Create a boundary conceptually:

```text
DecisionIntelligenceService
```

Inputs:

```text
AssuranceCase
AssuranceEvidence
Network Context
Temporal Context
Retrieved Knowledge
User Question
```

Output:

```text
DecisionAssessment
 |
 +-- assuranceCaseId
 +-- summary
 +-- likelyContributors[]
 +-- recommendedChecks[]
 +-- missingEvidence[]
 +-- urgency
 +-- citations[]
 +-- humanReviewRequired
```

This is advisory only.

## 16. Prompt Structure

The real LLM path should clearly separate:

```text
SAFETY / BEHAVIOURAL INSTRUCTIONS
ASSURANCE CASE
OPERATIONAL EVIDENCE
STRUCTURED NETWORK CONTEXT
TEMPORAL KPI HISTORY / TRENDS
RETRIEVED ENGINEERING KNOWLEDGE
USER QUESTION
```

Model instructions must state:

- deterministic case type/severity/confidence are authoritative;
- do not recalculate thresholds;
- distinguish evidence from inference;
- do not claim confirmed root cause without support;
- do not claim network actions were performed;
- require human review.

## 17. Knowledge Retrieval

Preserve Phase 2 context-aware retrieval enrichment.

It may use:

- technology
- band
- relevant metric names
- case type
- evidence descriptors

It must not inject a diagnosis or generated conclusion into the retrieval query.

Do not vectorize telemetry histories or Assurance Case state.

## 18. Canonical Scenario

Use Phase 2:

```text
high-bler-load
```

Expected:

```text
CELL-001
 |
 +-- BLER_DL increasing to 12%
 +-- PRB_UTILIZATION_DL increasing to 84%
        |
        v
DEGRADING_RADIO_QUALITY
        |
        v
Assurance Case
```

The case must contain traceable operational evidence.

## 19. Canonical Question

Mandatory:

> **Why has SNIP raised a DEGRADING_RADIO_QUALITY assurance case for CELL-001, and what should I investigate first?**

Run through:

```text
Go Simulator
 -> Kafka
 -> Telemetry Projection
 -> Temporal Context
 -> Assurance Detection
 -> Persisted Assurance Case
 -> Semantic RAG
 -> Actual Ollama LLM
 -> Decision Assessment
```

## 20. Read-Only APIs

Add conceptually equivalent endpoints:

```text
GET /api/v1/assurance/cases
GET /api/v1/assurance/cases/{caseId}
GET /api/v1/cells/{cellId}/assurance
GET /api/v1/assurance/cases/{caseId}/assessment
```

Do not add generic case-management write APIs unless strictly required for internal test/demo lifecycle and explicitly documented.

## 21. API Response Rules

Assurance responses should expose:

- case ID
- affected entity
- type
- severity
- confidence
- status
- evidence
- timestamps
- provenance

Assessment responses must distinguish:

```text
operationalEvidence
citations
likelyContributors
recommendedChecks
missingEvidence
humanReviewRequired
```

## 22. Evidence vs Inference

Decision output must preserve the difference between:

```text
Evidence:
BLER increased 4% -> 12%

Inference:
Congestion may be contributing

Confirmed Root Cause:
Not established
```

Add stable tests for this separation where practical.

## 23. Neighbour Context

Where straightforward, include relevant neighbour context in the Decision Intelligence input.

Do not implement advanced multi-cell causal correlation.

## 24. Observability

Add useful logs/counters such as:

```text
assuranceCasesDetected
assuranceCasesCreated
assuranceCasesUpdated
assuranceEvaluationsNoMatch
assuranceCaseSeverity
assuranceDetectionLatencyMs
decisionAssessmentLatencyMs
```

Preserve correlation IDs.

Do not add a new observability platform.

## 25. Unit Tests — Detection

At minimum test:

- threshold crossed + INCREASING -> case detected
- threshold not crossed -> no case
- insufficient trend -> no false detection
- corroborating PRB evidence affects severity/confidence as defined

## 26. Unit Tests — Severity and Confidence

Exercise each initial severity/confidence branch.

Verify LLM-facing code does not overwrite deterministic values.

## 27. Persistence Tests

Use PostgreSQL/Testcontainers.

Verify:

- Flyway applies
- case persists
- evidence persists
- active equivalent case updates instead of duplicates
- integrity constraints work
- queries by Cell work

## 28. API Tests

Test:

```text
GET /api/v1/assurance/cases
GET /api/v1/assurance/cases/{caseId}
GET /api/v1/cells/CELL-001/assurance
GET /api/v1/assurance/cases/{caseId}/assessment
```

Cover:

- success
- unknown case
- unknown Cell
- empty assurance result

## 29. Decision Intelligence Tests

Deterministic/stub tests must verify:

- Assurance Case appears in reasoning context
- evidence appears
- severity/confidence preserved
- citations trace to knowledge chunks
- `humanReviewRequired=true`
- no network-action semantics

## 30. Healthy Control Scenario

Run `healthy-stable`.

Expected:

```text
No DEGRADING_RADIO_QUALITY case
```

Do not create a case merely because the LLM can discuss network quality.

## 31. Real Local-AI End-to-End Validation

Execute:

```text
Go high-bler-load
 -> Kafka
 -> Java consumer
 -> PostgreSQL telemetry
 -> Temporal Context
 -> Assurance Detection
 -> PostgreSQL Assurance Case
 -> Vector Retrieval
 -> Ollama qwen2.5:7b
 -> Decision Assessment
```

Record:

- scenario
- assurance case ID
- case type
- severity
- confidence
- operational evidence
- retrieved chunks
- citations
- generated assessment
- detection latency
- retrieval latency
- generation latency
- total latency
- confirmation that no action occurred

## 32. No Incident Platform

Do not add:

- ServiceNow
- ticketing
- ITSM workflows
- incident escalation engine

The persisted Assurance Case is sufficient for Phase 3.

## 33. No Advanced Anomaly Detection

Do NOT add:

- ML anomaly models
- adaptive thresholds
- seasonal baselines
- forecasting
- Python anomaly services
- reinforcement learning

Phase 3 remains deterministic.

## 34. No MCP

Do not introduce:

```text
MCP client
MCP server
MCP gateway
MCP registry
tool execution
enterprise action
network action
```

## 35. No Autonomous Agents

Do not introduce an Agent runtime.

Decision Intelligence remains an application capability.

## 36. CI

CI must remain deterministic and must not require Ollama.

Run:

- existing Maven regression suite
- PostgreSQL/Testcontainers
- Kafka integration tests
- Phase 3 assurance tests
- Go tests/build as already established

Do not weaken prior tests.

## 37. Docker Compose

Reuse existing local PostgreSQL/Kafka/Ollama setup.

Do not introduce Kubernetes.

## 38. ADRs

Create concise ADRs for:

1. Assurance Case domain model
2. Deterministic condition detection
3. Severity/confidence model
4. Assurance-case active update semantics
5. Decision Intelligence boundary
6. Evidence vs inference separation

## 39. Documentation

Update implementation documentation and README with:

- Assurance Case concept
- canonical detector
- thresholds
- severity/confidence
- assurance APIs
- Decision Intelligence
- canonical scenario
- local validation instructions

Keep README concise.

## 40. Explicitly Out of Scope

Do NOT implement:

```text
MCP
Governed network action
Autonomous Agents
Agent Factory
Live OSS/NMS/EMS
Vendor adapters
Network writes
Incident-management integration
ML anomaly detection
Adaptive thresholds
Forecasting
Reinforcement Learning
Full Digital Twin platform
Production Kubernetes/EKS
Phase 4 functionality
```

## 41. Acceptance Criteria

### Baseline
- [ ] Phase 2 regression suite remains passing.
- [ ] Kafka telemetry path remains functional.
- [ ] Semantic RAG remains functional.
- [ ] Local-AI path remains available.
- [ ] No network writes exist.

### Assurance Domain
- [ ] AssuranceCase implemented.
- [ ] AssuranceEvidence implemented.
- [ ] PostgreSQL persistence implemented.
- [ ] Flyway migrations pass.
- [ ] Status/type/severity/confidence modeled.

### Detection
- [ ] DEGRADING_RADIO_QUALITY detector implemented.
- [ ] Thresholds configurable.
- [ ] Severity deterministic.
- [ ] Confidence deterministic.
- [ ] Healthy scenario produces no false case.

### Case Management
- [ ] Repeated matching detection updates active case.
- [ ] Evidence remains traceable.
- [ ] No event-by-event duplicate case explosion.

### APIs
- [ ] Case list works.
- [ ] Case detail works.
- [ ] Cell assurance lookup works.
- [ ] Assessment endpoint works.
- [ ] Errors/empty results handled.

### Decision Intelligence
- [ ] Case + evidence + context + RAG are combined.
- [ ] Severity/confidence are not overridden by LLM.
- [ ] Canonical question runs through actual local-AI path.
- [ ] Assessment distinguishes evidence from inference.
- [ ] Investigation priorities are produced.
- [ ] `humanReviewRequired=true`.
- [ ] Citations remain valid.

### Observability
- [ ] Detection visible.
- [ ] Create/update visible.
- [ ] Detection/assessment latency visible.

### CI / Docs
- [ ] Maven tests pass.
- [ ] Go tests/build pass.
- [ ] CI does not require Ollama.
- [ ] README/docs updated.
- [ ] ADRs created.

### Scope Control
- [ ] No MCP.
- [ ] No Agents.
- [ ] No network writes.
- [ ] No anomaly-ML platform.
- [ ] No Phase 4 implementation.

## 42. Required Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-3-COMPLETION-REPORT.md
```

Include:

1. Executive Summary
2. Baseline Verification
3. Scope Delivered
4. Assurance Domain Model
5. Condition Detection
6. Thresholds
7. Severity / Confidence
8. Case Persistence
9. Evidence Model
10. Duplicate / Update Semantics
11. APIs
12. Decision Intelligence
13. Prompt / RAG Integration
14. Tests
15. PostgreSQL Results
16. Canonical Assurance Scenario
17. Healthy Control Scenario
18. Local-AI Assessment Run
19. Observability
20. Safety Review
21. ADRs
22. Performance
23. Acceptance PASS/FAIL
24. Known Limitations
25. Technical Debt
26. Lessons Learned
27. Recommended Next Phase
28. Architectural Questions

End with exactly one:

```text
PHASE 3 STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 3 STATUS: ACCEPTANCE NOT RECOMMENDED
```

## 43. Final Instruction to Cursor

Treat this document as the authorised scope for **Phase 3 only**.

The architectural objective is:

> **Convert temporal network degradation into a persisted, evidence-backed Assurance Case and use Decision Intelligence to explain and prioritise investigation without taking action.**

Do not broaden the phase.

Do not start Phase 4.

When all acceptance criteria have been evaluated, STOP and produce the completion report for architectural review.
