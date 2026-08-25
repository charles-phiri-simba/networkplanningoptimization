# SNIP Phase 6 — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-25  
**Architecture:** `docs/architecture/SNIP-PHASE-6-DIGITAL-TWIN-SIMULATION-INTELLIGENCE-ARCHITECTURE.md`  
**Contract:** `docs/implementation/SNIP-PHASE-6-DIGITAL-TWIN-SIMULATION-INTELLIGENCE-SPECIFICATION.md`  
**Baseline:** `4e7a8feeb7a16924666e094828db829d5f4b703c` on `main` (Phase 5 architecturally accepted, 83 tests). HEAD is still this commit; Phase 6 is uncommitted working-tree work.  
**Method:** Extend Phase 5; `mvn -B test` (PostgreSQL + Kafka Testcontainers; stub generator; no Ollama); `go test ./...` and `go build ./cmd/simulator`. Phase 7 was not started. Git push / new baseline were not authorised.

---

## 1. Executive Summary

Phase 6 adds a **cell-centric Digital Twin and deterministic synthetic simulation foundation**. Operational CELL-001 state is projected into an immutable versioned Twin. A persisted `txPower` scenario is evaluated only after Phase 4 MEDIUM / REQUIRE_APPROVAL and MCP `simulation.cell-parameter.v1`. The LLM does not produce numeric KPI predictions. Agents still cannot call MCP or write the network.

Canonical proofs (CI stub path, 98 tests):

| Proof | Result |
|-------|--------|
| CELL-001 Twin sync | Versioned snapshot, provenance `SNIP_OPERATIONAL_STATE`, `synthetic=true` |
| Scenario 46→44 dBm | Equivalent 2 dB drop to architecture 40→38 on the demo fixture (`txPower=46`) |
| Governed Path B | MEDIUM / REQUIRE_APPROVAL; pre-approval 409; MCP delegates to Twin model |
| Comparison 46→44 vs 46→42 | Trade-offs returned; `automaticOptimumSelected=false` |
| Stale Twin | Simulation rejected until resync creates `N+1` |
| APPLY | HIGH / DENY; no MCP |

`mvn -B test`: **98 tests, 0 failures** (2026-08-25T07:38+02:00). `go test ./...` PASS. `go build ./cmd/simulator` exit 0. Ollama not used. No live network write path.

---

## 2. Phase 5 Baseline Verification

| Check | Result |
|-------|--------|
| Started from `4e7a8feeb7a16924666e094828db829d5f4b703c` | Yes (`git rev-parse HEAD`) |
| Phase 1–5 regressions | PASS (83 baseline tests remain in the 98) |
| Phase 4 risk/policy/approval/MCP unchanged except simulation tool delegation | PASS |
| Phase 5 Agent boundaries unchanged | PASS (no Agent import of MCP/execution) |
| Kafka default off | PASS |
| No live network write path | PASS |

---

## 3. Scope Delivered

- Cell-scoped `NetworkTwin` + immutable `NetworkTwinVersion`
- Manual/on-demand `TwinSynchronizationService`
- CURRENT / STALE / EXPIRED freshness
- Persisted `SimulationScenario` / `ScenarioChange`
- In-code `txPower` registry
- `CellParameterSimulationModel` (`snip.synthetic.cell-parameter.v1` / `1.0`)
- Immutable `SimulationRun` + metrics + limitations
- Scenario comparison without optimization
- Flyway `V7__digital_twin_simulation.sql`
- Twin management APIs; no public simulate endpoint
- MCP `simulation.cell-parameter.v1` delegates to Phase 6
- ADRs 035–042
- Tests, README, this report

---

## 4. NetworkTwin Domain

`NetworkTwin` is CELL-scoped (`scopeType=CELL`, `scopeId=CELL-001`). Fields: `id`, `name`, `status=ACTIVE`, `latestVersion`, `createdAt`, `synchronizedAt`, `synthetic`. It is a projection, not the operational cell row.

---

## 5. Twin Versioning

Each successful synchronize inserts `NetworkTwinVersion` `N+1` and never updates prior versions. The version stores captured/synchronized timestamps, `sourceEventTime`, `sourceContextVersion` fingerprint, provenance JSON, cell/serving identity, radio configuration, current KPIs, temporal summaries, and neighbour summaries.

---

## 6. Twin Synchronization

`TwinSynchronizationService.synchronizeCell` resolves `NetworkContextService`, builds a simulation-safe snapshot, records provenance, persists the next version, and updates Twin metadata. No LLM. No Kafka listener. Governed execution does not resynchronize.

---

## 7. Freshness / Drift

Algorithm (ADR 037):

