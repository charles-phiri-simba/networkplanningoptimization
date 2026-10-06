# SNIP Product Increment 4 — Network Operations & Assurance Workspace

## Implementation specification

**Status:** AUTHORIZED FOR IMPLEMENTATION REVIEW (specification only; implementation not started)

**Mode:** FRONTEND_ONLY

**Assurance level:** STANDARD_PRODUCT_ASSURANCE

---

## 1. Authority and baseline

Parent baseline (frozen PI3, exact-SHA verified):

```text
Branch: main
Commit: ed10833d7ee186f97a1602aa065905c71e055090
Product Increment 3: CLOSED
CI: SUCCESS — run 37435243178
Architecture impact: FRONTEND_ONLY
Material backend capability gaps: 0
Small backend enablers: 0
New persistence: NO
Database migration: NO
Production write capability: NO
Phase 19: NOT STARTED
```

Readiness review decision: `READY_FOR_PI4_SPECIFICATION`.

This document is the authorised implementation specification for Product Increment 4. Implement exactly this scope. Do not start Phase 19. Do not implement production write, vendor transport, new agents, MCP, or a generic network-health model.

If implementation discovers that a Java/backend contract change is required: **STOP** and report `BACKEND_CONTRACT_REVIEW_REQUIRED`. Do not add backend scope silently.

If implementation discovers that PI4 would require new mutation authority: **STOP** and report `SECURITY_BOUNDARY_REVIEW_REQUIRED`.

---

## 2. Product objective

PI3 gives SNIP a deep governed optimization workflow once an engineer has already selected a cell.

PI4 solves the preceding discovery problem:

How does an engineer determine what needs attention across the network, understand why, investigate the evidence, locate the affected object, and enter the existing cell/optimization workflow?

Authoritative journey:

```text
Demo Login
  → Network Operations (/network)
  → operational summary
  → active Assurance issue queue
  → filter / prioritize
  → operational map
  → inspect Assurance case / evidence
  → affected site / cell
  → existing Cell workspace
  → existing Optimize workflow (PI3)
```

PI4 must **not** duplicate PI3. PI4 acceptance does **not** repeat sandbox execute/verify.

---

## 3. Authoritative operational semantics

### 3.1 Do not invent a network-health model

The repository has no authoritative HEALTHY / DEGRADED / CRITICAL roll-up for network, site, or cell.

Do not manufacture one in the frontend. Do not combine unrelated health semantics.

Keep these domains **separate**:

| Domain | Fields | PI4 use |
|---|---|---|
| Inventory | site / gNB / cell `status` (typically `ACTIVE`) | identity only; not operational attention |
| Assurance | `severity`, `status` | **sole** source of “needs attention” |
| Phase 12 sync | `sourceHealth`, knowledge confidence | **not** shown as site/cell/network health |
| Digital Twin | `freshness` | **not** shown on operations overview |
| Phase 16–18 | production / campaign / vendor health | out of scope |

Forbidden primary labels: Healthy, Degraded, Unhealthy, network health score.

### 3.2 Needs attention

**Needs attention** means the affected cell (and therefore its site) has at least one **active** Assurance case.

**Active** Assurance case:

```text
status ∈ { OPEN, ACKNOWLEDGED }
```

`RESOLVED` is inactive and must not contribute to:

- active case count
- critical active count
- cells needing attention
- map attention
- site roll-up attention

### 3.3 Prioritization (deterministic, no score)

Issue queue default order:

1. Active cases before inactive, when inactive are shown (P0 queue shows **active only**).
2. Severity descending: `CRITICAL` > `MAJOR` > `WARNING` > `INFO` > unknown (unknown last).
3. Then `lastObservedAt` descending (nulls last).
4. Stable tie-break: `id` ascending.

No opaque score. No AI ranking. Do not add a persisted priority field. Do not alter backend Assurance semantics.

### 3.4 Operator labels

| Raw code | Operator label |
|---|---|
| `DEGRADING_RADIO_QUALITY` | Degrading radio quality |
| `OPEN` | Open |
| `ACKNOWLEDGED` | Acknowledged |
| `RESOLVED` | Resolved |
| `CRITICAL` | Critical |
| `MAJOR` | Major |
| `WARNING` | Warning |
| `INFO` | Info |

