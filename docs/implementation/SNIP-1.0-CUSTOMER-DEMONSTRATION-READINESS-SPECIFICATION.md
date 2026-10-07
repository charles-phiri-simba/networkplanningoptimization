# SNIP 1.0 — Customer Demonstration Readiness

## Implementation specification

**Status:** CORRECTED FOR IMPLEMENTATION REVIEW (OQ-1 closed; reset ownership hardened). Specification only; implementation not started.

**Mode:** RELEASE_HARDENING

**Assurance level:** ELEVATED_PRODUCT_ASSURANCE

**Authoritative title:** SNIP 1.0 — Customer Demonstration Readiness

**Milestone type:** Release-hardening. **Not** Product Increment 8. **Not** Phase 19. **Not** production readiness.

**Product truth:** Controlled synthetic customer demonstration of the already-implemented SNIP 1.0 journey. Real production execution remains not authorized.

Do **not** title or market this milestone as PI8, Phase 19, Production Readiness, Production Deployment, or Customer Production Pilot.

---

## 1. Document Control

| Field | Value |
|-------|--------|
| Document | `docs/implementation/SNIP-1.0-CUSTOMER-DEMONSTRATION-READINESS-SPECIFICATION.md` |
| Kind | Implementation specification |
| Parent product baseline | `f0f983db75a25957db457e83ab43abfb390375e9` |
| Product Increment 7 | CLOSED |
| PI7 architecture SHA-256 | `348be12183d7624afe25aa1ec256925364655da215f8a6bf2e8b1357dbc4193b` |
| PI7 specification SHA-256 | `4decff69f75e8db87405f5f6c9a1e06fe503c09ee0a07e3314b1fd321c6c7ea5` |
| SNIP 1.0 review decision | `SNIP_1_0_READY_FOR_RELEASE_HARDENING` |
| PI8 | NOT_STARTED |
| Phase 19 | NOT_STARTED |
| Specification committed | NO |
| Implementation started | NO |
| OQ-1 | **RESOLVED** — existing ENM simulator FULL import (`CELL-SIM-001` overlay; source-scoped knowledge) |
| OQ-2 | **RESOLVED** — script + existing GET APIs; **no** `/api/v1/demo/readiness` |

If implementation discovers that a new bounded context, coupled RF model, site twin, production write path, agent UI, MCP UI, or Phase 19 is required: **STOP**. This specification is then wrong.

---

## 2. Purpose

Product Increments 1A–7 already implement a coherent customer journey.

This specification defines the **minimum remaining work** so a technical presenter who did not write SNIP can, on a clean supported machine, start SNIP, reset SNIP, and run a truthful 30–45 minute customer demonstration **without SQL, curl, or tribal knowledge**.

It does **not** add a new product capability.

---

## 3. Authoritative Baseline

```text
Branch: main
HEAD: f0f983db75a25957db457e83ab43abfb390375e9
origin/main: f0f983db75a25957db457e83ab43abfb390375e9
Working tree at authoring start: CLEAN
PI7 commit subject: feat: add what-if configuration scenario workspace
PI7 CI: 37613490936 SUCCESS
```

Implementation must start from this SHA (or a later commit that contains only this specification and then this implementation). Do not rebase PI7.

---

## 4. Milestone Classification

| Question | Answer |
|----------|--------|
| New product increment? | **NO** |
| New architecture? | **NO** |
| New bounded context? | **NO** |
| New Flyway version allowed? | **YES** — one forward-only seed migration |
| New public mutation API? | **NO** |
| Production defaults may flip to enabled? | **NO** |
| Kafka required for the customer demo? | **NO** |
| Azure / Key Vault required? | **NO** |

---

## 5. Current Product State

Verified at `f0f983db…`:

- Customer UI: `snip-web` React 19 / Vite / React Router 7 at `http://127.0.0.1:5173`
- Backend: `snip-npo-app` Spring Boot, default `127.0.0.1:8080`
- Demo inventory: `V2__seed_demo_network.sql` — 2 sites, 2 gNBs, 3 cells, 3 neighbours, 10 synthetic KPIs
- Assurance: `DegradingRadioQualityDetector` requires `BLER_DL` ≥ 0.08 **and** `Trend.INCREASING`; Flyway does **not** insert `assurance_case`
- Kafka default: `snip.kafka-enabled: false`
- Simulator default time: fixed `2026-08-24T10:00:00Z` (can fall outside `recent-kpi-hours: 168`)
- Twins: explicit `POST /api/v1/twins/cells/{cellId}/synchronize`; no startup auto-sync
- Optimization: `NetworkChangeProposalGenerationService` + `KnowledgeGate` — LOW/UNKNOWN knowledge yields `EVALUATED`, not `RECOMMENDED`
- Change planning: `snip.change-planning.require-high-or-medium-knowledge: true`
- Sandbox: `snip.change-execution.enabled: false`; permitted target `SIMULATOR` only
- Production: `snip.production-change.enabled: false`, `global-execution-enabled: false`
- ENM: `implementation-type: SIMULATOR`; production transport remains unconfigured
- Phase 12 scheduler: `snip.enm.sync.scheduler-enabled: true`, cadence `15m` — **not** an acceptable demo wait
- No `scripts/` directory
- Phase 19: not started

---

## 6. Repository Evidence

| Topic | Evidence |
|-------|----------|
| Routes | `snip-web/src/routes/AppRoutes.tsx` |
| Nav | `snip-web/src/layouts/AppShell.tsx` |
| Demo personas | `snip-web/src/features/auth/demoIdentity.ts` |
| Seed | `snip-npo-app/src/main/resources/db/migration/V2__seed_demo_network.sql` |
| Latest migration | `V20__planning_what_if_scenario.sql` |
| Assurance schema | `V4__assurance_case.sql` — `case_type` CHECK `DEGRADING_RADIO_QUALITY` only |
| Detector | `DegradingRadioQualityDetector`, `TrendClassifier` (needs ≥2 points; first vs last) |
| Detection entry | `AssuranceDetectionService.evaluateCell` — invoked after telemetry projection, **not** after Flyway |
| Twin sync | `TwinSynchronizationService`; UI `snipApi.synchronizeCellTwin` |
| Knowledge | `NetworkKnowledgeStatusEntity.initial` → `UNKNOWN` / `NO_TRUSTED_BASELINE`; `KnowledgeGate` allows `RECOMMENDED` only for HIGH or MEDIUM |
| Trusted baseline (tests) | `ChangeIntelligenceApiTest.runTrustedBaseline`: `scenarios.use(FULL_SUCCESS)` + `VendorImportAuthorizer.runWith(PERMISSION)` + `SynchronizationControlPlane.triggerManual(ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)` |
| Default ENM simulator inventory | `SimulatorEnmTransport` FULL/`SUCCESS_SINGLE_PAGE` page-1 canonical IDs **`SITE-SIM-001`**, **`GNB-SIM-001`**, **`CELL-SIM-001`** — **not** `CELL-001` |
| JSON Ericsson fixture | `integration/ericsson/normal.json` uses `SITE-E001`/`CELL-E001`; **not** the Phase 12 control-plane path |
| Reconciliation MISSING | `NetworkReconciliationService.plan` lines 92–98: complete snapshot marks **ACTIVE same-source** references not in the snapshot; `markMissing` does **not** delete `cell` rows |
| Knowledge scope | `network_knowledge_status` is **source+scope**, not cell-id. CELL-001 need not appear in the ENM snapshot |
| Sandbox flag | `ChangeExecutionProperties.enabled` default false; `application.yml` `snip.change-execution.enabled: false` |
| Compose | `docker-compose.yml` — postgres + api; kafka/simulator profile `telemetry` |
| CI | `.github/workflows/ci.yml` — Go, Maven, frontend test, frontend build |
| Jargon | `LoginPage.tsx` Increment 1A; `PlanningScenarioPage.tsx` “PI3 Optimize handoff”; `AssuranceCasePage.tsx` “PI3 workflow”; `NotFoundPage.tsx` “SNIP increment”; `AppShell.tsx` “Later increment”; `AiPage.tsx` raw `POST /api/v1/recommendations`; `ChangePlanPage.tsx` “Backend status remains READY_FOR_EXECUTION” |
| Four-way | `snip-web/src/features/sandboxExecution/FourWayState.tsx` — Real network Unchanged |
| Planning truth | `snip-web/src/features/planning/planningCopy.ts` |
| Seed-without-trend test | `AssuranceDetectionTest.seedCell001DoesNotCreateCaseWithoutIncreasingTrend` — **must be rewritten** when CELL-001 gains an increasing series |

---

## 7. Problem Statement

A presenter following the current README can start Postgres + API + Vite and see a **3-cell inventory with an empty Assurance queue**, **no CURRENT twins**, **no HIGH/MEDIUM knowledge**, and **sandbox disabled**. The documented telemetry path needs Kafka and a simulator clock that is already stale relative to `recent-kpi-hours`. Repeat demos accumulate planning/proposal/plan/execution rows with no productized reset.

The software journey exists. The **demonstration environment does not**.

---

## 8. Goals

1. Deterministic **bootstrap** to a known customer-demo state without SQL or curl.
2. Deterministic **reset** that can be repeated: RESET → BOOTSTRAP → DEMO → RESET → BOOTSTRAP → DEMO.
3. Credible **synthetic** network scale (section 16) with **three** curated stories.
4. Featured Assurance finding produced by the **real detector**, calendar-independent.
5. Featured twins CURRENT and featured knowledge HIGH or MEDIUM so Optimize can reach `RECOMMENDED`.
6. Sandbox enabled **only** via explicit demo runtime; committed production-safety defaults unchanged.
7. Customer-visible increment/phase jargon removed from the demo path.
8. Authoritative UI is Vite `:5173`. Documentation cannot send presenters to the legacy `:8080` static page as the 1.0 demo.
9. Customer documents: overview, demo guide, limitations/truth.
10. Automated qualification + a specified browser acceptance test (browser test is release acceptance, not every-commit CI).

---

## 9. Non-Goals

PI8; Phase 19; real Ericsson/Nokia/Huawei production connectivity or writes; production credentials; production campaigns; closed-loop; agent or MCP UI; new AI/agent/MCP architecture; site or network Digital Twin; coupled RF; interference/coverage/HO/load/tilt models; new simulatable parameters; vendor-calibrated RF; BSS/billing/CEM; nationwide scale; production SSO; customer IAM; Oracle adapters; Kubernetes production deployment; Kafka as a demo dependency; Azure login on the presenter laptop; a public DELETE-ALL REST API.

---

## 10. Safety Invariants

Implementation **must** preserve:

