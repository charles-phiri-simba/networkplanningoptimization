# SNIP Phase 4 — Governed Action Intelligence & MCP Implementation Specification

## 1. Authority and Baseline
This is the authorised Phase 4 implementation specification.

Start from:
```text
Branch: main
Commit: c692eb8d42711ed523460d2de34ffb0a607e7f17
Phase 3: ARCHITECTURALLY ACCEPTED
CI: PASS
Java tests: 60 tests, 0 failures
Go simulator tests/build: PASS
```

Read `NIP-PHASE-4-GOVERNED-ACTION-MCP-ARCHITECTURE.md` completely before coding.

Do not start Phase 5. Do not push a Phase 4 baseline until architectural review.

## 2. Objective
Prove all three governance outcomes:
```text
GENERATE_REMEDIATION_PLAN
 LOW -> ALLOW -> MCP -> remediation artifact

SIMULATE_CELL_PARAMETER_CHANGE
 MEDIUM -> REQUIRE_APPROVAL
 -> blocked before approval
 -> explicit approval
 -> MCP -> synthetic simulation result

APPLY_CELL_PARAMETER_CHANGE
 HIGH -> DENY -> no MCP invocation
```

Zero live-network writes.

## 3. Preserve Existing Capabilities
Do not rewrite or destabilize:
- Phase 1 Knowledge Intelligence/RAG
- structured network context
- Site/gNB/Cell domain
- PostgreSQL/Flyway foundation
- Kafka telemetry architecture
- Go simulator
- temporal/trend logic
- Phase 3 Assurance Case/evidence
- deterministic assurance severity/confidence
- Decision Intelligence
- citations/provenance
- local-AI path
- deterministic CI path
- accepted ADRs

## 4. Domain Model
Implement clear domain/application representations for:
```text
ProposedAction
PolicyDecision
ActionApproval
CapabilityDefinition
ActionResult
ActionAuditEvent
```
Keep governance logic out of controllers.

## 5. ProposedAction
Minimum:
```text
id
assuranceCaseId
actionType
capabilityId
targetType
targetId
parameters
rationale
riskLevel
policyDecision
status
proposedAt
proposedBy
synthetic
```
Creation must never execute the action.

## 6. Action Types and Risk
Implement:
```text
GENERATE_REMEDIATION_PLAN       -> LOW
SIMULATE_CELL_PARAMETER_CHANGE  -> MEDIUM
APPLY_CELL_PARAMETER_CHANGE     -> HIGH
```
Also define `CRITICAL` for future use. Mapping is deterministic and cannot be overridden by the LLM.

## 7. Policy
Implement:
```text
ALLOW
DENY
REQUIRE_APPROVAL
```

Create a deterministic `ActionPolicyEvaluator` with:
```text
GENERATE_REMEDIATION_PLAN       -> ALLOW
SIMULATE_CELL_PARAMETER_CHANGE  -> REQUIRE_APPROVAL
APPLY_CELL_PARAMETER_CHANGE     -> DENY
```

Persist policy ID, reason and evaluation time. No LLM policy authority.

## 8. Lifecycle
Support the bounded lifecycle:
```text
PROPOSED
POLICY_EVALUATED
APPROVAL_REQUIRED
APPROVED
REJECTED
DENIED
EXECUTING
SUCCEEDED
FAILED
```
Validate transitions. Do not build a generic workflow engine.

## 9. ActionProposalService
Create a service responsible for:
- validating the Assurance Case;
- creating the proposal;
- assigning deterministic action/risk semantics;
- persisting it;
- invoking deterministic policy evaluation;
- recording audit events.

Proposal and execution remain separate.

## 10. Approval
Persist:
```text
id
actionId
decision
decidedBy
decidedAt
comment
```
with `APPROVED` / `REJECTED`.

Simulation before approval must fail without MCP invocation. Rejected actions remain non-executable.

## 11. Identity
Persist explicit local/demo:
```text
proposedBy
decidedBy
executedBy
```
Use a service identity such as `SNIP_ACTION_SERVICE` for execution. Never represent the LLM as executor.

## 12. Persistence
Add the next Flyway migration after Phase 3.

Persist sufficient state for:
```text
proposed_action
policy_decision
action_approval
action_result
action_audit_event
```

