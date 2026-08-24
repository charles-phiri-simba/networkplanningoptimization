# SNIP Phase 1A.1 — Semantic RAG Validation Specification

## 1. Purpose

Phase 1A.1 is a bounded corrective validation increment between Phase 1A and Phase 1B.

Phase 1A established the read-only planning copilot scaffold, but completion verification found two material gaps: retrieval is lexical rather than embedding/vector-based, and the verified end-to-end path uses a deterministic stub rather than an actual LLM.

Phase 1A.1 must validate:

```text
Enterprise Knowledge + Synthetic Context
                 |
                 v
          Semantic Retrieval
                 |
                 v
         Grounded LLM Reasoning
                 |
                 v
      Cited Engineering Recommendation
                 |
                 v
               Human
```

Phase 1B is NOT authorised by this specification.

## 2. Repository and Required Reading

Repository:

```text
https://github.com/charles-phiri-simba/networkplanningoptimization.git
```

Before modifying code, inspect the repository and read:

```text
docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md
docs/implementation/SNIP-IMPLEMENTATION-STATUS.md
docs/implementation/SNIP-PHASE-0-DISCOVERY-REPORT.md
docs/implementation/SNIP-PHASE-1A-IMPLEMENTATION-SPEC.md
docs/implementation/SNIP-PHASE-1A-COMPLETION-REPORT.md
```

The completion report is evidence, not a substitute for inspecting the implementation.

## 3. Core Rule — Extend, Do Not Rewrite

Preserve working Phase 1A components and abstractions, including where appropriate:

- Spring Boot application and existing API contract
- document ingestion and `DocumentChunker`
- citation/provenance model
- synthetic KPI context
- correlation IDs and logging
- existing tests
- `RecommendationGenerator` abstraction
- stub generator
- retrieval abstraction
- lexical retriever as deterministic fallback/test implementation

Do not perform a broad redesign.

## 4. Required Target Path

Phase 1A.1 must prove this real path:

```text
Corpus
  |
  v
Chunking + Provenance
  |
  v
Real Embedding Model
  |
  v
Vector Store
  |
  v
Semantic Retrieval
  |
  +----------------------+
  |                      |
  v                      v
Retrieved Knowledge   Synthetic KPI Context
  |                      |
  +----------+-----------+
             |
             v
       Context Assembly
             |
             v
         Actual LLM
             |
             v
  Grounded Recommendation
             |
             v
      Verified Citations
```

## 5. Technology Direction

Continue using Java, Spring Boot and Spring AI.

Prefer a local AI runtime such as Ollama, or an equivalent local runtime that integrates cleanly with Spring AI. The primary Phase 1A.1 acceptance path must not require a paid external AI API.

Select chat and embedding models based on Spring AI compatibility, local resource requirements, licensing, reproducibility and sufficient quality for this validation corpus.

Record significant choices in ADRs.

## 6. Real Embeddings

Integrate a real embedding model.

Required ingestion path:

```text
Document -> Chunk -> Embedding Model -> Vector -> Vector Store
```

Requirements:

- embedding model is configurable;
- credentials are never hard-coded;
- local execution is preferred;
- chunk provenance survives storage;
- re-ingestion behaviour is predictable;
- ordinary CI does not depend on an unavailable external cloud service.

Document the selected model and relevant resource requirements.

## 7. Real Vector Retrieval

Implement actual vector similarity retrieval behind the existing retrieval abstraction where practical.

Required query path:

```text
Question -> Query Embedding -> Vector Similarity Search -> Top-K Chunks
```

Top-K must be configurable.

Preserve source ID, locator, chunk ID and provenance metadata.

The architectural requirement is real embeddings plus real vector similarity retrieval. PostgreSQL/pgvector is preferred if its introduction remains proportionate; a suitable embedded/local vector store is acceptable if it proves the same architectural behaviour with less complexity.

Do not install PostgreSQL merely to claim PostgreSQL is present.

Record the vector-store decision in an ADR.

## 8. Preserve Lexical Retrieval

Do not automatically delete `LexicalRetriever`.

It may remain for deterministic tests, fallback behaviour, comparison/evaluation or future hybrid retrieval.

However, the mandatory Phase 1A.1 semantic acceptance path MUST use vector/semantic retrieval.

Clearly distinguish configured retrieval modes.

Hybrid retrieval is not required.

## 9. Actual Local LLM

Introduce and exercise a real LLM through the existing generator abstraction.

Target:

```text
RecommendationGenerator
        |
    +---+---+
    |       |
    v       v
  Stub    Local LLM
```

Keep the deterministic stub for CI.

The local-AI profile must invoke a real model through Spring AI.

Do not remove deterministic offline testing in order to add the real model.

## 10. Grounding

The actual LLM must receive retrieved knowledge explicitly as context.

Clearly distinguish:

- user question;
- retrieved knowledge;
- synthetic KPI context;
- behavioural instructions.

Instruct the model to:

- answer from supplied evidence;
- avoid unsupported network facts;
- acknowledge insufficient evidence;
- remain read-only;
- never claim it executed a network change;
- produce an engineering recommendation rather than an execution result.

## 11. Citation and Provenance

The LLM must not invent citations.

Use:

```text
Vector Retrieval
      |
      +----> Context supplied to LLM
      |
      +----> Citation objects from retrieved metadata
```

Every returned citation must trace to an actual retrieved chunk.

Where practical, clean the Phase 1A issue where provenance header lines appear unnecessarily in snippets without removing the provenance metadata itself.

## 12. Empty or Insufficient Retrieval

Preserve the refusal-to-invent behaviour.

If sufficient evidence is unavailable, return an explicit insufficient-evidence response and do not fabricate citations or authoritative recommendations.

Add tests for this behaviour.

## 13. Synthetic KPI Context

Preserve the existing synthetic KPI capability.

Use the existing canonical context such as:

```text
contextId=cell-midband-001
```

The actual LLM should receive this context in a clearly labelled form.

It must remain explicitly synthetic.

Do not introduce Kafka or real telemetry ingestion.

## 14. Mandatory End-to-End Validation

Run this question through the REAL semantic RAG + LLM path:

> What should I check if BLER is high on a mid-band cell?

Record:

- question;
- context ID;
- retrieval mode;
- embedding model;
- chat model;
- retrieved source IDs and locators;
- similarity scores if meaningful;
- generated answer;
- citations;
- retrieval latency;
- generation latency;
- total service latency;
- observed issues.

Code existing in the repository is not sufficient evidence. The real path must actually execute.

## 15. Evaluation / Regression Set

Add at least three evaluation cases.

### Case 1 — Canonical BLER question

Expected: relevant BLER/mid-band knowledge, correct source citation(s), grounded engineering recommendation.

### Case 2 — Unsupported question

Expected: no fabricated citations and explicit insufficient-evidence behaviour where appropriate.

### Case 3 — KPI context

Expected: synthetic KPI context contributes meaningfully to the response and is not represented as production telemetry.

Do not require byte-for-byte deterministic LLM output. Test stable properties.

## 16. Deterministic CI

Maintain two concerns:

```text
CI / deterministic path
  - stub generator
  - deterministic unit/API tests
  - build verification

Local semantic-AI validation path
  - real embedding model
  - vector retrieval
  - actual local LLM
  - semantic RAG E2E validation
```

Do not force ordinary CI to download large models on every run.

Document separately how semantic integration validation is executed.

## 17. Observability

Add separate measurements where practical:

```text
retrievalLatencyMs
generationLatencyMs
totalLatencyMs
retrievalHitCount
```

Preserve correlation IDs.

Do not log secrets, model credentials, protected documents or sensitive prompt content unnecessarily.

## 18. Docker Compose and Profiles

Improve local reproducibility.

Where appropriate, use Compose profiles such as:

```text
default/stub
local-ai
```

The local-AI profile may start the application, local model runtime and vector store if required.

Do not add unrelated enterprise infrastructure.

## 19. Configurable Host Port

Phase 1A verification found host port 8080 occupied by another local container.

Make the host-facing Compose port configurable, for example through an environment variable such as:

```text
SNIP_HOST_PORT
```

with a documented default.

The application's internal port may remain unchanged.

## 20. Spring AI Configuration

Review the existing OpenAI starter/dummy-key configuration.

Desired conceptual separation:

```text
Stub profile
  -> no real cloud model required

Local AI profile
  -> local chat + embedding model

Future cloud profile
  -> explicit configuration only
```

Avoid misleading cloud configuration in profiles that do not use it.

Do not introduce a cloud dependency simply to resolve configuration cleanliness.

## 21. ADRs

Create concise ADRs under:

```text
docs/architecture/adr/
```

At minimum:

1. Semantic Retrieval Strategy — embedding approach, vector store, alternatives, consequences.
2. Local LLM Strategy — runtime, chat model, Spring AI integration, CI separation.
3. Phase 1A.1 Profile Strategy — deterministic stub profile versus semantic/local-AI profile.

Do not make these decisions silently.

## 22. Tests

Preserve all existing passing tests.

Add appropriate tests for:

- semantic retrieval;
- vector-store integration where practical;
- provenance through vector storage/retrieval;
- citation construction;
- insufficient evidence;
- profile/configuration selection;
- KPI context;
- API behaviour.

Fix the previously identified weak/tautological lexical retriever assertion if that test remains.

Do not weaken tests to make the new implementation pass.

## 23. Security Boundary

Phase 1A.1 remains strictly READ ONLY.

Do not:

- modify network configuration;
- invoke OSS write APIs;
- execute gNB/AMF/UDM commands;
- add autonomous remediation;
- add privileged enterprise tools.

Local model/vector services must not be unintentionally exposed externally.

Commit no secrets.

## 24. Explicitly Out of Scope

