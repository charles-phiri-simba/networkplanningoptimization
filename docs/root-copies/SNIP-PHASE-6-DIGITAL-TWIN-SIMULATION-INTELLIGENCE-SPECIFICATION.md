# SNIP Phase 6 — Digital Twin & Simulation Intelligence Foundation Implementation Specification

## 1. Authority and Baseline

This is the bounded implementation contract for Phase 6.

Start from the accepted Phase 5 baseline:

```text
Branch: main
Commit: 4e7a8feeb7a16924666e094828db829d5f4b703c
Phase 5: ARCHITECTURALLY ACCEPTED
CI: PASS
Maven: 83 tests, 0 failures
Go: tests/build PASS
Working tree: clean
```

Read the Phase 6 architecture document completely before modifying code:

```text
SNIP-PHASE-6-DIGITAL-TWIN-SIMULATION-INTELLIGENCE-ARCHITECTURE.md
```

The architecture is authoritative.

Do not start Phase 7. Do not establish a Phase 6 Git baseline until architectural acceptance is granted.

---

## 2. Objective

Implement the minimum trustworthy Digital Twin proof:

```text
Operational Cell State
      ↓
Explicit Twin Synchronization
      ↓
Immutable Versioned Twin
      ↓
Scenario: txPower change
      ↓
Phase 4 Governed Simulation Action
      ↓
MCP simulation.cell-parameter.v1
      ↓
Deterministic Simulation Model
      ↓
Immutable Result
      ↓
Confidence + Limitations + Provenance
```

---

## 3. Preserve Existing Architecture

Do not rewrite or weaken:

- RAG / Knowledge Intelligence;
- structured network context;
- telemetry/Kafka;
- temporal trends;
- Assurance;
- deterministic severity/confidence;
- Decision Intelligence;
- Phase 4 `ProposedAction`;
- Phase 4 risk/policy/approval;
- Phase 4 MCP gateway/server;
- Phase 4 capability registry;
- Phase 5 Agent model/permissions;
- Phase 5 no-Agent-to-MCP rule;
- existing CI defaults.

---

## 4. Required Domain Concepts

Implement clear types for:

```text
NetworkTwin
NetworkTwinVersion
TwinFreshness
SimulationScenario
ScenarioChange
SimulatableParameterDefinition
SimulationRun
TwinSimulationResult
MetricComparison
SimulationConfidence
SimulationLimitation
SimulationModelMetadata
```

Names may follow repository conventions while preserving these semantics.

---

## 5. NetworkTwin and Versions

Persist a Cell-scoped Twin.

`NetworkTwin` minimum:

```text
twinId
name
scopeType
scopeId
status
latestVersion
createdAt
synchronizedAt
synthetic
```

`NetworkTwinVersion` minimum:

```text
twinId
version
capturedAt
synchronizedAt
sourceEventTime
sourceContextVersion
provenance
cell identity
serving gNB/site identity
simulation-relevant configuration
current KPI state
bounded temporal/trend summary
neighbour summary
```

Versions are immutable.

Each successful synchronization creates `N+1`.

---

## 6. TwinSynchronizationService

Implement a deterministic service that:

1. resolves the Cell through existing network context;
2. loads simulation-relevant current state;
3. loads bounded telemetry/trend state;
4. captures relevant configuration/neighbours;
5. records provenance/timestamps;
6. creates a new immutable version;
7. updates Twin metadata.

No LLM.

No Kafka subscription for automatic Twin updates.

---

## 7. Freshness and Drift

Implement:

```text
CURRENT
STALE
EXPIRED
```

Document the exact deterministic freshness algorithm.

Enforce:

```text
CURRENT -> simulation allowed
STALE -> reject; resync required
EXPIRED -> reject; resync required
```

A relevant newer operational/telemetry state must make the Twin stale.

Do not silently resynchronize during governed execution.

---

## 8. Twin Provenance

Persist enough provenance to identify:

```text
source = SNIP_OPERATIONAL_STATE
Cell ID
context/version reference where available
latest relevant telemetry timestamp
capture timestamp
synthetic/projection flag
```

Do not imply vendor provenance.

---

## 9. SimulationScenario / ScenarioChange

Persist:

```text
scenarioId
twinId
baselineTwinVersion
name
description
status
createdAt
createdBy
synthetic
```

Scenario change:

```text
parameterId
currentValue
proposedValue
unit
```

The current value must match the selected Twin baseline.

---

## 10. Parameter Registry

Implement a static/in-code whitelist.

Phase 6 supports exactly:

```text
txPower
```

Metadata:

```text
parameterId
unit
minValue
maxValue
scope
enabled
```

Reject unknown, disabled, wrong-scope, invalid-type and out-of-range values.

