# SNIP Product Increment 5 — AI-Assisted Network Investigation Workspace

## Implementation specification

**Status:** AUTHORIZED FOR IMPLEMENTATION (specification only; implementation not started)

**Mode:** FRONTEND_ONLY

**Assurance level:** ELEVATED_PRODUCT_ASSURANCE

---

## 1. Authority and baseline

Parent baseline (frozen PI4, exact-SHA verified):

```text
Branch: main
Commit: 8e602c70e63689700660f1aad51593ca45b51cae
Product Increment 4: CLOSED
Product Increment 3 parent: ed10833d7ee186f97a1602aa065905c71e055090
Architecture impact: FRONTEND_ONLY
Material AI capability gaps: 0
Required backend enablers: 0
New persistence: NO
Database migration: NO
New model provider: NO
New agent: NO
New MCP capability: NO
Production write capability: NO
AI mutation authority: NO
Phase 19: NOT STARTED
```

Readiness review decision: `READY_FOR_PI5_SPECIFICATION`.

This document is the authorised implementation specification for Product Increment 5. Implement exactly this scope. Do not start Phase 19. Do not implement production write, vendor transport, new agents, MCP, Model Gateway, a telecom LLM, or a generic network-health model.

If implementation discovers that a Java/backend contract change is required: **STOP** and report `BACKEND_CONTRACT_REVIEW_REQUIRED`. Do not add backend scope silently.

If implementation discovers that PI5 would require mutation authority, agent-runs, or MCP: **STOP** and report `SECURITY_BOUNDARY_REVIEW_REQUIRED`.

---

## 2. Product objective

PI4 answers:

- What needs attention?
- Where is it?
- What observed evidence triggered the finding?

PI5 answers, without increasing AI authority:

- What is happening?
- What observed evidence supports the finding?
- What plausible contributors should be investigated?
- What engineering checks should be performed?
- What evidence is missing?
- What knowledge sources support the analysis?
- Where should the engineer continue investigating?

Authoritative journey:

```text
Demo Login
  → Network Operations (/network)
  → active Assurance finding
  → Open case
  → /assurance/:caseId
  → Finding
  → Observed evidence
  → SNIP analysis
  → Likely contributors to investigate
  → Recommended engineering checks
  → Missing evidence
  → Knowledge sources / citations
  → Open affected cell
  → existing Cell workspace
  → existing Optimize workflow (PI3)
```

PI5 must **not** duplicate PI3. PI5 acceptance **stops** when the existing Optimize workspace is reached. Do not generate, approve, plan, or execute a change.

---

## 3. Architecture decision

```text
ARCHITECTURE_IMPACT: FRONTEND_ONLY
REQUIRED_BACKEND_ENABLERS: 0
NEW_PERSISTENCE: NO
DATABASE_MIGRATION: NO
NEW_MODEL_PROVIDER: NO
NEW_AGENT: NO
NEW_MCP_CAPABILITY: NO
PRODUCTION_WRITE_CAPABILITY: NO
```

PI5 is a presentation, loading-isolation, and truthfulness increment over existing contracts:

- `GET /api/v1/assurance/cases/{caseId}`
- `GET /api/v1/assurance/cases/{caseId}/assessment`

Optional P1 (existing contracts only):

- `GET /api/v1/cells/{cellId}/context`
- `POST /api/v1/recommendations` (operator-initiated Ask SNIP)

Do not add query parameters, persistence, generatorMode, case-scoped assessment questions, or new endpoints.

If the accepted frontend-only design cannot be implemented correctly against these contracts: **STOP**.

---

## 4. Assurance level

**ELEVATED_PRODUCT_ASSURANCE**

PI5 remains read-only and advisory. It nevertheless presents engineering interpretation to an operator. Therefore these are stronger acceptance concerns than ordinary UI work:

- truthfulness of intelligence origin
- observed evidence vs interpretation
- citation provenance
- uncertainty / missing evidence
- synthetic-data labelling
- graceful degradation when analysis fails

This does **not** make PI5 safety-critical.

---

## 5. Intelligence truth model (authoritative)

Do not label something “AI” merely because it appears on an investigation page.

