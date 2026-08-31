# SNIP Phase 15 — Governed Network Change Execution, Verification & Recovery Specification

## Status

**Architecture:** ACCEPTED AND FROZEN  
**Implementation specification:** COMPLETE — AUTHORIZED FOR IMPLEMENTATION  
**Implementation baseline:** NOT ESTABLISHED  
**Phase 16:** NOT STARTED

**Phase 14 immutable parent:** `043c5ad98b8a12fb8073ba40364a2e287d2cc65a`  
**Phase 15 architecture baseline:** `327ebb15eb2ddad477796410cb2403890cd7e299`  
**Frozen architecture SHA-256:** `a76c39589df990e7d90663b6fafbb7adec67ad5f932a13ac5af677175b2d482e`  
**Architecture CI:** workflow `ci`, run #19, run ID `33362417722`, exact-SHA success.

Cursor MUST start from exact HEAD `327ebb15eb2ddad477796410cb2403890cd7e299` with a clean tree. If HEAD differs, STOP.

## 1. Defining principle

Phase 15 introduces SNIP’s first governed mutation capability, but only against explicitly permitted non-production targets. It must execute the exact authorized Phase 14 `READY_FOR_EXECUTION` plan, independently verify the resulting target state, represent ambiguous outcomes truthfully, and govern recovery/rollback. Real Ericsson/Nokia production writes remain prohibited.

## 2. Package boundary

All Phase 15 production code belongs under:

`com.simba.snip.npo.changeexecution`

Recommended packages: `api`, `config`, `domain`, `entity`, `repository`, `service`, `adapter.spi`, `adapter.simulator`, `security`, `audit`, `metrics`, `exception`.

Do not put Phase 15 execution logic in `changeplanning`, `integration`, `agent`, or `mcp`.

## 3. Migration

Create exactly:

`V16__phase15_governed_change_execution.sql`

V1–V15 remain unchanged.

Required tables:

- `network_change_execution`
- `network_change_execution_operation`
- `network_change_execution_attempt`
- `network_change_execution_authorization`
- `network_change_execution_verification`
- `network_change_execution_recovery`
- `network_change_execution_rollback`
- `network_change_execution_audit_event`

No secrets, tokens, private keys, vendor passwords, connection strings or raw vendor payloads.

## 4. Aggregate

Implement a distinct `NetworkChangeExecution`. `NetworkChangePlan != NetworkChangeExecution`.

Persist at minimum: execution ID, plan ID/version/fingerprint, target ID/type/environment, adapter profile ID, capability profile version, execution fingerprint, authorized execution fingerprint, status, request/review/authorization actors and timestamps, lease/fencing references, safe failure information, verification/recovery/rollback state, audit timestamps and optimistic version.

Initial constraints:

- one ACTIVE execution per Phase 14 plan;
- one ACTIVE execution per `executionTargetId + cellId + parameter`.

Terminal history may remain, but uncontrolled repeated execution is forbidden.

## 5. Supported scope

Only:

```text
SET_PARAMETER
CELL
txPower
maximum operations = 1
maximum forward mutation attempts = 1
```

No bulk, multi-cell, multi-parameter, CLI, SSH, scripts, neighbor/tilt/frequency/carrier/lock operations, or vendor commands.

## 6. Target model

Implement `ExecutionTarget`, `ExecutionTargetDescriptor`, `ExecutionTargetRegistry`, `ExecutionTargetType`, `ExecutionTargetCapability`.

Allowed target types:

- `SIMULATOR`
- `CONTROLLED_SANDBOX`

`CONTROLLED_SANDBOX` is explicit opt-in only and requires known `NON_PRODUCTION` classification, allow-list membership, no production route, no production vendor account, no production inventory binding, and synthetic/test credentials if credentials are needed. Unknown classification fails closed.

Do not implement a production target capable of mutation.

## 7. Structural production-write prohibition

