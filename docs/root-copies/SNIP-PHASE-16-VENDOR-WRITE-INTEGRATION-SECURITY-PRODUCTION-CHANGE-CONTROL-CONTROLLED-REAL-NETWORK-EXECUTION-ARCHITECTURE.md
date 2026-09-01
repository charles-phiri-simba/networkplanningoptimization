# SNIP Phase 16 — Vendor Write Integration Security, Production Change Control & Controlled Real-Network Execution

## Architecture Document

**Status:** CORRECTED CANDIDATE — READY FOR FINAL ARCHITECTURAL REVIEW  
**Date:** 2026-09-01  
**Phase:** 16  
**Document type:** Architecture (normative; post-review correction candidate)  
**Parent implementation baseline (immutable):** `ae9c13d55b444fa50090813495b32b82f97c2ec3`  
**Failed historical Phase 15 candidate (preserve; do not rewrite):** `0cb1223e41ced5462ad552f993e6001a028ddb96`  
**Pre-correction architecture SHA-256 (historical review evidence):** `fd396f595472739b444a7e5c57971f719a1845488c5fec23c2d5212eedbd1f3f`  
**Implementation:** NOT STARTED  
**Implementation specification:** NOT AUTHORIZED  
**Architecture acceptance:** NOT YET ACCEPTED / NOT YET FROZEN  
**Real production execution:** NOT YET AUTHORIZED  
**Ericsson production write transport:** NOT CONFIGURED / UNRESOLVED  
**Nokia production write support:** NOT IMPLEMENTED  
**Closed-loop optimization:** NOT AUTHORIZED  

---

# 0. Purpose

This architecture defines the **security, change-control, and controlled real-network execution** plane that may — only after independent Phase 16 production authorization, separate runtime mediation, and future certification — execute an explicitly authorized, cryptographically bound, independently verified **Phase 15** execution against a specifically approved real-network target through a least-privilege vendor write adapter.

Phase 16 is the first SNIP phase that contemplates **real-network mutation**. It does so under deny-by-default, human-gated, scope-bounded, time-bounded, observable, recoverable controls that are **incapable of autonomous or agent-initiated execution**.

This document is architecture only. It does **not** authorize implementation, V17 creation, production transport configuration, architecture freeze, or Level-4 production certification.

---

# 1. Defining Principle

> Phase 16 may execute an explicitly authorized, cryptographically bound, independently verified Phase 15 execution against a specifically approved real-network target through a least-privilege vendor write adapter.
>
> Production mutation is deny-by-default, human-gated, scope-bounded, time-bounded, observable, recoverable, and incapable of autonomous or agent-initiated execution.

Corollaries:

1. Upstream Phase 13/14/15 approvals are **never** sufficient for production mutation.
2. The ordinary SNIP application process **MUST NOT** possess vendor-write workload identity or write credentials.
3. Vendor acceptance is **not** verification.
4. Ambiguous outcomes **MUST NOT** trigger blind retry.
5. Automatic rollback is **forbidden**.
6. Exact-once external network mutation is **not claimed**.
7. Fencing protects SNIP authority; it does **not** cancel an already-accepted external vendor write.
8. A caller-supplied request body is **never** mutation authority; durable grant state is.
9. Grant possession **never** overrides kill switch, suspension, or rate/blast-radius denial.

---

# 2. Parent Baseline and Phase Relationship

## 2.1 Immutable parents

| Artifact | SHA / status |
|---|---|
| Phase 15 immutable implementation baseline | `ae9c13d55b444fa50090813495b32b82f97c2ec3` |
| Failed historical Phase 15 candidate | `0cb1223e41ced5462ad552f993e6001a028ddb96` (preserve; do not rewrite) |
| Phase 14 implementation baseline candidate | `043c5ad98b8a12fb8073ba40364a2e287d2cc65a` |
| Phase 13 Git baseline | `5e9400005626fb93d5e61f96be680bea5540df31` |
| Phase 11 Git baseline | `78e699380be37109cfdd2111dd0f29c7052709c3` |

Phase 15 remains **ARCHITECTURALLY ACCEPTED** and **IMMUTABLE**. Phase 16 **MUST NOT** amend Phase 15 semantics, weaken Phase 15 simulator/sandbox boundaries, or rewrite Phase 15 history.

## 2.2 Authorization chain (non-substitutable)

```text
Phase 13 proposal approval
  ≠ Phase 14 plan authorization
  ≠ Phase 15 execution authorization
  ≠ Phase 16 production authorization
```

Each layer requires an independent human decision against its own fingerprint-bound artifact. Production authorization **MUST NOT** be inferred from any upstream approval.

## 2.3 What Phase 15 already proved (and what it did not)

Phase 15 proved governed execution lifecycle, fingerprinting, lease/fencing, expected-state guards, independent verification, recovery signaling, and governed rollback **against simulator / bounded non-production targets**.

Phase 15 **did not** authorize real vendor writes, production targets, write credential resolution, production change-control as mutation authority, or a production write gateway.

---

# 3. Scope

## 3.1 Initial production mutation scope (hard)

| Constraint | Value |
|---|---|
| Vendor | ERICSSON |
| Platform | ENM |
| Object | CELL |
| Parameter | txPower |
| Max cells per execution | 1 |
| Max parameters per execution | 1 |
| Max forward mutation operations | 1 |
| Rollback | exact persisted Phase 14/15 rollback operation only |
| Expected-state guard | mandatory |
| Independent verification | mandatory |
| Production execution authorization | mandatory |
| Production rollback authorization | mandatory |
| Maintenance / change window | mandatory |
| Production target registration | mandatory |

## 3.2 Explicit non-goals / forbidden in Phase 16

Phase 16 **MUST NOT** introduce:

- Nokia NetAct write adapter
- generic Ericsson command execution
- SSH / CLI execution
- arbitrary REST proxying
- caller-supplied vendor endpoint
- caller-supplied arbitrary parameter / desired / rollback values
- agent execution
- MCP mutation tools
- scheduled production execution
- event-driven production execution
- automatic rollback
- closed-loop optimization
- AI authorization / AI target selection / AI credential selection
- distributed transactions with vendor systems
- multi-cell / multi-parameter / bulk changes
- automatic retry after mutation may have been transmitted
- dual-purpose Phase 11 `EnmTransport` writes
- governance bypass / emergency ungoverned rollback in Phase 16
- bearer/self-contained grant as sole mutation authority

---

# 4. Primary Architectural Decision — Separate Production Write Gateway Runtime

## 4.1 Decision

Phase 16 **MUST** use a **SEPARATE PRODUCTION WRITE GATEWAY RUNTIME**.

Production vendor writes **MUST NOT** be implemented in the ordinary SNIP application process.

## 4.2 Required trust boundary

```text
SNIP application
    |
    | authenticated internal request carrying grantId
    | (+ immutable correlation metadata only)
    v
SNIP Production Write Gateway
    |
    | durable authoritative grant lookup + atomic consume
    | dedicated workload identity
    | dedicated vendor-write credentials
    | constrained egress
    v
Approved real vendor network target
```

## 4.3 Normative consequences

1. The write gateway is a **security boundary**, not merely another Spring `@Service` class in the SNIP app JVM.
2. The normal SNIP application runtime **MUST NOT** possess the vendor-write workload identity.
3. The normal SNIP application runtime **MUST NOT** be able to resolve production vendor-write credentials from Key Vault (or equivalent).
4. Absence of a valid durable `ProductionExecutionGrant` in status `ISSUED` that can be atomically consumed ⇒ write gateway **MUST deny**.
5. The gateway **MUST NOT** trust a caller-supplied self-contained grant token as sole authority.
6. Deployment topology (separate Deployment/Service/NetworkPolicy/identity binding) is part of the architecture.

## 4.4 What may remain in the SNIP application

The SNIP application owns governance lifecycle through lease acquisition and pre-grant preflight, creates durable grants, enforces SoD, aggregates audit/evidence for humans, and denies agent/MCP/scheduler/event mutation paths.

The SNIP application **initiates** a grantId-bound gateway call; it does **not** perform the vendor write itself and does **not** fabricate gateway evidence.

## 4.5 Gateway compromise honesty

Separate gateway protects write credentials from the ordinary SNIP application. It does **NOT** make a fully compromised write gateway harmless.

Defense-in-depth **MUST** include: typed mutation only; authoritative grant validation; single-use consume; target allowlist; rate limits; blast radius; kill switch; TLS; credential least privilege; egress restrictions; audit; suspension.

Architecture **MUST NOT** claim zero residual risk under total compromise of gateway workload identity and code.

---

# 5. Deployment Topology

## 5.1 Logical components

| Component | Role |
|---|---|
| SNIP NPO application | Governance plane; grant issuer into durable store; no write credentials |
| Authoritative production grant store | Durable grant/attempt/evidence authority (typically PostgreSQL) |
| SNIP Production Write Gateway | Mutation plane; grant consume; credentials; vendor I/O; evidence producer |
| Azure Key Vault (or approved equivalent) | Secret store; profile-referenced only |
| Microsoft Entra Workload Identity | Distinct identities for app / read / write-gateway |
| Approved Ericsson ENM endpoint(s) | Lab/test first; production only after Level 4 |
| PostgreSQL | Durable SNIP production-change, grant, attempt, audit, evidence state |

## 5.2 Network isolation expectations

```text
SNIP application  --(authenticated execution-plane)-->  Write Gateway
                                                              |
                                                              v
                                                   approved ENM endpoint(s) only
```

- Gateway ingress accepts only authenticated SNIP execution-plane traffic (mTLS or equivalent service identity).
- Gateway egress is restricted to specifically approved Ericsson ENM endpoint(s); **no** `0.0.0.0/0` production-write egress.
- Kubernetes NetworkPolicy / Cilium policies are mandatory defense-in-depth.
- Document known Cilium FQDN-cache/CIDR lab limitations without treating them as permission for broad egress.