| Invariant | Rule |
|-----------|------|
| `snip.production-change.enabled` | `false` in committed `application.yml` **and** in `application-demo.yml` |
| `snip.production-change.global-execution-enabled` | `false` |
| `snip.change-execution.enabled` | `false` in committed `application.yml`; `true` **only** in demo profile / demo runtime |
| `snip.change-execution.permitted-target-types` | `SIMULATOR` only |
| `snip.change-execution.automatic-rollback-enabled` | `false` |
| Kafka | remains default `false`; demo profile must **not** set `SNIP_KAFKA_ENABLED=true` |
| Production ENM | must not be configured; do not set production transport |
| Demo runner | `@Profile("demo")` only; ordinary tests and default `spring-boot:run` must not run it |
| Reset | local scripts + explicit env guards; **no** public reset REST endpoint |
| Azure | no `az login`, no Key Vault, no workload identity required for demo |

If demo profile starts with `production-change.enabled=true`, the process **must fail closed** at startup.

---

## 11. Product Truth Invariants

Do not remove or hide:

- `synthetic` / `DEMO_SEED` / demo-environment banners
- simulation **LOW** confidence and **not vendor-calibrated**
- `PLANNING_TRUTH` sentences in `planningCopy.ts`
- sandbox banners: `SANDBOX ONLY · SIMULATOR · NO REAL NETWORK CHANGE`
- `FourWayState` Real network **Unchanged**
- Ask SNIP “decision support only”
- missing-evidence / not-fabricated Assurance copy

Customer UI must keep distinguishable:

OBSERVED/SEEDED · DERIVED · SYNTHETIC · AI-ASSISTED INTERPRETATION · DETERMINISTIC RULE · CELL DIGITAL TWIN · SANDBOX · CANONICAL SNIP · REAL NETWORK · UNKNOWN/MISSING.

---

## 12. Customer Demonstration Target

- Duration: **30–45 minutes**
- Audience: RF / optimization / NOC-assurance / planning engineers; OSS/security architects; operations managers; technology executives
- Environment: controlled synthetic SNIP demo
- Next commercial stage: **read-only customer PoC**
- Not: production write integration

Authoritative customer UI: **`http://127.0.0.1:5173`**.

---

## 13. Existing Customer Journey

Do **not** redesign. Harden this existing chain:

`/login` → `/network` (map + queue) → `/assurance/:caseId` → `/network/cells/CELL-001` → site/related → `/planning/new?cells=CELL-001,CELL-002` → evaluate/compare → `/network/cells/CELL-001/optimize` → `/optimization/proposals/:id` → `/change-plans/:id` → `/sandbox/executions/:id` → verify → Four-Way State → real network unchanged.

Planning must **not** auto-generate a Phase 13 proposal (`increment7.test.tsx` contract remains).

---

## 14. Scope Summary

| Stream | In scope |
|--------|----------|
| Data | Forward-only `V21` synthetic inventory + NOW-relative KPI series |
| Runtime | Spring profile `demo` + `DemoBootstrapRunner` |
| Tooling | Local `scripts/snip-demo-up` and `scripts/snip-demo-reset` (+ ready check) |
| UX | Terminology, guidance, hide Changes/Campaigns, comparison errors, login/AI leaks |
| Docs | Three customer docs + README / snip-web README / implementation-status updates |
| Tests | Dataset, bootstrap, safety flags, frontend jargon, rewrite broken seed test |
| Out | New domains, Kafka demo path, production flags, PI8, Phase 19 |

---

## 15. Demo Data Architecture

**Decision: C — hybrid.**

| Layer | Mechanism | Persistence |
|-------|-----------|-------------|
| Stable synthetic network | Flyway `V21` INSERT (do **not** edit V1–V20) | Durable inventory, radio, neighbours, KPI series |
| Assurance cases | Runtime: `AssuranceDetectionService.evaluateCell` on featured cells | Durable after first demo-profile start; recreated after reset+bootstrap |
| Cell twins | Runtime: existing `TwinSynchronizationService.synchronize` for featured cells | Durable versions; recreated after reset |
| Knowledge eligibility | Runtime: **mandatory** existing `SynchronizationControlPlane.triggerManual(ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)` under `VendorImportAuthorizer.PERMISSION`, same path as `ChangeIntelligenceApiTest.runTrustedBaseline` | Durable checkpoint + `network_knowledge_status`; may **CREATE** overlay `SITE-SIM-001` / `GNB-SIM-001` / `CELL-SIM-001` |
| Planning / proposals / plans / executions | **Empty** after bootstrap; presenter creates live in UI | Ephemeral demo workflow |

Do **not** INSERT `assurance_case`, `network_change_proposal`, `network_change_plan`, `network_change_execution`, or `planning_*` rows in V21.

Do **not** hard-code terminal evaluations, approved proposals, or verified executions.

Do **not** SQL-update `network_knowledge_status` to HIGH. Knowledge is established only by the existing control-plane import (section 27.5).

---

## 16. Exact Demo Dataset

Preserve V2 featured identities. V21 **adds** inventory. Totals after V21:

| Entity | Count | IDs |
|--------|------:|-----|
| Sites | **6** | `SITE-001` … `SITE-006` |
| gNBs | **6** | `GNB-001` … `GNB-006` (one per site) |
| Cells | **18** | `CELL-001` … `CELL-018` |
| Neighbour relations | **36–42** | intra-site pairs + selected inter-site INTER_FREQUENCY |
| Radio `txPower` | 18 (one per cell) | integer **20–50** dBm |
| KPI observations | V2 10 + V21 series | all `source='DEMO_SEED'`, `synthetic=TRUE` |
| Seeded Assurance cases | **0** in SQL | 4 created at demo bootstrap |
| Seeded planning/proposals/plans | **0** | |

### 16.1 Sites (V2 retained; V21 adds 003–006)

| site_id | name | latitude | longitude | status |
|---------|------|----------|-----------|--------|
| SITE-001 | Midband Demo Site | -26.2041 | 28.0473 | ACTIVE |
| SITE-002 | Comparison Demo Site | -26.1950 | 28.0340 | ACTIVE |
| SITE-003 | Demo Site North | -26.1880 | 28.0410 | ACTIVE |
| SITE-004 | Demo Site East | -26.2010 | 28.0580 | ACTIVE |
| SITE-005 | Demo Site West | -26.2100 | 28.0280 | ACTIVE |
| SITE-006 | Demo Site South | -26.2180 | 28.0450 | ACTIVE |

Cluster remains a **fictional Johannesburg-area lab cluster**. Map positions are demonstration locations, not RF planning evidence.

### 16.2 gNBs

| gnb_id | site_id | name | vendor | model |
|--------|---------|------|--------|-------|
| GNB-001 | SITE-001 | Demo gNB Midband | DemoVendor | SNIP-RAN-1 |
| GNB-002 | SITE-002 | Demo gNB Comparison | DemoVendor | SNIP-RAN-1 |
| GNB-003 | SITE-003 | Demo gNB North | DemoVendor | SNIP-RAN-1 |
| GNB-004 | SITE-004 | Demo gNB East | DemoVendor | SNIP-RAN-1 |
| GNB-005 | SITE-005 | Demo gNB West | DemoVendor | SNIP-RAN-1 |
| GNB-006 | SITE-006 | Demo gNB South | DemoVendor | SNIP-RAN-1 |

Vendor string **must** remain `DemoVendor`. Do not use Ericsson/Nokia as inventory vendor labels on these rows (ENM simulator import is a separate source).

### 16.3 Cells

| cell_id | site | name | band | role |
|---------|------|------|------|------|
| CELL-001 | SITE-001 | n78-1 high-BLER demo | n78 | **Story 1 featured** |
| CELL-002 | SITE-001 | n78-2 healthier demo | n78 | **Story 2 healthy contrast** |
| CELL-003 | SITE-002 | n41-1 comparison demo | n41 | **Story 3 inter-frequency** |
| CELL-004 | SITE-001 | n78-3 demo | n78 | inventory |
| CELL-005 | SITE-002 | n41-2 demo | n41 | inventory |
| CELL-006 | SITE-002 | n41-3 demo | n41 | inventory |
| CELL-007 | SITE-003 | n78-1 major-BLER demo | n78 | **queue MAJOR** |
| CELL-008 | SITE-003 | n78-2 demo | n78 | inventory |
| CELL-009 | SITE-003 | n41-1 demo | n41 | inventory |
| CELL-010 | SITE-004 | n78-1 warning-BLER demo | n78 | **queue WARNING** |
| CELL-011 | SITE-004 | n78-2 demo | n78 | inventory |
| CELL-012 | SITE-004 | n41-1 demo | n41 | inventory |
| CELL-013 | SITE-005 | n78-1 demo | n78 | inventory |
| CELL-014 | SITE-005 | n78-2 major-BLER demo | n78 | **queue MAJOR** |
| CELL-015 | SITE-005 | n41-1 demo | n41 | inventory |
| CELL-016 | SITE-006 | n78-1 demo | n78 | inventory |
| CELL-017 | SITE-006 | n78-2 demo | n78 | inventory |
| CELL-018 | SITE-006 | n41-1 demo | n41 | inventory |

Technology `NR`, duplex `TDD`, status `ACTIVE`. PCI/ARFCN must be unique-enough for display; do not copy real operator PCI plans.

CELL-001 `txPower` **must remain `46`** (V2). Other featured: CELL-002 `43`, CELL-003 `40`. Additional cells: `40`–`46` inclusive.

### 16.4 Neighbours

Retain V2 three relations. V21 adds relations so every site has at least one intra-frequency pair and CELL-001 retains INTER_FREQUENCY to CELL-003. Total **36–42**. No self-relations. `status='ACTIVE'`.

### 16.5 Persistence IDs

Continue V2’s deterministic UUID pattern `00000000-0000-4000-a000-…`. Customer-visible labels are `SITE-*` / `GNB-*` / `CELL-*` / names above — not raw UUIDs.

### 16.6 Provenance

Every V21 `kpi_observation` row: `source='DEMO_SEED'`, `synthetic=TRUE`. Radio rows need no vendor secret fields.

### 16.7 Post-bootstrap ENM simulator overlay (expected; not V21)

V21 remains **exactly 6/6/18**. After the mandatory knowledge import (section 27.5), the canonical graph **also** contains the existing ENM simulator page-1 identities:

| ID | Origin |
|----|--------|
| `SITE-SIM-001` | `SimulatorEnmTransport` FULL page-1 (`"Sim Site"`) |
| `GNB-SIM-001` | same |
| `CELL-SIM-001` | same (`configuredMaxTxPower` 460 → 46 dBm) |

These IDs **do not collide** with `SITE-001`…`SITE-006` / `CELL-001`…`CELL-018`.

