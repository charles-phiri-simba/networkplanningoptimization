# SNIP Phase 1B — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-24  
**Contract:** `SNIP-PHASE-1B-CORE-NETWORK-CONTEXT-SPECIFICATION.md` (copied to repository root and `docs/implementation/`)  
**Baseline:** `8a2e83889ca726b43c53cf738475d7adeed57afb` on `main`  
**Method:** Extend Phase 1A/1A.1; `mvn -B test` with Testcontainers PostgreSQL; run the real `local-ai` path against host Ollama + Compose PostgreSQL. After architectural acceptance, KPI ratio formatting in reasoning context was corrected and re-validated. Phase 2 was not started. Git push was not authorised.

---

## 1. Executive Summary

Phase 1B adds a persistent cellular-network domain (Site / gNB / Cell / radio configuration / KPI observations / neighbours) in PostgreSQL, assembles reasoning-ready context for `CELL-001`, and combines that context with the existing Knowledge Intelligence path.

The mandatory local-AI end-to-end path **actually ran**: Ollama `nomic-embed-text` + vector retrieval + PostgreSQL `CELL-001` context + Ollama `qwen2.5:7b`. The model used both structured KPIs/neighbours and retrieved BLER notes, returned citations from retrieved chunks, labelled the context as `DEMO_SEED` / synthetic, and stayed read-only.

Deterministic CI remains lexical + stub. `mvn -B test`: **31 tests, 0 failures**. No Ollama in CI.

Ratio KPIs are formatted in reasoning context as `BLER_DL: 0.12 ratio (12%)` so the LLM cannot treat `0.12` as `0.12%`. Stored values remain `0.12` / `unit=ratio`.

**PHASE 1B STATUS: ACCEPTANCE RECOMMENDED**

---

## 2. Baseline Verification

| Check | Result |
|-------|--------|
| Started from `8a2e838` | Yes |
| Phase 1A/1A.1 API tests still pass | PASS |
| Lexical + stub default | PASS (`retrievalMode=lexical` in CI tests) |
| Vector retriever unit tests | PASS |
| Citations still from retrieved chunks | PASS |
| No network-write APIs | PASS |
| Semantic RAG not rewritten | PASS (`ChunkRetriever`, `SimpleVectorStore`, Ollama profile retained) |

---

## 3. Scope Delivered

- Core domain model and Flyway schema  
- PostgreSQL + Spring Data JPA (`ddl-auto=validate`)  
- Deterministic demo seed (`SITE-001` / `GNB-001` / `CELL-001` plus comparison cells)  
- Read-only domain APIs and `GET /api/v1/cells/{cellId}/context`  
- `NetworkDomainService` and `NetworkContextService`  
- Recommendation API `cellId` (legacy `contextId` preserved)  
- Context evidence on recommendation responses  
- Observability: context resolution latency, cell id, found flag, KPI/neighbour counts  
- Testcontainers PostgreSQL tests  
- Compose PostgreSQL  
- ADRs 004–007  
- README updates  
- Ratio KPI reasoning-context format (`0.12 ratio (12%)`) with regression tests  

---

## 4. Domain Model Implemented

```text
Site 1--* gNB 1--* Cell
                     +-- radio_configuration
                     +-- kpi_observation
                     +-- neighbour_relationship
```

UUID persistence keys; stable domain IDs `SITE-001`, `GNB-001`, `CELL-001`. Status constrained to ACTIVE/INACTIVE. Neighbour source ≠ target enforced in SQL and `DomainRules`.

---

## 5. Database Architecture

PostgreSQL 16 is the only relational store. Spring Boot → Spring Data JPA → PostgreSQL. Datasource URL/username/password from `SPRING_DATASOURCE_*`. Local Compose password `snip` is development-only.

Runtime controllers never return entities. Mapping is Entity → service records/DTOs.

---

## 6. Flyway / Migration Strategy

| Version | Purpose |
|---------|---------|
| `V1__create_network_domain.sql` | Tables, unique domain IDs, FKs, neighbour self-check |
| `V2__seed_demo_network.sql` | Synthetic demo graph |

Hibernate `ddl-auto=validate`. Demo seed is a separate versioned migration from the schema, not Hibernate `ddl-auto=create`.

---

## 7. Demo Dataset

- `SITE-001` Midband Demo Site → `GNB-001` → `CELL-001` (n78, elevated `BLER_DL=0.12`) and healthier `CELL-002`  
- `SITE-002` → `GNB-002` → `CELL-003` (n41 comparison)  
- Radio: `txPower`, `ssbPower`, `tilt` on CELL-001  
- Neighbours: CELL-001↔CELL-002 intra-frequency; CELL-001→CELL-003 inter-frequency  
- All KPI rows `synthetic=true`, `source=DEMO_SEED`  
- No encoded diagnosis in the database  

