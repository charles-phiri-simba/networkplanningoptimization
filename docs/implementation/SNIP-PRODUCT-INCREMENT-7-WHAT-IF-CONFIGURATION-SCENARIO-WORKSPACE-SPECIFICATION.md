# SNIP Product Increment 7 — What-If Configuration Scenario Workspace

## Implementation specification

**Status:** AUTHORIZED FOR IMPLEMENTATION (specification only; implementation not started)

**Mode:** BOUNDED_PLANNING_DOMAIN_EXTENSION

**Assurance level:** ELEVATED_PRODUCT_ASSURANCE

**Authoritative title:** What-If Configuration Scenario Workspace

**Product truth:** Independent cell-local synthetic what-if evaluation. Not joint site simulation.

Do **not** title or market PI7 as Network Planning Simulator, Site Simulator, Multi-Cell Simulator, Site Optimization, or Network Optimization.

---

## 1. Parent baseline

```text
Branch: main
Commit: 5305982c200ae55d45f94ce8397af4a1b004dae0
Product Increment 6: CLOSED
Readiness decision: BOUNDED_PLANNING_DOMAIN_REQUIRED
Selected option: B_PERSISTENT_PLANNING_SCENARIO_AGGREGATE
Architecture impact: BOUNDED_PLANNING_DOMAIN_EXTENSION
New persistence: YES
Database migration: YES
New simulation model: NO
New agent: NO
New MCP capability: NO
Production write capability: NO
Joint multi-cell simulation: NO
Site Digital Twin: NO
Site score: NO
Automatic Phase 13 proposal: NO
Phase 19: NOT STARTED
Real production execution: NOT_AUTHORIZED
```

If implementation discovers that a new simulation model, site twin, P13+ write, agent, or MCP tool is required: **STOP** and report the matching review decision. Do not expand scope silently.

---

## 2. Architecture document / hash

Authoritative architecture:

`docs/architecture/SNIP-PRODUCT-INCREMENT-7-WHAT-IF-CONFIGURATION-SCENARIO-WORKSPACE-ARCHITECTURE.md`

**Architecture SHA-256:** `64dd70dd8cbc18c82fe4724d5cdc553aea5f9b334cdb3d40f4cca46a717e9f9e`

Implement exactly that architecture. If a conflict appears, the architecture document wins for semantics; this specification wins for file/API/test exactness. Do not implement a third design.

---

## 3. Exact scope

**In scope (P0):**

- bounded `planning` domain + Flyway V20
- scenario CRUD (create, list, get, document-replace PATCH)
- cell membership, named alternatives, `txPower` intents
- prerequisites read
- Evaluate command using existing cell-local model
- immutable evaluation records + item correlation
- per-cell comparison API
- Planning frontend workspace
- PI6 suggested-cell entry
- one-cell navigation to existing Optimize
- backend + frontend tests specified here

**Out of scope:** archive/delete, revision table, SSO, planning permission header, AI, agents, MCP UI, combined scores, site twin, coupled RF, P13 generate from planning, P14–P18, Phase 19.

---

## 4. Exact packages

```text
com.simba.snip.npo.planning
com.simba.snip.npo.planning.api
com.simba.snip.npo.planning.persist
com.simba.snip.npo.planning.repository
com.simba.snip.npo.planning.service
com.simba.snip.npo.planning.config
```

Frontend feature folder: `snip-web/src/features/planning/`

Do not place planning services in `com.simba.snip.npo.twin` or `com.simba.snip.npo.api` except the listed exception-handler edit.

---

## 5. Exact files to add / modify

### 5.1 Add — backend

