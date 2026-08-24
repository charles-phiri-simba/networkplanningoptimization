# SNIP Phase 1B — Core Network Domain & Context Intelligence Foundation

## 1. Purpose

Phase 1B extends the accepted SNIP Phase 1A/1A.1 baseline:

```text
Branch: main
Commit: 8a2e83889ca726b43c53cf738475d7adeed57afb
CI: PASS
Tests: 11 tests, 0 failures
```

Phase 1A/1A.1 proved Knowledge Intelligence: ingestion, provenance, embeddings, semantic retrieval, real local LLM reasoning, citations, deterministic CI, and read-only safety.

Phase 1B must prove:

> **SNIP can represent cellular-network state as structured domain context and combine that context with Knowledge Intelligence to produce grounded engineering recommendations.**

Phase 1B remains strictly read-only. Phase 2 is not authorised.

## 2. Target Outcome

An engineer should be able to ask:

> **Why might BLER be high on CELL-001?**

SNIP must resolve `CELL-001`, assemble structured network context, retrieve relevant engineering knowledge, invoke the existing reasoning path, and return a grounded recommendation with both knowledge citations and context evidence.

```text
Query + CELL-001
       |
       v
Context Resolution -------- Semantic Knowledge Retrieval
       |                              |
       v                              v
Structured Network Context     Engineering Evidence
       |                              |
       +---------------+--------------+
                       |
                       v
                 Context Assembly
                       |
                       v
                    LLM
                       |
                       v
          Grounded Recommendation
                       |
                       v
       Citations + Context Evidence
                       |
                       v
                     Human
```

## 3. Preserve the Accepted Baseline

Extend rather than rewrite:

- Spring Boot and Spring AI
- existing recommendation API
- `ChunkRetriever`
- lexical retrieval
- vector retrieval
- Ollama integration
- stub generator
- real LLM generator
- citation/provenance mechanism
- correlation IDs
- health endpoint
- structured logging
- deterministic CI profile
- `local-ai` profile
- existing tests and ADRs

Do not redesign the working semantic RAG implementation.

## 4. Exact Scope

Phase 1B includes:

1. Core cellular-network domain model
2. PostgreSQL persistence for structured network state
3. Versioned database migrations
4. Deterministic synthetic/demo network dataset
5. Read-only network-domain APIs
6. Cell context API
7. Context resolution and assembly
8. Knowledge + structured-context integration
9. Context-aware recommendation flow
10. Context evidence/provenance
11. Domain validation and error handling
12. PostgreSQL integration tests
13. Context Intelligence tests
14. Existing RAG regression tests
15. Local-AI end-to-end validation
16. Observability extensions
17. Docker Compose PostgreSQL integration
18. Documentation and ADRs
19. Phase 1B completion report

## 5. Core Domain Model

Implement only the smallest useful model:

```text
Site
 |
 +-- gNB
      |
      +-- Cell
           |
           +-- Radio Configuration
           +-- KPI Observations
           +-- Neighbour Relationships
```

Do not attempt to model every 3GPP object.

### 5.1 Site

Minimum fields:

```text
id
siteId
name
latitude
longitude
status
```

`siteId` is the stable domain identifier. `status` should be constrained.

### 5.2 gNB

Minimum fields:

```text
id
gnbId
name
site reference
vendor
model
status
```

Relationship:

```text
Site 1 ---- * gNB
```

A gNB belongs to one Site in Phase 1B.

### 5.3 Cell

Minimum fields:

```text
id
cellId
name
gnb reference
technology
band
arfcn
pci
bandwidthMHz
duplexMode
status
```

Relationship:

```text
gNB 1 ---- * Cell
```

The canonical demo cell is `CELL-001`.

### 5.4 Radio Configuration

Use a deliberately small extensible representation:

```text
id
cell reference
parameterName
parameterValue
unit
effectiveFrom
```

Example parameters may include `txPower`, `ssbPower`, or `tilt`.

Do not reproduce a complete vendor parameter model.

### 5.5 KPI Observation

Evolve the Phase 1A synthetic KPI representation into persisted observations:

```text
id
cell reference
metric
value
unit
observedAt
source
synthetic
```

Example metrics:

```text
BLER_DL
BLER_UL
DROP_RATE
THROUGHPUT_DL
THROUGHPUT_UL
LATENCY
PRB_UTILIZATION_DL
PRB_UTILIZATION_UL
```