| Product concept | Current producer | Classification | Operator language |
|---|---|---|---|
| Case finding (type, severity, status, rule, times) | Assurance detection / case GET | DETERMINISTIC_RULE_BASED | Finding |
| Observed evidence | `AssuranceCaseDto.evidence[]` | OBSERVED (may be SYNTHETIC) | Observed evidence |
| Likely contributors | `DecisionSupportComposer.likelyContributors` | DETERMINISTIC_RULE_BASED | Rule-based investigation guidance |
| Recommended checks | `DecisionSupportComposer.recommendedChecks` | DETERMINISTIC_RULE_BASED | Recommended engineering checks |
| Missing evidence | `DecisionSupportComposer.missingEvidence` | DETERMINISTIC_RULE_BASED | Missing evidence |
| Urgency | `DecisionSupportComposer.urgency(severity)` | DETERMINISTIC_RULE_BASED | Urgency (from severity) |
| `humanReviewRequired` | always `true` in current service | DETERMINISTIC | Human engineering review required |
| Assessment `summary` (default/CI, `snip.generator=stub`) | `StubRecommendationGenerator` over retrieved notes | STUB_OR_FIXTURE | SNIP analysis — retrieved knowledge and investigation guidance |
| Assessment `summary` (`snip.generator=spring-ai` and retrieval hits) | Ollama `ChatClient` | LLM_GENERATED | Do not claim unless UI can prove this mode |
| Assessment `summary` (empty retrieval) | fixed `EMPTY_SUMMARY`; generator not called | TEMPLATE_GENERATED | No supporting knowledge source was retrieved |
| Citations | `RetrievedChunk` → `CitationDto` | retrieval-supplied | Knowledge sources / retrieved sources |
| Default retrieval | `LexicalRetriever` on `testdata/corpus/*.md` | PARTIALLY_GROUNDED lexical | Retrieved notes (sample/demo knowledge) |

The assessment as a whole is **MIXED**. Do not present all parts as if they share one origin.

### 5.1 Assessment execution path (do not redesign)

```text
GET /api/v1/assurance/cases/{caseId}/assessment
  → AssuranceController
  → DecisionIntelligenceService.assess
  → AssuranceCaseService.findById
  → NetworkContextService.resolve
  → ChunkRetriever.retrieve
  → DecisionSupportComposer (lists + urgency)
  → optional RecommendationGenerator.generate (summary only, if retrieval has hits)
  → citations from RetrievedChunk (not from model text)
```

Assessment is computed on every GET. It is not persisted.

### 5.2 LLM call (exact)

**DOES THE ASSESSMENT CURRENTLY CALL AN LLM?** **CONDITIONAL.**

- Default / CI (`snip.generator=stub`, match-if-missing): **NO**. `StubRecommendationGenerator` concatenates retrieved note text. It does not call an LLM.
- Optional local-ai (`snip.generator=spring-ai`) **and** retrieval hits: **YES**, via Spring AI `ChatClient` (Ollama).
- Empty retrieval: **NO**. Generator is not called. Fixed `EMPTY_SUMMARY`.

The DTO does **not** expose `generatorMode`, model ID, model version, or prompt version. Therefore P0 **must not** label the summary “AI-generated”, “LLM analysis”, or “telecom-trained model analysis”.

Forbidden P0 headings/copy:

- AI-generated
- LLM analysis
- AI-generated root cause
- telecom-trained model analysis
- confirmed root cause

Required P0 analysis heading: **SNIP analysis**

Required default/demo explanatory wording (or equivalent truthful neutral phrase):

> Retrieved knowledge and investigation guidance

### 5.3 Retrieval / RAG

Current grounding is **PARTIALLY_GROUNDED**.

Default retrieval: `LexicalRetriever`. Default corpus: `testdata/corpus/*.md` (sample/fictional planning notes; see `testdata/PROVENANCE.md`). This is **not** vendor-certified documentation, a 3GPP corpus, production OSS knowledge, or certified RF knowledge.

Vector retrieval exists only under optional `local-ai`. Do not describe default retrieval as semantic RAG unless the runtime `retrievalMode` actually proves it.