## 5.3 Fail-closed default

Fresh clone/deployment with no external production configuration **MUST** have **ZERO** real-network mutation capability because:

- production write transport unresolved / NOT CONFIGURED
- no Level-4 target
- global write enable false by default
- write workload identity not provisioned by default
- write credentials absent
- target absent/disabled
- certification incomplete

Until certified for a given target: production mutation **DENIED**; transport **NOT CONFIGURED**; certification below Level 4.

---

# 6. Identity Model

## 6.1 Distinct identities (conceptual)

| Identity | Purpose | May resolve write credentials? |
|---|---|---|
| SNIP application identity | APIs, DB, governance | **NO** |
| SNIP read identity | Phase 11/12 read-only vendor access | **NO** (write secrets) |
| SNIP production-write identity | Write gateway only | **YES** (write secrets only; least privilege) |

## 6.2 Rules

1. Reuse Phase 10 principles: AKS Microsoft Entra Workload Identity; Azure Key Vault; no long-lived secret cache; no credential persistence; no secrets in logs/audit/API/metrics.
2. Production write credentials **MUST** be separate from read-only vendor credentials.
3. Write credentials **MUST** be retrievable only by the write-gateway identity.
4. Compromised SNIP application workload **MUST NOT** be able to directly retrieve write credentials.
5. DefaultAzureCredential remains local-dev only; production uses explicit WorkloadIdentityCredential (Phase 10).

---

# 7. Credential Architecture and Lifecycle

## 7.1 Sequence (normative)

```text
production authorization
→ production lease
→ application pre-grant preflight
→ durable ProductionExecutionGrant issuance (ISSUED)
→ authenticated gateway request (grantId)
→ gateway validation
→ atomic grant consume (ISSUED → CONSUMED)
→ durable attempt mark
→ credential resolution (late)
→ vendor session
→ gateway final pre-mutation preflight
→ direct expected-state observation
→ typed mutation
→ independent verification
→ durable evidence persistence
→ session destruction / credential release
```

## 7.2 Persistence prohibition

Credential **values** **MUST** never enter: database, logs, audit, metrics, HTTP response, exception body, completion report, execution evidence.

Only credential **profile IDs / references** (and non-secret version identifiers if needed for evidence) may be persisted.

## 7.3 Late resolution

Credential resolution occurs **after** atomic grant consume and as late as practical before vendor session establishment.

## 7.4 Write credential rotation (normative Phase 10 restatement for write path)

For production write credentials:

1. Resolve the **current authorized credential version** per execution session.
2. **No** long-lived secret cache.
3. **No** silent fallback to older credential versions.
4. If current credential version fails or is disabled ⇒ **fail closed**.
5. Rotation between authorization and execution is handled via `credentialProfileId` / policy binding without persisting secret material.
6. Non-secret secret-version identifiers **MAY** be recorded for evidence; secret values **MUST NOT**.
7. **MUST NOT** reuse read credentials for write.

---

# 8. Phase 11 Read-Only Boundary (Immutable Constraint)

Phase 11 `EnmTransport` **MUST** remain read-only.

Phase 16 **MUST NOT** add to `EnmTransport`: `setParameter`, `apply`, `executeCommand`, `write`, `mutate`.

Phase 16 introduces a **separate write-side abstraction**.

---

# 9. Adapter and Transport SPI

## 9.1 Recommended types

| Type | Responsibility |
|---|---|
| `VendorNetworkWriteAdapter` | Vendor-neutral write adapter contract |
| `EricssonEnmWriteAdapter` | Ericsson ENM-scoped typed mutation adapter |
| `EricssonWriteTransport` | Protocol/transport SPI behind the adapter |
| `ProductionExecutionContext` | Gateway-side execution context |
| `AuthorizedVendorMutation` | Strongly typed mutation payload |
| `VendorMutationResult` | Structured mutation outcome |
| `PostMutationObservation` | Independent readback observation |
| `ProductionExecutionGrant` | Durable short-lived single-use production authority record |
| `ProductionGatewayAttempt` | Durable gateway attempt / handoff record |
| `ProductionGatewayEvidence` | Durable gateway-produced evidence package fragment |

## 9.2 Transport layering

```text
EricssonEnmWriteAdapter
        |
        v
EricssonWriteTransport
        |
        +--> controlled test / lab implementation
        |
        +--> production implementation: NOT CONFIGURED
```

## 9.3 Ericsson production write protocol — UNRESOLVED

The concrete production Ericsson write protocol is **UNRESOLVED** until the actual approved Ericsson interface is known from vendor/environment evidence.

Non-binding examples only: REST, CM interface, Bulk CM, vendor SDK, customer integration gateway, approved command interface.

Architecture **MUST NOT** select one without real evidence. While unresolved: production transport **NOT CONFIGURED**; production path **fail-closed**; code existence ≠ production authorization.

## 9.4 Typed mutation only

No generic `executeCommand(String)`, `apply(Map)`, or `POST /vendor-command`.

Adapter receives strongly typed mutation: `objectType=CELL`, cellId from governed state, `parameter=txPower`, `expectedValue`, `desiredValue`. Unsupported types/parameters **MUST** be rejected.

---

# 10. Production Network Target Model

## 10.1 Aggregate: `ProductionNetworkTarget`

Required fields (minimum): `targetId`, `vendor`, `platform`, `environment`, `region`, `networkDomain`, `adapterProfileId`, `capabilityProfileVersion`, `securityProfileId`, `credentialProfileId`, `allowedObjectTypes`, `allowedParameters`, `changeWindowPolicy`, `rollbackPolicy`, `verificationPolicy`, `enabled`, `targetState`.

## 10.2 Target states

| State | Meaning |
|---|---|
| `ACTIVE` | Eligible if all other gates pass |
| `SUSPENDED` | Safety suspension; execution denied; no auto-resume |
| `DISABLED` | Administratively disabled; execution denied |

## 10.3 Initial allowed target shape

vendor=ERICSSON, platform=ENM, environment=PRODUCTION (registration only; execution still certification-gated), object=CELL, parameter=txPower.

Target records contain **credential references/profiles only**, never secrets.

## 10.4 Target administration (controlled provisioning)

No general public target CRUD API is required in Phase 16.

Targets **MUST** be provisioned through a controlled configuration / infrastructure-as-code / privileged administration path.

Creation, edit, and enable operations require a dedicated privileged role, for example:

```text
ADMINISTER_PRODUCTION_TARGET
```

A normal production executor **MUST NOT** implicitly hold this permission.

Security-significant target changes **MUST** be audited, including at least: vendor, platform, environment, endpoint/profile bindings, `credentialProfileId`, `securityProfileId`, `capabilityProfileVersion`, allowed object types, allowed parameters, enabled state.

Any execution-significant target change **MUST** invalidate/stale existing production authorizations and unconsumed grants bound to the prior target fingerprint.

Resume from `SUSPENDED` requires explicit authorized human action. Automatic resume is **FORBIDDEN**.

API surface may expose privileged `suspend` / `resume` only; broad target CRUD is not required unless separately justified.

---

# 11. Production Change Aggregate

## 11.1 Aggregate name

Primary aggregate: **`ProductionNetworkChange`**, distinct from Phase 15 `NetworkChangeExecution`.

## 11.2 Creation request (only)

Creation accepts **only**: `phase15ExecutionId`, `productionTargetId`, `changeControlReference`.

Creation **MUST NOT** accept caller-controlled: `cellId`, `parameter`, `expectedValue`, `desiredValue`, `rollbackValue`, `vendorCommand`, `endpoint`, credentials, fingerprint override.

## 11.3 Mutation detail provenance

All mutation details **MUST** derive from already-governed Phase 14/15 state. Caller cannot inject mutation semantics. Request-body mutation data is **never** authoritative.

## 11.4 Creation side effects

Creation **MUST NOT** execute. Authorization **MUST NOT** execute. Window opening **MUST NOT** execute.

---

# 12. Change Control

## 12.1 `ChangeControlReference`

Minimum model:

| Field | Notes |
|---|---|
| `system` | Initial: `MANUAL` |
| `reference` | External or manual ticket/reference id |
| `status` | Validated status |
| `validatedByPrincipalId` | Stable principal who validated (authoritative) |
| `validatedAt` | Validation timestamp |
| `validUntil` | Expiry |

## 12.2 MANUAL meaning

`MANUAL` means a human-supplied external change reference validated by an authorized change-control reviewer/operator according to organizational policy.

The same requester **MUST NOT** self-validate if organizational policy requires separation.

## 12.3 Rules

1. A valid change-control reference is **REQUIRED**.
2. Change-control validity is **NOT** itself production authorization.
3. ServiceNow (or other ITSM) integration is **deferred** unless explicitly authorized later.
4. Ticket substitution that changes binding material invalidates production fingerprint / authorization.
5. Change-control validity **MUST** be rechecked at **gateway consume / final pre-mutation preflight** time against authoritative durable status — not only the status captured at grant issuance.
6. If expired, revoked, invalid, unknown, or source unavailable when validation is mandatory ⇒ **ZERO mutation**.

---

# 13. Authorization Model and Separation of Duties

## 13.1 Permissions (minimum)

`VIEW_PRODUCTION_CHANGE`, `REQUEST_PRODUCTION_CHANGE`, `REVIEW_PRODUCTION_CHANGE`, `AUTHORIZE_PRODUCTION_CHANGE`, `EXECUTE_PRODUCTION_CHANGE`, `VIEW_PRODUCTION_EXECUTION_EVIDENCE`, `REQUEST_PRODUCTION_ROLLBACK`, `REVIEW_PRODUCTION_ROLLBACK`, `AUTHORIZE_PRODUCTION_ROLLBACK`, `EXECUTE_PRODUCTION_ROLLBACK`, plus privileged `ADMINISTER_PRODUCTION_TARGET` for target administration.

