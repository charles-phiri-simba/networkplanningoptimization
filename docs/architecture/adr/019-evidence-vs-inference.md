# ADR 019 — Evidence versus inference separation

## Status

Accepted for Phase 3.

## Context

The architecture forbids promoting inference to confirmed root cause.

## Decision

Operational evidence rows store deterministic facts (threshold, trend, correlated KPI). Decision assessments label contributor statements as inference. Prompt instructions require the distinction. Stub and API tests assert `humanReviewRequired=true` and that confirmed root cause is not established.

## Consequences

Knowledge citations remain separate from operational evidence arrays in the assessment DTO.
