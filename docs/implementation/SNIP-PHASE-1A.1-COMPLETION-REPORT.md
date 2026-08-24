# SNIP Phase 1A.1 — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-24  
**Contract:** `docs/implementation/SNIP-PHASE-1A.1-SEMANTIC-RAG-VALIDATION.md`  
**Method:** Implement, `mvn -B test`, then execute the **real** `local-ai` path against host Ollama. Phase 1B was not started. Git push was not authorised.

---

## 1. Executive Summary

Phase 1A.1 extends the Phase 1A copilot without rewriting it. The default profile is still lexical retrieval + stub generator (CI-safe). The `local-ai` profile embeds the bundled corpus with **Ollama `nomic-embed-text`**, stores vectors in **Spring AI `SimpleVectorStore`**, retrieves by cosine similarity, and generates with **Ollama `qwen2.5:7b` through Spring AI**.

The mandatory end-to-end path **actually ran**. The canonical BLER question returned a grounded engineering recommendation with three corpus citations and similarity scores. An out-of-domain baking question returned insufficient-evidence with **no citations**. Synthetic KPI `cell-midband-001` was labelled synthetic and used in the LLM prompt.

**PHASE 1A.1 STATUS: ACCEPTANCE RECOMMENDED**

Remote GitHub Actions was not executed because these sources have not been pushed.

---

## 2. Scope Delivered

- Real embedding model (Ollama `nomic-embed-text`), configurable  
- In-process vector store (`SimpleVectorStore`), corpus embedded at startup  
- `VectorSimilarityRetriever` behind `ChunkRetriever`  
- Local LLM via existing `RecommendationGenerator` / Spring AI ChatClient  
- Stub + lexical path preserved for CI  
- Grounded prompt (question / retrieved knowledge / synthetic KPI / instructions)  
- Citations from retrieved metadata only (plus optional score, chunkId)  
- Separate retrieval / generation / total latency in logs and JSON  
- `SNIP_HOST_PORT` for Compose  
- OpenAI dummy-key starter removed  
- Three ADRs  
- Evaluation tests (stable properties) + vector-store unit tests  

---

## 3. Phase 1A Components Preserved

Kept: Spring Boot API contract (`GET /health`, `POST /api/v1/recommendations`), `DocumentChunker`, citation model, synthetic KPIs, correlation IDs, stub generator, `LexicalRetriever`, existing API tests (extended, not deleted).

Additive JSON fields (`retrievalMode`, latencies, `chunkId`, `score`, `synthetic`) do not remove previous fields.

---

## 4. Architecture Implemented

```text
testdata/corpus  --chunk-->  InMemoryChunkStore (always)
                 --embed-->  SimpleVectorStore (local-ai only)
                                    |
Question --embed--> cosine top-K ---+
                                    |
                          ContextAssembler
                                    |
                    +---------------+---------------+
                    |                               |
              stub generator                  Spring AI + Ollama
                    |                               |
                    +------ citations from retrieved metadata
```

Profiles:

| Mode | Retrieval | Generator | Models |
|------|-----------|-----------|--------|
| default | lexical | stub | none |
| `local-ai` | vector | spring-ai | nomic-embed-text + qwen2.5:7b |

---

## 5. Embedding Model Decision

See `docs/architecture/adr/001-semantic-retrieval-strategy.md`.

- **Model:** `nomic-embed-text` (Ollama, ~274 MB), pulled locally 2026-08-24.  
- **Why:** local, no paid API, Spring AI Ollama embedding support.  
- **Config:** `SNIP_EMBEDDING_MODEL`, `spring.ai.ollama.embedding.options.model`.  
- CI uses a **deterministic hash embedding** only inside unit tests (`DeterministicEmbeddingModel`), not on the acceptance path.

---

## 6. Vector Store Decision

See ADR 001.

- **Store:** Spring AI `SimpleVectorStore` (in-memory, rebuilt from corpus at each `local-ai` startup).  
- **Why not Postgres/pgvector:** five demo chunks; a database would be ceremony, not architecture proof.  
- **Re-ingestion:** predictable full rebuild on startup.  
- Provenance metadata: `chunkId`, `sourceId`, `locator`, `snippet` on each `Document`.

---

## 7. Semantic Retrieval Implementation

`VectorSimilarityRetriever`:

```text
Question → query embedding → similaritySearch(topK, minScore) → RetrievedChunk[]
```

- `snip.retrieve-top-k` (default 3)  
- `snip.retrieve-min-score` (`0.60` on `local-ai`)  
- `LexicalRetriever` remains default when `snip.retrieval-mode=lexical`

---

## 8. Local LLM Implementation

See `docs/architecture/adr/002-local-llm-strategy.md`.