Raw codes may appear only under **Technical details**. Do not leak phase numbers, REST paths, Java names, or permission constants in primary copy.

Only one detector/type currently exists. Do not imply a catalogue of issue types.

---

## 4. Existing contracts used

Compose these existing read APIs. **Do not add endpoints, query parameters, pagination, persistence, or migrations.**

| Call | Use |
|---|---|
| `GET /api/v1/sites` | site list, coordinates, inventory status, site count |
| `GET /api/v1/cells` | cell list, `siteId` join, cell count |
| `GET /api/v1/assurance/cases` | issue queue, evidence preview, attention derivation, case counts |

Optional, only if a visible gNB count remains on the landing page:

| Call | Use |
|---|---|
| `GET /api/v1/gnbs` | gNB count / site gNB tables (existing SitePage already uses this) |

Reuse existing detail routes without new fetches on the operations overview:

| Call / route | When |
|---|---|
| `GET /api/v1/assurance/cases/{caseId}` | existing AssuranceCasePage |
| `GET /api/v1/assurance/cases/{caseId}/assessment` | existing AssuranceCasePage |
| `GET /api/v1/cells/{cellId}/context` | existing CellPage only |
| `/network/cells/{cellId}/optimize` | existing PI3 OptimizePage |

### 4.1 Forbidden overview calls (N+1)

The Network Operations page **must not** call, for every cell or every site:

- `/cells/{id}/context`
- `/cells/{id}/kpis`
- `/cells/{id}/telemetry`
- `/cells/{id}/assurance`
- twin synchronize or twin GET
- network-knowledge source/state

Do not construct a KPI wall. Cell KPI/telemetry remain investigation-only after the operator opens a cell.

### 4.2 Completeness caveat

Client-computed totals are correct **only while these list APIs remain complete and unpaginated**. Current demo seed: 2 sites, 2 gNBs, 3 cells. If a future increment paginates lists, totals and aggregation **must be revisited**. Do not design PI4 as if browser-side national-scale aggregation will remain acceptable.

---

## 5. Network Operations landing page

Evolve **existing** `/network` (`NetworkPage.tsx`). Do not add a competing second landing route.

Retain route `/network` and primary nav label **Network** (or **Network Operations** as page heading; nav may stay “Network”).

Layout: reuse current workspace (side panel + map). Visual language unchanged. No design-system rewrite.

Bounded sections:

1. **Operational summary** (counts)
2. **Needs attention / Assurance issue queue** (filters + table)
3. **Network map** (existing SiteMap, attention styling)

Page heading: **Network operations**. Subtitle: operator language that this view shows inventory and **active Assurance findings**, not a network-health score.

---

## 6. Operational summary (P0)

Render only from successfully loaded lists. Never hardcode totals. Never invent percentages.

| Metric | Authoritative calculation | If source unavailable |
|---|---|---|
| Sites | `sites.length` | **Unavailable** — do not show `0` as authoritative |
| Cells | `cells.length` | **Unavailable** |
| Active Assurance cases | count cases with active status | **Unavailable** if Assurance request failed |
| Critical active cases | active and `severity === 'CRITICAL'` | **Unavailable** if Assurance request failed |
| Cells needing attention | distinct `affectedEntityId` among active cases whose `affectedEntityType === 'CELL'` | **Unavailable** if Assurance request failed |

Optional retained inventory count: gNBs from `gnbs.length` if that list is still loaded.

Empty vs unknown:

- Assurance loaded, zero active cases → show **0** and the empty-queue state. That zero is authoritative.
- Assurance **failed** → show **Unavailable** / unknown. **Never** render “0 critical cases” or “0 cells needing attention” as if measured.

Loading: existing `LoadingState` until the first successful partition arrives; then show loaded partitions immediately.

---

## 7. Assurance issue queue (P0)

Show **active cases only** by default.

### 7.1 Columns / fields