`assessment.retrievalMode` may appear as secondary technical/provenance detail using the **runtime value**.

If `retrievalEmpty === true`, the UI must state that no supporting knowledge source was retrieved. Observed evidence remains valid. Rule-based lists may still be present.

### 5.4 Citation integrity

Current integrity: **BOUNDED**.

`CitationDto` is derived from retrieved chunks:

- `sourceId`
- `locator`
- `snippet`
- `chunkId`
- `score`

The model does not create citation objects. Summary text could still mention an unsupported clause.

Rules:

- render authoritative citation objects separately from summary
- do not parse citations out of summary text
- do not imply every sentence in summary is individually cited
- do not fabricate source links, document titles, 3GPP references, vendor references, or URLs
- do not claim fixture notes are authoritative standards

### 5.5 Hardcoded assessment question (advisory, do not fix in PI5)

`DecisionIntelligenceService.CANONICAL_QUESTION` currently names `CELL-001` and `DEGRADING_RADIO_QUALITY`. The prompt and composer still receive the actual case. This is **P2 / deferred**. Do not change the backend in PI5.

---

## 6. Core UX principle — evidence first

The operator must see observed evidence independently of whether analysis succeeds.

Required page order:

1. Finding
2. Observed evidence
3. SNIP analysis
4. Likely contributors to investigate
5. Recommended engineering checks
6. Missing evidence
7. Knowledge sources / citations
8. Investigation actions (Open cell; existing PI3 handoff remains on CellPage)

Do not put generated/stub analysis above the operational facts that triggered the case.

Acceptance assertions:

1. Analysis may fail; operational evidence must remain available.
2. Interpretation must not be presented as observed fact.
3. Stub / retrieval-based demo intelligence must not be presented as a live telecom LLM.

---

## 7. Route decision

Enhance existing:

```text
/assurance/:caseId
AssuranceCasePage
```

Do **not** create `/investigation`, `/ai/investigation`, `/assurance/:id/ai`, or another competing case route.

Existing `/ai` remains the generic Ask SNIP experience. It is **not** the primary PI5 workspace.

PI4 entry remains:

```text
Network Operations → Open case → /assurance/:caseId
```

PI5 begins after the operator opens the case.

---

## 8. PI4 preservation

Do not change:

- Needs attention semantics (active Assurance on the object)
- ACTIVE = `OPEN` or `ACKNOWLEDGED`
- RESOLVED = inactive
- severity order `CRITICAL > MAJOR > WARNING > INFO`
- operational summary counts
- map attention
- issue prioritization
- Network Operations N+1 boundary

Do not call assessment for every queue row. Do not precompute analysis from `/network`.

---

## 9. Independent data loading (P0)

Current `AssuranceCasePage` loads case + assessment with fail-closed `Promise.all`. Replace that.

| Partition | Request | Authority |
|---|---|---|
| CASE | `GET /api/v1/assurance/cases/{id}` | Authoritative operational information |
| ANALYSIS | `GET /api/v1/assurance/cases/{id}/assessment` | Advisory investigation support |

Load independently. Represent independently.

If assessment fails:

- CASE: visible
- OBSERVED EVIDENCE: visible
- ANALYSIS and rule-based lists: unavailable (not zero, not fabricated)
- Open cell: available when case data permits (`affectedEntityType === 'CELL'` and `affectedEntityId` present)

If the case GET fails: existing bounded error/not-found. Do not render assessment as detached authoritative case information.

If both succeed: full investigation in the required order.

Retry of a failed assessment read may use existing Retry patterns. Do **not** frame retry as “Regenerate” / “Try another answer”.

---

## 10. Finding

From the case DTO:

- operator-facing issue type (reuse PI4 `Degrading radio quality` for `DEGRADING_RADIO_QUALITY`; raw code may remain secondary)
- affected entity type and id
- severity and status badges
- rule id (secondary)
- `detectedAt`, `firstObservedAt`, `lastObservedAt`
- case `confidence` only with its actual case semantics — **not** model accuracy, network health, or root-cause probability

If `item.synthetic`, show a case-level demo banner.

---

## 11. Observed evidence

Source: `AssuranceCaseDto.evidence[]`.

