# SNIP Phase 0 — Discovery Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Inspected locally:** `C:\workspaces\networkplanningoptimization`  
**Inspection date:** 2026-08-23  
**Default branch:** `main` (only branch)  
**Method:** Clone, `git ls-tree`, commit history, filesystem listing, README review  
**Rule followed:** Read, inspect, analyse and report. No SNIP features were implemented.

---

## Executive Summary

The repository is a **requirements stub**, not an implemented platform.

It contains **one tracked file**: `README.md` (about 6.8 KB). There are **five commits**, all dated 2026-07-30, all changing only that file. There is no application source, no build system, no tests, no containers, no CI, and no infrastructure-as-code.

The README describes an Intelligent 5G Network Planning & Optimization Platform: Ionic UI, AWS ALB, Kong, Keycloak/Cognito, ALICE ABAC, a Go telemetry service, a Java Spring AI planner/RAG service, Amazon MSK, OpenSearch Serverless, and EKS. Those statements are **design intent**. They are not present in the tree.

**Current maturity:** specification / Phase 0 planning.  
**SNIP readiness:** the target architecture (enterprise data, agents, MCP, digital twins, governance) has **no code foundation** in this repository yet.

Do not treat the README as as-built documentation.

---

## Repository Inventory

### What exists

```text
networkplanningoptimization
  |
  +-- README.md          (SRS prose + ASCII architecture)
  +-- .git/              (5 commits, origin/main only)
```

### What does not exist

No `src/`, `backend/`, `frontend/`, `docs/`, `pom.xml`, `go.mod`, `package.json`, Dockerfiles, Kubernetes/Helm, Terraform, `.github/workflows`, SQL, OpenAPI, LICENSE, or tests. Markdown stays in the repository root.

### Component

```text
README specification
  |
  +-- Language: Markdown
  +-- Framework: none
  +-- Purpose: Functional and non-functional requirements for a 5G planning/optimization product
  +-- Dependencies: none in-repo
  +-- APIs: described only (Kong routes, Go ingest, Java RAG)
  +-- Data: described only (MSK, OpenSearch, unnamed relational DB)
  +-- Tests: none
  +-- Deployment: described only (EKS, multi-stage Docker)
  +-- Maturity: draft SRS
```

### History

| Commit | Message | Files |
|--------|---------|--------|
| `0194568` | Initial commit | `README.md` (title only) |
| `463bdfd` … `2ff8e8e` | Update README.md (four times) | `README.md` expanded to full SRS |

No tags. No LICENSE. Public GitHub, 0 stars / 0 forks at inspection time.

---

## Current Architecture

What the repository **actually** implements: **nothing runnable**.

What the README **claims** (collapsed for readability):

```text
Ionic App (HTTPS/WSS)
        |
        v
     AWS ALB
        |
        v
  Kong API Gateway  <--->  Keycloak / Cognito
        |                         |
        |                         v
        +------------------->  ALICE ABAC
        |
        +---> Go Telemetry Ingest  ---> Amazon MSK (Kafka)
        |
        +---> Java Spring AI Planner / RAG  ---> OpenSearch (3GPP vectors)
                                            ---> Relational DB / GitOps (topology)
```

Functional requirements in the README:

- **FR-1** Go ingest of 5G KPIs (BLER, drop rate, latency) via Protobuf into Kafka  
- **FR-2** Java Spring AI optimization suggestions (tilt, beamforming, bandwidth) via Bedrock/Claude  
- **FR-3** 3GPP RAG copilot (embed → OpenSearch → cited answer)  
- **FR-4** ALICE policy on operational changes (role, region, senior sign-off)

Non-functional targets in the README (untestable today):

- 10,000 RPS per Go node, &lt;15 ms ingest-to-Kafka  
- Ionic map cluster load &lt;2 s  
- Image sizes &lt;150 MB (Go) / &lt;350 MB (Java)  
- EKS multi-AZ, HPA at 70% CPU/memory  
- mTLS ALB → Kong → pods  
- Immutable ALICE audit log stream  

Phase 2 preview: billing on Kafka (AI tokens + gNB/telemetry volume).

The README ends by offering to draft a Spring AI RAG class or a Kong declarative config. That confirms the author still treats this as **requirement planning**, not code realization.

---

## SNIP Architecture Mapping