| Field | Source | Notes |
|---|---|---|
| Affected cell | `affectedEntityId` when type is `CELL` | Link to `/network/cells/{id}` |
| Site | join `cells.find(c => c.cellId === affectedEntityId)?.siteId` | If join fails: **Site unknown** — do not invent |
| Issue type | `caseType` | Operator label; raw code in details |
| Severity | `severity` | `StatusBadge kind="severity"` |
| Status | `status` | `StatusBadge` |
| Evidence summary | first useful `evidence[]` row | compact; see §9 |
| First observed | `firstObservedAt` | existing timestamp format |
| Last observed | `lastObservedAt` | existing timestamp format |
| Case | link | `/assurance/{id}` — “Open case” |

Do not add owner, assignee, or acknowledge/resolve actions.

### 7.2 Empty state

When Assurance loaded and no active cases: genuine empty state, e.g. **No active Assurance findings**. Not an error.

---

## 8. Filter and sort

### 8.1 Filter (client-side only)

**P0**

- Status: All active (default) / Open / Acknowledged
- Severity: All / Critical / Major / Warning / Info

**P1**

- Site: All / each known `siteId` from inventory (only sites that can be listed)

Do not add backend query parameters. Do not add enterprise search. Type filter is unnecessary while only one `caseType` exists; do not fake a type catalogue.

Combined filters are AND.

### 8.2 Sort (client-side)

**P0 default:** severity rank descending, then `lastObservedAt` descending, then `id` ascending.

**P1:** optional explicit control (Severity / Last observed) if it stays a single control. No ranking score.

---

## 9. Evidence preview (P1, compact)

From `case.evidence[]`, pick the first row with a metric (prefer `evidenceType` threshold/KPI). Show at most one line on the queue:

- operator metric label when known (`BLER_DL` → Downlink BLER; otherwise the raw metric)
- value + unit
- trend if present
- observed time

Do not dump the full evidence table on the queue row. Full table remains on `AssuranceCasePage`. Preserve `source` and `synthetic` in case detail / technical details, not as primary queue chrome.

---

## 10. Map attention (P0)

Reuse `SiteMap`. Site-level only. Do **not** add cell coordinates or cell markers. Do **not** persist site severity.

### 10.1 Derivation

For each site:

1. Cells at site: `cells.filter(c => c.siteId === site.siteId)`
2. Active cases whose `affectedEntityId` is in that cell set
3. If none: **no active Assurance attention**
4. Else: highest severity among those cases (`CRITICAL` > `MAJOR` > `WARNING` > `INFO`)

### 10.2 Operator language (popup / legend)

| Derived state | Operator language | Not allowed |
|---|---|---|
| no active cases | No active Assurance findings | Healthy |
| any active | Needs attention | Degraded / Unhealthy |
| highest CRITICAL | Critical Assurance finding | Site health = CRITICAL as inventory |

Popup must include: site name/id, attention summary, active issue count, highest active severity (if any), existing **Open site** link to `/network/sites/{siteId}`.

Inventory `site.status` may remain visible as inventory, clearly secondary to Assurance attention. Marker tone for PI4 follows **Assurance attention**, not inventory `ACTIVE`.

Missing coordinates: omit that site from the map (`locatedSites` already does this). Do not fail the workspace.

### 10.3 Legend

A short legend: No active findings / Needs attention / Critical finding. Do not title it “Network health”.

---

## 11. Site page roll-up (P1)

On existing `SitePage.tsx`, add a bounded panel using list data already practical to load (`getSite`, `listCells`, `listGnbs`, plus **one** `listAssuranceCases` if not already loaded — not per-cell assurance).

Show:

- number of cells at this site
- cells needing attention (distinct active-case cells at this site)
- active Assurance case count for those cells
- highest active Assurance severity, or “No active Assurance findings”

Do **not** add site KPI aggregation, site health score, or per-cell telemetry/context fetches.

---

## 12. Investigation and handoff

### 12.1 Assurance case

Reuse `/assurance/:caseId` (`AssuranceCasePage`). Do not duplicate case-detail UI on Network Operations.

