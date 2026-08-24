# SNIP Phase 1A — Implementation Specification

**Status:** Architect-approved handoff. This is the coding contract for Phase 1A.  
**Authority:** Phase 0 discovery report + senior architect assessment (2026-08-24).  
**Repository role:** first SNIP **domain application / vertical slice**, not the enterprise platform.  
**Markdown location:** implementation pack under `docs/implementation/`; Cursor rule at `.cursor/rules/snip-architecture.mdc`.

Do not start Phase 1B, MCP, or platform foundation work under this spec.

---

## 1. What this repository is

```text
                    SNIP
         Enterprise Cognitive Platform
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
   Shared Platform   Data          AI/Agent
   Capabilities      Services       Services
                       │
                       ▼
             Domain Applications
                       │
                       ▼
          Network Planning & Optimisation
          (this repository, Phase 1A slice)
```

This GitHub repository is **Network Planning & Optimisation**: the first domain product sitting on the emerging SNIP architecture. It is not volumes 1–15 and must not be turned into the whole platform.

Phase 1A proves **value**, not infrastructure:

> Can SNIP take authoritative enterprise knowledge, assemble a little operational context, and return a useful, cited engineering recommendation for a human to review?

---

## 2. Phase sequence

```text
Phase 0     Repository Discovery                         (done)
    │
    ▼
Phase 1A    Read-Only Knowledge Intelligence Slice       (this spec)
    │
    ▼
Phase 1B    Core SNIP Platform Foundation                (out of scope)
    │
    ▼
Phase 2     Context + Event Intelligence
    │
    ▼
Phase 3     Governed Action + MCP
    │
    ▼
Phase 4     Learning + Agent Factory
```

Phase 1A exercises two cognitive pillars only: **Knowledge Intelligence** and a **thin Context Intelligence** stub. Action Intelligence, MCP, Learning Intelligence, and Agent Factory stay closed.

---

## 3. Settled decisions

| Decision | Resolution | Blocks coding? |
|----------|------------|----------------|
| Repo vs platform | Domain product / first vertical slice | No — settled |
| Runtime | **Spring Boot 3 + Spring AI** | No — settled |
| Quarkus | Do **not** use in Phase 1A. Revisit at Phase 1B if the platform should standardise on Quarkus, Spring, or both | No |
| Deploy target | **Local-first** (`docker compose`). No AWS account required | No — settled |
| ALICE | Illustrative only. **No ALICE client, policy pack, or dependency** | No — settled |
| UI | **Minimal HTML page or thin API client**. Ionic is **not** required | No — settled |
| Cloud mesh | No Kong, ALB, EKS, MSK, OpenSearch Serverless, Cognito | No — settled |
| MCP / agents / RL / billing | Explicitly out | No — settled |
| 3GPP corpus | **Bundled sample/public excerpts only**, with a provenance note in-repo. Do not scrape 3GPP.org. Do not ingest full copyrighted specs | No — settled for 1A |
| License | Add `LICENSE` as **Apache-2.0** unless the owner supplies another SPDX id before merge | No — default set |

If a later decision contradicts this table, update this file before changing code.

---

## 4. The slice

Canonical engineer question (must work as a recorded fixture):

> What should I check if BLER is high on a mid-band cell?

```text
3GPP Knowledge (bundled excerpts)
      +
Synthetic KPI Context (3–5 rows)
      │
      ▼
Knowledge Retrieval
      │
      ▼
Context Assembly
      │
      ▼
LLM Reasoning
      │
      ▼
Cited Engineering Recommendation
      │
      ▼
Human Review
```

Later (not this phase):

```text
Knowledge + Context → Agent → Governance → MCP → Enterprise Capability
```

---

## 5. Build this

```text
                 PHASE 1A
       READ-ONLY KNOWLEDGE SLICE
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
    Documents     API         Synthetic KPI
        │            │            │
        └────────────┼────────────┘
                     ▼
              RAG / Retrieval
                     │
                     ▼
                 LLM Reasoning
                     │
                     ▼
             Cited Recommendation
                     │
                     ▼
                  Human
```

**Application**

- One Spring Boot application (Maven or Gradle).
- `GET /health` (liveness/readiness-equivalent is enough).
- `POST /api/v1/recommendations` (or equivalent): question + optional cell/KPI hint in; recommendation text + citations out.
- Structured request logs (correlation id, latency, retrieval hit count). No PII.
- Minimal web page: question box, submit, show answer + citations. No Ionic, no map, no auth UI.

**Knowledge**

- Ingest a **small** bundled corpus at startup or via a documented local command.
- Chunk, embed, store, retrieve.
- Every answer **must** include citations (source id, locator/section if available, excerpt snippet).
- If retrieval is empty: refuse to invent 3GPP claims; say so.

**Context**