Requirements:
- referential integrity
- useful indexes
- unique action IDs
- append-only audit
- no in-memory authoritative workflow state

## 13. Audit
Append immutable events for material transitions:
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
Never replace previous audit rows.

## 14. Capability Registry
Create an explicit registry with metadata:
```text
capabilityId
name
version
description
riskLevel
owner
enabled
requiresApproval
dryRunOnly
compensationSupported
```

Register only:
```text
remediation.generate.v1
simulation.cell-parameter.v1
```
No live network-write capability.

## 15. Java MCP
Implement the first local MCP server in Java using the Java/Spring ecosystem already present in SNIP.

Maintain a real client/gateway boundary. Do not introduce Python or Go MCP servers in Phase 4.

Remote MCP is out of scope.

## 16. MCP Gateway
Create a boundary conceptually named `McpCapabilityGateway`.

Before invocation verify:
- action exists
- capability registered/enabled
- action/capability compatible
- policy permits progression
- approval present when required
- risk constraints
- dry-run constraint
- action not already successfully executed

Fail closed.

## 17. No Direct LLM-to-MCP
Required:
```text
Decision Intelligence / LLM
 ↓
ProposedAction
 ↓
ActionPolicyEvaluator
 ↓
Approval if required
 ↓
ActionExecutionService
 ↓
McpCapabilityGateway
 ↓
Local Java MCP Server
```
No unrestricted MCP execution handle may be exposed to LLM components.

## 18. ActionExecutionService
Responsibilities:
- load action
- validate lifecycle/policy/approval
- resolve capability
- enforce idempotency
- append invocation-start audit
- invoke MCP gateway
- capture result
- update status
- append outcome audit

Keep separate from proposal/policy services.

## 19. remediation.generate.v1
Governance:
```text
Action: GENERATE_REMEDIATION_PLAN
Risk: LOW
Policy: ALLOW
Approval: none
Mutation: none
```

Use existing Assurance Case + Decision Assessment.

Return structured:
```text
assuranceCaseId
summary
recommendedChecks[]
suggestedNextSteps[]
evidenceReferences[]
warnings[]
synthetic
```

Never claim remediation was applied or root cause confirmed beyond Phase 3 evidence.

## 20. simulation.cell-parameter.v1
Governance:
```text
Action: SIMULATE_CELL_PARAMETER_CHANGE
Risk: MEDIUM
Policy: REQUIRE_APPROVAL
Approval: mandatory
Mutation: none
dryRun: true
```

Input:
```text
cellId
parameter
currentValue
proposedValue
dryRun=true
```

Result:
```text
cellId
parameter
currentValue
proposedValue
predictedImpact
warnings[]
constraints[]
synthetic=true
```

This is synthetic architecture validation, not a production RF Digital Twin. Reject `dryRun=false` before MCP invocation.

## 21. APPLY_CELL_PARAMETER_CHANGE
Implement only as a denied action type:
```text
Risk = HIGH
Policy = DENY
Status = DENIED
```
Requirements:
```text
NO MCP invocation
NO network call
NO simulated substitute pretending to be an apply
```

## 22. Idempotency
Use `actionId` as execution idempotency key.

After successful execution, repeated execution:
- does not reinvoke MCP;
- returns/retrieves prior result;
- preserves auditability.

Test this explicitly.

## 23. Timeout and Failure
Use bounded MCP timeout. No uncontrolled automatic action retries.

Failure:
```text
Action -> FAILED
ActionResult -> FAILED
Audit -> MCP_INVOCATION_FAILED
```

## 24. Compensation
Initial capabilities use:
```text
compensationSupported=false
```
No network rollback.

## 25. APIs
Add conceptually equivalent APIs:
```text
POST /api/v1/assurance/cases/{caseId}/actions
GET  /api/v1/actions
GET  /api/v1/actions/{actionId}

POST /api/v1/actions/{actionId}/approve
POST /api/v1/actions/{actionId}/reject
POST /api/v1/actions/{actionId}/execute
```

These mutate SNIP governance state only.

Execution rules:
```text
ALLOW -> capability may execute
REQUIRE_APPROVAL -> only after APPROVED
DENY -> never execute
```

## 26. Read Model
Action detail must expose:
- action
- risk
- policy
- lifecycle
- approval
- result
- audit events

Do not expose transport credentials/secrets.

