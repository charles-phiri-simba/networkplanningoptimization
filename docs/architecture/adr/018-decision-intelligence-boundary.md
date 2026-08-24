# ADR 018 — Decision Intelligence boundary

## Status

Accepted for Phase 3.

## Context

Phase 3 needs advisory diagnosis without MCP or an Agent runtime.

## Decision

`DecisionIntelligenceService` is an application service. It loads the persisted case, cell context, retrieves engineering notes (with Phase 2 query expansion plus case type), and returns `DecisionAssessment`. `humanReviewRequired` is always true. Structured likely contributors / checks / missing evidence are composed deterministically; the LLM supplies the prose summary and must not change severity/confidence.

## Consequences

GET `/api/v1/assurance/cases/{id}/assessment` is read-only. No tool execution, no network action.