After bootstrap, totals are therefore **at least** 7 sites / 7 gNBs / 19 cells. The three curated stories **must** use only V21 featured IDs. The overlay site may appear on the map; that is truthful simulator inventory, not a defect. Do **not** hide it. Do **not** shrink V21 to absorb it.

Ready checks must assert **V21 identities present**, not `COUNT(site)=6` after import.

---

## 17. Demo Naming / Geography

- Prefixes `SITE-` `GNB-` `CELL-` plus short “demo” names.
- No real operator, customer, employer, or internal hostname in names.
- Coordinates: expand the existing fictional cluster (section 16.1). Do **not** relocate to another continent.
- Map copy (add once, Network page or map legend): “Synthetic demonstration locations. Not RF coverage or planning evidence.”

---

## 18. KPI Dataset

Use **only** metrics the product already understands: `BLER_DL`, `BLER_UL`, `DROP_RATE`, `THROUGHPUT_DL`, `LATENCY`, `PRB_UTILIZATION_DL`.

All timestamps: `NOW() - INTERVAL '…'` so they remain inside `recent-kpi-hours: 168` on any calendar date. Do **not** use absolute timestamps.

`TrendClassifier` uses first vs last of the context series (≥2 points).

### 18.1 CELL-001 (featured)

| Metric | Points | Approximate series (oldest → newest) | Trend | Notes |
|--------|-------:|--------------------------------------|-------|-------|
| BLER_DL | 8 | 0.04, 0.05, 0.06, 0.07, 0.09, 0.10, 0.11, **0.12** | INCREASING | last ≥ 0.12 |
| PRB_UTILIZATION_DL | 8 | 0.55, 0.60, 0.64, 0.68, 0.72, 0.76, 0.80, **0.82** | INCREASING | enables CRITICAL + HIGH |
| THROUGHPUT_DL | ≥2 | descending toward ~42 Mbps | optional contrast | |
| others | keep/extend V2 | — | — | remain synthetic |

Do not DELETE V2 rows. V21 INSERTs additional older points. Newest featured values must match V2 current (0.12 / 0.82) so the cell does not “jump” after migrate.

### 18.2 CELL-002 (healthy)

| Metric | Points | Series | Trend |
|--------|-------:|--------|-------|
| BLER_DL | 6 | ~0.008 ± 0.001 | STABLE (not INCREASING) |
| PRB_UTILIZATION_DL | 6 | ~0.41 | STABLE |
| THROUGHPUT_DL | ≥2 | ~180 | — |

Must **not** satisfy the detector.

### 18.3 CELL-003 (inter-frequency context)

Low stable `BLER_DL` (~0.006). No case.

### 18.4 Queue richness (same detector, not new types)

| Cell | BLER_DL last | PRB trend | Expected severity after evaluate |
|------|--------------|-----------|----------------------------------|
| CELL-007 | 0.11 | STABLE or absent | **MAJOR** (0.10 ≤ bler < 0.12 or 0.12 without PRB↑) |
| CELL-010 | 0.09 | STABLE | **WARNING** |
| CELL-014 | 0.11 | STABLE | **MAJOR** |

Each needs ≥2 BLER points with last > first and last ≥ threshold.

### 18.5 Remaining cells

2–3 recent healthy BLER/PRB points so cell pages are not empty. No increasing high BLER.

---

## 19. Assurance Story Design

| Story cell | case_type | severity | confidence | status | rule |
|------------|-----------|----------|------------|--------|------|
| CELL-001 | `DEGRADING_RADIO_QUALITY` | **CRITICAL** | **HIGH** | OPEN | existing detector |
| CELL-007 | same | MAJOR | MEDIUM (no PRB↑) | OPEN | same |
| CELL-010 | same | WARNING | MEDIUM or LOW | OPEN | same |
| CELL-014 | same | MAJOR | MEDIUM | OPEN | same |
| CELL-002 / CELL-003 | none | — | — | — | healthy |

No new detector. No invented case types. Unique index `assurance_case_active_uk` makes re-evaluate idempotent.

Evidence must come from the detector (`EVIDENCE_THRESHOLD`, `EVIDENCE_TREND`, optional `EVIDENCE_CORRELATED_KPI`), not from hand-written SQL evidence rows.

---

## 20. Curated Demo Story 1

**Title:** High DL BLER on CELL-001.

**Spine:** Network queue → CRITICAL case → investigation → CELL-001 → SITE-001 / CELL-002 contrast → what-if CELL-001+CELL-002 → Evaluate → Optimize CELL-001 → approve → plan → sandbox → Four-Way Unchanged.

**Eligibility the bootstrap must guarantee:**

- OPEN CRITICAL case on CELL-001
- CURRENT twin on CELL-001 (and CELL-002 for planning)
- Knowledge HIGH or MEDIUM for the enabled sync policy
- `txPower=46` on CELL-001 (candidates can step within 20–50 / max-delta 4)
- `snip.change-execution.enabled=true` via demo profile only

---

## 21. Curated Demo Story 2

**Title:** Healthy neighbour contrast.

CELL-002 on the same site, intra-frequency neighbour of CELL-001, stable low BLER, **no** Assurance case. Message: SNIP does not mark every cell unhealthy. Do **not** claim interference diagnosis.

---

## 22. Curated Demo Story 3

**Title:** Inter-frequency / second-site context.

CELL-003 on SITE-002, n41, INTER_FREQUENCY neighbour of CELL-001. Use for configuration/neighbour context only. Do **not** claim RF interference, coverage, or HO optimization.

---

## 23. Assurance Bootstrap Decision

**Chosen: Option C (hybrid), Kafka-free.**

| Option | Verdict |
|--------|---------|
| A — Kafka + simulator | Rejected for 1.0 demo. Adds brokers, clock footguns, nondeterminism. |
| B — SQL-insert cases | Rejected. Bypasses `DegradingRadioQualityDetector`; can diverge from KPIs. |
| C — NOW-relative KPI series + `evaluateCell` | **Accepted.** Truthful, deterministic, testable, no Kafka. |

`DemoBootstrapRunner` (profile `demo`) after Flyway **and after** the knowledge import (section 27.5):

1. Call `AssuranceDetectionService.evaluateCell` for exactly `{CELL-001, CELL-007, CELL-010, CELL-014}`.
2. Fail startup if CELL-001 did not yield CRITICAL OPEN `DEGRADING_RADIO_QUALITY`.

Optional live telemetry remains a **developer** path, not the customer-demo path.

Knowledge import does not create Assurance cases. Assurance still uses V21 KPI series + the real detector.

---

## 24. Temporal / Clock Strategy

**Decision: SQL `NOW()`-relative observations. No demo clock service. No simulator `fixed` T0 on the demo path.**

- V21 uses `NOW() - INTERVAL 'Nh'` with N ≤ 24 for featured series (well inside 168h).
- Bootstrap does not start the Go simulator.
- Do not add a custom `Clock` bean for the whole application (would break existing tests).

Months later, migrate+bootstrap still produces recent series without editing literals.

---

## 25. Digital Twin Bootstrap Strategy

**Decision: demo-profile featured sync via existing `TwinSynchronizationService`. No global auto-sync. Production semantic unchanged.**

Featured set (exact): `CELL-001`, `CELL-002`, `CELL-003`, `CELL-007`.

**Order:** run **after** the ENM simulator knowledge import so twin snapshots are taken on the post-import canonical graph. CELL-001 identity/config is unchanged by that import (no overlapping canonical ID), so the twin remains a DEMO_SEED cell twin.

Runner calls the same synchronize path the UI uses. Idempotent: re-sync may add a version; freshness must be CURRENT after bootstrap.

UI still shows Digital Twin prerequisites and a Synchronize control. Copy: “Featured demo cells are synchronized at demo startup. Synchronization remains an explicit action, not a continuous background twin.”

Ordinary `spring-boot:run` **without** `demo` must **not** synchronize. Do **not** auto-sync `CELL-SIM-001` unless a presenter opens it.

---

## 26. Sandbox Enablement Strategy

**Decision: Spring profile `demo` → `application-demo.yml`.**

```yaml
# application-demo.yml — DEMO ONLY. Do not copy these values into application.yml.
snip:
  change-execution:
    enabled: true
    permitted-target-types: [SIMULATOR]
    automatic-rollback-enabled: false
  production-change:
    enabled: false
    global-execution-enabled: false
  kafka-enabled: false
  demo:
    enabled: true
    bootstrap-assurance: true
    synchronize-featured-twins: true
    trigger-simulator-knowledge-baseline: true   # mandatory; not optional
    featured-assurance-cells: [CELL-001, CELL-007, CELL-010, CELL-014]
    featured-twin-cells: [CELL-001, CELL-002, CELL-003, CELL-007]
```

Startup: `--spring.profiles.active=demo` or `SPRING_PROFILES_ACTIVE=demo`.

Committed `application.yml` **must** keep `snip.change-execution.enabled: false`.

Equivalent JVM override `--snip.change-execution.enabled=true` remains valid for engineers but is **not** the documented customer-demo path.

Startup fail-closed if `snip.production-change.enabled=true` while `demo` is active.

---

## 27. Demo Bootstrap Architecture

**Decision: small documented command sequence + wrapper scripts. Not “Compose does everything.” Kafka not started.**

Why not Compose-only: frontend is Vite on the host; API may already run from Maven; WAODN/other stacks steal 8080/5432; Kafka is unnecessary.

### 27.1 Prerequisites

Java 17, Maven 3.9+, Node 20+, Docker (Postgres 16), npm. **No** Azure CLI, **no** Ollama, **no** Kafka.

### 27.2 Commands (authoritative sequence)

```text
1. docker compose up postgres -d
2. mvn -pl snip-npo-app -am spring-boot:run -Dspring-boot.run.profiles=demo
3. cd snip-web && npm ci && npm run dev
4. scripts/snip-demo-ready  (or the up-script’s ready step)
5. Open http://127.0.0.1:5173
```

Wrapper `scripts/snip-demo-up.ps1` and `scripts/snip-demo-up.sh` run 1–4, print the UI URL, and refuse to call the sequence “ready” until section 35 passes.

If port `5432` or `8080` is occupied, scripts **fail** with a message to set `SNIP_DB_PORT`, `SNIP_HOST_PORT`, and `SNIP_API_TARGET` (existing Vite mechanism in `snip-web/src/dev/snipApiTarget.ts`). Do not silently bind another process’s database.

### 27.3 What bootstrap starts

| Component | Required? |
|-----------|-----------|
| Postgres Compose volume `snip-postgres` under project `snip-demo` (physical volume `snip-demo_snip-postgres`) | YES |
| `snip-npo-app` profile `demo` | YES |
| Vite `5173` | YES |
| Kafka | NO |
| Go simulator | NO |
| Ollama | NO |
| Production write gateway | NO |