---

## 8. API Implementation

Read-only GET endpoints for sites, gNBs, cells, KPIs, neighbours, and cell context. Unknown IDs return `404` JSON `{error, id}` without stack traces. No POST/PUT/PATCH/DELETE domain APIs.

Recommendation:

```json
{ "question": "Why might BLER be high on CELL-001?", "cellId": "CELL-001" }
```

Unknown `cellId` → 404. Legacy `contextId` still attaches `testdata/kpis.json`.

---

## 9. Context Intelligence Implementation

- Repositories: persistence only  
- `NetworkDomainService`: lookups  
- `NetworkContextService`: Cell + gNB + Site + radio + KPIs in the last `snip.recent-kpi-hours` (168h, max 20) + neighbours + provenance  
- `RecommendationService`: question + context + retrieval + generation  

---

## 10. Knowledge + Context Integration

Structured inventory is **not** stored in `SimpleVectorStore`. Prompt sections:

```text
SAFETY / BEHAVIOURAL INSTRUCTIONS
USER QUESTION
STRUCTURED NETWORK CONTEXT
SYNTHETIC KPI CONTEXT (legacy contextId)
RETRIEVED ENGINEERING KNOWLEDGE
```

---

## 11. Context Provenance

`contextEvidence`: `cellId`, `gnbId`, `siteId`, `source=DEMO_SEED`, `synthetic=true`. Context API provenance matches. No OSS/NMS/EMS labels.

---

## 12. Recommendation Flow

```text
cellId → PostgreSQL CellContext
question → ChunkRetriever (lexical or vector)
assemble prompt → RecommendationGenerator (stub or Spring AI)
citations from retrieved chunks only
```

Empty retrieval still does not fabricate citations; context evidence is still returned if `cellId` resolved.

---

## 13. Tests Executed

`mvn -B test` — **BUILD SUCCESS**, **31 tests, 0 failures**.

| Area | Classes |
|------|---------|
| Phase 1A/1A.1 regression | RecommendationApiTest (3), RecommendationEvaluationTest (canonical/unsupported/KPI), DocumentChunkerTest, LexicalRetrieverTest, VectorSimilarityRetrieverTest |
| Domain rules | DomainRulesTest |
| Prompt labelling | AssembledPromptTest |
| PostgreSQL | NetworkPersistenceTest (Flyway seed, unique cell id, FK, neighbour ≠ self) |
| APIs | NetworkDomainApiTest |
| Context | NetworkContextServiceTest |
| KPI ratio formatting | KpiObservationFormatTest (`0.12` → `12%`, `0.03` → `3%`) |
| CELL-001 recommendation (stub) | RecommendationEvaluationTest case4 + unknown cell 404 |

CI-safe: default profile `chat=none` / `embedding=none`, generator stub, lexical retrieval. Testcontainers supplies PostgreSQL. No Ollama.

---

## 14. PostgreSQL Integration Test Results

Flyway applied V1+V2 on Testcontainers `postgres:16-alpine`. JPA validate succeeded. Canonical topology loaded. Unique `CELL-001` and missing-gNB FK inserts failed as `DataIntegrityViolationException`. Self-neighbour insert failed.

---

## 15. Canonical CELL-001 End-to-End Test

Deterministic (stub/lexical): question + `cellId=CELL-001` → `contextFound=true`, evidence CELL-001 / GNB-001 / SITE-001, citations present, `kpiObservationCount=6`, `neighbourCount=2`, recommendation mentions CELL-001. No writes.

---

## 16. Local-AI Validation

**This path used real Ollama embeddings, vector retrieval, PostgreSQL context, and `qwen2.5:7b`.**

Re-run after KPI format fix (2026-08-24):

| Field | Value |
|-------|--------|
| Question | Why might BLER be high on CELL-001? |
| cellId | CELL-001 |
| Profile | `local-ai` |
| URL | `http://127.0.0.1:18083` |
| Postgres | Compose `postgres:16-alpine` on host **55432** |
| Correlation id | phase1b-cell001-units |
| retrievalMode | vector |
| retrievalEmpty | false |
| hits | 3 |
| Citations | sample-bler-midband section-1#0 **0.687**; section-1#1 **0.647**; sample-mid-band-context section-2#0 **0.614** |
| contextEvidence | CELL-001 / GNB-001 / SITE-001 / DEMO_SEED / synthetic=true |
| Latencies | context 147 ms, retrieval 107 ms, generation 14139 ms, total 14395 ms |