Queue → Open case → evidence (existing) → assessment (existing GET) → **Open cell** (already present when `affectedEntityType === 'CELL'`).

No new AI. No new assessment endpoint.

### 12.2 Cell

Reuse `/network/cells/:cellId`. Do not create a second cell investigation page.

### 12.3 PI3

```text
Network Operations → issue → case → cell → Optimize → existing PI3 workspace
```

Optimize remains `/network/cells/{cellId}/optimize`. PI4 tests must prove the Optimize control remains reachable from the affected cell. PI4 browser acceptance **stops at Optimize**. Do not require approve / plan / sandbox / production.

---

## 13. Partial failure

Independent presentation boundaries:

| Partition | Requests | Failure behaviour |
|---|---|---|
| Inventory | sites, cells, optional gnbs | show whichever succeeded; failed partition = Unavailable; do not claim a zero total |
| Assurance | cases | failed → issue queue error + summary Assurance metrics Unavailable; **do not** report zero issues |
| Map | needs sites (coords) + cells + cases | sites without coords skipped; if sites fail, map empty/unavailable; if cases fail, map may show sites **without claiming** “no attention” — show attention unknown |
| Summary | derived | each metric independently available or unavailable |

`ZERO` ≠ `UNKNOWN`.

Backend completely unavailable: workspace-level error with retry (existing `ErrorState`), but if some of the three overview GETs succeed, prefer partial render over a blank page.

Current `NetworkPage` fails closed if any of `Promise.all([sites, cells, gnbs])` rejects. PI4 **must** load those requests independently (or `allSettled`) so one failure does not hide the others.

---

## 14. Freshness

| Surface | Timestamp |
|---|---|
| Issue queue | `firstObservedAt`, `lastObservedAt` |
| Cell investigation | existing KPI/telemetry timestamps on CellPage |

Do **not** display Phase 12 source freshness or Twin freshness as network/site operational freshness.

Optional: a clearly labelled “View loaded at {browser local/UTC time}” is browser-load time only, not a domain freshness semantic.

---

## 15. Security and authority

PI4 itself is **read-only**. No new mutation APIs, permissions, grants, sandbox execute, production-change, campaigns, vendor credentials, or MCP.

Existing PI3 governed actions remain only after handoff onto existing routes. PI4 must not weaken those boundaries and must not call production or MCP APIs.

---

## 16. File boundary (expected)

Predominantly `snip-web`. No Java unless this spec is proven wrong.

**New (suggested)**

- `snip-web/src/features/operations/operationsModel.ts` — pure derivations
- `snip-web/src/features/operations/operationsModel.test.ts`
- `snip-web/src/features/operations/issueLabels.ts` — operator labels
- `snip-web/src/features/operations/OperationalSummary.tsx`
- `snip-web/src/features/operations/AssuranceIssueQueue.tsx`
- `snip-web/src/pages/increment4.test.tsx` — page-level N+1, failure, navigation tests

**Modify**

- `snip-web/src/pages/NetworkPage.tsx` — independent loads, summary, queue, pass attention into map
- `snip-web/src/features/map/SiteMap.tsx` — attention derivation inputs + marker/popup/legend
- `snip-web/src/pages/SitePage.tsx` — P1 roll-up (may load `listAssuranceCases` once)
- `snip-web/src/index.css` — bounded attention marker/legend styles only
- `snip-web/src/test/fixtures.ts` — extra sites/cells/cases as needed for tests

**Reuse unchanged**

- `AssuranceCasePage`, `CellPage`, `OptimizePage`, `AskSnip`, `AssuranceList` (case detail/cell already use it), `snipApi` list methods, backend Java

Do not modify `snipApi.ts` unless a missing **existing** GET must be wired (all three P0 GETs already exist).

---

## 17. Pure derivation design

Keep helpers pure and unit-tested. Suggested smallest coherent API (names may be adjusted; behaviour must not):