Fields that exist and may be shown:

`id`, `evidenceType`, `metric`, `value`, `unit`, `trend`, `observedAt`, `source`, `synthetic`, `description`.

Do **not** fabricate a threshold column. Detection thresholds are not on this DTO.

Preferred heading: **Observed evidence**

If `synthetic === true` on a row or on the case, communicate:

> Synthetic demo observation — not live network data.

Do not imply Ericsson production observation, live NMS measurement, or production network evidence.

Assessment `operationalEvidence` is the same evidence mapped again. P0 shall treat **case evidence** as the observed-evidence section. Do not present two competing evidence tables as if they were independent observations. If assessment operational evidence is shown at all, label it as the evidence the analysis used, not as a second observed dataset.

---

## 12. SNIP analysis

Render `assessment.summary` only after observed evidence, and only when the analysis partition succeeded.

Heading: **SNIP analysis**

Explanatory wording (default/unknown generator): retrieved knowledge and investigation guidance.

If `retrievalEmpty === true`, make absence of retrieved supporting knowledge visible. Do not present `EMPTY_SUMMARY` as a confident root-cause conclusion.

If analysis is unavailable: bounded unavailable state with Retry. Do not hide observed evidence.

Do **not** add Regenerate / Generate again / Try another answer.

---

## 13. Rule-based investigation guidance

Render only when the analysis partition succeeded.

### 13.1 Likely contributors

Heading: **Likely contributors to investigate**

These are investigation hypotheses, not confirmed root cause. Bounded explanatory wording is required.

### 13.2 Recommended engineering checks

Heading: **Recommended engineering checks**

These are inspection prompts, not configuration changes. Existing disclaimer language may be retained:

> These are investigation prompts, not automatic network actions.

No Apply / Execute / Fix automatically / Change configuration controls. No check may trigger mutation.

### 13.3 Missing evidence

Heading: **Missing evidence**

First-class P0 section. Do not hide in technical details.

Empty list: bounded empty state such as “No additional missing evidence was identified.” Do not invent gaps.

### 13.4 Urgency

May display `assessment.urgency`. It is derived from severity. Do not invent another priority score. Do not replace PI4 severity ordering. Do not create a network-health model.

### 13.5 Human review

`humanReviewRequired` is currently always true. Communicate:

> Human engineering review required.

No automatic acceptance control.

### 13.6 Assessment confidence

If shown at all (P1), it is the **case confidence copied onto the assessment DTO**, not model accuracy. Do not reinterpret as network health, root-cause probability, or cell confidence. P0 may omit it if the Finding already shows case confidence.

---

## 14. Knowledge sources / citations

Heading: **Knowledge sources** (or **Retrieved sources**)

Render `assessment.citations[]` separately from summary.

Show available metadata: `sourceId`, optional `locator`, optional `snippet`, optional `score`.

Do not create clickable links unless an actual valid URI exists in the payload (current DTO has no URI field — so P0 has **no fabricated links**).

Empty citations: “No retrieved sources.” Do not fabricate.

If `retrievalEmpty === true`, this section must agree with that fact.

`retrievalMode` and latencies (`retrievalLatencyMs`, `generationLatencyMs`, `totalLatencyMs`) are **P1** technical details, preferably inside a bounded disclosure, not primary operator evidence.

---

## 15. Observed vs derived vs interpretation

No new backend enum. Use existing metadata and presentation.

| Class | Source | Presentation |
|---|---|---|
| OBSERVED | case `evidence[]` | Observed evidence |
| DERIVED / RULE-BASED | contributors, checks, missing, urgency | labelled as investigation guidance / hypotheses / gaps |
| INTERPRETATION | `summary` | SNIP analysis |
| KNOWLEDGE SOURCE | `citations[]` | Knowledge sources |
| SYNTHETIC | `synthetic === true` | demo/simulator labelling |
| UNKNOWN / UNAVAILABLE | failed or pending partitions | Unavailable — not zero |

---

## 16. Cell navigation and PI3 handoff

Preserve:

```text
Open cell → /network/cells/{cellId}
```