## 27. Canonical Path A Test
From a valid Phase 3 Assurance Case:
```text
GENERATE_REMEDIATION_PLAN
 -> LOW
 -> ALLOW
 -> execute
 -> remediation.generate.v1
 -> SUCCEEDED
```
Verify exactly one MCP invocation, artifact returned, no approval, no network mutation, complete audit.

## 28. Canonical Path B Test
```text
SIMULATE_CELL_PARAMETER_CHANGE
 -> MEDIUM
 -> REQUIRE_APPROVAL
```
Execution before approval:
```text
blocked
no MCP invocation
```
Then approve and execute:
```text
APPROVED
 -> simulation.cell-parameter.v1
 -> synthetic result
 -> SUCCEEDED
```
Verify `dryRun=true`.

## 29. Canonical Path C Test
```text
APPLY_CELL_PARAMETER_CHANGE
 -> HIGH
 -> DENY
 -> DENIED
 -> no MCP invocation
```
Mandatory acceptance test.

## 30. Rejection Test
Simulation:
```text
REQUIRE_APPROVAL -> REJECTED
```
Subsequent execution fails with no MCP invocation.

## 31. Registry Negative Tests
Reject:
- unknown capability
- disabled capability
- incompatible action/capability
- dry-run violation
- missing approval

## 32. Persistence Tests
Use PostgreSQL/Testcontainers. Verify Flyway, actions, policy, approval, results, append-only audit, integrity, and reload/query behaviour.

## 33. MCP Integration Tests
Prove the Java MCP client/server boundary works without public/remote MCP.

Where feasible, exercise the actual local MCP protocol boundary rather than replacing every interaction with an in-process call.

CI must not require Ollama for MCP governance tests.

## 34. Regression and CI
Preserve all Phase 1–3 tests; do not weaken the 60-test Phase 3 baseline.

Ensure required MCP integration tests actually execute in CI. Address the known Surefire `*IT.java` exclusion only if needed for these required tests and without unrelated build refactoring.

Run:
```text
mvn -B test
go test ./...
go build ./cmd/simulator
```

## 35. Local AI
Phase 4 may consume the existing Phase 3 `DecisionAssessment`, but governance success cannot depend on non-deterministic LLM wording.

Ollama remains optional for local E2E, not default CI.

## 36. Observability
Expose/log enough to inspect:
```text
actionsProposed
policyAllow
policyRequireApproval
policyDeny
actionsApproved
actionsRejected
mcpInvocations
mcpInvocationFailures
mcpInvocationLatencyMs
idempotentExecutionHits
```
Correlate by `actionId` and `assuranceCaseId`.

## 37. Security Validation
Verify:
- no live network endpoint
- no vendor credentials
- no unrestricted MCP path
- DENY cannot reach MCP
- approval-required action cannot reach MCP before approval
- `dryRun=false` cannot reach simulation MCP
- LLM cannot override risk/policy
- executor identity explicit
- audit append-only

## 38. Docker / Local Runtime
Reuse existing local topology where practical. Add only what is needed for the local Java MCP server.

Do not introduce Kubernetes, EKS, service mesh, or remote MCP infrastructure.

## 39. ADRs
Create sequential ADRs after ADR 019 for:
1. ProposedAction and lifecycle
2. deterministic policy enforcement
3. human approval boundary
4. MCP Gateway and explicit capability registry
5. local Java MCP / future polyglot MCP
6. append-only action audit and idempotency
7. prohibition of live network writes in Phase 4

## 40. Documentation
Update implementation context/status and README with:
- Phase 4 purpose
- action types/risk
- policy outcomes
- approval flow
- local Java MCP server
- registered capabilities
- ALLOW / REQUIRE_APPROVAL / DENY examples
- zero-live-write boundary

## 41. Explicitly Out of Scope
Do NOT implement:
- live network configuration writes
- Ericsson ENM writes
- Nokia NetAct writes
- OSS/NMS/EMS write integration
- auto-remediation
- autonomous Agent runtime
- Agent Factory
- reinforcement learning
- production IAM/OIDC
- remote third-party MCP
- vendor-specific MCP adapters
- production RF simulation
- full Digital Twin execution
- network rollback
- Phase 5 functionality

