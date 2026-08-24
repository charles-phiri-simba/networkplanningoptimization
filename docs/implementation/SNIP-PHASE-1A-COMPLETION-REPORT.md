# SNIP Phase 1A — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-24  
**Contract:** `docs/implementation/SNIP-PHASE-1A-IMPLEMENTATION-SPEC.md`  
**(Requested filename `SNIP-PHASE-1A-IMPLEMENTATION-SPECIFICATION.md` was not present; the approved spec above was used.)**  
**Method:** Source inspection, `mvn -B clean verify`, documented `mvn spring-boot:run` E2E, Docker image build/run. No Phase 1B work.

---

## Executive Summary

Phase 1A is a **local, read-only planning copilot**. It ingests bundled sample notes, retrieves chunks, optionally attaches synthetic KPI context, and returns a cited recommendation for a human. It does not write to a live network.

Verification **ran the code**. The Maven build succeeds. The documented local startup works. The canonical BLER question returns grounded text plus citations from the bundled corpus. Empty retrieval refuses to invent citations. Health, correlation IDs, tests, LICENSE, and the implementation-doc layout match the spec.

Retrieval is **lexical**, not embedding/vector search. Generation defaults to a **deterministic stub**, not a running local LLM. Those choices keep CI offline and match spec §9; they do **not** satisfy a literal reading of §5 “embed” or “Ollama in Compose.” They are recorded as known limitations, not as Phase 1B scope creep.

Out-of-scope capabilities (MCP, Kafka, autonomous agents, RL, ALICE, live writes) are absent from `src/`.

**Verdict:** acceptance is recommended against spec §8 (Definition of done), with the limitations below.

---

## Phase 1A Scope Delivered

| Spec item | Delivered |
|-----------|-----------|
| One Spring Boot 3 application | Yes — `NpoApplication`, Java 17, Boot 3.3.6 |
| `GET /health` | Yes |
| `POST /api/v1/recommendations` | Yes |
| Structured logs + correlation id | Yes |
| Minimal HTML client | Yes — `src/main/resources/static/index.html` |
| Corpus ingest at startup | Yes — `CorpusIngestor` (5 chunks from 4 markdown files) |
| Chunk + citation metadata | Yes — `DocumentChunker` |
| Retrieve | Yes — `LexicalRetriever` (not vector) |
| Embeddings | **No** — in-memory lexical overlap only |
| Store | Yes — `InMemoryChunkStore` (not pgvector) |
| Stub generator (CI default) | Yes — `StubRecommendationGenerator` |
| Spring AI optional path | Present — `SpringAiRecommendationGenerator` when `snip.generator=spring-ai` |
| Ollama / local model in Compose | **No** |
| Synthetic KPI file (5 rows) | Yes — `testdata/kpis.json` |
| Tests + GitHub Actions workflow | Yes (workflow not yet observed on origin/main) |
| README front door, SRS moved, LICENSE | Yes |
| Implementation docs + Cursor rule | Yes |

---

## Repository Structure

```text
networkplanningoptimization/
  README.md
  LICENSE                          # Apache-2.0
  pom.xml
  Dockerfile
  docker-compose.yml
  .github/workflows/ci.yml
  .cursor/rules/snip-architecture.mdc
  docs/implementation/
    SNIP-IMPLEMENTATION-CONTEXT.md
    SNIP-IMPLEMENTATION-STATUS.md
    SNIP-PHASE-0-DISCOVERY-REPORT.md
    SNIP-PHASE-1A-IMPLEMENTATION-SPEC.md
    SNIP-PHASE-1A-COMPLETION-REPORT.md   (this file)
  docs/requirements/product-requirements.md
  testdata/corpus/ + kpis.json + PROVENANCE.md
  src/main/java/com/simba/snip/npo/
    ingest/ retrieve/ assemble/ generate/ context/ api/ service/ web/
  src/test/java/...
```

No `docs/` tree items from §6 (Kong, EKS, MCP packages) exist as code.

---

## Architecture Implemented

```text
testdata/corpus/*.md          testdata/kpis.json
        │                            │
        v                            v
 CorpusIngestor + DocumentChunker   KpiRepository
        │
        v
 InMemoryChunkStore
        │
        v
 LexicalRetriever  ----->  ContextAssembler  ----->  RecommendationGenerator
        │                         │                      (stub default)
        └─────────────────────────┴---------> citations + recommendation
                                                    │
                                                    v
                                                 Human (HTML / curl)
```

HTTP surface (inspected in `src/main/java`):

- `GET /health`
- `POST /api/v1/recommendations`

No apply/execute/write mapping exists. A live `POST /api/v1/apply` probe returned **404**.

---

## Technology Decisions