1. `EXPIRED` if `now − synchronizedAt ≥ snip.twin-expired-hours` (default 24).
2. Else `STALE` if current operational fingerprint ≠ snapshot fingerprint.
3. Else `CURRENT`.

Fingerprint = `txPower={value}@{effectiveFrom}` plus each current KPI `{metric}={value}@{observedAt}`. Simulation allowed only for CURRENT.

---

## 8. Twin Provenance

Every version records `source=SNIP_OPERATIONAL_STATE`, cell id, context fingerprint, latest telemetry timestamp, capture timestamp, and `synthetic`. No vendor/live-network provenance is implied.

---

## 9. SimulationScenario

Persisted with `twinId`, `baselineTwinVersion`, name, description, `ACTIVE`, `createdAt`, `createdBy`, `synthetic=true`. One `ScenarioChange` row: `parameterId`, `currentValue`, `proposedValue`, `unit`. Current value must equal Twin baseline `txPower`.

---

## 10. Parameter Registry

In-code whitelist. Phase 6 registers only `txPower` (dBm, 20–50, CELL, enabled). `electricalTilt` is not registered.

---

## 11. txPower Validation

Rejected: unknown parameter, `electricalTilt`, `pci`, below 20, above 50, baseline mismatch (for example currentValue 40 against CELL-001 baseline 46).

---

## 12. Deterministic Simulation Model

`CellParameterSimulationModel` is non-LLM, synthetic, unit-tested, and versioned. Required inputs: current/proposed txPower, BLER_DL, PRB_UTILIZATION_DL. Optional: THROUGHPUT_DL and BLER trend (recorded in assumptions only). Missing BLER or PRB is a model failure (CELL-003 seed).

---

## 13. Model Formula / Assumptions

Let `R = currentTxPower_dBm − proposedTxPower_dBm`.

```text
BLER_DL'            = clamp(BLER_DL * (1 + 0.025 * R), 0, 1)
PRB_UTILIZATION_DL' = clamp(PRB_UTILIZATION_DL * (1 − 0.01 * R), 0, 1)
THROUGHPUT_DL'      = max(0, THROUGHPUT_DL * (1 − 0.02 * R))   if present
```

Outputs rounded to 6 decimal places, `HALF_UP`. Example (unit-tested): 46→44 dBm, BLER 0.12, PRB 0.84, throughput 42 → BLER 0.126, PRB 0.8232, throughput 40.32.

Assumptions always include: synthetic engineering model — not vendor-calibrated RF physics; isolated cell; linear first-order sensitivity; no mobility/traffic forecast.

---

## 14. Model Versioning

`modelId=snip.synthetic.cell-parameter.v1`, `modelVersion=1.0`, `modelType=RULE_BASED`. Recorded on every succeeded run and in MCP structured content.

---

## 15. Simulation Confidence

Categorical `LOW` / `MEDIUM` / `HIGH`. Phase 6 synthetic model always returns `LOW`. No percentages.

---

## 16. Structured Limitations

Every success persists:

- `NO_RF_PROPAGATION_MODEL`
- `NO_VENDOR_CALIBRATION`
- `NO_MOBILITY_MODEL`
- `NO_TRAFFIC_FORECAST`
- `SYNTHETIC_KPI_MODEL`

---

## 17. SimulationRun / Immutability

Each MCP execution inserts a new run (`SUCCEEDED` with metrics/limitations). Re-run of the same scenario allocates a new `simulationId`; the prior run remains queryable with identical metrics. Completed rows are not updated.

---

## 18. Metric Comparison

Each run stores `metric`, `baselineValue`, `candidateValue`, `delta`, `unit`. Ratios remain ratios internally. `txPower` is included as the input change (delta −2.0 for 46→44).

---

## 19. Scenario Comparison

`GET /api/v1/simulation-comparisons?left={id}&right={id}` requires two SUCCEEDED runs on the same Twin baseline. Response lists per-metric left/right deltas. `automaticOptimumSelected` is always `false`.

---

## 20. PostgreSQL / Flyway

`V7__digital_twin_simulation.sql`: `network_twin`, `network_twin_version`, `simulation_scenario`, `simulation_scenario_change`, `simulation_run`, `simulation_result_metric`, `simulation_limitation`. No additional database.

---

## 21. APIs

Management (Twin/scenario state only):

```text
POST /api/v1/twins/cells/{cellId}/synchronize
GET  /api/v1/twins/{twinId}
GET  /api/v1/twins/{twinId}/versions
GET  /api/v1/twins/{twinId}/versions/{version}
POST /api/v1/twins/{twinId}/scenarios
GET  /api/v1/twins/{twinId}/scenarios
GET  /api/v1/scenarios/{scenarioId}
GET  /api/v1/simulations/{simulationId}
GET  /api/v1/simulation-comparisons?left=&right=
```