SNIP target: enterprise intelligence platform (data, knowledge, context, agents, MCP, twins, events, learning, governance, security, observability, automation).

For each capability:

### Enterprise Platform

```text
Target Capability: Enterprise Platform
    |
    +-- Existing implementation: none
    +-- Existing technology: none
    +-- Current maturity: absent
    +-- Gap: no services, gateway, config, or shared platform layer
    +-- Recommended action: do not stand up Kong/EKS/ALICE first; start a thin local slice
```

### Data Platform

```text
Target Capability: Data Platform
    |
    +-- Existing implementation: none
    +-- Existing technology: none (README names MSK, OpenSearch, relational DB)
    +-- Current maturity: absent
    +-- Gap: no schemas, ownership, pipelines, or authoritative stores
    +-- Recommended action: define one bounded dataset (e.g. a few 3GPP docs + synthetic gNB KPIs) before any enterprise store
```

### Integration Platform

```text
Target Capability: Integration Platform
    |
    +-- Existing implementation: none
    +-- Existing technology: none
    +-- Current maturity: absent
    +-- Gap: no REST, Kafka clients, events, or gateway
    +-- Recommended action: one HTTP API in Phase 1; defer Kafka/MSK until there is a real producer
```

### AI Platform / Knowledge Intelligence

```text
Target Capability: AI / Knowledge (RAG)
    |
    +-- Existing implementation: none (README FR-2, FR-3)
    +-- Existing technology: none (Spring AI, Bedrock/Claude, OpenSearch claimed)
    +-- Current maturity: documented intent
    +-- Gap: no ingestion, chunking, embeddings, retrieval, citations, evaluation
    +-- Recommended action: one local RAG path with citations; treat as application feature until reusable
```

### Context Intelligence / Digital Twin

```text
Target Capability: Context / Digital Twin
    |
    +-- Existing implementation: none
    +-- Existing technology: none
    +-- Current maturity: vocabulary only (gNB, cell, topology, KPIs)
    +-- Recommended action: a small explicit model (plant/site/gNB/cell + a few KPIs), not a twin platform
```

### Action Intelligence / MCP / Agent Runtime

```text
Target Capability: Agents, MCP, governed actions
    |
    +-- Existing implementation: none
    +-- Existing technology: none
    +-- Current maturity: absent
    +-- Gap: no agents, tools, MCP servers/clients, policy runtime, human approval
    +-- Recommended action: do not implement MCP or an Agent Factory in Phase 1
```

### Learning Intelligence

```text
Target Capability: Continuous learning / RL
    |
    +-- Existing implementation: none
    +-- Existing technology: none
    +-- Current maturity: absent
    +-- Recommended action: not in Phase 1
```

### Security

```text
Target Capability: Security
    |
    +-- Existing implementation: none
    +-- Existing technology: none (Keycloak/Cognito, ALICE, JWT, mTLS claimed)
    +-- Current maturity: absent
    +-- Gap: no authn/authz, secrets, TLS, audit
    +-- Recommended action: local auth stub + audit log of “recommendations only”; no live gNB writes
```

### Observability / Operations / Developer Experience

```text
Target Capability: Observability, Ops, DevEx
    |
    +-- Existing implementation: none
    +-- Existing technology: none
    +-- Current maturity: absent
    +-- Gap: no health, metrics, traces, CI, local compose, LICENSE
    +-- Recommended action: health endpoint + compose + CI build/test as part of Phase 1
```

---

## Reusable Capabilities

**Nothing in this repository is reusable as runtime software.**

What **should be retained** as intent (not as code):

- The problem statement: 5G planning/optimization for radio engineers  
- The four functional themes: ingest KPIs, recommend changes, 3GPP RAG, policy on writes  
- The decision to keep billing as a later phase  
- The interface matrix idea (trigger → gateway → service → store)

What should **not** be treated as locked technology choices until Phase 1 proves them: Kong, EKS, MSK, OpenSearch Serverless, Ionic, ALICE, Bedrock/Claude, 10k RPS Go.

---

## Gaps

Important missing capabilities (all currently empty):

1. Any compilable service  
2. Any data model or store  
3. Any API contract  
4. Authentication and authorization  
5. RAG / LLM integration  
6. Telemetry ingest  
7. Network/topology domain model  
8. Policy enforcement on actions  
9. Observability  
10. Deployment path (even local)  
11. Tests and CI  
12. License and repo hygiene (README is an unformatted SRS dump)