Do NOT implement:

```text
MCP
MCP Gateway or Registry
Enterprise MCP Servers
Autonomous Agents
Agent Factory
Multi-Agent Orchestration
Kafka / Amazon MSK
EKS
Kong
ALICE integration
Cognito
Live OSS integration
Live network writes
Billing
Reinforcement Learning
Full Digital Twin
Enterprise Knowledge Graph
Production cloud deployment
Full enterprise IAM
Phase 1B functionality
```

## 25. Git and CI Verification

Phase 1A completion reported that the implementation had not yet run in GitHub Actions because the sources were still local/uncommitted.

Before claiming remote CI success:

- ensure the intended changes go through the normal repository workflow;
- ensure GitHub Actions actually executes on the relevant pushed branch/PR;
- verify the remote result.

Do not push or alter repository history unless authorised by the user.

If push permission is not authorised, stop at a clean locally verified state and clearly state what remains.

## 26. Acceptance Criteria

Phase 1A.1 is complete only when:

### Existing Phase 1A preserved

- [ ] Application builds.
- [ ] Existing API works.
- [ ] Health works.
- [ ] Synthetic KPI context works.
- [ ] Citations trace to real chunks.
- [ ] Existing tests pass.
- [ ] No network-write capability exists.

### Embeddings

- [ ] Real embedding model integrated.
- [ ] Corpus chunks receive embeddings.
- [ ] Query embeddings are generated.
- [ ] Embedding configuration is externalised.

### Vector retrieval

- [ ] Real vector similarity mechanism is used.
- [ ] Semantic retrieval is exercised end-to-end.
- [ ] Provenance survives vector storage/retrieval.
- [ ] Top-K is configurable.

### Actual LLM

- [ ] A real LLM runs through the application.
- [ ] Spring AI integration is actually exercised.
- [ ] The LLM receives retrieved evidence.
- [ ] Stub remains available for deterministic CI.

### Grounding

- [ ] Canonical BLER answer uses retrieved evidence.
- [ ] Citations come from retrieved chunks.
- [ ] Unsupported retrieval does not fabricate authority.
- [ ] Synthetic KPI context is clearly identified.

### Observability

- [ ] Correlation IDs remain operational.
- [ ] Retrieval latency is observable.
- [ ] Generation latency is observable.
- [ ] Total latency is observable.

### Reproducibility

- [ ] Semantic-RAG startup is documented.
- [ ] Host port is configurable.
- [ ] Another developer can reproduce the local semantic path from documentation.

### Architecture

- [ ] Required ADRs exist.
- [ ] No Phase 1B functionality introduced.
- [ ] No MCP, Kafka, Agents, RL or live network actions introduced.

## 27. Required Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-1A.1-COMPLETION-REPORT.md
```

Include:

1. Executive Summary
2. Scope Delivered
3. Phase 1A Components Preserved
4. Architecture Implemented
5. Embedding Model Decision
6. Vector Store Decision
7. Semantic Retrieval Implementation
8. Local LLM Implementation
9. Stub / CI Profile
10. Grounding Strategy
11. Citation and Provenance Verification
12. Synthetic KPI Context Verification
13. Tests Executed and Results
14. Canonical End-to-End Semantic RAG Test
15. Latency Observations
16. Docker Compose / Local Reproducibility
17. CI Verification
18. Security Review
19. ADRs Created
20. Acceptance Criteria — PASS / FAIL
21. Known Limitations
22. Technical Debt
23. Lessons Learned
24. Recommended Phase 1B Scope
25. Questions Requiring Architectural Decision

End with exactly one of:

```text
PHASE 1A.1 STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 1A.1 STATUS: ACCEPTANCE NOT RECOMMENDED
```

If acceptance is not recommended, identify exactly which criteria remain incomplete.

## 28. Definition of Done

Phase 1A.1 is NOT complete merely because dependencies/classes/configuration exist.

It is complete when this path has actually been demonstrated:

```text
Question
   |
   v
Real Embedding
   |
   v
Vector Similarity Retrieval
   |
   v
Retrieved Evidence + Synthetic Context
   |
   v
Actual LLM
   |
   v
Grounded Engineering Recommendation
   |
   v
Traceable Citations
```

while preserving:

```text
Read-Only Safety
+ Deterministic CI
+ Tests
+ Observability
+ Local Reproducibility
+ Architecture Boundaries
```

## 29. Final Instruction to Cursor

Treat this document as the authorised scope for **Phase 1A.1 only**.

Do not start Phase 1B.

Do not broaden this increment into the future SNIP platform.

Use the abstractions already created in Phase 1A.

The purpose is specific:

> **Prove that SNIP can embed enterprise knowledge, retrieve it semantically, combine it with controlled context, pass that evidence to an actual LLM, and return a grounded engineering recommendation with citations — without performing any enterprise action.**

Once that hypothesis is demonstrated and verified, STOP and produce the Phase 1A.1 completion report.
