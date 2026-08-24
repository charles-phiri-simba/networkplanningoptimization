# SNIP Phase 4 — Governed Action Intelligence & MCP Architecture

## 1. Purpose
Phase 4 introduces Governed Action Intelligence and Model Context Protocol (MCP) to SNIP.

Baseline entering Phase 4:
- Phase 1A/1A.1 — Knowledge Intelligence + Semantic RAG
- Phase 1B — Structured Network Context Intelligence
- Phase 2 — Event, Telemetry & Temporal Intelligence
- Phase 3 — Assurance & Decision Intelligence

Phase 4 answers:

> **How does SNIP move from recommending an action to safely invoking a governed enterprise capability through MCP while preserving deterministic policy enforcement, human control, auditability, idempotency, and zero live-network risk?**

Phase 4 is not autonomous network control.

## 2. Architectural Transition
```text
Assurance Case
      ↓
Decision Intelligence
      ↓
Proposed Action
      ↓
Risk Classification
      ↓
Policy Evaluation
      ↓
Approval Boundary
      ↓
MCP Gateway
      ↓
Registered Capability
      ↓
Action Result
      ↓
Append-only Audit
```

## 3. Governing Principle
> **AI may propose. Deterministic policy decides. Humans approve where required. MCP invokes only explicitly registered capabilities.**

```text
Reasoning != Proposal != Policy != Approval != Invocation != Result
```

Direct `LLM -> MCP -> Network` execution is prohibited.

## 4. ProposedAction
Primary Phase 4 domain object:

```text
ProposedAction
 + id
 + assuranceCaseId
 + actionType
 + capabilityId
 + targetType
 + targetId
 + parameters
 + rationale
 + riskLevel
 + policyDecision
 + status
 + proposedAt
 + proposedBy
 + synthetic
```

A proposal represents intent only; creation never implies execution.

## 5. Canonical Actions
```text
GENERATE_REMEDIATION_PLAN
  Risk: LOW
  Policy: ALLOW
  Capability: remediation.generate.v1
  Executable: yes

SIMULATE_CELL_PARAMETER_CHANGE
  Risk: MEDIUM
  Policy: REQUIRE_APPROVAL
  Capability: simulation.cell-parameter.v1
  Executable: only after explicit approval
  Synthetic/dry-run only

APPLY_CELL_PARAMETER_CHANGE
  Risk: HIGH
  Policy: DENY
  Executable: no
  MCP invocation: prohibited
```

No live network mutation capability is registered.

## 6. Risk Model
Use:
```text
LOW
MEDIUM
HIGH
CRITICAL
```
Risk is deterministic, not an LLM opinion. `CRITICAL` is reserved for future action classes.

## 7. Policy Model
Use:
```text
ALLOW
DENY
REQUIRE_APPROVAL
```

Conceptually:
```text
PolicyDecision
 + actionId
 + decision
 + policyId
 + reason
 + evaluatedAt
```

Initial deterministic rules:
```text
GENERATE_REMEDIATION_PLAN      -> ALLOW
SIMULATE_CELL_PARAMETER_CHANGE -> REQUIRE_APPROVAL
APPLY_CELL_PARAMETER_CHANGE    -> DENY
```

## 8. Governance AI Boundary
Governance AI may explain policies, references, conflicts, and approval requirements. It may not authorize, override deterministic policy, manufacture approval, bypass MCP governance, or execute actions.

No Governance AI Agent runtime is required in Phase 4.

## 9. Lifecycle
```text
PROPOSED
   ↓
POLICY_EVALUATED
   ├─> DENIED
   └─> APPROVAL_REQUIRED
          ├─> REJECTED
          └─> APPROVED
                 ↓
             EXECUTING
              /     \
             v       v
        SUCCEEDED   FAILED
```

Allowed actions that require no approval may proceed from policy evaluation to execution.

## 10. Human Approval
Conceptually:
```text
ActionApproval
 + id
 + actionId
 + decision
 + decidedBy
 + decidedAt
 + comment
```
Decisions: `APPROVED`, `REJECTED`.

Approval-required actions fail closed before explicit approval.

## 11. Identity
Record:
```text
requestedBy
approvedBy
executedBy
```
Use an explicit service executor such as `SNIP_ACTION_SERVICE`. Never represent the LLM as the privileged executor.