Scripts **must** invoke Compose as project **`snip-demo`**:

```text
docker compose -p snip-demo up postgres -d
```

Do not rely on the repository directory name (`networkplanningoptimization`) as the Compose project.

### 27.4 Runner order (`DemoBootstrapRunner`)

Exact order (required):

1. **Profile already loaded:** `application-demo.yml` has enabled sandbox execution and disabled production-change / Kafka.
2. **Safety assert:** `DemoSafetyGuard` — production-change false; change-execution true; kafka false. Fail closed otherwise.
3. **Flyway already applied** (Spring start) including V21. Assert **V21 featured IDs** `SITE-001`…`006`, `GNB-001`…`006`, `CELL-001`…`018` exist. Do **not** assert global `COUNT(*)=6` after later import.
4. **Knowledge / trusted baseline (OQ-1):** section 27.5.
5. **Featured twin synchronize** (`CELL-001`, `CELL-002`, `CELL-003`, `CELL-007`).
6. **Featured `evaluateCell`** (`CELL-001`, `CELL-007`, `CELL-010`, `CELL-014`). Fail if CELL-001 is not CRITICAL OPEN.
7. Log `SNIP demo bootstrap ready` with CELL-001 case id (no secrets).

**Why this order:** sandbox is a process flag and must be on before any UI click; V21 must exist before import so coexistence is testable; knowledge import must precede Optimize eligibility and should precede twins so snapshots match post-import canonical state; Assurance uses KPI context, not the ENM snapshot, so it runs last among data steps; frontend starts after the API is ready.

### 27.5 Knowledge bootstrap — OQ-1 RESOLVED

**Chosen mechanism: A — EXISTING ENM SIMULATOR IMPORT.**

Use the **same in-process path** as `ChangeIntelligenceApiTest.runTrustedBaseline`, not a new knowledge architecture:

1. `SimulatorEnmScenarioController.use(SimulatorEnmScenario.FULL_SUCCESS)` — FULL fetch uses the same page-1 inventory as default `SUCCESS_SINGLE_PAGE` (`SITE-SIM-001` / `GNB-SIM-001` / `CELL-SIM-001`). Pinning `FULL_SUCCESS` matches the tests that already produce `RECOMMENDED` for CELL-001.
2. `VendorImportAuthorizer.runWith(VendorImportAuthorizer.PERMISSION, …)` (`TRIGGER_VENDOR_IMPORT`). This is the existing authorization override tests use. It is **not** a KnowledgeGate bypass.
3. `SynchronizationControlPlane.triggerManual(ConnectorDefinition.ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)`.
4. First run: `SynchronizationModeSelector` returns **FULL** (checkpoint `UNVERIFIED` / no successful completion). `SynchronizationImportService` advances checkpoint then `recordOutcome` → `NetworkKnowledgeConfidenceEvaluator` with `lastSuccessfulCompletedAt` set → expected **HIGH** / `TRUSTED_FRESH_COMPLETE` / freshness **FRESH** (`aging-after: 30m`, `stale-after: 2h`).
5. Fail bootstrap if resulting confidence is not `HIGH` or `MEDIUM`.

**What this does to inventory:**

| Effect | V21 `CELL-001`…`018` | Simulator overlay |
|--------|----------------------|-------------------|
| Insert | No | CREATE `SITE-SIM-001`, `GNB-SIM-001`, `CELL-SIM-001` if absent |
| Upsert/replace V21 IDs | **No** — different canonical IDs | Updates only SIM-* if already present |
| Delete V21 cells | **No** — MISSING applies only to previously seen `ERICSSON_ENM_SIMULATOR` source references; V21 rows have none | `markMissing` never `DELETE FROM cell` |
| CELL-001 txPower / KPIs / provenance | Unchanged | N/A |
| Neighbours among V21 cells | Unchanged | May add none among V21 IDs on page-1 |

**What makes Optimize `RECOMMENDED`:** `KnowledgeGate` reads **source-scoped** `network_knowledge_status` for the enabled policy (`ERICSSON_ENM_SIMULATOR` / `DEFAULT`), not “is CELL-001 in the vendor snapshot?”. Tests already generate `RECOMMENDED` for **CELL-001** after importing **CELL-SIM-001**.

**Forbidden:** SQL `UPDATE network_knowledge_status SET confidence='HIGH'`; disabling `KnowledgeGate`; demo-only generate bypass; production ENM (`UnconfiguredProductionEnmTransport`); JSON `normal.json` Ericsson fixture import as a substitute unless implementation proves it is the same control-plane path (it is **not**).

**Failure behavior:** If `triggerManual` skips (overlap), fails, or leaves UNKNOWN/LOW: **fail bootstrap**. Do not continue to a demo that can only produce `EVALUATED` + `NETWORK_KNOWLEDGE_UNKNOWN`.

**Scheduler:** `cadence: 15m`. After a successful runner import, scheduled ticks are not due during a 45-minute demo. Do not wait for the scheduler. Aging at 30m still yields **MEDIUM**, which still `allowsRecommendation()`.

**Idempotent second run:** overlap SKIP or INCREMENTAL empty batch (`SUCCESS_SINGLE_PAGE` / `FULL_SUCCESS` incremental default is empty). Must not delete V21 cells. Knowledge remains HIGH/MEDIUM.

---

## 28. Bootstrap Contract

After successful bootstrap, all of the following are true:

1. `GET /health` → `{"status":"UP"}`
2. `GET /api/v1/sites` includes `SITE-001`…`SITE-006` (total **≥6**; overlay `SITE-SIM-001` allowed)
3. `GET /api/v1/cells` includes `CELL-001`…`CELL-018` (total **≥18**; overlay `CELL-SIM-001` allowed)
4. CELL-001 `txPower` is still `46`; KPI `source=DEMO_SEED` / `synthetic=true` still present
5. CELL-001 has an OPEN CRITICAL `DEGRADING_RADIO_QUALITY` case
6. CELL-002 has **no** active degrading case
7. Featured twins CURRENT
8. `GET /api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT` with header `X-SNIP-VENDOR-IMPORT-PERMISSION: VIEW_SYNCHRONIZATION_STATUS` shows knowledge **HIGH or MEDIUM**
9. `snip.change-execution.enabled` is true in the running process (demo profile)
10. `snip.production-change.enabled` is false (inspect `application-demo.yml` / process profile; **no** new readiness API)
11. Planning list is empty after a clean reset+bootstrap
12. Frontend `http://127.0.0.1:5173` proxies `/api` to the demo API

Presenter performs **zero** curl/SQL to reach this state (ready script may use HTTP GET internally).

---

## 29. Bootstrap Idempotency

| Situation | Behavior |
|-----------|----------|
| Bootstrap / runner runs twice on the same DB | **Safe.** `evaluateCell` upserts active case; twin re-sync allowed; knowledge trigger is existing overlap/SKIP-safe control plane. Exit success. |
| `snip-demo-up` while API already healthy and contract 28 holds | Print `SNIP demo already ready` and exit 0. Do not destroy volumes. |
| API healthy but any of `SITE-001`…`006` / `CELL-001`…`018` missing | **Fail** with “unexpected demo inventory; run snip-demo-reset”. Overlay SIM-* IDs must not cause this failure. |
| `demo` profile on a non-V21 database | Fail with migration/inventory error. |

No silent inventory rewrite.

---

## 30. Demo Reset Architecture

**Decision: B — dedicated local reset scripts that tear down the SNIP Compose Postgres volume, then require a new bootstrap.**

Rejected:

| Option | Why |
|--------|-----|
| Public DELETE REST API | Unsafe; rejected |
| Flyway clean inside the running app | Easy to point at the wrong DB; not a customer tool |
| Application demo-reset endpoint | Same as public API risk |

`scripts/snip-demo-reset.ps1` / `.sh`:

1. Enforce section 31 (confirmations **and** ownership).
2. Stop demo API/Vite **only if those scripts started them** (PID files under a demo-local path such as `.snip-demo/`). Do not `kill` unrelated Java/Node; do not `docker kill` containers lacking project label `com.docker.compose.project=snip-demo`.
3. `docker compose -p snip-demo down` then remove **only** the proven volume `snip-demo_snip-postgres`.
4. Do **not** remove `ollama-data`, `snip-postgres` volumes belonging to another project, or unnamed leftover volumes.
5. Do not start Kafka. If a leftover telemetry stack exists under **this** project, reset may `docker compose -p snip-demo --profile telemetry down` **only** when `SNIP_DEMO_RESET_TELEMETRY=YES` (default NO).
6. Print: “Reset complete. Run snip-demo-up.”
7. **Forbidden:** `docker volume prune`, `docker system prune`, `docker compose down --remove-orphans` against the default project name, Azure/cloud resource APIs.

Reset **destroys** this demo Postgres: twins, cases, planning, proposals, plans, executions, knowledge, import leases, ENM overlay rows.

---

## 31. Reset Safety Guard

Reset must refuse unless **all** confirmations are true:

```text
SNIP_DEMO_RESET=YES
SNIP_DEMO_RESET_CONFIRM=snip-demo
```

Missing either → **exit 2**, no Docker mutations. Do not accept `YES` on a comment alone.

**Additionally, ownership must be proven before any deletion.** The script must:

1. `cd` / require the repository root: files `docker-compose.yml` **and** directory `snip-npo-app` exist.
2. Parse **this** `docker-compose.yml` and confirm it defines Compose volume **key** `snip-postgres` (not a different volume name).
3. Use Compose project identity **`snip-demo`** exclusively (`docker compose -p snip-demo` / `COMPOSE_PROJECT_NAME=snip-demo`). Do not inherit the folder name.
4. Resolve the expected volume name: **`snip-demo_snip-postgres`**.
5. If that volume **exists**, `docker volume inspect` it and require labels:
   - `com.docker.compose.project=snip-demo`
   - `com.docker.compose.volume=snip-postgres`
6. If a `postgres` container exists for this project, require label `com.docker.compose.project=snip-demo` and Compose service `postgres`. Ports must bind `127.0.0.1` (not `0.0.0.0` on a shared LAN) as already specified in `docker-compose.yml`.
7. If ownership **cannot** be proven (wrong labels, unexpected project, inspect error that is not “volume not found”): **exit non-zero, delete nothing**.
8. If the target volume is **absent**: **exit 0**, print `SNIP demo volume already absent`, delete nothing else. Deterministic and safe.
9. Unrelated volumes (including a bare `snip-postgres` from another compose project) and unrelated containers: **untouched**.

Do not use Docker context other than the local engine implied by the developer’s `docker` CLI. Do not touch Azure.

---

## 32. Reset Contract

