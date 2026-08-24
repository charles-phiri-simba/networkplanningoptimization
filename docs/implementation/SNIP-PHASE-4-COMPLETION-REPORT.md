# SNIP Phase 4 — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-24  
**Architecture:** `docs/architecture/NIP-PHASE-4-GOVERNED-ACTION-MCP-ARCHITECTURE.md` (filename preserved as specified)  
**Contract:** `docs/implementation/SNIP-PHASE-4-GOVERNED-ACTION-MCP-SPECIFICATION.md`  
**Baseline:** `c692eb8d42711ed523460d2de34ffb0a607e7f17` on `main` (Phase 3 architecturally accepted, 60 tests). HEAD is still this commit; Phase 4 is uncommitted working-tree work.  
**Method:** Extend Phase 3; `mvn -B test` (PostgreSQL + Kafka Testcontainers; MCP over HTTP on `RANDOM_PORT`); `go test ./...` and `go build ./cmd/simulator`. Phase 5 was not started. Git push / new baseline were not authorised.

---

## 1. Executive Summary

Phase 4 moves SNIP from recommendation to **safe, governed capability invocation**. AI may propose. Deterministic policy decides. Humans approve where required. A local Java MCP server invokes only registered capabilities. Unsafe apply is denied before MCP.

All three governance outcomes were proven over a real HTTP MCP boundary:

| Path | Action | Risk | Policy | Result |
|------|--------|------|--------|--------|
| A | `GENERATE_REMEDIATION_PLAN` | LOW | ALLOW | MCP `remediation.generate.v1` → SUCCEEDED; replay does not reinvoke |
| B | `SIMULATE_CELL_PARAMETER_CHANGE` | MEDIUM | REQUIRE_APPROVAL | execute blocked; after approve → synthetic MCP success |
| C | `APPLY_CELL_PARAMETER_CHANGE` | HIGH | DENY | DENIED; no MCP invocation |

`mvn -B test`: **71 tests, 0 failures** (2026-08-24T16:30:27+02:00). `go test ./...` PASS. `go build ./cmd/simulator` exit 0. No Ollama in CI. No live network write path.

---

## 2. Phase 3 Baseline Verification

| Check | Result |
|-------|--------|
| Started from `c692eb8d42711ed523460d2de34ffb0a607e7f17` | Yes (`git rev-parse HEAD`) |
| Phase 1–3 regressions | PASS (60 baseline tests remain in the 71) |
| Phase 3 severity/confidence unchanged | PASS |
| Kafka default off | PASS |
| No live network write path | PASS |

---

## 3. Scope Delivered

- `ProposedAction` + policy, approval, result, append-only audit
- Flyway V5
- Deterministic risk/policy/lifecycle
- Human approval for simulation
- Capability registry (`remediation.generate.v1`, `simulation.cell-parameter.v1`)
- Local Java MCP JSON-RPC server at `POST /mcp` and HTTP gateway
- Governance APIs (propose/list/get/approve/reject/execute)
- Canonical path tests including MCP HTTP
- ADRs 020–026, README, CONTEXT/STATUS, cursor rule

---

## 4. ProposedAction Domain

Persisted fields match the specification: id, assuranceCaseId, actionType, capabilityId, targetType/targetId, parameters, rationale, riskLevel, policyDecision, status, proposedAt, proposedBy, synthetic, plus `executedBy` when executed. Creation never invokes MCP.

---

## 5. Risk Classification

Deterministic: GENERATE `LOW`, SIMULATE `MEDIUM`, APPLY `HIGH`. `CRITICAL` exists for future classes and is not assigned. The LLM cannot override risk.

---

## 6. Deterministic Policy Model

`ActionPolicyEvaluator` / `POLICY_GOVERNED_ACTION_V1`:

- GENERATE → ALLOW
- SIMULATE → REQUIRE_APPROVAL
- APPLY → DENY

Policy ID, reason, and `evaluatedAt` are persisted.

---

## 7. Action Lifecycle

Bounded statuses as specified. After proposal: ALLOW → `POLICY_EVALUATED`; REQUIRE_APPROVAL → `APPROVAL_REQUIRED`; DENY → `DENIED`. Invalid transitions return HTTP 409.