| Topic | As implemented | Spec |
|-------|----------------|------|
| Runtime | Spring Boot 3.3.6, Java 17 | Spring Boot 3 + Spring AI |
| Build | Maven | Maven or Gradle |
| AI | Spring AI OpenAI starter on classpath; **stub** is default | Stub allowed for CI; local model preferred in Compose |
| Retrieval | Token overlap, min score 2, top-k 3 | Embed + store + retrieve; embedded store allowed if tests stay deterministic |
| Store | In-memory list | pgvector **or** embedded |
| KPI | JSON file, 5 synthetic rows | 3–5 rows, file or Postgres |
| Bind | `127.0.0.1:8080` in `application.yml`; Compose publishes `127.0.0.1:8080` | Localhost in Compose |
| License | Apache-2.0 | Apache-2.0 default |

No ADR files were found in the tree. Decisions live in the Phase 1A spec §3 table.

---

## RAG Implementation

**Ingest.** `CorpusIngestor` runs at startup, reads `testdata/corpus/*.md`, chunk-stores them. Live log: `Ingested corpus chunks count=5`.

**Chunk.** Paragraph packing (~700 chars) with `Source-id` / `Locator` copied onto each `Chunk`.

**Retrieve.** `LexicalRetriever` scores query-term overlap. It is **not** semantic/vector retrieval. No embedding model, no pgvector, no OpenSearch.

**Generate.** Default `StubRecommendationGenerator` concatenates retrieved chunk text (plus KPI sentence). That is grounded extractive assembly, not LLM reasoning. `SpringAiRecommendationGenerator` exists but was **not** exercised (no `OPENAI_API_KEY`, `snip.generator=stub`).

**Empty retrieval.** Unrelated question returns `retrievalEmpty=true`, `citations=[]`, and the refuse-to-invent message. Verified live and in `RecommendationApiTest`.

---

## Citation and Provenance Implementation

Citations are a 1:1 map of **retrieved chunks** (`sourceId`, `locator`, `snippet`). They are not LLM-invented.

Corpus provenance is explicit in `testdata/PROVENANCE.md`: sample notes, **not** 3GPP copies.

Live canonical citations:

| sourceId | locator | Matches corpus file |
|----------|---------|---------------------|
| `sample-bler-midband` | `section-1#0` | `testdata/corpus/bler-midband-checks.md` |
| `sample-mid-band-context` | `section-2#0` | `testdata/corpus/mid-band-radio-context.md` |
| `sample-interference` | `section-3#0` | `testdata/corpus/interference-checklist.md` |

Unrelated core-signaling note was **not** cited for the BLER question.

Limitation: first chunks still contain the `Source-id:` / `Locator:` header lines in `text`/`snippet` because the title paragraph is packed with metadata. Metadata is preserved; the snippet is slightly noisy.

---

## Optional KPI Context Implementation

Present. `testdata/kpis.json` has five synthetic cells. Request `contextId=cell-midband-001` returned:

```json
"contextUsed": {
  "id": "cell-midband-001",
  "kpis": {
    "bler": 0.12,
    "band": "mid",
    "dropRate": 0.018,
    "latencyMs": 28.0,
    "cell": "n78-1",
    "site": "SYNTH-01"
  }
}
```

Unknown `contextId` yields `contextUsed: null`. Not a digital twin; not Kafka.

---

## Tests Executed and Results

Command: `mvn -B clean verify`  
Host: Windows, Maven 3.9.12, JDK 21 (Maven) / compile release 17  
**Result: BUILD SUCCESS**

| Class | Tests | Result |
|-------|-------|--------|
| `RecommendationApiTest` | 3 | PASS (health, canonical BLER+KPI, empty retrieval) |
| `DocumentChunkerTest` | 1 | PASS (sourceId + locator mapping) |
| `LexicalRetrieverTest` | 1 | PASS (BLER chunk ranked above core note) |
| **Total** | **5** | **0 failures** |

---

## Local Deployment Verification

**Documented path (`README.md`): `mvn spring-boot:run`**

Observed 2026-08-24 08:54:32:

- `Started NpoApplication in 1.763 seconds`
- Tomcat on **8080**
- Ingest 5 chunks

Live HTTP (client):

| Call | HTTP | Notes |
|------|------|--------|
| `GET /health` | 200 | `{"status":"UP"}`; `X-Correlation-Id=80efca96-...`; client 131 ms |
| Canonical `POST /api/v1/recommendations` | 200 | correlation id echoed `verify-phase1a-canonical`; client 62 ms |
| Empty retrieval | 200 | `retrievalEmpty=true`, no citations |
| `GET /` UI | 200 | HTML title contains “Planning copilot” |
| `POST /api/v1/apply` | 404 | no write API |

**Docker**

