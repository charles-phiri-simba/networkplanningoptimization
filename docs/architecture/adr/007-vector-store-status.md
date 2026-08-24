# ADR 007 — Vector store remains SimpleVectorStore

## Status

Accepted for Phase 1B.

## Context

PostgreSQL now exists for structured domain state. That does not by itself justify moving the small sample corpus into pgvector.

## Decision

Keep Spring AI `SimpleVectorStore` for the bundled engineering notes. PostgreSQL stores Site/gNB/Cell/KPI/neighbour state only.

Revisit pgvector when one or more of these is true:

- corpus size makes startup re-embedding costly;
- the index must persist across processes;
- more than one application instance must share the same vector index;
- operational durability of embeddings is required.

## Consequences

`local-ai` still rebuilds embeddings at startup. Structured context is never stored as RAG vectors in Phase 1B.
