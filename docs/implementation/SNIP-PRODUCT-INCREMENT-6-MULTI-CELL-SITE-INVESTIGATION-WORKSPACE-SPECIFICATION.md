# SNIP Product Increment 6 — Multi-Cell & Site Investigation Workspace

## Implementation specification

**Status:** AUTHORIZED FOR IMPLEMENTATION (specification only; implementation not started)

**Mode:** FRONTEND_ONLY

**Assurance level:** STANDARD_PRODUCT_ASSURANCE

**Authoritative title:** Multi-cell & site investigation workspace

Do **not** title or market PI6 as:

- coordinated multi-cell optimization
- site optimization engine
- network optimizer
- site-aware ranking
- multi-cell simulation

---

## 1. Authority and baseline

Parent baseline (frozen PI5, exact-SHA verified):

```text
Branch: main
Commit: 7615a0c908689a5dfd8fb387acca324aae2e6ec1
Product Increment 5: CLOSED
Product Increment 4 parent: 8e602c70e63689700660f1aad51593ca45b51cae
Architecture impact: FRONTEND_ONLY
Required backend enablers: 0
New persistence: NO
Database migration: NO
New simulation model: NO
New optimization algorithm: NO
New agent: NO
New MCP capability: NO
Production write capability: NO
Site-aware ranking: NO
Multi-cell scenario: NO
Cross-cell simulation: NO
Multi-cell execution: NO
Phase 19: NOT STARTED
```

Readiness review decision: `READY_FOR_PI6_SPECIFICATION`.

This document is the authorised implementation specification for Product Increment 6. Implement exactly this scope. Do not start Phase 19. Do not implement production write, vendor transport, new agents, MCP, Model Gateway, a telecom LLM, ML/RL, a site Digital Twin, a cross-cell simulator, or a generic network-health model.

If implementation discovers that a Java/backend contract change is required: **STOP** and report `BACKEND_CONTRACT_REVIEW_REQUIRED`. Do not add backend scope silently.

If implementation discovers that PI6 would require mutation authority, agent-runs, MCP, ranking changes, or multi-cell execution: **STOP** and report `SECURITY_BOUNDARY_REVIEW_REQUIRED`.

---

## 2. Product objective

PI3 answers: how may this **one cell** change `txPower` under existing governance?

PI4 answers: what needs attention, and where is it?

PI5 answers: what observed evidence and investigation guidance exist for this Assurance finding?

PI6 answers, without increasing optimization or mutation authority:

- Which cells are related to the selected cell?
- Which of those are same-site, same-gNB, or configured neighbours?
- How do txPower, KPI, and active Assurance differ among them?
- Which **one** cell should the engineer take into the existing PI3 Optimize workflow?

Authoritative primary journey:

```text
Demo Login
  → Network Operations (/network)
  → active Assurance finding
  → Open case
  → /assurance/:caseId (PI5 investigation)
  → Open affected cell
  → /network/cells/:cellId
  → related-cell investigation context
  → compare same-site / same-gNB / configured neighbours
  → inspect txPower / KPI / Assurance differences
  → explicitly choose ONE cell
  → existing /network/cells/{cellId}/optimize (PI3)
  → STOP
```

Authoritative secondary journey:

```text
Demo Login
  → Network
  → Site
  → /network/sites/:siteId
  → site cell comparison
  → Open cell
  → related-cell context
  → existing PI3 Optimize
  → STOP
```

PI6 is **investigation/comparison**. PI3 remains **optimization**. PI6 acceptance **stops** when the existing Optimize workspace is reached. Do not generate, approve, plan, or execute a change.

---

## 3. Architecture decision

```text
ARCHITECTURE_IMPACT: FRONTEND_ONLY
REQUIRED_BACKEND_ENABLERS: 0
NEW_PERSISTENCE: NO
DATABASE_MIGRATION: NO
NEW_SIMULATION_MODEL: NO
NEW_OPTIMIZATION_ALGORITHM: NO
NEW_AGENT: NO
NEW_MCP_CAPABILITY: NO
PRODUCTION_WRITE_CAPABILITY: NO
```

PI6 is a presentation, relationship-derivation, and bounded-loading increment over existing contracts:

- `GET /api/v1/sites`
- `GET /api/v1/sites/{siteId}`
- `GET /api/v1/gnbs`
- `GET /api/v1/cells`
- `GET /api/v1/cells/{cellId}/context`
- `GET /api/v1/assurance/cases`