- Runtime: host Ollama 0.32.8 at `http://localhost:11434`  
- Chat: `qwen2.5:7b` (already present; 4.7 GB)  
- Class: `SpringAiRecommendationGenerator` (`snip.generator=spring-ai`)  
- Prompt built by `AssembledPrompt.render()` with labelled sections  

---

## 9. Stub / CI Profile

See ADR 003.

- Default YAML: `spring.ai.model.chat=none`, `embedding=none`  
- OpenAI starter/dummy key **removed**  
- `mvn test` does not call Ollama  
- Semantic validation: `mvn spring-boot:run -Dspring-boot.run.profiles=local-ai` (documented in README)

---

## 10. Grounding Strategy

The LLM is instructed to answer only from retrieved knowledge, refuse unsupported facts, stay read-only, and never claim a network change. Citations are **not** parsed from the model; they are attached from retrieval metadata.

---

## 11. Citation and Provenance Verification

Canonical live citations (vector path):

| sourceId | locator | chunkId | score |
|----------|---------|---------|-------|
| sample-bler-midband | section-1#0 | sample-bler-midband::section-1#0 | 0.807 |
| sample-bler-midband | section-1#1 | sample-bler-midband::section-1#1 | 0.775 |
| sample-mid-band-context | section-2#0 | sample-mid-band-context::section-2#0 | 0.743 |

`Source-id:` / `Locator:` lines are stripped from snippet/text (chunker fix). Titles remain.

---

## 12. Synthetic KPI Context Verification

`contextId=cell-midband-001` returned `contextUsed.kpis.synthetic=true`, BLER 0.12, band mid. The LLM text referred to cell-midband-001 / SYNTH-01 / n78 and stated the context was for investigation, not a live change.

---

## 13. Tests Executed and Results

`mvn -B test` — **BUILD SUCCESS**, **11 tests, 0 failures**.

| Class | Tests |
|-------|-------|
| RecommendationApiTest | 3 (preserved Phase 1A) |
| RecommendationEvaluationTest | 3 (canonical, unsupported, KPI) |
| DocumentChunkerTest | 1 (metadata not in snippet) |
| LexicalRetrieverTest | 1 (non-tautological ranking) |
| VectorSimilarityRetrieverTest | 3 (semantic ranking, threshold empty, metadata round-trip) |

---

## 14. Canonical End-to-End Semantic RAG Test

**This path used real Ollama embeddings and `qwen2.5:7b`, not the stub.**

| Field | Value |
|-------|--------|
| Question | What should I check if BLER is high on a mid-band cell? |
| contextId | cell-midband-001 |
| retrievalMode | vector |
| embedding model | nomic-embed-text |
| chat model | qwen2.5:7b |
| Spring profile | local-ai |
| URL | `http://127.0.0.1:18081/api/v1/recommendations` |
| Correlation id | phase1a1-canonical-2 |
| retrievalEmpty | false |
| hits | 3 (table in §11) |
| Health | `GET /health` → `{"status":"UP"}` |

**Generated answer (abridged):** read-only checks — confirm measurement window, compare UL/DL BLER, inspect neighbours/PCI, review load vs throughput, review recent config, consider indoor loss / interference; document findings for human review; do not make live changes. Uses synthetic KPI figures.

**Out-of-domain (same profile):** “How do I bake sourdough bread at high altitude?” → `retrievalEmpty=true`, `citations=[]`, `generationLatencyMs=0`, refuse-to-invent message (min-score 0.60).

**Observed issues:** first empty-question run with min-score 0.35 still returned weakly related radio chunks (scores ~0.47–0.51); threshold raised to **0.60** and re-verified. Host **8080** still occupied by `waodn-backend`; E2E used port **18081**. Snippets can show `�` for em-dashes in PowerShell.

---

## 15. Latency Observations

Canonical (second run, after threshold change):

| Metric | Value |
|--------|--------|
| retrievalLatencyMs | 114 |
| generationLatencyMs | 8985 |
| totalLatencyMs | 9100 |
| HTTP filter | ~same order |

First canonical run: retrieval 114 ms, generation 12545 ms, total 12660 ms. Correlation IDs present in logs and `X-Correlation-Id`.

---

## 16. Docker Compose / Local Reproducibility

- Default `docker compose up`: stub API, host port `${SNIP_HOST_PORT:-8080}`  
- `docker compose --profile local-ai up ollama api-local --build`: Ollama + semantic API  
- Documented Maven semantic path is what was executed (host Ollama already running)  
- README describes model pull and profile  

---

## 17. CI Verification

- Workflow still `mvn -B test` on Java 17 — compatible with the stub profile  
- **Not run on GitHub:** Phase 1A.1 is local/uncommitted; `gh` is not authenticated; user did not authorise push  
- Local equivalent: 11/11 passing  

---

## 18. Security Review

