# SNIP Product Increment 7 — What-If Configuration Scenario Workspace

**Architecture status:** ACCEPTED (product increment; not a numbered SNIP phase)

**Implementation status:** NOT STARTED

**Parent baseline:** `5305982c200ae55d45f94ce8397af4a1b004dae0` (Product Increment 6, CLOSED)

**Readiness decision:** `BOUNDED_PLANNING_DOMAIN_REQUIRED`

**Selected option:** B — `PERSISTENT_PLANNING_SCENARIO_AGGREGATE`

**Architecture impact:** `BOUNDED_PLANNING_DOMAIN_EXTENSION`

**Assurance:** `ELEVATED_PRODUCT_ASSURANCE`

**Authoritative title:** What-If Configuration Scenario Workspace

**Product truth subtitle:** Independent cell-local synthetic what-if evaluation. Not joint site simulation.

Do **not** title, navigate, or market PI7 as:

- Network Planning Simulator
- Site Simulator
- Multi-Cell Simulator
- Site Optimization
- Network Optimization
- Site-aware ranking
- Coordinated multi-cell proposal

---

## 1. Context and objective

PI3 is governed single-cell `txPower` optimization. PI6 is frontend-only multi-cell investigation. Neither lets an engineer **construct, name, preserve, compare, and reopen** alternative configuration intents before entering Optimize.

PI7 introduces a **bounded planning domain** that correlates existing cell-local Digital Twin versions and existing cell-local synthetic simulations. It does not extend the simulator, create a site Digital Twin, or grant mutation authority.

An operator must be able to:

1. create a named what-if scenario
2. select a small bounded set of cells
3. create named configuration alternatives
4. specify `txPower` intents for individual cells
5. evaluate those intents using `snip.synthetic.cell-parameter.v1`
6. preserve simulation evidence
7. compare alternatives **per cell**
8. reopen the scenario later
9. see exactly which Digital Twin version was evaluated
10. select **one** cell and hand off to existing PI3 Optimize

PI7 is planning/evidence. PI7 is **not** execution authority.

---

## 2. Current-state evidence

Frozen on the PI6 baseline:

| Fact | Evidence |
|---|---|
| Digital Twin scope is CELL only | `TwinScopeType`, `network_twin` CHECK `scope_type IN ('CELL')` |
| One twin per cell | UNIQUE `(scope_type, scope_id)` |
| Twin versions are immutable snapshots | `network_twin_version` UNIQUE `(twin_id, version)` |
| Neighbour summary is stored and unused by the model | `TwinSnapshot.NeighbourSummary`; `CellParameterSimulationModel` |
| `SimulationScenario` is one cell, one change | `simulation_scenario_change.scenario_id` UNIQUE |
| Parameter whitelist is `txPower` 20–50 dBm | `SimulatableParameterRegistry` (`electricalTilt` named, not registered) |
| Simulation requires CURRENT | `TwinSynchronizationService.requireCurrentForSimulation` |
| Simulation execute is MCP-shaped, GET-only publicly | `DigitalTwinSimulationService.executeFromMcp`; `TwinController` has no POST simulate |
| Same-twin comparison only | `SimulationQueryService.compare` requires same `twinId` and `baselineTwinVersion` |
| Phase 13 owns candidate scenario+sim internally | `NetworkChangeProposalGenerationService` |
| Frontend cannot create/evaluate scenarios | `snipApi` has `getSimulation` + `synchronizeCellTwin` only |
| Demo identity is session metadata | `demoIdentity.ts` |
| PI3 Optimize route | `/network/cells/:cellId/optimize` |
| Latest Flyway version | `V19__phase18_production_change_campaigns.sql` |
| No planning / what-if aggregate exists | repository search |

The isolated-cell model assumption is explicit: neighbour coupling is not modelled. `SimulationLimitation` includes `NO_RF_PROPAGATION_MODEL`, `NO_VENDOR_CALIBRATION`, `NO_MOBILITY_MODEL`, `NO_TRAFFIC_FORECAST`, `SYNTHETIC_KPI_MODEL`.

---

## 3. Architecture decision

```text
OPTION:                              B_PERSISTENT_PLANNING_SCENARIO_AGGREGATE
BOUNDED CONTEXT:                     com.simba.snip.npo.planning
NEW PERSISTENCE:                     YES
DATABASE MIGRATION:                  YES (V20)
NEW SIMULATION MODEL:                NO
NEW MCP TOOL:                        NO
NEW AGENT:                           NO
SITE DIGITAL TWIN:                   NO
JOINT MULTI-CELL SIMULATION:         NO
CROSS-CELL EFFECTS:                  NOT_IMPLEMENTED
SITE / NETWORK SCORE:                PROHIBITED
PI7 MUTATION AUTHORITY:              NO
AUTOMATIC PHASE 13 PROPOSAL:         NO
PHASE 14 / 15 / 16 / 17 / 18:        NO INTEGRATION
PHASE 19:                            NOT STARTED
REAL PRODUCTION EXECUTION:           NOT_AUTHORIZED
```

Option A (ephemeral frontend composition) is rejected: the UI cannot evaluate today, composition cannot reload/share/audit, and it would tempt reuse of Phase 13 proposal IDs.

Option C (coupled multi-cell simulator) is rejected: the repository proves isolation is intentional, and the PI7 product problem does not require joint RF.

---

## 4. Bounded context

**Java package:** `com.simba.snip.npo.planning`

This follows existing domain packages (`changeintelligence`, `changeplanning`, `changeexecution`) and avoids:

- `whatif` (product language, not a repository convention)
- `changeplanning` (Phase 14 execution-readiness; different authority)
- `twin` (must not absorb a multi-cell aggregate)
- `persist` as the home of new services (entities may live under `planning.persist` like Phase 14)

**Allowed dependencies (inbound to existing frozen domains):**

- `twin.TwinSynchronizationService` — resolve twin, freshness, `requireCurrentForSimulation` (read / admit only)
- `twin.TwinScenarioService` — create/read cell-local `SimulationScenario`
- `twin.DigitalTwinSimulationService` — admitted cell-local dry-run
- `twin.SimulatableParameterRegistry` — `txPower` range 20–50 dBm
- `twin.CellParameterSimulationModel` constants — `MODEL_ID` / `MODEL_VERSION` (do not copy formulas)
- `twin.SimulationQueryService` — read simulation evidence
- inventory/cell existence via existing network persistence
- `domain` exceptions / `DomainRules`

**Forbidden dependencies:**

- `productionchange`, Production Write Gateway, grants
- `productioncampaign`
- Phase 17 transport
- `changeintelligence` generate/approve/reject
- `changeplanning` plan create/authorize
- `changeexecution` sandbox execute
- `agent`, Agent Registry, `/agent-runs`
- MCP client / new MCP tools
- credential providers, Key Vault, ENM, connector factory

Planning has **no dependency that confers production mutation authority**.

API controllers for this context live in `com.simba.snip.npo.planning.api` (not `com.simba.snip.npo.api`) so the planning surface is visibly separate from Twin/MCP-era controllers.

---

## 5. Domain model

### 5.1 Aggregates

**PlanningScenario** (root)

- Identity, name, optional description, demo `createdBy`, timestamps, optimistic `rowVersion`
- Selected cells (membership)
- Named alternatives
- Cell intents (owned by alternatives)
- Evaluations (immutable children)

The scenario is **always editable** when no evaluation is `IN_PROGRESS`. There is no `APPROVED`, `AUTHORIZED`, `READY_FOR_EXECUTION`, or `EXECUTED` state.

**PlanningAlternative**

- Belongs to one scenario
- `name` unique per scenario (case-insensitive)
- `ordinal` unique per scenario, contiguous from 1
- Not a “Baseline” row

**Baseline is not stored as an alternative.** Each admitted simulation already returns baseline vs candidate KPIs. Storing a no-change “Baseline” alternative would duplicate that evidence and invite a fake site baseline object.

**PlanningCellIntent**

- Belongs to one alternative
- `cellId` must be in the scenario membership
- `parameter` = `txPower` only
- `intendedValue` in registry range
- At most one intent per `(alternative, cell)`
- Baseline value is **not** stored on the intent (it would drift)

**PlanningEvaluation** (immutable once terminal)

- Belongs to one scenario
- `intentFingerprint`
- `admissionFingerprint` (pinned twins/versions after admission)
- `status`: `IN_PROGRESS` | `SUCCEEDED` | `PARTIAL` | `FAILED`
- `createdBy`, `createdAt`, `completedAt`
- Items

**PlanningEvaluationItem**

- One item per `(evaluation, alternative, cell)`
- Outcome: `SUCCEEDED` | `FAILED`
- Failure class when failed (admission vs simulation)
- Pinned `twinId`, `twinVersion`, captured baseline `txPower`, configuration fingerprint
- Child `simulationScenarioId`, `simulationRunId` when created/reused
- Model id/version, `synthetic`, `confidence`

### 5.2 Derived (not persisted as governance states)

| Derived view | Meaning |
|---|---|
| `NOT_EVALUATED` | no terminal evaluation whose `intentFingerprint` matches current intents |
| `EVALUATING` | an `IN_PROGRESS` evaluation exists for the scenario |
| `EVALUATED` | latest matching-intent evaluation is `SUCCEEDED` |
| `PARTIAL` | latest matching-intent evaluation is `PARTIAL` |
| `FAILED` | latest matching-intent evaluation is `FAILED` |
| `STALE` | a later terminal evaluation exists but `intentFingerprint` ≠ current intents |

Twin freshness is **not** an evaluation status. A matching-intent evaluation remains the current evaluation of those intents after operational drift. The UI must label it **Historical evaluation against pinned versions** and show CURRENT/STALE/EXPIRED as a **re-evaluation prerequisite**, not as deletion of evidence.

### 5.3 What “multi-cell” means

A scenario may contain several cells. That means:

> A named configuration scenario containing several **independent** cell-level `txPower` intents.

It does **not** mean a jointly simulated RF system. No item may infer an effect on another cell.

---

## 6. Persistence model

Flyway migration **to be written only during implementation:**

`snip-npo-app/src/main/resources/db/migration/V20__planning_what_if_scenario.sql`

PostgreSQL, same style as V7/V15. No vendor-specific types beyond existing `TIMESTAMPTZ` / `UUID` / `TEXT` / `VARCHAR` / `NUMERIC`.

### 6.1 `planning_scenario`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| name | VARCHAR(128) | NOT NULL |
| description | VARCHAR(1024) | NOT NULL DEFAULT '' |
| created_by | VARCHAR(64) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |
| row_version | INTEGER | NOT NULL DEFAULT 1 |

No status column. No approval columns.

Indexes: `planning_scenario_updated_idx (updated_at DESC)`.

### 6.2 `planning_scenario_cell`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| scenario_id | UUID | NOT NULL FK → `planning_scenario(id)` ON DELETE RESTRICT |
| cell_id | VARCHAR(64) | NOT NULL |
| ordinal | INTEGER | NOT NULL |

UNIQUE `(scenario_id, cell_id)`  
UNIQUE `(scenario_id, ordinal)`  
CHECK `ordinal >= 1`