Reuse already-loaded selected-cell context on CellPage. Do not add query parameters, persistence, aggregation endpoints, ranking inputs, or new REST resources.

If the accepted frontend-only design cannot be implemented correctly against these contracts: **STOP**.

---

## 4. Assurance level

**STANDARD_PRODUCT_ASSURANCE**

PI6 is read-only engineering comparison of existing canonical observations. It does not change ranking, simulation, proposal, plan, or execution semantics.

Elevated labelling discipline still applies:

- do not present neighbours as interference
- do not present comparison as simulated cross-cell effects
- do not present ranking as site- or network-optimal
- do not invent health scores
- do not invent RF geometry

If implementation attempts to change ranking, simulation, proposal, or mutation semantics: **STOP**. That exceeds PI6.

---

## 5. Truth boundary (authoritative)

| Domain | Current maximum | PI6 may claim | PI6 must not claim |
|---|---|---|---|
| Digital Twin | CELL-scoped only | Cell Digital Twin (if shown later) | Site Digital Twin; multi-cell twin |
| Synthetic simulation | isolated cell; `snip.synthetic.cell-parameter.v1` | existing PI3 cell simulation after handoff | cross-cell / neighbour / site simulation |
| Phase 13 | one CELL + one `txPower` | Ranked txPower for this cell | site-optimal / multi-cell-optimal |
| Phase 14 | one approved proposal / one forward operation | existing PI3 plan after operator chooses | multi-cell plan |
| Phase 15 | one cell / one operation / SIMULATOR | existing sandbox after PI3 | multi-cell sandbox |
| P16/P17 | 1/1/1 per child | unused | production execution |
| P18 | sequential children; campaign ≠ mutation authority | unused | campaign mutation |

Forbidden product language:

- Changing CELL-A will improve CELL-B
- Changing CELL-A reduces neighbour interference
- Changing CELL-A redistributes load
- multi-cell optimization
- coordinated optimization
- site optimization engine
- network optimizer

---

## 6. Hierarchy semantics

Preserve:

```text
Site → gNB → Cell
```

Neighbours are **separate directed relationships** (`source → target`). Site → Cell is **derived** through gNB and already exposed as `CellDto.siteId`.

Do not introduce region, market, province, cluster, or sector.

Relationship categories that PI6 must keep distinguishable:

| Category | Derivation | Authority |
|---|---|---|
| Selected cell | route `cellId` | canonical |
| Same-site cells | `listCells` where `siteId` equals selected `siteId` | API-derived |
| Same-gNB cells | `listCells` where `gnbId` equals selected `gnbId` | stored |
| Configured neighbours | selected `context.neighbours[]` `targetCellId` | stored, **outgoing only** |
| Cross-site neighbour | neighbour whose `siteId` (from inventory) ≠ selected `siteId` | derived |

A related cell may carry **more than one** label (for example same site **and** same gNB **and** configured neighbour). Show all applicable labels. Do not collapse them into one generic “related” type as the only information.

Do **not** imply:

- same-site = neighbour
- neighbour = interference
- neighbour = coverage overlap
- neighbour = handover quality
- source→target is bidirectional

Do not invent reverse-neighbour rows. If the seed also stores the reverse edge as its own row, it may appear when that cell is selected. That is stored direction, not inferred symmetry.

---

## 7. Entry points and routes

Enhance existing pages. Do **not** add:

- `/optimization/site/:siteId`
- `/network/sites/:siteId/optimize`
- `/investigation/related`
- any competing optimization route

| Entry | Route | Page | PI6 addition |
|---|---|---|---|
| PI5 handoff | `/network/cells/:cellId` | `CellPage` | related-cell investigation section |
| Site workspace | `/network/sites/:siteId` | `SitePage` | bounded site-cell comparison |
| Optimization | `/network/cells/:cellId/optimize` | `OptimizePage` | **unchanged PI3** |

Neighbour target navigation uses the existing CellPage route when the target exists in `listCells`.

---

## 8. Related-cell model (CellPage)

Enhance `CellPage` with a bounded **Related radio context** section. Preserve existing selected-cell identity, configuration, KPIs, neighbours table, Assurance list, Network knowledge, Ask SNIP, and PI3 Optimize control.

### 8.1 Required groups

Present, and keep distinguishable:

1. Selected cell
2. Same-site cells
3. Same-gNB cells
4. Configured neighbours

Authorised labels (or equivalent):