```text
snip-npo-app/src/main/resources/db/migration/V20__planning_what_if_scenario.sql

snip-npo-app/src/main/java/com/simba/snip/npo/planning/PlanningException.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/PlanningFailureCode.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/PlanningBounds.java

snip-npo-app/src/main/java/com/simba/snip/npo/planning/persist/PlanningScenarioEntity.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/persist/PlanningScenarioCellEntity.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/persist/PlanningAlternativeEntity.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/persist/PlanningCellIntentEntity.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/persist/PlanningEvaluationEntity.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/persist/PlanningEvaluationItemEntity.java

snip-npo-app/src/main/java/com/simba/snip/npo/planning/repository/PlanningScenarioRepository.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/repository/PlanningScenarioCellRepository.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/repository/PlanningAlternativeRepository.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/repository/PlanningCellIntentRepository.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/repository/PlanningEvaluationRepository.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/repository/PlanningEvaluationItemRepository.java

snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningController.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/CreatePlanningScenarioRequest.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/ReplacePlanningScenarioRequest.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/EvaluatePlanningScenarioRequest.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningScenarioSummaryDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningScenarioDetailDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningAlternativeDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningCellIntentDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningPrerequisiteDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningEvaluationDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningEvaluationItemDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningComparisonDto.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/api/PlanningMapper.java

snip-npo-app/src/main/java/com/simba/snip/npo/planning/service/PlanningScenarioService.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/service/PlanningEvaluationService.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/service/PlanningFingerprintService.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/service/PlanningPrerequisiteService.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/service/PlanningComparisonService.java
snip-npo-app/src/main/java/com/simba/snip/npo/planning/service/PlanningMetrics.java

snip-npo-app/src/test/java/com/simba/snip/npo/planning/PlanningApiTest.java
snip-npo-app/src/test/java/com/simba/snip/npo/planning/PlanningFingerprintServiceTest.java
snip-npo-app/src/test/java/com/simba/snip/npo/planning/PlanningEvaluationServiceTest.java
snip-npo-app/src/test/java/com/simba/snip/npo/planning/PlanningBoundsTest.java
```

### 5.2 Modify — backend (minimal)

```text
snip-npo-app/src/main/java/com/simba/snip/npo/twin/DigitalTwinSimulationService.java
  Add executeAdmittedCellLocalDryRun(UUID simulationScenarioId)
  Must delegate to the existing admission/persist path with dryRun=true and actionId=null.
  Do not change CellParameterSimulationModel formulas or SimulatableParameterRegistry.

snip-npo-app/src/main/java/com/simba/snip/npo/api/ApiExceptionHandler.java
  Map PlanningException → HTTP status by PlanningFailureCode.
```

### 5.3 Add — frontend

```text
snip-web/src/types/planning.ts
snip-web/src/features/planning/planningCopy.ts
snip-web/src/features/planning/planningGuards.ts
snip-web/src/features/planning/planningGuards.test.ts
snip-web/src/features/planning/ScenarioTruthBanner.tsx
snip-web/src/features/planning/PrerequisitePanel.tsx
snip-web/src/features/planning/AlternativeEditor.tsx
snip-web/src/features/planning/IntentEditor.tsx
snip-web/src/features/planning/EvaluationEvidencePanel.tsx
snip-web/src/features/planning/ScenarioComparisonTable.tsx
snip-web/src/pages/PlanningListPage.tsx
snip-web/src/pages/PlanningCreatePage.tsx
snip-web/src/pages/PlanningScenarioPage.tsx
snip-web/src/pages/increment7.test.tsx
```

### 5.4 Modify — frontend

```text
snip-web/src/routes/AppRoutes.tsx          add /planning, /planning/new, /planning/scenarios/:scenarioId; redirect /planning/scenarios → /planning
snip-web/src/layouts/AppShell.tsx          add Planning to PRIMARY
snip-web/src/api/snipApi.ts                planning methods only; no permission header
snip-web/src/pages/SitePage.tsx            Create what-if scenario entry
snip-web/src/pages/CellPage.tsx            Create what-if scenario entry
snip-web/src/pages/pages.test.tsx          nav + route smoke if this file asserts primary nav
```

### 5.5 Do not modify

`CellParameterSimulationModel.java`, `SimulatableParameterRegistry.java` (reuse only), Phase 13–18 packages, V1–V19 migrations, MCP tool registration, agent packages, productionchange, campaign, write gateway.

---

## 6. Exact migration file

`snip-npo-app/src/main/resources/db/migration/V20__planning_what_if_scenario.sql`

Next after `V19__phase18_production_change_campaigns.sql`. PostgreSQL. No secret columns. No JSON command column.

---

## 7. Exact schema

Implement the six tables and constraints defined in architecture section 6:

- `planning_scenario`
- `planning_scenario_cell`
- `planning_alternative`
- `planning_cell_intent`
- `planning_evaluation`
- `planning_evaluation_item`