After reset and before bootstrap: volume gone or empty; no leftover `planning_scenario`, `network_change_proposal`, `network_change_plan`, or change-execution rows can exist because the database does not exist.

After reset + bootstrap: section 28 holds again. Old proposal/plan/execution IDs from the previous demo return 404.

---

## 33. Reproducibility Contract

```text
RESET → BOOTSTRAP → full customer demo → RESET → BOOTSTRAP → full customer demo
```

Second run requires no SQL, curl, offset editing, or source edits. Featured CELL-001 story is available both times.

---

## 34. Startup / Port Contract

| Service | Default | Notes |
|---------|---------|-------|
| Postgres | `127.0.0.1:5432` (`SNIP_DB_PORT`) | Compose `snip-postgres` |
| API | `127.0.0.1:8080` (`SNIP_HOST_PORT`) | demo profile |
| Vite UI | `127.0.0.1:5173` | **authoritative SNIP 1.0 UI** |
| Vite API target | `http://127.0.0.1:8080` (`SNIP_API_TARGET`) | `snipApiTarget.ts` |
| Legacy static | `http://127.0.0.1:8080/` | **not** the customer demo |
| Write gateway | 8081 | must **not** be started |
| Kafka | 9092 | must **not** be started for demo |

Success: ready check green + browser login page.

Failure messages must name the occupied port and the env var to override. Do not bind WAODN’s database.

---

## 35. Demo Health / Readiness Check

`scripts/snip-demo-ready` (also invoked by up) performs HTTP GETs only. **OQ-2 RESOLVED: no `GET /api/v1/demo/readiness`.**

| Check | Existing interface | Pass |
|-------|--------------------|------|
| Backend up | `GET {API}/health` | `{"status":"UP"}` |
| V21 sites | `GET /api/v1/sites` | `SITE-001`…`SITE-006` present (`SITE-SIM-001` allowed extra) |
| V21 cells | `GET /api/v1/cells` | `CELL-001`…`CELL-018` present (`CELL-SIM-001` allowed extra) |
| Featured case | `GET /api/v1/cells/CELL-001/assurance` or list cases | OPEN CRITICAL `DEGRADING_RADIO_QUALITY` |
| Twin | existing twin GET used by the UI | CELL-001 CURRENT |
| Knowledge | `GET /api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT` with `X-SNIP-VENDOR-IMPORT-PERMISSION: VIEW_SYNCHRONIZATION_STATUS` | confidence HIGH or MEDIUM |
| Production-change disabled / sandbox enabled | inspect **local files**: `application.yml` still `change-execution.enabled: false`; running command line contains `demo` profile (`application-demo.yml` enables sandbox). Do not add an API for this. | |
| Frontend | TCP `127.0.0.1:5173` | listening |

Ready-script GET of the sync source is a **local presenter tool**, not a customer UI change. `snip-web` already calls this GET (`snipApi.ts`) with the view header on Optimize/Cell pages.

---

## 36. Customer UI Terminology Changes

Exact replacements (customer-visible strings only):

| Location | Current | Required |
|----------|---------|----------|
| `LoginPage.tsx` | `persona is frontend-only identity state for Increment 1A read-only screens.` | `The selected persona is frontend-only demo identity. This is not production authentication.` |
| `LoginPage.tsx` | visible `persona.actorId` | **Remove** from the button label. May remain in session payload. |
| `PlanningScenarioPage.tsx` | `PI3 Optimize handoff` | `Optimize handoff` |
| `AssuranceCasePage.tsx` | `Governed optimization remains the existing PI3 workflow.` | `Governed optimization remains the existing Optimize workflow.` |
| `NotFoundPage.tsx` | `That route is not part of this SNIP increment.` | `That page is not part of SNIP.` |
| `AppShell.tsx` | `title="Later increment"` | Items **removed** (section 38). If any disabled leftover remains, title `Not included in this demonstration.` |
| `AiPage.tsx` | `Uses POST /api/v1/recommendations. Output is decision support…` | `Output is decision support, not an approved change and not a live-network action.` (keep Ask SNIP caveats) |
| `ChangePlanPage.tsx` | `Backend status remains READY_FOR_EXECUTION.` | Remove from the customer banner. Keep `READY FOR SANDBOX ADMISSION` + `NO REAL NETWORK CHANGE`. Raw enum may stay inside existing `<details>` / diagnostic `dd` only. |

`StatusBadge` via `formatStatusLabel` may still show `READY FOR EXECUTION` if `plan.status` is passed through. **Required:** `ChangePlanPage` must not pass `READY_FOR_EXECUTION` to the primary badge without mapping. Map customer label to `READY FOR SANDBOX ADMISSION` when `plan.status === READY_FOR_EXECUTION`. Do **not** change the backend enum.

Do not remove: Digital Twin, Assurance, Sandbox, Synthetic, Confidence, LOW.

Frontend tests that search `/PI3/` or `/Increment 1A/` on rendered journey pages must flip to asserting **absence**.

---

## 37. Customer UI Guidance Changes

### 37.1 Planning (`PlanningScenarioPage` / create page)

Add a **short** `role="note"` (not a wizard):

1. Select up to 4 cells. Featured story uses CELL-001 and CELL-002.
2. Each alternative is an independent cell-local `txPower` what-if (20–50 dBm).
3. Evaluate requires a **CURRENT cell Digital Twin**. Demo startup synchronizes featured cells; Synchronize remains available.
4. Evaluate runs the synthetic cell-parameter model. Not joint RF.
5. Optimize handoff opens the existing Optimize page for **one** cell. It does not generate a proposal from Planning.

Keep `ScenarioTruthBanner` / `PLANNING_TRUTH`.

Wire `busyCell` / `setBusyCell` so Synchronize shows “Synchronizing…” (`PlanningScenarioPage` + `PrerequisitePanel`).

### 37.2 Network map

One sentence: synthetic locations, not coverage.

### 37.3 Optimize / plan / sandbox

Keep existing demo/sandbox banners. DemoPermissionBanner: add one sentence: “Demo authorization uses request headers, not production IAM or segregation of duties.”

---

## 38. Changes/Campaigns Navigation Decision

**Decision: A — hide.**

Remove the `LATER` Changes/Campaigns spans from `AppShell.tsx` for SNIP 1.0.

They currently read as unfinished production features. Do not implement campaign routes.

---

## 39. AI Truthfulness Contract

Customer wording **must** allow:

- “AI-assisted explanation”
- “decision support”

Customer wording **must not** imply:

- the LLM selects `txPower`
- the LLM controls the network
- the optimizer is an autonomous agent
- the Digital Twin is an LLM
- default install uses Qwen/Ollama

Default remains `snip.generator: stub`, `snip.retrieval-mode: lexical`. `local-ai` is optional and **not** part of the customer-demo startup.

Ask SNIP result may continue to show `retrievalMode`. Preferred customer phrase: “Explanation uses retrieved notes and structured cell context. Default demonstration generation is a stub, not a production telecom LLM.”

Agents remain off the journey. No `/agent-runs` from `snip-web`.

---

## 40. Digital Twin Truthfulness Contract

Always **cell Digital Twin**. Never site/network/coupled-RF twin in UI or customer docs.

It is versioned, freshness-aware (CURRENT/STALE/EXPIRED), cell-scoped, and used to admit synthetic cell-local `txPower` simulation.

---

## 41. Simulation Truthfulness Contract

Do not change:

| Field | Value |
|-------|--------|
| Model | `snip.synthetic.cell-parameter.v1` |
| Version | `1.0` |
| Parameter | `txPower` |
| Range | 20–50 dBm |
| Confidence | LOW |
| Synthetic | true |
| Vendor calibrated | no |
| Cross-cell | no |

`SimulationEvidence.tsx` and `PLANNING_TRUTH` stay.

---

## 42. Demo Persona / Authorization Contract

Primary presenter persona: **Priya Naidoo / RF Optimisation Engineer** (`demo-rf-optimisation`).

Keep all three personas. Switching is optional colour; it does **not** change backend permissions.

Document in the demo guide: governance calls send `X-SNIP-CHANGE-PROPOSAL-PERMISSION`, `X-SNIP-CHANGE-PLAN-PERMISSION`, `X-SNIP-CHANGE-EXECUTION-PERMISSION` from the browser. This is **DEMO AUTHORIZATION BEHAVIOR**, not production IAM/SoD. The same demo actor can review then authorize.

Do not implement real IAM.

---

## 43. Error / Failure UX

- Keep `ErrorState` operator headings; stack traces / SQL / raw Java must not render in page body.
- Correlation ID / `failureCode` stay in collapsed `<details>` only.
- **Planning comparison:** replace `.catch(() => null)` with visible `ErrorState` or inline alert when `getPlanningComparison` fails and `evaluationView` is EVALUATED/PARTIAL. Do not hide a failed compare as “no comparison.”
- Sandbox disabled (if someone runs without demo profile): keep existing `CHANGE_EXECUTION_DISABLED` banner; demo guide says to use profile `demo`.

---

## 44. Documentation Deliverables

Implementation **creates** these (this specification task does not):

| ID | Path | Audience |
|----|------|----------|
| A | `docs/customer/SNIP-1.0-PRODUCT-OVERVIEW.md` | ~1 page; executive / engineering lead |
| B | `docs/customer/SNIP-1.0-DEMONSTRATION-GUIDE.md` | presenter |
| C | `docs/customer/SNIP-1.0-LIMITATIONS-AND-TRUTH-STATEMENT.md` | customer + presenter |
| D | Update root `README.md` | developers |
| E | Update `snip-web/README.md` | frontend developers |
| F | Update `docs/implementation/SNIP-IMPLEMENTATION-STATUS.md` | engineers |

Customer docs (A–C) **must not** use Phase 13–18, PI3, PI7 as the product language. Historical docs stay historical.

### 44.1 Overview (A) must include

What SNIP 1.0 demonstrates; cell twin; synthetic txPower; human governance; sandbox; next step read-only PoC; not production-ready.

### 44.2 Demo guide (B) must include

Prerequisites; bootstrap; reset; ports; profile `demo`; 30–45 minute script (section 45); troubleshooting (ports, occupied 8080, twins STALE, sandbox flag, wrong UI); STOP conditions (section 63/83).

### 44.3 Limitations (C) must include

Synthetic data; cell-only twin; txPower-only; LOW confidence; no coupled RF; no production writes; no closed loop; stub+lexical default; sandbox-only execution; demo login; header-based demo auth.

### 44.4 Root README (D)

- Remove or replace “Phase 13 has not started.”
- Point the **product UI** to `http://127.0.0.1:5173`.
- Point demo startup to the demo guide / `snip-demo-up`.
- Keep health URL; label `:8080/` static page as legacy developer UI.

### 44.5 snip-web README (E)