- Selected cell
- Same site
- Same gNB
- Configured neighbour
- Cross-site neighbour (when derivable)

### 8.2 Related-cell identity set

Build a de-duplicated identity set:

1. Selected `cellId`
2. All inventory cells with the same `siteId`
3. All inventory cells with the same `gnbId`
4. Configured neighbour `targetCellId` values that exist in `listCells`

Unknown neighbour targets: show the id/relation/status from the neighbour DTO, **do not** fabricate a CellPage link, and **do not** fetch context for an unknown id.

### 8.3 Neighbour navigation

Current `NeighbourPanel` does not navigate. PI6 must add a CellPage link for a configured neighbour **only when** that `targetCellId` exists in canonical inventory (`listCells`).

Copy remains relationship language, not interference language.

### 8.4 Comparison columns on CellPage

A related-cell row may include actual available fields only:

| Column | Source | Missing |
|---|---|---|
| Cell | `CellDto` | — |
| gNB | `CellDto.gnbId` / `GnbDto` | Unavailable |
| Relationship | derived labels | — |
| txPower | `context.radioConfiguration` via existing `findTxPower` | Unavailable |
| BLER | context KPI/telemetry `BLER_DL` | Unavailable |
| PRB | context KPI/telemetry `PRB_UTILIZATION_DL` | Unavailable |
| Assurance | active case join (PI4 semantics) | no active Assurance finding |
| Synthetic | context `provenance.synthetic` or observation `synthetic` | — |

Do not invent zeros. Do not require Digital Twin columns in P0.

Other radio/inventory fields (`ssbPower`, `tilt`, `band`, `PCI`, `ARFCN`, `bandwidth`) may appear as **configuration**, never as optimization candidates unless PI3 already supports that parameter. PI3 supports **txPower only**.

---

## 9. Site comparison model (SitePage)

Preserve existing SitePage:

- site metadata and coordinates
- associated gNBs
- associated cells
- PI4 attention rollup (cells needing attention, active case count, highest active severity)
- Open cell

Add a bounded **Site cell comparison** surface for cells where `cell.siteId === siteId`.

This is **not** a Site Digital Twin and **not** a site health score. Attention rollup remains PI4 Assurance attention.

Comparison columns follow section 8.4 without a selected-cell relationship column, unless the operator arrived with a selected cell (not required for P0). Site comparison rows are site cells only. Do not pull the whole network.

Each row’s engineering action is **Open cell** and, where `txPower` is present, an explicit **Propose txPower optimization** link to the existing PI3 route. The operator must click. Do not auto-generate proposals for every site cell.

---

## 10. Configuration comparison

`txPower` is the only currently supported optimization parameter.

| Field | Display | Optimization implication |
|---|---|---|
| txPower | yes, with unit dBm when present | may hand off to PI3 |
| ssbPower, tilt | optional if already in radioConfiguration | configuration only |
| band, PCI, ARFCN, bandwidth, technology | optional from `CellDto` | inventory/configuration only |

Authorised wording: **configuration**. Forbidden: **optimization candidate** for any parameter other than txPower in a PI3 handoff control.

---

## 11. KPI and telemetry comparison

Use actual per-cell metrics from bounded `getCellContext` payloads.

Relevant metrics that **may** exist:

`BLER_DL`, `BLER_UL`, `DROP_RATE`, `THROUGHPUT_DL`, `THROUGHPUT_UL`, `LATENCY`, `PRB_UTILIZATION_DL`, `PRB_UTILIZATION_UL`

P0 comparison should prefer `BLER_DL` and `PRB_UTILIZATION_DL` because they are the metrics currently used by Assurance detection and Phase 13 benefit. Other metrics may appear if already present on the loaded context.

Rules:

- do not assume every metric exists for every cell
- do not fabricate zeros for unavailable values
- show **Unavailable** (or equivalent) for absent data
- respect `observedAt` / `eventTime`, `source`, `synthetic`, and `trend`
- do not describe historical observations as a live stream
- do not create an unbounded telemetry wall

P1 may add a compact trend/history disclosure per compared cell using the already-loaded context `telemetry` series (`telemetry-history-n` is already bounded by the backend). Do not add extra telemetry endpoints.

---

## 12. Assurance correlation

Preserve PI4 semantics exactly.

```text
ACTIVE = OPEN or ACKNOWLEDGED
RESOLVED = inactive
severity order = CRITICAL > MAJOR > WARNING > INFO
```