```ts
ACTIVE_STATUSES = ['OPEN', 'ACKNOWLEDGED']

isActiveAssuranceCase(c): boolean
  // CELL-affected cases: status OPEN or ACKNOWLEDGED

severityRank(severity: string): number
  // CRITICAL=4, MAJOR=3, WARNING=2, INFO=1, else=0

compareIssues(a, b): number
  // severityRank desc, lastObservedAt desc, id asc

activeCases(cases): Case[]
cellsNeedingAttention(cases): string[]  // distinct cell ids
criticalActiveCount(cases): number

siteIdForCell(cellId, cells): string | null
  // null if missing — never fabricate

siteAttention(siteId, cells, cases): {
  activeCount: number
  cellIds: string[]
  highestSeverity: string | null  // null → no active findings
}

networkOperationsSummary(sites, cells, cases, loaded: {
  sites: boolean, cells: boolean, cases: boolean
}): {
  sites: number | 'UNAVAILABLE'
  cells: number | 'UNAVAILABLE'
  activeCases: number | 'UNAVAILABLE'
  criticalActive: number | 'UNAVAILABLE'
  cellsNeedingAttention: number | 'UNAVAILABLE'
}
```

`networkOperationsSummary` must return `'UNAVAILABLE'` (or equivalent) when the corresponding load failed, including when the successful list is empty vs failed.

Overview fetch guard: NetworkPage tests assert `fetch` URLs on `/network` contain only `/api/v1/sites`, `/api/v1/cells`, `/api/v1/assurance/cases`, and optionally `/api/v1/gnbs` — never `/context`, `/kpis`, `/telemetry`, `/twins`, or `/integration/sync`.

---

## 18. P0 / P1 / P2

### P0 (required for PI4 acceptance)

1. `/network` as Network Operations workspace
2. Real summary counts (sites, cells, active cases, critical active, cells needing attention)
3. Active Assurance issue queue
4. Client filters: status, severity
5. Deterministic sort: severity then last observed
6. Site map attention from active Assurance
7. Issue → case → cell navigation
8. Cell → existing Optimize
9. Independent partial-failure handling; ZERO ≠ UNKNOWN
10. Empty-queue state
11. No N+1 overview calls
12. No generic health score; no `sourceHealth` / Twin freshness as network health

### P1

- Site filter on queue
- SitePage operational roll-up
- Evidence preview on queue row
- Explicit sort control if useful
- Link to existing case assessment (already on case page; queue may say “Open case” only)

### P2 / defer

Site/network KPI rollups; per-cell KPI cards on overview; generic text search; backend pagination/filters; operations-summary endpoint; Ask SNIP on the queue; acknowledge/resolve; owner/ITSM; network health scoring.

---

## 19. Test plan

### 19.1 Derivation unit tests (`operationsModel.test.ts`)

1. Active = OPEN or ACKNOWLEDGED
2. RESOLVED excluded from active attention
3. Severity ordering CRITICAL > MAJOR > WARNING > INFO
4. Time tie-break: later `lastObservedAt` first when severity equal
5. Summary counts from fixtures
6. Distinct cells needing attention
7. Site join from affected cell
8. Unknown cell does not fabricate site
9. Map/site attention derivation
10. Highest site severity
11. Status filter
12. Severity filter
13. Combined filters
14. Empty issue list → zeros only when loaded
15. Failed Assurance → metrics UNAVAILABLE, not zero
16. Failed inventory → inventory totals UNAVAILABLE, not zero

### 19.2 Page tests (`increment4.test.tsx`)

17. Missing site coordinates: workspace still renders
18. Issue → `/assurance/{id}`
19. Case → cell link remains valid (existing page)
20. Cell → Optimize handoff remains valid
21. No generic network-health enum/score in Network Operations copy
22. `sourceHealth` / Twin freshness not presented as site/cell health on `/network`
23. Overview fetch set has no per-cell KPI/context/twin/knowledge URLs
24. Operator labels: “Degrading radio quality” shown; raw code not required in primary column
25. Assurance fetch failure: no “0 active” / “0 critical” authoritative zeros

Use existing Vitest + Testing Library + `MemoryRouter` + mocked `fetch` (PI3 pattern). Do not add a Playwright platform.

### 19.3 Final qualification (before freeze, not during spec)