Stop “Product Increment 1A / 1B” as the product title. Describe current workspaces including Planning, Optimization, Assurance, sandbox. Keep security boundary (no `/mcp`, no production-change).

### 44.6 Implementation status (F)

Record PI1A–PI7 closed; this milestone as SNIP 1.0 hardening; Phase 19 NOT STARTED; production execution NOT AUTHORIZED.

---

## 45. Demo Script

Target 42 minutes. Achievable without rushing after bootstrap.

| Min | Page | Question | Action | SNIP shows | Message | Caveat |
|-----|------|----------|--------|------------|---------|--------|
| 0–2 | `/login` | Who is this? | Priya Naidoo | Demo actor | Controlled demo identity | Not SSO |
| 2–6 | `/network` | What do I have? | Map + counts + queue | 6 sites; CRITICAL on CELL-001 | Operations, not a health score | Synthetic cluster |
| 6–12 | `/assurance/:id` | What should I investigate? | Open CRITICAL case | Evidence + SNIP analysis + gaps | Prioritized finding | Rule-based; stub narrative |
| 12–16 | cell + site | Related context? | CELL-001, CELL-002, SITE-002/CELL-003 | Config, KPIs, neighbours | Contrast, not interference | Synthetic KPIs |
| 16–22 | `/planning/new` → scenario | What if txPower? | Cells 001+002; Evaluate | Per-cell synthetic results | Explore before recommend | Independent, LOW |
| 22–28 | Optimize → proposal | What does SNIP recommend? | Generate | Ranked candidates + sim evidence | Deterministic recommendation | Not the LLM |
| 28–36 | plan | How is it governed? | Approve → plan → review → authorize → readiness | Status + sandbox admission | Humans stay in control | Demo headers ≠ IAM |
| 36–43 | sandbox | Rehearse? | request → review → authorize → execute → verify | Four-way; real Unchanged | Rehearsal ≠ production | Simulator only |
| 43–45 | close | Live network? | Point at Unchanged | No | Next: read-only PoC | No write promise |

---

## 46. Customer Questions / Truthful Answers

| Question | Answer |
|----------|--------|
| Real network data? | No. Synthetic `DEMO_SEED` / demo labels. |
| AI making the decision? | No. Detection and txPower selection are deterministic. Ask SNIP is decision support. Default generator is a stub. |
| Digital Twin? | Versioned **cell** snapshot for synthetic `txPower` dry-run. Not site/network RF twin. |
| Interference? | Not modelled. |
| Joint multi-cell optimize? | No. Planning evaluates cells independently. |
| Write Ericsson? | Not in this demo. Production write not authorized. Sandbox = `snip-simulator`. |
| Nokia? | Fixture/read-only architecture exists; real NetAct deferred. |
| Huawei? | Not supported. |
| Your KPIs / OSS / Oracle? | Requires a later read-only PoC mapping. |
| Engineers reject? | Yes. Approve/reject is human. |
| Read-only? | Yes — intended next stage. |
| Autonomous? | No. |
| Real network touched? | No. Four-way Real network Unchanged. |

---

## 47. Read-Only PoC Handoff

Close: propose a **read-only** PoC (real inventory/config/KPIs/telemetry, shadow recommendations, human comparison, **no writes**). Do not implement PoC adapters in this milestone.

---

## 48. Commercial Claims

Allowed: AI-assisted investigation; network intelligence and Assurance; bounded what-if `txPower` planning; cell Digital Twin synthetic preview; deterministic evidence-backed single-cell optimization; human-governed change planning; sandbox rehearsal; multi-vendor **abstraction** architecture; read-only next-stage PoC.

---

## 49. Prohibited Claims

Autonomous AI optimization; self-healing; production-ready/certified; nationwide real-time ops; predictive RF; coverage/interference simulation; site/network Digital Twin; joint multi-cell optimization; live Ericsson/Nokia writes; Huawei support; vendor-calibrated RF.

---

## 50. Database / Migration Plan

- **Do not edit** V1–V20.
- **Create:** `snip-npo-app/src/main/resources/db/migration/V21__snip_1_0_customer_demo_network.sql`
- Next free version after `V20__planning_what_if_scenario.sql` is **V21**.
- V21: INSERT sites 003–006, gNBs 003–006, cells 004–018, neighbours, radio, NOW-relative KPIs. No assurance/planning/proposal INSERTs.
- Same PostgreSQL. No new schema or database product.

---

## 51. API Plan

Prefer existing APIs: sites/cells, assurance, twins synchronize, planning, change-intelligence, change-planning, change-execution, `/health`, `GET /api/v1/integration/sync/sources/{sourceSystem}/{sourceScope}`.

**Forbidden:** public reset DELETE; production-change from web; `/mcp` from web; `/agent-runs` from web; **`GET /api/v1/demo/readiness` (do not create).**

Knowledge baseline uses **existing in-process** `SynchronizationControlPlane.triggerManual` from `DemoBootstrapRunner`, not a new customer API.

### 51.1 Expanded-dataset coexistence proof (required test)

`DemoSimulatorImportCoexistenceTest` (name indicative), `@SpringBootTest` on V21 inventory (demo profile or equivalent runner steps):

Given V21 applied, pin `FULL_SUCCESS`, `runWith(TRIGGER_VENDOR_IMPORT)`, `triggerManual(ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER)` once.

**Must pass:**

- `SITE-001`…`SITE-006` exist
- `GNB-001`…`GNB-006` exist
- `CELL-001`…`CELL-018` exist
- featured IDs unchanged
- V21 neighbour endpoints still valid
- CELL-001 `txPower` still `46`
- CELL-001 KPI rows remain `source=DEMO_SEED` and `synthetic=true`
- no V21 cell row physically deleted
- overlay `SITE-SIM-001` / `GNB-SIM-001` / `CELL-SIM-001` **may** exist (expected)
- knowledge HIGH or MEDIUM
- CELL-001 `cell_id` string is still `CELL-001`

If this test fails against current code, that is an **implementation defect to fix without faking knowledge**, not a license to SQL-update confidence. Repository analysis at specification time shows IDs do not collide; the test **proves** that analysis on every build.

### 51.2 Primary-story automated proof (required test)

`DemoPrimaryStoryEligibilityTest` (name indicative), `@ActiveProfiles("demo")` after runner (or equivalent sequenced calls):

1. CELL-001 exists, `txPower=46`
2. Recent INCREASING BLER/PRB series present
3. OPEN CRITICAL `DEGRADING_RADIO_QUALITY` on CELL-001
4. Knowledge HIGH or MEDIUM for `ERICSSON_ENM_SIMULATOR`/`DEFAULT`
5. CELL-001 twin CURRENT after featured sync
6. Create a planning scenario with cells `{CELL-001, CELL-002}` and at least one `txPower` intent in 20–50; `evaluate` reaches a terminal evaluation that is not a hard API 5xx (`SUCCEEDED` or `PARTIAL` with CELL-001 item succeeded)
7. `POST /api/v1/change-intelligence/proposals` for CELL-001 / `txPower` with `GENERATE` permission
8. Result status **`RECOMMENDED`**, failureCode **not** `NETWORK_KNOWLEDGE_UNKNOWN` or `NETWORK_KNOWLEDGE_LOW`

Do **not** assert an exact proposed dBm. `NO_BENEFICIAL_CANDIDATE` is a **test failure** against the featured story (the seed must remain eligible); do not special-case CELL-001 in production code to force a value.

---

## 52. Frontend Plan

Keep React / TS / Vite / Router / Leaflet.

Modify (indicative): `AppShell.tsx`, `LoginPage.tsx`, `AiPage.tsx`, `NotFoundPage.tsx`, `AssuranceCasePage.tsx`, `PlanningScenarioPage.tsx`, `PlanningCreatePage.tsx`, `PrerequisitePanel.tsx`, `ChangePlanPage.tsx`, `DemoPermissionBanner.tsx`, `SiteMap.tsx` or `NetworkPage.tsx`, `StatusBadge` usage on plan page, tests `pages.test.tsx`, `increment2–7.test.tsx` as needed, new `customerTerminology.test.tsx`.

No new SPA, no Ionic, no extra state library.

---

## 53. Backend Plan

| Item | Action |
|------|--------|
| `V21__snip_1_0_customer_demo_network.sql` | CREATE |
| `application-demo.yml` | CREATE |
| `com.simba.snip.npo.demo.DemoBootstrapRunner` | CREATE — `@Profile("demo")` `ApplicationRunner` |
| `com.simba.snip.npo.demo.DemoSafetyGuard` | CREATE — fail if production-change enabled on demo |
| `SnipProperties` demo nested props **or** `DemoProperties` | CREATE small `@ConfigurationProperties(prefix="snip.demo")` |
| `AssuranceDetectionTest` | MODIFY — featured cell **does** match after V21; add healthy-cell no-match |
| Dataset / bootstrap / coexistence / primary-story tests | CREATE |
| Demo readiness controller | **DO NOT CREATE** |
| Default `application.yml` safety flags | UNCHANGED (`change-execution.enabled=false`, production-change false) |

Do not add a `demo` package capability that agents/MCP can call.

Tests that activate `@SpringBootTest` **must not** set `demo` except dedicated demo tests.

---

## 54. Go / Simulator Plan

**No functional Go change required** for the customer-demo path (simulator is not started).

Optional later: default README `time-mode now` — documentation only in this milestone (root README update). Do not change simulator T0 semantics for production freshness.

---

## 55. Configuration Plan

| File | Change |
|------|--------|
| `application.yml` | **UNCHANGED** safety defaults. Do not set `change-execution.enabled=true` here. |
| `application-demo.yml` | **CREATE** — section 26 |
| `application-local-ai.yml` | UNCHANGED; not used by demo |
| `docker-compose.yml` | UNCHANGED unless a comment is required; do not auto-enable Kafka on `api` |
| `snip-web/.env.example` | UNCHANGED except optional comment that 1.0 demo UI is 5173 |

---

## 56. CI / Qualification Plan

Keep `.github/workflows/ci.yml` jobs: Go, Maven, frontend test, frontend build.

Add **no** Playwright job to every-commit CI.

New Maven tests run inside `mvn -B test`.

Frontend new/adjusted Vitest tests run inside `npm test`.

Optional CI step: `test -f scripts/snip-demo-up.sh` existence — unnecessary if scripts are tracked.

Do not start Docker Compose telemetry in CI.

After implementation: Maven count may rise; **must not** drop unexplained vs PI7 reference 1543. Frontend 118 may rise. Go remains PASS.

---

## 57. Data Validation Tests

`DemoNetworkSeedTest` (indicative) on default IT Postgres (V21 applied, **not** demo profile — no ENM overlay yet):