## 13.2 Separation of duties (production)

| Rule | Normative |
|---|---|
| Requester ≠ Production Authorizer | MUST |
| Production Authorizer ≠ Executor | MUST |
| Reviewer permission ≠ Authorizer permission | MUST |
| Agents hold none of the mutation permissions | MUST |
| MCP holds none of the mutation permissions | MUST |

## 13.3 Stable actor principal identity

All separation-of-duties comparisons **MUST** use immutable/stable authenticated principal identifiers (for example subject ID, enterprise principal ID, or directory object ID).

Display names, email display text, mutable usernames, or presentation labels **MUST NOT** be the authoritative SoD key.

Persist:

- `actorPrincipalId` (authoritative)
- display information only as **non-authoritative** audit metadata

## 13.4 Independence

Production authorization is a **new independent** human decision, fingerprint-bound, and **not** inferred from Phase 13/14/15.

---

# 14. Production Execution Fingerprint

## 14.1 Algorithm

Deterministic **SHA-256** over a canonical encoding of execution-significant state.

## 14.2 Mandatory bindings (minimum)

`phase15ExecutionId`, `phase15ExecutionFingerprint`, `phase14PlanId`, `phase14PlanVersion`, `phase14PlanFingerprint`, `productionTargetId`, `vendor`, `platform`, `environment`, `cellId`, `parameter`, `expectedValue`, `desiredValue`, `rollbackExpectedValue`, `rollbackDesiredValue`, `adapterProfileId`, `capabilityProfileVersion`, `securityProfileId`, `credentialProfileId`, `changeWindowId`, `changeWindowStart`, `changeWindowEnd`, `productionPolicyVersion`, `verificationPolicyVersion`, `rollbackPolicyVersion`, `changeControlReference`, `authorizationGeneration`.

## 14.3 Rules

1. Any material binding change **MUST** make authorization **STALE**.
2. No silent fingerprint regeneration.
3. No silent reauthorization.
4. Grant issuance and gateway validation bind to the current production fingerprint.
5. Execution-significant target changes **MUST** stale authorizations and unconsumed grants bound to the prior fingerprint.

---

# 15. Production Execution Grant — Authoritative Durable Protocol

## 15.1 Purpose

`ProductionExecutionGrant` is **not** a vendor credential and is **not** a self-contained bearer capability.

It is a **durable server-side** short-lived authority record for **one exact** production mutation (or one exact rollback mutation under rollback grant rules).

## 15.2 Authoritative grant store

There **MUST** be exactly one authoritative durable production grant store (typically PostgreSQL tables under SNIP control).

```text
SNIP application
    |
    | creates durable grant (ISSUED)
    v
authoritative production grant store
    ^
    |
Write Gateway validates and atomically consumes
```

The gateway **MUST NOT** trust a caller-supplied self-contained grant as authority by itself.

## 15.3 Required properties

| Property | Notes |
|---|---|
| `grantId` | Opaque unique identifier |
| `productionChangeId` | Bound change |
| `phase15ExecutionId` | Upstream binding |
| `targetId` | Bound target |
| `productionFingerprint` | Exact fingerprint |
| `fencingToken` | Lease fencing |
| `authorizationGeneration` | Bound auth generation |
| `executionBinding` | Exact governed mutation binding reference |
| `issuedAt` / `expiresAt` | Short-lived |
| `singleUse` | Always true for Phase 16 |
| `status` | ISSUED / CONSUMED / EXPIRED / REVOKED |

## 15.4 Grant delivery (app → gateway)

The app→gateway request is an **authenticated internal protocol**.

The ordinary SNIP application **MAY** send:

- `grantId`
- `productionChangeId`
- execution correlation ID

It **MUST NOT** send: credential, vendor command, arbitrary mutation fields, arbitrary target endpoint, expected/desired override, rollback override, security-profile override.

Gateway **MUST** resolve the authoritative grant and mutation bindings from **trusted durable state**. Request-body mutation data is never authoritative.

Optional signed envelopes for transport optimization **MUST** be secondary evidence only and **MUST NOT** replace authoritative durable grant lookup and atomic consumption.

## 15.5 Atomic single-use consumption

Before vendor mutation, the gateway **MUST** perform an atomic conditional transition equivalent to:

```text
ISSUED → CONSUMED
```

only if **ALL** remain true:

- grantId matches
- status == ISSUED
- not expired
- not revoked
- fingerprint matches
- target matches
- fencing token matches
- authorization generation matches
- execution binding matches

Implementation-neutral requirement: database compare-and-set / optimistic conditional update / row-lock semantics.

Exactly one gateway invocation **MAY** successfully consume a given grant. Concurrent consumption attempts: **one succeeds; all others deny**.

Do **not** claim exactly-once vendor mutation.

A consumed grant is **NEVER** reset to `ISSUED` automatically.

## 15.6 Revocation

Statuses: `ISSUED`, `CONSUMED`, `EXPIRED`, `REVOKED`.

Revocation **MAY** occur only before successful consumption. Gateway consumption **MUST** observe current durable status. A revoked grant **MUST** deny even if the caller possesses an older request payload.

Once consumed, revoke does **not** claim an already-started external write was cancelled.

## 15.7 Grant timeout / crash matrix

| Scenario | Durable grant | Vendor mutation | Allowed action |
|---|---|---|---|
| 1. Timeout before gateway receives request | Remains ISSUED (unless expired/revoked) | None | Caller **MAY** retry delivery of **same** grantId while still ISSUED |
| 2. Gateway receives but fails before atomic consume | Remains ISSUED | Zero | Retry same grantId while ISSUED |
| 3. Gateway atomically consumes, then crashes before vendor send | Remains CONSUMED | None | **MUST NOT** auto-reissue/retry mutation; durable attempt → safe unresolved/pre-send-consumed; human/governed recovery |
| 4. Consume and vendor send may have started | CONSUMED | Uncertain | `PRODUCTION_OUTCOME_UNKNOWN`; **no blind retry** |
| 5. Gateway crashes after vendor send | CONSUMED | May have applied | `PRODUCTION_OUTCOME_UNKNOWN` unless durable evidence proves otherwise; direct readback required |
| 6. App crashes while gateway operates | Per gateway/durable protocol | Per protocol | App recovers from durable grant/attempt/evidence; **no automatic second send** |
| 7. Gateway response lost after successful verification | CONSUMED | Done | Caller **MUST** read existing durable result; **no second mutation** |

---

# 16. Consume-Before-Send Ordering

Normative ordering before any production mutation may be transmitted:

```text
authenticate
→ load authoritative grant
→ validate
→ atomic consume (ISSUED → CONSUMED)
→ durable mark of execution intent / attempt
→ credential resolution
→ vendor session
→ final gateway pre-mutation checks
→ vendor expected-state observation
→ vendor mutation send
```

The grant **MUST** be atomically consumed **BEFORE** any production mutation may be transmitted.

**No** mutation may be sent while grant state remains `ISSUED`.

---

# 17. Production Execution Lease / Fencing

## 17.1 Protected scope

```text
productionTargetId + cellId + parameter
```

## 17.2 Rules

1. Acquire before application pre-grant preflight completes / before grant issuance.
2. Only current holder may: receive grant issuance; cause gateway invocation; participate in durable attempt persistence; initiate rollback workflow after recovery signaling.
3. **Fencing protects SNIP state and pre-send authority.**
4. **Fencing does NOT cancel an already-accepted external vendor write.**
5. Do not claim distributed ACID with the vendor system.
6. Do not claim exact-once network mutation.

---

# 18. Split Preflight Model

## 18.1 Application admission / pre-grant preflight

Before grant issuance, the SNIP application **MUST** revalidate at least:

1. Phase 13 proposal still valid  
2. Phase 14 plan still valid  
3. Phase 14 plan fingerprint current  
4. Phase 15 execution still eligible  
5. Phase 15 execution fingerprint current  
6. Phase 16 production authorization current  
7. Production fingerprint current  
8. Change-control reference valid  
9. Change window currently open  
10. Production target registered, enabled, and `ACTIVE`  
11. Vendor/platform/environment unchanged  
12. Adapter / capability / security / credential profiles current  
13. Knowledge confidence acceptable  
14. Phase 12 synchronization trustworthy  
15. Freshness acceptable  
16. No relevant unresolved drift  
17. Target cell still exists  
18. Parameter still supported  
19. Rollback remains valid  
20. Lease current  
21. Fencing token current  
22. Operation count == 1  
23. Parameter == txPower  

Anything **UNKNOWN** ⇒ **DENY** (no grant issuance).

Passing application pre-grant preflight allows grant issuance. It **DOES NOT** authorize mutation by itself.

## 18.2 Gateway final pre-mutation preflight

Immediately before mutation (after consume, credentials, session), the gateway **MUST** independently verify at least:

1. Authenticated caller identity  
2. Authoritative grant exists and was successfully consumed for this attempt  
3. Grant was ISSUED at consume time (consume succeeded)  
4. Grant was unexpired and not revoked at consume time  
5. Fingerprint exact  
6. Target exact  
7. Vendor/platform/environment exact  
8. Fencing token current  
9. Lease authority current  
10. **Global production execution enabled**  
11. Target enabled  
12. Target `ACTIVE`  
13. Adapter/profile enabled  
14. Security profile valid  
15. Capability profile current  
16. Credential profile current  
17. **Change-control reference still valid**  
18. Change window still open  
19. **Rate limit permits**  
20. **Blast-radius limits satisfied**  
21. Typed operation exactly CELL / txPower / 1 operation  
22. Credential resolution successful  
23. TLS/security requirements satisfied  
24. Direct vendor state == expected  