Mandatory:

```sql
CREATE UNIQUE INDEX planning_evaluation_in_progress_uidx
    ON planning_evaluation (scenario_id)
    WHERE status = 'IN_PROGRESS';

CREATE UNIQUE INDEX planning_alternative_name_uidx
    ON planning_alternative (scenario_id, lower(name));
```

FKs to `simulation_scenario` and `simulation_run` are `ON DELETE RESTRICT`.
FKs among planning tables are `ON DELETE RESTRICT`.
No cascade delete of evidence.

CHECK `planning_cell_intent.parameter_id = 'txPower'` and `intended_value BETWEEN 20 AND 50` as defense in depth. Application validation via `SimulatableParameterRegistry` remains authoritative.

Fingerprint columns are `VARCHAR(64)` (not `CHAR(64)`) plus:

```sql
CHECK (intent_fingerprint ~ '^[0-9a-f]{64}$')
CHECK (admission_fingerprint IS NULL OR admission_fingerprint ~ '^[0-9a-f]{64}$')
```

Hibernate schema validation rejects PostgreSQL `bpchar` for these JPA `String` mappings. The CHECK is the exact SHA-256 representation invariant. Application code must also reject non-matching values via `PlanningFingerprintService.requireSha256Hex`.

---

## 8. Entities

JPA entities in `planning.persist` with static `create(...)` factories, matching Phase 14 persist style.

Statuses as strings matching CHECKs (`IN_PROGRESS`, `SUCCEEDED`, `PARTIAL`, `FAILED`, item `SUCCEEDED`/`FAILED`).

`PlanningEvaluationEntity` fields are set at create. `intentFingerprint` and pre-execution `admissionFingerprint` are stored on the `IN_PROGRESS` row. Terminal fields (`status`, `completedAt`, counts) are updated **only** by `PlanningEvaluationService` finalize/resume. Finalize MUST NOT recompute `admissionFingerprint` from item outcomes. No public setters that rewrite pins.

`PlanningEvaluationItemEntity` is insert-or-update by unique `(evaluationId, alternativeId, cellId)` during resume only.

---

## 9. Repositories

Spring Data JPA:

- `PlanningScenarioRepository.findAllByOrderByUpdatedAtDesc`
- `PlanningEvaluationRepository.findByScenario_IdOrderByCreatedAtDesc`
- `PlanningEvaluationRepository.findByScenario_IdAndStatus`
- `PlanningEvaluationItemRepository.findByEvaluation_Id`
- cell/alternative/intent finders by parent id

Add `PlanningScenarioRepository.lockById(UUID id)` using `@Lock(LockModeType.PESSIMISTIC_WRITE)` or a `@Query` `SELECT ... FROM PlanningScenarioEntity e WHERE e.id = :id` with lock. Use it only from Evaluate start.

---

## 10. DTOs

Records in `planning.api`. Required shapes (field names normative):

`CreatePlanningScenarioRequest` / `ReplacePlanningScenarioRequest`:

```text
name: String
description: String                 // optional; default ""
createdBy: String                   // create only; demo actorId
rowVersion: Integer                 // replace only; required
cells: List<{ cellId: String }>
alternatives: List<{
  name: String
  ordinal: Integer                  // 1..N contiguous
  intents: List<{
    cellId: String
    parameterId: String             // must be txPower
    intendedValue: BigDecimal / number
  }>
}>
```

`EvaluatePlanningScenarioRequest`:

```text
createdBy: String
rowVersion: Integer
```

No `modelId`, `tool`, `twinVersion`, `simulationId`, `dryRun`, `force`.

`PlanningScenarioDetailDto` includes: id, name, description, createdBy, timestamps, rowVersion, cells, alternatives/intents, `evaluationView` (`NOT_EVALUATED`|`EVALUATING`|`EVALUATED`|`PARTIAL`|`FAILED`|`STALE`), `currentEvaluationId` (matching-intent latest terminal, else in-progress id), `truth` object:

```text
truth: {
  independentCellLocal: true,
  jointSiteSimulation: false,
  crossCellEffectsModelled: false,
  synthetic: true,
  modelId: "snip.synthetic.cell-parameter.v1",
  confidence: "LOW"
}
```

`PlanningEvaluationItemDto` includes every traceability field in architecture §17.