## 12. Capability Registry
Conceptually:
```text
CapabilityDefinition
 + capabilityId
 + name
 + version
 + description
 + riskLevel
 + owner
 + enabled
 + requiresApproval
 + dryRunOnly
 + compensationSupported
```

Initial registered capabilities:
```text
remediation.generate.v1
simulation.cell-parameter.v1
```

Only registered and enabled capabilities can execute.

## 13. MCP Topology
```text
SNIP Spring Application
        ↓
ActionExecutionService
        ↓
MCP Gateway / Client Boundary
        ↓
Local Java MCP Server
        ├─ remediation.generate.v1
        └─ simulation.cell-parameter.v1
```

The first MCP server is Java. The long-term architecture remains polyglot:
- Java — enterprise/application capabilities
- Python — AI/ML/optimisation/advanced simulation
- Go — telemetry/runtime/platform capabilities

Remote MCP servers are deferred.

## 14. MCP Gateway Responsibilities
The gateway must verify:
- action identity
- capability registration and enabled state
- action/capability compatibility
- deterministic policy decision
- approval where required
- risk constraints
- dry-run constraints
- idempotency
- bounded timeout
- result capture
- audit correlation

It fails closed.

## 15. No Direct LLM-to-MCP Invocation
Required:
```text
Decision Intelligence / LLM
        ↓
ProposedAction
        ↓
Policy
        ↓
Approval when required
        ↓
ActionExecutionService
        ↓
MCP Gateway
        ↓
Local MCP Server
```

## 16. Capability — remediation.generate.v1
Input is based on an existing Assurance Case and Decision Assessment.

Conceptual output:
```text
RemediationPlan
 + actionId
 + assuranceCaseId
 + summary
 + recommendedChecks[]
 + suggestedNextSteps[]
 + evidenceReferences[]
 + warnings[]
 + synthetic
```

It cannot modify network state or claim remediation was applied.

## 17. Capability — simulation.cell-parameter.v1
Conceptual input:
```text
cellId
parameter
currentValue
proposedValue
dryRun=true
```

Conceptual output:
```text
SimulationResult
 + actionId
 + cellId
 + parameter
 + currentValue
 + proposedValue
 + predictedImpact
 + warnings[]
 + constraints[]
 + synthetic=true
```

This is an architectural synthetic simulation, not a production RF Digital Twin or vendor-certified predictor. `dryRun=false` is rejected.

## 18. Denied Action
`APPLY_CELL_PARAMETER_CHANGE` exists to prove governance can stop unsafe action:
```text
ProposedAction
  ↓
HIGH
  ↓
DENY
  ↓
DENIED
  ↓
Audit
  ↓
STOP — no MCP invocation
```

## 19. ActionResult
```text
ActionResult
 + actionId
 + capabilityId
 + status
 + startedAt
 + completedAt
 + output
 + error
 + synthetic
```
Minimum result states: `SUCCEEDED`, `FAILED`, `REJECTED`.

## 20. Append-only Audit
```text
ActionAuditEvent
 + id
 + actionId
 + eventType
 + actor
 + timestamp
 + details
```

Events include:
```text
ACTION_PROPOSED
POLICY_EVALUATED
APPROVAL_REQUESTED
ACTION_APPROVED
ACTION_REJECTED
ACTION_DENIED
MCP_INVOCATION_STARTED
MCP_INVOCATION_SUCCEEDED
MCP_INVOCATION_FAILED
```

Audit is append-only from inception.

## 21. Idempotency
`actionId` is the execution idempotency key. A successfully executed action cannot be executed again due to HTTP retry, workflow retry, or duplicate request; the prior result is returned instead.

## 22. Failure and Timeout
MCP invocation has bounded timeout. No uncontrolled action retries.

Failure produces `FAILED`, a safe captured error, and an append-only failure audit event.

## 23. Compensation
No rollback is required because live mutable actions are prohibited. Initial capabilities declare `compensationSupported=false`.

Future mutable capabilities must define compensation semantics before authorization.

## 24. APIs
Conceptual workflow APIs:
```text
POST /api/v1/assurance/cases/{caseId}/actions
GET  /api/v1/actions
GET  /api/v1/actions/{actionId}

POST /api/v1/actions/{actionId}/approve
POST /api/v1/actions/{actionId}/reject
POST /api/v1/actions/{actionId}/execute
```

