# SNIP Phase 5 — Agentic Orchestration & Controlled Autonomy Implementation Specification

## 1. Authority and Baseline

This is the authorised implementation specification for Phase 5.

Start from:

```text
Branch: main
Commit: 58c6e4111e83ef32137f2c0ffd083a060bd73796
Phase 4: ARCHITECTURALLY ACCEPTED
CI: PASS
Java tests: 71 tests, 0 failures
Go tests/build: PASS
Working tree: clean
```

Read `SNIP-PHASE-5-AGENTIC-ORCHESTRATION-CONTROLLED-AUTONOMY-ARCHITECTURE.md` completely before coding.

Do not start Phase 6. Do not push a Phase 5 baseline until architectural review grants acceptance.

## 2. Objective

Implement the minimum guided Agentic Orchestration foundation proving:

```text
Finite Objective
   ↓
Chief Orchestration Agent
   ↓
Structured Plan
   ↓
Bounded Specialist Calls
   ↓
Evidence Synthesis
   ↓
Candidate ProposedAction
   ↓
Existing Phase 4 Governance
```

Agents must not bypass Phase 4.

## 3. Preserve Existing Architecture

Do not rewrite or destabilize:

- Knowledge Intelligence/RAG
- Site/gNB/Cell domain
- PostgreSQL/Flyway
- Kafka telemetry/Go simulator
- temporal context/trends
- Assurance Case/detection/severity/confidence
- Decision Intelligence
- Phase 4 ProposedAction/risk/policy/approval
- MCP Gateway/server/capability registry
- action audit/idempotency
- existing CI profiles/tests/ADRs

Phase 5 sits above these layers.

## 4. Implement Exactly Five Initial Agents

```text
chief-orchestrator
knowledge-agent
context-agent
assurance-agent
decision-agent
```

No dynamic Agent creation.

## 5. Agent Registry

Create an in-code registry.

Minimum metadata:

```text
agentId
role
description
enabled
allowedServices
modelProfile
temperature
maxOutputTokens
timeoutMs
maxCalls
```

Permissions must be explicit.

## 6. Shared Model Strategy

Use one shared physical LLM initially. The existing local profile may continue to resolve to:

```text
qwen2.5:7b
```

Do not introduce different physical models merely to differentiate Agents.

## 7. AgentModelResolver

Create `AgentModelResolver` or equivalent model-profile boundary.

All Phase 5 Agent profiles may resolve to the same model.

Do not implement dynamic routing, automatic model choice, or heterogeneous physical models.

## 8. Agent-Specific Behavior

Define distinct role instructions/settings.

Suggested behavior:

```text
Knowledge Agent
  low creativity
  grounded retrieval/summarization

Context Agent
  very low creativity
  structured state interpretation
  avoid speculation

Assurance Agent
  very low creativity
  explain deterministic case/evidence
  never alter severity/confidence

Decision Agent
  moderate synthesis
  distinguish evidence/inference
  may recommend bounded Phase 4 action type

Chief Orchestrator
  low creativity
  plan/delegate/enforce limits/stop
```

Exact numeric configuration may be externalized.

## 9. Agent Permissions

Enforce bounded service access:

```text
Knowledge Agent -> Knowledge/RAG services only
Context Agent -> NetworkContextService only
Assurance Agent -> Assurance read services only
Decision Agent -> synthesis + proposal path only
Chief Orchestrator -> run/delegation services only
```

No Agent may directly access:

```text
McpCapabilityGateway
ActionExecutionService
approval mutation
policy override
network write API
```

Prefer enforceable boundaries over comments.

## 10. AgentRun Persistence

Implement:

```text
runId
objective
status
assuranceCaseId
initiatedBy
startedAt
completedAt
maxSteps
currentStep
maxAgentCalls
maxRetries
timeoutMs
```

Statuses:

```text
CREATED
RUNNING
WAITING_FOR_HUMAN
COMPLETED
FAILED
CANCELLED
```

## 11. AgentPlan / PlanStep

Persist structured plan only.

```text
AgentPlan
 + planId
 + runId
 + objective
```

```text
PlanStep
 + stepNumber
 + agentRole
 + task
 + requiredInputs
 + expectedOutput
 + status
```

Do not persist hidden chain-of-thought.

## 12. No Chain-of-Thought Storage

Do not store raw reasoning scratchpads, private chain-of-thought, unrestricted internal deliberation, or full hidden prompts.

Persist only structured tasks, evidence references, summaries, outputs and audit records.

## 13. Run Memory

Provide bounded per-run working context for:

