# ADR 001 — Semantic Retrieval Strategy

**Status:** Accepted  
**Date:** 2026-08-24  
**Phase:** 1A.1

## Context

Phase 1A used lexical token overlap. Phase 1A.1 must prove real embeddings and vector similarity without introducing an enterprise data platform.

## Decision

- Embedding model: Ollama `nomic-embed-text` on the `local-ai` profile.
- Vector store: Spring AI `SimpleVectorStore` (in-process, rebuilt at startup from `testdata/corpus`).
- Query path: question → embedding → cosine similarity → configurable top-K and minimum score.
- Lexical retrieval remains the default (`snip.retrieval-mode=lexical`) for CI.

## Alternatives

| Option | Why not now |
|--------|-------------|
| PostgreSQL/pgvector | Proportionate later; would add a database just to host a handful of demo chunks. |
| OpenAI embeddings | Conflicts with “no paid external API” for the 1A.1 acceptance path. |
| Delete lexical retrieval | Spec requires it as deterministic fallback. |

## Consequences

Startup on `local-ai` embeds the corpus (predictable rebuild). CI never downloads embedding models. Hybrid retrieval is out of scope.
