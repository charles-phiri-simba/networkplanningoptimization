# ADR 003 — Phase 1A.1 Profile Strategy

**Status:** Accepted  
**Date:** 2026-08-24  
**Phase:** 1A.1

## Context

CI must stay deterministic. Semantic RAG needs real embeddings and a real LLM. Host port 8080 is often occupied.

## Decision

| Profile / mode | Retrieval | Generator | Spring AI models | When |
|----------------|-----------|-----------|------------------|------|
| default (no Spring profile) | `lexical` | `stub` | `chat=none`, `embedding=none` | `mvn test`, CI, `docker compose up` |
| `local-ai` | `vector` | `spring-ai` | Ollama chat + embedding | Semantic validation |

Host publish port: `SNIP_HOST_PORT` (default 8080) in Compose.

Vector store stays in-process; Ollama is the only extra process.

## Alternatives

A single always-on Ollama dependency was rejected because it would break GitHub Actions.

## Consequences

Developers must opt into `local-ai`. Documentation must describe model pull and the Maven command. Do not enable `local-ai` on ordinary CI.