`cell_id` is the canonical inventory cell id (`CELL-001`), not a UUID. Existence is validated in the application against the `cell` table. No FK to `cell` if that would couple planning DDL to inventory surrogate keys; application validation is mandatory.

### 6.3 `planning_alternative`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| scenario_id | UUID | NOT NULL FK → `planning_scenario(id)` ON DELETE RESTRICT |
| name | VARCHAR(128) | NOT NULL |
| ordinal | INTEGER | NOT NULL |

UNIQUE `(scenario_id, ordinal)`  
UNIQUE `(scenario_id, lower(name))`  
CHECK `ordinal >= 1`

### 6.4 `planning_cell_intent`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| alternative_id | UUID | NOT NULL FK → `planning_alternative(id)` ON DELETE RESTRICT |
| cell_id | VARCHAR(64) | NOT NULL |
| parameter_id | VARCHAR(64) | NOT NULL |
| intended_value | NUMERIC(8,3) | NOT NULL |

UNIQUE `(alternative_id, cell_id)`  
CHECK `parameter_id = 'txPower'`  
CHECK `intended_value >= 20 AND intended_value <= 50`

Application must still call `SimulatableParameterRegistry` so the CHECK cannot drift from the registry without a dual-change. The CHECK is defense in depth, not a second source of truth.

### 6.5 `planning_evaluation`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| scenario_id | UUID | NOT NULL FK → `planning_scenario(id)` ON DELETE RESTRICT |
| status | VARCHAR(16) | NOT NULL |
| intent_fingerprint | VARCHAR(64) | NOT NULL; exactly 64 lowercase hex (`^[0-9a-f]{64}$`) |
| admission_fingerprint | VARCHAR(64) | set at Evaluate start from pre-execution admission identity; exactly 64 lowercase hex when present |
| created_by | VARCHAR(64) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| completed_at | TIMESTAMPTZ | NULL while `IN_PROGRESS` |
| item_count | INTEGER | NOT NULL |
| succeeded_count | INTEGER | NOT NULL DEFAULT 0 |
| failed_count | INTEGER | NOT NULL DEFAULT 0 |

CHECK `status IN ('IN_PROGRESS','SUCCEEDED','PARTIAL','FAILED')`  
CHECK `intent_fingerprint ~ '^[0-9a-f]{64}$'`  
CHECK `admission_fingerprint IS NULL OR admission_fingerprint ~ '^[0-9a-f]{64}$'`  
`VARCHAR(64)` is required because Hibernate schema validation maps PostgreSQL `CHAR`/`bpchar` as incompatible with the JPA `String` mapping used elsewhere in this repository. The CHECK, not the type, is the exact SHA-256 representation invariant.  
UNIQUE partial index: one `IN_PROGRESS` per scenario:

```sql
CREATE UNIQUE INDEX planning_evaluation_in_progress_uidx
    ON planning_evaluation (scenario_id)
    WHERE status = 'IN_PROGRESS';
```

Index: `(scenario_id, created_at DESC)`.

### 6.6 `planning_evaluation_item`

| Column | Type | Constraints |
|---|---|---|
| id | UUID | PK |
| evaluation_id | UUID | NOT NULL FK → `planning_evaluation(id)` ON DELETE RESTRICT |
| alternative_id | UUID | NOT NULL FK → `planning_alternative(id)` ON DELETE RESTRICT |
| cell_id | VARCHAR(64) | NOT NULL |
| parameter_id | VARCHAR(64) | NOT NULL |
| intended_value | NUMERIC(8,3) | NOT NULL |
| outcome | VARCHAR(16) | NOT NULL |
| failure_code | VARCHAR(64) | NULL |
| failure_message | VARCHAR(512) | NULL |
| twin_id | UUID | NULL FK → `network_twin(id)` |
| twin_version | INTEGER | NULL |
| pinned_baseline_tx_power | NUMERIC(8,3) | NULL |
| configuration_fingerprint | VARCHAR(128) | NULL |
| simulation_scenario_id | UUID | NULL FK → `simulation_scenario(id)` ON DELETE RESTRICT |
| simulation_run_id | UUID | NULL FK → `simulation_run(id)` ON DELETE RESTRICT |
| model_id | VARCHAR(128) | NULL |
| model_version | VARCHAR(32) | NULL |
| synthetic | BOOLEAN | NULL |
| confidence | VARCHAR(16) | NULL |
| reused_existing_run | BOOLEAN | NOT NULL DEFAULT FALSE |

UNIQUE `(evaluation_id, alternative_id, cell_id)`  
CHECK `outcome IN ('SUCCEEDED','FAILED')`  
CHECK `parameter_id = 'txPower'`  
CHECK `confidence IS NULL OR confidence IN ('LOW','MEDIUM','HIGH')`  
CHECK: `SUCCEEDED` implies `simulation_run_id`, `twin_id`, `twin_version`, `model_id`, `synthetic = TRUE` are NOT NULL

No cascade delete of simulation evidence. P0 has **no DELETE** of scenarios.

Avoid: generic command table, arbitrary JSON command payloads, field-level provenance tables, revision tables.

---

## 7. Digital Twin relationship

Planning **composes immutable CELL twin versions**. It does not:

- introduce `TwinScopeType.SITE`
- clone twins
- write twin snapshots
- call `TwinSynchronizationService.synchronizeCell` from Evaluate

For each intent at evaluation:

1. resolve `NetworkTwin` by `(CELL, cellId)`
2. require a latest version
3. call `requireCurrentForSimulation` on that latest version
4. pin `twinId` + `version`
5. read baseline `txPower` via `TwinScenarioService.baselineTxPower`
6. capture `sourceContextVersion` / operational fingerprint already stored on the version as `configuration_fingerprint`