Anything **UNKNOWN** ⇒ **DENY / ZERO MUTATION**.

---

# 19. Direct Vendor Expected-State Check

Canonical SNIP state **MUST NOT** be the sole final expected-state authority for a real production write.

Immediately before mutation, the write gateway/adapter **MUST** directly observe the approved vendor target and require `actual == expectedValue`.

Mismatch, unknown, stale, unavailable, or timeout ⇒ **ZERO mutation**.

---

# 20. Expected-State Guard Strength

| Strength | Meaning |
|---|---|
| `ATOMIC` | Vendor protocol provides compare-and-set / conditional apply |
| `READ_THEN_WRITE` | Observe then mutate without atomic CAS |

- If protocol supports CAS ⇒ use `ATOMIC`.
- If only read-then-write is possible ⇒ architecture **MUST** acknowledge residual **TOCTOU** race.
- Production policy decides whether `READ_THEN_WRITE` is permitted for a target.
- **MUST NOT** falsely claim atomicity when the vendor protocol does not provide it.
- While Ericsson production write protocol remains UNRESOLVED, guard strength for production is **policy-gated and fail-closed** until evidence exists.

---

# 21. PRE-SEND Boundary and Mutation Outcome Model

## 21.1 Strict PRE-SEND definition

**PRE-SEND** means: the gateway has **not** invoked any vendor transport operation capable of causing mutation **and** has positive proof that no mutation request has left SNIP.

Retries **MAY** be allowed only in PRE-SEND (including retry of same ISSUED grant delivery before consume).

Once mutation-capable transport invocation begins, or transmission status is uncertain ⇒ treat as **MAY_HAVE_SENT**.

**MAY_HAVE_SENT** ⇒ **NO** automatic mutation retry — even if a network exception appears transient.

## 21.2 Mutation outcomes

| Outcome | Meaning |
|---|---|
| `NOT_SENT` | Mutation not transmitted (still PRE-SEND) |
| `REJECTED` | Vendor/explicit rejection before/at accept boundary |
| `VENDOR_ACCEPTED` | Vendor accepted mutation request |
| `OUTCOME_UNKNOWN` | Mutation may have been sent; result uncertain |

Vendor acceptance is **NOT** verification. HTTP 200 **MUST NOT** mean `VERIFIED`.

---

# 22. Production Verification

After mutation, perform independent vendor readback (not mutation response replay).

Outcomes: `VERIFIED`, `MISMATCH`, `UNKNOWN`, `TIMEOUT`, `SOURCE_UNAVAILABLE`, `STALE_OBSERVATION`.

Only fresh direct observation matching desired state may produce `VERIFIED`.

Keep distinct: `PRODUCTION_VERIFIED` vs `CANONICAL_RECONCILED` (Phase 12).

Verification result **MUST** be durably persisted before the application may treat the change as `VERIFIED`.

If verification succeeded externally but persistence failed: recover by fresh readback; **do not** resend mutation.

---

# 23. Phase 12 Reconciliation Boundary

Phase 16 **MUST NOT** directly mutate canonical `radio_configuration` (or equivalent).

After successful production verification it **MAY** record or emit `NETWORK_SYNCHRONIZATION_REQUIRED`.

Phase 12 remains authoritative for normal read/reconciliation into canonical knowledge.

---

# 24. Ambiguous Production Outcome

If mutation may have been sent but result is uncertain:

1. State: `PRODUCTION_OUTCOME_UNKNOWN`
2. Independent vendor observation
3. actual == desired ⇒ `VERIFIED`
4. actual == expected ⇒ **NO automatic retry**; safe stop; new separately authorized execution required
5. third value ⇒ `MANUAL_INTERVENTION_REQUIRED`
6. observation unavailable ⇒ `PRODUCTION_OUTCOME_UNRESOLVED`; **NO retry**

No blind mutation retry. Ambiguous outcome **MUST NOT** automatically issue another grant.

---

# 25. Recovery and Rollback

## 25.1 Verification failure

⇒ `RECOVERY_REQUIRED`, **not** automatic rollback.

## 25.2 Governed rollback chain

Requires: rollback request, review, authorization, rollback fingerprint, new single-use production execution grant, expected-state observation, rollback mutation, independent rollback verification.

## 25.3 Rollback value provenance

Rollback value **MUST** come only from persisted Phase 14 rollback state (via Phase 15 lineage). No runtime recomputation, agent-calculated rollback, or caller-supplied rollback value.

## 25.4 Rollback outcome unknown

`ROLLBACK_OUTCOME_UNKNOWN` → independent readback; restored ⇒ `ROLLED_BACK`; still pre-rollback ⇒ no automatic retry; third state ⇒ manual intervention; unavailable ⇒ safe unresolved. No blind rollback retry.

## 25.5 Emergency rollback

Future names `STANDARD_ROLLBACK` / `EMERGENCY_ROLLBACK` **MAY** exist, but both remain governed in Phase 16. **No** emergency governance bypass.

---

# 26. Cancellation and Windows

- Before mutation: **MAY** become `CANCELLED_BEFORE_MUTATION`.
- After mutation may have been transmitted: **MUST NOT** falsely claim cancelled; verification/outcome determination continues.
- Window permits execution during interval; opening **MUST NOT** trigger execution.
- Authorization before window does not permit execution after expiry.
- Both application pre-grant and gateway final pre-mutation preflights **MUST** re-check window.
- **No** `@Scheduled` production execution; **no** event-driven production execution.

---

# 27. Lifecycle and Execution Ownership

## 27.1 Lifecycle states (no generic SUCCESS)

```text
REQUESTED
ADMISSION_CHECKING
ADMISSION_REJECTED
READY_FOR_REVIEW
REVIEWED
READY_FOR_AUTHORIZATION
AUTHORIZED
WAITING_FOR_WINDOW
ACQUIRING_LEASE
FINAL_PREFLIGHT
READY_TO_EXECUTE
EXECUTING
VENDOR_ACCEPTED
PRODUCTION_OUTCOME_UNKNOWN
VERIFYING
VERIFIED
VERIFICATION_FAILED
RECOVERY_REQUIRED
ROLLBACK_REQUESTED
ROLLBACK_REVIEWED
ROLLBACK_AUTHORIZED
ROLLING_BACK
ROLLBACK_OUTCOME_UNKNOWN
ROLLED_BACK
ROLLBACK_FAILED
PRODUCTION_OUTCOME_UNRESOLVED
MANUAL_INTERVENTION_REQUIRED
CANCELLED_BEFORE_MUTATION
EXPIRED
INVALIDATED
```

## 27.2 State ownership

**SNIP APPLICATION OWNS:**

`REQUESTED`, `ADMISSION_CHECKING`, `ADMISSION_REJECTED`, `READY_FOR_REVIEW`, `REVIEWED`, `READY_FOR_AUTHORIZATION`, `AUTHORIZED`, `WAITING_FOR_WINDOW`, `ACQUIRING_LEASE`

**SHARED DURABLE EXECUTION PROTOCOL OWNS** (persisted via defined durable transitions; not HTTP-only):

`FINAL_PREFLIGHT`, `READY_TO_EXECUTE`, `EXECUTING`, `VENDOR_ACCEPTED`, `PRODUCTION_OUTCOME_UNKNOWN`, `VERIFYING`, `VERIFIED`, `VERIFICATION_FAILED`, `RECOVERY_REQUIRED`, `PRODUCTION_OUTCOME_UNRESOLVED`, `MANUAL_INTERVENTION_REQUIRED`

Rollback **governance** remains application-owned until rollback execution crosses the gateway boundary. Rollback execution states follow the same durable protocol rules as forward execution.

## 27.3 Evidence authority

The **WRITE GATEWAY** is the authoritative **producer** of:

- vendor-send evidence
- vendor acceptance evidence
- direct observation evidence
- verification evidence
- rollback vendor evidence

Lifecycle state **MUST** be written through a defined durable state transition protocol, not only returned transiently over HTTP.

The application **MUST NOT** fabricate `VENDOR_ACCEPTED`, `VERIFIED`, or `ROLLED_BACK` without gateway-produced durable evidence.

---

# 28. Durable Gateway Handoff Protocol

## 28.1 Concepts

Introduce durable:

- `ProductionGatewayAttempt`
- `ProductionGatewayEvidence`

(or equivalent named constructs).

## 28.2 Before external mutation

Persist durable attempt record with at least: `attemptId`, `grantId`, `productionChangeId`, `targetId`, fingerprint, fencingToken, operation binding, status = `PREPARED` / `CONSUMED`, timestamps.

## 28.3 After significant gateway outcomes

Persist durable outcome **before or independently of** transient HTTP response.

HTTP response is convenience. **Durable evidence is authority.**

---

# 29. Rate Limits, Blast Radius, and Kill Switch

## 29.1 `ProductionBlastRadiusPolicy`

Hard initial limits: `maxCellsPerExecution=1`, `maxParametersPerExecution=1`, `maxOperationsPerExecution=1`.

Also: `maxChangesPerTargetPerHour`, `maxChangesPerCellPerDay`, `maxOutcomeUnknownBeforeSuspend`, `maxVerificationFailuresBeforeSuspend` with non-permissive defaults.

## 29.2 Gateway / shared durable rate enforcement

Rate/blast-radius authority **MUST NOT** rely only on the application.

Gateway **or** a shared durable enforcement service **MUST** enforce the hard 1/1/1 limits and the production rate / suspension counters.

Enforcement **MUST** use durable/shared counters (or equivalent) so multiple gateway replicas cannot independently exceed the limit.

Unknown limiter state ⇒ **DENY**.

## 29.3 Automatic safety suspension