- Image `networkplanningoptimization-api` **built successfully** (`BUILD SUCCESS` inside Dockerfile Maven stage).
- Documented `docker compose up --build` **failed to bind** `8080`: host port already used by container `waodn-backend` (unrelated project). Not a missing Compose file.
- Same image run as `docker run -p 127.0.0.1:18080:8080` (Java 17 in-container): `GET /health` 200, canonical POST 200, ingest log `chunks count=5`.

Default Compose/API path does **not** start Ollama or a vector DB. README documents optional `OPENAI_API_KEY` + `SNIP_GENERATOR=spring-ai`; that path was not executed.

---

## CI Verification

`.github/workflows/ci.yml` is valid GitHub Actions YAML:

- `on: push` to `main` and `pull_request`
- `actions/setup-java@v4`, Temurin 17, Maven cache
- `mvn -B test` (no deploy job)

`gh run list` could not be used (`gh auth login` required). `origin/main` is still commit `2ff8e8e` (README-only). **Phase 1A sources are uncommitted locally**, so CI has **not** run on GitHub `main`.

Local equivalent of the CI command passed.

---

## Observability Verification

From live `mvn spring-boot:run` logs:

```text
[80efca96-7cc2-4da4-9018-9a0a42e96d31] GET /health status=200 latencyMs=31
[verify-phase1a-canonical] recommendation retrievalEmpty=false hits=3 latencyMs=3
[verify-phase1a-canonical] POST /api/v1/recommendations status=200 latencyMs=45
```

- Correlation id in MDC and `X-Correlation-Id` (generated or echoed).
- Request method, path, status, latency.
- Retrieval hit count on recommend.
- Console pattern: `%d{ISO8601} %-5level [%X{correlationId}] ...`

Gaps: retrieval latency and generator/model latency are **not** logged separately. Stub generation is included in the 3 ms service timer. No metrics/tracing backend (not required for 1A done).

---

## Security Review

| Check | Finding |
|-------|---------|
| Hard-coded production secrets | None. `spring.ai.openai.api-key` defaults to `dummy-not-used`. |
| Auth | None (spec: no auth in 1A). API bound to localhost in YAML. |
| Network writes | No write endpoints in source; `/api/v1/apply` → 404. |
| PII in logs | Question text is not logged; hits/latency/path only. |
| Corpus rights | Sample notes + provenance file; no 3GPP scrape. |
| Compose publish | Intended `127.0.0.1:8080`; Docker Desktop still reported a bind conflict on host 8080. |
| Dependency surface | Spring AI OpenAI starter present even when stub is used (unused cloud client config). |

Phase 1A is a local demo, not a hardened service.

---

## Acceptance Criteria — PASS / FAIL

Criteria from spec **§8 Definition of done**, plus verification items called out in this exercise.

| # | Criterion | Result |
|---|-----------|--------|
| 1 | `docker compose up` starts API (+ store / local model, or README extra prerequisite) | **PASS with note.** Maven startup works. Image works. Documented compose command needs a free host 8080. Store is in-process; model path is stub (no extra prerequisite for default). Ollama not in Compose. |
| 2 | Canonical BLER question returns recommendation **and** ≥1 corpus citation | **PASS** (live + test) |
| 3 | Empty retrieval does not fabricate citations | **PASS** (live + test) |
| 4 | Synthetic KPI appears when `contextId` supplied | **PASS** |
| 5 | `GET /health` → 200 `{ "status": "UP" }` | **PASS** |
| 6 | Unit + API tests pass locally and in CI on `main` | **PASS locally / CI not observed on origin/main** |
| 7 | README is run guide; SRS in `docs/requirements/`; implementation markdown under `docs/implementation/` | **PASS** |
| 8 | `LICENSE` present (Apache-2.0) | **PASS** |
| 9 | Tree contains none of spec §6 (MCP, Kafka, EKS, Kong, ALICE, live writes, RL, billing, Ionic, Go ingest, …) | **PASS** (`src/` grep) |
| V1 | Application builds | **PASS** (`mvn -B clean verify`) |
| V2 | Documented local startup | **PASS** (`mvn spring-boot:run`) |
| V3 | Read-only copilot API | **PASS** |
| V4 | Document ingestion | **PASS** |
| V5 | Chunking preserves citation metadata | **PASS** |
| V6 | Embeddings and semantic retrieval | **FAIL** (lexical overlap only; no embeddings) |
| V7 | RAG grounded in retrieved content | **PASS** for stub (answer is retrieved text); LLM path not run |
| V8 | Citations identify actual source chunks | **PASS** |
| V9 | Structured logging and correlation IDs | **PASS** |
| V10 | No production network write capability | **PASS** |
| V11 | MCP, Kafka, autonomous agents, RL not introduced | **PASS** |

