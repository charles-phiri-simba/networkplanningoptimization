# SNIP Phase 5 — Agentic Orchestration & Controlled Autonomy Architecture

## 1. Purpose

Phase 5 introduces the first bounded Agentic Orchestration capability of the Simba Network Intelligence Platform (SNIP).

The accepted implementation progression entering Phase 5 is:

```text
Phase 1A / 1A.1  Knowledge Intelligence + Semantic RAG
Phase 1B         Structured Network Context Intelligence
Phase 2          Event, Telemetry & Temporal Intelligence
Phase 3          Assurance & Decision Intelligence
Phase 4          Governed Action Intelligence & MCP
```

Phase 5 answers:

> **How can SNIP use AI Agents to plan, coordinate, gather evidence, and propose governed actions without allowing those Agents to bypass deterministic policy, approval, capability registration, MCP governance, or human control?**

Phase 5 introduces guided agentic orchestration, not autonomous network operation.

---

## 2. Core Principle

> **Agents may reason, decompose tasks, gather evidence, coordinate specialist work, and propose actions. Agents may not bypass Phase 4 governance.**

```text
Agent Reasoning
    != Task Planning
    != Evidence Retrieval
    != Decision Synthesis
    != Action Proposal
    != Policy
    != Approval
    != MCP Execution
```

---

## 3. Phase 5 Target Architecture

```text
User / Assurance Trigger
          |
          v
Chief Orchestration Agent
          |
          v
      Agent Plan
          |
   +------+------+-------+
   |             |       |
   v             v       v
Knowledge     Context  Assurance
Agent         Agent    Agent
   |             |       |
   +------+------+-------+
          |
          v
     Decision Agent
          |
          v
     Recommendation
          |
          v
      ProposedAction
          |
          v
   PHASE 4 GOVERNANCE
          |
   +------+------+
   |             |
   v             v
 Policy       Approval
   |             |
   +------+------+
          |
          v
      MCP Gateway
          |
          v
Registered Capability
```

---

## 4. Initial Agent Model

Phase 5 starts with exactly one orchestrator and four bounded specialist Agents:

```text
Chief Orchestration Agent
Knowledge Agent
Context Agent
Assurance Agent
Decision Agent
```

Do not introduce a larger Agent ecosystem in this phase.

---

## 5. Chief Orchestration Agent

Responsibilities:

- accept a finite objective;
- create a structured plan;
- delegate tasks;
- sequence specialist calls;
- enforce run limits;
- collect specialist outputs;
- determine whether sufficient evidence exists;
- invoke the Decision Agent;
- terminate the run.

It may not:

- execute MCP capabilities;
- approve actions;
- override deterministic policy;
- alter Assurance severity/confidence;
- write network state;
- spawn arbitrary new Agents.

---

## 6. Knowledge Agent

Purpose:

> **What does authoritative engineering knowledge say about the detected condition?**

Allowed:

- existing Knowledge Intelligence / RAG;
- engineering retrieval;
- source citations;
- grounded summaries.

Prohibited:

- unrestricted external tools;
- MCP execution;
- approvals;
- policy decisions;
- network writes.

---

## 7. Context Agent

Purpose:

> **What structured state is currently true about the affected network entity?**

Allowed:

- Site/gNB/Cell context;
- current KPI state;
- temporal history/trends;
- neighbours;
- configuration;
- provenance.

Prohibited:

- topology writes;
- telemetry mutation;
- configuration changes;
- MCP execution.

---

## 8. Assurance Agent

Purpose:

> **What deterministic condition did SNIP detect and what evidence supports it?**

Allowed:

- persisted Assurance Cases;
- operational evidence;
- deterministic severity/confidence;
- missing-evidence summaries.

Prohibited:

- recalculating or changing severity/confidence;
- acknowledging/resolving cases;
- inventing root cause;
- MCP execution.

---

## 9. Decision Agent

Purpose:

> **Given the gathered evidence, what should the engineer investigate or safely propose next?**

Allowed:

- synthesize specialist outputs;
- distinguish evidence from inference;
- recommend checks;
- produce a candidate Phase 4 action type.

Prohibited:

- execute action;
- assign authoritative risk or policy;
- manufacture approval;
- invoke MCP directly.

---

## 10. Orchestrator-Mediated Communication

Specialist Agents do not form a freeform peer-to-peer mesh.

Required:

```text
Specialist Agent
      |
      v
Chief Orchestrator
      |
      v
Other Specialist / Decision Agent
```

The orchestrator is the control plane for collaboration.

---

## 11. AgentRun

Introduce a bounded `AgentRun`.

```text
AgentRun
 + runId
 + objective
 + status
 + assuranceCaseId
 + initiatedBy
 + startedAt
 + completedAt
 + maxSteps
 + currentStep
 + maxAgentCalls
 + maxRetries
 + timeoutMs
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

No continuously running background Agents are introduced.

---

## 12. Structured AgentPlan

Persist a concise structured plan, not hidden chain-of-thought.

```text
AgentPlan
 + planId
 + runId
 + objective
 + steps[]
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