No `POST /simulate`. Execution remains Phase 4 action approve/execute.

---

## 22. Phase 4 MCP Integration

Capability ID `simulation.cell-parameter.v1` is unchanged. `SimulationCellParameterTool` delegates to `DigitalTwinSimulationService.executeFromMcp`. Path B still requires approval then MCP; output includes Twin version, model id/version, limitations, `synthetic=true`, `dryRun=true`, `networkWriteAttempted=false`.

---

## 23. Approval Boundary

`SIMULATE_CELL_PARAMETER_CHANGE` remains MEDIUM / REQUIRE_APPROVAL. Pre-approval execute is 409. APPLY remains HIGH / DENY with no MCP.

---

## 24. Phase 5 Agent Boundary

No Digital Twin Agent. Agents may still propose `SIMULATE_CELL_PARAMETER_CHANGE` (`AgentProposalAdapter` now labels `parameter=txPower`). Agent package has no dependency on `McpCapabilityGateway`, `ActionExecutionService`, or `/mcp`. Agents cannot mutate Twin baselines.

---

## 25. Decision Intelligence Integration

`SimulationDetailDto` exposes baseline/candidate metrics, deltas, confidence, limitations, assumptions, Twin version, model version, and provenance. Narrative cannot overwrite these fields. Comparison does not choose a best scenario.

---

## 26. Canonical Scenario

Demo fixture CELL-001 has `txPower=46` (not 40). Canonical A is **46→44 dBm** (2 dB, equivalent to 40→38). Canonical B is **46→42 dBm** (4 dB, equivalent to 40→36). Flow: synchronize → scenario → propose SIMULATE → 409 → approve → MCP → Twin model → immutable result.

Testcontainers evidence (GovernedActionApiTest Path B / DigitalTwinApiTest):

| Field | Value |
|-------|--------|
| twinId | `482068f8-435c-4137-8b81-b30b7d84e852` (Path B) / `75d9bd31-c79c-423f-8d7f-9495c3264676` (comparison) |
| Twin version | 1 (then N+1 after resync) |
| freshness at execute | CURRENT |
| simulationId (Path B) | `17f433ef-6153-45ed-ade1-b5f2c8216574` |
| actionId (Path B) | `7a95a692-05c4-4808-9a14-8fcc3cbafc68` |
| model | `snip.synthetic.cell-parameter.v1` / `1.0` |
| input | txPower 46 → 44 |
| confidence | LOW |
| limitations | five structured codes |
| synthetic | true |
| network write | `networkWriteAttempted=false`; APPLY still DENY |

---

## 27. Stale Twin Proof

Synchronize → create scenario → project later BLER_DL → Twin GET returns STALE → approved execute FAILED with STALE (model not producing a succeeded run) → resynchronize creates version N+1 → new scenario simulates successfully. No automatic Twin synchronization.

---

## 28. Failure Cases

| Case | Outcome |
|------|---------|
| unknown Cell | 404 on synchronize |
| unknown Twin / version | 404 |
| STALE Twin | execute FAILED; resync required |
| EXPIRED Twin | execute FAILED after `synchronized_at` aged ≥ 24 h |
| unsupported parameter / electricalTilt / pci | 400 on scenario create |
| out-of-range txPower | 400 |
| baseline mismatch | 400 |
| missing approval | 409 |
| invalid/missing scenarioId | execute FAILED |
| model failure (CELL-003 missing PRB) | execute FAILED |
| APPLY | DENY, no MCP |

All fail closed. None write the network.

---

## 29. Tests

98 Maven tests, 0 failures. Added: `DigitalTwinApiTest` (7), `CellParameterSimulationModelTest` (3), `TwinFreshnessEvaluatorTest` (3), `SimulatableParameterRegistryTest` (2). Path B updated to Twin-backed txPower. Phase 1–5 regressions pass. CI remains Ollama-free.

---

## 30. Local E2E Evidence

Ollama is not required for Phase 6 correctness. The local proof is `mvn -B test` against PostgreSQL Testcontainers, which exercises synchronize → scenario → Phase 4 approval → loopback MCP → Twin model → GET result/comparison, plus stale/expired/validation failures. Host port 8080 was occupied by an unrelated stack during this run; a second long-running `spring-boot:run` was not started. Recorded identifiers are in §26 from that Testcontainers run. `networkWriteAttempted=false` is in MCP output. APPLY still never reaches MCP.

---

## 31. Observability

Counters/logs: `twinSynchronizations`, `twinSynchronizationFailures`, `twinVersionsCreated`, `twinStaleDetections`, `simulationScenariosCreated`, `simulationRunsStarted`, `simulationRunsSucceeded`, `simulationRunsFailed`, `simulationLatencyMs`, `scenarioComparisons`. Correlated with `cellId`, `twinId`, version, `scenarioId`, `simulationId`, `actionId`.