`PlanningComparisonDto`:

```text
scenarioId
evaluationId
evaluationStatus
intentFingerprint
historical: boolean          // true when any pinned twin is not CURRENT now
rows: List<{
  alternativeId, alternativeName, alternativeOrdinal,
  cellId, parameterId,
  baselineTxPower, intendedTxPower,
  metrics: List<{ metric, baselineValue, candidateValue, delta, unit }>,
  confidence, synthetic, modelId, modelVersion,
  twinId, twinVersion, simulationRunId, outcome
}>
```

**Forbidden fields on every planning DTO:** `siteScore`, `networkScore`, `combinedDelta`, `winner`, `optimum`, `bestAlternative`, `jointSimulation`.

---

## 11. Services

| Class | Responsibility |
|---|---|
| `PlanningScenarioService` | create, list, get, replace; bounds; inventory cell existence; increment `rowVersion`; reject replace when `IN_PROGRESS` |
| `PlanningPrerequisiteService` | per-cell twin resolve + `TwinSynchronizationService.freshness`; no sync |
| `PlanningFingerprintService` | intent + admission SHA-256; unit-tested canonical strings |
| `PlanningEvaluationService` | Evaluate algorithm (architecture §10); child scenario + dry-run; reuse; finalize |
| `PlanningComparisonService` | project current matching-intent evaluation; 404 if none matching |
| `PlanningMetrics` | counters in architecture §25 |
| `PlanningMapper` | entity → DTO |

`PlanningEvaluationService` is the only class allowed to call `TwinScenarioService.create` and `DigitalTwinSimulationService.executeAdmittedCellLocalDryRun` from this increment.

---

## 12. Controllers

One controller: `PlanningController` `@RequestMapping("/api/v1/planning")`.

No permission header bind. Do not call `VendorImportAuthorizer` or proposal/plan/execution authorizers.

---

## 13. API contracts

| Method | Path | Success |
|---|---|---|
| POST | `/api/v1/planning/scenarios` | 201 `PlanningScenarioDetailDto` |
| GET | `/api/v1/planning/scenarios` | 200 list, max 100 |
| GET | `/api/v1/planning/scenarios/{id}` | 200 detail |
| PATCH | `/api/v1/planning/scenarios/{id}` | 200 detail |
| GET | `/api/v1/planning/scenarios/{id}/prerequisites` | 200 `{ scenarioId, cells: PlanningPrerequisiteDto[] }` |
| POST | `/api/v1/planning/scenarios/{id}/evaluations` | 201 new evaluation body; 200 idempotent/resume return |
| GET | `/api/v1/planning/scenarios/{id}/evaluations/{evaluationId}` | 200 |
| GET | `/api/v1/planning/scenarios/{id}/comparison` | 200 comparison of current matching-intent evaluation |

`PlanningPrerequisiteDto`: `cellId`, `twinPresent`, `twinId`, `latestVersion`, `freshness` (`CURRENT`|`STALE`|`EXPIRED`|`MISSING`), `observedTxPower` (nullable, **not pinned**), `canEvaluate`.

---

## 14. Validation

On create/replace, in this order:

1. name required, trim, length ≤ 128
2. description ≤ 1024
3. `createdBy` / demo actor via `DomainRules.requireDomainId` (≤ 64)
4. cells non-empty; unique; each exists in `cell` table; count ≤ `MAX_CELLS_PER_SCENARIO`
5. alternatives non-empty; ordinals exactly `1..N`; names unique case-insensitive; count ≤ `MAX_ALTERNATIVES_PER_SCENARIO`
6. each intent cell ∈ scenario cells; `parameterId` enabled via `SimulatableParameterRegistry.requireEnabled(..., CELL)` (must be `txPower`); `requireInRange`
7. unique `(alternative, cell)`; intents per alternative ≤ `MAX_INTENTS_PER_ALTERNATIVE`
8. total intents ≤ `MAX_SIMULATION_RUNS_PER_EVALUATION`
9. reject `electricalTilt`, `ssbPower`, `tilt`, `band`, `pci`, `arfcn`, `bandwidth` as parameters

Unknown JSON fields on records are ignored by Jackson default; do not add a catch-all `Map`.

---

## 15. Bounds