## 42. Acceptance Criteria
### Baseline
- [ ] Starts from `c692eb8d42711ed523460d2de34ffb0a607e7f17`.
- [ ] Phase 1–3 regressions pass.
- [ ] Phase 3 responsibilities preserved.
- [ ] No live network write path.

### Domain/Persistence
- [ ] ProposedAction, PolicyDecision, ActionApproval, ActionResult, ActionAuditEvent implemented.
- [ ] PostgreSQL/Flyway persistence.
- [ ] Audit append-only.
- [ ] Idempotency preserved.

### Policy
- [ ] Remediation -> LOW -> ALLOW.
- [ ] Simulation -> MEDIUM -> REQUIRE_APPROVAL.
- [ ] Apply -> HIGH -> DENY.
- [ ] Policy deterministic.
- [ ] LLM cannot override policy.
- [ ] DENY never reaches MCP.

### Approval
- [ ] Simulation blocked before approval.
- [ ] Approval permits simulation.
- [ ] Rejection prevents execution.
- [ ] Actor/time persisted.
- [ ] Invalid transitions fail.

### MCP
- [ ] Local Java MCP server.
- [ ] Java/Spring MCP gateway/client.
- [ ] Capability registry.
- [ ] `remediation.generate.v1`.
- [ ] `simulation.cell-parameter.v1`.
- [ ] No live apply capability.
- [ ] Unknown/disabled capability fails closed.
- [ ] Timeout/failure captured.
- [ ] No direct LLM-to-MCP path.

### Safety
- [ ] Simulation requires `dryRun=true`.
- [ ] Simulation output labelled synthetic.
- [ ] No production RF accuracy claim.
- [ ] No network mutation.
- [ ] Re-execution does not reinvoke successful action.
- [ ] Executor identity explicit.
- [ ] Full audit available.

### Canonical Proof
- [ ] ALLOW -> MCP -> success.
- [ ] REQUIRE_APPROVAL -> blocked -> approve -> MCP -> success.
- [ ] DENY -> no MCP invocation.

### CI/Docs
- [ ] Maven tests pass.
- [ ] Go tests/build pass.
- [ ] MCP integration tests execute.
- [ ] CI does not require Ollama.
- [ ] README/docs updated.
- [ ] ADRs created.
- [ ] No secrets/generated binaries committed.
- [ ] Phase 5 not started.

## 43. Local End-to-End Validation
Begin with a persisted Phase 3 `DEGRADING_RADIO_QUALITY` Assurance Case for `CELL-001`.

Demonstrate and record action IDs, policy decisions, approvals, capability IDs, results, audit events and proof of no network mutation for all three paths.

## 44. Completion Report
Create:
```text
docs/implementation/SNIP-PHASE-4-COMPLETION-REPORT.md
```

Include:
1. Executive Summary
2. Phase 3 Baseline Verification
3. Scope Delivered
4. ProposedAction Domain
5. Risk Classification
6. Deterministic Policy Model
7. Action Lifecycle
8. Human Approval
9. Capability Registry
10. Java MCP Architecture
11. MCP Gateway
12. remediation.generate.v1
13. simulation.cell-parameter.v1
14. Denied APPLY_CELL_PARAMETER_CHANGE
15. Persistence/Flyway
16. Append-only Audit
17. Idempotency
18. Timeout/Failure
19. APIs
20. Tests
21. MCP Integration Evidence
22. Path A — ALLOW
23. Path B — REQUIRE_APPROVAL
24. Path C — DENY
25. Local E2E Evidence
26. Observability
27. Security/Zero-Live-Write Review
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
PHASE 4 STATUS: ACCEPTANCE RECOMMENDED
```
or:
```text
PHASE 4 STATUS: ACCEPTANCE NOT RECOMMENDED
```

Cursor must not mark Phase 4 architecturally accepted.

## 45. Final Instruction to Cursor
Treat this as the authorised scope for **Phase 4 only**.

> **Move SNIP from recommendation to safe, governed enterprise capability invocation through MCP, proving deterministic policy enforcement, explicit human approval, registered capabilities, idempotent execution, append-only audit, and denial of unsafe action — without any live network write.**

Do not broaden the phase. Do not start Phase 5.

Do not push or establish a new Git baseline until the completion report has been reviewed and explicit architectural acceptance has been granted.

When implementation and validation are complete, produce the completion report and STOP.
