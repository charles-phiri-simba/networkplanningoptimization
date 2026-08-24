# SNIP Phase 3 — Assurance & Decision Intelligence Architecture

## 1. Purpose

Phase 3 introduces the Assurance & Decision Intelligence foundation of the Simba Network Intelligence Platform (SNIP).

Phase 1A established Knowledge Intelligence. Phase 1B established structured Context Intelligence. Phase 2 established Event & Temporal Intelligence.

Phase 3 must answer:

> **Does the observed network condition matter, how serious is it, what evidence supports that conclusion, and what should an engineer investigate first?**

Phase 3 remains read-only. It does not execute network actions and does not introduce MCP.

## 2. Architectural Evolution

```text
Telemetry
   |
   v
Temporal Context
   |
   v
Condition Detection
   |
   v
Assurance Case
   |
   +-------------------+
   |                   |
   v                   v
Evidence            Severity
   |                   |
   +---------+---------+
             |
             v
      Decision Intelligence
             |
             +------ Knowledge Intelligence
             |
             v
          AI Reasoning
             |
             v
     Engineering Diagnosis
             |
             v
           Human
```

## 3. Core Principle

> **Telemetry tells us what changed. Assurance determines whether the change matters. Decision Intelligence evaluates the evidence. AI explains and prioritises investigation.**

```text
Telemetry != Condition != Assurance Case != Decision != Action
```

## 4. Assurance Case

An `AssuranceCase` is the core Phase 3 domain object.

Conceptually:

```text
AssuranceCase
 |
 +-- id
 +-- caseType
 +-- affectedEntityType
 +-- affectedEntityId
 +-- severity
 +-- confidence
 +-- status
 +-- detectedAt
 +-- firstObservedAt
 +-- lastObservedAt
 +-- evidence[]
 +-- ruleId / detectorId
 +-- synthetic
```

## 5. Lifecycle

Initial lifecycle:

```text
OPEN -> ACKNOWLEDGED -> RESOLVED
```

Phase 3 does not implement a full incident-management workflow.

## 6. Deterministic Condition Detection

Condition detection is deterministic in Phase 3.

Canonical condition:

```text
DEGRADING_RADIO_QUALITY
```

Example:

```text
BLER_DL current = 12%
BLER_DL trend = INCREASING
PRB_UTILIZATION_DL current = 84%
PRB_UTILIZATION_DL trend = INCREASING
Affected entity = CELL-001
```

This means the evidence warrants investigation; it does not prove root cause.

## 7. Rule-Based First

Example rule:

```text
IF
  BLER_DL >= configured threshold
AND
  BLER_DL trend = INCREASING
THEN
  detect DEGRADING_RADIO_QUALITY
```

Additional evidence may influence severity and confidence.

The LLM must not decide whether a deterministic threshold was crossed.

## 8. Rule Catalogue

Rules must be explicit, identifiable and testable.

Conceptually:

```text
RuleDefinition
 |
 +-- ruleId
 +-- name
 +-- conditionType
 +-- metric
 +-- operator
 +-- threshold
 +-- trendRequirement
 +-- severityMapping
 +-- enabled
```

A dynamic rule engine is not required.

## 9. Severity

Use:

```text
INFO
WARNING
MAJOR
CRITICAL
```

Severity is deterministic and configurable.

## 10. Confidence

Use:

```text
LOW
MEDIUM
HIGH
```

Confidence describes evidence completeness, not pseudo-precise model certainty.

## 11. Evidence

Conceptually:

```text
AssuranceEvidence
 |
 +-- evidenceType
 +-- metric
 +-- value
 +-- unit
 +-- trend
 +-- observedAt
 +-- source
 +-- synthetic
 +-- description
```

Knowledge citations remain separate from operational evidence.

## 12. Evidence vs Inference

```text
Evidence:
"BLER_DL increased from 4% to 12%"

Inference:
"Congestion may be contributing"

Confirmed root cause:
"Congestion caused the issue"
```

Phase 3 may produce evidence and inference. It must not automatically promote inference to confirmed root cause.

## 13. Multi-KPI Correlation

Phase 3 introduces simple co-occurrence correlation:

```text
BLER increasing
     +
PRB utilisation increasing
     |
     v
Correlated degradation pattern
```

This is not statistical causality.

## 14. Neighbour Context

Neighbour state may enrich assurance and indicate whether degradation appears localised or wider, but Phase 3 must not invent causal topology conclusions.

## 15. Persistence

Persist in PostgreSQL:

```text
assurance_case
assurance_evidence
```

No new database or incident platform is introduced.

## 16. Duplicate / Reuse Semantics

Repeated detection of the same active condition must not create one case per telemetry event.