Reasoning context now emits `BLER_DL: 0.12 ratio (12%)` and `BLER_UL: 0.03 ratio (3%)`. The generated answer discussed elevated DL vs UL BLER, neighbours CELL-002/CELL-003, measurement window, congestion, and human review. It did **not** state `0.12%` or otherwise treat the stored ratio as a percent of 0.12.

First local-AI run (before the fix) had rendered `0.12` as `0.12%`. That is corrected.

---

## 17. Observability Results

Correlation IDs remain on HTTP logs. Recommendation logs include `contextFound`, `contextCellId`, `kpiObservationCount`, `neighbourCount`, `contextResolutionLatencyMs`, retrieval/generation/total. JSON exposes the same. Domain objects are not dumped to logs.

---

## 18. Security / Read-Only Review

- No production secrets committed  
- Compose binds Postgres and API to 127.0.0.1  
- Domain APIs GET-only  
- Transactions on domain/context services are `readOnly=true`  
- Local-AI used 127.0.0.1:18083 for the unit-format re-run  
- Prompt instructs synthetic + read-only behaviour  

---

## 19. ADRs

- `docs/architecture/adr/004-core-network-domain-model.md`  
- `docs/architecture/adr/005-postgresql-persistence.md`  
- `docs/architecture/adr/006-context-intelligence-boundary.md`  
- `docs/architecture/adr/007-vector-store-status.md`  

---

## 20. Performance Observations

Context resolution is tens of milliseconds. Vector retrieval ~100 ms. Generation dominates (~17 s on `qwen2.5:7b`). Stub path remains sub-10 ms generation.

---

## 21. Acceptance Criteria — PASS / FAIL

### Baseline

| Criterion | Result |
|-----------|--------|
| Phase 1A/1A.1 tests still pass | **PASS** |
| Semantic retrieval still works | **PASS** (unit + live vector E2E) |
| Stub CI path still works | **PASS** |
| Local-AI path remains available | **PASS** |
| Citations remain traceable | **PASS** |
| No network writes exist | **PASS** |

### Domain / persistence / APIs / context

All specified Site/gNB/Cell/radio/KPI/neighbour items, Flyway, Testcontainers, lookup APIs, 404s, CELL-001 context graph, synthetic provenance: **PASS**.

### Knowledge + context / actual AI / observability / docs

Structured state not in the vector store; prompt sections distinct; context evidence returned; live CELL-001 local-AI run grounded and read-only; latencies observable; README + ADRs: **PASS**.

No MCP/Kafka/Agents/RL/live telemetry/live writes/Phase 2: **PASS**.

---

## 22. Known Limitations

- Host **8080**, **18081**, and **18082** were occupied; unit-format E2E used **18083** and Postgres **55432**.  
- `SimpleVectorStore` still in-memory; corpus re-embedded at `local-ai` startup.  
- Demo seed uses `NOW()`-relative timestamps (always “recent”, not a frozen clock).  
- Runtime `mvn spring-boot:run` requires a reachable PostgreSQL; tests use Testcontainers.  
- Remote GitHub Actions for this increment was **not** run (no push).  

---

## 23. Technical Debt

- Persist/reload vector index (still deferred per ADR 007).  
- Optional frozen `observed_at` in seed for bit-identical timestamps.  
- Grounding check that answer spans match both KPI rows and chunk text.  
- Compose health wait already present; document `SNIP_DB_PORT` more prominently if 5432 stays occupied.  

---

## 24. Lessons Learned

- `*IT` test names are skipped by Surefire; CI PostgreSQL tests must be `*Test`.  
- Structured context in the prompt is what made the LLM use CELL-001 KPIs; putting inventory in the vector store was unnecessary.  
- Raw `0.12 ratio` in the prompt was read as `0.12%`; explicit `0.12 ratio (12%)` in the assembled context removed that ambiguity without changing stored values.  
- Port conflicts are operational, not architectural (`SNIP_HOST_PORT` / `SNIP_DB_PORT`).  

---

## 25. Recommended Next Phase

Not authorised. When authorised: event/telemetry intelligence — still no MCP, Kafka mesh, agents, or live writes unless a later decision says so.

---

## 26. Questions Requiring Architectural Decision

1. When should demo seed timestamps be frozen versus `NOW()`-relative?  
2. Should `contextId` JSON KPI files be retired now that CELL-001 lives in PostgreSQL?  
3. When does corpus growth trigger pgvector (ADR 007)?  
4. Should unknown `cellId` on recommendations remain 404, or return insufficient-context 200?

---

PHASE 1B STATUS: ACCEPTANCE RECOMMENDED