Missing twin, STALE, EXPIRED, or missing `txPower` is an **item admission failure**. Evaluate never auto-synchronizes.

Operators synchronize through the existing explicit API:

`POST /api/v1/twins/cells/{cellId}/synchronize`

After sync, Evaluate is a separate explicit action.

---

## 8. Baseline semantics

| Moment | What is shown | Authoritative? |
|---|---|---|
| Create / edit | Inventory or live twin `txPower` as **current observed (not pinned)** | No |
| Evaluate admission | Twin snapshot `txPower` at the pinned CURRENT version | Yes for that evaluation |
| After drift | Historical item still reports the pinned baseline | Yes for that evaluation |
| After edit of intent | Previous evaluation becomes `STALE` (derived) | Historical only |

Do not silently overwrite historical `pinned_baseline_tx_power`.

Pinning an older version is **not** a license to re-simulate STALE/EXPIRED twins. Re-run against a drifted network requires operator sync + new evaluation.

---

## 9. Simulation relationship

Child evaluations reuse **unmodified**:

- `TwinScenarioService.create` — one cell-local `SimulationScenario`, one `txPower` change
- `DigitalTwinSimulationService` admission (`dryRun=true`, CURRENT, range, baseline match)
- `CellParameterSimulationModel` (`snip.synthetic.cell-parameter.v1` / `1.0` / `RULE_BASED` / `LOW` / `synthetic=true`)

**Required small enabler (existing class only):** add a typed application method on `DigitalTwinSimulationService`, for example:

```text
executeAdmittedCellLocalDryRun(UUID simulationScenarioId)
```

This method must:

- set `dryRun=true` internally
- omit `actionId` (planning is not a Phase 4 Action)
- reuse the same admission and persistence path as `executeFromMcp`
- accept **no** model id, tool name, or arbitrary parameter map from the HTTP API

It must **not** become `POST /api/v1/simulate`. The public command is planning-scoped (section 18).

Planning must not invoke MCP HTTP, register a new MCP tool, or require an Action approve/execute cycle.

`GET /api/v1/simulation-comparisons` remains same-twin only and is **not** the PI7 comparison API. PI7 comparison is assembled from evaluation items.

---

## 10. Evaluation orchestration

Command: **evaluate scenario** (all alternatives × all intents).

```text
POST /api/v1/planning/scenarios/{scenarioId}/evaluations
```

Algorithm (normative; implementation specification repeats it):

1. Begin short transaction. `SELECT ... FOR UPDATE` the scenario row.
2. If `rowVersion` supplied and mismatches → `409`.
3. Validate bounds and at least one intent. Reject **before** any simulation.
4. If an `IN_PROGRESS` evaluation exists → resume it (same transaction ends; continue items).
5. Compute `intentFingerprint` from current persisted intents.
6. Resolve would-be pins: for each distinct cell in the intent set, load CELL twin latest version, freshness, baseline `txPower`, and configuration token (read-only, no sync). Missing/STALE/EXPIRED/no-baseline cells contribute `-` pin fields.
7. Compute `wouldBeAdmissionFingerprint` from current intents + those pins. This value is pre-execution and MUST NOT include outcomes.
8. If latest terminal evaluation has the same `intentFingerprint` **and** the same `admissionFingerprint` as `wouldBeAdmissionFingerprint`:
   - `SUCCEEDED` → return it (idempotent; zero new runs).
   - `PARTIAL` / `FAILED` → create a **new** evaluation that stores the same `admissionFingerprint` and **reuses** SUCCEEDED children whose child identity (scenario, alternative, cell, parameter, intended value, twin/version, baseline, configuration token, model/version) still matches; re-attempt only FAILED/missing intents.
9. If some twins are not CURRENT / missing:
   - If a matching-intent `SUCCEEDED` terminal exists and `wouldBeAdmissionFingerprint` differs only because pins are no longer CURRENT → return that evaluation as historical evidence (no new runs).
   - Else → create a new evaluation, persist FAILED admission items for blocked cells, SUCCEEDED/FAILED independently for admitted cells, finalize `PARTIAL` or `FAILED`.
10. Else create `IN_PROGRESS` evaluation with both fingerprints already stored (`item_count` = number of intents). Commit the start transaction.
11. For each remaining intent, in deterministic order `(alternative.ordinal, cell_id)`:
    - own transaction
    - admit CURRENT + range
    - reuse an existing admitted `simulation_run` when section 13 allows
    - otherwise `TwinScenarioService.create` then `executeAdmittedCellLocalDryRun`
    - persist item
12. Finalize: `SUCCEEDED` if `failed_count=0`; `PARTIAL` if both counts > 0; `FAILED` if `succeeded_count=0`. Set `completed_at`.

Evaluate **must not** call `synchronizeCell`.

Maximum child runs created in one Evaluate: `MAX_CELLS × MAX_ALTERNATIVES` (section 26). Reused runs do not count as new executions but do count toward item cardinality.

---

## 11. Evaluation atomicity

**Chosen rule: per-intent fail-closed; evaluation-level PARTIAL.**

If Alternative A has CELL-001, CELL-002, CELL-003 and CELL-002 fails admission or simulation:

- CELL-001 and CELL-003 evidence **remain** if they succeeded
- CELL-002 item is `FAILED` with a failure code
- Evaluation status is `PARTIAL` (or `FAILED` if nothing succeeded)
- Successful `SimulationRun` rows are **not** rolled back

The alternative is **not** presented as fully evaluated. UI and DTO must expose item outcomes. A `PARTIAL` evaluation must not serialize a field that implies overall success.

