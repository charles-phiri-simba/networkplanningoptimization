# SNIP Phase 6 — Digital Twin & Simulation Intelligence Foundation Architecture

## 1. Purpose

Phase 6 introduces the first explicit Digital Twin and trustworthy simulation foundation for SNIP.

Starting accepted baseline:

```text
Phase 1A / 1A.1  Knowledge Intelligence + Semantic RAG
Phase 1B         Structured Network Context Intelligence
Phase 2          Event, Telemetry & Temporal Intelligence
Phase 3          Assurance & Decision Intelligence
Phase 4          Governed Action Intelligence + MCP
Phase 5          Agentic Orchestration & Controlled Autonomy
```

Phase 6 answers:

> **How can SNIP maintain a trustworthy synthetic representation of network state and evaluate hypothetical changes against that representation before any governed action is proposed or approved?**

Phase 6 improves simulation trust. It does not expand autonomous execution authority.

---

## 2. Core Principles

1. **A simulation is evidence, not truth.**
2. **The LLM is not the simulation engine.**
3. **The Digital Twin is not the operational database.**
4. **Every simulation is reproducible from a specific Twin version, scenario and model version.**
5. **Phase 4 remains authoritative for policy, approval and MCP execution.**
6. **Phase 5 Agents gain no new execution authority.**

```text
Reality / Source State
        !=
SNIP Operational State
        !=
Digital Twin State
        !=
Simulation Scenario
```

---

## 3. Target Architecture

```text
SNIP Operational Network State
            |
            v
 TwinSynchronizationService
            |
            v
 Versioned NetworkTwin State
            |
     +------+------+
     |             |
     v             v
 Baseline     SimulationScenario
                    |
                    v
              ScenarioChange
               txPower only
                    |
                    v
       Deterministic Simulation Model
                    |
                    v
          Immutable SimulationRun
                    |
                    v
          TwinSimulationResult
                    |
          +---------+---------+
          |                   |
          v                   v
      Confidence          Limitations
          |                   |
          +---------+---------+
                    |
                    v
          Decision Intelligence
```

Governed action-triggered execution remains:

```text
Agent / Human
    ↓
ProposedAction
    ↓
Phase 4 Policy
    ↓
Human Approval
    ↓
MCP
    ↓
Digital Twin Simulation
```

---

## 4. Twin Scope

Phase 6 is cell-centric.

The Twin may contain:

- Cell identity;
- serving gNB;
- Site;
- radio configuration;
- current KPI state;
- bounded KPI history/trend summary;
- neighbour summaries;
- relevant Assurance context.

Do not create a whole-RAN, core-network, transport or BSS Twin.

---

## 5. NetworkTwin

Introduce an explicit `NetworkTwin`.

Conceptually:

```text
NetworkTwin
 |
 +-- twinId
 +-- name
 +-- scopeType
 +-- scopeId
 +-- status
 +-- latestVersion
 +-- createdAt
 +-- synchronizedAt
 +-- synthetic
```

Phase 6 supports `scopeType = CELL`.

---

## 6. NetworkTwinVersion

Simulation runs against a fixed immutable snapshot.

Conceptually:

```text
NetworkTwinVersion
 |
 +-- twinId
 +-- version
 +-- capturedAt
 +-- synchronizedAt
 +-- sourceEventTime
 +-- sourceContextVersion
 +-- provenance
 +-- cellState
 +-- configuration
 +-- currentMetrics
 +-- temporalSummary
 +-- neighbourSummary
```

Each successful synchronization creates the next monotonically increasing version.

A simulation records `baselineTwinVersion`.

---

## 7. Twin Synchronization

Introduce `TwinSynchronizationService`.

Responsibilities:

- read authoritative SNIP operational state;
- validate required simulation inputs;
- build a simulation-safe projection;
- capture source timestamps and provenance;
- assign a new Twin version;
- persist the immutable snapshot;
- update Twin synchronization metadata.

Synchronization is deterministic and contains no LLM decision.

### Locked synchronization strategy

Phase 6 uses **manual/on-demand synchronization only**.

```text
Operational State
      ↓
Explicit Synchronize
      ↓
Twin Version N
```

Do not automatically synchronize on every Phase 2 telemetry/Kafka event.

---

## 8. Twin Freshness and Drift

Implement:

```text
CURRENT
STALE
EXPIRED
```

Freshness is determined deterministically from source state, relevant timestamps/versions and configurable thresholds.

Twin drift in Phase 6 asks only:

> Has relevant operational state advanced beyond the Twin baseline?

### Locked policy

```text
CURRENT -> simulation allowed
STALE   -> simulation blocked; resynchronization required
EXPIRED -> simulation blocked; resynchronization required
```

Do not silently simulate on stale state.

---

## 9. Twin Provenance

Every Twin version must identify its source.

Minimum conceptual provenance:

```text
source = SNIP_OPERATIONAL_STATE
sourceCellId
sourceContextVersion
sourceTelemetryTimestamp
capturedAt
synthetic
```

Do not imply vendor/live-network provenance that does not exist.

---

## 10. SimulationScenario

Introduce a persisted `SimulationScenario`.

```text
SimulationScenario
 |
 +-- scenarioId
 +-- twinId
 +-- baselineTwinVersion
 +-- name
 +-- description
 +-- status
 +-- createdAt
 +-- createdBy
 +-- synthetic
```

A Scenario expresses a hypothetical change against one immutable baseline.

---

## 11. ScenarioChange

```text
ScenarioChange
 |
 +-- parameterId
 +-- currentValue
 +-- proposedValue
 +-- unit
```

The current value must match the selected Twin baseline.

---

## 12. Simulatable Parameter Registry

Use an in-code whitelist:

```text
SimulatableParameterDefinition
 |
 +-- parameterId
 +-- unit
 +-- minValue
 +-- maxValue
 +-- scope
 +-- enabled
```

### Locked Phase 6 parameter

```text
txPower
```

Only `txPower` is simulatable in Phase 6.

Reject unknown, disabled, wrong-scope and out-of-range parameters.

`electricalTilt` is deferred.

---

## 13. Baseline vs Candidate State

Every simulation explicitly compares:

```text
Baseline
  txPower = current Twin value

Candidate
  txPower = proposed scenario value
```

Results expose baseline, candidate prediction and delta.

---

## 14. Deterministic Simulation Engine

Introduce a boundary such as:

```text
DigitalTwinSimulationService
        |
        v
CellParameterSimulationModel
```

The initial model is a documented deterministic rule/formula-based synthetic model.

It may use explicitly available inputs such as:

```text
txPower delta
current BLER
current PRB load
current throughput
bounded trend context
```

Identical input must produce identical output.

The model must be clearly labelled:

> **Synthetic engineering model — not vendor-calibrated RF physics.**

---

## 15. No LLM Simulation

Prohibited:

```text
Scenario -> LLM -> authoritative numeric prediction
```

Required:

```text
Scenario
   ↓
Deterministic Model
   ↓
Structured Numeric Result
   ↓
Optional LLM Explanation
```

An LLM explanation cannot modify the numeric result.

---

## 16. Model Metadata

Every simulation records:

```text
modelId
modelVersion
modelType
assumptions
```

Initial identity:

```text
modelId   = snip.synthetic.cell-parameter.v1
modelType = RULE_BASED
```

Use an explicit model version.

---

## 17. Confidence

Use:

```text
LOW
MEDIUM
HIGH
```

Do not create pseudo-precise confidence percentages.

The initial synthetic model should conservatively report `LOW` unless an explicit deterministic rule justifies otherwise.

---

## 18. Structured Limitations

Every successful result contains structured limitations.

Initial examples:

```text
NO_RF_PROPAGATION_MODEL
NO_VENDOR_CALIBRATION
NO_MOBILITY_MODEL
NO_TRAFFIC_FORECAST
SYNTHETIC_KPI_MODEL
```

Limitations are part of the result contract.

---

## 19. SimulationRun

Every execution creates a new immutable run.

```text
SimulationRun
 |
 +-- simulationId
 +-- scenarioId
 +-- twinId
 +-- baselineTwinVersion
 +-- modelId
 +-- modelVersion
 +-- status
 +-- startedAt
 +-- completedAt
 +-- synthetic
```

Re-execution creates a new `simulationId`.

Completed runs are not overwritten.

---

## 20. TwinSimulationResult

```text
TwinSimulationResult
 |
 +-- simulationId
 +-- baselineMetrics
 +-- predictedMetrics
 +-- deltas
 +-- confidence
 +-- limitations[]
 +-- assumptions[]
 +-- provenance
```