```text
cd snip-web && npm ci && npm test && npm run build
mvn -B test
cd simulator && go test ./...
```

Repository-wide Maven/Go remain required even though PI4 is frontend-only.

---

## 20. Local browser acceptance

Use real local backend-derived data. Isolated SNIP stack as in PI3 (do not disturb unrelated WAODN). Production mutation remains disabled.

Journey:

1. Demo Login (RF Optimisation Engineer or Network Engineer)
2. Network Operations (`/network`)
3. Confirm real counts for sites, cells, active cases, critical active cases, cells needing attention
4. Inspect issue queue
5. Filter to Critical; only matching active findings
6. Clear/change filters
7. Inspect evidence (preview and/or case page)
8. Identify affected site/cell
9. Confirm map attention for that site
10. Open Assurance case
11. Inspect detailed evidence / existing assessment
12. Open affected cell
13. Confirm KPI / Assurance context on CellPage
14. Click Optimize
15. Confirm existing PI3 Optimize workspace

**Do not require:** approve proposal, create/authorize plan, sandbox execute, production work.

### 20.1 Acceptance questions PI4 must answer in the browser

1. How many sites/cells in this demo dataset?
2. Are there active Assurance findings?
3. Which findings are most severe?
4. Which cells need attention?
5. Which site contains an affected cell?
6. What evidence supports the finding?
7. When was it last observed?
8. Can I open the affected cell?
9. Can I enter the existing optimization workflow from that cell?

PI4 does **not** answer “What is the health score of the whole network?”

---

## 21. Non-goals

NO Phase 19; production network execution; Ericsson production write; Nokia/Huawei; O-RAN; slicing; billing; OSS/BSS expansion; telecom-specialized LLM; AI Model Gateway; new agents; new MCP; closed-loop optimization; automatic remediation; campaign execution; SSO; mobile app; design-system rewrite; national-scale streaming dashboard; new generic network-health algorithm; opaque prioritization score; per-cell KPI wall; Assurance acknowledge/resolve workflow; owner/assignment ITSM.

---

## 22. Architecture impact and assurance

**Architecture:** FRONTEND_ONLY.

**Assurance:** STANDARD_PRODUCT_ASSURANCE — PI4 composes existing read-only operational data and creates no new network mutation authority.

---

## 23. Implementation sequence

1. Pure helpers + unit tests (`operationsModel`)
2. Operator labels
3. `NetworkPage` independent loads (`allSettled` / separate catches)
4. Operational summary (available vs unavailable)
5. Issue queue + default sort
6. P0 filters
7. Map attention + popup language + legend
8. Navigation: queue → case; cell link; Optimize regression
9. Partial-failure tests
10. P1: site filter, evidence preview, SitePage roll-up
11. Accessibility: headings, table headers, skip-link unchanged
12. `npm test` / `npm run build`
13. Local browser acceptance
14. Correction only if substantive
15. `npm ci && npm test && npm run build`, `mvn -B test`, `go test ./...`
16. Immutable PI4 baseline commit, normal push, exact-SHA CI, PI4 CLOSED

Do not freeze during specification. Do not freeze until the sequence above completes.

---

## 24. Freeze strategy

```text
implementation
  → automated tests
  → implementation review
  → browser acceptance (discovery/investigation only)
  → correction only if substantive
  → final qualification
  → one immutable PI4 baseline commit
  → normal push (no force)
  → exact-SHA CI verification
  → PI4 CLOSED
```

Parent remains `ed10833d7ee186f97a1602aa065905c71e055090` until that commit. Do not amend PI3.

---

## 25. Specification control

```text
PI4_TITLE: NETWORK_OPERATIONS_AND_ASSURANCE_WORKSPACE
PARENT_BASELINE: ed10833d7ee186f97a1602aa065905c71e055090
BACKEND_CHANGE: NOT AUTHORIZED
NEW_ENDPOINT: NOT AUTHORIZED
NEW_PERSISTENCE: NOT AUTHORIZED
PHASE_19: NOT STARTED
PI4_IMPLEMENTATION: NOT STARTED
```