All-or-nothing rollback is rejected: it would destroy truthful completed independent evidence.

---

## 12. Fingerprinting

Three identities. Only the first two are SHA-256 fingerprints.

| Identity | When computed | Contains outcomes? | Role |
|---|---|---|---|
| Intent fingerprint | from persisted planning contents | No | what the operator asked |
| Admission fingerprint | **before** child simulation | No | exact admitted inputs / pins |
| Evidence / result state | after each child and at finalize | Yes | what happened (items, runs, metrics, status) |

Canonicalization: sorted fields, `BigDecimal.stripTrailingZeros().toPlainString()`, UTF-8, `\n` separators, `-` for null/blank, lowercase 64-hex SHA-256. No `HashMap` iteration, no database row order, no incidental JSON property order.

### 12.1 Intent fingerprint

Binds editable planning content (not twin pins):

```text
schema=planning.intent.v1
scenarioId=<uuid>
alternative.<ordinal>.name=<normalized name>
alternative.<ordinal>.intent.<cellId>.parameter=txPower
alternative.<ordinal>.intent.<cellId>.intended=<normalized numeric>
modelId=snip.synthetic.cell-parameter.v1
modelVersion=1.0
```

Alternatives ordered by `ordinal`. Intents ordered by `cellId`. Numeric normalization: `BigDecimal.stripTrailingZeros().toPlainString()`.

If any intent/name/membership-used-by-intent changes, this fingerprint changes and the previous evaluation is derived `STALE`.

### 12.2 Admission fingerprint

Pre-execution admitted-input identity. Computable before any child simulation. MUST NOT contain `SUCCEEDED`, `FAILED`, `PARTIAL`, simulation ids, metrics, or failure codes.

```text
schema=planning.admission.v1
intentFingerprint=<hex>
item.<altOrdinal>.<cellId>.twinId=<uuid or ->
item.<altOrdinal>.<cellId>.twinVersion=<int or ->
item.<altOrdinal>.<cellId>.baseline=<normalized numeric or ->
item.<altOrdinal>.<cellId>.config=<configuration token or ->
item.<altOrdinal>.<cellId>.modelId=<id or ->
item.<altOrdinal>.<cellId>.modelVersion=<ver or ->
```

One line-group per intent. Items sorted by `altOrdinal`, then `cellId`. A CURRENT pin fills twin/baseline/config/model fields. A missing/STALE/EXPIRED/no-baseline cell uses `-`. A later successful pin therefore changes the fingerprint without encoding outcome.

Store this value on the `IN_PROGRESS` row at Evaluate start. Finalize MUST NOT recompute it from item outcomes.

No full revision table in P0. Fingerprint mismatch is sufficient invalidation.

### 12.3 Evidence / result state

Not a fingerprint. Persisted on `PlanningEvaluation` (`status`, counts, `completedAt`) and `PlanningEvaluationItem` (outcome, failure, simulation ids, confidence, `reusedExistingRun`). This is not the pre-execution idempotency key.

---

## 13. Idempotency

| Event | Behaviour |
|---|---|
| Double-click Evaluate after `SUCCEEDED` with same intents and same CURRENT pins | Return existing evaluation; **zero** new runs |
| HTTP retry while `IN_PROGRESS` | Resume; unique `(evaluation, alternative, cell)` prevents duplicate items |
| Crash after some items, before finalize | `IN_PROGRESS` remains; next Evaluate resumes; finalize from counts |
| Crash after start row, before any item | Resume creates items |
| `PARTIAL`/`FAILED` + same pins | New evaluation record; **reuse** matching SUCCEEDED runs; re-attempt FAILED only |
| Intents edited | Edits blocked during `IN_PROGRESS`; after terminal, new fingerprint → new evaluation |
| After operator sync (newer CURRENT version) | `wouldBeAdmissionFingerprint` differs → new evaluation; new pins; no silent rewrite of old items |

Do not implement idempotency with in-memory locks only. Durability is the partial unique index + item unique key + scenario row lock.

Client `Idempotency-Key` is **not** required in P0; the fingerprints are the natural keys.

---

## 14. Concurrency

| Conflict | Handling |
|---|---|
| Two Evaluate requests | `SELECT FOR UPDATE` + unique `IN_PROGRESS` index → loser `409 EVALUATION_IN_PROGRESS` or resumes the winner’s row if it can see it |
| PATCH during Evaluate | `409 EVALUATION_IN_PROGRESS` |
| Two PATCHes | `row_version` optimistic check → `409` |
| Evaluate with stale `rowVersion` | `409` |

No distributed lock manager. PostgreSQL row lock + optimistic version is sufficient (single JVM app, same as existing domains).

---

## 15. Failure and recovery

| Failure | Recovery |
|---|---|
| Twin missing | Item `FAILED` / `ADMISSION_TWIN_MISSING`; others continue |
| STALE / EXPIRED | Item `FAILED` / `ADMISSION_TWIN_NOT_CURRENT`; no sync |
| Incompatible / no txPower | Item `FAILED` / `ADMISSION_BASELINE_MISSING` |
| `TwinScenarioService` / model validation | Item `FAILED` / `SIMULATION_REJECTED` |
| Unexpected runtime | Item `FAILED` / `SIMULATION_FAILED`; log; do not write a planning-owned run |
| Process crash | Resume `IN_PROGRESS` |
| Client timeout | Retry Evaluate (resume) |