Prefer an extensible observation model over one database column per KPI.

All bundled observations must have `synthetic=true`.

### 5.6 Neighbour Relationship

Minimum fields:

```text
id
sourceCell reference
targetCell reference
relationType
status
```

Do not implement neighbour optimisation.

## 6. Domain Identity and Integrity

Separate persistence IDs from stable domain IDs.

Examples:

```text
Persistence identity: UUID/generated key
Domain identity: SITE-001, GNB-001, CELL-001
```

Enforce at minimum:

```text
Site.siteId unique
Gnb.gnbId unique
Cell.cellId unique

gNB -> existing Site
Cell -> existing gNB
KPI -> existing Cell
Radio configuration -> existing Cell
Neighbour source/target -> existing Cells
Neighbour source != target
```

Use database constraints where appropriate and application validation for domain semantics.

## 7. Persistence Rules

Introduce PostgreSQL for structured network state.

Preferred stack:

```text
Spring Boot
    |
Spring Data JPA
    |
PostgreSQL
```

Use PostgreSQL as the only relational database.

Externalise host, port, database, username and password. Commit no credentials.

Runtime domain APIs are read-only. Database writes are limited to migrations, deterministic demo data and tests.

Do not return JPA entities directly from controllers. Prefer:

```text
Entity -> Service -> DTO/Record -> API
```

Avoid uncontrolled lazy serialization, unnecessary bidirectional graphs, and obvious N+1 queries.

## 8. Schema Management

Use Flyway for versioned migrations.

Preferred lifecycle:

```text
Flyway -> creates/migrates schema
Hibernate -> validates mappings
```

Do not use destructive Hibernate schema generation as the authoritative schema lifecycle.

Keep demo-data loading separate from production-like schema migrations where practical.

## 9. Vector Storage Rule

Do NOT automatically migrate `SimpleVectorStore` to pgvector merely because PostgreSQL now exists.

For Phase 1B:

```text
PostgreSQL       -> structured network context
SimpleVectorStore -> small engineering knowledge corpus
```

Keep these responsibilities separate.

Document triggers for a future pgvector migration, such as corpus growth, index persistence, multi-instance deployment, startup embedding cost, or operational durability.

## 10. Deterministic Demo Dataset

Seed at least:

```text
2 Sites
 |
 +-- >=1 gNB per Site
       |
       +-- multiple Cells
```

The dataset must demonstrate:

- same-site relationships
- neighbour relationships
- different KPI states
- at least one high-BLER cell
- at least one healthier comparison cell

Canonical problematic cell:

```text
CELL-001
```

Include useful synthetic context such as NR/n78, elevated DL BLER, additional KPI observations, radio configuration and neighbours.

Do not encode a final diagnosis into the database.

All operational demo data must explicitly identify itself as synthetic/demo data.

## 11. Read-Only API Boundaries

Add read-only endpoints conceptually equivalent to:

```text
GET /api/v1/sites
GET /api/v1/sites/{siteId}

GET /api/v1/gnbs
GET /api/v1/gnbs/{gnbId}

GET /api/v1/cells
GET /api/v1/cells/{cellId}

GET /api/v1/cells/{cellId}/kpis
GET /api/v1/cells/{cellId}/neighbours
GET /api/v1/cells/{cellId}/context
```

Follow existing repository naming conventions if they differ.

No POST/PUT/PATCH/DELETE domain-management APIs are authorised.

Unknown identifiers should return clear 404-style responses. Do not expose stack traces.

## 12. Context API

`GET /api/v1/cells/{cellId}/context` is the central Phase 1B domain API.

It should assemble:

```text
CellContext
 |
 +-- Cell
 +-- gNB
 +-- Site
 +-- Radio Configuration
 +-- Recent KPI Observations
 +-- Neighbours
 +-- Provenance
```

Return explicit DTOs/records, not raw entities.

## 13. Context Intelligence Boundary

Create a clear service boundary, conceptually:

```text
NetworkDomainService
    -> domain lookup/query operations

NetworkContextService
    -> reasoning-ready context assembly

RecommendationService
    -> question + context + knowledge + generation

ChunkRetriever
    -> knowledge retrieval

RecommendationGenerator
    -> model generation
```

Repositories retrieve persisted state. They do not perform RAG or LLM work.

Context Intelligence assembles state into reasoning-ready context.