Automatic execution forbidden. Automatic **safety suspension** allowed. Automatic re-enable **FORBIDDEN**.

## 29.4 Global kill switch — gateway enforcement

Mutation requires all applicable gates, including:

```text
global execution enabled
AND target enabled
AND target ACTIVE
AND adapter/profile enabled
AND security profile valid
AND change policy permits
AND authorization valid
AND grant validly consumed for this attempt
AND rate limits permit
AND ...
```

No single boolean may enable production writes by itself.

The gateway **MUST** independently enforce immediately before mutation:

- global production execution enabled
- target enabled
- target `ACTIVE`
- adapter/profile enabled
- security policy permits

Application checks are necessary but **insufficient**.

If global kill switch changes after grant issuance but before vendor send ⇒ gateway **MUST deny**.

If target becomes `SUSPENDED`/`DISABLED` after grant issuance but before vendor send ⇒ gateway **MUST deny**.

Grant possession **never** overrides kill/suspension state.

---

# 30. Write Gateway Responsibilities

The Production Write Gateway **MUST**:

1. Authenticate caller/workload  
2. Load authoritative grant by grantId  
3. Atomically consume grant (ISSUED→CONSUMED) under conditional checks  
4. Persist durable attempt  
5. Validate fingerprint, target, fencing, lease  
6. Independently enforce global kill switch and target ACTIVE/enabled  
7. Independently revalidate change-control  
8. Independently enforce rate/blast-radius limits  
9. Validate operation scope (1 cell / 1 param / txPower)  
10. Resolve write credentials late  
11. Establish vendor session  
12. Perform gateway final pre-mutation preflight  
13. Perform direct vendor expected-state observation  
14. Apply only typed authorized mutation  
15. Persist mutation outcome durably  
16. Perform independent vendor readback  
17. Persist verification evidence durably  
18. Return sanitized structured evidence  
19. Destroy/session-release credentials after use  

The gateway **MUST NOT** accept arbitrary vendor commands or expose a generic command endpoint.

---

# 31. TLS / mTLS

Production vendor communication: TLS mandatory; hostname verification mandatory; approved trust store mandatory; trust-all forbidden.

If mTLS required: certificate/private key via approved secure credential mechanism only. No repository certificate/private-key material. Certificate expiry / untrusted issuer / hostname mismatch ⇒ fail closed.

---

# 32. API Surface

```text
POST /api/v1/production-changes
GET  /api/v1/production-changes
GET  /api/v1/production-changes/{id}

POST /api/v1/production-changes/{id}/review
POST /api/v1/production-changes/{id}/authorize
POST /api/v1/production-changes/{id}/execute

GET  /api/v1/production-changes/{id}/evidence

POST /api/v1/production-changes/{id}/rollback/request
POST /api/v1/production-changes/{id}/rollback/review
POST /api/v1/production-changes/{id}/rollback/authorize
POST /api/v1/production-changes/{id}/rollback/execute

POST /api/v1/production-targets/{id}/suspend
POST /api/v1/production-targets/{id}/resume
```

Create DTO: `phase15ExecutionId`, `productionTargetId`, `changeControlReference` only.

Only explicit human-authorized `execute` / `rollback/execute` may cross the production mutation boundary (via gateway + durable grant consume).

---

# 33. Agent Boundary

Agents **MAY**: explain, summarize, identify risk, recommend human consideration, summarize execution evidence.

Agents **MUST NOT**: request execution automatically; review; authorize; execute; choose production target; choose credential; change expected/desired/rollback values; retry; rollback; extend window; resume suspended target.

Chief Orchestration Agent gets **no exception**.

---

# 34. MCP Boundary

Read-only MCP **MAY eventually** expose: `get_production_change`, `get_production_change_status`, `get_execution_evidence`, `explain_production_change`.

Forbidden in Phase 16: `execute_production_change`, `apply_parameter`, `set_tx_power`, `run_vendor_command`, `rollback_production_change`, `authorize_production_change`, `resume_production_target`.

**No MCP mutation tool in Phase 16.**

---

# 35. Audit and Tamper Evidence

## 35.1 Durable audit (minimum)

Cover actors by `actorPrincipalId` (+ non-authoritative display), fingerprints, target/cell/parameter/expected/desired, change-control, window, lease/fencing, credential profile ID (not credential), profiles, preflight, mutation attempt/outcome, verification, rollback chain, reason codes, timestamps.

No secrets. No raw vendor payload by default.

## 35.2 Tamper-evident chain semantics

**Chain scope (normative recommendation):** one ordered chain per `productionChangeId` (or another deterministic documented scope fixed in implementation specification).

```text
genesis.previousEventHash = fixed canonical genesis value

eventHash = SHA-256(previousEventHash + canonicalSerializedAuditEvent)
```

Canonical serialization **MUST** be deterministic.

Concurrent writes **MUST** be serialized within the chain scope.

A gap/hash mismatch during verification:

- marks audit integrity **INVALID**
- raises security/audit alert
- **blocks new production mutation** for the affected change/target as policy requires

Audit-chain verification failure **MUST NOT** silently rewrite history.

Critical audit persistence is required before mutation authority crosses the gateway boundary.

If external mutation may already have occurred and audit subsystem fails afterward: **MUST NOT** lose mutation outcome by rolling back the DB transaction. Persist critical mutation evidence independently and raise audit integrity failure.

Hash chaining provides **tamper evidence**, not absolute immutability.

---

# 36. Production Execution Evidence Package

Safe `ProductionExecutionEvidence` includes: governance evidence, fingerprints, target identity, preflight decisions, direct expected-state observation, vendor mutation outcome, verification evidence, recovery evidence, rollback evidence, audit-chain verification, durable attempt references.

No credentials. No raw secret-bearing vendor payloads.

---

# 37. Persistence Proposal (architecture only — V17 NOT CREATED)

Propose future migration **V17** (do not create now):

Possible entities/tables: `production_network_target`, `production_change_request`, `production_change_review`, `production_change_authorization`, `production_execution_grant`, `production_gateway_attempt`, `production_gateway_evidence`, `production_execution_verification`, `production_execution_recovery`, `production_execution_rollback`, `production_execution_lease`, `production_target_health`, `production_change_audit_event`, rate-limit counter tables as needed.

Exact schema refined during implementation specification. V1–V16 remain unchanged by Phase 16 architecture authoring.

---

# 38. Transaction Durability and Vendor I/O Ordering

## 38.1 Critical outcome survival

Critical production outcome evidence **MUST** survive outer orchestration failure via independent transaction persistence for: `PRODUCTION_OUTCOME_UNKNOWN`, `VERIFICATION_FAILED`, `RECOVERY_REQUIRED`, `PRODUCTION_OUTCOME_UNRESOLVED`, `ROLLBACK_FAILED`, `ROLLBACK_OUTCOME_UNKNOWN`, `MANUAL_INTERVENTION_REQUIRED`, target suspension, and consumed-grant pre-send unresolved attempts.

Do **not** claim distributed ACID with vendor systems.

## 38.2 Persistence ordering around vendor I/O

Before vendor send, durable state **MUST** establish: grant consumed; attempt identity; exact mutation binding; current fencing token; send eligibility.

Then vendor send occurs.

After send:

- Definitive vendor rejection before apply ⇒ persist `REJECTED`.
- Vendor acceptance known ⇒ persist `VENDOR_ACCEPTED` before returning success to app where possible.
- Communication breaks after send may have occurred ⇒ persist `PRODUCTION_OUTCOME_UNKNOWN` using independent durable transaction.
- Gateway crashes and cannot persist outcome ⇒ recovery **MUST** inspect durable PREPARED/CONSUMED attempt and classify as potentially ambiguous rather than automatically retrying.

No `CONSUMED` attempt may cause automatic second mutation.

Apply equivalent rules to rollback.

---

# 39. Distributed Failure Table (normative)

| Scenario | Grant state | Attempt state | Auto retry mutation? | Readback? | Safe lifecycle | Human action? |
|---|---|---|---|---|---|---|
| App DB commit before gateway call | ISSUED | none/PREPARED | Delivery retry of same grantId only | No | READY_TO_EXECUTE / waiting gateway | No |
| Gateway receives before grant consume | ISSUED | none | Delivery/consume retry while ISSUED | No | still pre-send | No |
| Gateway crash before consume | ISSUED | none or failed prepare | Same grantId delivery OK | No | pre-send | No |
| Gateway crash after consume before vendor send | CONSUMED | CONSUMED/PREPARED | **NO** | Optional confirm no change | pre-send-consumed unresolved | Yes / governed recovery |
| Gateway crash during/after vendor send | CONSUMED | ambiguous | **NO** | **YES** | PRODUCTION_OUTCOME_UNKNOWN | Yes if unresolved |
| Vendor applies but response lost | CONSUMED | may lack accept | **NO** | **YES** | UNKNOWN→VERIFIED/expected/third | Yes if third/unresolved |
| Gateway verifies but response to app lost | CONSUMED | VERIFIED durable | **NO** (read durable) | If needed | VERIFIED | No if durable present |
| Gateway verifies but durable verification persistence fails | CONSUMED | incomplete | **NO** | **YES** fresh | recover to VERIFIED or unresolved | Yes if cannot persist |
| App crashes before gateway call | ISSUED | none | Same grantId delivery | No | pre-send | No |
| App crashes while gateway runs | per gateway | per gateway | **NO** second send | As needed | reconstruct from durable | Maybe |
| App crashes after gateway response | CONSUMED | durable outcome | **NO** | If reconstructing | durable outcome | No if durable |
| Rollback equivalents | same matrix | same rules | **NO** blind rollback retry | As needed | rollback unknown/resolved | As needed |

**No row authorizes blind second mutation.**

---

# 40. Retry Policy