A comparison row may show the highest **active** severity for that cell, or **no active Assurance finding**.

Reuse existing operations helpers (`isActiveAssuranceCase`, `severityRank`, labels) rather than inventing a new model.

Forbidden:

- site health score
- cell health score
- network health score
- `sourceHealth` as cell/site health
- Twin freshness as cell/site health

Join using `listAssuranceCases` once plus `affectedEntityType === 'CELL'` and `affectedEntityId`. Do not call `getAssuranceForCell` for every comparison row. Do not call `/assessment` for any comparison row.

---

## 13. Synthetic-data handling

If cell context provenance is synthetic, or a displayed KPI/telemetry/config observation is synthetic, communicate that truthfully.

Authorised wording (or equivalent):

> Synthetic demo observation — not live network data.

Do not imply Ericsson production provenance, vendor network provenance, or live OSS/NMS/EMS measurement.

---

## 14. Digital Twin boundary

Digital Twin is **cell-scoped only**.

P0 must not display a Site Digital Twin, Multi-cell Digital Twin, or composed “site twin” assembled from cell twins.

Frontend currently exposes twin **synchronize** (`POST`), not a cell-scoped twin **read**. PI6 must **not** call synchronize from comparison. Therefore **P0 and P1 must not add twin columns** unless a read-only cell-twin lookup is already wired without new Java. That lookup is not in `snipApi` today. Twin provenance remains **deferred / P2** for the comparison table.

Existing CellPage Network knowledge panel may remain. It is source-scoped Phase 12 knowledge, not a site twin.

---

## 15. Simulation boundary

Current model `snip.synthetic.cell-parameter.v1` is isolated-cell only. Assumption in code: neighbour coupling is not modelled.

PI6 comparison data are **observed/canonical**, not simulated cross-cell effects.

Do not say that changing CELL-A will improve CELL-B, reduce neighbour interference, change handover, or redistribute load.

PI3 may still run isolated-cell simulation **after** the operator enters Optimize. That simulation remains cell-local. PI6 must not preview it as a multi-cell result.

---

## 16. Ranking boundary

Phase 13 ranking is **CELL_LOCAL**.

PI6 must not:

- modify ranking
- feed neighbour or site KPIs into candidate scoring
- label existing ranking site-optimal, network-optimal, or multi-cell-optimal

Safe label remains equivalent to: **Ranked txPower for this cell.**

---

## 17. PI3 handoff

PI6 ends by letting the operator **explicitly** choose one cell and enter:

```text
/network/cells/{cellId}/optimize
```

Rules:

- the operator must click an existing-style Optimize control
- do not auto-generate proposals
- do not optimize every related cell
- do not generate multiple proposals from the comparison table
- do not batch `POST /api/v1/change-intelligence/proposals`

Preserve the existing CellPage Optimize control for the selected cell. Related rows may offer the same control for **that row’s cell** only.

---

## 18. PI4 preservation

Do not change:

- Needs attention semantics
- ACTIVE = OPEN or ACKNOWLEDGED
- RESOLVED = inactive
- severity order
- operational summary counts
- map attention
- issue-queue ordering
- Network Operations N+1 boundary

`NetworkPage` must continue to call only list APIs (`sites`, `cells`, `gnbs`, `assurance/cases`). Do not attach per-cell context fetching to Network Operations.

---

## 19. PI5 preservation

Do not destabilize `/assurance/:caseId`.

Preserve:

- independent case vs assessment loading
- evidence-first investigation order
- generator-neutral SNIP analysis wording
- Open cell to `/network/cells/{affectedEntityId}` for CELL cases

PI6 begins **after** Open cell. Do not add related-cell context fetching to the Assurance case page. Do not add assessment calls to comparison.

---

## 20. Request strategy

Existing APIs are sufficient. No aggregation endpoint.

### 20.1 Allowed in the engineering workspace

CellPage (focused workspace):

| Call | Why |
|---|---|
| `getCellContext(selected)` | already required; selected-cell authority |
| `getAssuranceForCell(selected)` | already required; selected-cell Assurance list |
| `listCells` | same-site / same-gNB / neighbour-target existence |
| `listGnbs` | optional labels; SitePage already uses it |
| `listAssuranceCases` | one-shot active-finding join for related cells |
| bounded extra `getCellContext(id)` | KPI/txPower for related ids not already loaded |

SitePage:

| Call | Why |
|---|---|
| `getSite`, `listCells`, `listGnbs`, `listAssuranceCases` | already required |
| bounded `getCellContext` for site cells | comparison columns |

### 20.2 Forbidden

- per-cell context/KPI/telemetry/assurance/twin on `/network`
- `/assessment` per comparison row
- `/api/v1/agent-runs`
- MCP
- production-change / production-campaign
- sandbox execution from PI6 surfaces
- `POST /api/v1/change-intelligence/proposals` from PI6
- twin synchronize from PI6 comparison

### 20.3 Deterministic bound

Demo seed related-cell cardinality is small (3 cells). PI6 must still define a guard so comparison cannot become a network-wide context crawl.

```text
RELATED_CELL_CONTEXT_LIMIT = 12
```

After building the de-duplicated related (or site-cell) identity set, fetch context for at most 12 cells, using this priority:

1. selected cell (already loaded on CellPage)
2. same-gNB cells
3. remaining same-site cells
4. configured neighbour targets that exist in inventory

If the identity set exceeds 12, show an explicit bounded notice equivalent to:

> Related-cell context is limited to 12 cells in this workspace.

Do not invent backend pagination. Do not silently drop neighbours without stating the limit.

Independent loading: failure of related-cell context must not hide the selected cell (CellPage) or site inventory (SitePage). Show **Unavailable** in comparison columns.

---

## 21. Map and geometry

Inventory has site latitude/longitude only. There is no authoritative cell azimuth, sector polygon, coverage footprint, or RF propagation shape.

Do **not** create a fake RF coverage map, sector geometry, or neighbour-interference overlay.

Existing site-point `SiteMap` on Network Operations may remain unchanged. Comparison tables/cards are the PI6 geometry.

---

## 22. AI, agents, and MCP

No new AI architecture.

Existing CellPage Ask SNIP and PI5 case assessment may continue where they already exist. PI6 comparison itself is canonical data, not LLM output.

AI must not:

- rank multi-cell changes
- simulate cross-cell effects
- choose the optimization target automatically
- trigger PI3

Do not activate agents. No `/api/v1/agent-runs`. No new MCP. Do not route comparison through MCP.

---

## 23. Security and authority

PI6 is read-only.

No investigation or comparison interaction may:

- acknowledge or resolve Assurance
- modify configuration or txPower
- create, approve, or reject a proposal
- create, review, authorize, or execute a plan
- request or execute sandbox
- mint a production grant
- invoke Production Write Gateway
- release, resume, or rollback a campaign
- invoke mutation-capable MCP
- execute a production change

---

## 24. P0 / P1 / P2

### P0 (required)

1. Related-cell section on CellPage
2. Selected-cell identity
3. Same-site cells
4. Same-gNB cells
5. Configured neighbours
6. Relationship labels, kept distinguishable
7. Neighbour target navigation when the target exists
8. SitePage comparison
9. txPower comparison
10. Bounded KPI comparison (`BLER_DL`, `PRB_UTILIZATION_DL` at minimum when present)
11. Bounded Assurance comparison with PI4 active-state semantics
12. Synthetic labelling
13. Unavailable-data handling (no fabricated zeros)
14. One-cell Optimize handoff
15. No health score
16. No fake RF geometry
17. No cross-cell simulation claims
18. No site-aware ranking claims
19. No automatic mutation / no proposal batch
20. PI3, PI4, and PI5 semantics preserved
21. Bounded related-cell context loading
22. No context N+1 on Network Operations
23. No assessment-per-row

### P1 (optional; defer if it threatens P0)

- compact telemetry trend/history from **already loaded** context
- relationship filter
- comparison sorting (for example severity, then cell id)
- neighbour-panel copy aligned with related-cell labels

Do **not** add twin columns in P1 (no read-only cell-twin lookup is wired; synchronize is forbidden here).

### P2 (defer)

Site-aware ranking; site Digital Twin; multi-cell scenario; cross-cell simulator; interference / handover / load-redistribution models; multi-cell proposal; multi-proposal plan; multi-cell sandbox; production campaign integration; backend aggregation endpoint; related-cell Ask SNIP; N+1 assessment; electricalTilt optimization.

---

## 25. Operator copy (authorised)