---

## Architecture Contradictions

| Contradiction | Severity | Why |
|---------------|----------|-----|
| README presents a production EKS/Kong/MSK/ALICE system; git has only Markdown | **CRITICAL** | Operators or investors could treat the repo as implemented |
| “Agentic AI” in the title; no agents, tools, or orchestration | **HIGH** | SNIP agent runtime cannot be mapped to code |
| ALICE named as mandatory write-gate; no ALICE client or policy pack | **HIGH** | FR-4 cannot be demonstrated |
| NFR-1 (10k RPS, &lt;15 ms) with no service to measure | **HIGH** | Performance claims are unfalsifiable |
| AWS-specific stack (ALB, MSK, OpenSearch Serverless, Bedrock, EKS, Cognito) vs SNIP “federate / portable platform” principle | **MEDIUM** | Early cloud lock-in without a running product |
| Billing called out in the repo title; marked Phase 2 only | **LOW** | Naming oversells scope |
| Ionic + WSS + map clusters specified before any API exists | **MEDIUM** | UI platform chosen before the first vertical slice |

These are **not** defects in running software. They are **spec-vs-reality** and **spec-vs-SNIP-target** gaps. They were not “fixed” in Phase 0.

---

## Security Assessment

- No OAuth2, OIDC, JWT, Keycloak, Cognito, or ALICE code  
- No secrets, TLS, or mTLS configuration  
- No RBAC/ABAC policies  
- No audit log  
- No LICENSE (legal/compliance gap for a public repo)  
- `.git/config` has only the public remote; no credentials observed in the tree  

**Finding:** security is entirely prospective. The README’s mTLS + immutable ALICE audit stream is appropriate for later regulated operation, not for Phase 1.

**Do not** implement live topology writes (antenna tilt, frequency rewrite) until a policy and audit path exist. Phase 1 actions should be **recommendations only**.

---

## Data Assessment

No PostgreSQL schemas, telemetry stores, topology graphs, 3GPP documents, embeddings, logs, or event streams exist.

README destinations:

| Flow | Claimed store | In repo |
|------|----------------|---------|
| KPI streams | Amazon MSK | No |
| RAG / 3GPP | OpenSearch Serverless | No |
| Topology overrides | Relational DB / GitOps | No |

**Authoritative sources today:** none.  
**Duplication / lineage / quality:** not applicable.

Phase 0 rule observed: no new copies of enterprise data were created.

---

## AI / RAG Assessment

```text
Prototype
   |
Proof of Concept     <-- not reached
   |
Application Feature
   |
Reusable Platform Capability
   |
Enterprise Capability
```

**Current level:** below Prototype. There is a written FR-3 (embed → vector lookup → 3GPP excerpt → resolution step). There is no parser, chunker, embedder, vector store, prompt layer, citation, access control, or evaluation harness.

“AI agents” appear in the executive summary only. No agent classes, tools, memory, planning, or human-approval flow.

Spring AI, LangChain, Bedrock, Claude, and OpenSearch clients: **not present**.

---

## MCP Assessment

MCP does **not** exist (no clients, servers, tools, resources, registries, or gateways).

**Do not implement MCP in Phase 0 or Phase 1.**

Later fit (conceptual only):

```text
                 AI Agent
                    |
                    v
               MCP Gateway
                    |
          +---------+---------+
          |                   |
          v                   v
    Local MCP Server    Remote MCP Server
          |                   |
          v                   v
   Planning API (Java)   3GPP / OSS APIs
   KPI read API (Go)     ALICE / IdP
```

Services that **could** become governed MCP tools later, once they exist:

- Read-only KPI query  
- 3GPP retrieval with citations  
- “Propose tilt change” (never apply without policy)  
- Topology read  

---

## Observability Assessment

None of: structured logs, metrics, tracing, health/readiness/liveness, correlation IDs, audit events, Kafka monitoring, AI telemetry.

**Production readiness:** not applicable (no runtime). Phase 1 should add `/health` and request logs so the slice is operable.

---

## Deployment Assessment