---

## 8. Human Approval

`POST .../approve` and `.../reject` persist decision, `decidedBy`, `decidedAt`, comment. Simulation cannot execute until APPROVED. Rejection is terminal.

---

## 9. Capability Registry

In-code `CapabilityRegistry`. Only two enabled capabilities. APPLY has **no** registered capability. Compensation is `false`. Simulation is `dryRunOnly` and `requiresApproval`.

---

## 10. Java MCP Architecture

Local Spring MVC JSON-RPC 2.0 at `POST /mcp` (`initialize`, `tools/list`, `tools/call`) plus `GET /mcp/health`. Same JVM as SNIP; gateway uses HTTP loopback. Spring Boot 3.3.6 / Spring AI 1.0.0 were not upgraded. No Python/Go MCP server. No remote MCP.

---

## 11. MCP Gateway

`McpCapabilityGateway` verifies action, registration/enabled, compatibility, policy, approval, risk (HIGH/CRITICAL blocked), dry-run, and not-already-succeeded, then POSTs `tools/call`. Fail closed.

---

## 12. remediation.generate.v1

Composes a structured plan from the persisted Assurance Case and `DecisionSupportComposer` (not LLM wording). Warnings state RCA is not confirmed and that nothing was applied.

---

## 13. simulation.cell-parameter.v1

Requires `dryRun=true` before MCP. Output labelled synthetic. Not a production RF twin. `dryRun=false` is rejected at proposal.

---

## 14. Denied APPLY_CELL_PARAMETER_CHANGE

HIGH / DENY / DENIED. Execute → 409. No MCP audit events. No simulated substitute apply.

---

## 15. Persistence/Flyway

`V5__governed_action.sql`: `proposed_action`, `policy_decision`, `action_approval`, `action_result`, `action_audit_event`. Unique action ids. FK to `assurance_case`.

---

## 16. Append-only Audit

Events: ACTION_PROPOSED, POLICY_EVALUATED, APPROVAL_REQUESTED, ACTION_APPROVED, ACTION_REJECTED, ACTION_DENIED, MCP_INVOCATION_STARTED/SUCCEEDED/FAILED. Rows are inserted only.

---

## 17. Idempotency

`actionId` is the key. After SUCCEEDED, a second execute returns the prior result and does not increment `mcpInvocations`.

---

## 18. Timeout/Failure

RestClient connect/read timeout `snip.mcp-timeout-ms` (default 3000). Failure sets action/result FAILED and appends `MCP_INVOCATION_FAILED`. No automatic retry storm.

---

## 19. APIs

```text
POST /api/v1/assurance/cases/{caseId}/actions
GET  /api/v1/actions
GET  /api/v1/actions/{actionId}
POST /api/v1/actions/{actionId}/approve
POST /api/v1/actions/{actionId}/reject
POST /api/v1/actions/{actionId}/execute
POST /mcp
```

These mutate SNIP governance state only.

---

## 20. Tests

| Suite | Result |
|-------|--------|
| `mvn -B test` | **71 tests, 0 failures** |
| `go test ./...` | PASS |
| `go build ./cmd/simulator` | PASS |

Phase 4 additions: `ActionPolicyEvaluatorTest` (4), `GovernedActionApiTest` (7, `RANDOM_PORT` HTTP MCP).

---

## 21. MCP Integration Evidence

`GovernedActionApiTest` uses `WebEnvironment.RANDOM_PORT` and `TestRestTemplate`. Execute calls `McpCapabilityGateway`, which HTTP POSTs `http://127.0.0.1:{port}/mcp`. `tools/list` and `/mcp/health` are asserted. This is not an in-process method stub.

---

## 22. Path A — ALLOW

GENERATE → LOW → ALLOW → execute → `remediation.generate.v1` → SUCCEEDED → executor `SNIP_ACTION_SERVICE` → audit includes `MCP_INVOCATION_SUCCEEDED` → replay does not reinvoke.

---

## 23. Path B — REQUIRE_APPROVAL

SIMULATE → MEDIUM → APPROVAL_REQUIRED → execute 409 (mcp count unchanged) → approve → execute → synthetic result with dry-run labelling.