Phase 15 production code must not depend on `EnmTransport`, real Ericsson/Nokia connectors, `CredentialHandle`, Azure Key Vault execution credential resolution, or vendor write credentials.

Do not create an operative `real-vendor-execution-enabled` switch. Structural absence of a production write adapter is the protection.

## 8. Adapter SPI

Implement a write adapter SPI for `execute()` and `rollback()`, and a logically independent observation SPI for readback/verification. The same simulator component may implement both, but mutation response MUST NOT count as verification.

The existing Go Kafka simulator and Phase 11 `SimulatorEnmTransport` are not Phase 15 execution adapters.

## 9. Simulator target

Implement a distinct `SimulatorExecutionAdapter` and simulator execution state store. Simulator target state must be separate from canonical SNIP state. Do not implement simulator execution by directly updating `radio_configuration`.

## 10. Request contract

Creation request contains only:

```json
{
  "planId": "...",
  "executionTargetId": "snip-simulator"
}
```

Caller cannot supply cell, parameter, expected/desired/rollback values, operation type, fingerprint, endpoint, credentials, protocol, vendor command or verification result.

## 11. Lifecycle

Implement at minimum:

```text
REQUESTED
PRELIMINARY_ADMISSION_CHECKING
PRELIMINARY_ADMISSION_REJECTED
READY_FOR_REVIEW
REVIEWED
READY_FOR_EXECUTION_AUTHORIZATION
AUTHORIZED
FINAL_PREFLIGHT_CHECKING
EXECUTING
APPLIED
EXECUTION_OUTCOME_UNKNOWN
VERIFYING
VERIFIED
EXECUTION_FAILED
VERIFICATION_FAILED
RECOVERY_REQUIRED
ROLLBACK_REQUESTED
ROLLBACK_REVIEWED
ROLLBACK_AUTHORIZED
ROLLING_BACK
ROLLBACK_APPLIED
ROLLBACK_OUTCOME_UNKNOWN
ROLLED_BACK
ROLLBACK_FAILED
MANUAL_INTERVENTION_REQUIRED
CANCELLED_BEFORE_MUTATION
```

Normative forward order:

`REQUESTED → preliminary admission → READY_FOR_REVIEW → REVIEWED → READY_FOR_EXECUTION_AUTHORIZATION → AUTHORIZED → acquire lease/fencing → FINAL_PREFLIGHT_CHECKING → EXECUTING → APPLIED/EXECUTION_OUTCOME_UNKNOWN → VERIFYING → VERIFIED`.

## 12. Preliminary admission

Validate plan existence/readiness/validity, proposal validity, Phase 14 plan fingerprint, Phase 14 authorization, target configuration/non-production classification, target capability profile, supported scope, rollback availability and execution-window structure.

Preliminary admission must not imply mutable network state remains valid later.

## 13. Final preflight

After human Phase 15 authorization, acquire execution authority first. Under lease/fencing re-check:

- plan still ready/valid;
- Phase 14 plan and authorization fingerprints current;
- Phase 15 authorization fingerprint current;
- target binding/capability profile current;
- execution window currently open;
- target exists;
- actual current value equals expected;
- knowledge confidence acceptable;
- synchronization trustworthy;
- no relevant unresolved drift;
- rollback still valid;
- dependencies valid;
- safety still passes;
- fencing token current.

Unknown mandatory evidence denies mutation.

## 14. Expected-state guard

Immediately before mutation, observe actual target state. If `actual != expected`, or actual is unknown/stale/unavailable, perform zero mutation.

Use target CAS/revision semantics when available. Otherwise acknowledge residual read/write race and rely on final preflight plus independent verification; do not claim atomic external CAS.

## 15. Review and authorization

Review is mandatory and persisted before authorization.

Permissions:

```text
VIEW_NETWORK_CHANGE_EXECUTION
REQUEST_NETWORK_CHANGE_EXECUTION
REVIEW_NETWORK_CHANGE_EXECUTION
AUTHORIZE_NETWORK_CHANGE_EXECUTION
CANCEL_NETWORK_CHANGE_EXECUTION
VIEW_NETWORK_CHANGE_EXECUTION_EVIDENCE
REQUEST_NETWORK_CHANGE_ROLLBACK
REVIEW_NETWORK_CHANGE_ROLLBACK
AUTHORIZE_NETWORK_CHANGE_ROLLBACK
```

Phase 14 authorization does not imply Phase 15 authorization. Agents cannot authorize. The model must support separation between Phase 14 plan authorizer and Phase 15 execution authorizer.

## 16. Execution fingerprint

Use deterministic canonical UTF-8 SHA-256.

Include plan fingerprint/version, target ID/type/environment, adapter profile, capability profile version, ordered operation binding, rollback binding, execution window and stable execution-policy fields.

Exclude execution ID, actors, timestamps, audit history, lease/fencing values, dynamic observations and verification outcomes.

Persist `executionFingerprint` and `authorizedExecutionFingerprint`. Target/profile/window substitution invalidates authorization.

## 17. Idempotency and attempts

Separate HTTP request idempotency, execution-attempt identity and target-side mutation semantics. Do not claim exactly-once external mutation.

Maximum forward mutation attempts = 1. Ambiguous outcome does not trigger automatic retry. A retry requires a new separately governed authorization path.

## 18. Ambiguous forward outcome

Persist `EXECUTION_OUTCOME_UNKNOWN`.

Then observe independently:

- target == desired → `VERIFIED`;
- target == original expected → no auto-retry; new authorized execution required;
- target == third value → `MANUAL_INTERVENTION_REQUIRED`;
- target unavailable/unknown → remain safe-stopped/recovery-required.

## 19. APPLIED != VERIFIED

Adapter success only permits `APPLIED`. Only fresh independent target observation permits `VERIFIED`.

Verification outcomes:

```text
VERIFIED
MISMATCH
UNKNOWN
TIMEOUT
SOURCE_UNAVAILABLE
STALE_OBSERVATION
```

Observation must prove it is post-mutation using timestamp or target revision/version.

## 20. Canonical-state isolation

Execution mutates simulator/sandbox target state only. It must not directly update canonical `radio_configuration` or other Phase 12 projections. Verification reads execution target state, not canonical state.

After verified execution, Phase 15 may record or emit `synchronization-needed`; it must not mark Phase 12 synchronization successful.

## 21. Recovery

`VERIFICATION_FAILED → RECOVERY_REQUIRED`.

A deterministic recovery evaluator may decide rollback eligibility or manual intervention. LLMs have no authority. Automatic rollback is prohibited.

## 22. Rollback governance

Rollback values come only from Phase 14 persisted rollback intent.

Required path:

`RECOVERY_REQUIRED → rollback request → rollback review → rollback authorization → rollback expected-state guard → rollback mutation → independent verification`.

Rollback authorization has its own deterministic fingerprint bound to execution, plan fingerprint, exact target, rollback operation, rollback expected/desired state, adapter profile and capability profile.

Unknown/stale/mismatching current state means zero rollback mutation.

`ROLLBACK_APPLIED != ROLLED_BACK`.

`ROLLBACK_OUTCOME_UNKNOWN` is first-class; no blind retry.

`MANUAL_INTERVENTION_REQUIRED` is a terminal safe-stop and must not expose hidden vendor commands/credentials/scripts.

## 23. Lease/fencing

Use a dedicated execution namespace, e.g. `change-execution:{targetId}:{cellId}:{parameter}`.

Acquire lease before final mutable-state checks. A stale holder may not begin mutation, persist APPLIED/VERIFIED, execute rollback, or overwrite newer evidence. Do not claim fencing cancels an already-sent external write.

## 24. Execution window and cancellation

Final preflight checks the window. Authorization before expiry does not permit mutation after expiry. No scheduler.

