# ADR 021 — Deterministic policy enforcement

## Status

Accepted for Phase 4.

## Context

The LLM must not authorise actions.

## Decision

`ActionPolicyEvaluator` maps action types deterministically: GENERATE → ALLOW, SIMULATE → REQUIRE_APPROVAL, APPLY → DENY. Policy ID, reason, and evaluation time are persisted. Risk is likewise deterministic (`LOW`/`MEDIUM`/`HIGH`; `CRITICAL` reserved).

## Consequences

Governance success does not depend on LLM wording.