only when `affectedEntityType === 'CELL'` and `affectedEntityId` is present. Do not fabricate site/cell joins.

PI3 handoff remains on **CellPage** via the existing control (currently “Propose txPower optimization”). PI5 must not add an Optimize action that bypasses CellPage, must not auto-open Optimize, and must not call proposal/plan/execution APIs.

Acceptance stops when the existing Optimize workspace is reached.

---

## 17. Cell context, Network Knowledge, Ask SNIP, agents, MCP

### 17.1 Cell context

P0 does **not** require `GET /api/v1/cells/{cellId}/context` on the case page. Prefer Open cell over duplicating CellPage.

Optional P1: at most **one** bounded context GET if it materially helps and does not create a KPI/telemetry wall.

### 17.2 Network Knowledge

Do not present `sourceHealth` / knowledge confidence as case or cell health. P0 does not add Network Knowledge to the Assurance case. CellPage remains the place for source-scoped knowledge.

### 17.3 Ask SNIP

`POST /api/v1/recommendations` is cell-scoped, same retriever/generator stack, default stub + lexical. **Not required for P0.**

If P1:

- operator initiated only
- `cellId` bound from the case
- no automatic question
- no mutation
- independent failure
- truthful stub/retrieval wording
- must not block P0

### 17.4 Agents

Do **not** call `POST /api/v1/agent-runs`. Do not activate chief-orchestrator, knowledge-agent, context-agent, assurance-agent, or decision-agent.

### 17.5 MCP

Assessment and Ask SNIP do not use MCP. PI5 introduces **NO NEW MCP CAPABILITY**. Agent→MCP mutation remains unauthorized.

---

## 18. Query boundary

One opened investigation may use:

```text
GET /api/v1/assurance/cases/{id}
GET /api/v1/assurance/cases/{id}/assessment
```

Optional P1:

```text
GET /api/v1/cells/{cellId}/context
POST /api/v1/recommendations   # explicit operator action only
```

Forbidden:

- assessment calls from the Network Operations queue
- N+1 AI/assessment calls
- agent-runs
- MCP
- production-change / campaign / sandbox execute from this page

---

## 19. Security and production safety

PI5 is read-only/advisory. It must **not**:

acknowledge or resolve Assurance; modify Assurance state; change configuration or txPower; automatically create/approve/reject a proposal; automatically create/review/authorize a plan; request, authorize, or execute sandbox; mint production grants; invoke Production Write Gateway; release/resume/rollback campaign; invoke mutation-capable MCP; perform production execution.

```text
REAL PRODUCTION EXECUTION: NOT_AUTHORIZED
REAL PRODUCTION CAMPAIGN EXECUTION: NOT_AUTHORIZED
AGENT/MCP EXECUTION: NOT_AUTHORIZED
AUTOMATIC REMEDIATION: NOT_AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT_AUTHORIZED
AI MUTATION AUTHORITY: NO
```

---

## 20. Failure matrix (minimum)

| Condition | Required behaviour |
|---|---|
| Case success + assessment success | Full investigation in evidence-first order |
| Case success + assessment failure | Case, evidence, Open cell visible; analysis unavailable |
| Case success + empty retrieval | Evidence visible; retrieval absence explicit; rule-based lists retained if returned |
| Case success + no citations | Do not fabricate sources |
| Case success + empty contributor/check lists | Bounded empty states |
| Case success + empty missing-evidence list | “No additional missing evidence was identified.” |
| Case failure | Bounded case error; do not treat assessment as the case |
| Optional spring-ai failure | Same as assessment failure: ANALYSIS UNAVAILABLE, not NETWORK DATA UNAVAILABLE |

---

## 21. P0 / P1 / P2

### P0 (required)

1. Enhance existing `AssuranceCasePage`
2. Independent case and assessment loading
3. Finding section
4. Observed evidence first
5. Synthetic/demo evidence labelling
6. SNIP analysis section
7. Generator-neutral truthful analysis wording
8. Likely contributors (hypotheses, not RCA)
9. Recommended engineering checks (no mutation controls)
10. Missing evidence
11. Human-review indication
12. Knowledge/retrieved sources
13. Citation metadata from backend objects
14. Retrieval-empty handling
15. Analysis-unavailable handling
16. Case/evidence usable when analysis fails
17. Open affected cell
18. Existing PI3 Optimize handoff
19. No mutation
20. PI4 semantics unchanged

