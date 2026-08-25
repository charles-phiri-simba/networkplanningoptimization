# ADR 031 — Shared LLM and AgentModelResolver

## Status

Accepted for Phase 5.

## Context

Phase 5 must specialise Agents without introducing model diversity.

## Decision

All Agents share one physical model profile resolved by `AgentModelResolver` (`stub` in CI, `qwen2.5:7b` on `local-ai`). Agent-specific temperature, token limits, timeouts, and instructions remain. No dynamic routing or per-Agent physical models.

## Consequences

CI does not require Ollama. Heterogeneous models are deferred.