`DigitalTwinSimulationService` already persists SUCCEEDED runs in its own transaction. Planning records the reference after that commit. Child reuse is allowed only when a prior `planning_evaluation_item` proves the child identity (same scenario, alternative, cell, parameter, intended value, twin/version, baseline, configuration token, model/version, `SUCCEEDED`, non-null `simulation_run_id`). If that correlation row was never committed, the run is an accepted orphan: it is not deleted and MUST NOT be guessed from cell, value, timestamp, or scenario-name parsing. The next Evaluate may create a new run.

---

## 16. Scenario / evaluation lifecycle

```text
PlanningScenario
  [created] ----edit----> [same object, row_version++]
                \                         ^
                 \-- evaluate -- IN_PROGRESS --+--> SUCCEEDED
                                               +--> PARTIAL
                                               +--> FAILED

Edits after terminal: allowed. Derived view becomes STALE.
Edits during IN_PROGRESS: rejected.
Delete: not in P0.
Archive: P1.
```

No execution-governance states.

---

## 17. Traceability

Each SUCCEEDED item must expose:

scenario id · evaluation id · alternative id/name/ordinal · cell · parameter · intended value · pinned baseline · twin id · twin version · configuration fingerprint · simulation scenario id · simulation run id · model id · model version · synthetic · confidence · timestamps · `reusedExistingRun`

This is **application correlation**, not a P16/P18 tamper-evident audit chain.

Do not log credentials, tokens, or vault material (planning never touches them).

---

## 18. API architecture

Base path: `/api/v1/planning`

| Method | Path | Purpose |
|---|---|---|
| POST | `/scenarios` | Create scenario + cells + alternatives + intents |
| GET | `/scenarios` | List summaries (updated_at DESC, hard limit 100) |
| GET | `/scenarios/{id}` | Detail + derived evaluation view + latest matching evaluation |
| PATCH | `/scenarios/{id}` | Replace metadata/cells/alternatives/intents (document replace) |
| GET | `/scenarios/{id}/prerequisites` | Per-cell twin existence + freshness (no sync) |
| POST | `/scenarios/{id}/evaluations` | Evaluate / resume / idempotent return |
| GET | `/scenarios/{id}/evaluations/{evaluationId}` | Immutable evaluation + items + evidence refs |
| GET | `/scenarios/{id}/comparison` | Per-cell / per-alternative projection of the **current matching-intent** evaluation |

No nested add/remove endpoints in P0. No DELETE. No archive. No simulate-site. No comparison winner field.

**PATCH** is a full replace of planning content (name, description, cells, alternatives, intents) plus `rowVersion`. It does not delete historical evaluations.

**POST evaluations** body: `{ "createdBy": "<demo actor>", "rowVersion": n }`. No model, tool, parameter, or twin version from the client.

Status codes:

| Code | When |
|---|---|
| 200 | GET; idempotent Evaluate return |
| 201 | Create scenario; new evaluation started/completed |
| 400 | Validation, bounds, unsupported parameter, empty intents, unknown cell |
| 404 | Unknown scenario/evaluation |
| 409 | Version conflict; evaluation in progress; (optional) conflict codes |

Error body follows existing `{ "error": "..." }` plus planning `failureCode` when using `PlanningException`.

Compare DTO **must not** include: `siteScore`, `networkScore`, `combinedDelta`, `winner`, `optimum`, `bestAlternative`.

---

## 19. Frontend architecture

### Routes

| Route | Surface |
|---|---|
| `/planning` | List + create entry |
| `/planning/new` | Create form (optional `?cells=CELL-001,CELL-002`) |
| `/planning/scenarios/:scenarioId` | Detail / edit / evaluate / compare / one-cell handoff |

`/planning/scenarios` redirects to `/planning`. Do not add `/planning/simulate-site` or `/site-optimizer`.

Primary nav: add **Planning** to `AppShell` PRIMARY (with Network, Assurance, Optimization, AI).

### Workspace journey

```text
Planning
  → scenario list
  → create scenario
  → review/select bounded cells
  → create alternatives
  → edit txPower intents
  → inspect prerequisites (no auto-sync)
  → explicit Synchronize (existing twin API) if needed
  → explicit Evaluate
  → per-cell synthetic evidence
  → compare alternatives per cell
  → select ONE cell
  → existing /network/cells/:cellId/optimize
  → STOP
```

### Truthfulness (not tooltip-only)

A persistent banner on list, create, and detail:

> Independent cell-local synthetic evaluation. Not a joint site RF simulation. Neighbour coupling, interference, handover, load redistribution, and coverage propagation are not modelled. Model `snip.synthetic.cell-parameter.v1` is synthetic with LOW confidence.

Item evidence must show `synthetic`, `LOW`, model id/version, limitations, twin version.

PARTIAL must be labeled **Partially evaluated** with failed cells listed.

Historical vs current:

- **Current evaluation** — latest matching-intent evaluation
- **Historical evaluation** — previous evaluations and/or matching-intent evaluation whose pinned twins are no longer CURRENT

### Reuse

`SimulationEvidence`, partition states, `StatusBadge`, `ConfirmDialog`, comparison table patterns from PI6 (`CellComparisonTable` is investigation, not planning — do not reuse it to imply site health). New planning tables must not compute combined scores.

---

## 20. PI6 integration

PI6 remains read-only investigation.

Site/Cell pages may offer **Create what-if scenario** with a **suggested** cell set (selected cell and, optionally, same-site cells the operator can deselect). Do **not** auto-include all PI6 related cells (limit 12). Operator reviews the selection on `/planning/new` before POST.

After create, `planning_scenario_cell` is authoritative. PI6 does not update it.

---

## 21. PI3 handoff

Handoff is navigation only:

```text
/network/cells/{cellId}/optimize
```

Rules:

- exactly one cell
- no query injection of intended `txPower` as an approved or ranked value
- no `generateChangeProposal` from the planning page
- Phase 13 still generates and ranks its own candidates
- disagreement between a PI7 intended value and a Phase 13 recommendation is allowed and must not be silently reconciled

---

## 22. Governance boundary

```text
PI7 planning evidence
        |
        v
explicit operator selects ONE cell
        |
        v
existing PI3 Optimize  (Phase 13 generate/review/approve — SNIP state only)
        |
        v
(existing later path, unchanged, not started by PI7)
P14 plan → P15 sandbox → P16 grant / write gateway → P17 transport → P18 campaign
```

PI7 APIs must not create: Phase 13 proposals, Phase 14 plans, Phase 15 executions, P16 grants, P17 transport calls, P18 campaigns.

A planning object has **NO MUTATION AUTHORITY**.

---

## 23. Security / threat model

P0 uses the same demo-open pattern as Twin scenario create (`TwinController` has no permission header). **No planning-specific demo permission header.** Do not reuse:

- `X-SNIP-CHANGE-PROPOSAL-PERMISSION`
- `X-SNIP-CHANGE-PLANNING-PERMISSION` / plan headers
- `X-SNIP-CHANGE-EXECUTION-PERMISSION`
- vendor import / gateway headers

`createdBy` is demo metadata from the signed-in persona. SSO is out of scope.

| Threat | Mitigation |
|---|---|
| Arbitrary model execution | API accepts planning ids/intents only; model constants server-side |
| Arbitrary parameter injection | CHECK + `SimulatableParameterRegistry`; reject non-`txPower` |
| Out-of-range txPower | Registry + DB CHECK 20–50 |
| Cell outside scenario | Membership validation |
| Scenario tampering | PATCH replace validated; evaluations immutable; no client run ids |
| Cross-scenario IDOR | UUID; items loaded by evaluation/scenario join; demo-open same as twins (advisory) |
| Double evaluation | Fingerprints + `IN_PROGRESS` unique + run reuse |
| Replay / retry | Resume or idempotent return |
| Stale twin presented as current | Prerequisites + item pins + historical wording |
| Edit during evaluation | `409` |
| Partial failure mis-labeled success | `PARTIAL` + item outcomes required |
| Forged simulation reference | Server assigns FKs only |
| Reach P13–P18 | No service calls; tests forbid |
| Agent/MCP bypass | No MCP HTTP; typed dry-run only |
| Unbounded fan-out | Backend constants; reject before work |
| DoS via many scenarios | List cap 100; evaluate bound 16 in-process sims; advisory only |
| Fake combined scores | DTO/schema omit; tests assert absence |

---

## 24. Invariants

- **P7-I01** Planning scenario is never mutation authority.
- **P7-I02** Only `txPower` is plannable in P0.
- **P7-I03** Every evaluation child is exactly one CELL + one `txPower` intent.
- **P7-I04** Every child simulation uses the existing admitted cell-local model.
- **P7-I05** No cross-cell effects are inferred or displayed as simulated.
- **P7-I06** Every SUCCEEDED item references exact twin id/version.
- **P7-I07** Every SUCCEEDED item references exact simulation run.
- **P7-I08** Synthetic / confidence / model provenance is preserved.
- **P7-I09** Editing evaluated intents invalidates current evaluation (fingerprint `STALE`).
- **P7-I10** Historical simulation evidence is immutable; planning never updates `simulation_run`.
- **P7-I11** No combined site/network score, winner, or optimum.
- **P7-I12** PI7 cannot create P13+ governed objects automatically.
- **P7-I13** Evaluation fan-out is backend bounded; UI limits are insufficient.
- **P7-I14** Duplicate evaluation cannot create uncontrolled duplicate runs (reuse rule).
- **P7-I15** Unknown/stale/incompatible baseline cannot be presented as **current** evaluated evidence (historical + prerequisite wording required).
- **P7-I16** Evaluate never auto-synchronizes twins.
- **P7-I17** Evaluate never calls MCP HTTP or creates a Phase 4 Action.
- **P7-I18** `dryRun=true` is mandatory for every child execution.
- **P7-I19** Client cannot supply model id, tool name, twin version, or simulation run id.
- **P7-I20** `PARTIAL` must not be serialized as overall success.
- **P7-I21** No DELETE of evaluated evidence in P0.
- **P7-I22** Planning must not depend on productionchange / campaign / write-gateway packages.

---

## 25. Observability

Structured info logs / counters (application metrics, not a new platform):

- `planningScenariosCreated`
- `planningScenariosUpdated`
- `planningEvaluationsStarted`
- `planningEvaluationsSucceeded`
- `planningEvaluationsPartial`
- `planningEvaluationsFailed`
- `planningEvaluationItemsSucceeded`
- `planningEvaluationItemsFailed`
- `planningEvaluationRunsReused`
- `planningEvaluationInProgressConflicts`

Log scenario id, evaluation id, item outcome, failure code, twin id/version, simulation run id. Do not reuse P16 hash chains.

---

## 26. Performance and bounds

Backend constants (`PlanningBounds`):

| Constant | P0 value | Rationale |
|---|---|---|
| `MAX_CELLS_PER_SCENARIO` | **4** | SITE-001 has two cells; SITE-002 adds a third. Four is enough for a small site-plus-one without inheriting PI6’s 12 |
| `MAX_ALTERNATIVES_PER_SCENARIO` | **4** | Baseline is implicit; A/B/C plus one more |
| `MAX_INTENTS_PER_ALTERNATIVE` | **4** | Equals cell bound |
| `MAX_SIMULATION_RUNS_PER_EVALUATION` | **16** | `4 × 4`; reject if intent count exceeds this |
| `MAX_SCENARIO_LIST` | **100** | List page bound |