- 3–5 **synthetic** gNB/cell KPI rows (file or local Postgres). Not Kafka.
- Optional request field to attach one row to the prompt (e.g. mid-band cell, elevated BLER).
- This is a context stub, not a digital twin.

**LLM**

- Spring AI against a **local or explicitly configured** model.
- Default path must work offline-friendly: Ollama (or equivalent) in Compose, **or** a documented `OPENAI_API_KEY` / compatible endpoint that is optional.
- Prefer the local path so `docker compose up` does not require a cloud vendor.

**Store**

- One local vector/document store in Compose (pgvector, or an embedded store if retrieval tests stay deterministic).
- No Amazon OpenSearch Serverless.

**Quality**

- Unit tests for chunk assembly / citation mapping.
- One API test for the canonical BLER question against the bundled corpus.
- GitHub Actions (or equivalent) on `main`: build + test. No deploy job.

**Docs**

- Rewrite `README.md` as the front door: what this repo is, how to run locally, how to ask the canonical question.
- Move the current SRS dump from `README.md` into `docs/requirements/product-requirements.md` (edit for headings; do not invent new requirements).
- Keep the Phase 0 report, this spec, CONTEXT, and STATUS under `docs/implementation/`.
- Keep `.cursor/rules/snip-architecture.mdc`.
- Add `LICENSE` (Apache-2.0).

Suggested Phase 1A tree:

```text
networkplanningoptimization/
  README.md
  LICENSE
  docker-compose.yml
  pom.xml
  docs/implementation/         # CONTEXT, STATUS, Phase 0, this spec
  docs/requirements/product-requirements.md
  .cursor/rules/snip-architecture.mdc
  src/
  testdata/
  .github/workflows/ci.yml
```

---

## 6. Do not build

- MCP servers, clients, gateways, or tool registries  
- Agent Factory, autonomous agents, tool-calling write paths  
- Kafka / Amazon MSK / event mesh  
- AWS ALB, EKS, Kong, Cognito, OpenSearch Serverless, Bedrock lock-in  
- ALICE API / ABAC  
- Live AMF/UDM/gNB integration or any network **write** (tilt, frequency, beam)  
- Reinforcement learning / continuous learning  
- Billing  
- Full digital twin  
- Ionic / PWA / native mobile  
- Go 10k RPS ingest service  
- Production secrets management, mTLS mesh, multi-AZ HPA  

Recommendations only. The API must not expose an apply/execute/write-to-network operation.

---

## 7. Interfaces (minimum)

**Health**

```http
GET /health
200  { "status": "UP" }
```

**Recommend**

```http
POST /api/v1/recommendations
Content-Type: application/json

{
  "question": "What should I check if BLER is high on a mid-band cell?",
  "contextId": "cell-midband-001"
}
```

```json
{
  "recommendation": "...",
  "citations": [
    { "sourceId": "...", "locator": "...", "snippet": "..." }
  ],
  "contextUsed": { "id": "cell-midband-001", "kpis": { "bler": 0.12, "band": "mid" } },
  "retrievalEmpty": false
}
```

No auth in Phase 1A. Bind the API to localhost in Compose.

---

## 8. Definition of done

Phase 1A is **done** when all of the following are true on a developer machine **without an AWS account**:

1. `docker compose up` starts the API, the store, and the local model path (or the README states a single extra local prerequisite, e.g. Ollama, and it is scripted).  
2. The canonical BLER question returns a recommendation **and** at least one citation from the bundled corpus.  
3. A request with unknown/empty retrieval does **not** fabricate 3GPP citations.  
4. Optional synthetic KPI context appears in the response when `contextId` is supplied.  
5. `GET /health` returns 200.  
6. Unit + API tests pass locally and in CI on `main`.  
7. `README.md` is a run guide; SRS lives in `docs/requirements/product-requirements.md`; implementation markdown stays under `docs/implementation/`.  
8. `LICENSE` is present.  
9. The tree contains **none** of the items in §6.

**Out of “done”:** pretty UI, auth, cloud deploy, evaluation harness beyond the one fixture, platform shared libraries, MCP.

---

## 9. Implementation rules

- Greenfield: establish a clean module layout from the first commit. Do not paste the SRS architecture into code as if it already exists.  
- Prefer boring, replaceable boundaries: `ingest`, `retrieve`, `assemble`, `generate`. Those names should survive Phase 1B.  
- Do not create SNIP platform packages that pretend shared enterprise services exist.  
- Do not expand scope to “just one” Kafka topic, gateway, or agent.  
- If blocked by model/API keys, keep retrieval + citation assembly testable **without** an LLM (template or stub generator) so CI stays deterministic.

---

## 10. What “authorised to code” means

Implement **only** what this file specifies.

When this spec is attached to a coding prompt, Phase 1A implementation is authorised. Phase 1B and later phases are not.