- site count 6; gNB 6; cell 18
- featured IDs present
- every neighbour endpoint exists
- every `txPower` in [20,50]
- KPI metrics ⊆ known set
- all V21 KPIs `synthetic=true` and `source=DEMO_SEED`
- SITE-001…006 have non-null coordinates
- no credential-like columns populated
- CELL-001 current BLER_DL 0.12 and PRB 0.82 still present
- CELL-001 series classifies INCREASING when loaded through `NetworkContextService`
- CELL-002 does not classify INCREASING high BLER

Do not assert row insertion order.

---

## 58. Bootstrap Tests

`DemoBootstrapRunnerTest` with `@ActiveProfiles("demo")` (isolated):

- clean start creates CELL-001 CRITICAL case
- second run does not duplicate active cases (unique index)
- featured twins CURRENT
- knowledge HIGH or MEDIUM after **required** ENM simulator import
- V21 18 cells still present after import
- `production-change.enabled` false; kafka false; change-execution true
- missing featured cell → runner fails
- knowledge UNKNOWN/LOW → runner fails

Duplicate bootstrap must not corrupt planning (none exist yet). Also run section 51.1 and 51.2.

---

## 59. Reset Tests

Cannot fully Docker-compose in every Maven test.

Specify **reset safety tests** (script unit/Pester or a test double around the ownership function):

| Case | Expected |
|------|----------|
| Confirmations set, volume labels `project=snip-demo` and `volume=snip-postgres`, compose file in repo root | reset **allowed** (destructive path may be stubbed in unit test) |
| Missing `SNIP_DEMO_RESET` | rejected, no deletion |
| Missing `SNIP_DEMO_RESET_CONFIRM` | rejected, no deletion |
| Confirmations present but cwd lacks `docker-compose.yml` + `snip-npo-app` | rejected |
| Compose project label ≠ `snip-demo` | rejected |
| Volume name/labels unexpected (e.g. `{otherproject}_snip-postgres`) | rejected |
| Target volume absent | exit 0, no other deletions |
| Unrelated Docker volume present | untouched |
| Unrelated container present | untouched |

Plus **state isolation IT:** create a planning scenario + proposal in a demo-profile IT; drop/recreate schema or use Testcontainers new DB (existing `AbstractPostgresIT`); bootstrap again; assert old IDs absent, CELL-001 case present, knowledge HIGH/MEDIUM again.

Release acceptance (section 65) is the full volume-teardown proof.

---

## 60. Frontend Tests

New `snip-web/src/pages/customerTerminology.test.tsx` (name indicative) renders login, planning scenario (mocked), assurance case, AI, not-found, shell:

- no `/Increment 1A/`, `/PI3/`, `/Phase 1[3-8]/`, `/Later increment/`
- Digital Twin / synthetic / sandbox strings remain where fixtures include them
- Ask SNIP page has no `/api/v1/recommendations`
- AppShell has no Changes/Campaigns
- comparison error path renders an alert (unit test the page handler)

Update increment tests that asserted old strings.

---

## 61. Backend Tests

In addition to 57–59:

- `DemoSafetyGuardTest`: demo + production-change true → context fails
- Rewrite `seedCell001DoesNotCreateCaseWithoutIncreasingTrend`
- Existing `ChangeIntelligenceApiTest` / `PlanningApiTest` / `ChangePlanningApiTest` / `ChangeExecutionApiTest` still pass (they manage their own telemetry/knowledge)
- Grep and fix any exact `COUNT(*)` on `site`/`cell`/`kpi_observation` that assumed V2-only totals

---

## 62. Regression Tests

Implementation complete only if:

- `mvn -B test` PASS
- `go test ./...` PASS
- `cd snip-web && npm test` PASS
- `cd snip-web && npm run build` PASS

No unexplained suite shrinkage.

---

## 63. Customer Browser Acceptance Test

**Release acceptance. Not every-commit CI. Do not execute in this specification task.**

**Preconditions:** reset + bootstrap; UI 5173; API demo profile; production-change false; change-execution true (demo); no Kafka required.

**Steps:**

1. Open `/login` — no Increment 1A, no actorId
2. Select Priya Naidoo → `/network`
3. See SITE-001…006 (an extra SITE-SIM-001 marker is allowed); CRITICAL queue row CELL-001
4. Open case → evidence + analysis; Open cell
5. Cell workspace: synthetic banner; neighbours include CELL-002; related/site reachable
6. Create what-if `CELL-001,CELL-002`; twins CURRENT; Evaluate
7. Comparison visible (or explicit error — not silent empty)
8. Optimize handoff heading is not PI3; open Optimize
9. Generate txPower proposal → `RECOMMENDED` (not `EVALUATED` for UNKNOWN/LOW knowledge). Do not require a specific dBm.
10. Review synthetic LOW evidence; Approve
11. Create plan; review; authorize; readiness
12. Request sandbox; review; authorize; Execute in Sandbox; Confirm readback
13. Four-Way: Real network **Unchanged**
14. Network inventory `txPower` for CELL-001 still 46 (canonical unchanged)

**STOP / fail:** production-change call; campaign execute; `/mcp`; `/agent-runs`; missing Unchanged; missing synthetic/LOW/sandbox labels; SQL/curl by the tester.

---

## 64. Network Boundary Acceptance Test

During section 63, inspect `snip-web` network log (Playwright `page.on('request')` or equivalent):

**Must not request:**

- `/mcp`
- `/api/v1/agent-runs`
- `/api/v1/production-changes`
- `/api/v1/production-campaigns`
- write-gateway `/execute`
- vendor hosts

**May request:** `/api/v1/sites|cells|gnbs|assurance|twins|planning|change-intelligence|change-planning|change-execution|recommendations|health`

---

## 65. Reset-and-Repeat Acceptance Test

```text
RESET → BOOTSTRAP → section 63 → RESET → BOOTSTRAP → section 63
```

Second run: no manual repair; previous IDs 404; CELL-001 story present.

---

## 66. Clean-Machine Acceptance Test

A person who did not implement the milestone follows **only** `docs/customer/SNIP-1.0-DEMONSTRATION-GUIDE.md` on a clean supported OS with Java/Maven/Node/Docker.

Pass: no undocumented SQL/curl/source edits; no Azure; no tribal Slack steps; section 28 + login page.

---

## 67. Documentation Acceptance

Reviewer checklist for A–C + README:

- no Phase 13–18 / PI3 / PI7 as customer product names
- no production-ready / autonomous / coupled RF / live vendor write claims
- synthetic/demo/sandbox/LOW explained
- next stage = read-only PoC
- UI = 5173
- reset guards documented

---

## 68. Workstreams

### WS1 — Demo data

- Files: `V21__snip_1_0_customer_demo_network.sql`; `DemoNetworkSeedTest`; possibly `AssuranceDetectionTest`
- BE yes / FE no / migrate **yes**
- Risk: test count assertions; knowledge import vs extra cells
- AC: section 16 counts; featured KPI trends; synthetic flags

### WS2 — Bootstrap / startup

- Files: `application-demo.yml`; `DemoBootstrapRunner`; `DemoProperties`; `DemoSafetyGuard`; `scripts/snip-demo-up.*`; `scripts/snip-demo-ready.*`
- BE yes / FE no / migrate no
- Depends on WS1; knowledge safety test
- AC: section 28

### WS3 — Reset

- Files: `scripts/snip-demo-reset.*`
- BE no (except guard tests) / FE no
- Depends on WS2
- AC: sections 31–33

### WS4 — UX terminology / guidance

- Files: listed in section 52
- FE yes / BE no
- AC: section 36–38, 43; frontend tests

### WS5 — Documentation

- Files: section 44
- AC: section 67

### WS6 — Automated qualification

- Files: new/updated tests; no CI redesign
- AC: section 56–62

### WS7 — Browser / customer acceptance

- Manual or future Playwright **outside** default CI
- Depends on WS1–WS6
- AC: sections 63–66

---

## 69. Implementation Order

```text
WS1 data + seed tests
 → knowledge-import safety test
 → WS2 demo profile + runner + up/ready scripts
 → WS3 reset scripts
 → WS4 UX
 → WS6 automated tests (continuous)
 → WS5 documentation
 → WS7 browser + reset-repeat + clean-machine
 → RC1
```

Do not write customer docs before ports/scripts exist.

---

## 70. Dependency Graph

```text
V21 ──────────────┐
                  ├─ DemoBootstrapRunner ─┬─ snip-demo-up ─ snip-demo-ready
Knowledge safety ─┘                       └─ snip-demo-reset
UX/docs independent after IDs stable
Browser acceptance after all of the above
```

---

## 71. Risk Register

| Risk | Mitigation | Verify |
|------|------------|--------|
| Demo looks like a toy | 6/6/18 + 4 cases | seed test + map |
| Demo looks like a real operator | DemoVendor, DEMO_SEED, banners, fictional names | grep + UI tests |
| Bootstrap too complex | 3 commands + script; no Kafka | clean-machine test |
| Reset outside demo | dual env guard + compose cwd | reset guard test |
| Calendar drift | NOW()-relative only | seed test months-logic (relative intervals) |
| Kafka nondeterminism | Kafka not used | demo yml kafka false |
| Stale twins | runner sync featured | ready check |
| Sandbox off | demo profile | ready + browser |
| Production flag on | DemoSafetyGuard | unit test |
| Leftover DB | volume remove | reset-repeat |
| Leftover Kafka | not started; optional telemetry down flag | guide |
| Duplicate simulator run IDs | simulator not on demo path | — |
| Wrong backend | SNIP_API_TARGET + ready | guide |
| Legacy :8080 UI | docs + overview | doc acceptance |
| Docs drift | update README in same milestone | checklist |
| AI overclaim | section 39 tests | terminology test |
| Twin overclaim | section 40 | planning truth tests remain |
| Simulation overclaim | section 41 | `FORBIDDEN_CAPABILITY_PHRASES` remain |
| Phase jargon | section 36 | customerTerminology test |
| Demo >45 min | live create plan/sandbox; no extra features | time the script once |
| Failed step recovery | guide troubleshooting; twins Synchronize; regenerate proposal | guide |
| ENM overlay adds SITE-SIM-001 | expected; V21 IDs unchanged; stories ignore overlay | 51.1 coexistence test |
| Knowledge UNKNOWN → EVALUATED only | fail bootstrap if not HIGH/MEDIUM | 51.2 primary-story test |
| Knowledge gone after reset | expected; bootstrap re-imports | reset-repeat |
| Compose folder-name project | scripts **must** `-p snip-demo` | reset ownership tests |
| Absent volume → prune | forbidden; exit 0 | reset safety tests |
| `seedCell001` test breaks | rewrite | Maven |
| Scheduler 15m surprises presenter | do not wait; runner triggers once; aging 30m still MEDIUM | bootstrap test |
| Same actor SoD | document demo headers | guide |
| Employer names in data | only DemoVendor / SITE-* | seed grep |
| busyCell unused | wire it | increment7 still passes |