`electricalTilt` must be rejected/not registered.

---

## 11. Deterministic Simulation Model

Implement a model boundary such as:

```text
CellParameterSimulationModel
```

The model must be:

- deterministic;
- non-LLM;
- synthetic;
- documented;
- unit tested;
- versioned.

Identical input must produce identical output.

Use only explicitly documented available inputs such as `txPower`, BLER, PRB utilization, throughput if available, and bounded trend context.

Do not invent unavailable RF features.

---

## 12. Model Identity and Formula

Use an explicit identity:

```text
modelId = snip.synthetic.cell-parameter.v1
modelVersion = 1.0
modelType = RULE_BASED
```

Document the exact synthetic formula/rules in source/tests/completion report.

Clearly state that it is not vendor-calibrated RF physics.

No LLM may produce authoritative numeric KPI predictions.

---

## 13. Confidence and Limitations

Implement:

```text
LOW
MEDIUM
HIGH
```

Do not use pseudo-precise confidence percentages.

Return structured limitations on every successful run, including appropriate codes from:

```text
NO_RF_PROPAGATION_MODEL
NO_VENDOR_CALIBRATION
NO_MOBILITY_MODEL
NO_TRAFFIC_FORECAST
SYNTHETIC_KPI_MODEL
```

---

## 14. SimulationRun / Result

Every execution creates a new immutable `SimulationRun`.

Minimum:

```text
simulationId
scenarioId
twinId
baselineTwinVersion
modelId
modelVersion
status
startedAt
completedAt
synthetic
```

Persist result data:

```text
baselineMetrics
predictedMetrics
deltas
confidence
limitations
assumptions
provenance
```

A re-run creates a new `simulationId`; do not overwrite prior completed runs.

---

## 15. Metric and Scenario Comparison

Expose metric comparisons:

```text
metric
baselineValue
candidateValue
delta
unit
```

Preserve existing ratio semantics internally.

Support deterministic comparison of two completed scenarios/runs against a meaningful shared baseline.

Do not automatically select a "best" parameter.

---

## 16. PostgreSQL / Flyway

Add the next sequential migration after Phase 5.

Persist concepts equivalent to:

```text
network_twin
network_twin_version
simulation_scenario
simulation_scenario_change
simulation_run
simulation_result_metric
simulation_limitation
```

Exact normalization may follow repository conventions.

Do not add another database.

---

## 17. Phase 4 MCP Integration

Preserve capability ID:

```text
simulation.cell-parameter.v1
```

unless a blocking technical conflict is found and reported before changing it.

Its implementation must delegate to the Phase 6 Digital Twin simulation service.

Preserve:

```text
SIMULATE_CELL_PARAMETER_CHANGE
risk = MEDIUM
policy = REQUIRE_APPROVAL
```

Do not alter Phase 4 policy semantics.

---

## 18. Governed Execution Path

Required:

```text
ProposedAction
    ↓
Phase 4 Policy
    ↓
APPROVAL_REQUIRED
    ↓
Human approve
    ↓
execute
    ↓
MCP Gateway
    ↓
simulation.cell-parameter.v1
    ↓
DigitalTwinSimulationService
```

No unapproved alternative execution route.

---

## 19. Management / Read APIs

Implement bounded APIs consistent with project conventions for:

- synchronize a Cell Twin;
- read Twin metadata;
- read Twin versions;
- create a Scenario definition;
- read Scenario;
- read completed Simulation;
- compare completed Simulations.

Suggested shapes:

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

Do not expose an ungoverned `POST /simulate` that bypasses Phase 4 approval.

---

## 20. Phase 5 Agent Boundary

Do not add a Digital Twin Agent.

Existing Agents may consume simulation evidence through bounded services and may propose `SIMULATE_CELL_PARAMETER_CHANGE`.

No Agent may directly depend on/call:

```text
McpCapabilityGateway
ActionExecutionService
/mcp
```

Agents cannot mutate Twin baseline or simulation numeric output.

---

## 21. Decision Intelligence

Expose structured simulation evidence for Decision Intelligence:

```text
baseline/candidate metrics
deltas
confidence
limitations
assumptions
twinVersion
modelVersion
```

Generated narrative cannot overwrite deterministic values.

---

## 22. Canonical Scenario

Use existing `CELL-001` fixtures where practical.

Establish a baseline with at least:

```text
txPower
BLER_DL
PRB_UTILIZATION_DL
```

and any additional metric used by the model.

Create scenario:

```text
txPower: 40 -> 38
```

or equivalent values matching the actual fixture.

Execute through Phase 4 approval + MCP.

Verify:

- MEDIUM risk;
- REQUIRE_APPROVAL;
- pre-approval execution blocked;
- approval succeeds;
- MCP invoked;
- Phase 6 simulation executes;
- result deterministic;
- exact Twin version recorded;
- model ID/version recorded;
- confidence present;
- limitations present;
- synthetic true;
- no network write.

---

## 23. Canonical Comparison

Against the same Twin version create two valid scenarios, e.g.:

```text
A: 40 -> 38
B: 40 -> 36
```

Execute through governed paths.

Verify comparison returns explicit trade-offs without automatic optimization.

---

## 24. Stale Twin Proof

Required test:

```text
synchronize Twin
    ↓
advance relevant operational/telemetry state
    ↓
Twin becomes STALE
    ↓
attempt governed simulation
    ↓
rejected before model execution
    ↓
resynchronize
    ↓
new Twin version
    ↓
simulation allowed
```

No automatic Twin synchronization.

---

## 25. Parameter Validation

Test:

```text
valid txPower
below minimum
above maximum
unknown parameter
electricalTilt rejected
baseline current-value mismatch
```

Fail deterministically.

---

## 26. Immutability / Determinism Tests

Verify:

- Twin versions are not overwritten;
- completed SimulationRuns are not overwritten;
- rerun creates a new simulation ID;
- old run remains queryable;
- each run references exact Twin/model versions;
- identical model inputs return identical predictions/deltas.

Do not test LLM wording for numeric correctness.

---

## 27. Failure Tests

Cover at least:

```text
unknown Cell
unknown Twin
unknown Twin version
STALE Twin
EXPIRED Twin where practical
unsupported parameter
out-of-range txPower
baseline mismatch
missing approval
simulation model failure
invalid scenario
```

Fail closed.

---

## 28. Regression Requirements

All Phase 1–5 tests remain passing.

Preserve proofs that:

- APPLY remains HIGH / DENY;
- Agent cannot call MCP;
- simulation requires approval;
- remediation generation works;
- default CI remains Ollama-free.

---

## 29. CI

Default:

```text
mvn -B test
```

must require no:

```text
Ollama
vendor OSS
live network
external Digital Twin
external simulation engine
```

Go simulator tests/build remain passing.

---

## 30. Local E2E

Demonstrate:

```text
CELL-001 operational context
      ↓
Twin synchronization
      ↓
Twin version
      ↓
Scenario
      ↓
Phase 4 action
      ↓
approval
      ↓
MCP
      ↓
deterministic simulation
      ↓
result
```

Record:

- twinId;
- Twin version;
- freshness;
- scenarioId;
- actionId;
- simulationId;
- model ID/version;
- input change;
- baseline metrics;
- predicted metrics;
- deltas;
- confidence;
- limitations;
- MCP evidence;
- no-network-write evidence.

Ollama is not required for Phase 6 correctness.

---

## 31. Observability

Add useful logs/counters:

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

Correlate identifiers where applicable.

---

## 32. Security / Safety Review

Completion review must confirm:

- no live network endpoint;
- no vendor credentials;
- no Agent-to-MCP path;
- no LLM numeric simulation authority;
- stale/expired Twin simulation blocked;
- invalid parameters blocked;
- simulation still requires Phase 4 approval;
- results are synthetic and limitation-labelled.

---

## 33. ADRs

Create sequential ADRs after Phase 5 ADR 034 covering at least:

```text
035 Digital Twin as separate versioned projection
036 Manual/on-demand Twin synchronization
037 Twin freshness and stale-simulation policy
038 txPower-only parameter registry
039 Deterministic versioned synthetic simulation model
040 Immutable simulation runs and provenance
041 Phase 4 MCP remains simulation execution boundary
042 Simulation evidence, confidence and limitations
```

Use established repository ADR format.

---

## 34. Documentation

Update as appropriate:

```text
README.md
docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md
docs/implementation/SNIP-IMPLEMENTATION-STATUS.md
.cursor/rules/snip-architecture.mdc
```

Copy architecture/spec into established repository documentation locations following prior phase conventions.

---

## 35. Explicitly Out of Scope

Do NOT implement:

```text
electricalTilt simulation
arbitrary parameter simulation
full RF propagation
ray tracing
vendor-calibrated models
live network writes
automatic optimization
best-parameter search
reinforcement learning
ML training
Digital Twin Agent
Optimization Agent
whole-network Twin
core-network Twin
transport Twin
remote vendor Twin
automatic Kafka-driven Twin synchronization
continuous Twin synchronization
graph database
3D GIS Twin
mobility simulation
traffic forecasting
production-grade capacity planning
Phase 7
```

---

## 36. Acceptance Criteria — Baseline

- [ ] Starts from `4e7a8feeb7a16924666e094828db829d5f4b703c`.
- [ ] Phase 1–5 regressions pass.
- [ ] Phase 4 governance remains authoritative.
- [ ] Phase 5 Agent boundaries remain unchanged.
- [ ] No live network writes.