```java
public final class PlanningBounds {
    public static final int MAX_CELLS_PER_SCENARIO = 4;
    public static final int MAX_ALTERNATIVES_PER_SCENARIO = 4;
    public static final int MAX_INTENTS_PER_ALTERNATIVE = 4;
    public static final int MAX_SIMULATION_RUNS_PER_EVALUATION = 16;
    public static final int MAX_SCENARIO_LIST = 100;
}
```

Frontend `planningGuards.ts` mirrors these numbers for UX only. Backend rejection is mandatory.

---

## 16. Fingerprint algorithm

`PlanningFingerprintService` must produce lowercase 64-char SHA-256 hex.

**Intent canonicalization** (lines joined by `\n`, no trailing spaces):

```text
schema=planning.intent.v1
scenarioId=<uuid>
alternative.<ordinal>.name=<trimmed name>
alternative.<ordinal>.intent.<cellId>.parameter=txPower
alternative.<ordinal>.intent.<cellId>.intended=<plain numeric>
modelId=snip.synthetic.cell-parameter.v1
modelVersion=1.0
```

Sort alternatives by ordinal. Sort intents by `cellId`. Numeric: `stripTrailingZeros().toPlainString()`.

**Admission canonicalization** (pre-execution; no outcomes):

```text
schema=planning.admission.v1
intentFingerprint=<hex>
item.<altOrdinal>.<cellId>.twinId=<uuid or ->
item.<altOrdinal>.<cellId>.twinVersion=<int or ->
item.<altOrdinal>.<cellId>.baseline=<plain numeric or ->
item.<altOrdinal>.<cellId>.config=<configuration token or ->
item.<altOrdinal>.<cellId>.modelId=<id or ->
item.<altOrdinal>.<cellId>.modelVersion=<ver or ->
```

Items sorted by `altOrdinal`, then `cellId`. Null/blank strings are `-`. Compute `wouldBeAdmissionFingerprint` in the Evaluate start transaction from current intents + CURRENT pins. Persist it on the new evaluation immediately. Do not include `outcome`, `failureCode`, simulation ids, or metrics.

Unit tests must lock at least one golden canonical string and prove outcome text cannot change the hash.

---

## 17. Evaluation algorithm

Implement architecture §10 exactly. Pseudocode:

```text
evaluate(scenarioId, createdBy, rowVersion):
  tx1:
    scenario = lock(scenarioId)
    assert rowVersion == scenario.rowVersion
    assert no other writer
    validate persisted graph + bounds
    inProgress = find IN_PROGRESS
    if inProgress: go resume(inProgress)

    intentFp = intentFingerprint(scenario)
    pins = resolveWouldBePins(distinct cells)   // no synchronize
    wouldAdmit = admissionFingerprintPreview(intentFp, pins, intents)
    matching = latestTerminalWithIntent(intentFp)

    if matching != null and matching.admissionFingerprint == wouldAdmit:
      if matching.SUCCEEDED: return matching    // zero new runs
      else: startNewEvaluation(intentFp, wouldAdmit); reuse matching SUCCEEDED children; retry FAILED
    else if matching != null and matching.SUCCEEDED and not all pins CURRENT:
      return matching                           // historical evidence
    else:
      startNewEvaluation(intentFp, wouldAdmit); process remaining intents

  for each remaining intent in (ordinal, cellId):
    txItem: admit / reuse / create+execute / persist item

  txFinal: set counts, status, completedAt      // do not rewrite admissionFingerprint
  return evaluation
```

`resolveWouldBePins` uses `NetworkTwinRepository.findByScopeTypeAndScopeId("CELL", cellId)` and `requireCurrentForSimulation` **or** `freshness` without throwing for preview. Preview must not persist.

Child `SimulationScenario` name: `planning-{evaluationId.short}-{altOrdinal}-{cellId}` (fit VARCHAR(128)). `createdBy` = evaluation `createdBy`. `baselineTwinVersion` = pinned latest CURRENT version. `change.currentValue` = pinned baseline. `change.proposedValue` = intended. `change.parameterId` = `txPower`.

---

## 18. Idempotency algorithm

Reuse a prior SUCCEEDED `simulation_run` when a previous `planning_evaluation_item` of this scenario proves the child identity:

- same planning `scenarioId`
- same `alternativeId`
- same `cellId`, `parameterId`, intended numeric
- same `twinId` + `twinVersion`
- same pinned baseline `txPower`
- same configuration token
- same `modelId` + `modelVersion`
- item `outcome = SUCCEEDED`
- `simulation_run_id` not null

Do not infer correlation from cell only, value only, timestamps, or child scenario name. An uncorrelated orphan run remains unused.

Set `reused_existing_run = true`. Do not call execute.

Never reuse FAILED runs. Never reuse runs from another planning scenario. Never accept a client-supplied run id.

---

## 19. Transaction boundaries

| Step | Transaction |
|---|---|
| create/replace scenario + children | one `@Transactional` |
| Evaluate start / lock / insert IN_PROGRESS | short `@Transactional` |
| each item admit+execute+persist | own `@Transactional` (execute already transactional) |
| finalize | short `@Transactional` |
| GET paths | readOnly |

Do not wrap all 16 simulations in one transaction.

---

## 20. Concurrency handling

- Evaluate: pessimistic lock on `planning_scenario` + unique `IN_PROGRESS` index
- unique violation on `IN_PROGRESS` → `PlanningFailureCode.EVALUATION_IN_PROGRESS` → 409
- replace: `WHERE id AND row_version = :expected`; 0 rows → 409 `SCENARIO_VERSION_CONFLICT`
- replace if `IN_PROGRESS` exists → 409 `EVALUATION_IN_PROGRESS`
- increment `row_version` and `updated_at` on successful replace only (not on Evaluate)

---

## 21. Failure handling

`PlanningFailureCode`:

```text
SCENARIO_NOT_FOUND
EVALUATION_NOT_FOUND
CELL_UNKNOWN
CELL_NOT_IN_SCENARIO
UNSUPPORTED_PARAMETER
PARAMETER_OUT_OF_RANGE
BOUNDS_EXCEEDED
SCENARIO_VERSION_CONFLICT
EVALUATION_IN_PROGRESS
EMPTY_INTENTS
ADMISSION_TWIN_MISSING          // item-level
ADMISSION_TWIN_NOT_CURRENT      // item-level
ADMISSION_BASELINE_MISSING      // item-level
SIMULATION_REJECTED             // item-level
SIMULATION_FAILED               // item-level
```

Item-level codes persist on the item; they do not fail the HTTP Evaluate unless **zero** items could be attempted and no historical matching evaluation is returned. HTTP Evaluate still returns 200/201 with `PARTIAL`/`FAILED` body when some/all items fail after an evaluation row exists.

Create/replace use 400 for validation codes.

---

## 22. Digital Twin admission

For each item to execute (not reuse):

1. twin must exist for `(CELL, cellId)`
2. latest version ≥ 1
3. `synchronizationService.requireCurrentForSimulation(latestVersion)`
4. pin that version id/number
5. `scenarioService.baselineTxPower(version)`
6. store `version.getSourceContextVersion()` as `configuration_fingerprint`

**Never call `synchronizeCell` from planning services.**

---

## 23. Simulation invocation

```java
public Map<String, Object> executeAdmittedCellLocalDryRun(UUID simulationScenarioId) {
    return executeFromMcp(Map.of(
            "dryRun", true,
            "scenarioId", simulationScenarioId.toString()
    ));
}
```

Do not pass `actionId`. Do not accept extra client arguments. Existing admission remains: `dryRun` mandatory, CURRENT, range, baseline match.

Add a focused unit/API test that `executeAdmittedCellLocalDryRun` fails for STALE twins the same way `executeFromMcp` does.

---

## 24. Evidence correlation

SUCCEEDED item must set: twin id/version, pinned baseline, configuration fingerprint, simulation scenario id, simulation run id, model id/version from the run, `synthetic=true`, `confidence` from the run, `reusedExistingRun`.

Comparison loads `SimulationQueryService.simulation(runId)` for SUCCEEDED items only (≤ 16). Do not invent metrics.

FAILED items appear in comparison with `outcome=FAILED`, null metrics, failure code — never with guessed KPIs.

---

## 25. Frontend routes

```text
/planning                              PlanningListPage
/planning/new                          PlanningCreatePage   ?cells=CELL-001,CELL-002
/planning/scenarios/:scenarioId        PlanningScenarioPage
/planning/scenarios                    Navigate → /planning
```