| Concept | Authorised wording |
|---|---|
| Workspace | Related radio context / Site cell comparison |
| Selected | Selected cell |
| Same site | Same site |
| Same gNB | Same gNB |
| Neighbour | Configured neighbour |
| Cross-site neighbour | Cross-site neighbour |
| Missing metric | Unavailable |
| No active case | No active Assurance finding |
| Synthetic | Synthetic demo observation — not live network data. |
| Config fields | Configuration |
| PI3 control | Propose txPower optimization |
| Ranking (unchanged PI3) | Ranked txPower for this cell |
| Twin (if ever shown) | Cell Digital Twin / Twin freshness |
| Limit | Related-cell context is limited to 12 cells in this workspace. |

Forbidden primary copy: interference, coverage overlap, handover quality, site health, cell health, network health, site Digital Twin, coordinated optimization, multi-cell simulation, site-optimal, network-optimal.

Raw internal codes, REST paths, Java names, Phase numbers, and permission constants must not be primary product language. Inventory codes (`INTRA_FREQUENCY`, `CELL-001`) may appear as secondary identifiers.

---

## 26. File boundary (expected)

Predominantly `snip-web`. No Java unless this spec is proven wrong.

**Modify (expected)**

- `snip-web/src/pages/CellPage.tsx`
- `snip-web/src/pages/SitePage.tsx`
- `snip-web/src/features/cell/NeighbourPanel.tsx` — target navigation when inventory contains the target
- `snip-web/src/index.css` — bounded comparison styles only if needed

**New (suggested; names may follow repo conventions)**

- `snip-web/src/features/network/` pure helpers: same-site / same-gNB / neighbour derivation, relationship labels, related-id bound, comparison-row mapping
- `snip-web/src/pages/increment6.test.tsx`
- helper unit tests beside the pure functions

**Reuse unchanged**

- `NetworkPage` request set, operations model active/severity semantics, `OptimizePage`, `AssuranceCasePage`, `AskSnip`, Phase 13–15 APIs, all Java
- `findTxPower`
- `issueTypeLabel` / `issueSeverityLabel` / `isActiveAssuranceCase`

Do not modify `snipApi.ts` except to call **existing** GETs already defined (`listCells`, `listGnbs`, `listAssuranceCases`, `getCellContext`). Do not add twin synchronize to PI6. Do not add a new REST client method that implies a new backend resource.

Do not change `SiteMap` to draw sectors or RF footprints.

---

## 27. Test plan

Use existing Vitest + Testing Library + `MemoryRouter` + mocked `fetch`. Do not add a Playwright platform to the product.

Frontend tests must prove:

1. Same-site cells identified correctly
2. Same-gNB cells identified correctly
3. Neighbour cells identified correctly
4. Cross-site neighbour remains distinguishable
5. Directed neighbour semantics preserved (no invented reverse edge)
6. Neighbour existence is not labelled interference
7. txPower comparison
8. KPI comparison
9. Unavailable KPI does not become `0`
10. Assurance active-state semantics preserved (OPEN/ACKNOWLEDGED active; RESOLVED inactive)
11. No site health score
12. No cell health score
13. Synthetic labelling
14. Neighbour target navigation when the target exists
15. No neighbour navigation when the target is unknown
16. Selected-cell distinction
17. Site comparison renders
18. One-cell Optimize handoff (`/network/cells/{id}/optimize`)
19. No automatic proposal generation
20. No Phase 13 batch POST
21. No `/api/v1/agent-runs`
22. No MCP
23. No production-change
24. No production-campaign
25. No sandbox execution from PI6
26. No context N+1 on Network Operations
27. No fake sector/coverage geometry
28. PI5 Open cell handoff preserved
29. PI3 Optimize route preserved
30. Related-context failure does not hide selected cell / site inventory
31. Context-limit notice if the related set exceeds 12 (fixture-driven)

Preserve all existing frontend tests. Update NeighbourPanel tests only for authorised navigation.

No Java tests are expected.

---

## 28. Browser acceptance

Use the isolated SNIP runtime as in PI4/PI5. Do not disturb unrelated WAODN. Do not assume generator/retrieval mode beyond what is safely observable; PI6 comparison does not depend on the LLM.

Journey:

1. Demo Login
2. Network Operations
3. Open an active Assurance case
4. Confirm PI5 investigation still evidence-first
5. Open affected cell
6. Verify related-cell context
7. Inspect same-site / same-gNB / configured-neighbour labels
8. Confirm a cross-site neighbour is distinguishable if present (demo: CELL-001 → CELL-003)
9. Compare txPower
10. Compare KPI (Unavailable where absent; never a fabricated zero)
11. Compare Assurance (no health score)
12. Navigate to a related cell and back if the target exists
13. Choose one cell
14. Existing Optimize route
15. **STOP**