These mutate SNIP governance/workflow state, not network state.

Execution requires:
```text
policy = ALLOW
OR
(policy = REQUIRE_APPROVAL AND approval = APPROVED)
```

## 25. Persistence
Use PostgreSQL for governance state, conceptually:
```text
proposed_action
policy_decision
action_approval
action_result
action_audit_event
```
Audit history is append-only.

## 26. Zero Trust Boundary
Every capability invocation is privileged, even locally. Preserve:
```text
Caller identity
Action identity
Policy identity
Approval identity
Executor identity
Capability identity
```
Production OIDC/service identity is deferred.

## 27. Canonical Path A — ALLOW
```text
CELL-001
 ↓
DEGRADING_RADIO_QUALITY
 ↓
Decision Assessment
 ↓
GENERATE_REMEDIATION_PLAN
 ↓
LOW
 ↓
ALLOW
 ↓
MCP Gateway
 ↓
remediation.generate.v1
 ↓
Remediation Artifact
 ↓
SUCCEEDED
 ↓
Audit
```

## 28. Canonical Path B — REQUIRE_APPROVAL
```text
CELL-001
 ↓
SIMULATE_CELL_PARAMETER_CHANGE
 ↓
MEDIUM
 ↓
REQUIRE_APPROVAL
 ↓
execution blocked
 ↓
Human Approval
 ↓
MCP Gateway
 ↓
simulation.cell-parameter.v1
 ↓
Synthetic Result
 ↓
SUCCEEDED
 ↓
Audit
```

## 29. Canonical Path C — DENY
```text
CELL-001
 ↓
APPLY_CELL_PARAMETER_CHANGE
 ↓
HIGH
 ↓
DENY
 ↓
DENIED
 ↓
Audit
 ↓
NO MCP INVOCATION
```

Phase 4 succeeds only if all three governance outcomes are proven.

## 30. Relationship to Phase 3
Phase 3 remains authoritative for `AssuranceCase`, evidence, severity, confidence, and `DecisionAssessment`. Phase 4 consumes these outputs and does not rewrite assurance logic.

## 31. Relationship to Future Agents
Future:
```text
Agent
 ↓
ProposedAction
 ↓
Policy
 ↓
Approval
 ↓
MCP
```
Never `Agent -> unrestricted tool/network execution`.

## 32. Explicitly Out of Scope
- Live network configuration writes
- Ericsson ENM writes
- Nokia NetAct writes
- OSS/NMS/EMS write integration
- Auto-remediation
- Autonomous Agent runtime
- Agent Factory
- Reinforcement Learning
- Production IAM/OIDC
- Remote third-party MCP servers
- Vendor-specific MCP adapters
- Production RF simulation
- Full Digital Twin execution
- Automatic network rollback
- Phase 5 functionality

## 33. Locked Decisions
- Core object: `ProposedAction`
- Policy: `ALLOW / DENY / REQUIRE_APPROVAL`
- Risk: `LOW / MEDIUM / HIGH / CRITICAL`
- Policy enforcement: deterministic
- Human approval: required for parameter simulation
- MCP gateway/client: Java/Spring
- First MCP server: local Java
- Long-term MCP ecosystem: polyglot Java/Python/Go
- Capability registry: required
- `remediation.generate.v1`: executable
- `simulation.cell-parameter.v1`: executable only after approval
- `APPLY_CELL_PARAMETER_CHANGE`: known but always denied
- Live write capability: absent
- Audit: append-only
- Idempotency: `actionId`
- Simulation: mandatory dry-run/synthetic
- Direct LLM-to-MCP: prohibited
- Remote MCP: deferred
- Autonomous Agents: deferred
- Network writes: prohibited

## 34. Architectural Outcome
At Phase 4 completion, SNIP transforms:
```text
Assurance Case + Decision Assessment
```
into:
```text
Proposed Action
+ Risk Classification
+ Deterministic Policy Decision
+ Human Approval when required
+ Registered MCP Capability Invocation
+ Action Result
+ Append-only Audit
```

while proving unsafe actions are denied before invocation.

```text
KNOW → UNDERSTAND → OBSERVE CHANGE → ASSESS → ACT SAFELY
```