- No secrets committed  
- No OpenAI dummy cloud key  
- API localhost-bound in YAML; E2E on 127.0.0.1:18081  
- Compose publishes 127.0.0.1 only  
- No write/apply/OSS endpoints  
- Ollama stayed on localhost:11434  
- Prompts are not dumped to logs (latencies/hits/mode only)  

---

## 19. ADRs Created

1. `docs/architecture/adr/001-semantic-retrieval-strategy.md`  
2. `docs/architecture/adr/002-local-llm-strategy.md`  
3. `docs/architecture/adr/003-phase-1a1-profile-strategy.md`  

---

## 20. Acceptance Criteria — PASS / FAIL

### Existing Phase 1A preserved

| Criterion | Result |
|-----------|--------|
| Application builds | **PASS** |
| Existing API works | **PASS** |
| Health works | **PASS** (stub tests + local-ai live) |
| Synthetic KPI context works | **PASS** |
| Citations trace to real chunks | **PASS** |
| Existing tests pass | **PASS** (extended suite 11/11) |
| No network-write capability | **PASS** |

### Embeddings

| Criterion | Result |
|-----------|--------|
| Real embedding model integrated | **PASS** (nomic-embed-text via Spring AI/Ollama) |
| Corpus chunks receive embeddings | **PASS** (log: Embedded corpus chunks=5) |
| Query embeddings generated | **PASS** (vector search + scores) |
| Embedding configuration externalised | **PASS** (`SNIP_EMBEDDING_MODEL` / YAML) |

### Vector retrieval

| Criterion | Result |
|-----------|--------|
| Real vector similarity used | **PASS** |
| Semantic retrieval E2E | **PASS** |
| Provenance survives store/retrieve | **PASS** |
| Top-K configurable | **PASS** (`snip.retrieve-top-k`) |

### Actual LLM

| Criterion | Result |
|-----------|--------|
| Real LLM runs through the app | **PASS** (qwen2.5:7b, ~9 s generation) |
| Spring AI exercised | **PASS** |
| LLM receives retrieved evidence | **PASS** (answer tracks retrieved checks + KPI) |
| Stub remains for CI | **PASS** |

### Grounding

| Criterion | Result |
|-----------|--------|
| Canonical BLER uses retrieved evidence | **PASS** |
| Citations from retrieved chunks | **PASS** |
| Unsupported retrieval does not fabricate authority | **PASS** after min-score 0.60 |
| Synthetic KPI clearly identified | **PASS** |

### Observability

| Criterion | Result |
|-----------|--------|
| Correlation IDs | **PASS** |
| Retrieval latency observable | **PASS** |
| Generation latency observable | **PASS** |
| Total latency observable | **PASS** |

### Reproducibility

| Criterion | Result |
|-----------|--------|
| Semantic-RAG startup documented | **PASS** (README) |
| Host port configurable | **PASS** (`SNIP_HOST_PORT`) |
| Another developer can reproduce from docs | **PASS** if they have Ollama + the two models |

### Architecture

| Criterion | Result |
|-----------|--------|
| Required ADRs exist | **PASS** |
| No Phase 1B functionality | **PASS** |
| No MCP/Kafka/Agents/RL/live writes | **PASS** |

---

## 21. Known Limitations

- `SimpleVectorStore` is in-memory; not a production vector DB.  
- Similarity threshold is corpus-specific (`0.60` on local-ai).  
- Remote CI not observed.  
- Compose `local-ai` profile was not the E2E vehicle (host Ollama + Maven was).  
- LLM output is non-deterministic; tests assert properties, not wording.  
- Default `docker compose up` still needs a free `SNIP_HOST_PORT`.

---

## 22. Technical Debt

- Persist/reload vector index instead of re-embedding every startup.  
- Optional pgvector when corpus grows.  
- Grounding checker (answer spans vs chunk text) if LLM quality is later gated.  
- Docker image could pre-declare Ollama wait/health.  
- Chunk titles still appear in snippets (headers `Source-id`/`Locator` stripped).

---

## 23. Lessons Learned

- Vector search without a high enough min-score will “retrieve” weakly related chunks for nonsense questions; the LLM may still refuse, but the API should treat them as empty.  
- Keeping stub/lexical as default is what makes CI honest.  
- Host port conflicts are operational, not architectural; `SNIP_HOST_PORT` is enough for 1A.1.

---

## 24. Recommended Phase 1B Scope

Not authorised. When authorised: shared platform modules, optional Postgres/pgvector, observability baseline — still no MCP, Kafka mesh, agents, or live writes unless a later decision says so.

---

## 25. Questions Requiring Architectural Decision

1. When should `SimpleVectorStore` be replaced by pgvector?  
2. Should `local-ai` become the developer default, or remain opt-in?  
3. Is `qwen2.5:7b` the standing chat model, or should a smaller CPU model be standardised?  
4. When may licensed 3GPP text replace the sample corpus?

---

PHASE 1A.1 STATUS: ACCEPTANCE RECOMMENDED
