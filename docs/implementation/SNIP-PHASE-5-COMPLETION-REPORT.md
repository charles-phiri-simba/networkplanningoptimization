# SNIP Phase 5 — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-24  
**Architecture:** `docs/architecture/SNIP-PHASE-5-AGENTIC-ORCHESTRATION-CONTROLLED-AUTONOMY-ARCHITECTURE.md`  
**Contract:** `docs/implementation/SNIP-PHASE-5-AGENTIC-ORCHESTRATION-CONTROLLED-AUTONOMY-SPECIFICATION.md`  
**Baseline:** `58c6e4111e83ef32137f2c0ffd083a060bd73796` on `main` (Phase 4 architecturally accepted, 71 tests). HEAD is still this commit; Phase 5 is uncommitted working-tree work.  
**Method:** Extend Phase 4; `mvn -B test` (PostgreSQL + Kafka Testcontainers; stub Agent narrator); `go test ./...` and `go build ./cmd/simulator`; separate local-ai E2E against host Ollama `qwen2.5:7b`. Phase 6 was not started. Git push / new baseline were not authorised.

---

## 1. Executive Summary

Phase 5 adds **Autonomy Level 1 — guided Agentic Orchestration**. One Chief Orchestration Agent creates a structured plan and delegates to Context, Assurance, Knowledge, and Decision specialists. Agents may gather evidence and propose a Phase 4 `ProposedAction`. They may not approve, set policy, invoke MCP, or write the live network.

Canonical proofs (CI stub path, 83 tests):

| Path | Agent candidate | Phase 4 outcome |
|------|-----------------|-----------------|
| A | `GENERATE_REMEDIATION_PLAN` | LOW / ALLOW; no Agent MCP |
| B | `SIMULATE_CELL_PARAMETER_CHANGE` | MEDIUM / REQUIRE_APPROVAL; execute blocked |
| C | `APPLY_CELL_PARAMETER_CHANGE` | HIGH / DENY; no MCP |

`mvn -B test`: **83 tests, 0 failures** (2026-08-24T17:59:06+02:00). `go test ./...` PASS. `go build ./cmd/simulator` exit 0. Local-ai E2E: run `ebcd04ce-b5ed-49f4-91e7-abad731864a8` COMPLETED in 27595 ms, proposed `87bd3ca7-4029-46a7-a7a9-ca07a996eac2` GENERATE / ALLOW. No Ollama in CI. No live network write path.

---

## 2. Phase 4 Baseline Verification

| Check | Result |
|-------|--------|
| Started from `58c6e4111e83ef32137f2c0ffd083a060bd73796` | Yes (`git rev-parse HEAD`) |
| Phase 1–4 regressions | PASS (71 baseline tests remain in the 83) |
| Phase 4 risk/policy/approval/MCP unchanged | PASS |
| Kafka default off | PASS |
| No live network write path | PASS |
| No Agent import of `McpCapabilityGateway` / `ActionExecutionService` | PASS |

---

## 3. Scope Delivered

- Five in-code Agents + `AgentRegistry` + `AgentPermissionGuard`
- `AgentModelResolver` over one shared physical model
- `AgentRun` / `AgentPlan` / `PlanStep` / concise Case Memory / append-only run audit
- Synchronous `AgentOrchestrationService` with control limits
- `AgentProposalAdapter` → existing `ActionProposalService`
- APIs `POST/GET /api/v1/agent-runs`
- Flyway `V6__agent_orchestration.sql`
- ADRs 027–034, README, CONTEXT/STATUS, cursor rule
- Canonical, limit, specialist-failure, permission, and shared-model tests

---

## 4. Agent Model

Exactly:

```text
chief-orchestrator
knowledge-agent
context-agent
assurance-agent
decision-agent
```

No Agent Factory. No dynamic creation. Specialisation is role, permissions, temperature/limits, and allowed services.

---

## 5. Chief Orchestrator

`ChiefOrchestrationAgent` produces the locked four-step canonical plan. `AgentOrchestrationService` sequences steps, enforces limits, collects outputs, optionally proposes via the adapter, and terminates. The Chief is not given MCP, approval, or execution collaborators.

---