Over-limit requests are `400` **before** twin I/O beyond existence checks and **before** any `executeAdmittedCellLocalDryRun`.

Evaluate is synchronous. Sixteen in-process synthetic runs are expected to finish in seconds. No job queue.

Detail GET must load scenario + cells + alternatives + intents + latest matching evaluation + items in a bounded query set (avoid N+1 per cell). Simulation metric bodies may be fetched per SUCCEEDED run with a cap of 16.

---

## 27. Test architecture

Backend (Testcontainers PostgreSQL, existing pattern):

- CRUD, uniqueness, bounds, txPower-only, range, membership
- baseline pin + CURRENT required; STALE/EXPIRED/missing
- no auto-sync on Evaluate
- child `SimulationScenario` remains one-change
- synthetic / LOW / model provenance
- fingerprint change after edit
- idempotent Evaluate; double Evaluate; concurrent Evaluate
- PARTIAL preserves successes
- crash/resume of `IN_PROGRESS`
- run reuse; no uncontrolled duplicates
- comparison has no combined score
- no P13/P14/P15/P16+ side effects
- `executeAdmittedCellLocalDryRun` still requires dry-run/CURRENT

Frontend (Vitest):

- routes/nav, create, cell selection, alternative/intent edit
- bounds errors, prerequisites, explicit Evaluate
- synthetic/LOW/limitations copy (not tooltip-only)
- per-cell comparison; no site score; no “site simulation”
- historical vs current
- PARTIAL display
- one-cell Optimize link; no generate proposal
- no agent/MCP/prod calls

---

## 28. CI

Default CI remains external-independent. PostgreSQL via Testcontainers only. No Azure, ENM, NetAct, Ollama, or live network. No new external dependency.

---

## 29. Non-goals

No Phase 19. No coupled RF. No site Digital Twin. No joint site simulation. No interference / handover / load-redistribution / coverage models. No site or network score. No site-aware ranking. No automatic winner. No automatic Phase 13 proposal. No multi-cell proposal. No P14–P18 integration. No production execution. No closed-loop. No new AI / telecom LLM / ML/RL / Model Gateway. No new agent. No new MCP. No Nokia / Huawei / O-RAN / slicing / billing / OSS/BSS expansion.

---

## 30. Future compatibility

A later coupled simulator may attach new evidence types to the **same** planning scenario identity without changing P0 semantics. Until that model exists, PI7 language must remain independent-cell.

A site Digital Twin remains a separate architecture decision and is **not** required for PI7.

P1 candidates: archive, Ask SNIP explanation of **stored** evidence only.

---

## 31. Adversarial review findings

Reviewed after the first draft of this decision. CRITICAL/BLOCKING items were corrected **in this document** before acceptance.

| ID | Severity | Finding | Disposition |
|---|---|---|---|
| P7-A01 | CRITICAL | Intent-only idempotency would block re-evaluate after sync | Corrected: admission fingerprint + pin-aware reuse |
| P7-A02 | CRITICAL | All-or-nothing evaluate would hide independent successes | Corrected: per-intent PARTIAL |
| P7-A03 | BLOCKING | Mutating a PARTIAL row would break evaluation immutability | Corrected: new evaluation + reuse SUCCEEDED runs |
| P7-A04 | BLOCKING | Evaluate auto-sync would hide operator consent | Corrected: forbidden; explicit synchronize only |
| P7-A05 | BLOCKING | Public simulate endpoint would bypass planning bounds | Corrected: planning-scoped command + typed dry-run |
| P7-A06 | BLOCKING | Combined score fields would invent site RF | Corrected: prohibited in API/UI/tests |
| P7-A07 | BLOCKING | Client-supplied twin version / run id forges evidence | Corrected: server pins and assigns FKs |
| P7-A08 | ADVISORY | Demo-open APIs (no SSO) | Accepted; same as Twin; `DEMO_METADATA_ONLY` |
| P7-A09 | ADVISORY | Orphan `SimulationScenario` / run rows after failed correlation | Accepted; RESTRICT delete; no cleanup job in P0 |
| P7-A10 | ADVISORY | Many scenarios × 16 runs is a soft DoS | Accepted; in-process synthetic; list cap 100 |
| P7-A11 | ADVISORY | No archive/delete may accumulate demo data | Accepted; P1 archive |
| P7-A12 | ADVISORY | `electricalTilt` name exists and must stay non-plannable | Covered by P7-I02 + tests |
| P7-A13 | BLOCKING | Specification placed execution outcomes inside `admissionFingerprint`, making pre-execution SUCCEEDED lookup impossible | Corrected: admission fingerprint is pre-execution pin identity; outcomes remain evidence state |

No remaining CRITICAL or BLOCKING findings.

---

## 32. Decision

```text
ARCHITECTURE:                         ACCEPTED
OPTION:                               B_PERSISTENT_PLANNING_SCENARIO_AGGREGATE
PACKAGE:                              com.simba.snip.npo.planning
MIGRATION (implementation only):      V20__planning_what_if_scenario.sql
NEW SIMULATION MODEL:                 NO
JOINT MULTI-CELL SIMULATION:          NO
IMPLEMENTATION SPECIFICATION:         REQUIRED NEXT
IMPLEMENTATION STARTED:               NO
PHASE 19:                             NOT_STARTED
REAL PRODUCTION EXECUTION:            NOT_AUTHORIZED
```

This architecture authorizes an implementation specification. It does not authorize implementation until that specification exists and is followed.

STOP.