### P1 (bounded; defer if it threatens P0)

- `retrievalMode` technical detail
- retrieval/generation/total latency in a disclosure
- bounded assessment confidence copy (case-confidence semantics only)
- one optional cell-context panel
- case-bound Ask SNIP
- improved source presentation

No backend work for P1.

### P2 (defer)

`generatorMode` backend field; case-scoped assessment question; agents; agent-runs; vector retrieval as new default; document viewer; regenerate; Model Gateway; telecom LLM; vendor/3GPP corpus ingestion; new RAG; new model provider; new MCP; new AI orchestration.

---

## 22. File boundary (expected)

Predominantly `snip-web`. No Java unless this spec is proven wrong.

**Modify (expected)**

- `snip-web/src/pages/AssuranceCasePage.tsx` — independent loads, evidence-first layout, analysis isolation
- `snip-web/src/index.css` — bounded investigation section styles only if needed
- `snip-web/src/test/fixtures.ts` — extra assessment/evidence fixtures if needed

**New (suggested, names may follow repo conventions)**

- `snip-web/src/features/assurance/` bounded presentational helpers if they keep the page readable (labels, empty/unavailable copy)
- `snip-web/src/pages/increment5.test.tsx` — independent load, failure isolation, truthfulness, navigation tests

**Reuse unchanged**

- `NetworkPage`, operations model, `CellPage`, `OptimizePage`, `AskSnip` (unless P1 case-bound Ask SNIP), `snipApi` GET methods already present, all Java

Do not modify `snipApi.ts` unless wiring an **existing** GET that is somehow unused (both P0 GETs already exist).

Do not call `listAssuranceCases` again from the case page.

---

## 23. Test plan

Use existing Vitest + Testing Library + `MemoryRouter` + mocked `fetch`. Do not add a Playwright platform.

### 23.1 Page tests (`increment5.test.tsx` and/or focused AssuranceCasePage tests)

1. Finding renders
2. Observed evidence renders before / independently of analysis
3. Assessment success renders summary
4. Likely contributors render
5. Recommended checks render
6. Missing evidence renders
7. Citations render from backend objects
8. No citation fabricated when empty
9. Retrieval-empty state visible
10. Synthetic evidence clearly labelled
11. Assessment failure leaves case visible
12. Assessment failure leaves evidence visible
13. Assessment failure leaves cell navigation available
14. Case failure produces bounded case error
15. No generic “LLM-generated” / “AI-generated” label in default-unknown mode
16. Deterministic guidance not labelled LLM-generated
17. Contributor wording does not claim confirmed root cause
18. Checks do not expose mutation controls
19. Human review indication
20. Observed vs interpretation distinction
21. No `sourceHealth` used as case/cell health
22. No network-health score introduced
23. PI4 Open case navigation unchanged
24. Case → cell navigation
25. Cell → Optimize remains valid
26. Fetch set on the case page does not include `/api/v1/agent-runs`
27. No MCP request
28. No production-change request
29. No production-campaign request
30. No sandbox execution from investigation

Preserve all existing frontend tests, including Increment 1A case/assessment rendering and Increment 4 queue → case navigation. Update existing case-page tests only where heading/copy changes are authorised by this spec (for example Assessment → SNIP analysis).

Backend tests are not expected to change.

---

## 24. Browser acceptance

Use the isolated SNIP runtime as in PI4. Do not disturb unrelated WAODN. Record actual generator/retrieval mode from runtime (default expected: stub + lexical).

Journey:

1. Demo Login
2. Network Operations
3. Identify an active Assurance finding
4. Open case
5. Confirm Finding
6. Confirm Observed evidence
7. Confirm synthetic/demo labelling where applicable
8. Confirm SNIP analysis (generator-neutral wording; not live LLM claim if stub)
9. Confirm likely contributors
10. Confirm recommended engineering checks
11. Confirm missing evidence
12. Confirm retrieved sources/citations
13. Confirm observed vs interpretation separation
14. Open affected cell
15. Enter existing Optimize workspace