---

## 37. Acceptance Criteria — Twin

- [ ] Cell-centric `NetworkTwin` implemented.
- [ ] Immutable versioned snapshots implemented.
- [ ] Manual/on-demand synchronization implemented.
- [ ] Provenance persisted.
- [ ] CURRENT/STALE/EXPIRED implemented.
- [ ] STALE/EXPIRED simulation blocked.
- [ ] Resynchronization creates a new version.

---

## 38. Acceptance Criteria — Scenario / Parameter

- [ ] `SimulationScenario` implemented.
- [ ] `ScenarioChange` implemented.
- [ ] Static parameter registry implemented.
- [ ] `txPower` supported.
- [ ] `electricalTilt` not supported.
- [ ] Invalid/out-of-range values rejected.
- [ ] Baseline mismatch rejected.

---

## 39. Acceptance Criteria — Simulation

- [ ] Deterministic non-LLM model implemented.
- [ ] Model ID/version recorded.
- [ ] SimulationRun immutable.
- [ ] Rerun creates a new run.
- [ ] Metric comparisons explicit.
- [ ] Confidence categorical.
- [ ] Structured limitations mandatory.
- [ ] Exact Twin version recorded.
- [ ] Synthetic result clearly labelled.

---

## 40. Acceptance Criteria — Governance

- [ ] `simulation.cell-parameter.v1` delegates to Phase 6.
- [ ] SIMULATE remains MEDIUM.
- [ ] REQUIRE_APPROVAL unchanged.
- [ ] Pre-approval execution blocked.
- [ ] Approved execution reaches MCP.
- [ ] MCP reaches Digital Twin simulation.
- [ ] No public bypass execution endpoint.
- [ ] No Agent direct MCP invocation.

---

## 41. Acceptance Criteria — Comparison / Failure

- [ ] Two scenarios against same Twin can be compared.
- [ ] Comparison exposes trade-offs.
- [ ] No automatic optimization.
- [ ] Stale Twin proof passes.
- [ ] Unsupported parameter proof passes.
- [ ] Model determinism proof passes.
- [ ] Immutable history proof passes.
- [ ] Failure cases fail closed.

---

## 42. Acceptance Criteria — CI / Docs

- [ ] `mvn -B test` passes.
- [ ] `go test ./...` passes.
- [ ] `go build ./cmd/simulator` passes.
- [ ] Default CI requires no Ollama.
- [ ] Local E2E documented.
- [ ] ADRs 035–042 created.
- [ ] Architecture/status docs updated.
- [ ] No secrets/generated binaries committed.
- [ ] Phase 7 not started.

---

## 43. Required Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-6-COMPLETION-REPORT.md
```

Include:

1. Executive Summary
2. Phase 5 Baseline Verification
3. Scope Delivered
4. NetworkTwin Domain
5. Twin Versioning
6. Twin Synchronization
7. Freshness / Drift
8. Twin Provenance
9. SimulationScenario
10. Parameter Registry
11. txPower Validation
12. Deterministic Simulation Model
13. Model Formula / Assumptions
14. Model Versioning
15. Simulation Confidence
16. Structured Limitations
17. SimulationRun / Immutability
18. Metric Comparison
19. Scenario Comparison
20. PostgreSQL / Flyway
21. APIs
22. Phase 4 MCP Integration
23. Approval Boundary
24. Phase 5 Agent Boundary
25. Decision Intelligence Integration
26. Canonical Scenario
27. Stale Twin Proof
28. Failure Cases
29. Tests
30. Local E2E Evidence
31. Observability
32. Security / Zero-Live-Write Review
33. ADRs
34. Performance
35. Acceptance PASS/FAIL
36. Known Limitations
37. Technical Debt
38. Lessons Learned
39. Recommended Next Phase
40. Architectural Questions

End with exactly one:

```text
PHASE 6 STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 6 STATUS: ACCEPTANCE NOT RECOMMENDED
```

Do not mark Phase 6 architecturally accepted yourself.

---

## 44. Final Instruction to Cursor

Treat this as the authorised scope for **Phase 6 only**.

The objective is:

> **Establish a versioned, auditable Cell Digital Twin and deterministic synthetic simulation capability so SNIP can evaluate a governed hypothetical `txPower` change against a reproducible baseline before any real network mutation exists.**

Preserve Phase 4 governance and Phase 5 Agent least privilege.

Do not broaden Phase 6.

Do not start Phase 7.

Do not commit or push a new Git baseline until the completion report has been reviewed and explicit architectural acceptance has been granted.

When implementation and validation are complete, produce the Phase 6 completion report and STOP.