## 6. Specialist Agents

| Agent | Allowed service | Output |
|-------|-----------------|--------|
| Knowledge | existing RAG (`ChunkRetriever`) | summary, citations, sources, `insufficientEvidence` |
| Context | `NetworkContextService` | cell/site/gNB, KPIs, trends, config, neighbours, provenance |
| Assurance | `AssuranceCaseService` read | case type, persisted severity/confidence/status, evidence, missing evidence |
| Decision | synthesis only | summary, contributors, checks, missing evidence, optional candidate Phase 4 type |

Empty RAG does not fabricate citations. Assurance does not recalculate severity/confidence.

---

## 7. Agent Registry

In-code `AgentRegistry` with `agentId`, role, description, enabled, `allowedServices`, shared `modelProfile=shared-llm`, temperature, maxOutputTokens, timeoutMs, maxCalls.

---

## 8. Agent Permissions

`AgentPermissionGuard` fails closed on disallowed services. Constructor inspection tests prove Agents and `AgentOrchestrationService` do not depend on `McpCapabilityGateway`, `ActionExecutionService`, `ActionApprovalService`, or `McpServerController`. Proposal is an orchestrator adapter, not an Agent MCP client.

---

## 9. AgentRun / AgentPlan

Persisted fields match the specification. Statuses: CREATED, RUNNING, WAITING_FOR_HUMAN (unused), COMPLETED, FAILED, CANCELLED. Plan steps are first-class rows with short `outputSummary` only.

---

## 10. Memory Model

- **Run Memory:** ephemeral `AgentRunMemory`, discarded when the request ends.
- **Case Memory:** concise summary + JSON findings + proposed action IDs.
- **Enterprise Memory:** unchanged PostgreSQL / RAG / Assurance / Action stores. Agent memory is not authoritative.

GET `/api/v1/agent-runs/{id}` reconstructs the run from plan/steps/case memory/audit, not from hidden prompts.

---

## 11. No-Chain-of-Thought Persistence Review

Persisted: structured plan, step task/expected output/status, short summaries, citations/evidence references, case-memory findings, audit event types and bounded summaries.

Not persisted: raw prompts, private chain-of-thought, unrestricted scratchpads, full hidden deliberation.

---

## 12. Shared LLM Strategy

One shared physical model. CI: `stub`. `local-ai`: `qwen2.5:7b`. Agent-specific temperature and output limits remain in the registry.

---

## 13. AgentModelResolver

Every Agent definition resolves through `AgentModelResolver`. Stub tests assert one physical model (`stub`). Spring-ai profile tests assert one physical model (`qwen2.5:7b`). No routing, no automatic model selection.

---

## 14. Control Limits

Externalised on `snip.agent-*` (defaults: maxSteps=8, maxAgentCalls=10, maxTotalModelCalls=20, maxRetriesPerStep=1, maxProposedActions=2, perAgentTimeoutMs=8000, overallRunTimeoutMs=30000). Request may override steps/calls/retries/timeout for tests. Limit violations emit `LIMIT_REACHED` and fail the run.

---

## 15. Failure / Retry Behavior

Deterministic rule:

1. A specialist may retry once (`maxRetriesPerStep`).
2. **Context, Assurance, or Decision** failure after retry → `STEP_FAILED`, later steps `SKIPPED`, run `FAILED`, no proposal.
3. **Knowledge** failure after retry → `STEP_FAILED`, Decision still runs with missing-evidence noted; no fabricated knowledge output.
4. Do not invent specialist evidence on failure.

---

## 16. AgentRun Audit

Append-only: `RUN_STARTED`, `PLAN_CREATED`, `STEP_STARTED`, `STEP_COMPLETED`, `STEP_FAILED`, `ACTION_PROPOSED`, `RUN_COMPLETED`, `RUN_FAILED`, `LIMIT_REACHED`. Summaries are clipped and do not store chain-of-thought.

---

## 17. APIs

```text
POST /api/v1/agent-runs
GET  /api/v1/agent-runs
GET  /api/v1/agent-runs/{runId}
```

No arbitrary prompt-execution API. Cancel omitted: runs are synchronous and finish in the request. These POST APIs mutate SNIP orchestration state only.