Canonical plan:

```text
1. Context Agent
   Retrieve CELL-001 structured and temporal context.

2. Assurance Agent
   Load DEGRADING_RADIO_QUALITY case and evidence.

3. Knowledge Agent
   Retrieve engineering guidance relevant to BLER and load.

4. Decision Agent
   Synthesize findings and recommend the next safe action.
```

---

## 13. No Hidden Chain-of-Thought Persistence

Do not persist:

- hidden reasoning traces;
- private chain-of-thought;
- unrestricted scratchpad text;
- full internal model deliberation.

Persist only:

- structured plans;
- task/result summaries;
- citations/evidence references;
- structured findings;
- action proposal provenance;
- audit events.

---

## 14. Memory Model

Phase 5 uses three layers:

```text
Run Memory
  -> ephemeral active-run context

Case Memory
  -> concise persisted summary tied to Assurance Case

Enterprise Memory
  -> existing authoritative PostgreSQL/RAG/Assurance/Action stores
```

---

## 15. Run Memory

Contains only active-run working state such as plan, specialist outputs, evidence references, structured findings, and current step status.

It expires or is discarded at run completion except for deliberately persisted summary/audit records.

---

## 16. Case Memory

Conceptually:

```text
AgentCaseMemory
 + assuranceCaseId
 + runId
 + summary
 + findings
 + proposedActionIds
 + createdAt
```

Do not persist raw prompts or hidden reasoning.

---

## 17. Enterprise Memory

Authoritative information remains in existing systems:

```text
PostgreSQL domain state
Telemetry history
Assurance Cases
Action governance state
Action audit
Vector knowledge store
```

Agent memory is never authoritative over these stores.

---

## 18. Shared LLM Strategy

Phase 5 uses **one shared underlying LLM initially**.

The existing local profile may continue to use:

```text
qwen2.5:7b
```

All five Agents may use the same physical model.

---

## 19. Agent-Specific Behavior

The shared model does not create shared behavior.

Each Agent has distinct:

- role instructions;
- allowed services;
- context;
- temperature;
- output limits;
- timeouts;
- call budgets.

Conceptually:

```text
AgentDefinition
 + agentId
 + role
 + description
 + systemInstructions
 + allowedServices[]
 + modelProfile
 + temperature
 + maxOutputTokens
 + timeoutMs
 + maxCalls
 + enabled
```

---

## 20. AgentModelResolver

Introduce:

```text
Agent
  |
  v
AgentModelResolver
  |
  v
ModelProfile
  |
  v
Chat Model
```

In Phase 5, every Agent profile may resolve to the same physical model.

Future heterogeneous model routing is supported architecturally but not implemented.

---

## 21. Locked Model Strategy

```text
One shared physical model initially: YES
Agent-specific prompts: YES
Agent-specific permissions: YES
Agent-specific temperature/limits: YES
Agent-specific model-profile abstraction: YES
Different physical models per Agent: NO
Dynamic model routing: NO
Automatic model selection: NO
Future heterogeneous models: SUPPORTED
```

> **Phase 5 validates specialization through role, context, permissions, and orchestration—not through model diversity.**

---

## 22. Agent Registry

Use a static/in-code registry initially:

```text
chief-orchestrator
knowledge-agent
context-agent
assurance-agent
decision-agent
```

No dynamic Agent Factory.

---

## 23. Agent Permissions

Explicit least-privilege examples:

```text
Knowledge Agent
  allow: Knowledge/RAG services
  deny: MCP, approval, execution

Context Agent
  allow: NetworkContextService
  deny: topology/network writes, MCP

Assurance Agent
  allow: Assurance read services
  deny: severity/status mutation, MCP

Decision Agent
  allow: synthesis + candidate action proposal
  deny: policy, approval, execution

Chief Orchestrator
  allow: delegation + run control
  deny: policy bypass + MCP execution
```

---

## 24. Control Limits

Every run is bounded by configurable:

```text
maxSteps
maxAgentCalls
maxTotalModelCalls
maxRetriesPerStep
maxProposedActions
perAgentTimeoutMs
overallRunTimeoutMs
```

Recommended initial conceptual defaults:

```text
maxSteps = 8
maxAgentCalls = 10
maxRetriesPerStep = 1
maxProposedActions = 2
```

No infinite loop is permitted.

---

## 25. Synchronous First

Phase 5 orchestration is synchronous and request-bounded.

Do not introduce:

- continuous background Agents;
- recurring autonomous runs;
- indefinite async orchestration;
- self-resuming workflows.

---

## 26. Failure Handling

Failure must remain visible:

```text
Specialist step
   ↓
Failure
   ↓
Bounded retry if permitted
   ↓
Still fails
   ↓
STEP_FAILED
   ↓
partial/incomplete result or FAILED run
```

Never fabricate missing specialist evidence.

---

## 27. Relationship to Human Control

Phase 5 creates no second approval mechanism.

Agents may propose a Phase 4 `ProposedAction`.

Phase 4 remains authoritative for:

```text
risk
policy
approval
MCP execution
audit
```

---

## 28. ProposedAction Reuse

Do not create `AgentAction`.

Use Phase 4 `ProposedAction` with provenance such as:

```text
proposedBy = AGENT
agentRunId
agentId / agentRole
```

```text
Human proposal --+
                  +--> ProposedAction -> Phase 4 Governance
Agent proposal ---+
```

---

## 29. Relationship to Phase 4

Phase 4 is unchanged and authoritative.

Agents cannot:

- set policy;
- approve;
- execute MCP;
- override denial;
- bypass dry-run rules;
- modify action audit semantics.

All execution remains:

```text
ProposedAction
   ↓
Policy
   ↓
Approval if required
   ↓
ActionExecutionService
   ↓
MCP Gateway
   ↓
Registered Capability
```

---

## 30. MCP Relationship

Specialist Agents do not use executable MCP directly.

For Phase 5:

```text
Knowledge Agent -> existing Knowledge/RAG services
Context Agent -> NetworkContextService
Assurance Agent -> Assurance read services
Decision Agent -> synthesis / proposal service
```

MCP remains Phase 4's controlled execution boundary.

---

## 31. Agent Run Audit

Persist append-only orchestration audit:

```text
AgentRunAuditEvent
 + id
 + runId
 + eventType
 + agentId
 + timestamp
 + summary
```

Events include:

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

Audit summaries do not contain hidden chain-of-thought.

---

## 32. Autonomy Level

Phase 5 establishes:

```text
Autonomy Level 1 — Guided Agentic Orchestration
```

Agents may plan, retrieve, synthesize, delegate, and propose.

They may not execute network actions, approve actions, change deterministic policy, or operate indefinitely.

---

## 33. Canonical Scenario A

Objective:

> **Investigate the DEGRADING_RADIO_QUALITY case for CELL-001 and recommend the next safe action.**

```text
User
 ↓
Chief Orchestration Agent
 ↓
Structured AgentPlan
 ├─ Context Agent
 ├─ Assurance Agent
 └─ Knowledge Agent
       ↓
Chief Orchestrator
       ↓
Decision Agent
       ↓
Candidate GENERATE_REMEDIATION_PLAN
       ↓
Phase 4 ProposedAction
       ↓
LOW / ALLOW
       ↓
Phase 4 MCP boundary
```

---

## 34. Canonical Scenario B

Agent proposes:

```text
SIMULATE_CELL_PARAMETER_CHANGE
```

Phase 4 must still produce:

```text
MEDIUM -> REQUIRE_APPROVAL
```

The Agent cannot approve or execute it.

---

## 35. Canonical Scenario C

Agent proposes:

```text
APPLY_CELL_PARAMETER_CHANGE
```

Phase 4 must still produce:

```text
HIGH -> DENY -> NO MCP INVOCATION
```

Agents gain no additional privilege.

---

## 36. Observability

Expose enough information for:

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

No new observability platform is required.

---

## 37. Zero Trust for Agents

Treat every Agent as a constrained principal.

Agent identity, allowed services, model profile, max calls, timeouts, and proposal privileges are explicit.

> **An Agent receives only the authority required for its role.**

No Agent inherits MCP Gateway or ActionExecutionService privileges.

---

## 38. Explicitly Out of Scope

Do not implement:

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

---

## 39. Locked Phase 5 Decisions

- Phase: Agentic Orchestration & Controlled Autonomy
- Autonomy: Level 1 / guided
- Orchestrator: one Chief Orchestration Agent
- Specialists: Knowledge, Context, Assurance, Decision
- Orchestration: synchronous and bounded
- Planning: structured `AgentPlan`
- Communication: orchestrator-mediated only
- Memory: ephemeral Run Memory + concise persisted Case Memory
- Hidden chain-of-thought persistence: prohibited
- Agent Registry: static/in-code
- Permissions: explicit least privilege
- Model: one shared underlying LLM initially
- Agent-specific prompts/settings: yes
- AgentModelResolver: yes
- Multi-model routing: deferred
- Control limits: mandatory/configurable
- Direct Agent MCP access: prohibited
- Action model: reuse Phase 4 `ProposedAction`
- Agent provenance: recorded
- Phase 4 governance: unchanged/authoritative
- Human approval: owned by Phase 4
- Agent audit: append-only
- Agent Factory: deferred
- RL: deferred
- Auto-remediation: prohibited

---

## 40. Architectural Outcome

At Phase 5 completion, SNIP transforms:

```text
User / Assurance Objective
```

into:

```text
Bounded AgentRun
+ Structured Plan
+ Specialist Evidence Gathering
+ Decision Synthesis
+ Governed ProposedAction
```

while preserving:

```text
Deterministic Assurance
+ Phase 4 Policy
+ Human Approval
+ MCP Governance
+ No Direct Agent Execution
```

Progression:

```text
KNOW -> UNDERSTAND -> OBSERVE CHANGE -> ASSESS -> ACT SAFELY -> COORDINATE INTELLIGENTLY
```