- plan
- current step
- specialist outputs
- evidence references
- structured findings

It must not become authoritative enterprise state.

## 14. Case Memory

If persisted, keep concise:

```text
assuranceCaseId
runId
summary
findings
proposedActionIds
createdAt
```

Do not persist full prompts/raw deliberation.

## 15. Enterprise Memory Rule

Agents retrieve authoritative data from existing services/stores rather than duplicating it.

## 16. AgentOrchestrationService

Create a clear orchestration boundary responsible for:

- create AgentRun
- create structured plan
- execute plan steps in order
- enforce limits
- invoke only registered/enabled Agents
- collect outputs
- bounded retry
- invoke Decision Agent
- terminate/fail run
- append orchestration audit

It must not invoke MCP.

## 17. Communication

All specialist output returns to the orchestrator. No freeform peer-to-peer Agent mesh.

## 18. Synchronous First

Phase 5 runs are request-bounded/synchronous.

Do not introduce background autonomous workers, recurring runs, indefinite orchestration, or self-resuming loops.

## 19. Control Limits

Externalize and enforce:

```text
maxSteps
maxAgentCalls
maxTotalModelCalls
maxRetriesPerStep
maxProposedActions
perAgentTimeoutMs
overallRunTimeoutMs
```

Recommended initial defaults:

```text
maxSteps=8
maxAgentCalls=10
maxRetriesPerStep=1
maxProposedActions=2
```

## 20. Loop Protection

Stop safely on repeated-step loops, recursive delegation, call-budget exhaustion, model-budget exhaustion, timeout, or proposal-budget exhaustion.

Record `LIMIT_REACHED` audit event where applicable.

## 21. Failure Handling

A specialist may retry once if configured. After exhaustion:

- mark step failed;
- preserve completed evidence;
- do not fabricate missing output;
- produce partial result or fail according to a deterministic orchestration rule.

Document the chosen rule.

## 22. Knowledge Agent Output

Use existing RAG and return structured:

```text
summary
citations
retrievedSources
insufficientEvidence
```

No MCP.

## 23. Context Agent Output

Use existing context services and return structured:

```text
cell/site/gnb
current KPIs
history/trends
configuration
neighbours
provenance
```

No writes.

## 24. Assurance Agent Output

Use persisted Phase 3 state:

```text
caseId
caseType
severity
confidence
status
operationalEvidence
missingEvidence
```

Do not recalculate severity/confidence.

## 25. Decision Agent Output

Inputs:

```text
Knowledge result
Context result
Assurance result
Objective
```

Output:

```text
summary
likelyContributors
recommendedChecks
missingEvidence
candidateAction
humanReviewRequired
```

Candidate action may be absent. If present, it maps to an existing Phase 4 action type.

## 26. Reuse ProposedAction

Do not create `AgentAction`.

Reuse Phase 4 `ProposedAction` and extend provenance as needed:

```text
proposedBy=AGENT
agentRunId
agentId/agentRole
```

Do not change Phase 4 policy semantics.

## 27. Proposal Integration

Required flow:

```text
Decision Agent
 ↓
Candidate Action
 ↓
Orchestration / Proposal Adapter
 ↓
Phase 4 ActionProposalService
 ↓
ProposedAction
 ↓
Phase 4 Policy
```

Agents never assign authoritative risk/policy themselves.

## 28. Phase 4 Authority

Preserve Phase 4 as authoritative for:

```text
risk
policy
approval
MCP execution
audit
idempotency
```

An Agent-proposed `APPLY_CELL_PARAMETER_CHANGE` remains denied.

## 29. No Direct MCP

Tests/code review must show no Agent implementation directly calls or depends on:

```text
McpCapabilityGateway
ActionExecutionService
local MCP HTTP client
/mcp endpoint
```

## 30. AgentRun Audit

Persist append-only:

```text
RUN_STARTED
PLAN_CREATED
STEP_STARTED
STEP_COMPLETED
STEP_FAILED
ACTION_PROPOSED
RUN_COMPLETED
RUN_FAILED
RUN_CANCELLED
LIMIT_REACHED
```

Audit summaries must not contain hidden chain-of-thought.

## 31. APIs

Add conceptually:

```text
POST /api/v1/agent-runs
GET  /api/v1/agent-runs
GET  /api/v1/agent-runs/{runId}
```

Optional cancel endpoint only if meaningful.

Do not expose arbitrary prompt-execution APIs.

## 32. Scenario A — Governed ALLOW Proposal

Objective:

> **Investigate the DEGRADING_RADIO_QUALITY case for CELL-001 and recommend the next safe action.**

Expected:

```text
Orchestrator
 -> Context Agent
 -> Assurance Agent
 -> Knowledge Agent
 -> Decision Agent
 -> GENERATE_REMEDIATION_PLAN candidate
 -> Phase 4 ProposedAction
 -> LOW / ALLOW
```

Phase 5 itself need not execute MCP for orchestration acceptance, but local E2E may continue through Phase 4 to prove boundary integrity.

## 33. Scenario B — Approval Required

Make Decision Agent propose:

```text
SIMULATE_CELL_PARAMETER_CHANGE
```

Expected:

```text
Phase 4 -> MEDIUM -> REQUIRE_APPROVAL
```

Agent may not approve or execute.

## 34. Scenario C — Denied

Make Decision Agent propose:

```text
APPLY_CELL_PARAMETER_CHANGE
```

Expected:

```text
Phase 4 -> HIGH -> DENY
```

No Agent/MCP bypass.

## 35. Bounded-Run Test

Force a run to exceed configured step/call limit.

Expected:

```text
LIMIT_REACHED
safe termination
no infinite loop
no MCP invocation
```

## 36. Specialist Failure Test

Force one specialist failure.

Verify bounded retry, recorded failure, no fabricated evidence, and deterministic partial/failure behavior.

## 37. Permission Tests

Verify:

- Knowledge Agent cannot execute actions;
- Context Agent cannot write network state;
- Assurance Agent cannot alter severity/confidence;
- Decision Agent cannot set policy/approval;
- Orchestrator cannot invoke MCP.

## 38. Shared Model Tests

All Agent definitions resolve through `AgentModelResolver`.

In Phase 5 they may resolve to one shared model profile.

Do not require different physical models.

## 39. Deterministic CI

Default CI must not require Ollama.

Use stub/deterministic Agent model behavior for orchestration tests.

Assert stable structured properties, not model wording.

## 40. Local-AI E2E

Run real local-AI path using the shared model.

Record:

- runId
- objective
- plan steps
- Agents invoked
- model profile
- per-Agent latency
- model-call counts
- specialist outputs
- candidate action
- resulting Phase 4 ProposedAction
- risk/policy
- proof no Agent directly invoked MCP
- total run latency

## 41. Observability

Add enough to inspect:

```text
agentRunsStarted
agentRunsCompleted
agentRunsFailed
agentStepsStarted
agentStepsCompleted
agentStepsFailed
agentModelCalls
agentRetries
agentActionsProposed
agentLimitReached
agentRunLatencyMs
```

Correlate by `runId`, `assuranceCaseId`, and action ID when present.

## 42. PostgreSQL / Flyway

Use the next versioned Flyway migration to persist AgentRun, AgentPlan/steps, concise Case Memory if implemented, and append-only audit.

Do not add another database.

## 43. ADRs

Create sequential ADRs after Phase 4 ADRs for at least:

1. Initial Agent role model
2. Chief Orchestrator boundary
3. Structured plan / no chain-of-thought persistence
4. Three-layer Agent memory strategy
5. Shared LLM + AgentModelResolver
6. Agent least-privilege permissions
7. Phase 4 governance authority over Agent proposals
8. Bounded synchronous orchestration/control limits

## 44. Documentation

Update README/context/status with Phase 5 purpose, roles, orchestration, model strategy, memory, limits, Phase 4 relationship, canonical scenario, and no-direct-Agent-MCP rule.

Keep README concise.

## 45. Explicitly Out of Scope

Do NOT implement:

```text
Agent Factory
Dynamic Agent creation
Self-modifying Agents
Persistent conversational memory
Long-running autonomous Agents
Continuous background Agents
Freeform Agent-to-Agent mesh
Automatic remediation
Direct Agent-to-MCP execution
Agent approval authority
Policy override
Production Agent identity federation
Remote enterprise MCP
Reinforcement Learning
Self-learning Agents
Automatic model routing
Different physical models per Agent
Phase 6 functionality
```

## 46. Acceptance Criteria — Baseline

- [ ] Starts from `58c6e4111e83ef32137f2c0ffd083a060bd73796`.
- [ ] Phase 1–4 regressions pass.
- [ ] Phase 4 governance semantics unchanged.
- [ ] No live network writes added.

## 47. Acceptance Criteria — Agents

- [ ] Chief Orchestrator implemented.
- [ ] Knowledge Agent implemented.
- [ ] Context Agent implemented.
- [ ] Assurance Agent implemented.
- [ ] Decision Agent implemented.
- [ ] Static Agent Registry implemented.
- [ ] Agent permissions explicit.