---

## 18. Phase 4 Governance Integration

`AgentProposalAdapter` calls `ActionProposalService` with `proposedBy=AGENT`, `agentRunId`, `agentId=decision-agent`. Risk and policy remain `ActionPolicyEvaluator`. Agents cannot approve or execute.

---

## 19. Scenario A — ALLOW

CI: objective “recommend the next safe action” → candidate `GENERATE_REMEDIATION_PLAN` → LOW / ALLOW → `proposedBy=AGENT` → MCP invocation count unchanged.

---

## 20. Scenario B — REQUIRE_APPROVAL

CI: objective contains `SIMULATE_CELL_PARAMETER_CHANGE` → MEDIUM / REQUIRE_APPROVAL → execute 409 → MCP unchanged.

---

## 21. Scenario C — DENY

CI: objective contains `APPLY_CELL_PARAMETER_CHANGE` → HIGH / DENY → execute 409 → no MCP audit events.

---

## 22. Bounded-Run / Loop Protection

`maxSteps=1` → first step completes, remaining `SKIPPED`, `LIMIT_REACHED`, run `FAILED`, no proposal, MCP unchanged.

---

## 23. Specialist Failure Test

Forced `knowledge-agent` failure: one retry, `STEP_FAILED` on Knowledge, Decision still completes, output summary is the failure reason (not fabricated 3GPP text).

---

## 24. Tests

`mvn -B test`: **83 tests, 0 failures**. New: `AgentRegistryTest` (4), `AgentModelResolverTest` (2), `AgentOrchestrationApiTest` (5), `AgentSpecialistFailureApiTest` (1). Phase 4 `GovernedActionApiTest` still PASS (7).

`go test ./...` PASS. `go build ./cmd/simulator` exit 0.

---

## 25. Local-AI E2E

Profile `local-ai`, shared model `qwen2.5:7b`, host Ollama, Postgres `networkplanningoptimization-postgres-1`, app on `127.0.0.1:18080`.

| Field | Value |
|-------|--------|
| runId | `ebcd04ce-b5ed-49f4-91e7-abad731864a8` |
| objective | Investigate DEGRADING_RADIO_QUALITY for CELL-001 and recommend the next safe action |
| assuranceCaseId | `d6c6bbcd-c0cc-4ce9-b0fd-a0a3b31c86e2` |
| status | COMPLETED |
| plan | Context → Assurance → Knowledge → Decision (all COMPLETED) |
| Agents invoked | chief-orchestrator + four specialists |
| model profile | `shared-llm` / `qwen2.5:7b` / generator `spring-ai` |
| Chief plan latency | ~11.8 s |
| Context | ~5.4 s |
| Assurance | ~2.8 s |
| Knowledge | ~4.0 s (`insufficientEvidence=false`, 2 sources) |
| Decision | ~3.5 s (`GENERATE_REMEDIATION_PLAN`) |
| agentCalls / modelCalls | 5 / 5 |
| candidate | GENERATE_REMEDIATION_PLAN |
| ProposedAction | `87bd3ca7-4029-46a7-a7a9-ca07a996eac2` |
| risk / policy | LOW / ALLOW (`POLICY_GOVERNED_ACTION_V1`) |
| proposedBy / agentId | AGENT / decision-agent |
| Agent MCP | none (action audit has ACTION_PROPOSED + POLICY_EVALUATED only; result null) |
| agentRunLatencyMs | 27595 |
| client elapsed | ~27741 ms |

---

## 26. Observability

Counters and logs: `agentRunsStarted/Completed/Failed`, step started/completed/failed, `agentModelCalls`, `agentRetries`, `agentActionsProposed`, `agentLimitReached`, `agentRunLatencyMs`. Correlated by `runId`, `assuranceCaseId`, and action id when present.

---

## 27. Security / Least Privilege Review

Agents are constrained principals. No Agent inherits MCP Gateway or ActionExecutionService. Governance POST APIs still do not write the live network. Direct LLM-to-MCP remains prohibited.

---

## 28. ADRs