---

## 32. Security / Zero-Live-Write Review

- No live network endpoint or vendor credentials added
- No Agent-to-MCP path
- No LLM numeric simulation authority
- Stale/expired simulation blocked
- Invalid parameters blocked
- Simulation still requires Phase 4 approval
- Results labelled synthetic with structured limitations
- `networkWriteAttempted=false` on succeeded MCP payload

---

## 33. ADRs

| ADR | Title |
|-----|--------|
| 035 | Digital Twin as a separate versioned projection |
| 036 | Manual on-demand Twin synchronization |
| 037 | Twin freshness and stale-simulation policy |
| 038 | txPower-only parameter registry |
| 039 | Deterministic versioned synthetic simulation model |
| 040 | Immutable simulation runs and provenance |
| 041 | Phase 4 MCP remains simulation execution boundary |
| 042 | Simulation evidence, confidence, and limitations |

---

## 34. Performance

Synchronize, scenario create, and in-process model evaluation complete well inside the existing 3 s MCP timeout in CI. No new remote simulation engine.

---

## 35. Acceptance PASS/FAIL

| Criterion group | Result |
|-----------------|--------|
| Baseline / Phase 1–5 / no live writes | PASS |
| Twin / versions / sync / freshness | PASS |
| Scenario / txPower registry / validation | PASS |
| Deterministic model / immutability / limitations | PASS |
| Governance / MCP delegation / no bypass / no Agent MCP | PASS |
| Comparison / stale / failure / CI / ADRs / docs | PASS |
| Phase 7 not started | PASS |

---

## 36. Known Limitations

- Synthetic linear model, not vendor-calibrated RF
- `txPower` only; `electricalTilt` deferred
- Manual sync only; Kafka-triggered Twin updates deferred
- Confidence is conservatively LOW
- CELL-001 fixture baseline is 46 dBm, not the architecture’s illustrative 40 dBm
- Comparison endpoint is `GET /api/v1/simulation-comparisons` to avoid clashing with `GET /api/v1/simulations/{id}`

---

## 37. Technical Debt

- Non-interruptible per-Agent timeout (Phase 5, preserved)
- FAILED Phase 4 `action_result` row replacement (Phase 4, preserved)
- Kafka listener `groupId` hardcoded (Phase 2, preserved)
- FAILED Twin simulation attempts are not persisted as `SimulationRun` rows (MCP/action FAILED is the audit); optional later
- Action list pagination (carried)

---

## 38. Lessons Learned

- Returning a lazy Twin association from one `@Transactional` method and reading it in another causes `LazyInitializationException` after the session closes; version/scenario `ManyToOne` Twin fetch is EAGER for this slice.
- Architecture’s 40→38 example must be mapped to the actual CELL-001 radio fixture (46 dBm) without rewriting seed data.
- A literal `/simulations/compare` path is captured as `{simulationId}` unless given a distinct route.

---

## 39. Recommended Next Phase

Phase 6 is **frozen**. Do not add functionality, resolve deferred technical debt, perform unrelated refactoring, or start Phase 7 from this report. Agent Factory, production RF / whole-network Twin, Kafka-triggered Twin sync, electricalTilt, automatic optimization, remote MCP, and live network writes remain closed until explicitly authorised.

---

## 40. Architectural Questions

Phase 6 was architecturally accepted on 2026-08-25. The architectural review accepts all Phase 6 locked decisions listed in this section. The locked decisions are closed as follows:

1. **Cell-centric Twin separate from operational DB — ACCEPT.**
2. **Immutable versioned snapshots — ACCEPT.**
3. **Manual/on-demand synchronization only — ACCEPT.**
4. **CURRENT / STALE / EXPIRED; STALE/EXPIRED block simulation — ACCEPT.**
5. **Mandatory Twin provenance — ACCEPT.**
6. **Persisted SimulationScenario — ACCEPT.**
7. **txPower-only in-code registry — ACCEPT.**
8. **Deterministic non-LLM synthetic model with model id/version — ACCEPT.**
9. **Categorical confidence and structured limitations — ACCEPT.**
10. **Immutable SimulationRuns — ACCEPT.**
11. **Scenario comparison without automatic optimization — ACCEPT.**
12. **PostgreSQL + Flyway — ACCEPT.**
13. **Phase 4 → MCP remains the execution boundary — ACCEPT.**
14. **Phase 4 approval authoritative; Agent direct execution prohibited — ACCEPT.**
15. **No Digital Twin Agent; no live network writes; no Phase 7 — ACCEPT.**

Do not treat this list as a request to reopen locked decisions.

---

PHASE 6 STATUS: ARCHITECTURALLY ACCEPTED