```text
Developer Workstation   -- missing (no compose, no README how-to-run)
        |
        v
       Build            -- missing (no Maven/Go/npm)
        |
        v
     Container          -- missing (no Dockerfile)
        |
        v
    Kubernetes          -- missing (no manifests/Helm/EKS)
        |
        v
     Runtime            -- missing
```

**Works today:** `git clone` and read `README.md`.  
**Architectural only:** ALB, Kong, EKS, HPA, multi-stage images, mTLS mesh.

---

## Testing Assessment

| Type | Count |
|------|-------|
| Unit | 0 |
| Integration | 0 |
| API / contract | 0 |
| Database | 0 |
| End-to-end | 0 |
| Performance | 0 |
| Security | 0 |
| AI evaluation | 0 |

No coverage numbers exist; none were invented.

**Critical untested paths:** all of FR-1…FR-4, because they have no code.

---

## Technical Debt

Prioritised for SNIP evolution (not style nits):

1. **Security** — public repo with no LICENSE; title implies a live agentic/billing product  
2. **Data integrity** — no model of gNB/cell/KPI; later RAG and twins will invent schemas ad hoc  
3. **Reliability** — NFRs written as if a platform exists  
4. **Scalability** — 10k RPS and EKS specified before a single request handler  
5. **Maintainability** — SRS pasted into `README.md` as one unformatted paragraph; hard to version as architecture  
6. **Developer productivity** — no scaffold, so every first commit will invent structure  

---

## Risks

```text
Risk: README treated as implemented platform
  |
  +-- Impact: wrong delivery dates, false security posture
  +-- Likelihood: high
  +-- Why it exists: title and architecture diagram read as as-built
  +-- Recommended mitigation: keep this report as the as-built record; keep SRS markdown in the repository root

Risk: Building the full AWS/Kong/EKS/ALICE stack before a vertical slice
  |
  +-- Impact: months of infra with no engineer-facing outcome
  +-- Likelihood: high if Phase 1 copies the README literally
  +-- Why it exists: SRS lists the target end-state, not a first increment
  +-- Recommended mitigation: Phase 1 local compose only

Risk: Live network actions without policy
  |
  +-- Impact: unsafe gNB/frequency changes
  +-- Likelihood: medium if FR-2 is implemented as write APIs
  +-- Why it exists: FR-2/FR-4 describe operational modifications
  +-- Recommended mitigation: recommendations + human approval only in Phase 1

Risk: AWS-only data/AI choices become the platform
  |
  +-- Impact: SNIP cannot federate or run elsewhere
  +-- Likelihood: medium
  +-- Why it exists: every store in the README is an AWS managed service
  +-- Recommended mitigation: interfaces first; managed AWS later if needed

Risk: “Agentic” and MCP scope creep
  |
  +-- Impact: no shippable slice
  +-- Likelihood: high
  +-- Why it exists: SNIP volumes and README both name agents
  +-- Recommended mitigation: no MCP, no Agent Factory, no RL until authorised after Phase 1

Risk: ALICE dependency unclear
  |
  +-- Impact: blocked FR-4 or a Mercedes-specific control in a public repo
  +-- Likelihood: medium
  +-- Why it exists: ALICE is named without an owner, contract, or environment
  +-- Recommended mitigation: architectural decision (see below)
```

---

## Existing Code vs Target Architecture

```text
CURRENT STATE
      |
      v
README-only git repo (SRS)
      |
      v
Architecture Gap Analysis (this report)
      |
      v
TARGET STATE
SNIP platform + 5G planning product on top
```

1. **What already exists?** A written product SRS and a public git remote.  
2. **What can be reused?** The problem framing and FR themes — not binaries or libraries.  
3. **What needs extension?** Everything must be created; there is no core to extend.  
4. **What needs refactoring?** Split SRS out of `README.md` when implementation starts (keep markdown in the repository root, not a rewrite of a platform).  
5. **What is missing?** Runtime, data, AI, security, integration, ops, tests.  
6. **What should NOT be built yet?** MCP, Agent Factory, reinforcement learning, billing, EKS/Kong/MSK production mesh, live OSS write-backs, a new AI platform, digital-twin runtime.

---

## Phase 1 Recommendation

Superseded for implementation by **Phase 1A**: see `SNIP-PHASE-1A-IMPLEMENTATION-SPEC.md`. The recommendation below is the discovery-time source that Phase 1A refined (Knowledge + thin Context; Spring Boot + Spring AI; local-first; no MCP).