---

## 24. Path C — DENY

APPLY → HIGH → DENY → DENIED → execute 409 → no result row → no MCP_* audit events.

---

## 25. Local E2E Evidence

Verification of record is the Maven `RANDOM_PORT` suite against Testcontainers PostgreSQL (same HTTP MCP loopback the process uses in `spring-boot:run`). Governance does not depend on Ollama. A separate host-Ollama Compose run was not required to prove policy/MCP/deny.

---

## 26. Observability

Logs/counters: `actionsProposed`, policy allow/require/deny, `actionsApproved`/`actionsRejected`, `mcpInvocations`, `mcpInvocationFailures`, `mcpInvocationLatencyMs`, `idempotentExecutionHits`, correlated by `actionId` / `assuranceCaseId`.

---

## 27. Security/Zero-Live-Write Review

- No live network endpoint or vendor credentials
- LLM has no MCP handle
- DENY never reaches MCP
- Simulation blocked before approval
- `dryRun=false` rejected at proposal
- Risk/policy not taken from generated text
- Executor is `SNIP_ACTION_SERVICE`
- Audit append-only

---

## 28. ADRs

020 ProposedAction/lifecycle · 021 deterministic policy · 022 human approval · 023 gateway/registry · 024 local Java MCP · 025 audit/idempotency · 026 no live network writes.

---

## 29. Performance

MCP HTTP in tests is milliseconds on loopback. Bounded timeout 3 s. No extra Docker MCP container.

---

## 30. Acceptance PASS/FAIL

### Baseline
- [x] Starts from `c692eb8d42711ed523460d2de34ffb0a607e7f17`
- [x] Phase 1–3 regressions pass
- [x] Phase 3 responsibilities preserved
- [x] No live network write path

### Domain/Persistence / Policy / Approval / MCP / Safety / Canonical / CI
- [x] All specification checklists in §42 evaluated PASS in this working tree

### Scope
- [x] No Phase 5, Agents, remote MCP, ENM/NetAct writes

---

## 31. Known Limitations

- MCP is JSON-RPC 2.0 over HTTP in-process loopback, not Spring AI 2.0 SSE/streamable HTTP.
- Capability registry is code, not a DB table.
- `ACKNOWLEDGE`/`RESOLVE` for assurance cases remain Phase 3-deferred.
- FAILED retry deletes/replaces the prior failed `action_result` row (success remains idempotent). This is accepted Phase 4 technical debt; immutable execution-attempt history is deferred. Do not redesign it in Phase 4.
- `GovernedActionApiTest` commits then deletes p4 telemetry so it does not pollute the shared Testcontainers DB.

---

## 32. Technical Debt

- Bind Kafka listener `groupId` (Phase 2, still deferred).
- Immutable execution-attempt history for FAILED results (Phase 4 currently replaces/deletes the prior failed `action_result` row). Do not redesign in this phase.
- Pagination for action list.

---

## 33. Lessons Learned

- Shared Testcontainers PostgreSQL plus non-transactional HTTP tests will leak CELL-001 telemetry into later classes unless cleaned up.
- `@RestControllerAdvice` must keep `DomainValidationException` → 400.
- Same-JVM MCP is still a protocol boundary if the gateway uses HTTP.

---

## 34. Recommended Next Phase

Phase 4 is **frozen**. Do not add functionality, resolve deferred technical debt, perform unrelated refactoring, or start Phase 5 from this report. Agent Factory, remote MCP, and live network writes remain closed until explicitly authorised.

---

## 35. Architectural Questions — resolved

Phase 4 was architecturally accepted on 2026-08-24. The three questions are closed as follows:

1. **Same-JVM loopback JSON-RPC HTTP MCP — ACCEPT for Phase 4.** Do not upgrade Spring Boot or Spring AI solely to change MCP transport.
2. **In-code capability registry — ACCEPT for Phase 4.** Database-backed / distributed capability registration is deferred.
3. **Phase 4 POST APIs — ACCEPT as governance-state mutations only.** They do not authorise live network mutation.

---

PHASE 4 STATUS: ARCHITECTURALLY ACCEPTED