`AppShell` PRIMARY insert Planning after Network (or after Optimization). Do not enable Changes/Campaigns.

---

## 26. Frontend components

| Component | Duty |
|---|---|
| `ScenarioTruthBanner` | architecture §19 wording; visible on list, create, detail — not tooltip-only |
| `PrerequisitePanel` | freshness per cell; Synchronize button calls `snipApi.synchronizeCellTwin`; does **not** Evaluate |
| `AlternativeEditor` | names/ordinals; max 4 |
| `IntentEditor` | txPower only; 20–50; cells from membership |
| `EvaluationEvidencePanel` | per-item evidence; reuse `SimulationEvidence` for SUCCEEDED runs; show synthetic/LOW/limitations/twin version |
| `ScenarioComparisonTable` | per-cell rows; no totals; no winner |
| `planningCopy.ts` | forbidden-phrase helpers |
| `planningGuards.ts` | client bounds + one-cell handoff href |

Handoff control: “Open Optimize for this cell” → `/network/cells/${cellId}/optimize`. Single selection. No generate button on planning pages.

---

## 27. UI truthfulness

Required visible strings (or equivalent exact meaning):

- Independent cell-local synthetic evaluation
- Not a joint site RF simulation
- Neighbour coupling is not modelled
- Interference is not modelled
- Handover is not modelled
- Load redistribution is not modelled
- Coverage propagation is not modelled
- Synthetic model confidence is LOW

Forbidden UI strings: `site simulation`, `site score`, `network score`, `best site configuration`, `joint simulation`, `multi-cell simulation` (as a capability claim), `winning alternative`.

`PARTIAL` label: **Partially evaluated**. List failed cells.

If matching-intent evaluation exists and any pinned twin is not CURRENT: show **Historical evaluation** and block implying it reflects the live twin. Prerequisites explain re-sync then Evaluate.

Display pre-evaluate txPower as **Current observed (not pinned)**.

---

## 28. PI6 entry

`SitePage`: button/link “Create what-if scenario” → `/planning/new?cells=` up to 4 site cells, operator-reviewable.

`CellPage`: same with selected cell first; may include same-site cells up to the bound; **must not** dump all PI6 related cells (12).

Create page shows checkboxes; POST only selected cells.

---

## 29. PI3 handoff

Only `Link` / `navigate` to `/network/cells/:cellId/optimize`.

`increment7.test.tsx` must assert planning pages do **not** call `snipApi.generateChangeProposal`.

Phase 13 remains unchanged.

---

## 30. Backend tests

`PlanningApiTest` (Testcontainers, style of `DigitalTwinApiTest`):

1. create / get / list / replace happy path
2. bounds: 5th cell, 5th alternative, 17th intent → 400
3. non-txPower parameter → 400
4. txPower 19 and 51 → 400
5. unknown cell → 400
6. intent cell not in membership → 400
7. duplicate alternative name / ordinal → 400
8. Evaluate pins CURRENT twin version and baseline 46 for CELL-001
9. STALE twin: item FAILED `ADMISSION_TWIN_NOT_CURRENT`; no `synchronize` side effect
10. missing twin: admission failure
11. two cells independent: CELL-001 46→44 and CELL-002 43→45 produce two runs, different `twinId`
12. child `SimulationScenario` still one change
13. run `synthetic=true`, `confidence=LOW`, model id/version exact
14. PATCH intents after SUCCEEDED → `evaluationView=STALE`; old evaluation GET unchanged
15. second Evaluate same pins → same evaluation id; run count unchanged
16. concurrent Evaluate → one 201/200, other 409 or same id
17. PARTIAL: succeed one, fail one; successes persist
18. resume `IN_PROGRESS` (simulate by inserting/leaving in-progress if testable)
19. reuse run after PARTIAL retry; `reusedExistingRun=true`
20. comparison rows exist; JSON has no `siteScore`/`winner`/`combinedDelta`
21. Evaluate does not insert `network_change_proposal`, plan, execution, grant, campaign
22. `executeAdmittedCellLocalDryRun` STALE fail-closed
23. replace during IN_PROGRESS → 409
24. no DELETE mapping (404/405)

`PlanningFingerprintServiceTest`: golden intent/admission canonical; order independence; edit changes intent hash; model/twin version change admission hash; outcome text is absent from admission canonical; SHA-256 hex invariant.

