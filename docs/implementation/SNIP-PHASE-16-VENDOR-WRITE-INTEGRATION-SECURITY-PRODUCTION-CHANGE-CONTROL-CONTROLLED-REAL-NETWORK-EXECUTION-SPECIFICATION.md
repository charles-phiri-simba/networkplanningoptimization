# SNIP Phase 16 — Vendor Write Integration Security, Production Change Control & Controlled Real-Network Execution — Implementation Specification

## Status

**Architecture:** ARCHITECTURALLY ACCEPTED AND FROZEN  
**Implementation specification:** CORRECTED CANDIDATE — READY FOR FINAL SPECIFICATION REVIEW  
**Implementation baseline:** NOT ESTABLISHED  
**Real production execution:** NOT AUTHORIZED  
**Ericsson production write transport:** NOT CONFIGURED / UNRESOLVED  
**Nokia production write support:** NOT IMPLEMENTED  
**Closed-loop optimization:** NOT AUTHORIZED  
**Phase 17:** NOT STARTED  

**Phase 15 immutable parent:** `ae9c13d55b444fa50090813495b32b82f97c2ec3`  
**Failed historical Phase 15 candidate (preserve; do not rewrite):** `0cb1223e41ced5462ad552f993e6001a028ddb96`  
**Phase 16 architecture baseline:** `8c0791b67ddd9121b1dd5d0abf452c056a8c9a52`  
**Frozen architecture SHA-256:** `dfb4f477e813161843036482d3a6aafc7e19528c91cba1dbdecf2adfb5a5a3b0`  
**Architecture CI:** workflow `ci`, run ID `33501814438`, exact-SHA success.
**Pre-correction specification SHA-256 (historical review evidence):** `b7a68a96ea0aabbae0bd384b934c8ed8232d8ebe1b0fba99001106052ec575d6`

Cursor MUST start from exact HEAD `8c0791b67ddd9121b1dd5d0abf452c056a8c9a52` with a clean tree when implementation begins. If HEAD differs, STOP.

---

## 1. Implementation principle

Phase 16 introduces SNIP’s first **contemplated** real-network mutation capability under deny-by-default, human-gated, scope-bounded, time-bounded, observable, recoverable controls. **Completion of Phase 16 implementation does NOT authorize production mutation.**

Normative rules:

1. **Level 4 certification is mandatory** before any real production network mutation may occur against a registered production target. Code existence, configuration presence, or passing default CI **MUST NOT** imply Level 4 or production authorization.
2. **Zero real network mutation in dev, test, or default CI.** Default Maven/Go CI remains Azure-independent, vendor-independent, and credential-independent. All behavioral proof uses controlled test doubles, simulator/lab transports, or structural/behavioral isolation tests — never live production ENM.
3. Phase 16 implements governance, grant protocol, gateway runtime, audit/evidence, and fail-closed transport stubs only. Production Ericsson write transport remains **NOT CONFIGURED** until separately evidenced and certified.
4. The ordinary SNIP application process **MUST NOT** perform vendor writes or resolve write credentials.
5. Upstream Phase 13/14/15 approvals are **never** sufficient for production mutation. Phase 16 production authorization is an independent human decision.
6. Automatic execution, automatic rollback, agent/MCP-triggered mutation, scheduler/event-driven mutation, and closed-loop optimization remain **NOT AUTHORIZED**.

---

## 2. Package and module structure

Phase 16 **MUST** introduce an explicit Maven multi-module layout. The repository today is a single-module root (`pom.xml` + `src/`). Implementation **MUST** adopt the following **exact** structure (no alternative):

```text
network-planning-optimisation/
├── pom.xml                              # parent aggregator; packaging=pom
├── snip-npo-app/
│   ├── pom.xml                          # child module; artifactId network-planning-optimisation (unchanged for CI continuity)
│   └── src/                             # ALL existing src/ moves here unchanged
│       ├── main/java/...                # Phase 1–15 packages unchanged
│       └── test/java/...
├── production-change-protocol/
│   ├── pom.xml
│   └── src/main/java/com/simba/snip/npo/productionchange/protocol/
└── production-write-gateway/
    ├── pom.xml
    └── src/main/java/com/simba/snip/npo/productionwritegateway/
```

**Migration rule (implementation time only):** move existing root `src/` → `snip-npo-app/src/`; convert root `pom.xml` to parent aggregator listing the three modules; child `snip-npo-app/pom.xml` inherits parent and retains current dependencies/plugins. **Do not** duplicate Phase 1–15 sources. **Do not** leave Phase 16 governance code in the root module.

| Module | Artifact | Role |
|---|---|---|
| `snip-npo-app` | `network-planning-optimisation` | Governance plane; grant issuer; API; no write credentials |
| `production-write-gateway` | `production-write-gateway` | Separate executable JAR; mutation plane |
| `production-change-protocol` | `production-change-protocol` | Shared DTOs/enums; no Key Vault or vendor deps |

### 2.1 SNIP application packages

All Phase 16 governance code belongs under:

```text
com.simba.snip.npo.productionchange
```

Required packages:

```text
api
domain
entity
repository
service
security
policy
audit
metrics
adapter          (gateway client only; no vendor write adapter)
config
exception
```

Do **not** place Phase 16 production-change logic in `changeexecution`, `changeplanning`, `integration`, `agent`, or `mcp`.

### 2.2 Production Write Gateway packages

Gateway code belongs under:

```text
com.simba.snip.npo.productionwritegateway
  ProductionWriteGatewayApplication.java
  api
  service
  security
  adapter
  transport
  audit
  metrics
  config
  exception
```

The gateway **MUST NOT** be registered as a Spring `@Service`, `@Component`, or any other bean inside `snip-npo-app`.

### 2.3 Shared protocol module

`production-change-protocol` contains grant/attempt/evidence DTOs, gateway request/response contracts, shared enums, correlation identifiers. **MUST NOT** depend on Azure SDK, Key Vault, vendor connectors, or write credential resolution.

### 2.4 Build, runtime, deployment, identity, network, and credential boundaries

| Concern | SNIP application (`snip-npo-app`) | Production Write Gateway | Shared protocol |
|---|---|---|---|
| **Build** | Depends on protocol; gateway HTTP client only | Depends on protocol; write adapter SPI | No credential/vendor deps |
| **Runtime process** | Ordinary SNIP JVM | Separate gateway JVM/pod | Library only |
| **Deployment** | `deploy/k8s/deployment.yaml` (existing) | `deploy/k8s/production-write-gateway-deployment.yaml` (new) | N/A |
| **Workload identity** | SNIP application UAMI | Distinct production-write UAMI | N/A |
| **Key Vault access** | Read secrets only; **no write secrets** | Write credential + optional mTLS only | None |
| **Network egress** | App→gateway authenticated plane only | Approved ENM endpoint(s) only | N/A |
| **Credential boundary** | **IMPOSSIBLE BY DESIGN** to resolve write credentials | Late write credential resolution | Never holds secrets |
| **Vendor mutation** | **Forbidden** | Typed mutation via adapter only | N/A |

**MAIN APPLICATION WRITE CREDENTIAL: MUST BE IMPOSSIBLE BY DESIGN.**

## 3. Dependency direction

### 3.1 Permitted dependency chain

```text
Phase 13 proposal governance
  → Phase 14 plan governance
    → Phase 15 execution governance (simulator/sandbox only)
      → Phase 16 production change governance (snip-npo-app)
        → production-change-protocol (shared)
          → gateway client (app module)
            → Production Write Gateway (separate process)
              → VendorNetworkWriteAdapter
                → EricssonEnmWriteAdapter
                  → EricssonWriteTransport (lab/test stub; production NOT CONFIGURED)
```

Phase 16 app services **MAY** read authoritative Phase 13/14/15/12 state through existing services. Phase 16 **MUST NOT** duplicate proposal/plan/execution/knowledge logic when reuse is available.

### 3.2 Forbidden dependency directions

The following **MUST NOT** reach `VendorNetworkWriteAdapter`, `EricssonEnmWriteAdapter`, `EricssonWriteTransport`, or production write credentials:

| Source | Prohibition |
|---|---|
| Agent layer (`agent`, orchestrator, specialists) | No production review/auth/execute/rollback path |
| MCP layer | No mutation tools; no write adapter references |
| Scheduler (`@Scheduled`, import/sync schedulers) | No production execution initiation |
| Event consumers (Kafka, application events) | No production execution side effects |
| Phase 11 `EnmTransport` | Read-only; **no write methods** |
| Ordinary app → write credentials | App **MUST NOT** resolve Key Vault write secrets |
| App → direct vendor write transport | All mutation crosses gateway only |
| Caller HTTP body | Never authoritative for mutation fields |

Structural isolation tests **MUST** prove these forbidden paths are absent at compile time and runtime wiring.

---

## 4. A16-01 — Audit chain (one chain per productionChangeId)

Architecture observation **A16-01** is normative in this specification.

### 4.1 Chain scope

Exactly **ONE** ordered tamper-evident hash chain per `productionChangeId`. All `production_change_audit_event` rows for a given production change **MUST** share that chain scope. Cross-change chaining is **FORBIDDEN**.

### 4.2 Genesis event

The first audit event for a `productionChangeId` uses a fixed canonical genesis predecessor:

```text
genesisPreviousEventHash = SHA-256("SNIP-PHASE16-PRODUCTION-CHANGE-AUDIT-GENESIS-v1")
```

Stored as `previous_event_hash` on the first event. `sequence_number = 1`.

### 4.3 Canonical JSON serialization

Each audit event **MUST** be serialized to canonical JSON before hashing:

1. UTF-8 encoding
2. Object keys sorted lexicographically ascending at every nesting level
3. No insignificant whitespace
4. Null fields omitted (not serialized as `null`)
5. Timestamps as ISO-8601 UTC with millisecond precision (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`)
6. Decimal/numeric values as plain JSON numbers without locale formatting
7. Enum values as stable uppercase string names matching domain enums
8. Arrays preserve governed order only where order is semantically significant (e.g., reason code lists sorted lexicographically)

Canonical payload fields for hashing (minimum):

```text
productionChangeId
eventType
eventVersion
sequenceNumber
occurredAt
actorPrincipalId
reasonCodes (sorted)
safePayload (canonical nested object; no secrets)
```

`eventHash` itself, `previousEventHash` of the current row, and database surrogate keys **MUST NOT** be included in the payload being hashed.

### 4.4 Hash calculation

```text
eventHash = SHA-256( previousEventHash + canonicalSerializedAuditEvent )
```

Where:

- `previousEventHash` is the lowercase hex SHA-256 string (64 chars) of the prior event in the chain, or the genesis hash for sequence 1
- `+` is raw UTF-8 concatenation without delimiter
- Output stored lowercase hex

### 4.5 Concurrency control

Append to a chain **MUST** occur under row-level serialization:

```sql
SELECT ... FROM production_change_audit_event
 WHERE production_change_id = ?
 ORDER BY sequence_number DESC
 LIMIT 1
 FOR UPDATE;
```

Implementation **MAY** lock the parent `production_network_change` row or use a dedicated chain head row, but within-chain appends **MUST** be strictly serialized. Concurrent append attempts **MUST** result in one successor sequence; losers retry or fail closed.

### 4.6 Integrity states

| State | Meaning |
|---|---|
| `VALID` | Chain verifies through latest event |
| `UNVERIFIED` | Not yet verified (initial / lazy) |
| `INVALID` | Gap, hash mismatch, or sequence discontinuity detected |
| `UNAVAILABLE` | Verification could not complete (storage/read failure) |

Verification **MUST** walk from genesis through highest `sequence_number` for the `productionChangeId`.

### 4.7 Gap and mismatch policy

If verification detects:

- missing sequence number
- `previous_event_hash` mismatch
- recomputed `event_hash` mismatch
- out-of-order insert

Then:

1. Mark chain integrity `INVALID` for that `productionChangeId`
2. Emit security/audit alert metric and structured log (no secrets)
3. **Block new production mutation** for the affected production change
4. **Block new grant issuance** for the affected production change until integrity restored by governed human process
5. **MUST NOT** silently rewrite or delete audit history

If external mutation may have already occurred and audit persistence fails afterward, critical mutation evidence **MUST** still be persisted in independent transactions; audit integrity failure is surfaced separately (architecture gate 148).

---

## 5. A16-02 — Distinct grant and attempt states

Architecture observation **A16-02** is normative. Grant state and attempt state **MUST** remain distinct enums, tables, and terminology.

### 5.1 Grant statuses (`production_execution_grant.status`)

| Status | Meaning |
|---|---|
| `ISSUED` | Durable authority; consumable while unexpired and not revoked |
| `CONSUMED` | Atomically consumed by exactly one gateway invocation; **never** reset to `ISSUED` |
| `EXPIRED` | TTL elapsed before consumption |
| `REVOKED` | Explicitly revoked before successful consumption |

Grant statuses **MUST NOT** include attempt lifecycle values (`VERIFIED`, `VENDOR_ACCEPTED`, etc.).

### 5.2 Attempt statuses (`production_gateway_attempt.status`) — canonical enum

Use **exactly** this enum everywhere (no synonyms):

| Status | Meaning |
|---|---|
| `PRE_SEND` | Durable attempt exists; grant is `CONSUMED`; mutation-capable transport **not** yet invoked; positive proof no vendor mutation left SNIP |
| `SEND_ELIGIBLE` | All pre-send checks passed; eligible to invoke mutation-capable transport |
| `MAY_HAVE_SENT` | Mutation-capable transport invoked or transmission uncertain; **no blind retry** |
| `VENDOR_REJECTED` | Vendor explicitly rejected; positive proof no apply ( remains `PRE_SEND` phase) |
| `VENDOR_ACCEPTED` | Vendor accepted apply request (**not** verification) |
| `OUTCOME_UNKNOWN` | Send outcome uncertain |
| `VERIFYING` | Independent readback in progress |
| `VERIFIED` | Independent readback matches desired |
| `VERIFICATION_FAILED` | Readback mismatch/unavailable/stale |
| `RECOVERY_REQUIRED` | Verification/recovery evaluator requires governed recovery |
| `MANUAL_INTERVENTION_REQUIRED` | Safe stop; human remediation |

**Forbidden:** using `CONSUMED` as an attempt status; creating `ProductionGatewayAttempt` while grant remains `ISSUED`.

### 5.3 Pre-consume audit vs attempt row

If the gateway needs a diagnostic record before consume, it **MAY** append a `production_change_audit_event` (e.g., `GATEWAY_GRANT_RECEIVE`) or transient in-memory validation only. It **MUST NOT** create a `production_gateway_attempt` row before successful atomic consume.

### 5.4 Lifecycle mapping (normative)

| Phase | Grant state | Attempt state |
|---|---|---|
| Pre-grant / grant issuance | `ISSUED` | **none** |
| Gateway receives request (pre-consume) | `ISSUED` | **none** (audit only) |
| Atomic consume succeeds | `CONSUMED` | row created → `PRE_SEND` |
| Pre-send checks pass | `CONSUMED` | `SEND_ELIGIBLE` |
| Mutation-capable invoke begins | `CONSUMED` | `MAY_HAVE_SENT` |
| Vendor reject (pre-apply proof) | `CONSUMED` | `VENDOR_REJECTED` (sendPhase may remain `PRE_SEND`) |
| Vendor accept response | `CONSUMED` | `VENDOR_ACCEPTED` |
| Uncertain send | `CONSUMED` | `OUTCOME_UNKNOWN` |
| Verifying | `CONSUMED` | `VERIFYING` |
| Verified | `CONSUMED` | `VERIFIED` |
| Verification failed | `CONSUMED` | `VERIFICATION_FAILED` → may become `RECOVERY_REQUIRED` |
| Grant unused TTL | `EXPIRED` | none |
| Grant revoked unused | `REVOKED` | none |
| Consume without attempt (crash) | `CONSUMED` | none → production change `CONSUMED_PRE_SEND_RECOVERY_REQUIRED` |

## 6. A16-03 — State writer table

Architecture observation **A16-03** is normative. Every durable transition **MUST** have exactly one authoritative writer, explicit transaction boundary, required evidence predecessor, and failure persistence rule.

| Transition | Initiator | Authoritative writer | TX boundary | Required evidence / predecessor | Success successor | Failure persistence |
|---|---|---|---|---|---|---|
| Create production change | Human/API | App: `ProductionChangeService` | App default TX | Phase 15 execution eligible | `REQUESTED` | Admission rejection durable |
| Admission pass/fail | App | App: `ProductionAdmissionService` | App default TX | Upstream validity checks | `READY_FOR_REVIEW` / `ADMISSION_REJECTED` | Rejection reason durable |
| Review | Human | App: `ProductionReviewService` | App default TX | Reviewer SoD | `REVIEWED` | Review denial durable |
| Authorize | Human | App: `ProductionAuthorizationService` | App default TX | Fingerprint + SoD | `AUTHORIZED` | Auth denial durable |
| Acquire lease | App | App: `ProductionLeaseService` | `REQUIRES_NEW` | Authorized + fingerprint current | lease row `ACTIVE` | `LEASE_FAILURE` durable |
| App pre-grant preflight | App | App: `ProductionPreGrantPreflightService` | App default TX | Full checklist §18.1 | preflight audit event | deny; no grant |
| Issue grant | App | App: `ProductionExecutionGrantService` | `REQUIRES_NEW` | Lease + preflight pass | grant `ISSUED` | no grant row |
| Revoke grant | Human/admin | App: `ProductionExecutionGrantService` | `REQUIRES_NEW` | grant `ISSUED` | grant `REVOKED` | audit event |
| Expire grant | System/job | App: `ProductionGrantExpiryService` | `REQUIRES_NEW` | `expiresAt` elapsed | grant `EXPIRED` | audit event |
| Gateway receive request | App executor | Gateway: `GatewayAdmissionService` | Gateway TX (no attempt row) | Authenticated caller; grant `ISSUED` | audit event only | sanitized deny response |
| Atomic consume | Gateway | Gateway: `ProductionGrantConsumeService` | `REQUIRES_NEW` | grant `ISSUED` + full-binding SQL §16.2 | grant `CONSUMED` | consume deny metric; zero mutation |
| Create gateway attempt | Gateway | Gateway: `ProductionGatewayAttemptService` | `REQUIRES_NEW` | consume row count = 1 | attempt `PRE_SEND` inserted | `CONSUMED_PRE_SEND_RECOVERY_REQUIRED` if crash before insert §16.4 |
| Pre-send checks pass | Gateway | Gateway: `ProductionGatewayPreflightService` | Gateway TX | attempt `PRE_SEND` | attempt `SEND_ELIGIBLE` | deny; remain `PRE_SEND` |
| Vendor expected-state observe | Gateway | Gateway: `ExpectedStateObservationService` | Gateway TX | Direct vendor read | observation evidence | zero mutation; `NOT_SENT` |
| Vendor mutation send | Gateway | Gateway: `VendorNetworkWriteAdapter` | **No outer TX** around I/O | expected-state pass | attempt `VENDOR_ACCEPTED`/`MAY_HAVE_SENT`/etc. | `REQUIRES_NEW` outcome |
| Persist mutation outcome | Gateway | Gateway: `ProductionGatewayEvidenceService` | `REQUIRES_NEW` | vendor I/O completed/uncertain | evidence row + attempt update | `REQUIRES_NEW` critical outcome |
| Independent verification | Gateway | Gateway: `ProductionVerificationService` | Gateway TX + `REQUIRES_NEW` persist | mutation evidence | attempt `VERIFIED`/`VERIFICATION_FAILED` | `REQUIRES_NEW` |
| App aggregate → `VENDOR_ACCEPTED` | Gateway evidence sync | App: `ProductionExecutionSyncService` | App TX | **gateway evidence row** | lifecycle `VENDOR_ACCEPTED` | **MUST NOT** without evidence |
| App aggregate → `VERIFIED` | Gateway evidence sync | App: `ProductionExecutionSyncService` | App TX | **gateway verification evidence** | lifecycle `VERIFIED` | **MUST NOT** without evidence |
| App aggregate → `ROLLED_BACK` | Gateway evidence sync | App: `ProductionRollbackSyncService` | App TX | **rollback gateway evidence** | lifecycle `ROLLED_BACK` | **MUST NOT** without evidence |
| Audit append | App/Gateway | App/Gateway audit services | Same TX as triggering transition when possible; else `REQUIRES_NEW` | prior chain head | new audit event | independent audit failure alert |
| Rate limit increment | Gateway/shared | `ProductionRateLimitService` | `REQUIRES_NEW` | shared durable counter | counter updated | deny if unknown |
| Target suspend | Admin | App: `ProductionTargetAdministrationService` | `REQUIRES_NEW` | privileged role | target `SUSPENDED` | audit + stale auth |
| Recovery signaling | App | App: `ProductionRecoveryService` | `REQUIRES_NEW` | verification failed | `RECOVERY_REQUIRED` | durable |

**Hard rule:** The SNIP application **MUST NOT** persist `VENDOR_ACCEPTED`, `VERIFIED`, or `ROLLED_BACK` on the production change aggregate or attempt mirror without a corresponding durable `production_gateway_evidence` row produced by the write gateway.

---

## 7. A16-04 — Grant abuse controls

Architecture observation **A16-04** is normative.

### 7.1 Active grant limits

Per `productionChangeId`:

| Limit | Value |
|---|---|
| Active forward grants (`ISSUED`, unexpired, unconsumed) | **1** |
| Active rollback grants (`ISSUED`, unexpired, unconsumed) | **1** |
| Total concurrent `ISSUED` grants across all changes per target | policy default **10** (configurable) |

Issuance **MUST** be transactional: check limits and insert grant in one TX.

### 7.2 Rate limits (issuance-side)

| Limit | Default |
|---|---|
| Grants issued per target per hour | 6 |
| Grants issued per cell per day | 3 |
| Grants issued per actorPrincipalId per hour | 10 |

Exceeding issuance rate **MUST** deny grant creation (not merely log).

### 7.3 TTL and cleanup

| Parameter | Default |
|---|---|
| Forward grant TTL | 5 minutes |
| Rollback grant TTL | 5 minutes |
| Minimum TTL | 60 seconds |
| Maximum TTL | 15 minutes |

Expired grants transition to `EXPIRED` via expiry sweeper. Expired grants **MUST NOT** be consumable.

### 7.4 Metrics and alerts

Low-cardinality metrics (Section 39):

- `production_grant_issuance_total{result}`
- `production_grant_consume_conflicts_total`
- `production_grant_expired_total`
- `production_grant_revoked_total`
- `production_grant_issuance_denied_total{reasonCategory}`

Alerts **SHOULD** fire on:

- consume conflict rate spike
- issuance rate spike per target
- outcome-unknown count threshold per target (coordination with blast-radius policy)

### 7.5 Transactional issuance pattern

Grant issuance SQL pattern (conceptual):

```sql
-- within REQUIRES_NEW transaction
-- 1. verify no other ISSUED forward grant for production_change_id
-- 2. verify rate-limit counters permit
-- 3. verify lease + fingerprint + authorization current
-- 4. INSERT production_execution_grant ... status = 'ISSUED'
-- 5. increment rate-limit counters
-- 6. append audit event in same chain
```

Concurrent issuers: one succeeds; others receive `GRANT_ISSUANCE_LIMIT_EXCEEDED` or `GRANT_ACTIVE_CONFLICT`.

---

## 8. V17 migration — `V17__phase16_production_change_execution.sql`

**DO NOT CREATE THIS FILE during specification authoring or until implementation explicitly begins migration work authorized by this specification.**

When implemented, create exactly:

```text
src/main/resources/db/migration/V17__phase16_production_change_execution.sql
```

V1–V16 remain unchanged. No secrets, credential values, private keys, connection strings, or raw vendor payloads in any column.

### 8.1 Reconciliation with Phase 15

Phase 15 tables (`network_change_execution*` from V16) remain immutable in semantics. Phase 16 tables reference Phase 15 via `phase15_execution_id` FK or UUID reference with existence check at admission. Phase 16 **MUST NOT** alter Phase 15 columns or lifecycle meaning.

### 8.2 Table definitions (normative minimum columns)

#### `production_network_target`

| Column | Type | Notes |
|---|---|---|
| `target_id` | VARCHAR PK | Stable SNIP id |
| `vendor` | VARCHAR | ERICSSON |
| `platform` | VARCHAR | ENM |
| `environment` | VARCHAR | PRODUCTION/LAB/etc. |
| `region` | VARCHAR | |
| `network_domain` | VARCHAR | |
| `adapter_profile_id` | VARCHAR | |
| `capability_profile_version` | VARCHAR | |
| `security_profile_id` | VARCHAR | |
| `credential_profile_id` | VARCHAR | Reference only |
| `allowed_object_types` | VARCHAR/JSON | CELL |
| `allowed_parameters` | VARCHAR/JSON | txPower |
| `change_window_policy` | VARCHAR/JSON | |
| `rollback_policy` | VARCHAR/JSON | |
| `verification_policy` | VARCHAR/JSON | |
| `certification_level` | VARCHAR | L0–L4 |
| `enabled` | BOOLEAN | |
| `target_state` | VARCHAR | ACTIVE/SUSPENDED/DISABLED |
| `target_fingerprint` | VARCHAR | SHA-256 |
| `created_at` / `updated_at` | TIMESTAMPTZ | |
| `version` | BIGINT | Optimistic lock |

#### `production_network_change`

| Column | Type | Notes |
|---|---|---|
| `production_change_id` | UUID PK | |
| `phase15_execution_id` | UUID FK/ref | |
| `production_target_id` | VARCHAR FK | |
| `change_control_reference` | VARCHAR/JSON | |
| `status` | VARCHAR | Lifecycle enum |
| `production_fingerprint` | VARCHAR | |
| `authorization_generation` | INT | |
| `phase14_plan_id` | UUID | Denormalized binding |
| `phase14_plan_fingerprint` | VARCHAR | |
| `phase15_execution_fingerprint` | VARCHAR | |
| `cell_id` | VARCHAR | From governed state |
| `parameter` | VARCHAR | txPower |
| `expected_value` | NUMERIC | |
| `desired_value` | NUMERIC | |
| `rollback_expected_value` | NUMERIC | |
| `rollback_desired_value` | NUMERIC | |
| `requester_principal_id` | VARCHAR | |
| `reviewer_principal_id` | VARCHAR | |
| `authorizer_principal_id` | VARCHAR | |
| `executor_principal_id` | VARCHAR | |
| `audit_chain_integrity` | VARCHAR | VALID/INVALID/... |
| `created_at` / `updated_at` | TIMESTAMPTZ | |
| `version` | BIGINT | |

#### `production_change_review`

| Column | Type | Notes |
|---|---|---|
| `review_id` | UUID PK | |
| `production_change_id` | UUID FK | |
| `reviewer_principal_id` | VARCHAR | |
| `decision` | VARCHAR | APPROVED/REJECTED |
| `reason_codes` | VARCHAR/JSON | |
| `reviewed_at` | TIMESTAMPTZ | |
| `production_fingerprint_at_review` | VARCHAR | |

#### `production_change_authorization`

| Column | Type | Notes |
|---|---|---|
| `authorization_id` | UUID PK | |
| `production_change_id` | UUID FK | |
| `authorizer_principal_id` | VARCHAR | |
| `authorization_generation` | INT | |
| `production_fingerprint` | VARCHAR | Bound fingerprint |
| `status` | VARCHAR | ACTIVE/STALE/REVOKED |
| `authorized_at` | TIMESTAMPTZ | |
| `expires_at` | TIMESTAMPTZ | Optional |

#### `production_change_control`

| Column | Type | Notes |
|---|---|---|
| `control_id` | UUID PK | |
| `production_change_id` | UUID FK | |
| `system` | VARCHAR | MANUAL |
| `reference` | VARCHAR | Ticket/reference id |
| `status` | VARCHAR | VALID/INVALID/EXPIRED |
| `validated_by_principal_id` | VARCHAR | |
| `validated_at` | TIMESTAMPTZ | |
| `valid_until` | TIMESTAMPTZ | |

#### `production_execution_grant`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `grant_id` | UUID | PK, NOT NULL | Opaque id |
| `production_change_id` | UUID | FK NOT NULL | |
| `phase15_execution_id` | UUID | NOT NULL | Authoritative upstream binding |
| `target_id` | VARCHAR(128) | NOT NULL | |
| `grant_type` | VARCHAR(16) | NOT NULL | `FORWARD` / `ROLLBACK` |
| `status` | VARCHAR(16) | NOT NULL | `ISSUED`/`CONSUMED`/`EXPIRED`/`REVOKED` |
| `production_fingerprint` | CHAR(64) | NOT NULL | SHA-256 hex |
| `authorization_generation` | INT | NOT NULL | |
| `fencing_token` | BIGINT | NOT NULL | |
| `operation_binding_hash` | CHAR(64) | NOT NULL | Hash of governed operation binding |
| `issued_at` | TIMESTAMPTZ | NOT NULL | |
| `expires_at` | TIMESTAMPTZ | NOT NULL | |
| `consumed_at` | TIMESTAMPTZ | NULL | Set on consume |
| `version` | BIGINT | NOT NULL DEFAULT 0 | Optimistic lock / consume CAS |

**Revocation:** `status = 'ISSUED'` excludes `REVOKED`/`EXPIRED`/`CONSUMED`. No separate revoked flag required.

**Indexes / constraints:**

```sql
CREATE INDEX production_execution_grant_change_idx ON production_execution_grant (production_change_id);
CREATE INDEX production_execution_grant_execution_idx ON production_execution_grant (phase15_execution_id);
CREATE INDEX production_execution_grant_expiry_idx ON production_execution_grant (expires_at) WHERE status = 'ISSUED';

CREATE UNIQUE INDEX production_execution_grant_one_active_forward_idx
  ON production_execution_grant (production_change_id, grant_type, operation_binding_hash)
  WHERE status = 'ISSUED' AND grant_type = 'FORWARD';

CREATE UNIQUE INDEX production_execution_grant_one_active_rollback_idx
  ON production_execution_grant (production_change_id, grant_type, operation_binding_hash)
  WHERE status = 'ISSUED' AND grant_type = 'ROLLBACK';
```

No secret columns.

#### `production_gateway_attempt`

| Column | Type | Notes |
|---|---|---|
| `attempt_id` | UUID PK | |
| `grant_id` | UUID FK | |
| `production_change_id` | UUID FK | |
| `status` | VARCHAR | Attempt enum |
| `send_phase` | VARCHAR | PRE_SEND/MAY_HAVE_SENT |
| `mutation_outcome` | VARCHAR | NOT_SENT/REJECTED/... |
| `started_at` / `completed_at` | TIMESTAMPTZ | |
| `gateway_instance_id` | VARCHAR | Low-cardinality pod id allowed |
| `version` | BIGINT | |

#### `production_gateway_evidence`

| Column | Type | Notes |
|---|---|---|
| `evidence_id` | UUID PK | |
| `attempt_id` | UUID FK | |
| `evidence_type` | VARCHAR | MUTATION/VERIFICATION/OBSERVATION/ROLLBACK |
| `evidence_version` | INT | |
| `payload_json` | JSONB | Sanitized; no secrets |
| `produced_at` | TIMESTAMPTZ | |
| `producer` | VARCHAR | WRITE_GATEWAY |

#### `production_execution_verification`

| Column | Type | Notes |
|---|---|---|
| `verification_id` | UUID PK | |
| `production_change_id` | UUID FK | |
| `attempt_id` | UUID FK | |
| `result` | VARCHAR | VERIFIED/MISMATCH/UNKNOWN/TIMEOUT/... |
| `observed_value` | NUMERIC | |
| `desired_value` | NUMERIC | |
| `verified_at` | TIMESTAMPTZ | |

#### `production_execution_recovery`

| Column | Type | Notes |
|---|---|---|
| `recovery_id` | UUID PK | |
| `production_change_id` | UUID FK | |
| `status` | VARCHAR | REQUIRED/IN_PROGRESS/CLOSED |
| `reason_codes` | VARCHAR/JSON | |
| `signaled_at` | TIMESTAMPTZ | |

#### `production_execution_rollback`

| Column | Type | Notes |
|---|---|---|
| `rollback_id` | UUID PK | |
| `production_change_id` | UUID FK | |
| `status` | VARCHAR | Rollback lifecycle |
| `rollback_fingerprint` | VARCHAR | |
| `authorization_generation` | INT | |

#### `production_execution_lease`

| Column | Type | Notes |
|---|---|---|
| `lease_id` | UUID PK | |
| `production_target_id` | VARCHAR | |
| `cell_id` | VARCHAR | |
| `parameter` | VARCHAR | |
| `holder_id` | VARCHAR | production_change_id |
| `fencing_token` | BIGINT | Monotonic |
| `status` | VARCHAR | ACTIVE/RELEASED/EXPIRED |
| `acquired_at` / `expires_at` | TIMESTAMPTZ | |

Unique active constraint on `(production_target_id, cell_id, parameter)` where status = ACTIVE.

#### `production_target_health`

| Column | Type | Notes |
|---|---|---|
| `health_id` | UUID PK | |
| `production_target_id` | VARCHAR FK | |
| `health_state` | VARCHAR | HEALTHY/DEGRADED/UNHEALTHY |
| `outcome_unknown_count` | INT | Rolling |
| `verification_failure_count` | INT | Rolling |
| `last_checked_at` | TIMESTAMPTZ | |

#### `production_change_audit_event`

| Column | Type | Notes |
|---|---|---|
| `audit_event_id` | UUID PK | |
| `production_change_id` | UUID FK | |
| `sequence_number` | BIGINT | Per-chain |
| `event_type` | VARCHAR | |
| `event_version` | INT | |
| `previous_event_hash` | CHAR(64) | |
| `event_hash` | CHAR(64) | |
| `occurred_at` | TIMESTAMPTZ | |
| `actor_principal_id` | VARCHAR | |
| `safe_payload_json` | JSONB | No secrets |
| `chain_integrity` | VARCHAR | Snapshot at write |

#### `production_rate_limit_state`

| Column | Type | Notes |
|---|---|---|
| `counter_id` | VARCHAR PK | Composite key encoded |
| `scope_type` | VARCHAR | TARGET/CELL/ACTOR |
| `scope_key` | VARCHAR | |
| `window_start` | TIMESTAMPTZ | |
| `count` | INT | |
| `updated_at` | TIMESTAMPTZ | |

---

## 9. Production network target model

Implement aggregate `ProductionNetworkTarget` (distinct from Phase 15 `ExecutionTarget`).

Required fields minimum:

```text
targetId
vendor            (initial: ERICSSON)
platform          (initial: ENM)
environment
region
networkDomain
adapterProfileId
capabilityProfileVersion
securityProfileId
credentialProfileId   (reference only — never secret value)
allowedObjectTypes    (initial: [CELL])
allowedParameters     (initial: [txPower])
changeWindowPolicy
rollbackPolicy
verificationPolicy
certificationLevel    (L0–L4)
enabled
targetState           (ACTIVE | SUSPENDED | DISABLED)
targetFingerprint
```

Target records **MUST NOT** store credential values, private keys, trust material, or raw vendor endpoints supplied by API callers.

`ProductionTargetRegistry` loads targets from durable store and/or controlled static configuration. Runtime registration via ordinary executor APIs is **FORBIDDEN**.

---

## 10. Target administration — `ADMINISTER_PRODUCTION_TARGET`

Privileged permission:

```text
ADMINISTER_PRODUCTION_TARGET
```

Required for:

- target create/provision
- target enable/disable
- execution-significant profile binding changes
- credential profile binding changes (reference only)
- capability/security profile changes

Normal production executor permissions **MUST NOT** include this permission.

Execution-significant target changes **MUST**:

1. Be audited with before/after fingerprint
2. Invalidate/stale active production authorizations bound to prior target fingerprint
3. Revoke unconsumed `ISSUED` grants bound to stale fingerprint

API surface (minimum):

```text
POST /api/v1/production-targets/{id}/suspend
POST /api/v1/production-targets/{id}/resume
```

Broad public target CRUD is **NOT REQUIRED**. Provisioning may occur via IaC/seeding in test; production provisioning is external operator controlled.

Resume from `SUSPENDED` requires explicit authorized human action. Automatic resume is **FORBIDDEN**.

---

## 11. Production change request

`POST /api/v1/production-changes` accepts **only**:

```json
{
  "phase15ExecutionId": "...",
  "productionTargetId": "...",
  "changeControlReference": {
    "system": "MANUAL",
    "reference": "...",
    "status": "...",
    "validatedByPrincipalId": "...",
    "validatedAt": "...",
    "validUntil": "..."
  }
}
```

Rejected if caller supplies: `cellId`, `parameter`, `expectedValue`, `desiredValue`, `rollbackValue`, `vendorCommand`, `endpoint`, credentials, fingerprint override, authorization flags, or execution directives.

All mutation semantics **MUST** derive from governed Phase 14/15 durable state at admission time and be revalidated at each gate.

Creation **MUST NOT** execute, authorize, open windows, or issue grants.

---

## 12. Production fingerprint

Algorithm: deterministic **SHA-256** over canonical UTF-8 JSON with lexicographically sorted keys (same rules as audit canonicalization §4.3).

Mandatory bound fields in order for canonical array encoding (sorted keys apply; list shown for human review):

```text
adapterProfileId
authorizationGeneration
capabilityProfileVersion
cellId
changeControlReference
changeWindowEnd
changeWindowId
changeWindowStart
credentialProfileId
desiredValue
environment
expectedValue
parameter
phase14PlanFingerprint
phase14PlanId
phase14PlanVersion
phase15ExecutionFingerprint
phase15ExecutionId
platform
productionPolicyVersion
productionTargetId
rollbackDesiredValue
rollbackExpectedValue
rollbackPolicyVersion
securityProfileId
vendor
verificationPolicyVersion
```

Rules:

1. Any material binding change **MUST** mark authorization `STALE`
2. No silent regeneration
3. No silent reauthorization
4. Grant issuance binds current fingerprint
5. Gateway consume validates exact match

Implement `ProductionFingerprintService` with golden-vector tests.

---

## 13. Change control — `MANUAL`

Initial change-control system is **`MANUAL`** only. ServiceNow/ITSM integration is **DEFERRED**.

`ChangeControlReference` minimum:

| Field | Requirement |
|---|---|
| `system` | `MANUAL` |
| `reference` | External/manual ticket id |
| `status` | Validated status |
| `validatedByPrincipalId` | Stable principal |
| `validatedAt` | Timestamp |
| `validUntil` | Expiry |

Rules:

1. Valid change-control reference **REQUIRED** for authorization progression
2. Change-control validity is **NOT** production authorization
3. Requester **MUST NOT** self-validate when policy requires separation
4. Ticket substitution invalidates fingerprint/authorization
5. Gateway **MUST** revalidate at consume/final pre-mutation preflight
6. Expired/invalid/unknown/unavailable ⇒ **ZERO mutation**

---

## 14. Stable actor identity and SoD

All separation-of-duties comparisons **MUST** use immutable `actorPrincipalId` (subject ID / enterprise principal ID / directory object ID).

Display names, email labels, mutable usernames **MUST NOT** be authoritative SoD keys.

Persist per transition:

- `actorPrincipalId` (authoritative)
- display metadata (non-authoritative audit decoration only)

| Rule | Enforcement |
|---|---|
| Requester ≠ Production Authorizer | MUST |
| Production Authorizer ≠ Executor | MUST |
| Reviewer ≠ Authorizer (permissions distinct) | MUST |
| Change-control validator ≠ requester (when policy requires) | MUST |
| Agents hold none of production mutation permissions | MUST |
| MCP holds none of production mutation permissions | MUST |

---

## 15. Production authorization

Independent human authorization after review. Permission: `AUTHORIZE_PRODUCTION_CHANGE`.

Authorization **MUST**:

1. Bind to current `productionFingerprint`
2. Increment or record `authorizationGeneration`
3. Enforce SoD vs requester and future executor
4. **NOT** be inferred from Phase 13/14/15 approvals

Authorization record status values: `ACTIVE`, `STALE`, `REVOKED`.

Stale triggers include: fingerprint change, target profile change, Phase 15 execution invalidation, change-control invalidation, window policy change, upstream plan/execution staleness.

---

## 16. Execution grant with atomic consume SQL

`ProductionExecutionGrant` is durable server-side authority — **not** a bearer token or vendor credential.

### 16.1 Issuance (app)

Issued only after production authorization ACTIVE, lease acquired, application pre-grant preflight pass (§18.1), and grant abuse limits (§7).

### 16.2 Atomic consume SQL (gateway) — full binding predicate (C16-S-01)

The gateway **MUST** perform a single conditional update equivalent to:

```sql
UPDATE production_execution_grant
   SET status = 'CONSUMED',
       consumed_at = :now,
       version = version + 1
 WHERE grant_id = :grantId
   AND status = 'ISSUED'
   AND expires_at > :now
   AND production_change_id = :productionChangeId
   AND phase15_execution_id = :phase15ExecutionId
   AND target_id = :targetId
   AND production_fingerprint = :productionFingerprint
   AND authorization_generation = :authorizationGeneration
   AND fencing_token = :fencingToken
   AND operation_binding_hash = :operationBindingHash
   AND grant_type = :grantType;
```

**Semantics:**

- Exactly **one** row updated ⇒ consume success.
- Zero rows updated ⇒ **DENY**; **ZERO MUTATION**; return sanitized consume-denied reason.
- No fallback lookup may treat zero rows as success.
- No Java `synchronized`, no process-local lock, no in-memory mutex substitutes for this predicate.
- Consumed grants **MUST NEVER** automatically return to `ISSUED`.
- No second consume attempt may recreate the same grant.

Because `status = 'ISSUED'`, rows in `REVOKED`, `EXPIRED`, or `CONSUMED` are excluded.

### 16.3 Consume-before-attempt ordering (C16-S-02)

**Required order:**

```text
authenticate → load grant → validate bindings → atomic ISSUED→CONSUMED
  → INSERT production_gateway_attempt (PRE_SEND)
  → final pre-mutation checks → credential resolution → vendor session
  → direct observation → SEND_ELIGIBLE → mutation invoke → MAY_HAVE_SENT
  → durable outcome → independent verification
```

**Forbidden:** any `production_gateway_attempt` row while grant.status = `ISSUED`.

### 16.4 Post-consume / pre-attempt failure (crash window)

If grant is successfully `CONSUMED` but the gateway crashes **before** `production_gateway_attempt` persistence:

| Rule | Required behavior |
|---|---|
| Grant reset | **FORBIDDEN** — grant remains `CONSUMED` |
| Auto reissue | **FORBIDDEN** |
| Auto vendor retry | **FORBIDDEN** |
| Assume vendor sent | **FORBIDDEN** from absent HTTP alone |
| Assume vendor not sent | Allowed only with positive `PRE_SEND` evidence (no attempt row, no transport invoke) |
| Durable consume evidence | **REQUIRED** — `consumed_at`, grant consume audit event, optional `production_grant_consume_event` |
| Production change state | **MUST** transition to `CONSUMED_PRE_SEND_RECOVERY_REQUIRED` (or equivalent) |
| Replacement grant | Requires **new separately governed** authorization/recovery action |

### 16.5 Atomic consume integration evidence (mandatory)

`ProductionChangeGrantConsumeIT` **MUST** prove (not structural scans):

| Test | Expected |
|---|---|
| Two concurrent gateway instances consume same grant | Exactly one success; one consume-denied; mutation counter = 0 at consume level |
| Wrong target / fingerprint / auth generation / fencing / operation binding / grant type | Zero-row update; deny |
| Expired / revoked / already consumed | Zero-row update; deny |
| End-to-end concurrent execute | Total vendor mutation count ≤ 1 |

## 17. App→gateway protocol

Authenticated internal protocol between SNIP app and write gateway.

### 17.1 Request (minimum)

```json
{
  "grantId": "...",
  "productionChangeId": "...",
  "correlationId": "..."
}
```

Optional: signed envelope as **secondary** transport evidence only — **MUST NOT** replace durable grant lookup.

### 17.2 Forbidden in request

Credentials, vendor commands, cell/parameter/value overrides, endpoint overrides, security-profile overrides, rollback overrides, self-contained grant JWT as sole authority.

### 17.3 Authentication

mTLS between app and gateway **OR** equivalent service identity (Azure AD workload-to-workload, SPIFFE, or approved mutual service auth). Caller identity **MUST** map to an allow-listed SNIP application service principal.

Gateway **MUST** load grant and mutation bindings from PostgreSQL — never from request body alone.

---

## 18. Production Write Gateway responsibilities

The gateway **MUST**:

1. Authenticate caller workload
2. Load authoritative grant by `grantId`
3. Atomically consume grant (`ISSUED`→`CONSUMED`)
4. Persist durable attempt record
5. Validate fingerprint, target, fencing token, lease authority
6. Independently enforce global kill switch and target ACTIVE/enabled
7. Independently revalidate change-control reference
8. Independently enforce rate/blast-radius limits (shared durable counters)
9. Validate operation scope (1 cell / 1 param / txPower)
10. Resolve write credentials **late** (Phase 10)
11. Establish vendor TLS session
12. Perform gateway final pre-mutation preflight (§18.2)
13. Perform direct vendor expected-state observation
14. Apply typed authorized mutation only
15. Persist mutation outcome durably (`REQUIRES_NEW`)
16. Perform independent vendor readback verification
17. Persist verification evidence durably
18. Return sanitized structured evidence to app
19. Destroy/release credential session after use

The gateway **MUST NOT**:

- Accept arbitrary vendor commands
- Expose generic command endpoint
- Trust caller-supplied mutation fields
- Reset consumed grants to ISSUED
- Perform blind HTTP retry after MAY_HAVE_SENT

### 18.1 Application pre-grant preflight (before grant issuance)

Revalidate at minimum:

1. Phase 13 proposal valid  
2. Phase 14 plan valid and fingerprint current  
3. Phase 15 execution eligible and fingerprint current  
4. Phase 16 authorization ACTIVE and fingerprint current  
5. Change-control valid  
6. Change window open  
7. Target registered, enabled, ACTIVE, certification policy satisfied  
8. Vendor/platform/environment unchanged  
9. Profiles current  
10. Phase 12 knowledge confidence acceptable  
11. Synchronization trustworthy; freshness acceptable  
12. No relevant unresolved drift  
13. Target cell exists  
14. Parameter supported (txPower)  
15. Rollback valid  
16. Lease current; fencing token current  
17. Operation count == 1  

Anything `UNKNOWN` ⇒ **DENY** (no grant).

### 18.2 Gateway final pre-mutation preflight (after consume)

Independently revalidate at minimum items in architecture §18.2 including kill switch, target ACTIVE, rate limits, change-control, credential resolution, TLS, direct expected-state match.

Anything `UNKNOWN` ⇒ **ZERO MUTATION**.

---

## 19. Gateway attempt entity

`ProductionGatewayAttempt` durable record minimum:

```text
attemptId
grantId
productionChangeId
productionTargetId
status                  (attempt enum §5.2)
sendPhase               (PRE_SEND | MAY_HAVE_SENT)
mutationOutcome         (NOT_SENT | REJECTED | VENDOR_ACCEPTED | OUTCOME_UNKNOWN)
operationBindingRef     (governed operation identity)
fencingToken
productionFingerprint
gatewayInstanceId
startedAt
completedAt
version
```

Every gateway invocation **MUST** create or continue exactly one attempt row per consume. Retries after MAY_HAVE_SENT **MUST NOT** create a second forward mutation attempt under the same grant.

---

## 20. Execution order orchestration

Normative ordering:

```text
explicit human POST .../execute
→ app validates executor permission + SoD
→ app pre-grant preflight
→ lease verify
→ grant issuance (ISSUED)
→ authenticated gateway call (grantId only)
→ gateway authenticate
→ load grant
→ atomic consume (ISSUED→CONSUMED)
→ persist attempt PRE_SEND
→ credential resolution (late)
→ vendor session
→ gateway final preflight
→ direct expected-state observation
→ typed mutation send (if eligible)
→ persist mutation outcome evidence (REQUIRES_NEW)
→ independent verification readback
→ persist verification evidence (REQUIRES_NEW)
→ return sanitized evidence
→ app sync lifecycle from durable evidence only
```

Rollback execution follows parallel governed chain with rollback grant kind and rollback fingerprint.

---

## 21. PRE_SEND / MAY_HAVE_SENT — no HTTP retry on mutation

### 21.1 PRE_SEND

Gateway has **not** invoked vendor transport capable of mutation **and** has positive proof no mutation request left SNIP.

Allowed retries in PRE_SEND only:

- Same `grantId` delivery while grant remains `ISSUED`
- Consume retry while grant remains `ISSUED`
- Read-only vendor observation retries
- Credential/TLS/DNS connectivity retries **before** mutation-capable invoke

### 21.2 MAY_HAVE_SENT

Mutation-capable transport invoked **or** transmission status uncertain.

**NO** automatic HTTP retry on mutation. **NO** second grant consume for same forward operation. Classification → `OUTCOME_UNKNOWN` / independent readback / human recovery.

---

## 22. Vendor write SPI — `VendorNetworkWriteAdapter`

Implement vendor-neutral SPI:

```java
public interface VendorNetworkWriteAdapter {
    MutationResult applyAuthorizedMutation(
        ProductionExecutionContext context,
        AuthorizedParameterMutation mutation);
}
```

Initial typed mutation:

```text
AuthorizedParameterMutation
  objectType = CELL
  parameter  = txPower
  cellId     = <from governed state>
  expectedValue
  desiredValue
```

Unsupported types/parameters **MUST** reject without vendor I/O.

**Forbidden:** `executeCommand(String)`, `apply(Map)`, generic REST proxy.

---

## 23. `EricssonEnmWriteAdapter`

Ericsson-scoped adapter implementing `VendorNetworkWriteAdapter`.

Rules:

1. Delegates to `EricssonWriteTransport`
2. Enforces ENM/Ericsson profile bindings
3. Production transport selection **MUST** fail closed when not configured
4. **MUST NOT** extend Phase 11 `EnmTransport`
5. Lab/test transport **MAY** exist for Level 0–2 certification only

Default wiring in gateway: lab/test double or `UnconfiguredProductionEricssonWriteTransport` that denies with `PRODUCTION_WRITE_TRANSPORT_NOT_CONFIGURED`.

---

## 24. `EricssonWriteTransport` interface only

Define transport SPI behind adapter:

```java
public interface EricssonWriteTransport {
    VendorMutationResult transmitMutation(EricssonMutationRequest request);
    PostMutationObservation observeParameter(EricssonObservationRequest request);
}
```

Production implementation: **NOT CONFIGURED** in default codebase.

While unresolved:

- All production paths return fail-closed denial
- No guessed Ericsson REST/CLI/Bulk CM endpoints in production profile
- Architecture honesty: protocol **UNRESOLVED** until vendor evidence

---

## 25. `ExpectedStateGuardStrength`

Enum:

| Value | Meaning |
|---|---|
| `ATOMIC` | Vendor protocol supports compare-and-set / conditional apply |
| `READ_THEN_WRITE` | Observe then mutate; residual TOCTOU acknowledged |

Policy per target in `verificationPolicy` / capability profile.

Rules:

1. Use `ATOMIC` when protocol provably supports CAS
2. **MUST NOT** claim atomicity under `READ_THEN_WRITE`
3. Production policy may deny `READ_THEN_WRITE` for production targets until evidence accepts residual race
4. While Ericsson production protocol unresolved, default production policy is **fail-closed**

---

## 26. Direct vendor observation

Canonical SNIP/`radio_configuration` state **MUST NOT** be sole final expected-state authority.

Before mutation, gateway **MUST** directly observe vendor target:

```text
actual txPower for governed cellId == expectedValue
```

Mismatch, unknown, stale, unavailable, timeout ⇒ **ZERO mutation** (fail closed).

Observation evidence **MUST** be persisted in `production_gateway_evidence`.

---

## 27. Mutation outcome — NOT_SENT / REJECTED / VENDOR_ACCEPTED / OUTCOME_UNKNOWN

| Outcome | Meaning |
|---|---|
| `NOT_SENT` | PRE_SEND; no mutation transmitted |
| `REJECTED` | Vendor explicit rejection |
| `VENDOR_ACCEPTED` | Vendor accepted request — **not verification** |
| `OUTCOME_UNKNOWN` | MAY_HAVE_SENT; result uncertain |

HTTP 200 from vendor **MUST NOT** map directly to `VERIFIED`.

Vendor acceptance evidence **MUST** be persisted before app visibility where possible.

---

## 28. Independent verification

After mutation, gateway performs independent vendor readback (not mutation response replay).

Verification outcomes:

```text
VERIFIED
MISMATCH
UNKNOWN
TIMEOUT
SOURCE_UNAVAILABLE
STALE_OBSERVATION
```

Only fresh direct observation matching `desiredValue` yields attempt `VERIFIED` and lifecycle `VERIFIED`.

Distinct concepts:

- `PRODUCTION_VERIFIED` — vendor observation success
- `CANONICAL_RECONCILED` — Phase 12 reconciliation (separate)

Verification persistence **MUST** complete in `REQUIRES_NEW` before app treats change as verified. If external verification succeeded but DB persist failed: recover by fresh readback — **do not** resend mutation.

---

## 29. Phase 12 boundary — `NETWORK_SYNCHRONIZATION_REQUIRED`

Phase 16 **MUST NOT** directly mutate canonical `radio_configuration`.

After successful production verification, **MAY** emit/record:

```text
NETWORK_SYNCHRONIZATION_REQUIRED
```

Phase 12 remains authoritative for read/reconciliation into canonical knowledge. Agents/LLMs **MUST NOT** assign authoritative confidence or reconciliation outcomes.

---

## 30. Ambiguous outcome decision table

| Observation after MAY_HAVE_SENT | Next state | Auto retry mutation? | Action |
|---|---|---|---|
| actual == desired | `VERIFIED` | **NO** | Persist verification; emit sync required |
| actual == expected (unchanged) | Safe stop | **NO** | New separately authorized execution required |
| actual == third value | `MANUAL_INTERVENTION_REQUIRED` | **NO** | Human investigation |
| observation timeout/unavailable | `PRODUCTION_OUTCOME_UNRESOLVED` | **NO** | Human recovery |
| vendor reject confirmed | `VENDOR_REJECTED` / failed lifecycle | **NO** | Governed recovery |

**No row authorizes blind second mutation.**

---

## 31. Lease / fencing — `productionTargetId + cellId + parameter`

Scope:

```text
productionTargetId + cellId + parameter
```

Rules:

1. Acquire before application pre-grant preflight completes / before grant issuance
2. Only current holder may receive grant, invoke gateway, persist attempts, initiate rollback after recovery
3. Fencing protects SNIP pre-send authority — **does not** cancel external vendor write already accepted
4. No claim of distributed ACID with vendor
5. No claim of exactly-once network mutation

Implement `ProductionLeaseService` mirroring Phase 15 lease patterns with production scope.

---

## 32. Kill switch — default false

Configuration:

```yaml
snip:
  production-change:
    global-execution-enabled: false   # default MUST be false
```

Kill switch is **one gate among many** — no single boolean may enable production writes alone.

Gateway **MUST** independently re-check immediately before mutation. If disabled after grant issuance but before send ⇒ **DENY**.

Grant possession **never** overrides kill switch.

---

## 33. Target health — ACTIVE / SUSPENDED / DISABLED

### Target state (`target_state`)

| State | Execution |
|---|---|
| `ACTIVE` | Eligible if all other gates pass |
| `SUSPENDED` | Deny; safety suspension; no auto-resume |
| `DISABLED` | Deny; administrative disable |

### Health aggregate (`production_target_health`)

Tracks rolling counters: outcome-unknown count, verification failure count, health_state (`HEALTHY`/`DEGRADED`/`UNHEALTHY`).

Automatic **safety suspension** allowed when thresholds exceeded. Automatic **re-enable FORBIDDEN**.

`ProductionTargetHealth` is separate from Spring Boot application readiness.

---

## 34. Rate limits — shared durable defaults

Shared durable enforcement (gateway or shared service with PostgreSQL counters):

| Limit | Default |
|---|---|
| maxCellsPerExecution | 1 |
| maxParametersPerExecution | 1 |
| maxOperationsPerExecution | 1 |
| maxChangesPerTargetPerHour | 6 |
| maxChangesPerCellPerDay | 3 |
| maxOutcomeUnknownBeforeSuspend | 3 |
| maxVerificationFailuresBeforeSuspend | 3 |

Unknown limiter state ⇒ **DENY**.

Multiple gateway replicas **MUST** share counters — local in-memory only is **FORBIDDEN** for production enforcement.

---

## 35. Credential security
### 35.1 Gateway-centric credential evidence (B16-S-05)

Primary evidence **MUST** be gateway/integration focused:

| Evidence ID | Requirement |
|---|---|
| P16-E021 | App module has no write credential provider dependency |
| P16-E022 | Gateway module owns `ProductionCredentialResolutionService` |
| P16-E072–P16-E075 | Credential resolved only after consume + gateway final preflight; failure ⇒ zero mutation; no old-version fallback; read≠write profile |

App-side credential failure tests are **secondary** only.

 — Phase 10 reuse

1. Production write credentials separate from read credentials
2. Resolvable only by write-gateway workload identity
3. Azure Key Vault via explicit `WorkloadIdentityCredential` in production
4. `DefaultAzureCredential` local dev only
5. Per-session resolution; **no** long-lived secret cache
6. Latest enabled version only; **no** older-version fallback
7. Credential values **NEVER** in DB, logs, audit, metrics, API, exceptions, evidence payloads
8. Only `credentialProfileId` and non-secret version identifiers in evidence
9. SNIP application **MUST NOT** import write-side Key Vault secret resolution beans

---

## 36. Network and runtime security — deploy / k8s paths

Implement or document deployment artifacts (repository path or approved external deployment repo per architecture §46):

```text
deploy/k8s/production-write-gateway/
  deployment.yaml
  service.yaml
  networkpolicy.yaml
  serviceaccount.yaml
  workload-identity-binding.yaml
```

Requirements:

1. Gateway ingress: authenticated SNIP execution plane only
2. Gateway egress: approved ENM FQDN/IP allowlist only
3. SNIP app NetworkPolicy: may reach gateway; **not** vendor ENM write endpoints
4. Distinct UAMI for gateway with Key Vault Secrets User scoped to write secrets only
5. TLS mandatory; hostname verification; trust-all forbidden
6. Document Cilium FQDN-cache/CIDR lab limitation without broadening egress

Terraform equivalents allowed if deployment is external — evidence required before Level 4.

---

## 37. Audit event catalog

Minimum `eventType` values:

```text
PRODUCTION_CHANGE_REQUESTED
PRODUCTION_ADMISSION_PASSED
PRODUCTION_ADMISSION_REJECTED
PRODUCTION_REVIEW_APPROVED
PRODUCTION_REVIEW_REJECTED
PRODUCTION_AUTHORIZED
PRODUCTION_AUTHORIZATION_STALE
PRODUCTION_LEASE_ACQUIRED
PRODUCTION_LEASE_RELEASED
PRODUCTION_LEASE_CONFLICT
PRODUCTION_PREGRANT_PREFLIGHT_PASSED
PRODUCTION_PREGRANT_PREFLIGHT_DENIED
PRODUCTION_GRANT_ISSUED
PRODUCTION_GRANT_REVOKED
PRODUCTION_GRANT_EXPIRED
PRODUCTION_GRANT_CONSUME_SUCCEEDED
PRODUCTION_GRANT_CONSUME_DENIED
PRODUCTION_GATEWAY_ATTEMPT_PRE_SEND
PRODUCTION_MUTATION_NOT_SENT
PRODUCTION_MUTATION_REJECTED
PRODUCTION_MUTATION_VENDOR_ACCEPTED
PRODUCTION_MUTATION_OUTCOME_UNKNOWN
PRODUCTION_VERIFICATION_STARTED
PRODUCTION_VERIFIED
PRODUCTION_VERIFICATION_FAILED
PRODUCTION_RECOVERY_REQUIRED
PRODUCTION_ROLLBACK_REQUESTED
PRODUCTION_ROLLBACK_AUTHORIZED
PRODUCTION_ROLLBACK_EXECUTED
PRODUCTION_ROLLED_BACK
PRODUCTION_MANUAL_INTERVENTION_REQUIRED
PRODUCTION_CANCELLED_BEFORE_MUTATION
PRODUCTION_TARGET_SUSPENDED
PRODUCTION_TARGET_RESUMED
PRODUCTION_KILL_SWITCH_DENY
PRODUCTION_RATE_LIMIT_DENY
PRODUCTION_AUDIT_CHAIN_INVALID
```

Each event records `actorPrincipalId`, safe payload, reason codes, fingerprints (where applicable). No secrets.

---

## 38. Audit hash chain — reference to A16-01

All audit events **MUST** implement §4 (A16-01):

- One chain per `productionChangeId`
- Genesis SHA-256 formula
- Canonical JSON key order
- `eventHash` calculation
- `FOR UPDATE` concurrency on append
- Integrity states and gap/mismatch blocks mutation

`ProductionChangeAuditService` is the single append API; bypass inserts **FORBIDDEN**.

---

## 39. Metrics — low cardinality

Implement `ProductionChangeMetrics` / gateway metrics with labels limited to:

```text
vendor
platform
environment
result
reasonCategory
```

Examples:

```text
production_change_requests_total
production_change_authorizations_total
production_execution_attempts_total
production_execution_verified_total
production_execution_outcome_unknown_total
production_verification_failures_total
production_rollbacks_total
production_manual_intervention_total
production_target_suspensions_total
production_grant_consume_conflicts_total
production_kill_switch_denials_total
production_rate_limit_denials_total
production_audit_chain_invalid_total
```

**Forbidden** high-cardinality labels: `cellId`, `executionId`, `planId`, `productionChangeId`, `userId`, `grantId`, `ticket`, `fingerprint`, `endpoint`.

---

## 40. API — `/api/v1/production-changes`

Base path:

```text
/api/v1/production-changes
/api/v1/production-targets
```

Endpoints:

```text
POST   /api/v1/production-changes
GET    /api/v1/production-changes
GET    /api/v1/production-changes/{id}
POST   /api/v1/production-changes/{id}/review
POST   /api/v1/production-changes/{id}/authorize
POST   /api/v1/production-changes/{id}/execute
GET    /api/v1/production-changes/{id}/evidence
POST   /api/v1/production-changes/{id}/rollback/request
POST   /api/v1/production-changes/{id}/rollback/review
POST   /api/v1/production-changes/{id}/rollback/authorize
POST   /api/v1/production-changes/{id}/rollback/execute
POST   /api/v1/production-targets/{id}/suspend
POST   /api/v1/production-targets/{id}/resume
```

Permissions enforced per §14/§15. Only explicit `execute` and `rollback/execute` may initiate gateway invocation.

Responses use stable reason codes (§52). No credential or raw vendor secret-bearing payloads in responses.

---

## 41. Idempotency rules

| Operation | Idempotency key | Behavior |
|---|---|---|
| Create production change | Client `Idempotency-Key` header optional | Same key + same body returns same resource |
| Review / authorize | productionChangeId + actor + generation | Duplicate approve: no-op or conflict |
| Execute forward | productionChangeId + authorizationGeneration | Second execute after terminal outcome: deny; no second mutation |
| Gateway consume | grantId | Exactly one successful consume |
| Grant delivery retry | grantId while ISSUED | Allowed PRE_SEND only |
| Rollback execute | rollback authorization generation | One rollback mutation attempt unless new authorized rollback |

Terminal states **MUST** be durable idempotency barriers.

---

## 42. Transaction boundaries — `REQUIRES_NEW` points

Mandatory `REQUIRES_NEW` (or equivalent independent commit):

1. Lease acquisition / fencing token bump
2. Grant issuance
3. Grant consume (gateway)
4. Attempt pre-send durable mark
5. Mutation outcome persistence after vendor I/O
6. Verification outcome persistence
7. Critical failure persistence (`OUTCOME_UNKNOWN`, `VERIFICATION_FAILED`, `RECOVERY_REQUIRED`, `MANUAL_INTERVENTION_REQUIRED`, consumed-grant pre-send unresolved)
8. Rate-limit counter increment (when separate from grant TX)
9. Target suspension side effects
10. Audit append when outer TX rolls back but event must survive (rare; prefer same TX)

**MUST NOT** hold database transactions open across vendor network I/O.

If outer orchestration rolls back after vendor MAY_HAVE_SENT, critical outcome rows **MUST** survive via independent commits.

---

## 43. Failure injection — test-only hooks (FI-01…FI-15)

Failure injection **MUST** be active only under `@ActiveProfiles("test")` plus explicit property gate. **FORBIDDEN** in production profile.

### 43.1 Forward injection points

| ID | Injection point | Send phase | Expected classification |
|---|---|---|---|
| FI-01 | Before grant lookup | PRE_SEND | No consume; no attempt |
| FI-02 | After grant validation, before consume | PRE_SEND | Grant remains ISSUED |
| FI-03 | After consume DB commit, before attempt insert | PRE_SEND | Grant CONSUMED; no attempt; recovery required |
| FI-04 | After attempt insert, before final gateway preflight | PRE_SEND | Attempt PRE_SEND |
| FI-05 | After preflight, before credential resolution | PRE_SEND | Attempt PRE_SEND/SEND_ELIGIBLE |
| FI-06 | After credential resolution, before direct observation | PRE_SEND | No transport invoke |
| FI-07 | After direct observation, before mutation invocation | PRE_SEND | No transport invoke |
| FI-08 | Immediately when mutation invocation begins | **MAY_HAVE_SENT** | No auto retry |
| FI-09 | Vendor applied, connection dropped | **MAY_HAVE_SENT** | OUTCOME_UNKNOWN; readback governs |
| FI-10 | Vendor rejected | PRE_SEND or MAY_HAVE_SENT | VENDOR_REJECTED; mutation count 0 if pre-apply proof |
| FI-11 | Vendor response received before durable outcome commit | MAY_HAVE_SENT | Outcome must persist via REQUIRES_NEW |
| FI-12 | Durable outcome committed before caller HTTP response | MAY_HAVE_SENT/terminal | App reconstructs from DB |
| FI-13 | Verification readback begins | post-send | VERIFYING |
| FI-14 | Verification succeeds before persistence | post-send | Evidence must durably persist |
| FI-15 | Verification persistence fails | post-send | Prior outcome survives; alert |

Rollback injection points RB-FI-01…RB-FI-15 mirror forward at rollback grant/attempt boundaries.

### 43.2 Controlled mutation counter

All mutation-path tests **MUST** use a test-only `VendorNetworkWriteAdapter` (or transport double) with an atomic `mutationInvocationCounter`. Assertions **MUST** specify exact counts where determinable (see §56 scenarios).

## 44. Test isolation — Testcontainers

Integration tests use PostgreSQL Testcontainers (required). Tests **MUST** restore shared state:

- canonical `radio_configuration`
- Phase 6 twin state
- Phase 12 sync/knowledge/drift
- Phase 13 proposals
- Phase 14 plans
- Phase 15 executions
- Phase 16 production targets/changes/grants/attempts/evidence
- simulator/lab vendor state doubles

No class-order dependency. No Surefire ordering workaround. No live production ENM in default CI.

---

## 45. Structural security tests

Minimum structural test class list:

```text
ProductionChangeArchitectureIsolationTest
ProductionChangeModuleBoundaryTest
ProductionChangePackageBoundaryTest
ProductionChangeMigrationSpecTest
ProductionChangeDependencyRuleTest
ProductionWriteGatewayIsolationTest
EnmTransportReadOnlyContractTest
AgentProductionMutationIsolationTest
McpProductionMutationIsolationTest
SchedulerProductionMutationIsolationTest
EventConsumerProductionMutationIsolationTest
ProductionCredentialIsolationTest
ProductionChangeStructuralTest
```

Each test **MUST** map to evidence catalog items (§55).

---

## 46. Behavioral security tests

Minimum behavioral test class list:

```text
ProductionChangeSoDTest
ProductionFingerprintTest
ProductionAdmissionTest
ProductionAuthorizationStaleTest
ProductionGrantIssuanceLimitTest
ProductionGrantConsumeTest
ProductionKillSwitchTest
ProductionRateLimitTest
ProductionChangeControlValidationTest
ProductionPreGrantPreflightTest
ProductionGatewayPreflightTest
ProductionExpectedStateGuardTest
ProductionPreSendRetryTest
ProductionMayHaveSentNoRetryTest
ProductionVerificationTest
ProductionAmbiguousOutcomeTest
ProductionRollbackGovernanceTest
ProductionAuditChainTest
ProductionIdempotencyTest
ProductionFailurePersistenceTest
ProductionReasonCodeTest
```

Behavioral tests **MUST** assert deny paths and durable state — not merely HTTP status codes.

---

## 47. Integration tests

Minimum integration test class list:

```text
ProductionChangeLifecycleIntegrationTest
ProductionChangeGatewayHandoffIntegrationTest
ProductionGrantConcurrentConsumeIntegrationTest
ProductionLeaseConflictIntegrationTest
ProductionTargetSuspensionIntegrationTest
ProductionChangeWindowIntegrationTest
ProductionOutcomeUnknownRecoveryIntegrationTest
ProductionRollbackLifecycleIntegrationTest
ProductionCanonicalIsolationIntegrationTest
ProductionPhase15EligibilityIntegrationTest
ProductionPhase12BoundaryIntegrationTest
ProductionAuditChainConcurrencyIntegrationTest
ProductionRateLimitSharedCounterIntegrationTest
ProductionChangeApiIntegrationTest
ProductionChangeRegressionIntegrationTest
```

Tests run with Testcontainers PostgreSQL and in-process or testcontainer gateway where applicable. Zero real network mutation.

---

## 48. Infrastructure tests

Validate deployment/security artifacts structurally:

```text
ProductionGatewayKubernetesManifestTest
ProductionGatewayNetworkPolicyTest
ProductionWorkloadIdentityBindingTest
ProductionGatewayEgressAllowlistTest
ProductionTerraformPlanSnapshotTest   (if Terraform in repo)
ProductionCertificationLevelDefaultTest
```

These tests verify manifest presence and policy shape — not live cluster deployment in default CI.

---

## 49. Level certification — L0–L4

| Level | Meaning | Default CI |
|---|---|---|
| L0 | Simulator / structural verified | Required PASS |
| L1 | Vendor lab verified | Manual workflow |
| L2 | Pre-production verified | Manual workflow |
| L3 | Production target registered | Manual evidence |
| L4 | Controlled production execution authorized | External operator sign-off |

**Code existence MUST NOT imply L4.**

Level stored on `production_network_target.certification_level`. Execution against target requires policy check of minimum level per environment.

---

## 50. Real Ericsson E2E — separate manual

Real Ericsson E2E is **NOT** part of default CI.

Requires:

- manually triggered workflow
- environment gates
- approved lab/test endpoint first (architecture gate 145)
- Level 1–2 evidence before production consideration
- external operator authorization

Completion report **MUST NOT** claim real vendor E2E unless manual workflow evidence attached.

---

## 51. Configuration — `snip.production-change` namespace

```yaml
snip:
  production-change:
    enabled: false
    global-execution-enabled: false
    maximum-cells-per-execution: 1
    maximum-parameters-per-execution: 1
    maximum-operations-per-execution: 1
    maximum-forward-grant-ttl: PT5M
    maximum-rollback-grant-ttl: PT5M
    maximum-active-forward-grants-per-change: 1
    maximum-active-rollback-grants-per-change: 1
    maximum-changes-per-target-per-hour: 6
    maximum-changes-per-cell-per-day: 3
    maximum-outcome-unknown-before-suspend: 3
    maximum-verification-failures-before-suspend: 3
    require-production-review: true
    require-production-authorization: true
    require-change-control-validation: true
    require-current-value-match: true
    require-independent-verification: true
    require-rollback-review: true
    require-rollback-authorization: true
    automatic-rollback-enabled: false
    gateway-base-url: ${PRODUCTION_WRITE_GATEWAY_URL:}
    permitted-vendors:
      - ERICSSON
    permitted-platforms:
      - ENM
    minimum-certification-level-for-execution: L0
```

All defaults **fail closed**. Missing gateway URL ⇒ execute denies.

---

## 52. Reason codes catalog

Stable reason codes minimum:

```text
PRODUCTION_PHASE15_EXECUTION_INELIGIBLE
PRODUCTION_PHASE15_EXECUTION_NOT_VERIFIED
PRODUCTION_PHASE14_PLAN_STALE
PRODUCTION_AUTHORIZATION_MISSING
PRODUCTION_AUTHORIZATION_STALE
PRODUCTION_TARGET_NOT_FOUND
PRODUCTION_TARGET_NOT_ACTIVE
PRODUCTION_TARGET_SUSPENDED
PRODUCTION_TARGET_DISABLED
PRODUCTION_TARGET_CERTIFICATION_INSUFFICIENT
PRODUCTION_CHANGE_CONTROL_INVALID
PRODUCTION_CHANGE_CONTROL_EXPIRED
PRODUCTION_CHANGE_WINDOW_CLOSED
PRODUCTION_FINGERPRINT_MISMATCH
PRODUCTION_LEASE_UNAVAILABLE
PRODUCTION_FENCING_TOKEN_STALE
PRODUCTION_GRANT_NOT_FOUND
PRODUCTION_GRANT_EXPIRED
PRODUCTION_GRANT_REVOKED
PRODUCTION_GRANT_CONSUME_CONFLICT
PRODUCTION_GRANT_ISSUANCE_LIMIT_EXCEEDED
PRODUCTION_GRANT_ACTIVE_CONFLICT
PRODUCTION_KILL_SWITCH_DENY
PRODUCTION_RATE_LIMIT_EXCEEDED
PRODUCTION_PREFLIGHT_DENIED
PRODUCTION_EXPECTED_STATE_MISMATCH
PRODUCTION_EXPECTED_STATE_UNKNOWN
PRODUCTION_CREDENTIAL_RESOLUTION_FAILURE
PRODUCTION_TLS_FAILURE
PRODUCTION_WRITE_TRANSPORT_NOT_CONFIGURED
PRODUCTION_VENDOR_CONNECTION_FAILURE
PRODUCTION_VENDOR_REJECTION
PRODUCTION_MUTATION_OUTCOME_UNKNOWN
PRODUCTION_VERIFICATION_MISMATCH
PRODUCTION_VERIFICATION_TIMEOUT
PRODUCTION_VERIFICATION_UNAVAILABLE
PRODUCTION_ROLLBACK_AUTHORIZATION_MISSING
PRODUCTION_ROLLBACK_OUTCOME_UNKNOWN
PRODUCTION_SOD_VIOLATION
PRODUCTION_AUDIT_CHAIN_INVALID
PRODUCTION_GATEWAY_UNAVAILABLE
MANUAL_INTERVENTION_REQUIRED
```

API error responses **MUST** use these codes — not free-text authority.

---

## 53. Threat model — T01–T48 traceability

| ID | Threat | Primary control | Spec section | Evidence ID(s) |
|---|---|---|---|---|
| T01 | Stolen operator session | SoD via actorPrincipalId | §14 | P16-E023,P16-E112,P16-E169 |
| T02 | Malicious requester crafted create | Field rejection | §11 | P16-E026,P16-E027,P16-E163 |
| T03 | Malicious reviewer | Review≠auth | §14 | P16-E140 |
| T04 | Malicious authorizer | Authorizer≠executor | §14 | P16-E024,P16-E112 |
| T05 | Compromised agent | No agent perms | §3.2, §45 | P16-E017,P16-E043 |
| T06 | Compromised MCP | No MCP mutation tools | §3.2, §45 | P16-E018,P16-E044 |
| T07 | Replayed authorization | Fingerprint generation | §12, §15 | P16-E028,P16-E126 |
| T08 | Replayed grant after consume | Atomic consume | §16 | P16-E060,P16-E081,P16-E162 |
| T09 | Stale authorization | Preflight stale checks | §15, §18 | P16-E028,P16-E053,P16-E149,P16-E165 |
| T10 | Target substitution | Fingerprint binds target | §12 | P16-E029,P16-E052 |
| T11 | Environment substitution | Registry + fingerprint | §9 | P16-E029 |
| T12 | Adapter substitution | Profile binding | §9 | P16-E116 |
| T13 | Capability downgrade | Version bind | §9 | P16-E080 |
| T14 | Credential exfiltration from app | Separate WI | §2, §35 | P16-E021,P16-E072,P16-E102 |
| T15 | Secret logging | Sanitization | §35, §37 | P16-E049 |
| T16 | Vendor endpoint spoofing | TLS + egress | §36 | P16-E090,P16-E104 |
| T17 | TLS downgrade | TLS mandatory | §36 | P16-E091,P16-E104 |
| T18 | DNS manipulation | Egress allowlist | §36 | P16-E089,P16-E103 |
| T19 | Stale expected state | Direct observe | §26 | P16-E033,P16-E034,P16-E150 |
| T20 | TOCTOU race | Guard strength honesty | §25 | P16-E036 |
| T21 | Concurrent SNIP mutation | Lease scope | §31 | P16-E064 |
| T22 | Ambiguous vendor response | OUTCOME_UNKNOWN | §27, §30 | P16-E037,P16-E069,P16-E151,P16-E153,P16-E154,P16-E155 |
| T23 | Replay after timeout | MAY_HAVE_SENT rule | §21 | P16-E037,P16-E038 |
| T24 | Forged verification | Gateway evidence required | §6, §28 | P16-E039,P16-E070,P16-E152 |
| T25 | Rollback manipulation | Phase 14 persisted rollback only | §20 | P16-E077,P16-E079 |
| T26 | Change-window extension | Fingerprint window | §12 | P16-E158 |
| T27 | Ticket substitution | CC revalidation | §13 | P16-E032 |
| T28 | Configuration weakening | Multi-gate deny-default | §32, §51 | P16-E047,P16-E048 |
| T29 | Event-triggered execution | Structural absence | §3.2 | P16-E020 |
| T30 | Scheduler-triggered execution | Structural absence | §3.2 | P16-E019 |
| T31 | Excessive mutation rate | Shared rate limits | §34 | P16-E065 |
| T32 | Audit tampering | Hash chain A16-01 | §4, §38 | P16-E066,P16-E067,P16-E168 |
| T33 | Compromised SNIP app minting grants | Gateway enforce + limits | §7, §18 | P16-E041,P16-E165 |
| T34 | Compromised write gateway | Least privilege typed mutation | §18, §22 | P16-E114,P16-E117 |
| T35 | Grant issuance spam | A16-04 limits | §7 | P16-E041,P16-E042 |
| T36 | Concurrent grant consumption | Atomic SQL | §16 | P16-E051 |
| T37 | Grant replay race | Consume-before-send | §16, §20 | P16-E051,P16-E061 |
| T38 | Revoked grant replay | Durable status at consume | §16 | P16-E057,P16-E058 |
| T39 | Consumed then crash | Crash matrix | §21, §42 | P16-E062,P16-E063,P16-E141 |
| T40 | App/gateway split brain | Evidence authority | §6 | P16-E040,P16-E166 |
| T41 | Kill switch after grant | Gateway independent check | §32 | P16-E030 |
| T42 | Target suspended after grant | Gateway ACTIVE check | §33 | P16-E031 |
| T43 | Rate-limit bypass replicas | Shared durable counters | §34 | P16-E065 |
| T44 | Target registry privilege escalation | ADMINISTER_PRODUCTION_TARGET | §10 | P16-E167 |
| T45 | Display-name SoD collision | actorPrincipalId only | §14 | P16-E025 |
| T46 | Audit-chain gap/truncation | A16-01 invalid blocks | §4 | P16-E068 |
| T47 | Old credential version fallback | Phase 10 no fallback | §35 | P16-E074 |
| T48 | Change ticket invalid after grant | Gateway CC revalidation | §13, §18 | P16-E032 |

---

## 54. Architecture gate traceability matrix (154 gates)

**Semantic mapping rule (B16-S-01):** Evidence IDs **MUST NOT** be assigned by ordinal coincidence (`G16-N` → `P16-E{N}` forbidden).

**High-risk rule:** Gates involving runtime production safety **MUST** include at least one BEHAVIORAL or INTEGRATION evidence item. STRUCTURAL-only high-risk gates **MUST** equal 0.

### 54.1 Traceability validation (mandatory)

Implementation **MUST** include `ProductionChangeGateTraceabilityValidationTest` validating this matrix and `docs/implementation/phase16-gate-evidence-map.json` for:

- 154 unique gates `G16-001`…`G16-154`
- no missing / duplicate gate IDs
- every evidence reference exists in §55
- every gate has ≥1 evidence item
- high-risk runtime gates are not STRUCTURAL-only
- Markdown and JSON mappings identical
- zero placeholder/generic evidence rows

| Gate | Architecture requirement | Spec section | Component | Evidence ID(s) | Evidence type(s) | Why this evidence proves the gate | Expected |
|---|---|---|---|---|---|---|---|
| G16-001 | Parent Phase 15 immutable baseline pinned exactly (ae9c13d…). | §2–§3 | ProductionChangeService | P16-E045 | BEHAVIORAL | Evidence P16-E045 exercises the architecture requirement with BEHAVIORAL proof: Parent Phase 15 immutable baseline pinned exactly (ae9c13d…). | PASS |
| G16-002 | Failed historical Phase 15 candidate preserved (0cb1223…); history not rewritten. | §2–§3 | ProductionChangeService | P16-E115 | STRUCTURAL | Evidence P16-E115 exercises the architecture requirement with STRUCTURAL proof: Failed historical Phase 15 candidate preserved (0cb1223…); history not rewritten. | PASS |
| G16-003 | Separate Production Write Gateway runtime required. | §2–§3 | ProductionChangeService | P16-E013, P16-E014 | STRUCTURAL | Evidence P16-E013, P16-E014 exercises the architecture requirement with STRUCTURAL proof: Separate Production Write Gateway runtime required. | PASS |
| G16-004 | Ordinary SNIP application process must not implement production vendor writes. | §2–§3 | ProductionChangeService | P16-E001, P16-E002, P16-E003, P16-E004, P16-E005, P16-E006, P16-E007, P16-E008, P16-E009, P16-E010, P16-E011, P16-E012 | STRUCTURAL | Evidence P16-E001, P16-E002, P16-E003, P16-E004, P16-E005, P16-E006, P16-E007, P16-E008, P16-E009, P16-E010, P16-E011, P16-E012 exercises the architecture requirement with STRUCTURAL proof: Ordinary SNIP application process must not implement production vendor writes. | PASS |
| G16-005 | Ordinary SNIP application must not possess vendor-write workload identity. | §2–§3 | ProductionWriteGateway | P16-E021, P16-E088, P16-E102 | EXTERNAL_CERTIFICATION, INFRASTRUCTURE, STRUCTURAL | Evidence P16-E021, P16-E088, P16-E102 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INFRASTRUCTURE/STRUCTURAL proof: Ordinary SNIP application must not possess vendor-write workload identity. | PASS |
| G16-006 | Write gateway is a security boundary, not merely an in-process service class. | §2–§3 | ProductionChangeService | P16-E001, P16-E002, P16-E003, P16-E004, P16-E005, P16-E006, P16-E007, P16-E008, P16-E009, P16-E010, P16-E011, P16-E012, P16-E013, P16-E014 | STRUCTURAL | Evidence P16-E001, P16-E002, P16-E003, P16-E004, P16-E005, P16-E006, P16-E007, P16-E008, P16-E009, P16-E010, P16-E011, P16-E012, P16-E013, P16-E014 exercises the architecture requirement with STRUCTURAL proof: Write gateway is a security boundary, not merely an in-process service class. | PASS |
| G16-007 | Distinct SNIP application / read / production-write identities. | §2–§3 | ProductionChangeService | P16-E087, P16-E100 | EXTERNAL_CERTIFICATION, INFRASTRUCTURE | Evidence P16-E087, P16-E100 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INFRASTRUCTURE proof: Distinct SNIP application / read / production-write identities. | PASS |
| G16-008 | Production write credentials resolvable only by write-gateway identity. | §2–§3 | ProductionChangeService | P16-E015, P16-E021, P16-E022, P16-E072, P16-E100, P16-E101 | EXTERNAL_CERTIFICATION, INTEGRATION, STRUCTURAL | Evidence P16-E015, P16-E021, P16-E022, P16-E072, P16-E100, P16-E101 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INTEGRATION/STRUCTURAL proof: Production write credentials resolvable only by write-gateway identity. | PASS |
| G16-009 | Phase 10 WI + Key Vault principles reused; no long-lived secret cache. | §2–§3 | ProductionChangeService | P16-E015, P16-E074 | INTEGRATION, STRUCTURAL | Evidence P16-E015, P16-E074 exercises the architecture requirement with INTEGRATION/STRUCTURAL proof: Phase 10 WI + Key Vault principles reused; no long-lived secret cache. | PASS |
| G16-010 | Phase 11 EnmTransport remains read-only; no write methods added. | §2–§3 | ProductionWriteGateway | P16-E016 | STRUCTURAL | Evidence P16-E016 exercises the architecture requirement with STRUCTURAL proof: Phase 11 EnmTransport remains read-only; no write methods added. | PASS |
| G16-011 | Separate write-side SPI (VendorNetworkWriteAdapter / EricssonEnmWriteAdapter / EricssonWriteTransport). | §8–§16 | ProductionChangeService | P16-E116 | STRUCTURAL | Evidence P16-E116 exercises the architecture requirement with STRUCTURAL proof: Separate write-side SPI (VendorNetworkWriteAdapter / EricssonEnmWriteAdapter / EricssonWriteTransport). | PASS |
| G16-012 | Ericsson production write protocol explicitly UNRESOLVED until evidence. | §8–§16 | ProductionChangeService | P16-E099 | EXTERNAL_CERTIFICATION | Evidence P16-E099 exercises the architecture requirement with EXTERNAL_CERTIFICATION proof: Ericsson production write protocol explicitly UNRESOLVED until evidence. | PASS |
| G16-013 | Production Ericsson write transport NOT CONFIGURED and fail-closed by default. | §8–§16 | ProductionChangeService | P16-E135 | BEHAVIORAL | Evidence P16-E135 exercises the architecture requirement with BEHAVIORAL proof: Production Ericsson write transport NOT CONFIGURED and fail-closed by default. | PASS |
| G16-014 | No dual-purpose read/write transport conflation. | §8–§16 | ProductionChangeService | P16-E016 | STRUCTURAL | Evidence P16-E016 exercises the architecture requirement with STRUCTURAL proof: No dual-purpose read/write transport conflation. | PASS |
| G16-015 | No arbitrary / generic vendor command interface. | §8–§16 | ProductionWriteGateway | P16-E136 | BEHAVIORAL | Evidence P16-E136 exercises the architecture requirement with BEHAVIORAL proof: No arbitrary / generic vendor command interface. | PASS |
| G16-016 | Typed mutation only (CELL / txPower / expected / desired). | §8–§16 | ProductionChangeService | P16-E117 | BEHAVIORAL | Evidence P16-E117 exercises the architecture requirement with BEHAVIORAL proof: Typed mutation only (CELL / txPower / expected / desired). | PASS |
| G16-017 | Initial scope hard-limited to Ericsson ENM CELL txPower. | §8–§16 | ProductionChangeService | P16-E047, P16-E117 | BEHAVIORAL | Evidence P16-E047, P16-E117 exercises the architecture requirement with BEHAVIORAL proof: Initial scope hard-limited to Ericsson ENM CELL txPower. | PASS |
| G16-018 | Max cells per execution = 1. | §8–§16 | ProductionChangeService | P16-E047 | BEHAVIORAL | Evidence P16-E047 exercises the architecture requirement with BEHAVIORAL proof: Max cells per execution = 1. | PASS |
| G16-019 | Max parameters per execution = 1. | §8–§16 | ProductionChangeService | P16-E118 | BEHAVIORAL | Evidence P16-E118 exercises the architecture requirement with BEHAVIORAL proof: Max parameters per execution = 1. | PASS |
| G16-020 | Max forward mutation operations = 1. | §8–§16 | ProductionWriteGateway | P16-E119 | BEHAVIORAL | Evidence P16-E119 exercises the architecture requirement with BEHAVIORAL proof: Max forward mutation operations = 1. | PASS |
| G16-021 | ProductionNetworkTarget model defined with required fields. | §8–§16 | ProductionChangeService | P16-E120 | STRUCTURAL | Evidence P16-E120 exercises the architecture requirement with STRUCTURAL proof: ProductionNetworkTarget model defined with required fields. | PASS |
| G16-022 | Target states include ACTIVE / SUSPENDED / DISABLED. | §8–§16 | ProductionChangeService | P16-E121 | BEHAVIORAL | Evidence P16-E121 exercises the architecture requirement with BEHAVIORAL proof: Target states include ACTIVE / SUSPENDED / DISABLED. | PASS |
| G16-023 | Target records store credential profile references only. | §8–§16 | ProductionChangeService | P16-E122 | STRUCTURAL | Evidence P16-E122 exercises the architecture requirement with STRUCTURAL proof: Target records store credential profile references only. | PASS |
| G16-024 | ProductionNetworkChange aggregate distinct from Phase 15 execution. | §8–§16 | ProductionChangeService | P16-E123 | STRUCTURAL | Evidence P16-E123 exercises the architecture requirement with STRUCTURAL proof: ProductionNetworkChange aggregate distinct from Phase 15 execution. | PASS |
| G16-025 | Create request accepts only phase15ExecutionId, productionTargetId, changeControlReference. | §8–§16 | ProductionWriteGateway | P16-E137 | BEHAVIORAL | Evidence P16-E137 exercises the architecture requirement with BEHAVIORAL proof: Create request accepts only phase15ExecutionId, productionTargetId, changeControlReference. | PASS |
| G16-026 | API rejects caller-controlled mutation fields. | §8–§16 | ProductionChangeService | P16-E026, P16-E027 | BEHAVIORAL | Evidence P16-E026, P16-E027 exercises the architecture requirement with BEHAVIORAL proof: API rejects caller-controlled mutation fields. | PASS |
| G16-027 | Mutation details derive from governed Phase 14/15 state only. | §8–§16 | ProductionChangeService | P16-E124 | BEHAVIORAL | Evidence P16-E124 exercises the architecture requirement with BEHAVIORAL proof: Mutation details derive from governed Phase 14/15 state only. | PASS |
| G16-028 | Phase 13 ≠ 14 ≠ 15 ≠ 16 authorization independence. | §8–§16 | ProductionChangeService | P16-E138 | INTEGRATION | Evidence P16-E138 exercises the architecture requirement with INTEGRATION proof: Phase 13 ≠ 14 ≠ 15 ≠ 16 authorization independence. | PASS |
| G16-029 | Production authorization not inferred from upstream approvals. | §8–§16 | ProductionChangeService | P16-E138 | INTEGRATION | Evidence P16-E138 exercises the architecture requirement with INTEGRATION proof: Production authorization not inferred from upstream approvals. | PASS |
| G16-030 | Requester must not be Production Authorizer. | §8–§16 | ProductionWriteGateway | P16-E023, P16-E112 | BEHAVIORAL, EXTERNAL_CERTIFICATION | Evidence P16-E023, P16-E112 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION proof: Requester must not be Production Authorizer. | PASS |
| G16-031 | Production Authorizer must not be Executor. | §8–§16 | ProductionChangeService | P16-E024, P16-E112 | BEHAVIORAL, EXTERNAL_CERTIFICATION | Evidence P16-E024, P16-E112 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION proof: Production Authorizer must not be Executor. | PASS |
| G16-032 | Reviewer and Authorizer permissions distinct. | §8–§16 | ProductionChangeService | P16-E140 | BEHAVIORAL | Evidence P16-E140 exercises the architecture requirement with BEHAVIORAL proof: Reviewer and Authorizer permissions distinct. | PASS |
| G16-033 | Agents hold no production review/auth/execute/rollback permissions. | §8–§16 | ProductionChangeService | P16-E017, P16-E043 | BEHAVIORAL, STRUCTURAL | Evidence P16-E017, P16-E043 exercises the architecture requirement with BEHAVIORAL/STRUCTURAL proof: Agents hold no production review/auth/execute/rollback permissions. | PASS |
| G16-034 | MCP holds no production mutation permissions. | §8–§16 | ProductionChangeService | P16-E018, P16-E044 | BEHAVIORAL, STRUCTURAL | Evidence P16-E018, P16-E044 exercises the architecture requirement with BEHAVIORAL/STRUCTURAL proof: MCP holds no production mutation permissions. | PASS |
| G16-035 | Permission set includes VIEW/REQUEST/REVIEW/AUTHORIZE/EXECUTE and rollback counterparts. | §8–§16 | ProductionWriteGateway | P16-E125 | STRUCTURAL | Evidence P16-E125 exercises the architecture requirement with STRUCTURAL proof: Permission set includes VIEW/REQUEST/REVIEW/AUTHORIZE/EXECUTE and rollback counterparts. | PASS |
| G16-036 | Deterministic SHA-256 production fingerprint defined. | §8–§16 | ProductionChangeService | P16-E051, P16-E080 | INTEGRATION | Evidence P16-E051, P16-E080 exercises the architecture requirement with INTEGRATION proof: Deterministic SHA-256 production fingerprint defined. | PASS |
| G16-037 | Fingerprint binds Phase15/14/target/cell/parameter/values/profiles/window/policies/change-control/auth generation. | §8–§16 | ProductionChangeService | P16-E080 | INTEGRATION | Evidence P16-E080 exercises the architecture requirement with INTEGRATION proof: Fingerprint binds Phase15/14/target/cell/parameter/values/profiles/window/policies/change-control/auth generation. | PASS |
| G16-038 | Material binding change makes authorization STALE. | §8–§16 | ProductionChangeService | P16-E028 | BEHAVIORAL | Evidence P16-E028 exercises the architecture requirement with BEHAVIORAL proof: Material binding change makes authorization STALE. | PASS |
| G16-039 | No silent fingerprint regeneration or silent reauthorization. | §8–§16 | ProductionChangeService | P16-E126 | BEHAVIORAL | Evidence P16-E126 exercises the architecture requirement with BEHAVIORAL proof: No silent fingerprint regeneration or silent reauthorization. | PASS |
| G16-040 | ChangeControlReference required; not itself authorization. | §8–§16 | ProductionWriteGateway | P16-E127 | BEHAVIORAL | Evidence P16-E127 exercises the architecture requirement with BEHAVIORAL proof: ChangeControlReference required; not itself authorization. | PASS |
| G16-041 | Initial change-control system MANUAL; ServiceNow deferred. | §8–§16 | ProductionChangeService | P16-E128 | STRUCTURAL | Evidence P16-E128 exercises the architecture requirement with STRUCTURAL proof: Initial change-control system MANUAL; ServiceNow deferred. | PASS |
| G16-042 | ProductionExecutionGrant distinct from vendor credential. | §8–§16 | ProductionChangeService | P16-E129 | STRUCTURAL | Evidence P16-E129 exercises the architecture requirement with STRUCTURAL proof: ProductionExecutionGrant distinct from vendor credential. | PASS |
| G16-043 | Grant short-lived, single-use, fingerprint-bound, target-bound, fencing-bound. | §17–§34 | ProductionChangeService | P16-E042, P16-E083 | BEHAVIORAL, INTEGRATION | Evidence P16-E042, P16-E083 exercises the architecture requirement with BEHAVIORAL/INTEGRATION proof: Grant short-lived, single-use, fingerprint-bound, target-bound, fencing-bound. | PASS |
| G16-044 | Grant statuses include ISSUED / CONSUMED / EXPIRED / REVOKED. | §17–§34 | ProductionChangeService | P16-E130 | STRUCTURAL | Evidence P16-E130 exercises the architecture requirement with STRUCTURAL proof: Grant statuses include ISSUED / CONSUMED / EXPIRED / REVOKED. | PASS |
| G16-045 | No valid consumable grant ⇒ gateway deny. | §17–§34 | ProductionWriteGateway | P16-E131 | BEHAVIORAL | Evidence P16-E131 exercises the architecture requirement with BEHAVIORAL proof: No valid consumable grant ⇒ gateway deny. | PASS |
| G16-046 | Consumed grant not reusable. | §17–§34 | ProductionChangeService | P16-E060, P16-E081 | INTEGRATION | Evidence P16-E060, P16-E081 exercises the architecture requirement with INTEGRATION proof: Consumed grant not reusable. | PASS |
| G16-047 | Ambiguous outcome does not auto-issue another grant. | §17–§34 | ProductionChangeService | P16-E132 | BEHAVIORAL | Evidence P16-E132 exercises the architecture requirement with BEHAVIORAL proof: Ambiguous outcome does not auto-issue another grant. | PASS |
| G16-048 | Production lease scope = productionTargetId + cellId + parameter. | §17–§34 | ProductionChangeService | P16-E055, P16-E082 | INTEGRATION | Evidence P16-E055, P16-E082 exercises the architecture requirement with INTEGRATION proof: Production lease scope = productionTargetId + cellId + parameter. | PASS |
| G16-049 | Lease acquired before grant issuance / application pre-grant completion. | §17–§34 | ProductionChangeService | P16-E133 | INTEGRATION | Evidence P16-E133 exercises the architecture requirement with INTEGRATION proof: Lease acquired before grant issuance / application pre-grant completion. | PASS |
| G16-050 | Fencing protects SNIP pre-send authority only; does not cancel external accepted write. | §17–§34 | ProductionWriteGateway | P16-E082 | INTEGRATION | Evidence P16-E082 exercises the architecture requirement with INTEGRATION proof: Fencing protects SNIP pre-send authority only; does not cancel external accepted write. | PASS |
| G16-051 | No exact-once external network mutation claim. | §17–§34 | ProductionChangeService | P16-E170 | BEHAVIORAL | Evidence P16-E170 exercises the architecture requirement with BEHAVIORAL proof: No exact-once external network mutation claim. | PASS |
| G16-052 | No distributed ACID claim with vendor. | §17–§34 | ProductionChangeService | P16-E171 | BEHAVIORAL | Evidence P16-E171 exercises the architecture requirement with BEHAVIORAL proof: No distributed ACID claim with vendor. | PASS |
| G16-053 | Application pre-grant preflight checklist mandatory; UNKNOWN ⇒ DENY (no grant). | §17–§34 | ProductionChangeService | P16-E149 | INTEGRATION | Evidence P16-E149 exercises the architecture requirement with INTEGRATION proof: Application pre-grant preflight checklist mandatory; UNKNOWN ⇒ DENY (no grant). | PASS |
| G16-054 | Direct vendor expected-state observation mandatory before mutation. | §17–§34 | ProductionChangeService | P16-E150 | INTEGRATION | Evidence P16-E150 exercises the architecture requirement with INTEGRATION proof: Direct vendor expected-state observation mandatory before mutation. | PASS |
| G16-055 | Expected-state mismatch ⇒ zero mutation. | §17–§34 | ProductionWriteGateway | P16-E033 | BEHAVIORAL | Evidence P16-E033 exercises the architecture requirement with BEHAVIORAL proof: Expected-state mismatch ⇒ zero mutation. | PASS |
| G16-056 | Expected-state unknown/stale/unavailable/timeout ⇒ zero mutation. | §17–§34 | ProductionChangeService | P16-E034 | BEHAVIORAL | Evidence P16-E034 exercises the architecture requirement with BEHAVIORAL proof: Expected-state unknown/stale/unavailable/timeout ⇒ zero mutation. | PASS |
| G16-057 | ExpectedStateGuardStrength distinguishes ATOMIC vs READ_THEN_WRITE. | §17–§34 | ProductionChangeService | P16-E035 | BEHAVIORAL | Evidence P16-E035 exercises the architecture requirement with BEHAVIORAL proof: ExpectedStateGuardStrength distinguishes ATOMIC vs READ_THEN_WRITE. | PASS |
| G16-058 | No false atomicity claim without protocol support. | §17–§34 | ProductionChangeService | P16-E035 | BEHAVIORAL | Evidence P16-E035 exercises the architecture requirement with BEHAVIORAL proof: No false atomicity claim without protocol support. | PASS |
| G16-059 | Residual TOCTOU acknowledged for READ_THEN_WRITE. | §17–§34 | ProductionChangeService | P16-E036 | BEHAVIORAL | Evidence P16-E036 exercises the architecture requirement with BEHAVIORAL proof: Residual TOCTOU acknowledged for READ_THEN_WRITE. | PASS |
| G16-060 | Mutation outcomes distinguish NOT_SENT / REJECTED / VENDOR_ACCEPTED / OUTCOME_UNKNOWN. | §17–§34 | ProductionWriteGateway | P16-E151 | INTEGRATION | Evidence P16-E151 exercises the architecture requirement with INTEGRATION proof: Mutation outcomes distinguish NOT_SENT / REJECTED / VENDOR_ACCEPTED / OUTCOME_UNKNOWN. | PASS |
| G16-061 | Vendor accepted ≠ verified. | §17–§34 | ProductionChangeService | P16-E070, P16-E152 | INTEGRATION | Evidence P16-E070, P16-E152 exercises the architecture requirement with INTEGRATION proof: Vendor accepted ≠ verified. | PASS |
| G16-062 | Independent post-mutation vendor readback mandatory. | §17–§34 | ProductionChangeService | P16-E070, P16-E110, P16-E152 | EXTERNAL_CERTIFICATION, INTEGRATION | Evidence P16-E070, P16-E110, P16-E152 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INTEGRATION proof: Independent post-mutation vendor readback mandatory. | PASS |
| G16-063 | Only fresh observation matching desired may yield VERIFIED. | §17–§34 | ProductionChangeService | P16-E070, P16-E076 | INTEGRATION | Evidence P16-E070, P16-E076 exercises the architecture requirement with INTEGRATION proof: Only fresh observation matching desired may yield VERIFIED. | PASS |
| G16-064 | PRODUCTION_VERIFIED distinct from CANONICAL_RECONCILED. | §17–§34 | ProductionChangeService | P16-E046, P16-E084 | INTEGRATION | Evidence P16-E046, P16-E084 exercises the architecture requirement with INTEGRATION proof: PRODUCTION_VERIFIED distinct from CANONICAL_RECONCILED. | PASS |
| G16-065 | Phase 16 must not directly mutate canonical radio_configuration. | §17–§34 | ProductionWriteGateway | P16-E046 | INTEGRATION | Evidence P16-E046 exercises the architecture requirement with INTEGRATION proof: Phase 16 must not directly mutate canonical radio_configuration. | PASS |
| G16-066 | May emit NETWORK_SYNCHRONIZATION_REQUIRED after verified production change. | §17–§34 | ProductionChangeService | P16-E084 | INTEGRATION | Evidence P16-E084 exercises the architecture requirement with INTEGRATION proof: May emit NETWORK_SYNCHRONIZATION_REQUIRED after verified production change. | PASS |
| G16-067 | Phase 12 remains reconciliation authority. | §17–§34 | ProductionChangeService | P16-E046, P16-E084 | INTEGRATION | Evidence P16-E046, P16-E084 exercises the architecture requirement with INTEGRATION proof: Phase 12 remains reconciliation authority. | PASS |
| G16-068 | Ambiguous desired observation path defined (VERIFIED). | §17–§34 | ProductionChangeService | P16-E069 | INTEGRATION | Evidence P16-E069 exercises the architecture requirement with INTEGRATION proof: Ambiguous desired observation path defined (VERIFIED). | PASS |
| G16-069 | Ambiguous expected observation ⇒ safe stop; no blind retry; new auth required. | §17–§34 | ProductionChangeService | P16-E153 | INTEGRATION | Evidence P16-E153 exercises the architecture requirement with INTEGRATION proof: Ambiguous expected observation ⇒ safe stop; no blind retry; new auth required. | PASS |
| G16-070 | Ambiguous third-state ⇒ MANUAL_INTERVENTION_REQUIRED. | §17–§34 | ProductionWriteGateway | P16-E154 | INTEGRATION | Evidence P16-E154 exercises the architecture requirement with INTEGRATION proof: Ambiguous third-state ⇒ MANUAL_INTERVENTION_REQUIRED. | PASS |
| G16-071 | Unavailable observation ⇒ PRODUCTION_OUTCOME_UNRESOLVED; no retry. | §17–§34 | ProductionChangeService | P16-E155 | INTEGRATION | Evidence P16-E155 exercises the architecture requirement with INTEGRATION proof: Unavailable observation ⇒ PRODUCTION_OUTCOME_UNRESOLVED; no retry. | PASS |
| G16-072 | No blind forward mutation retry after MAY_HAVE_SENT. | §17–§34 | ProductionChangeService | P16-E037, P16-E069, P16-E085 | BEHAVIORAL, INTEGRATION | Evidence P16-E037, P16-E069, P16-E085 exercises the architecture requirement with BEHAVIORAL/INTEGRATION proof: No blind forward mutation retry after MAY_HAVE_SENT. | PASS |
| G16-073 | One forward attempt unless separately authorized new execution. | §17–§34 | ProductionChangeService | P16-E060, P16-E064, P16-E081 | INTEGRATION | Evidence P16-E060, P16-E064, P16-E081 exercises the architecture requirement with INTEGRATION proof: One forward attempt unless separately authorized new execution. | PASS |
| G16-074 | Verification failure ⇒ RECOVERY_REQUIRED, not automatic rollback. | §17–§34 | ProductionChangeService | P16-E156 | INTEGRATION | Evidence P16-E156 exercises the architecture requirement with INTEGRATION proof: Verification failure ⇒ RECOVERY_REQUIRED, not automatic rollback. | PASS |
| G16-075 | Automatic rollback prohibited. | §17–§34 | ProductionWriteGateway | P16-E156, P16-E157 | BEHAVIORAL, INTEGRATION | Evidence P16-E156, P16-E157 exercises the architecture requirement with BEHAVIORAL/INTEGRATION proof: Automatic rollback prohibited. | PASS |
| G16-076 | Rollback requires request/review/authorize/execute separation. | §17–§34 | ProductionChangeService | P16-E077, P16-E109 | EXTERNAL_CERTIFICATION, INTEGRATION | Evidence P16-E077, P16-E109 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INTEGRATION proof: Rollback requires request/review/authorize/execute separation. | PASS |
| G16-077 | Rollback fingerprint required. | §17–§34 | ProductionChangeService | P16-E077 | INTEGRATION | Evidence P16-E077 exercises the architecture requirement with INTEGRATION proof: Rollback fingerprint required. | PASS |
| G16-078 | Rollback expected-state guard mandatory. | §17–§34 | ProductionChangeService | P16-E079 | INTEGRATION | Evidence P16-E079 exercises the architecture requirement with INTEGRATION proof: Rollback expected-state guard mandatory. | PASS |
| G16-079 | Rollback value from persisted Phase 14 state only. | §17–§34 | ProductionChangeService | P16-E077 | INTEGRATION | Evidence P16-E077 exercises the architecture requirement with INTEGRATION proof: Rollback value from persisted Phase 14 state only. | PASS |
| G16-080 | Rollback ambiguous outcome policy defined; no blind rollback retry. | §17–§34 | ProductionWriteGateway | P16-E077, P16-E173 | INTEGRATION | Evidence P16-E077, P16-E173 exercises the architecture requirement with INTEGRATION proof: Rollback ambiguous outcome policy defined; no blind rollback retry. | PASS |
| G16-081 | No emergency governance bypass in Phase 16. | §17–§34 | ProductionChangeService | P16-E157 | BEHAVIORAL | Evidence P16-E157 exercises the architecture requirement with BEHAVIORAL proof: No emergency governance bypass in Phase 16. | PASS |
| G16-082 | Cancellation before mutation only; no false cancel after MAY_HAVE_SENT. | §17–§34 | ProductionChangeService | P16-E142 | BEHAVIORAL | Evidence P16-E142 exercises the architecture requirement with BEHAVIORAL proof: Cancellation before mutation only; no false cancel after MAY_HAVE_SENT. | PASS |
| G16-083 | Window permits but does not trigger execution. | §17–§34 | ProductionChangeService | P16-E139 | BEHAVIORAL | Evidence P16-E139 exercises the architecture requirement with BEHAVIORAL proof: Window permits but does not trigger execution. | PASS |
| G16-084 | Application and gateway preflights re-check window. | §17–§34 | ProductionChangeService | P16-E032, P16-E158 | BEHAVIORAL, INTEGRATION | Evidence P16-E032, P16-E158 exercises the architecture requirement with BEHAVIORAL/INTEGRATION proof: Application and gateway preflights re-check window. | PASS |
| G16-085 | No scheduler-driven production execution. | §18–§35 | ProductionChangeService | P16-E019 | STRUCTURAL | Evidence P16-E019 exercises the architecture requirement with STRUCTURAL proof: No scheduler-driven production execution. | PASS |
| G16-086 | No event-driven production execution. | §18–§35 | ProductionChangeService | P16-E020 | STRUCTURAL | Evidence P16-E020 exercises the architecture requirement with STRUCTURAL proof: No event-driven production execution. | PASS |
| G16-087 | Creation/authorization/window-open do not execute. | §18–§35 | ProductionChangeService | P16-E139 | BEHAVIORAL | Evidence P16-E139 exercises the architecture requirement with BEHAVIORAL proof: Creation/authorization/window-open do not execute. | PASS |
| G16-088 | Only explicit execute endpoints may cross mutation boundary. | §18–§35 | ProductionChangeService | P16-E076 | INTEGRATION | Evidence P16-E076 exercises the architecture requirement with INTEGRATION proof: Only explicit execute endpoints may cross mutation boundary. | PASS |
| G16-089 | ProductionBlastRadiusPolicy hard limits = 1/1/1. | §18–§35 | ProductionChangeService | P16-E064, P16-E085, P16-E119, P16-E143 | BEHAVIORAL, INTEGRATION | Evidence P16-E064, P16-E085, P16-E119, P16-E143 exercises the architecture requirement with BEHAVIORAL/INTEGRATION proof: ProductionBlastRadiusPolicy hard limits = 1/1/1. | PASS |
| G16-090 | Rate / unknown / verification failure suspension thresholds required. | §18–§35 | ProductionWriteGateway | P16-E144 | BEHAVIORAL | Evidence P16-E144 exercises the architecture requirement with BEHAVIORAL proof: Rate / unknown / verification failure suspension thresholds required. | PASS |
| G16-091 | Automatic safety suspension allowed; automatic resume forbidden. | §18–§35 | ProductionChangeService | P16-E106, P16-E121, P16-E144, P16-E145 | BEHAVIORAL, EXTERNAL_CERTIFICATION | Evidence P16-E106, P16-E121, P16-E144, P16-E145 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION proof: Automatic safety suspension allowed; automatic resume forbidden. | PASS |
| G16-092 | Global kill switch multi-gate; no single boolean enable. | §18–§35 | ProductionChangeService | P16-E030, P16-E105 | BEHAVIORAL, EXTERNAL_CERTIFICATION | Evidence P16-E030, P16-E105 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION proof: Global kill switch multi-gate; no single boolean enable. | PASS |
| G16-093 | Target kill switch / disable / suspend deny execution. | §18–§35 | ProductionChangeService | P16-E030, P16-E031 | BEHAVIORAL | Evidence P16-E030, P16-E031 exercises the architecture requirement with BEHAVIORAL proof: Target kill switch / disable / suspend deny execution. | PASS |
| G16-094 | Credential lifecycle late resolution; secrets never persisted. | §18–§35 | ProductionChangeService | P16-E072, P16-E073 | INTEGRATION | Evidence P16-E072, P16-E073 exercises the architecture requirement with INTEGRATION proof: Credential lifecycle late resolution; secrets never persisted. | PASS |
| G16-095 | Network egress restriction to approved ENM endpoints; no 0.0.0.0/0 write egress. | §18–§35 | ProductionChangeService | P16-E088, P16-E089, P16-E103 | EXTERNAL_CERTIFICATION, INFRASTRUCTURE | Evidence P16-E088, P16-E089, P16-E103 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INFRASTRUCTURE proof: Network egress restriction to approved ENM endpoints; no 0.0.0.0/0 write egress. | PASS |
| G16-096 | TLS mandatory; hostname verification; trust-all forbidden. | §18–§35 | ProductionChangeService | P16-E090, P16-E091, P16-E104 | EXTERNAL_CERTIFICATION, INFRASTRUCTURE | Evidence P16-E090, P16-E091, P16-E104 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INFRASTRUCTURE proof: TLS mandatory; hostname verification; trust-all forbidden. | PASS |
| G16-097 | mTLS private keys only via secure credential mechanism when required. | §18–§35 | ProductionChangeService | P16-E091, P16-E104 | EXTERNAL_CERTIFICATION, INFRASTRUCTURE | Evidence P16-E091, P16-E104 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INFRASTRUCTURE proof: mTLS private keys only via secure credential mechanism when required. | PASS |
| G16-098 | Durable audit with actorPrincipalId/fingerprint/target/outcome coverage; no secrets. | §18–§35 | ProductionChangeService | P16-E049, P16-E066 | BEHAVIORAL, INTEGRATION | Evidence P16-E049, P16-E066 exercises the architecture requirement with BEHAVIORAL/INTEGRATION proof: Durable audit with actorPrincipalId/fingerprint/target/outcome coverage; no secrets. | PASS |
| G16-099 | Tamper-evident previousEventHash / eventHash chain with defined scope. | §18–§35 | ProductionChangeService | P16-E066, P16-E067, P16-E108 | EXTERNAL_CERTIFICATION, INTEGRATION | Evidence P16-E066, P16-E067, P16-E108 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INTEGRATION proof: Tamper-evident previousEventHash / eventHash chain with defined scope. | PASS |
| G16-100 | Critical outcome evidence survives outer failure via independent persistence. | §18–§35 | ProductionChangeService | P16-E071, P16-E168 | INTEGRATION | Evidence P16-E071, P16-E168 exercises the architecture requirement with INTEGRATION proof: Critical outcome evidence survives outer failure via independent persistence. | PASS |
| G16-101 | Failure taxonomy non-generic; no catch-and-retry. | §18–§35 | ProductionChangeService | P16-E159 | BEHAVIORAL | Evidence P16-E159 exercises the architecture requirement with BEHAVIORAL proof: Failure taxonomy non-generic; no catch-and-retry. | PASS |
| G16-102 | Low-cardinality metrics only; forbidden high-cardinality labels. | §18–§35 | ProductionChangeService | P16-E050 | BEHAVIORAL | Evidence P16-E050 exercises the architecture requirement with BEHAVIORAL proof: Low-cardinality metrics only; forbidden high-cardinality labels. | PASS |
| G16-103 | Target health separate from application readiness. | §18–§35 | ProductionChangeService | P16-E121 | BEHAVIORAL | Evidence P16-E121 exercises the architecture requirement with BEHAVIORAL proof: Target health separate from application readiness. | PASS |
| G16-104 | Agent execution not authorized. | §18–§35 | ProductionChangeService | P16-E017, P16-E043 | BEHAVIORAL, STRUCTURAL | Evidence P16-E017, P16-E043 exercises the architecture requirement with BEHAVIORAL/STRUCTURAL proof: Agent execution not authorized. | PASS |
| G16-105 | MCP execution not authorized. | §18–§35 | ProductionChangeService | P16-E018, P16-E044 | BEHAVIORAL, STRUCTURAL | Evidence P16-E018, P16-E044 exercises the architecture requirement with BEHAVIORAL/STRUCTURAL proof: MCP execution not authorized. | PASS |
| G16-106 | Closed-loop optimization not authorized. | §18–§35 | ProductionChangeService | P16-E160 | BEHAVIORAL | Evidence P16-E160 exercises the architecture requirement with BEHAVIORAL proof: Closed-loop optimization not authorized. | PASS |
| G16-107 | Nokia write deferred / not implemented. | §18–§35 | ProductionChangeService | P16-E146 | BEHAVIORAL | Evidence P16-E146 exercises the architecture requirement with BEHAVIORAL proof: Nokia write deferred / not implemented. | PASS |
| G16-108 | Default CI Azure/vendor/credential independent. | §18–§35 | ProductionChangeService | P16-E147 | BEHAVIORAL | Evidence P16-E147 exercises the architecture requirement with BEHAVIORAL proof: Default CI Azure/vendor/credential independent. | PASS |
| G16-109 | Certification levels 0–4 defined; code ≠ Level 4. | §18–§35 | ProductionChangeService | P16-E096, P16-E097, P16-E172 | BEHAVIORAL, EXTERNAL_CERTIFICATION | Evidence P16-E096, P16-E097, P16-E172 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION proof: Certification levels 0–4 defined; code ≠ Level 4. | PASS |
| G16-110 | V17 proposed only; not created by architecture authoring. | §18–§35 | ProductionWriteGateway | P16-E134 | BEHAVIORAL | Evidence P16-E134 exercises the architecture requirement with BEHAVIORAL proof: V17 proposed only; not created by architecture authoring. | PASS |
| G16-111 | Phase 16 implementation not started by this document. | §18–§35 | ProductionChangeService | P16-E161 | STRUCTURAL | Evidence P16-E161 exercises the architecture requirement with STRUCTURAL proof: Phase 16 implementation not started by this document. | PASS |
| G16-112 | Future evidence catalog concrete and gate-mapped in implementation specification. | §18–§35 | ProductionChangeService | P16-E093, P16-E094, P16-E148 | BEHAVIORAL, INFRASTRUCTURE | Evidence P16-E093, P16-E094, P16-E148 exercises the architecture requirement with BEHAVIORAL/INFRASTRUCTURE proof: Future evidence catalog concrete and gate-mapped in implementation specification. | PASS |
| G16-113 | High-risk gates require behavioral/integration evidence, not docs-only. | §18–§35 | ProductionChangeService | P16-E093, P16-E148 | BEHAVIORAL, INFRASTRUCTURE | Evidence P16-E093, P16-E148 exercises the architecture requirement with BEHAVIORAL/INFRASTRUCTURE proof: High-risk gates require behavioral/integration evidence, not docs-only. | PASS |
| G16-114 | Threat model covers session theft through gateway compromise and grant/rate/kill races (T01–T48). | §18–§35 | ProductionChangeService | P16-E148 | BEHAVIORAL | Evidence P16-E148 exercises the architecture requirement with BEHAVIORAL proof: Threat model covers session theft through gateway compromise and grant/rate/kill races (T01–T48). | PASS |
| G16-115 | Write gateway returns sanitized structured evidence only. | §18–§35 | ProductionChangeService | P16-E049 | BEHAVIORAL | Evidence P16-E049 exercises the architecture requirement with BEHAVIORAL proof: Write gateway returns sanitized structured evidence only. | PASS |
| G16-116 | ProductionExecutionEvidence excludes credentials and secret-bearing payloads. | §18–§35 | ProductionChangeService | P16-E049 | BEHAVIORAL | Evidence P16-E049 exercises the architecture requirement with BEHAVIORAL proof: ProductionExecutionEvidence excludes credentials and secret-bearing payloads. | PASS |
| G16-117 | Authoritative durable production grant store required. | §18–§35 | ProductionChangeService | P16-E051, P16-E130 | INTEGRATION, STRUCTURAL | Evidence P16-E051, P16-E130 exercises the architecture requirement with INTEGRATION/STRUCTURAL proof: Authoritative durable production grant store required. | PASS |
| G16-118 | Gateway must not trust caller-supplied self-contained grant as sole authority. | §18–§35 | ProductionChangeService | P16-E162 | INTEGRATION | Evidence P16-E162 exercises the architecture requirement with INTEGRATION proof: Gateway must not trust caller-supplied self-contained grant as sole authority. | PASS |
| G16-119 | App→gateway request carries grantId/correlation only; no mutation payload authority. | §18–§35 | ProductionChangeService | P16-E163 | INTEGRATION | Evidence P16-E163 exercises the architecture requirement with INTEGRATION proof: App→gateway request carries grantId/correlation only; no mutation payload authority. | PASS |
| G16-120 | Atomic conditional ISSUED→CONSUMED before vendor send. | §18–§35 | ProductionChangeService | P16-E051, P16-E052, P16-E053, P16-E054, P16-E055, P16-E056, P16-E057, P16-E058, P16-E059 | INTEGRATION | Evidence P16-E051, P16-E052, P16-E053, P16-E054, P16-E055, P16-E056, P16-E057, P16-E058, P16-E059 exercises the architecture requirement with INTEGRATION proof: Atomic conditional ISSUED→CONSUMED before vendor send. | PASS |
| G16-121 | Concurrent grant consume: exactly one success; others deny. | §18–§35 | ProductionChangeService | P16-E051, P16-E060, P16-E064, P16-E078 | INTEGRATION | Evidence P16-E051, P16-E060, P16-E064, P16-E078 exercises the architecture requirement with INTEGRATION proof: Concurrent grant consume: exactly one success; others deny. | PASS |
| G16-122 | Consumed grant never automatically resets to ISSUED. | §18–§35 | ProductionChangeService | P16-E062 | INTEGRATION | Evidence P16-E062 exercises the architecture requirement with INTEGRATION proof: Consumed grant never automatically resets to ISSUED. | PASS |
| G16-123 | Grant timeout/crash matrix defined and normative. | §18–§35 | ProductionChangeService | P16-E062, P16-E063, P16-E141 | INTEGRATION | Evidence P16-E062, P16-E063, P16-E141 exercises the architecture requirement with INTEGRATION proof: Grant timeout/crash matrix defined and normative. | PASS |
| G16-124 | Consume-before-send ordering mandatory. | §18–§35 | ProductionChangeService | P16-E061, P16-E062 | INTEGRATION | Evidence P16-E061, P16-E062 exercises the architecture requirement with INTEGRATION proof: Consume-before-send ordering mandatory. | PASS |
| G16-125 | Application vs gateway preflight split defined. | §18–§35 | ProductionWriteGateway | P16-E164 | INTEGRATION | Evidence P16-E164 exercises the architecture requirement with INTEGRATION proof: Application vs gateway preflight split defined. | PASS |
| G16-126 | Gateway final pre-mutation preflight mandatory and independent. | §18–§35 | ProductionChangeService | P16-E165 | INTEGRATION | Evidence P16-E165 exercises the architecture requirement with INTEGRATION proof: Gateway final pre-mutation preflight mandatory and independent. | PASS |
| G16-127 | Gateway independently enforces global kill switch before mutation. | §18–§35 | ProductionChangeService | P16-E030 | BEHAVIORAL | Evidence P16-E030 exercises the architecture requirement with BEHAVIORAL proof: Gateway independently enforces global kill switch before mutation. | PASS |
| G16-128 | Gateway denies if target suspended/disabled after grant issuance. | §18–§35 | ProductionChangeService | P16-E031 | BEHAVIORAL | Evidence P16-E031 exercises the architecture requirement with BEHAVIORAL proof: Gateway denies if target suspended/disabled after grant issuance. | PASS |
| G16-129 | Gateway/shared durable rate and blast-radius enforcement required. | §18–§35 | ProductionChangeService | P16-E041, P16-E065, P16-E107 | BEHAVIORAL, EXTERNAL_CERTIFICATION, INTEGRATION | Evidence P16-E041, P16-E065, P16-E107 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION/INTEGRATION proof: Gateway/shared durable rate and blast-radius enforcement required. | PASS |
| G16-130 | Unknown limiter state denies. | §18–§35 | ProductionChangeService | P16-E065 | INTEGRATION | Evidence P16-E065 exercises the architecture requirement with INTEGRATION proof: Unknown limiter state denies. | PASS |
| G16-131 | Gateway revalidates change-control at consume/pre-send. | §18–§35 | ProductionChangeService | P16-E032, P16-E111 | BEHAVIORAL, EXTERNAL_CERTIFICATION | Evidence P16-E032, P16-E111 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION proof: Gateway revalidates change-control at consume/pre-send. | PASS |
| G16-132 | Invalid/expired/unknown change-control after grant ⇒ zero mutation. | §18–§35 | ProductionChangeService | P16-E032 | BEHAVIORAL | Evidence P16-E032 exercises the architecture requirement with BEHAVIORAL proof: Invalid/expired/unknown change-control after grant ⇒ zero mutation. | PASS |
| G16-133 | Execution state ownership (app vs shared durable protocol) defined. | §37–§51 | ProductionChangeService | P16-E166 | INTEGRATION | Evidence P16-E166 exercises the architecture requirement with INTEGRATION proof: Execution state ownership (app vs shared durable protocol) defined. | PASS |
| G16-134 | Durable ProductionGatewayAttempt / ProductionGatewayEvidence (or equivalent) required. | §37–§51 | ProductionChangeService | P16-E061, P16-E063 | INTEGRATION | Evidence P16-E061, P16-E063 exercises the architecture requirement with INTEGRATION proof: Durable ProductionGatewayAttempt / ProductionGatewayEvidence (or equivalent) required. | PASS |
| G16-135 | Application must not fabricate VENDOR_ACCEPTED / VERIFIED / ROLLED_BACK without durable gateway evidence. | §37–§51 | ProductionChangeService | P16-E039 | BEHAVIORAL | Evidence P16-E039 exercises the architecture requirement with BEHAVIORAL proof: Application must not fabricate VENDOR_ACCEPTED / VERIFIED / ROLLED_BACK without durable gateway evidence. | PASS |
| G16-136 | Vendor I/O persistence ordering defined without distributed ACID claim. | §37–§51 | ProductionChangeService | P16-E071 | INTEGRATION | Evidence P16-E071 exercises the architecture requirement with INTEGRATION proof: Vendor I/O persistence ordering defined without distributed ACID claim. | PASS |
| G16-137 | Distributed failure table defined; no blind second mutation. | §37–§51 | ProductionChangeService | P16-E037, P16-E085 | BEHAVIORAL, INTEGRATION | Evidence P16-E037, P16-E085 exercises the architecture requirement with BEHAVIORAL/INTEGRATION proof: Distributed failure table defined; no blind second mutation. | PASS |
| G16-138 | Stable actorPrincipalId required for SoD comparisons. | §37–§51 | ProductionChangeService | P16-E025 | BEHAVIORAL | Evidence P16-E025 exercises the architecture requirement with BEHAVIORAL proof: Stable actorPrincipalId required for SoD comparisons. | PASS |
| G16-139 | Display names not authoritative SoD keys. | §37–§51 | ProductionChangeService | P16-E025 | BEHAVIORAL | Evidence P16-E025 exercises the architecture requirement with BEHAVIORAL proof: Display names not authoritative SoD keys. | PASS |
| G16-140 | Target administration via privileged ADMINISTER_PRODUCTION_TARGET (or equivalent). | §37–§51 | ProductionWriteGateway | P16-E167 | BEHAVIORAL | Evidence P16-E167 exercises the architecture requirement with BEHAVIORAL proof: Target administration via privileged ADMINISTER_PRODUCTION_TARGET (or equivalent). | PASS |
| G16-141 | Execution-significant target change invalidates/stales auth and unconsumed grants. | §37–§51 | ProductionChangeService | P16-E029 | BEHAVIORAL | Evidence P16-E029 exercises the architecture requirement with BEHAVIORAL proof: Execution-significant target change invalidates/stales auth and unconsumed grants. | PASS |
| G16-142 | Write credential current-version resolution; no old-version fallback. | §37–§51 | ProductionChangeService | P16-E022, P16-E074 | INTEGRATION, STRUCTURAL | Evidence P16-E022, P16-E074 exercises the architecture requirement with INTEGRATION/STRUCTURAL proof: Write credential current-version resolution; no old-version fallback. | PASS |
| G16-143 | Write credentials never reuse read credentials. | §37–§51 | ProductionChangeService | P16-E074, P16-E075 | INTEGRATION | Evidence P16-E074, P16-E075 exercises the architecture requirement with INTEGRATION proof: Write credentials never reuse read credentials. | PASS |
| G16-144 | Level-4 explicit external evidence checklist required; target/profile-specific. | §37–§51 | ProductionChangeService | P16-E098, P16-E172 | BEHAVIORAL, EXTERNAL_CERTIFICATION | Evidence P16-E098, P16-E172 exercises the architecture requirement with BEHAVIORAL/EXTERNAL_CERTIFICATION proof: Level-4 explicit external evidence checklist required; target/profile-specific. | PASS |
| G16-145 | First real Ericsson integration MUST be vendor lab/test. | §37–§51 | ProductionChangeService | P16-E095 | EXTERNAL_CERTIFICATION | Evidence P16-E095 exercises the architecture requirement with EXTERNAL_CERTIFICATION proof: First real Ericsson integration MUST be vendor lab/test. | PASS |
| G16-146 | Audit-chain genesis, deterministic serialization, concurrency serialization defined. | §37–§51 | ProductionChangeService | P16-E066 | INTEGRATION | Evidence P16-E066 exercises the architecture requirement with INTEGRATION proof: Audit-chain genesis, deterministic serialization, concurrency serialization defined. | PASS |
| G16-147 | Audit gap/mismatch marks INVALID, alerts, and blocks new mutation as policy requires. | §37–§51 | ProductionChangeService | P16-E067, P16-E068 | INTEGRATION | Evidence P16-E067, P16-E068 exercises the architecture requirement with INTEGRATION proof: Audit gap/mismatch marks INVALID, alerts, and blocks new mutation as policy requires. | PASS |
| G16-148 | Critical mutation evidence survives audit subsystem failure after MAY_HAVE_SENT. | §37–§51 | ProductionChangeService | P16-E168 | INTEGRATION | Evidence P16-E168 exercises the architecture requirement with INTEGRATION proof: Critical mutation evidence survives audit subsystem failure after MAY_HAVE_SENT. | PASS |
| G16-149 | Strict PRE-SEND vs MAY_HAVE_SENT definitions. | §37–§51 | ProductionChangeService | P16-E037, P16-E038 | BEHAVIORAL | Evidence P16-E037, P16-E038 exercises the architecture requirement with BEHAVIORAL proof: Strict PRE-SEND vs MAY_HAVE_SENT definitions. | PASS |
| G16-150 | Gateway compromise residual risk acknowledged; defense-in-depth required. | §37–§51 | ProductionChangeService | P16-E114 | EXTERNAL_CERTIFICATION | Evidence P16-E114 exercises the architecture requirement with EXTERNAL_CERTIFICATION proof: Gateway compromise residual risk acknowledged; defense-in-depth required. | PASS |
| G16-151 | Fresh deployment fail-closed to zero production mutation capability. | §37–§51 | ProductionChangeService | P16-E048, P16-E092 | BEHAVIORAL, INFRASTRUCTURE | Evidence P16-E048, P16-E092 exercises the architecture requirement with BEHAVIORAL/INFRASTRUCTURE proof: Fresh deployment fail-closed to zero production mutation capability. | PASS |
| G16-152 | Phase 16 infra deliverables scoped; production secrets/endpoints/transport/Level-4 not defaulted by code. | §37–§51 | ProductionChangeService | P16-E086, P16-E087, P16-E113 | EXTERNAL_CERTIFICATION, INFRASTRUCTURE | Evidence P16-E086, P16-E087, P16-E113 exercises the architecture requirement with EXTERNAL_CERTIFICATION/INFRASTRUCTURE proof: Phase 16 infra deliverables scoped; production secrets/endpoints/transport/Level-4 not defaulted by code. | PASS |
| G16-153 | MANUAL change-control validatedByPrincipalId and requester self-validation restriction defined. | §37–§51 | ProductionChangeService | P16-E169 | BEHAVIORAL | Evidence P16-E169 exercises the architecture requirement with BEHAVIORAL proof: MANUAL change-control validatedByPrincipalId and requester self-validation restriction defined. | PASS |
| G16-154 | HTTP response is convenience; durable evidence is authority. | §37–§51 | ProductionChangeService | P16-E040 | BEHAVIORAL | Evidence P16-E040 exercises the architecture requirement with BEHAVIORAL proof: HTTP response is convenience; durable evidence is authority. | PASS |

---

## 55. Implementation evidence catalog (173 items)

**Catalog rule (B16-S-01 / B16-S-02):** Evidence IDs are assigned by semantic meaning, not ordinal coincidence with gate numbers. Generic placeholder evidence is forbidden. Generic placeholder count **MUST** remain 0.

| ID | Title | Type | Gate(s) | Threat(s) | Requirement | Component | Setup | Action | Expected durable state | Mut# | Retry# | Reason | Expected result | CI |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P16-E001 | App package api under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.api exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/api | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_api_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E002 | App package domain under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.domain exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/domain | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_domain_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E003 | App package entity under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.entity exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/entity | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_entity_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E004 | App package repository under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.repository exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/repository | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_repository_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E005 | App package service under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.service exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/service | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_service_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E006 | App package security under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.security exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/security | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_security_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E007 | App package policy under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.policy exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/policy | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_policy_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E008 | App package audit under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.audit exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/audit | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_audit_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E009 | App package metrics under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.metrics exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/metrics | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_metrics_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E010 | App package adapter under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.adapter exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/adapter | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_adapter_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E011 | App package config under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.config exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/config | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_config_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E012 | App package exception under productionchange | STRUCTURAL | G16-004, G16-006 | — | Phase 16 governance package com.simba.snip.npo.productionchange.exception exists in snip-npo-app and contains no vendor write transport classes. | snip-npo-app/.../productionchange/exception | Module layout from §2 applied | ProductionChangeModuleBoundaryTest.package_exception_under_productionchange | N/A (structural) | N/A | N/A | — | Package present; zero write-transport types in app module | DEFAULT_CI |
| P16-E013 | Parent POM aggregator with three modules | STRUCTURAL | G16-003, G16-006 | — | Root pom packaging=pom lists exactly snip-npo-app, production-change-protocol, production-write-gateway. | pom.xml | Multi-module layout §2 | ProductionChangeModuleBoundaryTest.parentAggregatorModules | N/A | N/A | N/A | — | Exactly three Phase 16 modules declared | DEFAULT_CI |
| P16-E014 | Gateway independent Spring Boot main | STRUCTURAL | G16-003, G16-006 | — | production-write-gateway has ProductionWriteGatewayApplication and is not scanned as a bean by snip-npo-app. | production-write-gateway/ | Separate gateway module | ProductionWriteGatewayIsolationTest.independentMainClass | N/A | N/A | N/A | — | Separate executable main; not @Service in app | DEFAULT_CI |
| P16-E015 | Protocol module has no Key Vault dependency | STRUCTURAL | G16-008, G16-009 | — | production-change-protocol POM excludes Azure Key Vault and write-credential dependencies. | production-change-protocol/pom.xml | Protocol module present | ProductionChangeDependencyRuleTest.protocolNoKeyVault | N/A | N/A | N/A | — | No azure-security-keyvault-secrets dependency | DEFAULT_CI |
| P16-E016 | EnmTransport read-only contract | STRUCTURAL | G16-010, G16-014 | — | Phase 11 EnmTransport declares no write/mutate/apply methods and Phase 16 introduces no write dependency on it. | EnmTransport.java | Phase 11 baseline present | EnmTransportReadOnlyContractTest.noWriteMethods | N/A | N/A | N/A | — | Zero write method signatures | DEFAULT_CI |
| P16-E017 | Agent cannot reference production gateway adapter | STRUCTURAL | G16-033, G16-104 | T05 | Agent packages compile without imports of production-write-gateway mutation adapters. | agent/ | Agent layer present | AgentProductionMutationIsolationTest.noWriteAdapterImports | N/A | 0 | 0 | — | Compile-time boundary holds | DEFAULT_CI |
| P16-E018 | MCP registry has no production mutation tool | STRUCTURAL | G16-034, G16-105 | T06 | MCP tool registry excludes production execute/apply/rollback mutation tools. | mcp/ | MCP registry present | McpProductionMutationIsolationTest.noProductionMutationTools | N/A | 0 | 0 | — | No production mutation tool registered | DEFAULT_CI |
| P16-E019 | Scheduler has no production execute trigger | STRUCTURAL | G16-085 | T30 | No @Scheduled method invokes production execute/rollback services. | src/main/java | Scheduler code present | SchedulerProductionMutationIsolationTest.noProductionExecuteScheduled | N/A | 0 | 0 | — | Zero scheduled production execute calls | DEFAULT_CI |
| P16-E020 | Event consumers cannot invoke production execute | STRUCTURAL | G16-086 | T29 | Kafka/event listeners do not call production execute endpoints or services. | src/main/java | Event consumers present | EventConsumerProductionMutationIsolationTest.noProductionExecuteListener | N/A | 0 | 0 | — | Zero event-driven production mutation paths | DEFAULT_CI |
| P16-E021 | App module excludes write credential provider | STRUCTURAL | G16-005, G16-008 | T14 | snip-npo-app has no ProductionWriteCredentialProvider / write Key Vault resolution bean. | snip-npo-app | App module classpath | ProductionCredentialIsolationTest.appNoWriteCredentialProvider | N/A | 0 | 0 | — | No write credential provider in app classpath | DEFAULT_CI |
| P16-E022 | Gateway owns write credential resolution service | STRUCTURAL | G16-008, G16-142 | T14 | ProductionCredentialResolutionService exists only in production-write-gateway. | production-write-gateway | Gateway module present | ProductionCredentialIsolationTest.gatewayOwnsCredentialService | N/A | N/A | N/A | — | Credential service only in gateway module | DEFAULT_CI |
| P16-E023 | Requester cannot authorize | BEHAVIORAL | G16-030 | T01 | Requester principal must not authorize the same production change. | ProductionChangeSoDTest | Distinct requester/authorizer | requesterCannotAuthorize | No AUTHORIZED transition | 0 | 0 | PRODUCTION_SOD_VIOLATION | 403 SoD deny | DEFAULT_CI |
| P16-E024 | Authorizer cannot execute | BEHAVIORAL | G16-031 | T04 | Authorizer principal must not execute the same production change. | ProductionChangeSoDTest | Authorized; authorizer executes | authorizerCannotExecute | Execute denied | 0 | 0 | PRODUCTION_SOD_VIOLATION | 403 SoD deny | DEFAULT_CI |
| P16-E025 | Display name cannot satisfy SoD | BEHAVIORAL | G16-138, G16-139 | T45 | SoD comparisons use actorPrincipalId only; display names are non-authoritative. | ProductionChangeSoDTest | Shared display names | displayNameCannotSatisfySoD | SoD deny | 0 | 0 | PRODUCTION_SOD_VIOLATION | Deny weak identity comparison | DEFAULT_CI |
| P16-E026 | Create rejects mutation fields | BEHAVIORAL | G16-026 | T02 | Create API rejects caller-supplied cellId/parameter/value mutation fields. | ProductionChangeApiTest | POST create with cellId | createRejectsCallerMutationFields | No production_change row | 0 | 0 | PRODUCTION_INVALID_REQUEST | 400; no row | DEFAULT_CI |
| P16-E027 | Execute rejects mutation override | BEHAVIORAL | G16-026 | T02 | Execute DTO cannot override governed mutation values. | ProductionChangeApiTest | Execute with override | executeRejectsMutationOverride | Unchanged binding | 0 | 0 | PRODUCTION_INVALID_REQUEST | 400/409 | DEFAULT_CI |
| P16-E028 | Stale authorization blocks grant | BEHAVIORAL | G16-038 | T09 | Fingerprint material change marks authorization STALE and blocks grant issuance. | ProductionChangeAuthorizationStaleTest | Auth then fingerprint change | staleAuthorizationBlocksGrant | Auth STALE; no ISSUED grant | 0 | 0 | PRODUCTION_FINGERPRINT_STALE | No ISSUED grant | DEFAULT_CI |
| P16-E029 | Target change stales auth and revokes grants | BEHAVIORAL | G16-141 | T10 | Execution-significant target change stales authorization and revokes unconsumed ISSUED grants. | ProductionChangeTargetTest | ISSUED grant; target changes | targetChangeRevokesGrants | Grants REVOKED; auth STALE | 0 | 0 | PRODUCTION_FINGERPRINT_STALE | Grants revoked | DEFAULT_CI |
| P16-E030 | Kill switch denies after grant issued | BEHAVIORAL | G16-127, G16-092, G16-093 | T41 | Global kill switch at gateway send denies mutation even after grant ISSUED. | ProductionChangeKillSwitchTest | Grant ISSUED; enabled=false | killSwitchAfterGrantDeniesSend | EXECUTE_DENIED | 0 | 0 | PRODUCTION_DISABLED | 0 vendor mutations | DEFAULT_CI |
| P16-E031 | Target suspended after grant denies send | BEHAVIORAL | G16-128, G16-093 | T42 | Target SUSPENDED after grant issuance denies gateway send. | ProductionChangeTargetHealthTest | Grant ISSUED; SUSPENDED | suspendedAfterGrantDeniesSend | Deny durable | 0 | 0 | PRODUCTION_TARGET_SUSPENDED | 0 vendor mutations | DEFAULT_CI |
| P16-E032 | Change control expired after grant denies | BEHAVIORAL | G16-131, G16-132, G16-084 | T48 | Gateway revalidates change-control at consume/pre-send; expired ticket after grant yields zero mutation. | ProductionChangeControlTest | Grant ISSUED; CC expired | expiredTicketAfterGrantDenies | CC invalid deny | 0 | 0 | PRODUCTION_CHANGE_CONTROL_INVALID | 0 vendor mutations | DEFAULT_CI |
| P16-E033 | Expected state mismatch zero mutation | BEHAVIORAL | G16-055 | T19 | Direct observation MISMATCH before mutation yields zero vendor mutation. | ProductionChangeExpectedStateTest | Observation mismatch | mismatchZeroMutation | NOT_SENT | 0 | 0 | PRODUCTION_VENDOR_STATE_MISMATCH | mutation counter=0 | DEFAULT_CI |
| P16-E034 | Expected state unknown zero mutation | BEHAVIORAL | G16-056 | T19 | Direct observation UNKNOWN/TIMEOUT/STALE/UNAVAILABLE yields zero vendor mutation. | ProductionChangeExpectedStateTest | Observation unavailable | unknownZeroMutation | NOT_SENT | 0 | 0 | PRODUCTION_VERIFICATION_UNAVAILABLE | mutation counter=0 | DEFAULT_CI |
| P16-E035 | ATOMIC unsupported denies when required | BEHAVIORAL | G16-057, G16-058 | — | When profile requires ATOMIC and transport lacks certified ATOMIC, mutation is denied with zero send. | ProductionChangeExpectedStateTest | ATOMIC required unsupported | atomicUnsupportedDenies | Deny | 0 | 0 | PRODUCTION_ATOMIC_UNSUPPORTED | Deny; 0 mutations | DEFAULT_CI |
| P16-E036 | READ_THEN_WRITE disallowed by policy | BEHAVIORAL | G16-059 | T20 | Target policy forbidding READ_THEN_WRITE denies the residual TOCTOU path. | ProductionChangeExpectedStateTest | Policy forbids READ_THEN_WRITE | readThenWriteDisallowed | Deny | 0 | 0 | PRODUCTION_POLICY_DENY | Deny | DEFAULT_CI |
| P16-E037 | No blind retry after MAY_HAVE_SENT | BEHAVIORAL | G16-072, G16-149, G16-137 | T22,T23 | After MAY_HAVE_SENT / OUTCOME_UNKNOWN the gateway must not re-invoke mutation transport. | ProductionChangeAmbiguousOutcomeTest | OUTCOME_UNKNOWN durable | noBlindRetryAfterMayHaveSent | OUTCOME_UNKNOWN retained | <=1 | 0 | PRODUCTION_OUTCOME_UNKNOWN | retry=0; max 1 mutation | DEFAULT_CI |
| P16-E038 | Mutation HTTP retry disabled | BEHAVIORAL | G16-149 | T23 | Vendor mutation HTTP client has retries disabled and no framework retry wrapper around send. | ProductionChangeTransportTest | Mutation client bean | mutationHttpRetryDisabled | N/A | N/A | 0 | — | No RetryInterceptor on mutation bean | DEFAULT_CI |
| P16-E039 | App cannot fabricate VERIFIED | BEHAVIORAL | G16-135 | T24 | Application sync without durable gateway evidence cannot set VERIFIED/VENDOR_ACCEPTED/ROLLED_BACK. | ProductionChangeEvidenceAuthorityTest | App fabricate attempt | appCannotFabricateVerified | State unchanged | 0 | 0 | PRODUCTION_EVIDENCE_REQUIRED | State unchanged | DEFAULT_CI |
| P16-E040 | HTTP response not authoritative | BEHAVIORAL | G16-154 | T40 | Lost HTTP response reconstructs state from durable DB evidence only. | ProductionChangeEvidenceAuthorityTest | Response lost | httpResponseNotSoleAuthority | Durable evidence authoritative | <=1 | 0 | — | Reconstruct from DB only | DEFAULT_CI |
| P16-E041 | Grant issuance rate limit | BEHAVIORAL | G16-129 | T35 | Excessive ISSUED grant creation is denied by abuse limits. | ProductionChangeGrantAbuseTest | Burst issuance | issuanceRateLimitEnforced | No excess ISSUED | 0 | 0 | GRANT_ISSUANCE_DENIED | Issuance denied | DEFAULT_CI |
| P16-E042 | Single active forward ISSUED grant | BEHAVIORAL | G16-043 | T35 | At most one active forward ISSUED grant per binding. | ProductionChangeGrantAbuseTest | Second forward grant | singleActiveForwardGrant | One ISSUED max | 0 | 0 | PRODUCTION_GRANT_ALREADY_ISSUED | One ISSUED max | DEFAULT_CI |
| P16-E043 | Agent execute denied | BEHAVIORAL | G16-033, G16-104 | T05 | Agent principal cannot POST production execute. | ProductionChangeAgentBoundaryTest | Agent principal | agentExecuteDenied | Unchanged | 0 | 0 | PRODUCTION_UNAUTHORIZED | 403 | DEFAULT_CI |
| P16-E044 | MCP execute denied | BEHAVIORAL | G16-034, G16-105 | T06 | MCP client cannot invoke production execute/apply. | ProductionChangeMcpBoundaryTest | MCP client | mcpExecuteDenied | Unchanged | 0 | 0 | PRODUCTION_UNAUTHORIZED | 403/tool absent | DEFAULT_CI |
| P16-E045 | Phase15 execution unchanged | BEHAVIORAL | G16-001 | — | Phase 15 simulator execution path remains unchanged under Phase 16 presence. | ProductionChangePhase15IsolationTest | Phase15 suite | phase15ExecutionUnchanged | Phase15 unchanged | N/A | N/A | — | Phase15 tests pass unchanged | DEFAULT_CI |
| P16-E046 | Phase12 canonical not mutated by production execute | INTEGRATION | G16-065, G16-064, G16-067 | — | Successful production mutation+verification must not directly update canonical radio_configuration; only NETWORK_SYNCHRONIZATION_REQUIRED may be emitted. | ProductionChangeCanonicalIsolationTest | Production VERIFIED path | noDirectCanonicalMutation | VERIFIED; canonical unchanged | 1 production / 0 canonical | 0 | — | Zero canonical writes | DEFAULT_CI |
| P16-E047 | Configuration cannot expand scope beyond CELL/txPower | BEHAVIORAL | G16-017, G16-018 | T28 | Config validation rejects multi-cell / non-txPower scope expansion. | ProductionChangeConfigTest | Multi-cell config | scopeExpansionRejected | N/A | 0 | 0 | — | BindException | DEFAULT_CI |
| P16-E048 | Default enabled false | BEHAVIORAL | G16-151 | T28 | Fresh profile defaults snip.production-change.enabled=false with zero production mutation capability. | ProductionChangeConfigTest | Default profile | defaultEnabledFalse | enabled=false | 0 | 0 | PRODUCTION_DISABLED | enabled=false | DEFAULT_CI |
| P16-E049 | Reason codes and evidence sanitized | BEHAVIORAL | G16-115, G16-116, G16-098 | T15 | API/audit/evidence payloads exclude secrets; reason codes are sanitized stable codes. | ProductionChangeReasonCodeTest | Failure with vendor error | reasonCodesSanitized | Sanitized reason only | N/A | N/A | stable sanitized code | No secret substrings | DEFAULT_CI |
| P16-E050 | Metrics low cardinality | BEHAVIORAL | G16-102 | — | Micrometer tags exclude cellId/grantId/productionChangeId and other high-cardinality labels. | ProductionChangeMetricsTest | Metrics registry | forbiddenLabelsAbsent | N/A | N/A | N/A | — | No high-cardinality tags | DEFAULT_CI |
| P16-E051 | Concurrent grant consume one winner | INTEGRATION | G16-120, G16-121, G16-036, G16-117 | T36,T37 | Two concurrent gateway consumes of the same grantId: exactly one success; one deny; zero mutations at consume. | ProductionChangeGrantConsumeIT | Same ISSUED grant | concurrentConsume_oneWinner | One CONSUMED | 0 at consume | 0 | PRODUCTION_GRANT_ALREADY_CONSUMED | 1 success; 1 deny | DEFAULT_CI |
| P16-E052 | Consume deny wrong target | INTEGRATION | G16-120 | T10 | Consume predicate rejects mismatched target_id with zero-row update. | ProductionChangeGrantConsumeIT | Wrong target_id | consumeDeny_wrongTarget | Grant remains ISSUED | 0 | 0 | PRODUCTION_GRANT_BINDING_MISMATCH | 0 rows updated | DEFAULT_CI |
| P16-E053 | Consume deny wrong fingerprint | INTEGRATION | G16-120 | T09 | Consume predicate rejects mismatched production_fingerprint. | ProductionChangeGrantConsumeIT | Wrong fingerprint | consumeDeny_wrongFingerprint | Grant remains ISSUED | 0 | 0 | PRODUCTION_GRANT_BINDING_MISMATCH | 0 rows updated | DEFAULT_CI |
| P16-E054 | Consume deny wrong auth generation | INTEGRATION | G16-120 | — | Consume predicate rejects mismatched authorization_generation. | ProductionChangeGrantConsumeIT | Wrong auth generation | consumeDeny_wrongAuthGeneration | Grant remains ISSUED | 0 | 0 | PRODUCTION_GRANT_BINDING_MISMATCH | 0 rows updated | DEFAULT_CI |
| P16-E055 | Consume deny wrong fencing token | INTEGRATION | G16-120, G16-048 | — | Consume predicate rejects mismatched fencing_token. | ProductionChangeGrantConsumeIT | Wrong fencing | consumeDeny_wrongFencingToken | Grant remains ISSUED | 0 | 0 | PRODUCTION_FENCING_MISMATCH | 0 rows updated | DEFAULT_CI |
| P16-E056 | Consume deny wrong operation binding | INTEGRATION | G16-120 | — | Consume predicate rejects mismatched operation_binding_hash. | ProductionChangeGrantConsumeIT | Wrong binding hash | consumeDeny_wrongOperationBinding | Grant remains ISSUED | 0 | 0 | PRODUCTION_GRANT_BINDING_MISMATCH | 0 rows updated | DEFAULT_CI |
| P16-E057 | Consume deny expired grant | INTEGRATION | G16-120 | T38 | Expired grant cannot be consumed. | ProductionChangeGrantConsumeIT | expires_at past | consumeDeny_expired | EXPIRED or ISSUED unchanged | 0 | 0 | PRODUCTION_GRANT_EXPIRED | 0 rows updated | DEFAULT_CI |
| P16-E058 | Consume deny revoked grant | INTEGRATION | G16-120 | T38 | REVOKED grant cannot be consumed. | ProductionChangeGrantConsumeIT | status REVOKED | consumeDeny_revoked | REVOKED | 0 | 0 | PRODUCTION_GRANT_REVOKED | 0 rows updated | DEFAULT_CI |
| P16-E059 | Consume deny wrong grant type | INTEGRATION | G16-120 | — | FORWARD vs ROLLBACK grant_type mismatch denies consume. | ProductionChangeGrantConsumeIT | Wrong grant_type | consumeDeny_wrongGrantType | ISSUED unchanged | 0 | 0 | PRODUCTION_GRANT_BINDING_MISMATCH | 0 rows updated | DEFAULT_CI |
| P16-E060 | Consume deny already consumed | INTEGRATION | G16-046, G16-121, G16-073 | T08 | Replay of consumed grantId denies with zero additional mutations. | ProductionChangeGrantConsumeIT | Already CONSUMED | consumeDeny_alreadyConsumed | CONSUMED unchanged | 0 additional | 0 | PRODUCTION_GRANT_ALREADY_CONSUMED | 0 additional mutations | DEFAULT_CI |
| P16-E061 | Consume before attempt invariant | INTEGRATION | G16-124, G16-134 | — | No production_gateway_attempt row may exist while grant.status=ISSUED; attempt created only after consume. | ProductionChangeAttemptOrderingIT | Pre-consume request | noAttemptBeforeConsume | Grant ISSUED; attempt none | 0 | 0 | — | 0 attempt rows pre-consume | DEFAULT_CI |
| P16-E062 | Post-consume pre-attempt crash recovery | INTEGRATION | G16-123, G16-124, G16-122 | T39 | After consume commit and before attempt insert: grant stays CONSUMED; no reset to ISSUED; CONSUMED_PRE_SEND_RECOVERY_REQUIRED; no auto vendor retry. | ProductionChangeGatewayHandoffIT | FI-03 injection | consumedPreAttemptCrashRecovery | CONSUMED_PRE_SEND_RECOVERY_REQUIRED; grant CONSUMED | 0 | 0 | PRODUCTION_OUTCOME_UNKNOWN | No grant reset | DEFAULT_CI |
| P16-E063 | Crash after attempt before send | INTEGRATION | G16-123, G16-134 | T39 | After attempt persist before send: attempt remains PRE_SEND; no auto retry. | ProductionChangeGatewayHandoffIT | FI-04 injection | attemptPersistedBeforeSendCrash | Attempt PRE_SEND; grant CONSUMED | 0 | 0 | — | No auto retry; PRE_SEND evidence | DEFAULT_CI |
| P16-E064 | Concurrent execute max one mutation | INTEGRATION | G16-121, G16-089, G16-073 | T21 | Two concurrent execute callers for same binding produce at most one vendor mutation. | ProductionChangeExecuteRaceIT | Parallel execute | concurrentExecute_maxOneMutation | One active path | <=1 | 0 | PRODUCTION_LEASE_CONFLICT | vendor mutation count <= 1 | DEFAULT_CI |
| P16-E065 | Shared rate limit multi-gateway | INTEGRATION | G16-129, G16-130 | T31,T43 | Shared durable rate-limit counters across gateway replicas deny when unknown/exceeded. | ProductionChangeRateLimitIT | Two gateway pods near limit | sharedRateLimitAcrossGateways | Loser denied | <=1 | 0 | PRODUCTION_RATE_LIMIT_EXCEEDED | Second denied | DEFAULT_CI |
| P16-E066 | Audit chain concurrent append | INTEGRATION | G16-146, G16-099, G16-098 | T32,T46 | Parallel audit appends for same productionChangeId serialize with strict sequence and no gaps. | ProductionChangeAuditChainIT | Parallel appends | concurrentAppendSerialized | Strict sequence | N/A | N/A | — | Strict sequence; no gaps | DEFAULT_CI |
| P16-E067 | Audit chain tamper detection | INTEGRATION | G16-147, G16-099 | T32 | Mutating event_hash marks chain INVALID and blocks new mutation per policy. | ProductionChangeAuditChainIT | Tampered hash | tamperDetectionInvalid | INTEGRITY_INVALID | 0 new | 0 | PRODUCTION_AUDIT_INTEGRITY_INVALID | Mutation blocked | DEFAULT_CI |
| P16-E068 | Audit chain gap detection | INTEGRATION | G16-147 | T46 | Deleted sequence row marks chain INVALID. | ProductionChangeAuditChainIT | Gap injected | gapDetection | INTEGRITY_INVALID | 0 new | 0 | PRODUCTION_AUDIT_INTEGRITY_INVALID | INTEGRITY_INVALID | DEFAULT_CI |
| P16-E069 | Ambiguous outcome desired readback verifies | INTEGRATION | G16-068, G16-072 | T22 | After MAY_HAVE_SENT/OUTCOME_UNKNOWN, fresh readback of desired value yields VERIFIED with no additional mutation/retry. | ProductionChangeAmbiguousOutcomeIT | FI-09; readback desired | vendorAppliedResponseLost | VERIFIED | 1 | 0 | — | VERIFIED; mutation=1; retry=0 | DEFAULT_CI |
| P16-E070 | Verification independent of mutation response | INTEGRATION | G16-062, G16-063, G16-061 | — | VERIFIED requires separate fresh desired-state readback; mutation-accepted response alone must not mark VERIFIED. | ProductionChangeVerificationIT | Vendor accepted then separate readback | verificationUsesSeparateReadback | VENDOR_ACCEPTED then VERIFIED | 1 | 0 | — | VERIFIED only after readback | DEFAULT_CI |
| P16-E071 | Verification persistence failure recovery | INTEGRATION | G16-100, G16-136 | — | Verification persistence failure preserves prior durable mutation outcome via independent TX. | ProductionChangeVerificationIT | FI-15 | verificationPersistFailureSurvives | Prior outcome survives | 1 | 0 | — | Durable outcome preserved | DEFAULT_CI |
| P16-E072 | Gateway credential resolution timing | INTEGRATION | G16-008, G16-094 | T14 | Write credential resolution occurs only after valid consume and final preflight progression to credential step. | ProductionGatewayCredentialIT | Execute path | credentialAfterConsumeAndPreflight | Credential after consume | 0 if fail early | 0 | — | No credential call pre-consume | DEFAULT_CI |
| P16-E073 | Gateway credential failure zero mutation | INTEGRATION | G16-094 | T14 | Key Vault failure before send yields zero vendor mutation. | ProductionGatewayCredentialIT | KV failure | credentialFailureZeroMutation | PRE_SEND deny | 0 | 0 | PRODUCTION_CREDENTIAL_FAILURE | mutation counter=0 | DEFAULT_CI |
| P16-E074 | No old credential version fallback | INTEGRATION | G16-142, G16-143 | T47 | Disabled/old secret version fails closed; no older-version fallback. | ProductionGatewayCredentialIT | Disabled version | noOldVersionFallback | Deny | 0 | 0 | PRODUCTION_CREDENTIAL_FAILURE | Fail closed | DEFAULT_CI |
| P16-E075 | Read credential cannot substitute write | INTEGRATION | G16-143 | — | Write credential profile is distinct; read credential cannot substitute. | ProductionGatewayCredentialIT | Wrong profile | readCredentialCannotSubstituteWrite | Deny | 0 | 0 | PRODUCTION_CREDENTIAL_FAILURE | Deny wrong profile | DEFAULT_CI |
| P16-E076 | Simulator happy path E2E | INTEGRATION | G16-088, G16-063 | — | Full governed L0 path reaches VERIFIED with exactly one mutation and zero retries. | ProductionChangeSimulatorE2EIT | L0 valid chain | happyPath | VERIFIED | 1 | 0 | — | VERIFIED; mutation=1; retry=0 | DEFAULT_CI |
| P16-E077 | Rollback governed E2E | INTEGRATION | G16-076, G16-077, G16-079, G16-080 | — | Rollback requires separate request/review/authorize/grant/consume/attempt; rollback fingerprint and Phase-14 persisted rollback value; max one rollback mutation per valid grant. | ProductionChangeRollbackE2EIT | Forward VERIFIED; rollback authorized | rollbackHappyPath | ROLLED_BACK | 1 rollback | 0 | — | ROLLED_BACK; rollback mutation=1 | DEFAULT_CI |
| P16-E078 | Rollback consume race | INTEGRATION | G16-121 | — | Concurrent rollback grant consume: exactly one winner. | ProductionChangeRollbackIT | Same rollback grant | rollbackGrantConsumeRace | One CONSUMED rollback | <=1 rollback | 0 | PRODUCTION_GRANT_ALREADY_CONSUMED | One winner | DEFAULT_CI |
| P16-E079 | Rollback expected mismatch | INTEGRATION | G16-078 | — | Rollback expected-state mismatch denies rollback mutation. | ProductionChangeRollbackTest | Wrong expected for rollback | rollbackExpectedMismatch | Deny | 0 rollback | 0 | PRODUCTION_VENDOR_STATE_MISMATCH | 0 rollback mutations | DEFAULT_CI |
| P16-E080 | Fingerprint deterministic SHA-256 | INTEGRATION | G16-036, G16-037 | — | Same fingerprint inputs produce identical SHA-256 production fingerprint. | ProductionChangeFingerprintIT | Identical inputs | deterministicFingerprint | Stable hash | N/A | N/A | — | Stable hash | DEFAULT_CI |
| P16-E081 | Idempotent execute after consume | INTEGRATION | G16-046, G16-073 | T08 | Duplicate execute after consume returns durable state with no second mutation. | ProductionChangeIdempotencyIT | After consume/terminal | duplicateExecuteAfterConsume | Unchanged terminal/durable | 0 additional | 0 | — | No second mutation | DEFAULT_CI |
| P16-E082 | Fencing changed after grant | INTEGRATION | G16-048, G16-050 | — | Fencing token change after grant denies send with zero mutation. | ProductionChangeLeaseIT | Stale fencing at send | fencingChangedAfterGrantDenies | EXECUTE_DENIED | 0 | 0 | PRODUCTION_FENCING_MISMATCH | 0 mutations | DEFAULT_CI |
| P16-E083 | Grant issuance concurrency | INTEGRATION | G16-043 | T35 | Parallel grant issue same binding yields one ISSUED. | ProductionChangeGrantConcurrencyIT | Parallel issue | issuanceConcurrencyOneActive | One ISSUED | 0 | 0 | PRODUCTION_GRANT_ALREADY_ISSUED | One ISSUED | DEFAULT_CI |
| P16-E084 | NETWORK_SYNCHRONIZATION_REQUIRED emitted | INTEGRATION | G16-066, G16-067, G16-064 | — | After VERIFIED emit NETWORK_SYNCHRONIZATION_REQUIRED only; Phase 12 remains reconciliation authority; production VERIFIED ≠ CANONICAL_RECONCILED. | ProductionChangePhase12BoundaryIT | After VERIFIED | syncRequiredEmittedNotCanonicalWrite | Sync signal; canonical unchanged | 1 production / 0 canonical | 0 | — | Event/status only | DEFAULT_CI |
| P16-E085 | Controlled mutation counter assertions | INTEGRATION | G16-089, G16-072 | — | Test adapter mutation counter asserts deterministic counts for deny/happy/ambiguous paths. | ProductionChangeMutationCounterIT | Controlled adapter | assertExactMutationCounts | Per-scenario durable | per scenario | 0 auto | — | Deterministic counts | DEFAULT_CI |
| P16-E086 | Gateway deployment manifest exists | INFRASTRUCTURE | G16-152 | — | deploy/k8s production-write-gateway Deployment is separate from ordinary app Deployment. | ProductionChangeInfraValidationTest | Manifests present | gatewayDeploymentManifestSeparate | N/A | N/A | N/A | — | STATIC: separate Deployment name | DEFAULT_CI |
| P16-E087 | Gateway service account separate | INFRASTRUCTURE | G16-007, G16-152 | — | production-write-gateway ServiceAccount is distinct from snip-npo app SA. | ProductionChangeInfraValidationTest | SA manifests | gatewayServiceAccountSeparate | N/A | N/A | N/A | — | STATIC: distinct SA name | DEFAULT_CI |
| P16-E088 | App NetworkPolicy denies vendor egress | INFRASTRUCTURE | G16-095, G16-005 | — | App NetworkPolicy does not permit ENM vendor write egress. | ProductionChangeInfraValidationTest | App NP | appNetworkPolicyNoVendorEgress | N/A | N/A | N/A | — | STATIC: no ENM egress from app | DEFAULT_CI |
| P16-E089 | Gateway NetworkPolicy restricted egress | INFRASTRUCTURE | G16-095 | — | Gateway NetworkPolicy allowlists approved ENM only; no 0.0.0.0/0 write egress. | ProductionChangeInfraValidationTest | Gateway NP | gatewayNetworkPolicyRestrictedEgress | N/A | N/A | N/A | — | STATIC: no 0.0.0.0/0 | DEFAULT_CI |
| P16-E090 | TLS verification enabled in config | INFRASTRUCTURE | G16-096 | — | Default TLS profile is strict with hostname verification. | ProductionChangeInfraValidationTest | Config defaults | tlsVerificationEnabled | N/A | N/A | N/A | — | STATIC: strict TLS profile default | DEFAULT_CI |
| P16-E091 | No trust-all SSL in gateway config | INFRASTRUCTURE | G16-096, G16-097 | — | Gateway SSL context prohibits trust-all; mTLS keys only via secure credential mechanism when required. | ProductionChangeInfraValidationTest | SSL config | noTrustAllSsl | N/A | N/A | N/A | — | STATIC: trust-all prohibited | DEFAULT_CI |
| P16-E092 | Default production-change enabled false | INFRASTRUCTURE | G16-151 | — | application.yml default snip.production-change.enabled=false. | ProductionChangeInfraValidationTest | YAML defaults | defaultEnabledFalseInYaml | N/A | N/A | N/A | — | STATIC: enabled=false | DEFAULT_CI |
| P16-E093 | Traceability validation fixture exists | INFRASTRUCTURE | G16-112, G16-113 | — | ProductionChangeGateTraceabilityValidationTest loads gate/evidence map and asserts every gate has >=1 concrete evidence and high-risk gates are not STRUCTURAL-only. | ProductionChangeGateTraceabilityValidationTest | Map artifact | allGatesHaveEvidence | N/A | N/A | N/A | — | 154 gates; high-risk runtime typed | DEFAULT_CI |
| P16-E094 | Machine-readable gate map artifact | INFRASTRUCTURE | G16-112 | — | docs/implementation/phase16-gate-evidence-map.json present and identical to §54 mappings. | ProductionChangeGateTraceabilityValidationTest | JSON map | jsonMapMatchesMarkdown | N/A | N/A | N/A | — | JSON IDs match §54 | DEFAULT_CI |
| P16-E095 | Level 1 vendor lab Ericsson E2E | EXTERNAL_CERTIFICATION | G16-145 | T16 | Manual vendor-approved lab transport evidence recorded. | External certification package | Org-approved environment | Manual: LEVEL1_VENDOR_LAB | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E096 | Level 2 pre-production E2E | EXTERNAL_CERTIFICATION | G16-109 | — | Manual pre-prod target verification evidence recorded. | External certification package | Org-approved environment | Manual: LEVEL2_PREPROD | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E097 | Level 3 production target registration | EXTERNAL_CERTIFICATION | G16-109 | — | External target registration evidence recorded. | External certification package | Org-approved environment | Manual: LEVEL3_TARGET | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E098 | Level 4 controlled production authorization | EXTERNAL_CERTIFICATION | G16-144 | — | Manual target/profile-specific Level-4 sign-off evidence recorded; code ≠ L4. | External certification package | Org-approved environment | Manual: LEVEL4_AUTHORIZATION | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E099 | Approved Ericsson write protocol evidence | EXTERNAL_CERTIFICATION | G16-012 | — | Vendor-documented write protocol evidence; production transport remains unresolved until present. | External certification package | Org-approved environment | Manual: ERICSSON_PROTOCOL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E100 | Production write WI token exchange | EXTERNAL_CERTIFICATION | G16-007, G16-008 | T14 | Actual AKS Workload Identity token exchange for write UAMI. | External certification package | Org-approved environment | Manual: WRITE_WI_RUNTIME | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E101 | Key Vault write secret access from gateway only | EXTERNAL_CERTIFICATION | G16-008 | T14 | Runtime RBAC proves gateway-only write-secret access. | External certification package | Org-approved environment | Manual: KV_WRITE_RBAC_RUNTIME | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E102 | Ordinary app identity denied write secret | EXTERNAL_CERTIFICATION | G16-005 | T14 | Runtime Key Vault deny from ordinary app UAMI. | External certification package | Org-approved environment | Manual: KV_APP_DENY_RUNTIME | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E103 | Production egress restriction runtime | EXTERNAL_CERTIFICATION | G16-095 | T18 | Actual pod egress restricted to approved ENM. | External certification package | Org-approved environment | Manual: EGRESS_RUNTIME | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E104 | Production TLS/mTLS validation | EXTERNAL_CERTIFICATION | G16-096, G16-097 | T16,T17 | Live TLS/mTLS handshake verification. | External certification package | Org-approved environment | Manual: TLS_MTLS_RUNTIME | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E105 | Production kill switch drill | EXTERNAL_CERTIFICATION | G16-092 | T41 | Operator kill-switch drill evidence. | External certification package | Org-approved environment | Manual: KILL_SWITCH_DRILL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E106 | Production suspension drill | EXTERNAL_CERTIFICATION | G16-091 | T42 | Auto/manual suspension drill evidence. | External certification package | Org-approved environment | Manual: SUSPENSION_DRILL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E107 | Production rate limit drill | EXTERNAL_CERTIFICATION | G16-129 | T31 | Cross-replica rate-limit drill evidence. | External certification package | Org-approved environment | Manual: RATE_LIMIT_DRILL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E108 | Production audit chain drill | EXTERNAL_CERTIFICATION | G16-099 | T32 | Production audit-chain verification drill. | External certification package | Org-approved environment | Manual: AUDIT_CHAIN_DRILL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E109 | Production rollback drill | EXTERNAL_CERTIFICATION | G16-076 | — | Governed production rollback drill. | External certification package | Org-approved environment | Manual: ROLLBACK_DRILL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E110 | Production readback drill | EXTERNAL_CERTIFICATION | G16-062 | — | Independent production verification drill. | External certification package | Org-approved environment | Manual: READBACK_DRILL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E111 | Production change-control drill | EXTERNAL_CERTIFICATION | G16-131 | T48 | Ticket invalidation-at-send drill. | External certification package | Org-approved environment | Manual: CC_DRILL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E112 | Production SoD role assignment | EXTERNAL_CERTIFICATION | G16-030, G16-031 | T01 | IAM roles separated for SoD. | External certification package | Org-approved environment | Manual: SOD_ROLES | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E113 | Cilium FQDN lab limitation documented | EXTERNAL_CERTIFICATION | G16-152 | — | Known Cilium FQDN limitation documented without broadening egress. | External certification package | Org-approved environment | Manual: CILIUM_FQDN_DOC | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E114 | Gateway compromise residual acknowledged | EXTERNAL_CERTIFICATION | G16-150 | T34 | Runbook documents gateway-compromise residual risk and defense-in-depth. | External certification package | Org-approved environment | Manual: GATEWAY_COMPROMISE_RESIDUAL | External evidence package | N/A | N/A | — | External evidence recorded; not default CI | EXTERNAL |
| P16-E115 | Phase 15 failed candidate preserved in docs | STRUCTURAL | G16-002 | — | Documentation references failed Phase 15 candidate 0cb1223… and does not rewrite history. | ProductionChangeBaselineTest | Docs present | failedPhase15CandidateDocumented | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E116 | Write-side SPI interfaces in gateway module | STRUCTURAL | G16-011 | — | VendorNetworkWriteAdapter / EricssonEnmWriteAdapter / EricssonWriteTransport live in gateway module only. | ProductionChangeStructuralTest | Gateway SPI | writeSideSpiInterfacesPresent | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E117 | Typed CELL txPower mutation only | BEHAVIORAL | G16-016, G16-017 | — | AuthorizedParameterMutation rejects non-txPower / non-CELL mutations. | ProductionChangeScopeTest | Non-txPower request | typedMutationTxPowerOnly | Reject | 0 | 0 | PRODUCTION_SCOPE_DENIED | PASS | DEFAULT_CI |
| P16-E118 | Max parameters per execution = 1 | BEHAVIORAL | G16-019 | — | Second parameter rejected at admission. | ProductionChangeScopeTest | Two parameters | maxOneParameter | ADMISSION_REJECTED | 0 | 0 | PRODUCTION_SCOPE_DENIED | PASS | DEFAULT_CI |
| P16-E119 | Max forward operations = 1 | BEHAVIORAL | G16-020, G16-089 | — | Second forward mutation operation rejected; blast radius 1/1/1. | ProductionChangeScopeTest | Second operation | maxOneOperation | Deny | 0 | 0 | PRODUCTION_SCOPE_DENIED | PASS | DEFAULT_CI |
| P16-E120 | ProductionNetworkTarget required fields | STRUCTURAL | G16-021 | — | Target entity contains required registry fields from architecture. | ProductionChangeMigrationSpecTest | Entity model | targetEntityFields | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E121 | Target states ACTIVE SUSPENDED DISABLED | BEHAVIORAL | G16-022, G16-091, G16-103 | — | Invalid target state transitions rejected; target health is independent of app readiness. | ProductionChangeTargetTest | Invalid transition | targetStatesEnforced | State machine enforced | 0 | 0 | PRODUCTION_TARGET_INVALID_STATE | PASS | DEFAULT_CI |
| P16-E122 | Target stores credential profile ref only | STRUCTURAL | G16-023 | — | Target table has no secret columns; only credential profile references. | ProductionChangeMigrationSpecTest | Schema | targetNoSecretColumns | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E123 | ProductionNetworkChange distinct from Phase15 execution | STRUCTURAL | G16-024 | — | ProductionNetworkChange is a distinct aggregate from network_change_execution. | ProductionChangeMigrationSpecTest | Entities | distinctFromPhase15Execution | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E124 | Mutation details from Phase14/15 only | BEHAVIORAL | G16-027 | — | Create derives mutation details from governed Phase 14/15 state only. | ProductionChangeAdmissionIT | Create from execution | mutationDetailsFromGovernedStateOnly | Bound from upstream | 0 | 0 | — | PASS | DEFAULT_CI |
| P16-E125 | Production permission set complete | STRUCTURAL | G16-035 | — | Permission set includes VIEW/REQUEST/REVIEW/AUTHORIZE/EXECUTE and rollback counterparts. | ProductionChangeSecurityTest | Permission enum | productionPermissionSetComplete | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E126 | No silent fingerprint reauthorization | BEHAVIORAL | G16-039 | — | Material fingerprint change requires explicit re-authorization; no silent regen. | ProductionChangeFingerprintTest | Material change | noSilentReauthorization | Auth STALE | 0 | 0 | PRODUCTION_FINGERPRINT_STALE | PASS | DEFAULT_CI |
| P16-E127 | Change control required not authorization | BEHAVIORAL | G16-040 | — | Valid change-control alone is insufficient for execute; authorization still required. | ProductionChangeControlTest | CC valid without auth | changeControlRequiredNotAuthorization | Execute denied | 0 | 0 | PRODUCTION_UNAUTHORIZED | PASS | DEFAULT_CI |
| P16-E128 | MANUAL change control only | STRUCTURAL | G16-041 | — | No ServiceNow/ITSM deep integration in Phase 16; MANUAL change-control only. | ProductionChangeControlTest | Integrations | manualChangeControlOnly | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E129 | Grant distinct from vendor credential | STRUCTURAL | G16-042 | — | ProductionExecutionGrant entity is distinct from vendor credential material. | ProductionChangeMigrationSpecTest | Schema | grantDistinctFromCredential | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E130 | Grant statuses enum complete | STRUCTURAL | G16-044, G16-117 | — | Grant statuses are exactly ISSUED/CONSUMED/EXPIRED/REVOKED in durable store. | ProductionChangeGrantTest | Enum | grantStatusesComplete | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E131 | No valid grant gateway deny | BEHAVIORAL | G16-045 | — | Missing/invalid consumable grant causes gateway deny with zero mutation. | ProductionChangeGrantConsumeIT | No valid grant | noValidGrantGatewayDeny | Deny | 0 | 0 | PRODUCTION_GRANT_MISSING | PASS | DEFAULT_CI |
| P16-E132 | Ambiguous outcome no auto re-grant | BEHAVIORAL | G16-047 | — | OUTCOME_UNKNOWN does not automatically issue another grant. | ProductionChangeAmbiguousOutcomeTest | OUTCOME_UNKNOWN | noAutoRegrantAfterAmbiguous | No new ISSUED grant | <=1 | 0 | — | PASS | DEFAULT_CI |
| P16-E133 | Lease before grant issuance | INTEGRATION | G16-049 | — | Grant issuance is denied without an active production lease. | ProductionChangeLeaseIT | No lease | leaseRequiredBeforeGrant | No ISSUED grant | 0 | 0 | PRODUCTION_LEASE_REQUIRED | PASS | DEFAULT_CI |
| P16-E134 | V17 migration proposed tables only | BEHAVIORAL | G16-110 | — | Specification lists proposed V17 tables; repository contains no V17__*.sql migration file until implementation authorization. | ProductionChangeMigrationSpecTest | Repo scan + §8 | allTablesDefinedAndV17Absent | V17 absent | N/A | N/A | — | Tables defined; V17 file absent | DEFAULT_CI |
| P16-E135 | Production write transport NOT CONFIGURED default | BEHAVIORAL | G16-013 | — | Default production Ericsson write transport is NOT CONFIGURED and fail-closed. | ProductionWriteGatewayIsolationTest | Default profile | productionTransportFailClosed | Fail-closed | 0 | 0 | PRODUCTION_TRANSPORT_NOT_CONFIGURED | PASS | DEFAULT_CI |
| P16-E136 | No generic vendor command SPI | BEHAVIORAL | G16-015 | — | Write SPI exposes typed CELL/txPower mutation only; no generic command/executeRaw method. | ProductionChangeStructuralTest | Adapter SPI | noGenericCommandMethod | N/A | 0 | 0 | — | PASS | DEFAULT_CI |
| P16-E137 | Create accepts only three fields | BEHAVIORAL | G16-025 | — | Create request accepts only phase15ExecutionId, productionTargetId, changeControlReference. | ProductionChangeApiTest | Extra fields | createOnlyThreeFields | Reject extras | 0 | 0 | PRODUCTION_INVALID_REQUEST | PASS | DEFAULT_CI |
| P16-E138 | Authorization independent from Phase14/15 | INTEGRATION | G16-028, G16-029 | — | Phase 13/14/15 approvals do not confer Phase 16 production authorization. | ProductionChangeAdmissionIT | Upstream approved only | authorizationIndependence | No AUTHORIZED without Phase16 auth | 0 | 0 | PRODUCTION_UNAUTHORIZED | PASS | DEFAULT_CI |
| P16-E139 | Window open / authorize does not execute | BEHAVIORAL | G16-083, G16-087 | — | Creation, authorization, and open window must not invoke vendor mutation; only explicit execute may. | ProductionChangeApiTest | Authorize + open window | authorizeDoesNotExecute | AUTHORIZED; no attempt | 0 | 0 | — | 0 mutations | DEFAULT_CI |
| P16-E140 | Review permission distinct from authorize | BEHAVIORAL | G16-032 | — | Reviewer permission is distinct from authorizer permission. | ProductionChangeSoDTest | Reviewer principal | reviewerNotAuthorizerPermission | Authorize denied for reviewer-only | 0 | 0 | PRODUCTION_UNAUTHORIZED | PASS | DEFAULT_CI |
| P16-E141 | Grant timeout and crash matrix enforced | INTEGRATION | G16-123 | T39 | Grant timeout/crash matrix behaviors from §16/§21/§43 are exercised (expiry, FI-03/FI-04 recovery). | ProductionChangeGrantIT | Timeout + FI matrix | grantTimeoutMatrix | Per-matrix durable states | 0 when PRE_SEND | 0 | matrix codes | PASS | DEFAULT_CI |
| P16-E142 | Cancellation before mutation only | BEHAVIORAL | G16-082 | — | Cancel succeeds only before mutation; after MAY_HAVE_SENT cancel cannot claim NOT_SENT. | ProductionChangeExecuteTest | Cancel pre/post send | cancelBeforeMutationOnly | Pre-send cancelled; post-send ambiguous retained | <=1 | 0 | — | PASS | DEFAULT_CI |
| P16-E143 | Blast radius 1/1/1 enforced | BEHAVIORAL | G16-089 | — | ProductionBlastRadiusPolicy hard limits cells/parameters/ops = 1/1/1. | ProductionChangeRateLimitTest | Over-limit request | blastRadiusOneOneOne | Deny | 0 | 0 | PRODUCTION_RATE_LIMIT_EXCEEDED | PASS | DEFAULT_CI |
| P16-E144 | Automatic suspension allowed | BEHAVIORAL | G16-091, G16-090 | — | Repeated rate/unknown/verification failures may auto-suspend target; thresholds are enforced. | ProductionChangeTargetHealthTest | Failure threshold | autoSuspendOnFailures | SUSPENDED | 0 further | 0 | PRODUCTION_TARGET_SUSPENDED | PASS | DEFAULT_CI |
| P16-E145 | Automatic resume forbidden | BEHAVIORAL | G16-091 | — | Suspended target must not automatically resume; human/admin action required. | ProductionChangeTargetHealthTest | SUSPENDED target | noAutomaticResume | Remains SUSPENDED | 0 | 0 | PRODUCTION_TARGET_SUSPENDED | PASS | DEFAULT_CI |
| P16-E146 | Nokia write not implemented | BEHAVIORAL | G16-107 | — | Nokia NetAct write target/adapter is rejected/deferred; not implemented. | ProductionTargetRegistryTest | Nokia target | nokiaRejected | Reject | 0 | 0 | PRODUCTION_VENDOR_UNSUPPORTED | PASS | DEFAULT_CI |
| P16-E147 | Default CI Azure-independent | BEHAVIORAL | G16-108 | — | Default CI remains Azure/vendor/credential independent. | ProductionChangeInfraValidationTest | CI workflow | ciAzureIndependent | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E148 | Evidence catalog and threat map complete | BEHAVIORAL | G16-112, G16-113, G16-114 | — | Every catalog item has concrete requirement/setup/action/expected result; T01–T48 reference existing evidence IDs; no placeholder rows. | ProductionChangeMatrixEvidenceCatalog | Catalog + threats | allItemsHaveRequirements | N/A | N/A | N/A | — | PASS | DEFAULT_CI |
| P16-E149 | App pre-grant UNKNOWN denies grant issuance | INTEGRATION | G16-053 | T09 | If any mandatory application pre-grant preflight input is UNKNOWN/INVALID/STALE/DISABLED/MISMATCHED, no grant is issued and vendor mutation count remains 0. | ProductionPreGrantPreflightIT | Mandatory preflight input UNKNOWN | unknownInputDeniesGrant | No ISSUED grant; durable preflight denial | 0 | 0 | PRODUCTION_PREFLIGHT_DENIED | No grant; mutation=0; sanitized reason | DEFAULT_CI |
| P16-E150 | Direct vendor observation before mutation send | INTEGRATION | G16-054 | T19 | Direct vendor observation occurs after final preflight progression and before mutation invoke; canonical SNIP state alone is insufficient; unavailable/mismatch ⇒ mutation counter=0. | ProductionChangeExpectedStateIT | Grant consumed; observation instrumented | observationBeforeMutationOrdering | Observation evidence before SEND_ELIGIBLE | 0 if fail | 0 | PRODUCTION_VENDOR_STATE_MISMATCH\|UNAVAILABLE | Observation precedes send; counter proves no send on fail | DEFAULT_CI |
| P16-E151 | Mutation outcomes NOT_SENT/REJECTED/VENDOR_ACCEPTED/OUTCOME_UNKNOWN distinct | INTEGRATION | G16-060 | T22 | Gateway persists distinct mutation outcomes NOT_SENT, REJECTED, VENDOR_ACCEPTED, OUTCOME_UNKNOWN with no state collapsing; MAY_HAVE_SENT without response must not become NOT_SENT. | ProductionChangeMutationOutcomeIT | Simulator modes for each outcome | outcomesRemainDistinct | Per-outcome durable attempt/evidence | 0 or 1 by mode | 0 | outcome-specific | Four outcomes distinct; timeout-after-invoke ≠ NOT_SENT | DEFAULT_CI |
| P16-E152 | VENDOR_ACCEPTED is not VERIFIED | INTEGRATION | G16-061, G16-062 | T24 | Vendor-accepted mutation response durably becomes VENDOR_ACCEPTED (or pre-verification state); VERIFIED only after separate fresh desired readback; test fails if mutation response alone marks VERIFIED. | ProductionChangeVerificationIT | Accepted response; withhold readback | vendorAcceptedNotVerifiedUntilReadback | VENDOR_ACCEPTED then VERIFIED after readback | 1 | 0 | — | Not VERIFIED on accept alone | DEFAULT_CI |
| P16-E153 | Ambiguous expected-value readback safe stop | INTEGRATION | G16-069 | T22 | When mutation outcome is OUTCOME_UNKNOWN and readback observes original expected value: no automatic resend, no second mutation, retry=0, governed follow-up/new authorization required, durable recovery state recorded. | ProductionChangeAmbiguousOutcomeIT | OUTCOME_UNKNOWN; readback expected | ambiguousExpectedValueSafeStop | RECOVERY_REQUIRED / safe stop; no new grant auto | 1 (no additional) | 0 | PRODUCTION_OUTCOME_UNKNOWN | No second mutation; new auth required | DEFAULT_CI |
| P16-E154 | Ambiguous third-value requires manual intervention | INTEGRATION | G16-070 | T22 | When post-send readback observes neither expected nor desired value, state becomes MANUAL_INTERVENTION_REQUIRED with retry=0 and no second mutation. | ProductionChangeAmbiguousOutcomeIT | OUTCOME_UNKNOWN; third value | thirdValueManualIntervention | MANUAL_INTERVENTION_REQUIRED | 1 (no additional) | 0 | PRODUCTION_MANUAL_INTERVENTION_REQUIRED | No second mutation | DEFAULT_CI |
| P16-E155 | Ambiguous unavailable observation unresolved | INTEGRATION | G16-071 | T22 | When post-send observation is unavailable/unknown: PRODUCTION_OUTCOME_UNRESOLVED (or architecture-equivalent), retry=0, no second mutation, audit/recovery evidence persisted. | ProductionChangeAmbiguousOutcomeIT | OUTCOME_UNKNOWN; observation unavailable | ambiguousUnavailableUnresolved | PRODUCTION_OUTCOME_UNRESOLVED | 1 (no additional) | 0 | PRODUCTION_OUTCOME_UNRESOLVED | No second mutation | DEFAULT_CI |
| P16-E156 | Verification failure enters RECOVERY_REQUIRED without auto rollback | INTEGRATION | G16-074, G16-075 | — | Verification mismatch/failure transitions to RECOVERY_REQUIRED and must not automatically execute rollback. | ProductionChangeVerificationIT | APPLY_WRONG_VALUE | verificationFailureRecoveryNotAutoRollback | RECOVERY_REQUIRED; no rollback attempt | 1 forward / 0 rollback | 0 | PRODUCTION_VERIFICATION_MISMATCH | No automatic rollback | DEFAULT_CI |
| P16-E157 | Automatic rollback and emergency bypass prohibited | BEHAVIORAL | G16-075, G16-081 | — | No code path auto-triggers rollback or emergency governance bypass after verification/recovery failures. | ProductionChangeRollbackTest | RECOVERY_REQUIRED | noAutoRollbackOrEmergencyBypass | Remains RECOVERY_REQUIRED | 0 rollback | 0 | PRODUCTION_ROLLBACK_BLOCKED | Rollback only via governed API | DEFAULT_CI |
| P16-E158 | App and gateway preflights re-check window | INTEGRATION | G16-084 | T26 | Both application pre-grant and gateway final preflight re-check change window; closed window denies with zero mutation. | ProductionChangeWindowIT | Window closed at gateway | windowRecheckDeniesSend | Deny | 0 | 0 | PRODUCTION_WINDOW_CLOSED | 0 mutations | DEFAULT_CI |
| P16-E159 | Failure taxonomy non-generic no catch-and-retry | BEHAVIORAL | G16-101 | — | Failures map to specific production reason codes; generic catch-and-retry around mutation is prohibited. | ProductionChangeFailureTaxonomyTest | Injected failure classes | specificReasonCodesNoCatchRetry | Durable failure code | 0 or 1 by class | 0 auto | class-specific | No generic retry wrapper | DEFAULT_CI |
| P16-E160 | Closed-loop optimization not authorized | BEHAVIORAL | G16-106 | — | No closed-loop optimizer/scheduler path can execute production mutation. | ProductionChangeAutomationBoundaryTest | Optimizer principal/path | closedLoopExecuteDenied | Unchanged | 0 | 0 | PRODUCTION_UNAUTHORIZED | Denied | DEFAULT_CI |
| P16-E161 | Phase16 implementation artifacts absent at specification time | STRUCTURAL | G16-111 | — | Repository has no V17 migration, no productionchange/productionwritegateway Java packages, and no production write transport configuration at specification time. | ProductionChangeBaselineTest | Repo scan | phase16ImplementationAbsent | N/A | N/A | N/A | — | Absent | DEFAULT_CI |
| P16-E162 | Gateway rejects self-contained grant as sole authority | INTEGRATION | G16-118 | T08 | Caller-supplied grant JWT/blob without durable DB grant lookup is denied; durable store is sole authority. | ProductionChangeGatewayAuthIT | Self-contained grant only | selfContainedGrantRejected | Deny | 0 | 0 | PRODUCTION_GRANT_MISSING | Denied | DEFAULT_CI |
| P16-E163 | App-gateway request carries grantId/correlation only | INTEGRATION | G16-119 | T02 | App→gateway request with mutation payload/endpoint overrides is rejected; only grantId/correlation (and auth) accepted. | ProductionChangeGatewayAuthIT | Request with mutation override | requestRejectsMutationPayloadAuthority | Deny | 0 | 0 | PRODUCTION_INVALID_REQUEST | Denied | DEFAULT_CI |
| P16-E164 | Application vs gateway preflight split enforced | INTEGRATION | G16-125 | — | App pre-grant preflight and gateway final pre-mutation preflight are distinct services; passing app preflight alone cannot send. | ProductionChangePreflightSplitIT | App preflight pass; gateway final fail | appPassGatewayFailDeniesSend | Grant may exist; send denied | 0 | 0 | PRODUCTION_PREFLIGHT_DENIED | 0 mutations | DEFAULT_CI |
| P16-E165 | Gateway final preflight UNKNOWN/INVALID denies send | INTEGRATION | G16-126 | T09,T41,T42,T48 | If any mandatory final gateway preflight input is UNKNOWN/INVALID/STALE/DISABLED/MISMATCHED, vendor mutation=0, retry=0, durable denial and sanitized reason exist. | ProductionGatewayFinalPreflightIT | Final preflight UNKNOWN after consume | finalPreflightUnknownDeniesSend | Attempt PRE_SEND deny / EXECUTE_DENIED | 0 | 0 | PRODUCTION_PREFLIGHT_DENIED | mutation=0; durable deny; sanitized reason | DEFAULT_CI |
| P16-E166 | App vs gateway durable state ownership enforced | INTEGRATION | G16-133 | T40 | App cannot authoritatively write gateway attempt/evidence outcomes; gateway owns attempt/evidence; app mirrors only with evidence. | ProductionChangeEvidenceAuthorityTest | App writes attempt outcome | appCannotOwnGatewayAttemptState | Rejected / unchanged | 0 | 0 | PRODUCTION_EVIDENCE_REQUIRED | Ownership enforced | DEFAULT_CI |
| P16-E167 | Target administration requires privileged permission | BEHAVIORAL | G16-140 | T44 | Target create/update/suspend/disable requires ADMINISTER_PRODUCTION_TARGET (or equivalent); ordinary execute cannot. | ProductionChangeTargetAdminTest | Non-admin principal | targetAdminRequiresPrivilege | Deny admin mutation | 0 | 0 | PRODUCTION_UNAUTHORIZED | Denied | DEFAULT_CI |
| P16-E168 | Critical mutation evidence survives audit subsystem failure | INTEGRATION | G16-148, G16-100 | T32 | After MAY_HAVE_SENT, critical mutation outcome evidence persists even if audit append fails. | ProductionChangeAuditResilienceIT | Audit append fails post-send | mutationEvidenceSurvivesAuditFailure | OUTCOME persisted independently | 1 | 0 | — | Outcome survives | DEFAULT_CI |
| P16-E169 | Change-control validator cannot be requester | BEHAVIORAL | G16-153 | T01 | MANUAL change-control validatedByPrincipalId must not equal requesterPrincipalId. | ProductionChangeControlTest | Requester self-validates CC | requesterCannotSelfValidateChangeControl | CC rejected | 0 | 0 | PRODUCTION_SOD_VIOLATION | Denied | DEFAULT_CI |
| P16-E170 | No exact-once external mutation claim | BEHAVIORAL | G16-051 | — | API/docs/metrics must not claim exactly-once external network mutation; ambiguous outcomes use OUTCOME_UNKNOWN. | ProductionChangeHonestyContractTest | Public contracts | noExactOnceClaim | N/A | N/A | N/A | — | No exact-once claim | DEFAULT_CI |
| P16-E171 | No distributed ACID claim with vendor | BEHAVIORAL | G16-052 | — | API/docs must not claim distributed ACID with vendor systems; REQUIRES_NEW local durability only. | ProductionChangeHonestyContractTest | Public contracts | noDistributedAcidClaim | N/A | N/A | N/A | — | No distributed ACID claim | DEFAULT_CI |
| P16-E172 | Code paths do not activate Level 4 | BEHAVIORAL | G16-109, G16-144 | — | Default code/config cannot mark certification Level 4; L4 remains external checklist only. | ProductionChangeCertificationTest | Default runtime | level4NotActivatedByCode | Level <=3/0 default | 0 | 0 | — | L4 not satisfied by code | DEFAULT_CI |
| P16-E173 | Rollback ambiguous outcome no blind retry | INTEGRATION | G16-080 | T22 | Rollback MAY_HAVE_SENT / ROLLBACK_OUTCOME_UNKNOWN does not blind-retry rollback mutation. | ProductionChangeRollbackIT | Rollback response lost | rollbackAmbiguousNoBlindRetry | ROLLBACK_OUTCOME_UNKNOWN | <=1 rollback | 0 | PRODUCTION_OUTCOME_UNKNOWN | No second rollback mutation | DEFAULT_CI |

### 55.1 Evidence totals

```text
TOTAL=173
STRUCTURAL=32
BEHAVIORAL=57
INTEGRATION=55
INFRASTRUCTURE=9
EXTERNAL_CERTIFICATION=20
GENERIC_PLACEHOLDERS=0
```

### 55.2 High-risk runtime proof index

Each entry maps to concrete BEHAVIORAL/INTEGRATION evidence (STRUCTURAL only where runtime proof is impossible, e.g. Phase 11 interface contract).

| Proof topic | Evidence ID(s) | Types |
|---|---|---|
| atomic consume full binding | P16-E051, P16-E052, P16-E053, P16-E054, P16-E055, P16-E056 | INTEGRATION |
| concurrent consume | P16-E051 | INTEGRATION |
| wrong-binding consume denial | P16-E052, P16-E053, P16-E054, P16-E055, P16-E056, P16-E059 | INTEGRATION |
| grant replay | P16-E060, P16-E081 | INTEGRATION |
| consume-before-attempt | P16-E061 | INTEGRATION |
| post-consume/pre-attempt crash | P16-E062 | INTEGRATION |
| attempt-before-send durability | P16-E063 | INTEGRATION |
| kill switch after grant issue | P16-E030 | BEHAVIORAL |
| target suspended after grant issue | P16-E031 | BEHAVIORAL |
| change-control invalidated after grant | P16-E032 | BEHAVIORAL |
| fencing changed after grant | P16-E082 | INTEGRATION |
| shared durable rate-limit race | P16-E065 | INTEGRATION |
| grant issuance abuse limit | P16-E041, P16-E042 | BEHAVIORAL |
| stable principal SoD | P16-E023, P16-E024, P16-E025 | BEHAVIORAL |
| caller mutation authority denied | P16-E026, P16-E027 | BEHAVIORAL |
| direct vendor observation | P16-E150, P16-E033, P16-E034 | BEHAVIORAL, INTEGRATION |
| expected-state mismatch | P16-E033 | BEHAVIORAL |
| expected-state unavailable | P16-E034 | BEHAVIORAL |
| ATOMIC capability denial | P16-E035 | BEHAVIORAL |
| READ_THEN_WRITE policy denial | P16-E036 | BEHAVIORAL |
| mutation retry disabled | P16-E038 | BEHAVIORAL |
| MAY_HAVE_SENT no retry | P16-E037 | BEHAVIORAL |
| vendor accepted != verified | P16-E152, P16-E070 | INTEGRATION |
| vendor applied/response lost | P16-E069 | INTEGRATION |
| expected-value ambiguous recovery | P16-E153 | INTEGRATION |
| third-value ambiguous recovery | P16-E154 | INTEGRATION |
| unavailable ambiguous recovery | P16-E155 | INTEGRATION |
| independent verification | P16-E070, P16-E076 | INTEGRATION |
| verification persistence failure | P16-E071 | INTEGRATION |
| rollback authorization | P16-E077 | INTEGRATION |
| rollback grant consume race | P16-E078 | INTEGRATION |
| rollback expected-state mismatch | P16-E079 | INTEGRATION |
| rollback ambiguous outcome | P16-E173 | INTEGRATION |
| Phase12 canonical isolation | P16-E046, P16-E084 | INTEGRATION |
| Phase11 read-only | P16-E016 | STRUCTURAL |
| main app credential isolation | P16-E021, P16-E102 | EXTERNAL_CERTIFICATION, STRUCTURAL |
| gateway credential resolution | P16-E072, P16-E073 | INTEGRATION |
| audit concurrent append | P16-E066 | INTEGRATION |
| audit gap detection | P16-E068 | INTEGRATION |
| audit tamper detection | P16-E067 | INTEGRATION |
| target change invalidates authorization | P16-E028, P16-E029 | BEHAVIORAL |
| target change invalidates unconsumed grant | P16-E029 | BEHAVIORAL |
| app pre-grant UNKNOWN deny | P16-E149 | INTEGRATION |
| gateway final preflight deny | P16-E165 | INTEGRATION |

### 55.3 Architecture-correct mapping note (G16-053 vs G16-126)

Frozen architecture gate **G16-053** is *application pre-grant preflight UNKNOWN ⇒ DENY (no grant)* and is proven by **P16-E149**. Gateway *final* pre-mutation preflight safe denial is architecture gate **G16-126**, proven by **P16-E165**. Both runtime proofs are mandatory; they are not interchangeable.

### 55.4 Correction closure status

```text
B16-S2-01 CLOSED — former generic G16 circular evidence rows removed/replaced
B16-S2-02 CLOSED — G16-053/054/060/061/064/068/069 have BEHAVIORAL/INTEGRATION evidence
B16-S2-03 CLOSED — residual generic requirement boilerplate removed from catalog
B16-S-01 CLOSED — semantic gate↔evidence mapping rebuilt
B16-S-02 CLOSED — zero generic placeholders
C16-S-01 REMAINS CLOSED
C16-S-02 REMAINS CLOSED
STRUCTURAL-ONLY HIGH-RISK GATES = 0
EVIDENCE TOTAL = 173
```

---

## 56. Critical scenarios — A–Z (fully expanded)

| ID | Title | Setup | Action | Expected production-change state | Expected grant state | Expected attempt state | Vendor mutations | Auto retries | Verification/readback | Audit evidence | Reason code | Target health |
|---|---|---|---|---|---|---|---:|---:|---|---|---|---|
| A | Normal governed simulator path | L0 target; valid P13-P16 chain; open window | POST execute with valid grant handoff | VERIFIED | CONSUMED | VERIFIED | 1 | 0 | Independent readback matches desired | Full audit chain + gateway evidence | — | — |
| B | Expected state changes before execute | Authorized; vendor observation will mismatch | POST execute | AUTHORIZED or PREFLIGHT_DENIED | ISSUED or none | none | 0 | 0 | Not performed | PREFLIGHT_FAILED audit | PRODUCTION_VENDOR_STATE_MISMATCH | — |
| C | Timeout after apply MAY_HAVE_SENT | Simulator TIMEOUT_AFTER_APPLY | POST execute | OUTCOME_UNKNOWN then VERIFIED | CONSUMED | VERIFIED | 0 or 1 | 0 | Readback desired after ambiguous | OUTCOME_UNKNOWN + VERIFICATION events | — | — |
| D | Wrong resulting state | Simulator APPLY_WRONG_VALUE | POST execute | VERIFICATION_FAILED / RECOVERY_REQUIRED | CONSUMED | VERIFICATION_FAILED | 1 | 0 | Mismatch observed | VERIFICATION_FAILED audit | PRODUCTION_VERIFICATION_MISMATCH | May trigger SUSPENDED |
| E | Rollback without authorization | Forward VERIFIED; no rollback auth | POST rollback/execute | RECOVERY_REQUIRED | none rollback | none | 0 | 0 | N/A | ROLLBACK deny audit | PRODUCTION_ROLLBACK_BLOCKED | — |
| F | Authorized rollback success | Forward VERIFIED; rollback reviewed/authorized | POST rollback/execute | ROLLED_BACK | CONSUMED rollback | VERIFIED rollback | 1 rollback | 0 | Rollback readback desired | Rollback audit chain | — | — |
| G | Rollback current mismatch | Rollback authorized; expected state wrong | POST rollback/execute | MANUAL_INTERVENTION_REQUIRED | CONSUMED or none | VENDOR_REJECTED/NOT_SENT | 0 | 0 | Guard blocks | Rollback guard fail | PRODUCTION_VENDOR_STATE_MISMATCH | — |
| H | Concurrent same target scope | Two changes same cell/param | Parallel execute | One active lease | At most one CONSUMED forward | At most one active attempt | <=1 total | 0 | Winner only verifies | Lease conflict for loser | PRODUCTION_LEASE_CONFLICT | — |
| I | Target substitution after authorization | Target fingerprint changes | POST execute | AUTHORIZATION_STALE | ISSUED revoked or deny | none | 0 | 0 | N/A | STALE auth audit | PRODUCTION_FINGERPRINT_STALE | — |
| J | Duplicate execute after VERIFIED | Terminal VERIFIED exists | POST execute again | VERIFIED unchanged | CONSUMED | VERIFIED | 0 additional | 0 | Reconstruct only | Idempotent execute audit | — | — |
| K | Window expires after authorization | Window closed at gateway | POST execute | AUTHORIZED but execute denied | ISSUED or none | none/PRE_SEND deny | 0 | 0 | N/A | WINDOW deny | PRODUCTION_WINDOW_CLOSED | — |
| L | Stale fencing token | Fencing incremented elsewhere | POST execute | EXECUTE_DENIED | ISSUED or CONSUMED deny | none | 0 | 0 | N/A | FENCING deny | PRODUCTION_FENCING_MISMATCH | — |
| M | Stale cached readback | Verification uses stale observation | Verify path | VERIFICATION_FAILED | CONSUMED | VERIFICATION_FAILED | <=1 prior | 0 | Reject stale | STALE_OBSERVATION | PRODUCTION_VERIFICATION_UNAVAILABLE | — |
| N | Canonical DB unchanged | Successful production VERIFIED | POST execute | VERIFIED | CONSUMED | VERIFIED | 1 | 0 | Normal verify | Canonical isolation audit | — | — |
| O | Canonical via Phase12 only | After VERIFIED | Wait for Phase12 sync | NETWORK_SYNCHRONIZATION_REQUIRED | CONSUMED | VERIFIED | 1 | 0 | Phase12 reconciles | Sync signal only | — | — |
| P | Agent denied | Agent principal | Agent invoke execute | unchanged | none | none | 0 | 0 | N/A | Agent deny audit | PRODUCTION_UNAUTHORIZED | — |
| Q | MCP denied | MCP client | MCP tool call | unchanged | none | none | 0 | 0 | N/A | MCP deny | PRODUCTION_UNAUTHORIZED | — |
| R | Rollback outcome unknown | Rollback MAY_HAVE_SENT | Rollback execute + readback | ROLLBACK_OUTCOME_UNKNOWN or safe stop | CONSUMED | OUTCOME_UNKNOWN | 0 or 1 rollback | 0 | Readback governs | Ambiguous rollback audit | PRODUCTION_OUTCOME_UNKNOWN | — |
| S | Third state ambiguous forward | Readback neither expected nor desired | Recovery evaluate | MANUAL_INTERVENTION_REQUIRED | CONSUMED | OUTCOME_UNKNOWN/VERIFYING | 0 or 1 | 0 | No auto retry | Manual intervention audit | PRODUCTION_MANUAL_INTERVENTION_REQUIRED | — |
| T | Rate limit race | Two gateway instances near limit | Parallel execute | One succeeds | One CONSUMED | One attempt | <=1 mutation total | 0 | Normal for winner | Rate limit deny for loser | PRODUCTION_RATE_LIMIT_EXCEEDED | — |
| U | Concurrent grant consume | Two gateway workers same grantId | Simultaneous consume | One proceeds | One CONSUMED one deny | One PRE_SEND attempt max | 0 at consume test | 0 | N/A at consume | Consume conflict metrics | PRODUCTION_GRANT_ALREADY_CONSUMED | — |
| V | Revoked grant at consume | Grant REVOKED before consume | Gateway consume | EXECUTE_DENIED | REVOKED | none | 0 | 0 | N/A | Revoke audit | PRODUCTION_GRANT_REVOKED | — |
| W | Consume then crash before attempt | FI-03 injection | Execute then crash | CONSUMED_PRE_SEND_RECOVERY_REQUIRED | CONSUMED | none | 0 | 0 | N/A | Grant consumed audit without attempt | PRODUCTION_OUTCOME_UNKNOWN | — |
| X | Kill switch after grant | Grant ISSUED then global disabled | Gateway send | AUTHORIZED/grant issued | CONSUMED or ISSUED | PRE_SEND deny | 0 | 0 | N/A | Kill switch deny | PRODUCTION_DISABLED | — |
| Y | Target suspended after grant | Grant ISSUED then SUSPENDED | Gateway send | SUSPENDED deny | ISSUED/CONSUMED | none/PRE_SEND deny | 0 | 0 | N/A | Suspend deny | PRODUCTION_TARGET_SUSPENDED | SUSPENDED |
| Z | Change-control expired after grant | Ticket expires | Gateway send | CC invalid deny | ISSUED | none | 0 | 0 | N/A | CC deny audit | PRODUCTION_CHANGE_CONTROL_INVALID | — |

All scenarios **MUST** map to integration/behavioral evidence in §55. Ambiguous outcomes: vendor mutation count may be 0 or 1; automatic retry count **MUST** be 0.

## 57. Acceptance commands

Before implementation completion sign-off:

```bash
mvn -B clean test
cd simulator
go test ./...
go build ./cmd/simulator
git diff --check
```

Additional targeted commands (minimum):

```bash
mvn -B test -Dtest=ProductionChange*Test,ProductionWriteGateway*Test,ProductionChangeArchitectureIsolationTest
mvn -B test -Dtest=ProductionChangeLifecycleIntegrationTest,ProductionGrantConcurrentConsumeIntegrationTest
```

Default CI **MUST** remain Azure-independent, vendor-independent, credential-independent. Do not claim Go CI unless workflow runs it.

Mandatory closure targets:

```text
173 / 173 VERIFIED PASS (actual count in §55)
154 / 154 ARCHITECTURE GATES PASS
0 EVIDENCE INSUFFICIENT
0 FAIL
```

Never inflate totals. Report actual results in completion report.

---

## 58. Completion report format

Create on implementation completion:

```text
docs/implementation/SNIP-PHASE-16-VENDOR-WRITE-INTEGRATION-SECURITY-PRODUCTION-CHANGE-CONTROL-CONTROLLED-REAL-NETWORK-EXECUTION-COMPLETION-REPORT.md
```

Report **MUST** include:

1. Architecture baseline commit and SHA-256
2. Phase 15 parent baseline confirmation
3. V17 migration applied (yes/no)
4. Module list and main class names
5. Production/test file inventory
6. Evidence catalog totals by type (STRUCTURAL/BEHAVIORAL/INTEGRATION/INFRA)
7. Architecture gate matrix totals
8. Critical scenarios A–Z results
9. Targeted test commands and results
10. Full Maven and Go results
11. `git diff --check` result
12. Canonical mutation status (must remain isolated)
13. EnmTransport read-only confirmation
14. Write credential resolution boundary confirmation
15. Agent/MCP/scheduler/event isolation confirmation
16. Real Ericsson E2E status (manual/separate)
17. Certification level achieved (default L0)
18. Working tree status
19. Git mutations (must be none unless explicitly authorized)

---

## 59. Git lifecycle — STOP before commit/push

Implementation agent **MUST STOP** before:

- `git commit`
- `git push`
- establishing Phase 16 implementation Git baseline

unless the human operator explicitly authorizes Git mutation in the same session.

Preservation rules:

- Do **not** amend Phase 15 immutable baseline `ae9c13d55b444fa50090813495b32b82f97c2ec3`
- Do **not** rewrite failed Phase 15 candidate `0cb1223e41ced5462ad552f993e6001a028ddb96`
- Do **not** amend frozen Phase 16 architecture baseline without explicit authorization

---

## 60. Explicit non-goals

Phase 16 implementation **MUST NOT** include:

- Nokia NetAct write adapter
- generic Ericsson command execution / REST proxy
- SSH/CLI execution
- caller-supplied vendor endpoint or mutation values
- agent/MCP-triggered production mutation
- scheduled or event-driven production execution
- automatic rollback or emergency governance bypass
- closed-loop optimization
- multi-cell / multi-parameter / bulk production changes
- production writes inside ordinary SNIP app JVM
- extending Phase 11 `EnmTransport` with write methods
- production Ericsson write transport until protocol evidenced and separately certified
- actual production credentials/endpoints committed to repository
- Level 4 activation in default CI or code paths
- Phase 17 capabilities
- ServiceNow/ITSM deep integration
- direct canonical `radio_configuration` mutation by Phase 16
- exactly-once external network mutation claims
- distributed ACID claims with vendor systems

---

## 61. Core services checklist

Implement at minimum in SNIP application:

```text
ProductionChangeService
ProductionAdmissionService
ProductionReviewService
ProductionAuthorizationService
ProductionFingerprintService
ProductionTargetRegistry
ProductionTargetAdministrationService
ProductionLeaseService
ProductionPreGrantPreflightService
ProductionExecutionGrantService
ProductionGrantExpiryService
ProductionExecutionOrchestrationService
ProductionExecutionSyncService
ProductionVerificationSyncService
ProductionRecoveryService
ProductionRollbackRequestService
ProductionRollbackReviewService
ProductionRollbackAuthorizationService
ProductionRollbackOrchestrationService
ProductionRollbackSyncService
ProductionChangeControlService
ProductionChangeAuditService
ProductionRateLimitService
ProductionTargetHealthService
ProductionChangeMetrics
ProductionFailurePersistenceService
ProductionWriteGatewayClient
```

Implement at minimum in Production Write Gateway:

```text
GatewayAdmissionService
ProductionGrantConsumeService
ProductionGatewayAttemptService
ProductionGatewayEvidenceService
ProductionGatewayPreflightService
ExpectedStateObservationService
ProductionVerificationService
VendorNetworkWriteAdapter (wired)
EricssonEnmWriteAdapter
EricssonWriteTransport (lab/test + NOT CONFIGURED production stub)
ProductionGatewayAuditService
ProductionGatewayMetrics
ProductionCredentialResolutionService (gateway only)
ProductionKillSwitchEnforcementService
ProductionGatewayRateLimitEnforcementService
```

Reuse authoritative Phase 12/13/14/15 services for upstream validity — do not duplicate planning/knowledge logic.

---

## Final status

```text
PHASE 16 IMPLEMENTATION SPECIFICATION: COMPLETE — AUTHORIZED FOR IMPLEMENTATION
PHASE 16 IMPLEMENTATION: NOT STARTED
PHASE 16 IMPLEMENTATION BASELINE: NOT YET ESTABLISHED
REAL PRODUCTION EXECUTION: NOT AUTHORIZED
ERICSSON PRODUCTION WRITE TRANSPORT: NOT CONFIGURED / UNRESOLVED
NOKIA PRODUCTION WRITE: DEFERRED / NOT IMPLEMENTED
V17 MIGRATION: NOT CREATED
PRODUCTION WRITE GATEWAY RUNTIME: NOT DEPLOYED
LEVEL 4 CERTIFICATION: NOT ACHIEVED
AGENT PRODUCTION MUTATION: NOT AUTHORIZED
MCP PRODUCTION MUTATION: NOT AUTHORIZED
AUTOMATIC PRODUCTION EXECUTION: NOT AUTHORIZED
AUTOMATIC ROLLBACK: NOT AUTHORIZED
CLOSED-LOOP OPTIMIZATION: NOT AUTHORIZED
PHASE 17: NOT STARTED
```