- No automatic forward mutation retry after MAY_HAVE_SENT.
- No automatic rollback retry after rollback MAY_HAVE_SENT.
- Bounded retry **MAY** be allowed only in strict PRE-SEND (credential resolution before send, DNS/connectivity before send, direct read-only observation, same ISSUED grant delivery before consume).
- Architecture **MUST** distinguish PRE-SEND from MAY_HAVE_SENT / outcome-ambiguous failure.

---

# 41. Failure Taxonomy (minimum)

`ADMISSION_FAILURE`, `AUTHORIZATION_FAILURE`, `LEASE_FAILURE`, `PREFLIGHT_FAILURE`, `CHANGE_CONTROL_FAILURE`, `CREDENTIAL_RESOLUTION_FAILURE`, `TLS_FAILURE`, `VENDOR_CONNECTION_FAILURE`, `PRE_EXECUTION_TRANSIENT_FAILURE`, `VENDOR_REJECTION`, `MUTATION_OUTCOME_UNKNOWN`, `VERIFICATION_MISMATCH`, `VERIFICATION_TIMEOUT`, `VERIFICATION_SOURCE_UNAVAILABLE`, `ROLLBACK_REJECTION`, `ROLLBACK_OUTCOME_UNKNOWN`, `SECURITY_POLICY_FAILURE`, `TARGET_SUSPENDED`, `GRANT_CONSUME_CONFLICT`, `RATE_LIMIT_EXCEEDED`, `KILL_SWITCH_DENY`, `MANUAL_INTERVENTION_REQUIRED`.

No generic catch-and-retry semantics.

---

# 42. Observability

Low-cardinality metrics examples: `production_change_requests_total`, `production_change_authorizations_total`, `production_execution_attempts_total`, `production_execution_verified_total`, `production_execution_outcome_unknown_total`, `production_verification_failures_total`, `production_rollbacks_total`, `production_manual_intervention_total`, `production_target_suspensions_total`, `production_grant_consume_conflicts_total`.

Allowed labels: `vendor`, `platform`, `environment`, `result`, `reasonCategory`.

Forbidden high-cardinality labels: `cellId`, `executionId`, `planId`, `userId`, `ticket`, `fingerprint`, `endpoint`.

Production target health is **separate** from application readiness.

---

# 43. Threat Model

| # | Threat | Attack path | Impact | Preventive | Detective | Fail-safe | Residual |
|---|---|---|---|---|---|---|---|
| T01 | Stolen operator session | Compromised human session | Unauthorized request/review | SoD via principalId; short sessions | Audit actor chain | Deny without full chain | Insider multi-role |
| T02 | Malicious requester | Crafted create | Invalid request | Field rejection; admission | Audit | Admission deny | Social engineering |
| T03 | Malicious reviewer | Approve bad request | Progress toward auth | Review≠auth | Audit | Auth still required | Collusion |
| T04 | Malicious authorizer | Authorize harmful change | Near-execution | Authorizer≠executor | Audit | Execute gated | Collusion |
| T05 | Compromised agent | Agent tries execute | Unauthorized mutation | No agent perms | Audit denials | Deny | Prompt noise |
| T06 | Compromised MCP client | Mutation tool call | Unauthorized mutation | No MCP mutation tools | Absence tests | Deny | Tool creep |
| T07 | Replayed authorization | Replay auth | Stale auth use | Fingerprint generation; STALE | Audit | Deny stale | Clock skew |
| T08 | Replayed execution grant | Replay grantId after consume | Duplicate mutation attempt | Atomic consume; durable status | Consume conflict metrics | Deny consumed | Race before durable commit (mitigated by CAS) |
| T09 | Stale authorization | Bindings changed | Wrong mutation | Fingerprint completeness | Preflight | Deny | Operator ignore STALE |
| T10 | Target substitution | Swap targetId | Wrong network | Fingerprint binds target | Gateway target check | Deny | Registry compromise |
| T11 | Environment substitution | LAB↔PROD swap | Prod hit via lab path | Fingerprint env; registry | Profile checks | Deny | Mis-registration |
| T12 | Adapter substitution | Weaker adapter | Unsafe protocol | Profile binding | Profile audit | Deny | Admin error |
| T13 | Capability downgrade | Older capability | Unsafe ops | Capability version bind | Preflight | Deny | Admin downgrade |
| T14 | Credential exfiltration | App reads write secret | Direct vendor write | Separate WI; KV ACL | KV access logs | App cannot resolve | Gateway compromise |
| T15 | Secret logging | Exception/log leak | Credential exposure | Sanitization | Log scanners | Redact/deny | Memory dump |
| T16 | Vendor endpoint spoofing | Fake ENM | MITM mutation | TLS+hostname; egress | TLS failures | Fail closed | Compromised CA |
| T17 | TLS downgrade | Force insecure | Interception | TLS mandatory | Policy checks | Deny | Misconfig |
| T18 | DNS manipulation | Redirect ENM | Wrong target | Egress allowlist | Conn failures | Deny | FQDN-cache limits |
| T19 | Stale expected state | Old SNIP truth | Wrong apply | Direct vendor observe | Observation audit | Zero mutation | TOCTOU |
| T20 | TOCTOU race | Change between read/write | Unexpected state | Prefer ATOMIC; policy on R-T-W | Post verify | Recovery | Honest residual race |
| T21 | Concurrent mutation | Two writers | Conflicting change | Lease scope | Lease conflict metrics | Deny loser | External non-SNIP writer |
| T22 | Ambiguous vendor response | Timeout after send | Unknown state | OUTCOME_UNKNOWN; readback | Metrics | No blind retry | Vendor lies |
| T23 | Replay after timeout | Retry blindly | Double mutation | MAY_HAVE_SENT rule | Outcome taxonomy | Safe stop | Operator manual duplicate |
| T24 | Forged verification | Fake VERIFIED | False safety | Gateway durable evidence | Evidence package | Require fresh observe | Compromised gateway |
| T25 | Rollback manipulation | Caller rollback value | Wrong restore | Persisted Phase14 only | Fingerprint | Deny caller value | Bad persisted plan |
| T26 | Change-window extension | Stretch window | Off-hours change | Fingerprint window; gateway recheck | Preflight | Deny expired | Clock attack |
| T27 | Ticket substitution | Swap change-control | Bypass change mgmt | Fingerprint; gateway revalidation | CC validation | Deny invalid | Manual abuse |
| T28 | Configuration weakening | Disable gates | Broad writes | Multi-gate | Config audit | Deny-by-default | Privileged admin |
| T29 | Event-triggered execution | Kafka/app event | Auto mutation | Explicit prohibition | Structural absence | N/A | Feature creep |
| T30 | Scheduler-triggered execution | @Scheduled | Auto mutation | Explicit prohibition | Structural absence | N/A | Feature creep |
| T31 | Excessive mutation rate | Burst changes | Blast radius | Gateway/shared durable rate limits | Rate metrics | Suspend/deny | Threshold tuning |
| T32 | Audit tampering | Edit history | Cover tracks | Hash chain; serialized writes | Chain verify | Invalid + block | Storage rewrite+chain |
| T33 | Compromised SNIP app | Steal app identity | Forge many grants | App cannot hold write WI; gateway consume+limits+kill | WI logs; rate metrics | Gateway still enforces | App issues grants |
| T34 | Compromised write gateway | Direct vendor access | Arbitrary typed writes if profiles weak | Least privilege; typed only; egress; limits | Gateway audit | Still no generic command | Highest residual |
| T35 | Compromised app minting many grants | Rapid grant creation | Rate abuse | SoD; rate limits at gateway/shared durable | Rate metrics; suspensions | Deny/suspend | Threshold tuning |
| T36 | Concurrent grant consumption | Two gateway replicas | Double mutation attempt | Atomic CAS consume | Consume conflicts | One success only | DB isolation bugs |
| T37 | Grant replay race | Consume vs replay | Double send | Consume-before-send | Conflict metrics | Deny loser | Implementation defect |
| T38 | Revoked grant replay | Old payload after revoke | Unauthorized mutation | Durable status at consume | Audit revoke | Deny | Clock/replica lag |
| T39 | Consumed then gateway crash | Crash post-consume | Stuck authority | Crash matrix; no auto reset | Attempt state | Human recovery | Operator error |
| T40 | App/gateway split brain | Divergent state views | False VERIFIED | Durable evidence authority | Evidence missing checks | App cannot fabricate | Ops confusion |
| T41 | Kill switch after grant | Disable after ISSUED | Unwanted mutation | Gateway independent kill check | Deny metrics | Deny | Race mid-send |
| T42 | Target suspended after grant | Suspend after ISSUED | Unwanted mutation | Gateway target ACTIVE check | Deny metrics | Deny | Race mid-send |
| T43 | Rate-limit bypass across replicas | Local counters only | Excess mutations | Shared durable counters | Rate metrics | Deny if unknown | Counter design flaw |
| T44 | Target registry privilege escalation | Executor edits target | Wrong env/creds | ADMINISTER_PRODUCTION_TARGET; SoD | Target change audit | Stale auth/grants | Privileged admin abuse |
| T45 | Display-name identity collision | SoD via display name | Fake SoD | actorPrincipalId only | Audit principal | Deny weak identity | Directory compromise |
| T46 | Audit-chain truncation/gap | Delete/reorder events | Hidden activity | Serialized chain; gap=INVALID | Chain verify alerts | Block new mutation | Storage rewrite |
| T47 | Credential rollback to older secret | Fallback to old version | Use revoked/compromised secret | No old-version fallback | Cred failure metrics | Fail closed | Misconfigured vault |
| T48 | Change ticket invalid after grant | Ticket expires post-ISSUED | Mutation without valid CC | Gateway CC revalidation | CC deny metrics | Zero mutation | Source unavailable policy |