Use the conceptual active identity:

```text
affectedEntityId + caseType + active status
```

An equivalent active case is updated with new evidence, timestamps and deterministic severity/confidence.

## 17. Resolution

Resolution must be conservative.

Do not close a case after one healthy sample. If a recovery window is implemented, it must be deterministic and testable.

## 18. Decision Intelligence

Decision Intelligence receives:

```text
Assurance Case
+
Operational Evidence
+
Network Context
+
Temporal History
+
Engineering Knowledge
```

and returns structured decision support:

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

It remains advisory.

## 19. Deterministic Before Generative

The LLM receives already-computed case type, severity, confidence, trends and threshold results.

The model must not override these deterministic facts.

## 20. AI Role

AI may:

- explain the assurance condition;
- connect evidence to engineering knowledge;
- identify plausible contributors;
- prioritise checks;
- identify missing evidence;
- create a human-readable diagnosis.

AI may not:

- execute changes;
- override deterministic severity/confidence;
- close assurance cases;
- create topology;
- confirm root cause without evidence.

## 21. Canonical Scenario

Use Phase 2 `high-bler-load`:

```text
Go Simulator
   |
   v
Kafka
   |
   v
Telemetry Projection
   |
   v
Temporal Context
   |
   v
Condition Detection
   |
   v
DEGRADING_RADIO_QUALITY
   |
   v
Assurance Case
   |
   v
Decision Intelligence
   |
   +------ Semantic RAG
   |
   v
Actual LLM
   |
   v
Engineering Diagnosis
```

## 22. Canonical Question

> **Why has SNIP raised a DEGRADING_RADIO_QUALITY assurance case for CELL-001, and what should I investigate first?**

The model must answer from persisted case state, explicit operational evidence, network context, temporal history and retrieved engineering knowledge.

## 23. APIs

Read-only APIs conceptually:

```text
GET /api/v1/assurance/cases
GET /api/v1/assurance/cases/{caseId}
GET /api/v1/cells/{cellId}/assurance
GET /api/v1/assurance/cases/{caseId}/assessment
```

## 24. Event Participation

Kafka remains the telemetry transport.

Assurance evaluation may be triggered after projection, but Phase 3 does not require new enterprise assurance topics.

## 25. No MCP Yet

Progression:

```text
Observe -> Understand -> Detect -> Build Evidence -> Assess -> Recommend -> Human
```

Future:

```text
Govern -> MCP -> Controlled Action
```

MCP remains deferred.

## 26. No Autonomous Agent

Decision Intelligence is an application capability, not an autonomous Agent runtime.

## 27. No Advanced Anomaly Detection

Phase 3 uses deterministic rules.

Deferred:

- statistical baselines
- seasonality
- adaptive thresholds
- ML anomaly detection
- forecasting
- learned patterns

## 28. Assurance vs Incident

An Assurance Case is not yet an enterprise Incident.

```text
Assurance Case -> may later escalate -> Incident
```

No ITSM integration in Phase 3.

## 29. Complete Architecture

```text
Telemetry
   |
   v
Kafka
   |
   v
Projection
   |
   v
Temporal Context
   |
   v
Condition Detector
   |
   v
Assurance Case
   |
   +-------------------------+
   |                         |
   v                         v
Operational Evidence    Network Context
   |                         |
   +------------+------------+
                |
                v
       Decision Intelligence
                |
        +-------+-------+
        |               |
        v               v
   Semantic RAG      Case Evidence
        |               |
        +-------+-------+
                |
                v
            Actual LLM
                |
                v
      Engineering Assessment
                |
                v
              Human
```

## 30. Locked Decisions

- Primary object: `AssuranceCase`
- Canonical case type: `DEGRADING_RADIO_QUALITY`
- Detection: deterministic rules
- Severity: INFO / WARNING / MAJOR / CRITICAL
- Confidence: LOW / MEDIUM / HIGH
- Evidence: explicit operational evidence
- Persistence: PostgreSQL
- Duplicate handling: reuse/update equivalent active case
- Resolution: conservative
- Decision Intelligence: advisory
- AI: explanation/prioritisation, not authority
- Advanced anomaly detection: deferred
- MCP: deferred
- Autonomous Agents: deferred
- Network writes: prohibited

## 31. Architectural Outcome

At Phase 3 completion, SNIP should transform:

```text
Telemetry + Trends
```

into:

```text
Detected Condition
+
Persisted Assurance Case
+
Traceable Evidence
+
Severity / Confidence
+
Context-aware Decision Assessment
+
Engineering Knowledge
+
AI Explanation
```

while preserving human control.

This becomes the foundation for later Governed Action Intelligence and MCP.
