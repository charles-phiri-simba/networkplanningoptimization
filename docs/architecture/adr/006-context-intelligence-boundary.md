# ADR 006 — Context intelligence boundary

## Status

Accepted for Phase 1B.

## Context

Recommendation generation already retrieves knowledge chunks. Structured network state must not be stuffed into the vector store so the LLM can “see” it.

## Decision

Keep three boundaries:

- Repositories load persisted rows.
- `NetworkDomainService` performs domain lookup.
- `NetworkContextService` assembles reasoning-ready `CellContext`.
- `RecommendationService` combines question + cell context + retrieved knowledge + generator.

Structured context is passed in the prompt as **STRUCTURED NETWORK CONTEXT**. Retrieved notes remain **RETRIEVED ENGINEERING KNOWLEDGE**. Citations still come only from retrieved chunks. Context evidence (`cellId`, `gnbId`, `siteId`, `source`, `synthetic`) is returned separately.

## Consequences

The LLM can use CELL-001 observations without embedding inventory rows. Vector search stays responsible for engineering notes only.