Before mutation, cancellation may yield `CANCELLED_BEFORE_MUTATION`. Once mutation may have been sent, do not falsely mark cancelled; determine outcome instead.

## 25. Durable failure evidence

These states must survive outer HTTP failures:

```text
EXECUTION_OUTCOME_UNKNOWN
VERIFICATION_FAILED
RECOVERY_REQUIRED
MANUAL_INTERVENTION_REQUIRED
ROLLBACK_FAILED
ROLLBACK_OUTCOME_UNKNOWN
```

Use a dedicated Spring-managed independent persistence component with `REQUIRES_NEW` where needed, invoked through a Spring proxy.

## 26. API

Namespace: `/api/v1/change-execution/executions`

Required endpoints:

```text
POST /executions
GET  /executions
GET  /executions/{executionId}
GET  /executions/{executionId}/evidence
POST /executions/{executionId}/review
POST /executions/{executionId}/authorize
POST /executions/{executionId}/execute
POST /executions/{executionId}/verify
POST /executions/{executionId}/cancel
POST /executions/{executionId}/rollback/request
POST /executions/{executionId}/rollback/review
POST /executions/{executionId}/rollback/authorize
POST /executions/{executionId}/rollback/execute
```

`verify` is readback-only and never mutates target state.

## 27. Stable reason codes

Implement at least:

```text
EXECUTION_PLAN_NOT_READY
EXECUTION_PLAN_EXPIRED
EXECUTION_PLAN_INVALIDATED
EXECUTION_PLAN_CANCELLED
EXECUTION_PLAN_SUPERSEDED
EXECUTION_AUTHORIZATION_MISSING
EXECUTION_AUTHORIZATION_STALE
EXECUTION_TARGET_NOT_ALLOWED
EXECUTION_TARGET_NOT_FOUND
EXECUTION_TARGET_CAPABILITY_MISSING
EXECUTION_TARGET_ENVIRONMENT_PROHIBITED
EXECUTION_CURRENT_VALUE_MISMATCH
EXECUTION_KNOWLEDGE_LOW
EXECUTION_KNOWLEDGE_UNKNOWN
EXECUTION_SYNCHRONIZATION_STALE
EXECUTION_RELEVANT_DRIFT_PRESENT
EXECUTION_LEASE_UNAVAILABLE
EXECUTION_FENCING_TOKEN_STALE
EXECUTION_CONFLICT
EXECUTION_OPERATION_REJECTED
EXECUTION_OPERATION_TIMEOUT
EXECUTION_OUTCOME_UNKNOWN
EXECUTION_VERIFICATION_MISMATCH
EXECUTION_VERIFICATION_TIMEOUT
EXECUTION_VERIFICATION_UNKNOWN
ROLLBACK_AUTHORIZATION_MISSING
ROLLBACK_AUTHORIZATION_STALE
ROLLBACK_CURRENT_VALUE_MISMATCH
ROLLBACK_OPERATION_FAILED
ROLLBACK_OUTCOME_UNKNOWN
ROLLBACK_VERIFICATION_FAILED
MANUAL_INTERVENTION_REQUIRED
```

## 28. Audit and metrics

Audit must distinguish request/admission/review/authorization/preflight/mutation/ambiguous outcome/verification/recovery/rollback/manual intervention/cancellation/completion. No secrets or raw vendor credentials.

Metrics must be low cardinality. Never label by cellId, planId, executionId, userId, fingerprint or endpoint.

## 29. Configuration

Fail closed:

```yaml
snip:
  change-execution:
    enabled: false
    maximum-operation-count: 1
    maximum-forward-attempts: 1
    permitted-target-types:
      - SIMULATOR
    require-execution-review: true
    require-execution-authorization: true
    require-current-value-match: true
    require-verification: true
    require-rollback-review: true
    require-rollback-authorization: true
    automatic-rollback-enabled: false
```

CONTROLLED_SANDBOX is explicit opt-in. No operative production-write flag.

