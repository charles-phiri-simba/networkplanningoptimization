# ADR 033 — Phase 4 governance authority over Agent proposals

## Status

Accepted for Phase 5.

## Context

Agents must not gain extra privilege over actions.

## Decision

Do not create `AgentAction`. Agents may produce a candidate Phase 4 action type. `AgentProposalAdapter` calls existing `ActionProposalService`. `proposedBy=AGENT` with `agentRunId` / `agentId`. Phase 4 remains authoritative for risk, policy, approval, MCP, audit, and idempotency.

## Consequences

GENERATE is still ALLOW, SIMULATE still requires approval, APPLY is still DENIED. Agents cannot approve or execute.