Do not generate a proposal. Do not approve. Do not create a plan. Do not execute sandbox. Do not invoke production functionality.

---

## 29. Truthfulness acceptance

Browser acceptance must also prove:

- no multi-cell optimization claim
- no cross-cell simulation claim
- no interference inference from neighbour relationship
- no health score
- no fake RF geometry
- selected cell vs related cells is clear
- one-cell Optimize handoff is explicit
- synthetic/demo labelling where applicable

---

## 30. Failure matrix (minimum)

| Condition | Required behaviour |
|---|---|
| Selected context success + related inventory success | Full related-cell section |
| Selected context success + related context partial failure | Selected cell remains; failed rows Unavailable |
| Selected context success + listCells failure | Selected cell remains; related derivation unavailable |
| Site inventory success + site-cell context failure | Site metadata/gNBs/cells/attention remain; comparison metrics Unavailable |
| Neighbour target missing from inventory | Show relation; no Open cell / Optimize link |
| KPI absent on a loaded context | Unavailable; not 0 |
| No active Assurance on a row | No active Assurance finding |
| Related set > 12 | Load by priority; show limit notice |
| Network Operations | Still no per-cell context calls |

---

## 31. Non-goals

NO Phase 19; coordinated multi-cell optimization; multi-cell scenario; cross-cell simulation; interference model; handover model; load-redistribution model; site Digital Twin; site-aware ranking; multi-cell proposal; multi-cell plan; multi-cell sandbox execution; production execution; automatic remediation; closed-loop optimization; production campaign changes; new production transport; Nokia; Huawei; O-RAN; slicing; billing; OSS/BSS expansion; telecom LLM; Model Gateway; ML/RL; new agent; new MCP; fake RF coverage; fake sector geometry; network/site/cell health score; `/optimization/site/:siteId`; backend aggregation endpoint; assessment-per-row; electricalTilt optimization; PI3/P13–P15 governance bypass.

---

## 32. Implementation sequence

1. Pure relationship/comparison helpers and unit tests
2. Related-cell derivation (same-site, same-gNB, outgoing neighbours, cross-site)
3. Bounded context loading with independent failure
4. CellPage related-cell UI
5. Neighbour navigation when the target exists
6. SitePage comparison
7. Configuration / KPI / Assurance presentation
8. Synthetic and unavailable states
9. PI5 handoff regression
10. PI3 handoff regression
11. Network Operations N+1 regression
12. Frontend automated tests
13. Browser acceptance
14. Implementation review
15. Full repository qualification (`npm ci`, `npm test`, `npm run build`, `mvn -B test`, `go test ./...`)
16. Freeze only after acceptance (one commit, normal push, exact-SHA CI)

Do not freeze during specification. Do not implement during specification.

---

## 33. Freeze strategy

```text
Specification
  → implementation
  → implementation review
  → browser acceptance
  → final qualification
  → one baseline commit
  → normal push
  → exact-SHA CI verification
  → PI6 CLOSED
```

Do not amend PI5. Do not rewrite history.

---

## 34. Self-review against readiness evidence

| Check | Specification stance |
|---|---|
| Level A vs B vs C | PI6 is Level A investigation/comparison only |
| Isolated-cell simulator | Documented; no cross-cell claims |
| CELL_LOCAL ranking | Unchanged; no neighbour inputs |
| Frontend-only | 0 backend enablers; existing GETs only |
| SitePage natural entry | Enhanced; no new route |
| CellPage PI5 handoff | Related context after Open cell |
| Neighbour navigation | Added only for inventory-known targets |
| N+1 | Forbidden on `/network`; bounded in engineering workspace |
| Twin columns | Deferred; no synchronize from PI6 |
| PI3 coexistence | Explicit one-cell Optimize handoff |
| Governance | Pattern A only (select one cell → PI3) |

---

## 35. Decision after implementation

Implementers may report only:

`READY_FOR_PI6_IMPLEMENTATION_REVIEW`  
`PI6_IMPLEMENTATION_REQUIRES_CORRECTION`  
`BACKEND_CONTRACT_REVIEW_REQUIRED`  
`SECURITY_BOUNDARY_REVIEW_REQUIRED`  
`INSUFFICIENT_EVIDENCE`

Do not start Phase 19.