**STOP.** Do not generate/approve a PI3 proposal, create a plan, execute sandbox, or invoke production functionality.

If default runtime uses stub + lexical retrieval, the acceptance record must state that and confirm the UI does not misrepresent it as live LLM analysis.

---

## 25. Non-goals

NO Phase 19; production execution; autonomous diagnosis-to-action; automatic remediation; closed-loop optimization; automatic proposal/plan/sandbox; new production transport; Nokia; Huawei; O-RAN; slicing; billing; OSS/BSS; telecom-specialized LLM; Model Gateway; new agent; agent activation; new MCP; campaign execution; SSO; mobile application; broad design-system rewrite; network-health score; LLM replacement of deterministic optimization; certified-root-cause claims; vendor-certified knowledge claims.

---

## 26. Implementation sequence

1. Preserve case/evidence semantics
2. Separate case and assessment loading
3. Add failure-isolation tests
4. Add evidence-first presentation
5. Add synthetic labelling
6. Add SNIP analysis presentation (generator-neutral)
7. Add deterministic guidance sections
8. Add missing-evidence presentation
9. Add citation/source presentation
10. Preserve cell navigation
11. Verify PI4 regression
12. Verify PI3 handoff
13. Automated frontend regression (`cd snip-web && npm test && npm run build`)
14. Browser acceptance
15. Implementation review
16. Full repository qualification (`npm ci`, `npm test`, `npm run build`, `mvn -B test`, `go test ./...`)
17. Freeze only after acceptance (one commit, normal push, exact-SHA CI)

Do not freeze during specification. Do not implement during specification.

---

## 27. Freeze strategy

```text
Specification
  → implementation
  → implementation review
  → browser acceptance
  → final qualification
  → one baseline commit
  → normal push
  → exact-SHA CI verification
  → PI5 CLOSED
```

Do not amend PI4. Do not rewrite history.

---

## 28. Operator copy (authorised)

| Concept | Authorised wording |
|---|---|
| Page/workspace | Investigation of the existing Assurance case (page remains `/assurance/:caseId`) |
| Evidence | Observed evidence |
| Synthetic | Synthetic demo observation — not live network data. |
| Summary heading | SNIP analysis |
| Summary origin (P0) | Retrieved knowledge and investigation guidance |
| Contributors heading | Likely contributors to investigate |
| Contributors caveat | Investigation hypotheses, not confirmed root cause |
| Checks heading | Recommended engineering checks |
| Checks caveat | Investigation prompts, not automatic network actions |
| Missing | Missing evidence |
| Citations | Knowledge sources / Retrieved sources |
| Empty retrieval | No supporting knowledge source was retrieved |
| Analysis failed | SNIP analysis unavailable |
| Human review | Human engineering review required |
| Cell action | Open cell |

Raw internal codes, REST paths, Java names, Phase numbers, and permission constants must not be primary product language.

---

## 29. Self-review against readiness evidence

| Check | Specification stance |
|---|---|
| Stub vs LLM | Documented MIXED; P0 generator-neutral |
| Retrieval | PARTIALLY_GROUNDED lexical fixture corpus |
| Citations | Retriever-owned objects; no fabricated links |
| Evidence first | Required order + independent load |
| Independent loading | P0; replace Promise.all fail-closed |
| Synthetic safety | Required banners from `synthetic` |
| Authority | Read-only; no agent-runs/MCP/production |
| PI4 semantics | Unchanged; no queue assessment |
| PI3 handoff | CellPage Optimize; stop there |
| Frontend-only | 0 backend enablers |
| P0/P1/P2 | Backend generatorMode/question deferred to P2 |

---

## 30. Decision after implementation

Implementers may report only:

`READY_FOR_PI5_IMPLEMENTATION_REVIEW`  
`PI5_IMPLEMENTATION_REQUIRES_CORRECTION`  
`BACKEND_CONTRACT_REVIEW_REQUIRED`  
`SECURITY_BOUNDARY_REVIEW_REQUIRED`  
`INSUFFICIENT_EVIDENCE`

Do not start Phase 19.