Do not create a generic God service.

## 14. Context Resolution

For a request referencing `cellId`, resolve:

1. Cell
2. serving gNB
3. Site
4. relevant/recent KPIs
5. radio configuration
6. neighbours
7. provenance

Define a simple bounded rule for "recent" KPI observations. KPI time semantics must retain `observedAt`.

## 15. Recommendation API Evolution

Preserve the existing recommendation endpoint and evolve it compatibly so a caller can supply structured context, conceptually:

```json
{
  "question": "Why might BLER be high on CELL-001?",
  "cellId": "CELL-001"
}
```

Avoid breaking existing consumers unnecessarily.

## 16. Knowledge + Context Integration

Do not put structured network state into the vector store simply so the LLM can see it.

Use:

```text
Structured Network Context ----+
                               |
                               +--> Reasoning Context --> LLM
                               |
Semantic Knowledge Retrieval --+
```

The prompt should distinguish:

```text
USER QUESTION
STRUCTURED NETWORK CONTEXT
RETRIEVED ENGINEERING KNOWLEDGE
SAFETY / BEHAVIOURAL INSTRUCTIONS
```

The model must know that bundled operational context is synthetic, must remain read-only, must not claim an action was executed, and must acknowledge insufficient evidence.

## 17. Context Evidence and Provenance

Responses should expose concise context evidence in addition to document citations, conceptually:

```json
{
  "answer": "...",
  "citations": [],
  "contextEvidence": {
    "cellId": "CELL-001",
    "gnbId": "GNB-001",
    "siteId": "SITE-001",
    "source": "DEMO_SEED",
    "synthetic": true
  }
}
```

Do not expose every database field.

Do not invent future provenance such as OSS/NMS/EMS if the source is actually demo seed data.

## 18. Recommendation Quality

A context-aware recommendation should:

1. identify relevant observations;
2. connect observations to retrieved engineering knowledge;
3. distinguish evidence from inference;
4. identify reasonable engineering checks;
5. acknowledge missing evidence;
6. remain read-only;
7. require human review.

Do not present the response as a definitive autonomous root-cause determination.

## 19. Transactions

Runtime domain/context operations should use read-only transaction semantics where appropriate.

No runtime API may write network/domain state in Phase 1B.

## 20. Docker Compose and Profiles

Extend Compose with PostgreSQL.

Conceptually:

```text
postgres
  -> structured SNIP domain state

api
  -> Spring Boot

ollama (local-ai)
  -> embeddings/chat
```

Preserve the Phase 1A.1 separation:

```text
CI/test
  -> deterministic, no Ollama

local
  -> PostgreSQL + deterministic/stub path where useful

local-ai
  -> PostgreSQL + semantic retrieval + Ollama
```

Do not create excessive profiles.

## 21. Observability

Preserve existing correlation IDs and retrieval/generation timings.

Add where useful:

```text
contextResolutionLatencyMs
contextCellId
contextFound
kpiObservationCount
neighbourCount
```

Do not log entire domain objects or sensitive payloads.

Maintain correlation across HTTP -> context -> retrieval -> generation.

## 22. Tests — Domain

Add meaningful unit tests for:

- domain identifiers
- relationship rules
- neighbour source != target
- context mapping
- synthetic provenance

Do not write tests that merely test getters/setters.

## 23. Tests — PostgreSQL

Use PostgreSQL-backed integration tests, preferably Testcontainers PostgreSQL.

Verify:

- Flyway migrations apply
- JPA mappings validate
- repositories work
- relationships load correctly
- constraints work
- deterministic fixtures work

Do not substitute H2 merely for convenience.

## 24. Tests — API

Test at minimum:

```text
GET /api/v1/sites/{siteId}
GET /api/v1/gnbs/{gnbId}
GET /api/v1/cells/{cellId}
GET /api/v1/cells/{cellId}/context
```

Cover success, unknown identifier, relationships and context evidence.

## 25. Tests — Context Intelligence

Verify `CELL-001` context contains stable structural properties:

```text
Cell
gNB
Site
KPIs
Radio configuration
Neighbours
Synthetic provenance
```

## 26. Tests — Recommendation Integration

Preserve all Phase 1A/1A.1 regression tests.

Add a deterministic context-aware test for:

```text
question = "Why might BLER be high on CELL-001?"
cellId = "CELL-001"
```