Residual risks that remain accepted until later phases: external non-SNIP writers on ENM; honest TOCTOU under READ_THEN_WRITE; absolute immutability of audit storage; privileged administrator collusion; total gateway compromise.

---

# 44. Production Certification Levels

| Level | Meaning |
|---|---|
| LEVEL 0 | SIMULATOR VERIFIED |
| LEVEL 1 | VENDOR LAB VERIFIED |
| LEVEL 2 | PRE-PRODUCTION VERIFIED |
| LEVEL 3 | PRODUCTION TARGET REGISTERED |
| LEVEL 4 | CONTROLLED PRODUCTION EXECUTION AUTHORIZED |

**Code existence MUST NOT imply LEVEL 4.**

## 44.1 Level 4 transition criteria (explicit)

LEVEL 4 **MUST** require external evidence including at least:

1. approved real production target registration  
2. approved Ericsson write protocol/profile  
3. vendor-lab Level 1 evidence  
4. pre-production Level 2 evidence  
5. production target Level 3 registration evidence  
6. approved write workload identity  
7. approved Key Vault/RBAC  
8. approved target-specific egress policy  
9. TLS/mTLS validation evidence  
10. approved credential profile  
11. approved production change policy  
12. validated rollback path  
13. validated independent readback  
14. validated kill switch  
15. validated target suspension  
16. validated rate/blast-radius enforcement  
17. validated audit/evidence path  
18. operator/requester/reviewer/authorizer/executor governance roles  
19. manual controlled certification sign-off  

No code/config existence alone may move a target to Level 4.

Level 4 authorization is **target/profile-specific**, not global.

## 44.2 First real integration MUST be lab/test

The first real Ericsson integration **MUST** target a vendor-approved lab/test environment.

Production may only be considered after Level 1 and Level 2 evidence.

**MUST NOT** connect the first real transport implementation directly to production.

---

# 45. Default CI Strategy

Default CI **MUST** remain Azure independent, vendor independent, credential independent.

No real vendor target required for ordinary Maven/Go CI.

Real vendor certification uses separate manually triggered, environment-gated workflows.

Actual Level-4 activation is external/operator controlled and **NOT** part of default CI.

---

# 46. Future Implementation Constraints and Infrastructure Scope

## 46.1 IN PHASE16 IMPLEMENTATION (when later authorized)

- SNIP application governance code  
- Production Write Gateway code  
- gateway/app authentication  
- workload identity definitions  
- federated identity configuration  
- Key Vault RBAC required for write identity  
- target-specific Kubernetes/Cilium/NetworkPolicy  
- deployment manifests for gateway  
- default CI changes needed for gateway build/test  
- manual environment-gated certification workflow skeleton  
- Terraform/infrastructure code needed to define the above safely  

If repository infrastructure layout places Terraform/K8s in a separate deployment repository, that separation is allowed **only if** equivalent external artifact evidence is required before Level 4.

## 46.2 Explicitly NOT created/activated by Phase 16 code defaults

- actual production Ericsson credentials: **NOT COMMITTED / NOT CREATED BY CODE**  
- actual production Ericsson endpoint values: environment-controlled / not hardcoded  
- actual production Ericsson write transport implementation: **NOT IMPLEMENTED** until real approved interface known  
- actual Level-4 activation: external/operator controlled  

## 46.3 Other constraints

1. Do not implement production writes in the SNIP app process.  
2. Do not extend `EnmTransport` with writes.  
3. Do not invent Ericsson protocol without evidence; keep production NOT CONFIGURED / fail-closed.  
4. Do not create V17 until implementation specification authorizes it.  
5. Do not start Phase 17 from this document alone.  
6. Mandatory matrix / evidence catalog (~300 items) must be enumerated in the future implementation specification; high-risk runtime behavior cannot be satisfied by structural scans alone.  
7. Preserve Phase 15 immutable baseline `ae9c13d55b444fa50090813495b32b82f97c2ec3`.

---

# 47. Critical Future Test Scenarios (mandatory; catalog later)

Future implementation evidence **MUST** include behavioral/integration scenarios for at least the prior mandatory set (wrong expected, expired auth, wrong target, substitution, expired window, stale fingerprint, invalid CC, lease conflict, credential/TLS failure, vendor reject, timeout before/after send, ambiguous readbacks, grant replay/expiry/wrong fingerprint/target, agent/MCP deny, scheduler/event absence, disabled/suspended/kill/rate deny, app cannot retrieve write credential, canonical isolation) **plus**:

- concurrent grant consume: one success only  
- revoked grant after issuance: deny  
- consume then crash before send: no auto retry  
- kill switch flipped after grant: deny  
- target suspended after grant: deny  
- rate limit across gateway replicas: deny  
- change-control expired after grant: deny  
- display-name collision cannot satisfy SoD  
- old credential version fallback denied  

---

# 48. Mandatory Future Evidence Contract

Architecture sets the future implementation evidence target at approximately **300** mandatory evidence items with strong behavioral/integration weighting.

This document does **not** create the implementation test catalog. Future implementation specification **MUST** enumerate items explicitly. High-risk production mutation gates require behavioral or integration evidence, not documentation-only proof.

---

# 49. Architecture Acceptance Gates

The following gates are mandatory before Phase 16 architecture is accepted. High-risk gates require future behavioral/integration evidence at implementation time.

