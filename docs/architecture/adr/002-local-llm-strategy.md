# ADR 002 — Local LLM Strategy

**Status:** Accepted  
**Date:** 2026-08-24  
**Phase:** 1A.1

## Context

Phase 1A verified the copilot with a stub generator. Phase 1A.1 must invoke a real LLM through Spring AI without making ordinary CI pull large models.

## Decision

- Runtime: Ollama already present on the developer machine.
- Chat model: `qwen2.5:7b` (already local; no extra paid API).
- Integration: `spring-ai-starter-model-ollama` and `SpringAiRecommendationGenerator` (`snip.generator=spring-ai`).
- Stub generator remains the default for tests and CI.
- Cloud/OpenAI starter is removed so stub/local profiles are not disguised as cloud clients.

## Alternatives

| Option | Why not now |
|--------|-------------|
| OpenAI-compatible cloud | Not the 1A.1 acceptance path. |
| Quarkus / LangChain4j | Would rewrite the Phase 1A Spring Boot slice. |
| Tiny CPU-only chat in-process | Weaker quality; Ollama is already installed. |

## Consequences

`local-ai` requires a running Ollama daemon and the two named models. CI uses stub only. Prompt assembly labels question, retrieved knowledge, synthetic KPI, and read-only instructions separately.