Verify:

- context resolves;
- knowledge retrieval participates;
- context evidence identifies `CELL-001`;
- citations remain valid;
- no write operation occurs.

## 27. Mandatory Local-AI End-to-End Validation

Separately run the actual local-AI path with:

- real embedding model;
- vector retrieval;
- PostgreSQL-backed `CELL-001` context;
- actual local LLM.

Mandatory question:

> **Why might BLER be high on CELL-001?**

Record:

- cell/context identifiers;
- relevant KPI observations;
- neighbour/configuration context used;
- retrieved source chunks;
- generated recommendation;
- citations;
- context evidence;
- context-resolution latency;
- retrieval latency;
- generation latency;
- total latency;
- observed limitations.

The answer must meaningfully use both structured context and retrieved knowledge.

## 28. Security Boundary

Continue:

- localhost-oriented development
- no committed secrets
- read-only APIs
- input validation
- safe logging
- no network actions

Do not add fake enterprise security purely to match future architecture diagrams.

## 29. ADRs

Create/update concise ADRs under `docs/architecture/adr/`.

At minimum:

### Core Network Domain Model
Explain Site/gNB/Cell, KPI observation, neighbour model and intentionally excluded 3GPP complexity.

### PostgreSQL Persistence
Explain why PostgreSQL is justified now and the migration strategy.

### Context Intelligence Boundary
Explain repositories vs domain service vs context service and why structured context is not placed in the vector store.

### Vector Store Status
Confirm whether `SimpleVectorStore` remains and document triggers for pgvector migration.

## 30. Documentation

Update README and implementation documentation with:

- Phase 1B architecture
- domain model
- PostgreSQL startup
- migrations
- demo dataset
- read-only APIs
- context endpoint
- context-aware recommendation example
- deterministic tests
- local-AI validation

Keep README concise; do not turn it into the full architecture library.

## 31. Explicitly Out of Scope

Do NOT implement:

```text
MCP
MCP Gateway / Registry / Servers

Autonomous Agents
Agent Factory
Multi-Agent Runtime

Kafka / Amazon MSK
Event Mesh
Live telemetry streaming

Live OSS/NMS/EMS integration
Vendor network APIs
gNB/AMF/UDM command execution

Network configuration writes
Parameter optimisation execution
Automatic neighbour changes
Autonomous remediation

ALICE integration
Cognito
Enterprise IAM rollout

Full Digital Twin platform
Knowledge Graph platform

Reinforcement Learning
Training pipelines
Python AI-training services

Billing / BSS implementation

AWS production deployment
EKS / Kong / production Kubernetes

Full multi-vendor abstraction
Complete 3GPP information model

Phase 2 functionality
```

Python remains part of the long-term SNIP AI landscape for ML/RL training, but no Python service is required in Phase 1B.

## 32. Why MCP and Kafka Remain Deferred

Maturity sequence:

```text
Phase 1A      Knowledge Intelligence          COMPLETE
Phase 1A.1    Semantic RAG + Actual LLM       COMPLETE
Phase 1B      Structured Context Intelligence NOW
Future        Event/Telemetry Intelligence
Future        Governed Action Intelligence
Future        MCP
```

We need trustworthy domain/context boundaries before exposing actions to Agents.

Kafka is likewise unnecessary for deterministic persisted demo context.

## 33. CI

Extend CI to verify:

```text
Build
Flyway migrations
Unit tests
PostgreSQL integration tests
API tests
Context tests
Phase 1A/1A.1 regressions
```

CI must not require Ollama for the deterministic path.

## 34. Git Discipline

Implement on top of:

```text
8a2e83889ca726b43c53cf738475d7adeed57afb
```

Do not rewrite baseline history or force-push.

Do not push unless the user authorises it.

At completion, first produce clean local verification and the completion report.

## 35. Acceptance Criteria

### Baseline
- [ ] Phase 1A/1A.1 tests still pass.
- [ ] Semantic retrieval still works.
- [ ] Stub CI path still works.
- [ ] Local-AI path remains available.
- [ ] Citations remain traceable.
- [ ] No network writes exist.

### Domain
- [ ] Site implemented.
- [ ] gNB implemented.
- [ ] Cell implemented.
- [ ] Radio configuration implemented.
- [ ] KPI observation implemented.
- [ ] Neighbour relationship implemented.
- [ ] Stable domain IDs and core constraints enforced.