## 48. Acceptance Criteria — Planning / Memory

- [ ] AgentRun implemented.
- [ ] Structured AgentPlan implemented.
- [ ] PlanStep implemented.
- [ ] No hidden chain-of-thought persisted.
- [ ] Run Memory bounded.
- [ ] Case Memory concise if persisted.
- [ ] Enterprise data remains authoritative.

## 49. Acceptance Criteria — Model

- [ ] Shared underlying LLM strategy implemented.
- [ ] Agent-specific instructions/settings implemented.
- [ ] AgentModelResolver implemented.
- [ ] No multi-model routing introduced.
- [ ] CI does not require Ollama.

## 50. Acceptance Criteria — Controls

- [ ] Max steps enforced.
- [ ] Max Agent calls enforced.
- [ ] Model-call budget enforced.
- [ ] Retries bounded.
- [ ] Per-Agent timeout enforced.
- [ ] Overall run timeout enforced.
- [ ] Max proposed actions enforced.
- [ ] Limit violation terminates safely.

## 51. Acceptance Criteria — Governance

- [ ] Agent-generated actions reuse Phase 4 ProposedAction.
- [ ] Agent provenance recorded.
- [ ] Agent does not assign authoritative risk/policy.
- [ ] Agent cannot approve.
- [ ] Agent cannot invoke MCP.
- [ ] Phase 4 remains authoritative.

## 52. Acceptance Criteria — Canonical Proof

All must pass:

```text
Scenario A
Agent orchestration -> GENERATE_REMEDIATION_PLAN -> Phase 4 LOW / ALLOW

Scenario B
Agent proposal -> SIMULATE_CELL_PARAMETER_CHANGE -> Phase 4 MEDIUM / REQUIRE_APPROVAL

Scenario C
Agent proposal -> APPLY_CELL_PARAMETER_CHANGE -> Phase 4 HIGH / DENY
```

No Agent receives additional privilege.

## 53. Acceptance Criteria — Failure / Audit

- [ ] Specialist failure visible.
- [ ] No fabricated fallback evidence.
- [ ] Bounded retry works.
- [ ] AgentRun audit append-only.
- [ ] Audit contains no hidden chain-of-thought.
- [ ] Run completion/failure persisted.

## 54. Acceptance Criteria — CI / Docs

- [ ] Maven tests pass.
- [ ] Go tests/build remain passing.
- [ ] Required orchestration tests execute.
- [ ] Local-AI path validated separately.
- [ ] README/docs updated.
- [ ] ADRs created.
- [ ] No secrets/generated binaries committed.
- [ ] Phase 6 not started.

## 55. Required Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-5-COMPLETION-REPORT.md
```

Include:

1. Executive Summary
2. Phase 4 Baseline Verification
3. Scope Delivered
4. Agent Model
5. Chief Orchestrator
6. Specialist Agents
7. Agent Registry
8. Agent Permissions
9. AgentRun / AgentPlan
10. Memory Model
11. No-Chain-of-Thought Persistence Review
12. Shared LLM Strategy
13. AgentModelResolver
14. Control Limits
15. Failure / Retry Behavior
16. AgentRun Audit
17. APIs
18. Phase 4 Governance Integration
19. Scenario A — ALLOW
20. Scenario B — REQUIRE_APPROVAL
21. Scenario C — DENY
22. Bounded-Run / Loop Protection
23. Specialist Failure Test
24. Tests
25. Local-AI E2E
26. Observability
27. Security / Least Privilege Review
28. ADRs
29. Performance
30. Acceptance PASS/FAIL
31. Known Limitations
32. Technical Debt
33. Lessons Learned
34. Recommended Next Phase
35. Architectural Questions

End with exactly one:

```text
PHASE 5 STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 5 STATUS: ACCEPTANCE NOT RECOMMENDED
```

Do not mark Phase 5 architecturally accepted yourself.

## 56. Final Instruction to Cursor

Treat this as the authorised scope for **Phase 5 only**.

> **Introduce bounded, role-specialized AI Agents that coordinate evidence gathering and decision synthesis while ensuring every action proposal still enters the existing Phase 4 governance pipeline and no Agent receives direct MCP or network execution authority.**

Do not broaden the phase. Do not start Phase 6.

Do not push or establish a new Git baseline until the completion report has been reviewed and explicit architectural acceptance has been granted.

When implementation and validation are complete, produce the completion report and STOP.