---

## End-to-end engineering question (recorded)

**Question**

> What should I check if BLER is high on a mid-band cell?

**contextId:** `cell-midband-001`  
**Correlation id:** `verify-phase1a-canonical`  
**Time:** 2026-08-24 08:54:57 (+02)

**Retrieved source chunks (citations)**

1. `sample-bler-midband` / `section-1#0` — high BLER mid-band checks (measurement window, UL/DL split, neighbours, load, recent config).  
2. `sample-mid-band-context` / `section-2#0` — mid-band coverage/interference context.  
3. `sample-interference` / `section-3#0` — interference checklist (SINR vs RSRP, neighbours).

**Generated answer (stub, abbreviated)**

Cited engineering recommendation (read-only). Do not change the live network. Synthetic context `cell-midband-001` shows BLER 0.12 on a mid-band cell. Then the concatenated retrieved notes (same three sources).

**Citations:** three objects as in the table above; `retrievalEmpty: false`.

**Latency**

| Measure | Value |
|---------|--------|
| Client round-trip | 62 ms |
| HTTP filter (`latencyMs`) | 45 ms |
| Service timer (retrieve + stub generate) | **3 ms** (`hits=3`) |
| Retrieval (separate) | **not instrumented**; ≤ 3 ms |
| Model / LLM | **N/A** (stub; no model call) |

**Observed issues**

- Stub answer is extractive, not reasoned.  
- Unicode dash in markdown showed as `�??` in some PowerShell consoles (file encoding display, not API logic).  
- `docker compose up` blocked by foreign container on 8080.

---

## Known Limitations

1. No embeddings; lexical retrieval only.  
2. Default generator is a stub, not Ollama/Spring AI in Compose.  
3. No separate retrieval vs model latency fields.  
4. CI workflow never executed on GitHub `main` (Phase 1A uncommitted).  
5. Sample corpus is fictional; not licensed 3GPP.  
6. No authentication.  
7. Chunk snippets include YAML-like provenance lines.  
8. Spring AI OpenAI auto-config sits on the default classpath with a dummy key.

---

## Technical Debt

- Replace lexical retrieval with embed + vector store when a local embedder is authorised.  
- Put a local chat model behind `RecommendationGenerator` without breaking CI (profile: stub vs ollama).  
- Split `retrievalLatencyMs` / `generateLatencyMs` in logs (and optionally the JSON).  
- Exclude OpenAI auto-configuration unless `snip.generator=spring-ai`.  
- Docker build should cache Maven dependencies; host Compose port is brittle on machines already using 8080.  
- ADRs were specified in CONTEXT but not created as files.  
- `LexicalRetrieverTest` second assertion is tautological (`noneMatch || first==bler`).

---

## Lessons Learned

- Spec §9 (deterministic stub) and §5 (“embed”) can be read as conflicting; implementation favoured CI determinism. Record that trade-off in an ADR next time.  
- Host port 8080 is not free on a developer workstation that already runs other stacks; localhost publish still needs a unique port or a documented override.  
- Citation quality is easy to prove when the generator is extractive; an LLM path will need a grounding check, not only “citations.length >= 1”.

---

## Architecture Decisions / ADRs Created

**None as files.** Settled decisions remain in `SNIP-PHASE-1A-IMPLEMENTATION-SPEC.md` §3 (domain product, Spring Boot, local-first, no ALICE, no Ionic, sample corpus, Apache-2.0).

---

## Recommended Phase 1B Scope

Do **not** start until explicitly authorised. Suggested 1B (platform foundation **without** MCP/agents):

- Shared module layout / config / observability baseline.  
- Optional local Postgres + pgvector (or equivalent) behind the existing `ChunkStore` / retriever ports.  
- Optional Ollama (or equivalent) Compose profile; keep stub for CI.  
- Lightweight ADRs for retrieval and generator.  
- Decide Spring vs Quarkus for **shared** platform code (Phase 1A app stays Spring unless explicitly changed).  
- Do **not** add Kafka, Kong, EKS, ALICE, MCP, or live OSS writes in 1B unless a later decision says so.

---

## Questions Requiring Architectural Decision

1. Must Phase 1A be **reopened** to add embeddings before 1B, or is lexical retrieval accepted as the 1A knowledge slice?  
2. Should Compose **require** a local LLM (Ollama), or remain stub-default?  
3. Default host port if 8080 is commonly taken?  
4. When may a licensed 3GPP excerpt set replace `testdata/corpus/`?  
5. Is dummy OpenAI autoconfig acceptable until a generator profile exists?

---

PHASE 1A STATUS: ACCEPTANCE RECOMMENDED

Phase 1B is **not** authorised by this report.