### Persistence
- [ ] PostgreSQL integrated.
- [ ] Flyway/versioned migrations used.
- [ ] JPA mappings validate.
- [ ] Deterministic demo/test data works.
- [ ] PostgreSQL integration tests pass.

### APIs
- [ ] Site lookup works.
- [ ] gNB lookup works.
- [ ] Cell lookup works.
- [ ] KPI lookup works.
- [ ] Neighbour lookup works.
- [ ] Cell context endpoint works.
- [ ] Unknown IDs return correct errors.
- [ ] Runtime APIs remain read-only.

### Context Intelligence
- [ ] `CELL-001` resolves.
- [ ] Context includes Cell, gNB and Site.
- [ ] Context includes relevant KPIs.
- [ ] Context includes radio configuration.
- [ ] Context includes neighbours.
- [ ] Synthetic provenance is explicit.

### Knowledge + Context
- [ ] Existing semantic retrieval integrates with structured context.
- [ ] Structured domain state is not unnecessarily stored as RAG vectors.
- [ ] Prompt distinguishes network context from retrieved knowledge.
- [ ] Context evidence is returned.

### Actual AI Validation
- [ ] Canonical `CELL-001` question runs through actual local-AI path.
- [ ] Actual LLM uses structured context.
- [ ] Actual LLM uses retrieved knowledge.
- [ ] Recommendation is grounded and read-only.
- [ ] Citations remain valid.
- [ ] Response does not claim autonomous root-cause certainty.

### Observability
- [ ] Correlation IDs preserved.
- [ ] Context-resolution latency observable.
- [ ] Retrieval latency observable.
- [ ] Generation latency observable.
- [ ] Useful context counts observable.

### Documentation / Architecture
- [ ] README/docs updated.
- [ ] PostgreSQL and local startup documented.
- [ ] Domain model and APIs documented.
- [ ] ADRs created/updated.
- [ ] No MCP/Kafka/Agents/RL/live telemetry/live writes.
- [ ] No Phase 2 implementation.

## 36. Required Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-1B-COMPLETION-REPORT.md
```

Include:

1. Executive Summary
2. Baseline Verification
3. Scope Delivered
4. Domain Model Implemented
5. Database Architecture
6. Flyway / Migration Strategy
7. Demo Dataset
8. API Implementation
9. Context Intelligence Implementation
10. Knowledge + Context Integration
11. Context Provenance
12. Recommendation Flow
13. Tests Executed
14. PostgreSQL Integration Test Results
15. Canonical CELL-001 End-to-End Test
16. Local-AI Validation
17. Observability Results
18. Security / Read-Only Review
19. ADRs
20. Performance Observations
21. Acceptance Criteria — PASS / FAIL
22. Known Limitations
23. Technical Debt
24. Lessons Learned
25. Recommended Next Phase
26. Questions Requiring Architectural Decision

End with exactly one:

```text
PHASE 1B STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 1B STATUS: ACCEPTANCE NOT RECOMMENDED
```

## 37. Definition of Done

Phase 1B is NOT complete merely because entities/repositories exist.

Actually demonstrate:

```text
CELL-001
    |
    v
PostgreSQL Domain State
    |
    v
Context Resolution
    |
    +-----------------------------+
    |                             |
    v                             v
Structured Network Context   Semantic Knowledge Retrieval
    |                             |
    +--------------+--------------+
                   |
                   v
                Actual LLM
                   |
                   v
       Grounded Engineering Analysis
                   |
                   v
       Citations + Context Evidence
                   |
                   v
                 Human
```

Phase 1B is complete when SNIP proves:

> **A persistent structured cellular-network domain can be resolved into reasoning-ready context, combined with semantically retrieved engineering knowledge, and used by an actual LLM to produce a grounded, traceable, read-only engineering recommendation.**

## 38. Final Instruction to Cursor

Treat this document as the authorised specification for **Phase 1B only**.

Start from the accepted Phase 1A/1A.1 baseline.

Do not redesign semantic RAG.

Do not implement future SNIP architecture prematurely.

Build incrementally and run tests continuously.

Preserve the read-only boundary.

When all acceptance criteria have been evaluated, STOP.

Do not start Phase 2.

Produce `docs/implementation/SNIP-PHASE-1B-COMPLETION-REPORT.md` and wait for architectural review.