Phase 1 must be small, testable, deployable, and tied to a real engineering job: **answer a radio-planning question with cited 3GPP text and optional KPI context, without changing the live network.**

**In scope**

- Repository layout for one Java/Spring Boot API (or equivalent) and a minimal web page  
- Local `docker-compose`: API + one vector/document store (or embedded store)  
- Ingest a **small, licensed/public** 3GPP (or sample) document set  
- RAG query API: question → retrieve chunks → answer **with citations**  
- Optional: 3–5 **synthetic** gNB/cell KPI rows as prompt context (files or Postgres), not Kafka  
- `GET /health`, structured request logs  
- Unit tests for retrieval assembly; one API test for a known question  
- CI: build + test on `main`  
- README that says how to run locally (keep the SRS as a root markdown file)

**Out of scope (do not do in Phase 1)**

- Go 10k RPS ingest, Amazon MSK, OpenSearch Serverless, EKS, Kong, ALB, Ionic app, ALICE, mTLS mesh, Bedrock lock-in, billing, MCP, agents that call write tools, live AMF/UDM/gNB integration

**Why this is architecturally meaningful**

It exercises Knowledge Intelligence, a thin Data/API boundary, and a **non-actioning** AI path. It produces learning about document quality, citations, and latency without pretending SNIP already exists.

**Success criteria**

- `docker compose up` works on a developer machine  
- One recorded question returns an answer plus source chunk IDs  
- CI is green  
- No production cloud account required  

---

## First Vertical Slice

Recommended business capability: **“3GPP-backed planning copilot (read-only).”**

Engineer question, e.g. *“What should I check if BLER is high on a mid-band cell?”*

```text
Sample / public 3GPP excerpts (+ optional synthetic KPIs)
    |
    v
Ingest / chunk / embed (local)
    |
    v
Retrieve + generate with citations
    |
    v
Recommendation text (no network write)
    |
    v
Engineer reviews
    |
    v
Feedback: thumbs / comment (log only)
```

This is **not** the full SNIP example (Agent → MCP → governed action). That full chain is explicitly **not** the first slice, because those layers do not exist and would be invented.

A later slice (Phase 2+) can add: real KPI ingest, a site/gNB model, policy-gated **propose** (not apply) actions, then MCP.

---

## Questions Requiring Architectural Decisions

1. **Is this repository the SNIP platform, or a domain product that will sit on SNIP?** The README is a 5G planning app; SNIP volumes describe a multi-domain enterprise platform. Phase 1 should assume **domain product first**.  
2. **ALICE:** is there a real ALICE API this public repo may call, or was it illustrative? If illustrative, Phase 1 must not depend on it.  
3. **Cloud:** must Phase 1 be AWS-native, or local-first with AWS as a later option?  
4. **Document rights:** which 3GPP/spec corpus may be stored and embedded?  
5. **License** for the public GitHub repository (currently unspecified).  
6. **UI:** is Ionic required for the first slice, or is a single HTML/API client enough until the copilot works?

No other decisions are required to start Phase 1 as specified above.

---

## Phase 0 Completion Checklist

- [x] Repository inspected  
- [x] Existing architecture understood (specification only)  
- [x] Existing technologies identified (none in code)  
- [x] Existing services identified (none)  
- [x] Existing data stores identified (none)  
- [x] Existing integrations identified (none)  
- [x] Existing AI/RAG identified (documented only)  
- [x] MCP state assessed (absent)  
- [x] Security assessed  
- [x] Observability assessed  
- [x] Deployment assessed  
- [x] Testing assessed  
- [x] Architecture gaps identified  
- [x] Architecture contradictions identified  
- [x] Technical debt identified  
- [x] Risks identified  
- [x] Phase 1 proposed  
- [x] First vertical slice proposed  
- [x] Discovery report produced  

---

## Final Position

Inspect first, understand second, recommend third.

The existing repository is the starting point: **a written SRS in git**.  
The SNIP architecture is the target direction: **not present in code**.  
The bridge is a **small, local, cited, read-only planning copilot** — not Kong, EKS, MCP, or agents.

**Phase 1A is specified in** `SNIP-PHASE-1A-IMPLEMENTATION-SPEC.md`. Implement only what that spec authorises.