## 30. Agent/MCP/event boundary

Agents may inspect/explain only. They may not request/review/authorize/execute/rollback or alter bindings.

MCP has no execute/apply/set/rollback tool.

No proposal-ready, plan-ready, authorization, window-open, synchronization, agent or MCP event may automatically trigger mutation.

## 31. Simulator failure injection

Test/simulator-only deterministic modes:

```text
SUCCESS
REJECT_BEFORE_APPLY
TIMEOUT_BEFORE_APPLY
TIMEOUT_AFTER_APPLY
APPLY_WRONG_VALUE
READBACK_TIMEOUT
READBACK_STALE
ROLLBACK_FAILURE
ROLLBACK_TIMEOUT_AFTER_APPLY
```

Not available for arbitrary sandbox/future production adapters.

## 32. Core services

At minimum:

```text
NetworkChangeExecutionService
ExecutionAdmissionService
ExecutionReviewService
ExecutionAuthorizationService
ExecutionFingerprintService
ExecutionTargetRegistry
ExecutionLeaseService
ExecutionFinalPreflightService
ChangeOperationExecutionService
ExecutionVerificationService
ExecutionRecoveryService
RollbackReviewService
RollbackAuthorizationService
RollbackExecutionService
ExecutionValidityService
ExecutionAuditService
ExecutionMetrics
ExecutionFailurePersistenceService
```

Reuse authoritative Phase 12/13/14 services rather than duplicating recommendation/planning/knowledge logic.

## 33. Test isolation

Restore all touched shared state: canonical configuration, Phase 6 twin state, Phase 12 sync/knowledge/drift, Phase 13 proposals/candidates, Phase 14 plans/reviews/readiness, Phase 15 execution/evidence and simulator target state. No class-order dependency or Surefire ordering workaround.

## 34. Default CI and local verification

Default CI remains Azure/vendor/production-network independent.

Before completion:

```bash
mvn -B clean test
cd simulator
go test ./...
go build ./cmd/simulator
git diff --check
```

Do not claim CI Go build unless workflow actually runs it.

## 35. Critical integration scenarios

A. READY plan → review → authorize → lease → final preflight → simulator execute → APPLIED → fresh readback → VERIFIED.  
B. Current state changes before execute → zero mutation.  
C. Timeout after apply → OUTCOME_UNKNOWN → fresh desired readback → VERIFIED; no duplicate write.  
D. Wrong resulting state → VERIFICATION_FAILED → RECOVERY_REQUIRED.  
E. Rollback without authorization → rejected; zero rollback mutation.  
F. Authorized rollback → guard → apply → fresh readback → ROLLED_BACK.  
G. Rollback current mismatch → zero write → MANUAL_INTERVENTION_REQUIRED.  
H. Concurrent same target/cell/parameter → one authority only.  
I. Target substitution after authorization → stale authorization → zero mutation.  
J. Duplicate execute after terminal result → no duplicate mutation.  
K. Window expires after authorization → final preflight rejects.  
L. Stale fencing holder cannot mutate/persist success.  
M. Stale cached readback cannot verify.  
N. Simulator mutation leaves canonical DB unchanged.  
O. Canonical state changes only through normal synchronization/reconciliation.  
P. Agent cannot execute/authorize.  
Q. MCP cannot execute/rollback.  
R. Rollback outcome unknown → readback; no blind retry.  
S. Third value after ambiguous forward outcome → manual intervention.  
T. CONTROLLED_SANDBOX unknown environment → fail closed.

## 36. Evidence architecture

Create an evidence catalog mapping every mandatory matrix item:

```text
ID
requirement
evidenceType (STRUCTURAL | BEHAVIORAL | INTEGRATION)
testClass
testMethod
productionClass(optional)
status
notes(optional)
```

A green JUnit invocation is not automatically sufficient behavioral evidence.

## 37. Mandatory matrix — 240 normative items

The implementation must create and maintain a 240-item mandatory matrix. The matrix must cover all frozen architecture gates and at least the following domains:

1. architecture/package isolation;
2. V16/persistence;
3. plan eligibility;
4. target classification/capabilities;
5. review/authorization;
6. execution fingerprint;
7. lease/fencing/concurrency;
8. final preflight;
9. forward execution;
10. idempotency/attempt limits;
11. ambiguous outcomes;
12. independent verification;
13. canonical-state isolation;
14. Phase 12 reconciliation boundary;
15. recovery;
16. rollback request/review/authorization;
17. rollback fingerprint;
18. rollback expected-state guard;
19. rollback ambiguous outcomes;
20. execution windows/cancellation;
21. API permissions/DTO attack surface;
22. transaction durability;
23. audit/metrics/config;
24. agent/MCP/event isolation;
25. simulator failure injection;
26. shared Testcontainer isolation;
27. full Phase 1–14 regression;
28. Go simulator tests/build;
29. all critical scenarios A–T;
30. all 73 frozen architecture gates.

Mandatory closure target:

```text
240 / 240 VERIFIED PASS
0 EVIDENCE INSUFFICIENT
0 FAIL
```

If evidence is weaker, report the actual result. Never inflate 240/240.

## 38. Architecture gates

All frozen 73 Phase 15 architecture gates remain normative implementation gates and must be cross-referenced to structural, behavioral or integration evidence.

Target:

```text
73 / 73 PASS
0 CLARIFICATION REQUIRED
0 CORRECTION REQUIRED
```

## 39. Completion report

Create:

`docs/implementation/SNIP-PHASE-15-GOVERNED-NETWORK-CHANGE-EXECUTION-VERIFICATION-RECOVERY-COMPLETION-REPORT.md`

Report architecture baseline/hash, V16, production/test files, mandatory matrix totals/evidence split, architecture gates, targeted tests, full Maven, Go tests/build, diff check, canonical mutation status, EnmTransport/vendor/credential dependency status, agent/MCP/automatic execution/rollback/closed-loop status, Phase 16 status, working tree and Git mutations.

Required retained safety record:

```text
REAL VENDOR WRITE CAPABILITY: NOT AUTHORIZED
PRODUCTION ERICSSON WRITE ADAPTER: NOT PRESENT
PRODUCTION NOKIA WRITE ADAPTER: NOT PRESENT
VENDOR WRITE CREDENTIAL RESOLUTION: NONE
AGENT EXECUTION: NOT AUTHORIZED
MCP EXECUTION: NOT AUTHORIZED
AUTOMATIC EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
PHASE 16: NOT STARTED
```

## 40. Implementation order

1. domain/enums;
2. V16;
3. entities/repositories;
4. target registry;
5. simulator target state;
6. adapter SPI;
7. fingerprint;
8. preliminary admission;
9. review;
10. execution authorization;
11. lease/fencing;
12. final preflight;
13. forward execution;
14. independent verification;
15. durable failure persistence;
16. recovery;
17. rollback governance;
18. rollback execution/verification;
19. API/security;
20. audit/metrics/config;
21. structural tests;
22. behavioral/integration matrix;
23. full regression;
24. completion report;
25. STOP before Git mutation.

## 41. Explicit non-goals

Do not implement real Ericsson ENM writes, Nokia NetAct writes, production write credentials, vendor CLI/REST/SSH mutation, automatic scheduler execution, agent/MCP execution, automatic rollback, closed-loop optimization, multi-cell/multi-parameter/bulk execution, production canary rollout, production maintenance orchestration, or break-glass mutation.

## 42. Cursor stop rule

After implementation and full local verification:

**STOP BEFORE `git commit`.**  
**STOP BEFORE `git push`.**  
**DO NOT START PHASE 16.**

Return the completion report for a separate architectural conformance review.

**PHASE 15 IMPLEMENTATION SPECIFICATION: COMPLETE — AUTHORIZED FOR IMPLEMENTATION**