---

## 72. Security Review

- No Azure/Key Vault/vendor secrets in V21 or scripts
- Reset cannot run without two explicit env values
- No demo readiness API; ready script uses existing GETs + local profile files
- Frontend still must not call `/mcp` or production-change
- Demo login remains acceptable
- Git author emails are not customer UI
- `application-demo.yml` must not contain production endpoints or credentials

---

## 73. Adversarial Review Checklist

| Attack | Specification response |
|--------|------------------------|
| Reset deletes another project's DB | Dual confirm + `snip-demo` project labels + inspect; fail closed |
| Compose project-name drift | Scripts always `-p snip-demo`; never default directory name |
| Absent target volume | Exit 0; no prune |
| Bootstrap enables production | DemoSafetyGuard; yml production-change false; test |
| Synthetic mistaken for real | Banners, DEMO_SEED, DemoVendor, map sentence; overlay named Sim Site |
| Fixed timestamps expire | Forbidden on demo path; NOW() only |
| Duplicate bootstrap corrupts | Idempotent evaluate/sync/import SKIP; fail if V21 IDs missing |
| Kafka timing | Kafka not used |
| Stale twins | Featured sync + UI Synchronize |
| Go KPI simulator leak | Not started on demo path |
| Old planning blocks unique names | Volume wipe removes rows |
| Browser hits :8080 static | Docs + guide; ready checks 5173 |
| Actor bypasses SoD | Documented demo behavior; do not claim IAM |
| AI/twin/vendor overclaim | Sections 39–41, 49; tests |
| Employer content in seed | Forbidden strings in dataset test |
| >45 minutes | Script budget; no extra workspaces |
| Failed Evaluate | Guidance + CURRENT twins pre-synced |
| Import deletes V21 cells | IDs do not collide; 51.1 must stay green |
| Import changes CELL-001 identity | Canonical `CELL-SIM-001` ≠ `CELL-001`; 51.1 |
| Import invalidates V21 neighbours | Page-1 overlay has no V21 relations; 51.1 |
| Import changes DEMO_SEED provenance | Import does not rewrite KPI rows; 51.1 |
| CELL-001 only EVALUATED | Fail bootstrap + 51.2 requires RECOMMENDED |
| Trusted knowledge disappears after reset | Re-bootstrap re-runs triggerManual |

---

## 74. Release Candidate Criteria

**SNIP 1.0 RC1** when:

- Implementation on a descendant of `f0f983db…`
- Maven, Go, frontend test, frontend build PASS
- Bootstrap + ready check PASS on a clean volume
- Reset guard PASS
- Automated dataset/bootstrap/safety/terminology tests PASS
- Production defaults in `application.yml` still disabled
- Customer docs exist (may still need a timed dry-run)

RC1 is **not** “customer demonstration ready” until section 75.
RC1 is **not** production-ready.

Proposed tag **after** RC1 acceptance (do not create in this task): `snip-v1.0.0-rc1`.

---

## 75. SNIP 1.0 Promotion Criteria

Promote RC1 to **SNIP 1.0 CUSTOMER DEMONSTRATION READY** only when:

1. Section 63 browser journey passes
2. Section 64 network boundary passes
3. Section 65 reset-and-repeat passes
4. Section 66 clean-machine passes
5. Exact-SHA CI on the implementation commit is SUCCESS
6. No CRITICAL/BLOCKING findings (section 83)

This promotion is **demonstration readiness**, not production authorization.

Proposed later tag (not now): `snip-v1.0.0-demo`.

---

## 76. Definition of Done

SNIP 1.0 Customer Demonstration Readiness is complete when:

1. Implementation exists on a clean baseline
2. All automated regression suites pass
3. Demo bootstrap works from clean state
4. Demo reset works
5. Bootstrap works again after reset
6. Dataset meets section 16
7. Three curated stories are available (1 via case+eligibility; 2–3 via inventory)
8. No customer-visible PI/increment jargon on the demo path
9. Documentation matches implementation
10. No SQL/curl required during the normal customer demo
11. Complete browser journey passes
12. Browser journey invokes no production mutation path
13. Synthetic / LOW / sandbox labels remain visible
14. Real network remains unchanged
15. Committed production execution defaults remain disabled
16. Exact-SHA CI succeeds
17. Full demo succeeds after reset
18. Full demo succeeds a second time after reset

---

## 77. Out-of-Scope / Post-1.0 Register

Coupled RF; site/network twin; extra radio parameters; real vendor writes; Nokia/Huawei production; campaigns UI; agents UI; MCP UI; closed-loop; Phase 19; customer IAM; nationwide scale; RF heatmaps; corpus expansion as a 1.0 blocker.

---

## 78. Implementation File Impact Forecast

| Path | Class |
|------|--------|
| `snip-npo-app/src/main/resources/db/migration/V21__snip_1_0_customer_demo_network.sql` | CREATE |
| `snip-npo-app/src/main/resources/application-demo.yml` | CREATE |
| `snip-npo-app/src/main/java/com/simba/snip/npo/demo/DemoBootstrapRunner.java` | CREATE |
| `snip-npo-app/src/main/java/com/simba/snip/npo/demo/DemoSafetyGuard.java` | CREATE |
| `snip-npo-app/src/main/java/com/simba/snip/npo/demo/DemoProperties.java` | CREATE |
| `.../api/DemoReadinessController.java` | **DO NOT CREATE** |
| `snip-npo-app/src/test/java/com/simba/snip/npo/demo/*` | CREATE (including 51.1, 51.2, reset-guard tests) |
| `AssuranceDetectionTest.java` | MODIFY |
| other ITs with V2-only counts | MODIFY if they fail |
| `application.yml` | UNCHANGED (defaults) |
| `scripts/snip-demo-up.ps1` `.sh` | CREATE |
| `scripts/snip-demo-reset.ps1` `.sh` | CREATE |
| `scripts/snip-demo-ready.ps1` `.sh` | CREATE |
| `snip-web/src/layouts/AppShell.tsx` | MODIFY |
| `snip-web/src/pages/LoginPage.tsx` | MODIFY |
| `snip-web/src/pages/AiPage.tsx` | MODIFY |
| `snip-web/src/pages/NotFoundPage.tsx` | MODIFY |
| `snip-web/src/pages/AssuranceCasePage.tsx` | MODIFY |
| `snip-web/src/pages/PlanningScenarioPage.tsx` | MODIFY |
| `snip-web/src/pages/PlanningCreatePage.tsx` | MODIFY |
| `snip-web/src/pages/ChangePlanPage.tsx` | MODIFY |
| `snip-web/src/features/planning/PrerequisitePanel.tsx` | MODIFY |
| `snip-web/src/features/optimization/DemoPermissionBanner.tsx` | MODIFY |
| `snip-web/src/pages/NetworkPage.tsx` or `SiteMap.tsx` | MODIFY (map sentence) |
| `snip-web/src/pages/customerTerminology.test.tsx` | CREATE |
| increment/pages tests | MODIFY as needed |
| `docs/customer/SNIP-1.0-PRODUCT-OVERVIEW.md` | CREATE |
| `docs/customer/SNIP-1.0-DEMONSTRATION-GUIDE.md` | CREATE |
| `docs/customer/SNIP-1.0-LIMITATIONS-AND-TRUTH-STATEMENT.md` | CREATE |
| `README.md` | MODIFY |
| `snip-web/README.md` | MODIFY |
| `docs/implementation/SNIP-IMPLEMENTATION-STATUS.md` | MODIFY |
| `.github/workflows/ci.yml` | UNCHANGED unless a trivial path check is justified |
| `docker-compose.yml` | UNCHANGED (project identity via `docker compose -p snip-demo`, not folder name) |
| `simulator/**` | UNCHANGED |
| V1–V20 | UNCHANGED |
| PI7 architecture/specification | UNCHANGED |
| this specification | already CREATE (uncommitted) |

---

## 79. Open Questions

**None remain open.**

### OQ-1 — RESOLVED

**Status:** RESOLVED  
**Choice:** **A — EXISTING ENM SIMULATOR IMPORT** (`SynchronizationControlPlane.triggerManual` + `FULL_SUCCESS` + `TRIGGER_VENDOR_IMPORT`).  
**Evidence:** knowledge is source-scoped; simulator canonical IDs are `SITE-SIM-001` / `CELL-SIM-001`; MISSING does not delete cells; `ChangeIntelligenceApiTest` already reaches `RECOMMENDED` for CELL-001 after this import.  
**Tests:** §51.1 coexistence, §51.2 primary story.  
**Failure:** bootstrap fails closed if knowledge is UNKNOWN/LOW. No fake HIGH.

### OQ-2 — RESOLVED

**Status:** RESOLVED  
**Choice:** script + existing GET `/health`, inventory, assurance, twins, and `GET /api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT` with `VIEW_SYNCHRONIZATION_STATUS`. Profile/sandbox flags from local files.  
**Do not add** `GET /api/v1/demo/readiness`.

---

## 80. Final Implementation Recommendation

Implement this specification as **SNIP 1.0 — Customer Demonstration Readiness**.

Do **not** open PI8. Do **not** start Phase 19. Do **not** enable production execution. Do **not** start Kafka for the customer demo.

Smallest coherent package: V21 synthetic network + demo profile runner (mandatory ENM-simulator knowledge import + featured twins + detector-backed Assurance) + local up/reset/ready scripts with `snip-demo` Compose ownership + terminology/guidance + customer/developer docs + tests + timed browser acceptance.

If implementation begins to resemble a new product increment, **stop**.

---

## Appendix A — Featured constants (copy into DemoProperties)

```text
FEATURED_ASSURANCE_CELLS=CELL-001,CELL-007,CELL-010,CELL-014
FEATURED_TWIN_CELLS=CELL-001,CELL-002,CELL-003,CELL-007
FEATURED_OPTIMIZE_CELL=CELL-001
FEATURED_PLANNING_CELLS=CELL-001,CELL-002
PRIMARY_PERSONA=demo-rf-optimisation
AUTHORITATIVE_UI=http://127.0.0.1:5173
COMPOSE_PROJECT_NAME=snip-demo
COMPOSE_VOLUME=snip-demo_snip-postgres
KNOWLEDGE_CONNECTOR=ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER
SIMULATOR_SCENARIO=FULL_SUCCESS
```

## Appendix B — Phase 19

```text
PHASE_19: NOT_STARTED
```

This milestone must not create Phase 19 documents, packages, or APIs.
