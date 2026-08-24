# ADR 020 — ProposedAction and lifecycle

## Status

Accepted for Phase 4.

## Context

Phase 4 must persist intent separately from execution.

## Decision

`ProposedAction` is the core object. Creation never executes. Lifecycle is bounded: `PROPOSED` / `POLICY_EVALUATED` / `APPROVAL_REQUIRED` / `APPROVED` / `REJECTED` / `DENIED` / `EXECUTING` / `SUCCEEDED` / `FAILED`. Invalid transitions fail closed.

## Consequences

No generic workflow engine. APPLY is denied at policy time.