- 027 Initial Agent role model
- 028 Chief Orchestrator boundary
- 029 Structured plan / no chain-of-thought persistence
- 030 Three-layer Agent memory
- 031 Shared LLM + AgentModelResolver
- 032 Agent least-privilege permissions
- 033 Phase 4 governance authority over Agent proposals
- 034 Bounded synchronous orchestration / control limits

---

## 29. Performance

CI orchestration tests ~10.9 s for five HTTP runs including Testcontainers reuse. Local-ai canonical run ~28 s (five shared-model narrations). No new observability platform.

---

## 30. Acceptance PASS/FAIL

### Baseline
- [x] Starts from `58c6e4111e83ef32137f2c0ffd083a060bd73796`
- [x] Phase 1–4 regressions pass
- [x] Phase 4 governance semantics unchanged
- [x] No live network writes added

### Agents / planning / model / controls / governance / canonical / failure / CI
All specification checklists in §§47–54 are **PASS** for the implemented scope. Local-ai validated separately as required.

---

## 31. Known Limitations

- Per-Agent timeout is an elapsed-time check after the specialist returns; it does not cancel a hung model call. Accepted as Phase 5 technical debt; do not redesign. A future interruptible timeout / run watchdog remains required.
- GET run DTO leaves reconstructed specialist objects null; truth is in plan steps, case memory, and audit.
- `WAITING_FOR_HUMAN` exists on `AgentRun` but is unused (Phase 4 owns approval).
- Decision candidate type in CI is deterministic from the objective text so Scenarios B/C are stable; the LLM narrates summaries only.
- Failed-result row replacement remains Phase 4 technical debt (untouched).

---

## 32. Technical Debt

- Interruptible per-Agent timeout / overall run watchdog. Accepted as Phase 5 technical debt; preserve the requirement, do not redesign in this phase.
- Richer GET reconstruction of specialist structured payloads (without storing CoT).
- Bind Kafka listener `groupId` (Phase 2, still deferred).
- Immutable execution-attempt history for FAILED Phase 4 results (still deferred).

---

## 33. Lessons Learned

- Audit text must not mention “chain-of-thought” even to deny it; tests looking for leakage will match the denial.
- Nested `snip.agent.force-fail-*` YAML does not bind to a flat `agentForceFailAgentId` field.
- Specialist work must stay on the request thread so JPA lazy-loading of context/assurance remains in session.

---

## 34. Recommended Next Phase

Phase 5 is **frozen**. Do not add functionality, resolve deferred technical debt, perform unrelated refactoring, or start Phase 6 from this report. Agent Factory, remote MCP, heterogeneous models, and live network writes remain closed until explicitly authorised.

---

## 35. Architectural Questions

Phase 5 was architecturally accepted on 2026-08-25. The locked decisions are closed as follows:

1. **Exactly one Chief Orchestration Agent plus Knowledge, Context, Assurance, and Decision specialists — ACCEPT.**
2. **Orchestrator-mediated Agent communication — ACCEPT.**
3. **Synchronous bounded orchestration — ACCEPT.**
4. **Structured AgentPlan / PlanStep — ACCEPT.**
5. **No hidden chain-of-thought persistence — ACCEPT.**
6. **Ephemeral Run Memory, concise persisted Case Memory, existing authoritative Enterprise Memory — ACCEPT.**
7. **Static/in-code Agent Registry — ACCEPT.**
8. **Explicit least-privilege Agent permissions — ACCEPT.**
9. **One shared physical LLM initially — ACCEPT.**
10. **AgentModelResolver retained for future evolution — ACCEPT.** No heterogeneous model routing in Phase 5.
11. **Phase 4 ProposedAction reused for Agent proposals — ACCEPT.** Phase 4 remains authoritative for risk, policy, approval, execution, MCP, and action audit.
12. **Agents have no direct MCP or network execution authority — ACCEPT.**
13. **Non-interruptible per-Agent timeout — ACCEPT as Phase 5 technical debt.** Preserve the future interruptible timeout / run watchdog requirement; do not redesign in Phase 5.
14. **Agent Factory, RL, automatic remediation, remote MCP, heterogeneous models, and Phase 6 remain out of scope.**

---

PHASE 5 STATUS: ARCHITECTURALLY ACCEPTED