1. Parent Phase 15 immutable baseline pinned exactly (`ae9c13d…`).  
2. Failed historical Phase 15 candidate preserved (`0cb1223…`); history not rewritten.  
3. Separate Production Write Gateway runtime required.  
4. Ordinary SNIP application process must not implement production vendor writes.  
5. Ordinary SNIP application must not possess vendor-write workload identity.  
6. Write gateway is a security boundary, not merely an in-process service class.  
7. Distinct SNIP application / read / production-write identities.  
8. Production write credentials resolvable only by write-gateway identity.  
9. Phase 10 WI + Key Vault principles reused; no long-lived secret cache.  
10. Phase 11 `EnmTransport` remains read-only; no write methods added.  
11. Separate write-side SPI (`VendorNetworkWriteAdapter` / `EricssonEnmWriteAdapter` / `EricssonWriteTransport`).  
12. Ericsson production write protocol explicitly UNRESOLVED until evidence.  
13. Production Ericsson write transport NOT CONFIGURED and fail-closed by default.  
14. No dual-purpose read/write transport conflation.  
15. No arbitrary / generic vendor command interface.  
16. Typed mutation only (CELL / txPower / expected / desired).  
17. Initial scope hard-limited to Ericsson ENM CELL txPower.  
18. Max cells per execution = 1.  
19. Max parameters per execution = 1.  
20. Max forward mutation operations = 1.  
21. ProductionNetworkTarget model defined with required fields.  
22. Target states include ACTIVE / SUSPENDED / DISABLED.  
23. Target records store credential profile references only.  
24. ProductionNetworkChange aggregate distinct from Phase 15 execution.  
25. Create request accepts only phase15ExecutionId, productionTargetId, changeControlReference.  
26. API rejects caller-controlled mutation fields.  
27. Mutation details derive from governed Phase 14/15 state only.  
28. Phase 13 ≠ 14 ≠ 15 ≠ 16 authorization independence.  
29. Production authorization not inferred from upstream approvals.  
30. Requester must not be Production Authorizer.  
31. Production Authorizer must not be Executor.  
32. Reviewer and Authorizer permissions distinct.  
33. Agents hold no production review/auth/execute/rollback permissions.  
34. MCP holds no production mutation permissions.  
35. Permission set includes VIEW/REQUEST/REVIEW/AUTHORIZE/EXECUTE and rollback counterparts.  
36. Deterministic SHA-256 production fingerprint defined.  
37. Fingerprint binds Phase15/14/target/cell/parameter/values/profiles/window/policies/change-control/auth generation.  
38. Material binding change makes authorization STALE.  
39. No silent fingerprint regeneration or silent reauthorization.  
40. ChangeControlReference required; not itself authorization.  
41. Initial change-control system MANUAL; ServiceNow deferred.  
42. ProductionExecutionGrant distinct from vendor credential.  
43. Grant short-lived, single-use, fingerprint-bound, target-bound, fencing-bound.  
44. Grant statuses include ISSUED / CONSUMED / EXPIRED / REVOKED.  
45. No valid consumable grant ⇒ gateway deny.  
46. Consumed grant not reusable.  
47. Ambiguous outcome does not auto-issue another grant.  
48. Production lease scope = productionTargetId + cellId + parameter.  
49. Lease acquired before grant issuance / application pre-grant completion.  
50. Fencing protects SNIP pre-send authority only; does not cancel external accepted write.  
51. No exact-once external network mutation claim.  
52. No distributed ACID claim with vendor.  
53. Application pre-grant preflight checklist mandatory; UNKNOWN ⇒ DENY (no grant).  
54. Direct vendor expected-state observation mandatory before mutation.  
55. Expected-state mismatch ⇒ zero mutation.  
56. Expected-state unknown/stale/unavailable/timeout ⇒ zero mutation.  
57. ExpectedStateGuardStrength distinguishes ATOMIC vs READ_THEN_WRITE.  
58. No false atomicity claim without protocol support.  
59. Residual TOCTOU acknowledged for READ_THEN_WRITE.  
60. Mutation outcomes distinguish NOT_SENT / REJECTED / VENDOR_ACCEPTED / OUTCOME_UNKNOWN.  
61. Vendor accepted ≠ verified.  
62. Independent post-mutation vendor readback mandatory.  
63. Only fresh observation matching desired may yield VERIFIED.  
64. PRODUCTION_VERIFIED distinct from CANONICAL_RECONCILED.  
65. Phase 16 must not directly mutate canonical radio_configuration.  
66. May emit NETWORK_SYNCHRONIZATION_REQUIRED after verified production change.  
67. Phase 12 remains reconciliation authority.  
68. Ambiguous desired observation path defined (VERIFIED).  
69. Ambiguous expected observation ⇒ safe stop; no blind retry; new auth required.  
70. Ambiguous third-state ⇒ MANUAL_INTERVENTION_REQUIRED.  
71. Unavailable observation ⇒ PRODUCTION_OUTCOME_UNRESOLVED; no retry.  
72. No blind forward mutation retry after MAY_HAVE_SENT.  
73. One forward attempt unless separately authorized new execution.  
74. Verification failure ⇒ RECOVERY_REQUIRED, not automatic rollback.  
75. Automatic rollback prohibited.  
76. Rollback requires request/review/authorize/execute separation.  
77. Rollback fingerprint required.  
78. Rollback expected-state guard mandatory.  
79. Rollback value from persisted Phase 14 state only.  
80. Rollback ambiguous outcome policy defined; no blind rollback retry.  
81. No emergency governance bypass in Phase 16.  
82. Cancellation before mutation only; no false cancel after MAY_HAVE_SENT.  
83. Window permits but does not trigger execution.  
84. Application and gateway preflights re-check window.  
85. No scheduler-driven production execution.  
86. No event-driven production execution.  
87. Creation/authorization/window-open do not execute.  
88. Only explicit execute endpoints may cross mutation boundary.  
89. ProductionBlastRadiusPolicy hard limits = 1/1/1.  
90. Rate / unknown / verification failure suspension thresholds required.  
91. Automatic safety suspension allowed; automatic resume forbidden.  
92. Global kill switch multi-gate; no single boolean enable.  
93. Target kill switch / disable / suspend deny execution.  
94. Credential lifecycle late resolution; secrets never persisted.  
95. Network egress restriction to approved ENM endpoints; no 0.0.0.0/0 write egress.  
96. TLS mandatory; hostname verification; trust-all forbidden.  
97. mTLS private keys only via secure credential mechanism when required.  
98. Durable audit with actorPrincipalId/fingerprint/target/outcome coverage; no secrets.  
99. Tamper-evident previousEventHash / eventHash chain with defined scope.  
100. Critical outcome evidence survives outer failure via independent persistence.  
101. Failure taxonomy non-generic; no catch-and-retry.  
102. Low-cardinality metrics only; forbidden high-cardinality labels.  
103. Target health separate from application readiness.  
104. Agent execution not authorized.  
105. MCP execution not authorized.  
106. Closed-loop optimization not authorized.  
107. Nokia write deferred / not implemented.  
108. Default CI Azure/vendor/credential independent.  
109. Certification levels 0–4 defined; code ≠ Level 4.  
110. V17 proposed only; not created by architecture authoring.  
111. Phase 16 implementation not started by this document.  
112. Future evidence target ~300 items; catalog deferred to implementation specification.  
113. High-risk gates require behavioral/integration evidence, not docs-only.  
114. Threat model covers session theft through gateway compromise and grant/rate/kill races (T01–T48).  
115. Write gateway returns sanitized structured evidence only.  
116. ProductionExecutionEvidence excludes credentials and secret-bearing payloads.  
117. Authoritative durable production grant store required.  
118. Gateway must not trust caller-supplied self-contained grant as sole authority.  
119. App→gateway request carries grantId/correlation only; no mutation payload authority.  
120. Atomic conditional ISSUED→CONSUMED before vendor send.  
121. Concurrent grant consume: exactly one success; others deny.  
122. Consumed grant never automatically resets to ISSUED.  
123. Grant timeout/crash matrix defined and normative.  
124. Consume-before-send ordering mandatory.  
125. Application vs gateway preflight split defined.  
126. Gateway final pre-mutation preflight mandatory and independent.  
127. Gateway independently enforces global kill switch before mutation.  
128. Gateway denies if target suspended/disabled after grant issuance.  
129. Gateway/shared durable rate and blast-radius enforcement required.  
130. Unknown limiter state denies.  
131. Gateway revalidates change-control at consume/pre-send.  
132. Invalid/expired/unknown change-control after grant ⇒ zero mutation.  
133. Execution state ownership (app vs shared durable protocol) defined.  
134. Durable ProductionGatewayAttempt / ProductionGatewayEvidence (or equivalent) required.  
135. Application must not fabricate VENDOR_ACCEPTED / VERIFIED / ROLLED_BACK without durable gateway evidence.  
136. Vendor I/O persistence ordering defined without distributed ACID claim.  
137. Distributed failure table defined; no blind second mutation.  
138. Stable actorPrincipalId required for SoD comparisons.  
139. Display names not authoritative SoD keys.  
140. Target administration via privileged ADMINISTER_PRODUCTION_TARGET (or equivalent).  
141. Execution-significant target change invalidates/stales auth and unconsumed grants.  
142. Write credential current-version resolution; no old-version fallback.  
143. Write credentials never reuse read credentials.  
144. Level-4 explicit external evidence checklist required; target/profile-specific.  
145. First real Ericsson integration MUST be vendor lab/test.  
146. Audit-chain genesis, deterministic serialization, concurrency serialization defined.  
147. Audit gap/mismatch marks INVALID, alerts, and blocks new mutation as policy requires.  
148. Critical mutation evidence survives audit subsystem failure after MAY_HAVE_SENT.  
149. Strict PRE-SEND vs MAY_HAVE_SENT definitions.  
150. Gateway compromise residual risk acknowledged; defense-in-depth required.  
151. Fresh deployment fail-closed to zero production mutation capability.  
152. Phase 16 infra deliverables (gateway, WI, KV RBAC, NetworkPolicy, manifests, CI skeleton, Terraform-or-equivalent) scoped; production secrets/endpoints/transport/Level-4 activation not defaulted by code.  
153. MANUAL change-control validatedByPrincipalId and requester self-validation restriction defined.  
154. HTTP response is convenience; durable evidence is authority.  

**Architecture gate count: 154**

---

# 50. Known Limitations

1. Concrete Ericsson production write protocol unresolved.  
2. Real production execution not yet authorized.  
3. Absolute audit immutability not claimed (tamper evidence only).  
4. External non-SNIP writers on ENM are outside SNIP fencing.  
5. READ_THEN_WRITE retains residual TOCTOU if ATOMIC unavailable.  
6. Exact-once network mutation not claimed.  
7. ServiceNow / ITSM integration deferred.  
8. Nokia NetAct writes deferred.  
9. Human IAM/OIDC production federation may remain limited to existing SNIP patterns until separately authorized.  
10. Cilium FQDN-cache/CIDR lab behaviors remain known networking limitations.  
11. Fully compromised write gateway retains residual risk despite defense-in-depth.

---

# 51. Deferred Capabilities

- Nokia NetAct write adapter  
- Multi-cell / multi-parameter / bulk production changes  
- Generic vendor command execution  
- Automatic rollback / break-glass ungoverned rollback  
- Closed-loop optimization  
- Agent/MCP-triggered production mutation  
- Scheduled or event-driven production mutation  
- ServiceNow (or other ITSM) deep integration  
- Production Level-4 certification evidence  
- Phase 17 and beyond  

---

# 52. Acceptance Criteria (architecture review)

Architecture may be accepted only if reviewers confirm:

1. Separate write gateway runtime is mandatory and non-negotiable.  
2. Ordinary SNIP app has no write-credential identity.  
3. Phase 11 EnmTransport remains read-only.  
4. Ericsson production write transport remains unresolved / fail-closed.  
5. Authoritative durable grant store + atomic consume-before-send are normative.  
6. Application vs gateway preflight split is complete.  
7. Gateway independently enforces kill switch, rate limits, and change-control revalidation.  
8. Execution ownership and durable handoff are defined.  
9. Production authorization is independent and SoD-enforced via actorPrincipalId.  
10. Single-use grants, fingerprints, lease/fencing, direct expected-state, independent verification, and no-blind-retry are normative.  
11. Phase 12 canonical boundary preserved.  
12. Automatic rollback, agent/MCP/scheduler/event execution, and closed-loop optimization remain unauthorized.  
13. Level-4 checklist and lab-first first-integration rule are explicit.  
14. Implementation remains NOT STARTED; V17 not created; architecture not self-declared frozen by this correction alone.  

---

# 53. Implementation Lifecycle (after future acceptance)

```text
architecture review
→ architecture freeze
→ architecture Git baseline (when authorized)
→ implementation specification (when authorized)
→ Cursor implementation (when authorized)
→ simulator / lab certification
→ conformance / evidence (~300)
→ production certification levels progression
→ Level 4 only with external evidence
```

This document does **not** authorize those steps and does **not** declare freeze.

---

# 54. Final Status

```text
PHASE 16 ARCHITECTURE STATUS:
CORRECTED CANDIDATE — READY FOR FINAL ARCHITECTURAL REVIEW

PHASE 16 IMPLEMENTATION:
NOT STARTED

REAL PRODUCTION EXECUTION:
NOT YET AUTHORIZED

ERICSSON PRODUCTION WRITE TRANSPORT:
NOT CONFIGURED

NOKIA PRODUCTION WRITE SUPPORT:
NOT IMPLEMENTED

CLOSED-LOOP OPTIMIZATION:
NOT AUTHORIZED

PARENT PHASE 15 IMPLEMENTATION BASELINE:
ae9c13d55b444fa50090813495b32b82f97c2ec3 (IMMUTABLE)

FAILED HISTORICAL PHASE 15 CANDIDATE:
0cb1223e41ced5462ad552f993e6001a028ddb96 (PRESERVED)

PRE-CORRECTION ARCHITECTURE SHA-256:
fd396f595472739b444a7e5c57971f719a1845488c5fec23c2d5212eedbd1f3f (HISTORICAL)
```

---

*End of Phase 16 architecture document (corrected candidate).*