Every result is traceable to the exact Twin and model version.

---

## 21. Metric Comparison

Expose:

```text
MetricComparison
 |
 +-- metric
 +-- baselineValue
 +-- candidateValue
 +-- delta
 +-- unit
```

Preserve existing SNIP ratio semantics internally.

---

## 22. Simulation History

Simulation execution history is immutable.

Example:

```text
Run A -> Twin v42 -> txPower 40 -> 38
Run B -> Twin v42 -> txPower 40 -> 36
Run C -> Twin v43 -> txPower 40 -> 38
```

This provides reproducible evidence history.

---

## 23. Scenario Comparison

Phase 6 supports comparison of completed candidate simulations sharing a meaningful scope/baseline.

Example:

```text
Scenario A: txPower 40 -> 38
Scenario B: txPower 40 -> 36
```

Comparison exposes metric trade-offs, confidence and limitations.

It does not automatically select an optimum.

---

## 24. No Optimization Engine

Phase 6 may:

```text
simulate A
simulate B
compare A vs B
```

It may not:

```text
search the parameter space
choose the best txPower
optimize the network automatically
```

---

## 25. Phase 4 MCP Integration

The existing capability:

```text
simulation.cell-parameter.v1
```

must delegate to the Phase 6 Digital Twin simulation layer.

```text
MCP tools/call
      ↓
simulation.cell-parameter.v1
      ↓
DigitalTwinSimulationService
      ↓
NetworkTwinVersion
      ↓
SimulationScenario
      ↓
CellParameterSimulationModel
      ↓
TwinSimulationResult
```

Do not create an alternative governed execution path.

---

## 26. Phase 4 Approval Boundary

The existing action remains:

```text
SIMULATE_CELL_PARAMETER_CHANGE
```

with:

```text
MEDIUM
  ↓
REQUIRE_APPROVAL
```

Phase 6 does not change Phase 4 risk/policy semantics.

---

## 27. Management APIs vs Execution

Twin-management APIs may:

- synchronize a Twin;
- inspect Twin versions;
- create scenario definitions;
- inspect scenarios;
- inspect completed simulation results;
- compare completed simulations.

Authoritative action-triggered simulation execution remains:

```text
Phase 4 -> MCP -> Phase 6
```

Do not expose an ungoverned public simulation-execution endpoint that bypasses approval.

---

## 28. Relationship to Phase 5 Agents

Existing Agents may:

- propose `SIMULATE_CELL_PARAMETER_CHANGE`;
- consume completed simulation evidence;
- compare scenarios;
- explain results.

They may not:

- mutate Twin baseline;
- approve simulation;
- execute MCP directly;
- alter deterministic numeric results.

No Digital Twin Agent is added in Phase 6.

---

## 29. Decision Intelligence Integration

Simulation becomes structured evidence.

Decision Intelligence may consume:

```text
baseline metrics
candidate metrics
deltas
confidence
limitations
assumptions
Twin version
model version
```

Narrative explanation remains subordinate to deterministic data.

---

## 30. Persistence

Continue using PostgreSQL + Flyway.

Conceptual persistence:

```text
network_twin
network_twin_version
simulation_scenario
simulation_scenario_change
simulation_run
simulation_result_metric
simulation_limitation
```

No new database technology.

---

## 31. Conceptual APIs

```text
POST /api/v1/twins/cells/{cellId}/synchronize

GET  /api/v1/twins/{twinId}
GET  /api/v1/twins/{twinId}/versions
GET  /api/v1/twins/{twinId}/versions/{version}

POST /api/v1/twins/{twinId}/scenarios
GET  /api/v1/twins/{twinId}/scenarios
GET  /api/v1/scenarios/{scenarioId}

GET  /api/v1/simulations/{simulationId}
```

A bounded comparison endpoint may compare completed simulations.

Exact shapes may follow existing API conventions without weakening governance.

---

## 32. Canonical Scenario

Use `CELL-001`.

Illustrative baseline:

```text
BLER_DL = 0.12 ratio (12%)
PRB_UTILIZATION_DL = 0.84 ratio (84%)
txPower = 40
```

Scenario:

```text
txPower: 40 -> 38
```

Flow:

```text
Synchronize CELL-001
      ↓
Twin version N
      ↓
Create Scenario
      ↓
SIMULATE_CELL_PARAMETER_CHANGE
      ↓
MEDIUM / REQUIRE_APPROVAL
      ↓
Human approval
      ↓
MCP
      ↓
snip.synthetic.cell-parameter.v1
      ↓
Immutable synthetic result
```

Exact predicted values must be deterministic, documented and tested; they must not be invented by an LLM.

---

## 33. Canonical Comparison

Against the same Twin version:

```text
A: txPower 40 -> 38
B: txPower 40 -> 36
```

Compare modeled KPI deltas and trade-offs.

No automatic "best scenario" selection.

---

## 34. Required Failure Cases

Fail safely for:

```text
unknown Cell
unknown Twin
unknown Twin version
STALE Twin
EXPIRED Twin
unsupported parameter
out-of-range txPower
baseline/current-value mismatch
missing approval
simulation model failure
invalid scenario
```

No failure may cause network mutation.

---

## 35. Observability

Expose useful logs/counters:

```text
twinSynchronizations
twinSynchronizationFailures
twinVersionsCreated
twinStaleDetections
simulationScenariosCreated
simulationRunsStarted
simulationRunsSucceeded
simulationRunsFailed
simulationLatencyMs
scenarioComparisons
```

Correlate with `cellId`, `twinId`, `twinVersion`, `scenarioId`, `simulationId`, and `actionId` where applicable.

---

## 36. Security and Safety

Phase 6 adds:

- no vendor credentials;
- no live network endpoint;
- no network write path;
- no Agent execution privilege;
- no LLM authority over numeric simulation.

Simulation does not authorize a network action.

---

## 37. Explicitly Out of Scope

Do not implement:

```text
electricalTilt simulation
arbitrary parameter simulation
full RF propagation engine
ray tracing
vendor-calibrated radio models
live network writes
autonomous optimization
automatic best-parameter search
reinforcement learning
ML training
Digital Twin Agent
Optimization Agent
whole-network Twin
core-network Twin
transport Twin
remote vendor Digital Twin
real-time continuous Twin synchronization
Kafka-triggered automatic Twin synchronization
graph database
3D GIS Twin
mobility simulation
traffic forecasting
production-grade capacity planning
Phase 7 functionality
```

---

## 38. Locked Phase 6 Decisions

- Phase: **Digital Twin & Simulation Intelligence Foundation**
- Twin scope: Cell-centric
- Twin is separate from operational DB
- Twin snapshots: immutable/versioned
- Synchronization: manual/on-demand
- Automatic telemetry synchronization: deferred
- Freshness: CURRENT / STALE / EXPIRED
- STALE/EXPIRED: resynchronization required
- Twin provenance: mandatory
- Scenario: explicit persisted object
- First simulatable parameter: `txPower` only
- Parameter registry: in-code whitelist initially
- Simulation: deterministic rule/formula model
- LLM as simulation engine: prohibited
- Model ID/version: mandatory
- Confidence: LOW / MEDIUM / HIGH
- Limitations: structured/mandatory
- Simulation runs: immutable
- Scenario comparison: included
- Automatic optimization: excluded
- Persistence: PostgreSQL + Flyway
- Governed execution: Phase 4 -> MCP
- Approval: Phase 4 authoritative
- Agent direct execution: prohibited
- Digital Twin Agent: excluded
- Continuous sync: deferred
- Full RF Twin: deferred
- Live network writes: prohibited

---

## 39. Architectural Outcome

At Phase 6 completion SNIP should transform:

```text
Authoritative Cell Context
```

into:

```text
Versioned Twin Snapshot
+
Explicit Candidate Scenario
+
Deterministic Simulation
+
Reproducible Simulation Evidence
+
Confidence / Limitations
+
Scenario Comparison
```

while preserving:

```text
Phase 4 Governance
+
Human Approval
+
MCP Boundary
+
Phase 5 Agent Least Privilege
+
No Live Network Writes
```

SNIP progression:

```text
KNOW
  ↓
UNDERSTAND
  ↓
OBSERVE CHANGE
  ↓
ASSESS
  ↓
ACT SAFELY
  ↓
COORDINATE INTELLIGENTLY
  ↓
SIMULATE BEFORE CHANGE
```