Additional `PlanningApiTest` cases:

25. same intent + same pins + SUCCEEDED → same evaluation id and `admissionFingerprint`; run count unchanged
26. same intent + new CURRENT twin version → new evaluation and new `admissionFingerprint`
27. same intent + same pins + PARTIAL → old PARTIAL row unchanged; new evaluation; SUCCEEDED child reused
28. client-supplied fingerprint / simulation id fields are ignored; server assigns fingerprints and run ids

`PlanningBoundsTest`: constants exact.

---

## 31. Frontend tests

`increment7.test.tsx` and `planningGuards.test.ts`:

1. `/planning` list empty and populated
2. create form + cell selection from query
3. alternative / txPower edit
4. client bound messages
5. prerequisite STALE shows Synchronize, not hidden Evaluate-as-sync
6. Evaluate button explicit
7. truth banner + LOW + limitations visible
8. comparison per cell; no combined score node
9. historical vs current copy
10. PARTIAL copy
11. one Optimize link; no generate
12. forbidden phrases absent
13. `snipApi` planning methods used; no agent-runs, no MCP, no execution/grant URLs from planning pages
14. Site/Cell entry links present
15. nav contains Planning

---

## 32. Browser acceptance

Design only in this increment’s implementation phase; do not execute during specification.

```text
Demo Login
  → Planning
  → Create What-If Scenario
  → select SITE-001 cells CELL-001, CELL-002 (review selection)
  → Alternative A: CELL-001 46 → 44; CELL-002 43 → 45
  → inspect prerequisites
  → if twin missing/STALE: explicit Synchronize per cell
  → do not auto-Evaluate after sync
  → explicit Evaluate
  → observe independent CELL-001 evidence (synthetic, LOW, limitations, twin version)
  → observe independent CELL-002 evidence
  → compare alternatives per cell (no site score)
  → select CELL-001
  → existing /network/cells/CELL-001/optimize
  → STOP
```

Do not generate a PI3 proposal. Do not open sandbox. Do not invoke production.

Optional second alternative B on the same scenario is in-scope if time allows; not required for the minimum journey.

---

## 33. Qualification

Before freeze:

1. `npm ci` (snip-web)
2. `npm test`
3. `npm run build`
4. `mvn -B test`
5. `go test ./...` (simulator)
6. browser acceptance of §32
7. confirm no V1–V19 edits; only V20 added
8. confirm `CellParameterSimulationModel` diff empty
9. confirm planning package has no import of `productionchange`, `productioncampaign`, agent run APIs

Default CI must stay vendor/Azure/Ollama independent.

---

## 34. Freeze process

```text
Specification (this document)
  → implementation
  → implementation review
  → browser acceptance
  → qualification
  → one baseline commit
  → normal push
  → exact-SHA CI verification
  → PI7 CLOSED
```

Do not amend PI6 (`5305982c…`). Do not rewrite history. Do not start Phase 19. Do not commit this specification in the specification task.

---

## 35. Prohibited changes

- new simulation model or formula change
- site Digital Twin / `TwinScopeType` extension
- public `POST /api/v1/simulate`
- new MCP tool or MCP UI
- new agent or `/agent-runs`
- planning → generate/approve proposal
- planning → P14/P15/P16/P17/P18
- combined site/network score
- auto-synchronize on Evaluate
- auto-evaluate after synchronize
- plannable `ssbPower` / tilt / PCI / band / ARFCN / bandwidth
- cascade delete of `simulation_run`
- reuse of Phase 13 proposal ids as planning ids
- SSO / production IAM
- planning permission header that reuses write/approve/execute permissions
- Changes/Campaigns nav enablement

---

## Decision after specification

Implementers may report only:

`READY_FOR_PI7_IMPLEMENTATION_REVIEW`  
`PI7_IMPLEMENTATION_REQUIRES_CORRECTION`  
`DIGITAL_TWIN_BOUNDARY_REVIEW_REQUIRED`  
`SIMULATION_AUTHORITY_REVIEW_REQUIRED`  
`SECURITY_BOUNDARY_REVIEW_REQUIRED`  
`INSUFFICIENT_EVIDENCE`

Do not start Phase 19.

STOP.
