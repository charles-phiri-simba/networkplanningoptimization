# SNIP Phase 11 — First Real Vendor Connector & Production Read-Only Integration Architecture

**Repository:** `https://github.com/charles-phiri-simba/networkplanningoptimization.git`  
**Phase 10 immutable baseline:** `c7d85e32ee5871d23855784d141ae66c68655bfa`  
**Parent phase:** Phase 10 — Production Secret Integration, Workload Identity & Connector Runtime Security  
**Architecture status:** ACCEPTED  
**Implementation status:** COMPLETE — ARCHITECTURALLY ACCEPTED  
**Phase 11 baseline:** NOT YET ESTABLISHED  
**Phase 12:** NOT STARTED

---

## 1. Purpose

Phase 11 introduces SNIP's first production-oriented real-vendor integration boundary.

The objective is to allow SNIP to observe a real vendor-managed network through a strictly read-only connector, translate vendor-specific inventory/configuration into SNIP's existing vendor-neutral snapshot and canonical network model, and preserve all Phase 7–10 synchronization, security, lease, fencing, reconciliation, and secret-management guarantees.

Phase 11 does **not** introduce network mutation.

> **Phase 11 does not teach SNIP how to change a network. It teaches SNIP how to safely observe a real one.**

The first real-vendor target is **Ericsson ENM**. Nokia NetAct remains architecturally supported by the existing multi-vendor model but is explicitly deferred as a real connector.

---

## 2. Architectural Goals

Phase 11 SHALL establish a production-grade read-only integration architecture that:

1. integrates one real Ericsson ENM source without weakening Phase 7–10 boundaries;
2. retains the existing SNIP application deployment boundary;
3. prevents Ericsson-specific models from leaking into SNIP domain code;
4. introduces an explicit vendor transport abstraction;
5. supports bounded, cancellable, deadline-aware vendor acquisition;
6. protects canonical state from partial, failed, stale, duplicated, or ambiguous vendor snapshots;
7. preserves Phase 8 distributed lease and fencing authority;
8. preserves Phase 9 read-only and transport-security controls;
9. preserves Phase 10 Workload Identity and Key Vault credential architecture;
10. records source provenance for imported network facts;
11. prevents Agents, MCP, Phase 4 Actions, and schedulers from directly accessing the real vendor connector;
12. keeps default CI deterministic and vendor-independent;
13. provides a separate, controlled real-vendor E2E acceptance gate.

---

## 3. Non-Goals

Phase 11 SHALL NOT implement:

- Nokia NetAct as a real production connector;
- network configuration writes;
- command execution;
- parameter changes;
- automated remediation;
- autonomous network actions;
- Agent-to-ENM access;
- MCP-to-ENM access;
- Phase 4 Action-to-ENM access;
- scheduled production polling;
- streaming telemetry ingestion;
- automatic vendor discovery;
- raw vendor-payload archival by default;
- simultaneous production import from multiple real vendors;
- connector microservice extraction;
- vendor write credentials;
- production write authorization;
- production network mutation APIs;
- speculative support for unverified Ericsson ENM API families or releases.

---

## 4. Architectural Starting Point

Phase 11 starts from the immutable Phase 10 Git baseline:

```text
c7d85e32ee5871d23855784d141ae66c68655bfa
```

The following frozen capabilities are inherited:

- Phase 7 multi-vendor normalization and reconciliation;
- Phase 8 PostgreSQL lease, fencing, retry/replay, watchdog, and synchronization runtime;
- Phase 9 connector security foundation, read-only contract, TLS/mTLS envelope, and egress controls;
- Phase 10 Azure Key Vault production secret resolution;
- AKS Workload Identity with dedicated UAMI;
- secret-scoped Key Vault RBAC;
- per-session credential resolution;
- no older-version fallback;
- no vendor-secret value cache;
- fail-closed connector initialization;
- secure mock connector runtime;
- Azure-independent default CI.

Phase 11 SHALL extend these capabilities rather than replace them.

---

## 5. First Real Vendor

The first real-vendor target is:

```text
Vendor:         ERICSSON
Platform:       ENM
Access mode:    READ_ONLY
Initial scope:  INVENTORY_READ / CONFIGURATION_READ
```

The existing secure mock connectors remain available.

```text
Phase 11 connector estate
 ├── Ericsson MOCK_SECURE
 ├── Nokia MOCK_SECURE
 ├── Ericsson ENM REAL_READ_ONLY
 └── Nokia NetAct REAL_READ_ONLY      [deferred]
```

Only the Ericsson ENM real connector is in Phase 11 scope.

---

## 6. Connector Deployment Boundary

Phase 11 SHALL retain the connector inside the existing SNIP runtime.

```text
SNIP application
      │
      ├── Phase 8 Import Runtime
      ├── Phase 9 Connector Security
      ├── Phase 10 Credential Runtime
      └── Connector SPI
              │
              ├── Ericsson Secure Mock
              ├── Nokia Secure Mock
              └── Ericsson ENM Read-Only Connector
```

Phase 11 SHALL NOT extract the connector into a standalone microservice.

This keeps the phase focused on validating the connector contract against a real vendor rather than simultaneously redesigning deployment topology.

---

## 7. Real-Vendor Trust Boundary

The following paths are forbidden:

```text
Agents ───────────────X
MCP ──────────────────X
Phase 4 Actions ──────X
Scheduler ────────────X
```

The only authorized runtime path is:

```text
Authorized Import API
        │
        ▼
Phase 8 Import Runtime
        │
        ▼
Phase 10 Security Runtime
        │
        ▼
Ericsson READ_ONLY Connector
        │
        ▼
EnmTransport
        │
        ▼
Ericsson ENM
```

Agents may consume resulting canonical data after reconciliation, but SHALL NOT directly invoke, configure, authenticate to, or query ENM.

---

## 8. Explicit Connector Classification

Phase 11 SHALL introduce an explicit connector classification model.

```text
ConnectorDescriptor
 ├── vendor
 ├── platform
 ├── environment
 ├── implementationType
 ├── accessMode
 └── capabilities
```

Example:

```text
vendor             = ERICSSON
platform           = ENM
environment        = INT
implementationType = REAL
accessMode         = READ_ONLY
capabilities       =
    INVENTORY_READ
    CONFIGURATION_READ
```

The Phase 11 real connector SHALL NOT expose `CONFIGURATION_WRITE`, `COMMAND_EXECUTION`, `NETWORK_MUTATION`, or `PARAMETER_CHANGE`. These capabilities are absent, not merely disabled.

---

## 9. Production Transport Must Remain Abstract

The exact Ericsson production interface is intentionally unresolved until the available ENM integration mechanism is known.

Phase 11 SHALL introduce an abstraction equivalent to:

```text
EnmTransport
```

The architecture SHALL support:

```text
EricssonEnmConnector
        │
        ▼
EnmTransport
        │
        ├── SimulatorEnmTransport
        └── ProductionEnmTransport
```

The simulator/fixture transport is the default Phase 11 implementation and CI proof mechanism.

The concrete production transport SHALL NOT be guessed. It may later represent a supported Ericsson REST API, bulk CM interface, CLI-mediated interface, or another approved ENM integration surface.

---

## 10. Vendor Model Isolation

Ericsson-specific transport and source objects SHALL remain inside the Ericsson adapter.

SNIP domain services SHALL NOT depend directly on Ericsson model types such as `ManagedElement`, `ENodeBFunction`, `EUtranCellFDD`, `GNBCUCPFunction`, or `NRCellCU`.

The required transformation boundary is:

```text
Ericsson response
      │
      ▼
Ericsson source/transport model
      │
      ▼
Ericsson mapper
      │
      ▼
Vendor-neutral NetworkSnapshot
      │
      ▼
Phase 7 normalization/reconciliation
      │
      ▼
SNIP canonical network model
```

A future Nokia adapter SHALL map its own source representation into the same vendor-neutral snapshot boundary.

---

## 11. Three-Layer Data Representation

Phase 11 SHALL maintain three distinct representations.

### 11.1 Vendor Transport Model

Represents what Ericsson returned. It is vendor-specific, transport-specific, short-lived, not exposed through public APIs, and not persisted as the canonical model.

### 11.2 Vendor-Neutral Snapshot

Represents what the connector understood. It is vendor-neutral enough for Phase 7 reconciliation and includes completeness and provenance metadata.

### 11.3 SNIP Canonical Model

Represents what SNIP believes about the network. It remains vendor-neutral and authoritative for downstream SNIP intelligence.

---

## 12. Initial Inventory Scope

Phase 11 SHALL deliberately limit the first real-vendor inventory scope.

The first vertical slice SHOULD include:

```text
Managed Element / Network Node
        ↓
Radio Function
        ↓
Cell
```

The exact Ericsson source object names depend on the selected ENM transport/profile.

Phase 11 SHALL NOT attempt to import every managed-object class available in ENM.

---

## 13. Snapshot as the Unit of Vendor Observation

A real-vendor acquisition SHALL be represented as a snapshot.

```text
VendorSnapshot
 ├── snapshotId
 ├── sourceVendor
 ├── sourceSystem
 ├── connectorId
 ├── startedAt
 ├── completedAt
 ├── completeness
 ├── pagesExpected
 ├── pagesReceived
 ├── entitiesRead
 ├── warnings
 ├── sourceCursor
 ├── sourceVersion
 └── executionId
```

Not every transport must populate every optional field, but the model SHALL support them.

---

## 14. Snapshot Completeness

Required states:

```text
COMPLETE
PARTIAL
FAILED
```

Required reconciliation semantics:

```text
COMPLETE
    → create/update allowed
    → absence inference may be considered

PARTIAL
    → create/update allowed only under explicit safe policy
    → deletion/absence inference forbidden

FAILED
    → zero canonical mutation
```

A partial or failed acquisition SHALL NOT be interpreted as evidence that vendor objects were deleted.

---

## 15. Source Authority and Disappearance Semantics

A real ENM source may fail to report objects because of failed pages, permission changes, filtering, maintenance, topology partitions, inconsistent pagination, or replication delays.

Therefore Phase 11 SHALL NOT physically delete canonical entities merely because they are absent from one vendor snapshot.

Recommended lifecycle:

```text
Seen previously + seen now
    → ACTIVE

Newly seen
    → CREATE

Previously seen + absent from one trustworthy COMPLETE snapshot
    → NOT_OBSERVED / candidate stale

Repeated absence across sufficient trustworthy COMPLETE snapshots
    → candidate STALE
```

Hard-delete semantics are out of scope for Phase 11.

---

## 16. Atomic Reconciliation

Phase 11 SHALL prefer correctness over streaming mutation.

```text
Acquire vendor snapshot
      │
      ▼
Validate snapshot
      │
      ▼
Determine completeness
      │
      ▼
Normalize
      │
      ▼
Reconcile transactionally
```

The following pattern is forbidden:

```text
GET page 1 → mutate canonical DB
GET page 2 → mutate canonical DB
GET page 3 → FAIL
```

No canonical mutation SHALL occur until the snapshot has passed required validation and completeness gates.

---

## 17. Bounded Pagination

All vendor pagination SHALL be bounded.

The connector configuration SHALL support limits equivalent to:

```text
maxPages
maxEntities
pageSize
requestTimeout
overallExecutionTimeout
```

The connector SHALL detect and fail closed on repeated page tokens, repeated pages, continuation-token cycles, malformed cursors, unexpected empty pages with continuation, page-count overflow, entity-count overflow, and invalid pagination metadata.

No unbounded pagination loop is permitted.

---

## 18. Rate Limiting and Retry

The connector SHALL assume vendor throttling is possible.

At minimum, where relevant to the selected transport, the architecture SHALL understand `429`, `503`, and `Retry-After`.

Retries SHALL be bounded, deadline-aware, cancellation-aware, jittered where applicable, observable, and subordinate to the Phase 8 execution deadline.

Unlimited retry is forbidden.

---

## 19. Cooperative Cancellation

Cooperative cancellation SHALL be implemented for the Phase 11 connector path.

```text
ImportExecutionContext
 ├── executionId
 ├── deadline
 ├── fencingToken
 └── cancellationToken
```

Long-running connector operations SHALL check this context before credential resolution, before authentication, before every vendor request/page, during retry/backoff, before snapshot finalization, and before reconciliation.

Cancellation SHALL result in a safe failed/cancelled execution with no unsafe canonical mutation.

This closes the cooperative-cancellation debt for the real-vendor import path but does not automatically resolve the older Phase 5 Agent timeout debt.

---

## 20. Lease and Fencing During Long Acquisition

Phase 8 remains authoritative.

```text
lease acquired
    ↓
credential resolution
    ↓
session establishment
    ↓
page 1
    ↓
assert lease ownership
    ↓
page 2
    ↓
assert lease ownership
    ↓
...
    ↓
before reconciliation
    ↓
assert lease ownership
```

If ownership is lost:

```text
LEASE_LOST
    ↓
cancel vendor acquisition
    ↓
discard uncommitted snapshot
    ↓
zero unsafe canonical mutation
```

A stale replica SHALL NOT reconcile vendor data after losing fencing authority.

---

## 21. Credential Architecture

Phase 10 remains canonical.

The Ericsson connector SHALL receive a credential abstraction equivalent to `CredentialHandle` and SHALL NOT know whether the credential originated from Azure Key Vault, local development, or synthetic tests.

Production remains:

```text
AKS ServiceAccount
      ↓
Microsoft Entra Workload Identity
      ↓
Dedicated UAMI
      ↓
Azure Key Vault
      ↓
CredentialHandle
      ↓
Ericsson ENM connector
```

Azure SDK code SHALL NOT be introduced into `EricssonEnmConnector`.

---

## 22. Vendor Account Least Privilege

The actual ENM account used by SNIP SHALL be read-only and least-privileged.

```text
SNIP connector accessMode       READ_ONLY
+
connector implementation       READ_ONLY
+
vendor identity                READ_ONLY
+
network egress restriction
```

A production ENM credential with network-write authority is not acceptable for Phase 11.

---

## 23. HTTP / Protocol Method Enforcement

Where the selected production ENM interface is HTTP-based, connector operations SHOULD expose only explicit safe operations required for read access.

Generic arbitrary method dispatch SHOULD NOT be part of the connector contract.

Preferred conceptual API:

```text
get(...)
head(...)
```

rather than:

```text
exchange(method, ...)
```

If an authentication protocol requires POST, the architecture SHALL distinguish authentication POST from network-mutating POST. Any operation capable of mutating managed network state requires separate architecture approval and is out of Phase 11 scope.

---

## 24. TLS and mTLS

TLS server verification is mandatory.

Production SHALL NOT allow `trust-all`, disabled hostname verification, or insecure TLS.

mTLS SHALL be mandatory when required by the selected ENM deployment/profile, but is not universally mandated before the actual security profile is known.

Trust material continues through the existing Phase 9/10 security architecture.

---

## 25. Network Isolation

The production target is controlled private connectivity:

```text
SNIP AKS
   │
private routing
   │
firewall / egress policy
   │
Ericsson ENM
```

Real ENM traffic SHOULD NOT traverse uncontrolled public paths.

Application-level egress policy remains canonical; Kubernetes/Cilium/network infrastructure remains defense in depth.

---

## 26. Cilium FQDN Limitation as Deployment Gate

Phase 10 accepted a disposable-lab limitation in which Cilium `toFQDNs` did not obtain DNS proxy/FQDN-cache behavior and bounded Azure CIDRs were used instead.

For real vendor deployment, Phase 11 raises the requirement:

> Real vendor connectivity SHALL NOT be declared production-ready until the deployment environment has a controlled and validated egress mechanism for the ENM destination.

The solution may be working FQDN-aware policy, stable private IP allow-list, firewall rule, private route, or another approved platform mechanism.

A broad `0.0.0.0/0` vendor-access policy is not acceptable.

---

## 27. Vendor API / Profile Versioning

The Ericsson connector SHALL support an explicit API/profile abstraction.

```text
EricssonEnmConnector
        │
        ▼
EnmApiProfile
        │
        ├── supported profile A
        └── future profile B
```

A profile may define endpoint paths, supported source object types, pagination semantics, field mappings, protocol behavior, and capability flags.

Phase 11 SHALL implement only the profile actually proven through simulator and, where available, a real ENM environment. Speculative multi-version support is prohibited.

---

## 28. Connector Capability Discovery

Before acquisition, the runtime SHALL be able to determine connector capabilities.

```text
ConnectorCapabilities
 ├── INVENTORY_READ
 ├── CONFIGURATION_READ
 ├── PAGINATION
 ├── SOURCE_VERSION
 └── ...
```

The real Phase 11 connector SHALL NOT expose `WRITE`, `EXECUTE_COMMAND`, `MODIFY_PARAMETER`, or `NETWORK_MUTATION`.

---

## 29. Provenance

Every canonical fact originating from the real vendor SHALL be traceable to its source.

The provenance model SHALL support recovery of information equivalent to:

```text
sourceVendor
sourceSystem
sourceObjectType
sourceObjectId
sourceSnapshotId
observedAt
importExecutionId
```

The physical storage design may normalize or reference these values rather than duplicating them across all domain tables, but SNIP MUST be able to answer why it believes a network object or fact exists.

---

## 30. Raw Vendor Payload Retention

Complete raw ENM payloads SHALL NOT be persisted by default.

Phase 11 SHOULD persist snapshot metadata, source identifiers, normalized data, provenance, hashes/checksums where useful, and safe diagnostics.

A controlled raw-payload forensic/archive capability is deferred.

---

## 31. Observability

Phase 11 SHOULD expose safe metrics equivalent to:

```text
connectorSessions
connectorSessionFailures
vendorRequests
vendorRequestFailures
vendorThrottles
vendorRetries
pagesRead
entitiesRead
snapshotDuration
snapshotPartial
snapshotFailed
leaseLostDuringAcquisition
connectorCancellation
```

Metrics SHALL NOT expose usernames, passwords, tokens, cookies, authorization headers, raw vendor payloads, private keys, or high-cardinality sensitive vendor identifiers.

---

## 32. Audit Trail

A successful execution may include events equivalent to:

```text
IMPORT_REQUESTED
LEASE_ACQUIRED
CREDENTIAL_RESOLVED
NETWORK_POLICY_VALIDATED
TLS_VALIDATED
AUTHENTICATION_SUCCEEDED
VENDOR_SESSION_ESTABLISHED
SNAPSHOT_STARTED
PAGE_RECEIVED
SNAPSHOT_COMPLETED
RECONCILIATION_COMPLETED
SESSION_COMPLETED
LEASE_RELEASED
```

Failure events may include:

```text
SNAPSHOT_PARTIAL
VENDOR_RATE_LIMITED
VENDOR_TIMEOUT
VENDOR_AUTHENTICATION_FAILED
VENDOR_AUTHORIZATION_DENIED
VENDOR_PROTOCOL_ERROR
LEASE_LOST
CONNECTOR_CANCELLED
```

Audit records SHALL NOT contain secret values, tokens, private keys, or raw vendor responses.

---

## 33. Manual Import Only

The real ENM import SHALL be manually/explicitly initiated in Phase 11.

```text
authorized operator
      ↓
explicit import request
      ↓
Phase 8 runtime
      ↓
real connector
```

Scheduled production polling is deferred until real load, latency, pagination, throttling, and operational impact are understood.

---

## 34. Authorization Boundary

The real import endpoint SHALL require an explicit authorization distinct from general read-only SNIP usage.

The authorization permits triggering vendor read/import only. It SHALL NOT imply vendor configuration write, command execution, or network mutation.

The exact role mapping is deferred to the implementation specification.

---

## 35. Agents and AI Boundary

Agents SHALL remain consumers of canonical SNIP intelligence only.

Agents SHALL NOT hold ENM credentials, receive `CredentialHandle`, invoke `EnmTransport`, invoke `EricssonEnmConnector`, trigger real imports directly, bypass the authorized import API, or receive raw ENM transport payloads.

---

## 36. MCP Boundary

No MCP tool SHALL be added for direct ENM querying, connector-session triggering, credential exposure, raw ENM response exposure, or ENM mutation.

Phase 4 MCP governance remains unchanged.

---

## 37. Phase 4 Action Boundary

Phase 4 governed actions SHALL NOT be wired to the real ENM connector.

No Phase 11 code may translate a Phase 4 action into a vendor write.

Phase 11 is observation-only.

---

## 38. Failure Taxonomy

Phase 11 SHALL formalize vendor failures behind connector-safe codes.

Recommended codes:

```text
VENDOR_UNAVAILABLE
VENDOR_AUTHENTICATION_FAILED
VENDOR_AUTHORIZATION_DENIED
VENDOR_RATE_LIMITED
VENDOR_TIMEOUT
VENDOR_PROTOCOL_ERROR
VENDOR_RESPONSE_INVALID
VENDOR_PAGINATION_INVALID
SNAPSHOT_LIMIT_EXCEEDED
SNAPSHOT_PARTIAL
CONNECTOR_CANCELLED
LEASE_LOST
```

Vendor-specific responses SHALL remain inside the adapter. Public API and audit outputs SHALL expose safe normalized failure codes.

---

## 39. Retryability Classification

Failure codes SHALL be classified as retryable or non-retryable.

Example policy:

```text
VENDOR_RATE_LIMITED       retryable within budget
VENDOR_TIMEOUT            retryable within budget
VENDOR_UNAVAILABLE        retryable within budget

VENDOR_AUTHENTICATION_FAILED
                         non-retryable

VENDOR_AUTHORIZATION_DENIED
                         non-retryable

VENDOR_RESPONSE_INVALID
                         non-retryable

VENDOR_PAGINATION_INVALID
                         non-retryable
```

The final implementation specification SHALL lock the exact retry matrix.

---

## 40. Configuration Model

The real connector configuration SHOULD support fields equivalent to:

```text
connectorId
vendor
platform
environment
accessMode
apiProfile
baseEndpoint
credentialReference
trustReference
pageSize
maxPages
maxEntities
requestTimeout
overallExecutionTimeout
maxAttempts
initialBackoff
maxBackoff
```

Configuration SHALL NOT contain secret values.

---

## 41. Public API Exposure

Public APIs SHALL expose only safe connector/import metadata, such as connector id, vendor, platform, access mode, readiness, snapshot id, execution id, timing, entity counts, completeness, and normalized failure code.

Public APIs SHALL NOT expose credential values, tokens, cookies, private keys, raw authorization headers, raw vendor payloads, or Azure identity tokens.

---

## 42. Health and Readiness

Connector health/readiness SHALL distinguish configuration from live vendor availability.

Example fields may include:

```text
configured
credentialProviderConfigured
trustConfigured
networkPolicyConfigured
transportConfigured
```

A readiness endpoint SHOULD NOT cause an unbounded vendor query or Key Vault secret read.

Live vendor reachability may be exposed through an explicit diagnostic operation rather than a normal health probe.

---

## 43. Testing Strategy

### 43.1 Default CI

Default CI remains vendor-independent and SHALL include unit tests, connector contract tests, synthetic/recorded ENM protocol fixtures, simulator-backed protocol tests, secure mock regression, Phase 7–10 regression, pagination-cycle tests, rate-limit tests, timeout tests, cancellation tests, lease-loss tests, partial snapshot tests, provenance tests, and redaction tests.

Default CI SHALL NOT require Azure login, live Key Vault, real ENM, or real vendor credentials.

### 43.2 Controlled Simulator Integration

A local or CI-safe simulator SHALL model the selected ENM profile and SHOULD support deterministic scenarios including valid paginated inventory, malformed responses, 401, 403, 429, 503, timeout, repeated cursor, partial snapshot, snapshot limits, and cancellation.

### 43.3 Real Vendor E2E

Real ENM verification SHALL be manual or environment-gated, separate from default CI, authorized by the environment owner, read-only, least-privileged, sanitized in evidence, and executed only when real ENM access is available.

---

## 44. Two-Level Acceptance Model

### 44.1 Phase 11 Implementation Acceptance

May be achieved when architecture is implemented, simulator/contract proof passes, default CI passes, Phase 7–10 regressions pass, no mutation capability exists, and real-vendor E2E status is explicitly recorded.

### 44.2 Real Vendor E2E Verification

Requires an authorized real ENM environment and SHALL prove, where applicable, endpoint reachability, TLS verification, authentication, least-privileged read-only authorization, bounded acquisition, snapshot completeness, normalization, reconciliation, provenance, no mutation, redaction, and safe failure behavior.

If no real ENM environment is available, Phase 11 SHALL NOT claim `REAL VENDOR E2E VERIFIED`.

---

## 45. Real Credential Handling

Real ENM credentials SHALL NOT be required on developer workstations.

Developer testing uses synthetic credentials, simulator, and fixtures.

Production uses:

```text
AKS Workload Identity
      ↓
Azure Key Vault
      ↓
real read-only ENM credential
```

No real vendor credential belongs in Git, source code, Terraform state, developer `.env`, test fixtures, CI logs, or completion reports.

---

## 46. Security Defense in Depth

Phase 11 real-vendor access SHALL be protected by independent controls:

1. explicit connector `READ_ONLY` access mode;
2. read-only connector capabilities;
3. no write methods in the connector contract;
4. least-privileged vendor account;
5. Phase 9 application egress policy;
6. Kubernetes/network egress restriction;
7. TLS verification;
8. Phase 10 secret management;
9. Phase 8 lease/fencing;
10. authorization on the import trigger;
11. audit/redaction;
12. no direct Agent/MCP/Phase 4 connector access.

No single control is considered sufficient by itself.

---

## 47. Production Network Readiness

A real ENM deployment SHALL NOT be considered production-ready until the environment validates private or otherwise controlled routing, required DNS, firewall/egress restrictions, TLS trust, vendor endpoint identity, read-only vendor role, Key Vault access, Workload Identity, least-privilege secret RBAC, and no broad internet egress requirement for ENM access.

Key Vault Private Endpoint/private DNS remains a platform-level target inherited from Phase 10 and is not automatically implemented by Phase 11 application code.

---

## 48. Performance Model

Phase 11 SHALL avoid unbounded operations.

Required controls include bounded page size, page count, entity count, request timeout, retry count, backoff, overall execution deadline, cooperative cancellation, and lease ownership checks.

The architecture SHALL prefer predictable bounded failure over uncontrolled long-running acquisition.

---

## 49. Concurrency

Phase 8 lease keys remain the authority for concurrent import execution.

Phase 11 SHALL NOT add a second independent distributed lock system.

Different connector scopes may run concurrently where Phase 8 permits. The same connector/scope SHALL retain existing lease/fencing semantics.

---

## 50. Data Consistency

The canonical SNIP model SHALL never be mutated from an unvalidated snapshot, failed snapshot, stale fencing token, cancelled execution, or connector session that lost lease ownership.

Partial snapshot mutation, if permitted for safe create/update-only behavior, SHALL explicitly prohibit absence/deletion inference.

---

## 51. Replay

Phase 8 retry/replay semantics remain authoritative.

A repeated real-vendor import whose canonical snapshot is unchanged MAY result in replay/no-op semantics.

Replay SHALL NOT bypass authorization, lease acquisition, credential resolution, security validation, or fencing checks.

---

## 52. Schema Evolution

Phase 11 MAY add schema required for source provenance, snapshot completeness, source-system metadata, safe connector execution metadata, and new failure codes.

Schema changes SHALL use the existing migration mechanism.

No schema shall persist secret values, access tokens, raw authorization headers, private keys, or unrestricted raw vendor payloads.

---

## 53. Architecture Components

The implementation is expected to introduce components conceptually equivalent to:

```text
EricssonEnmConnector
EnmTransport
SimulatorEnmTransport
ProductionEnmTransport        [only when actual interface is selected]
EnmApiProfile
ConnectorDescriptor
ConnectorCapabilities
VendorSnapshot
SnapshotCompleteness
VendorSourceObject(s)
VendorSnapshotMapper
ImportExecutionContext
ConnectorCancellationToken
VendorFailureMapper
VendorRetryPolicy
SourceProvenance
```

Exact Java type names are deferred to the implementation specification.

---

## 54. End-to-End Logical Flow

```text
Authorized operator
       │
       ▼
Import API
       │
       ▼
Authorization
       │
       ▼
Phase 8 lease acquisition
       │
       ▼
ImportExecutionContext
(deadline / fencing / cancellation)
       │
       ▼
Phase 10 credential resolution
       │
       ▼
Phase 9 secure connector factory
       │
       ▼
EricssonEnmConnector
       │
       ▼
EnmTransport
       │
       ▼
Ericsson ENM
       │
       ▼
Vendor transport objects
       │
       ▼
Ericsson source mapper
       │
       ▼
Vendor-neutral snapshot
       │
       ├── completeness
       ├── provenance
       └── validation
       │
       ▼
Phase 7 normalization
       │
       ▼
Phase 8 reconciliation
       │
       ▼
Canonical Network Model
       │
       ├── Context Intelligence
       ├── Assurance
       └── Digital Twin
```

---

## 55. Failure Flow

```text
import requested
      ↓
lease acquired
      ↓
credential resolved
      ↓
session established
      ↓
pages 1..N acquired
      ↓
lease lost / timeout / invalid page
      ↓
cancellation
      ↓
snapshot FAILED or PARTIAL
      ↓
unsafe reconciliation blocked
      ↓
safe failure code
      ↓
audit
      ↓
lease release
```

No partial unvalidated canonical state may leak from this path.

---

## 56. Architecture Acceptance Gates

Phase 11 architecture SHALL NOT be frozen until all of the following decisions are explicitly accepted:

1. Ericsson ENM is the first real-vendor target.
2. Access remains strictly read-only.
3. Connector remains inside the current SNIP runtime.
4. Production transport is abstract and unresolved until the actual ENM interface is known.
5. Ericsson types remain isolated inside the adapter.
6. Snapshot completeness is first-class.
7. Failed snapshots cause zero canonical mutation.
8. Partial snapshots cannot infer deletion/absence.
9. Pagination, retries, timeouts, and entity counts are bounded.
10. Cooperative cancellation is added to the vendor connector path.
11. Lease/fencing checks continue throughout long acquisition.
12. Phase 10 Workload Identity/Key Vault architecture remains canonical.
13. Vendor credentials are least-privileged/read-only.
14. TLS verification is mandatory.
15. mTLS depends on the selected production ENM profile.
16. Provenance is mandatory.
17. Raw vendor payload persistence is disabled by default.
18. Manual authorized import is the only Phase 11 trigger.
19. Agents cannot directly access the real connector.
20. MCP cannot directly access the real connector.
21. Phase 4 Actions cannot directly access the real connector.
22. Scheduled production polling is deferred.
23. Default CI remains vendor-independent.
24. Real vendor E2E is a separate controlled acceptance gate.
25. Nokia real integration remains deferred.
26. Network write capability remains out of scope.

---

## 57. Open Architectural Item

The following item is intentionally unresolved:

> **Which Ericsson ENM production interface will SNIP be authorized to consume?**

The answer may determine concrete transport implementation, authentication exchange, pagination model, source-object representation, API profile, endpoint paths, request/response parsing, and rate-limit behavior.

No credentials, hostnames, IP addresses, tenant details, or production secrets are required to resolve this item.

Until resolved, Phase 11 SHALL use the `EnmTransport` abstraction plus simulator/contract fixtures.

---

## 58. Phase 11 Definition of Done

Phase 11 implementation SHALL eventually be considered complete when:

- Phase 10 baseline remains intact;
- the accepted architecture is implemented;
- Ericsson real-read-only connector contract exists;
- simulator-backed ENM transport proves the contract;
- connector capabilities/access mode are explicit;
- snapshot completeness semantics are implemented;
- pagination/retry/timeout bounds are enforced;
- cooperative cancellation works;
- lease-loss cancels acquisition safely;
- failed snapshots do not mutate canonical state;
- partial snapshots do not infer deletion;
- provenance is recoverable;
- Phase 10 credentials/security remain authoritative;
- all default CI tests pass;
- real vendor E2E status is explicitly recorded as either VERIFIED or NOT YET VERIFIED;
- no Phase 12 work has started;
- completion report is reviewed;
- architectural acceptance is explicit;
- Git baseline is established only after acceptance.

---

## 59. Proposed Acceptance Statement

After architectural review and explicit approval, the architecture may be frozen with:

```text
PHASE 11 ARCHITECTURE STATUS: ACCEPTED
```

This statement SHALL NOT imply that a real Ericsson ENM E2E has been performed.

Real-vendor verification must be recorded separately.

---

## 60. Frozen Phase Boundaries After Acceptance

When accepted, Phase 11 SHALL lock the following boundaries:

```text
SNIP may READ a real vendor network.

SNIP may NOT WRITE a real vendor network.

Only the governed import runtime may reach the vendor connector.

Agents, MCP, Phase 4 Actions, and schedulers do not receive direct vendor access.

Vendor data must cross:
vendor transport → vendor adapter → vendor-neutral snapshot → canonical model.

Failed, cancelled, stale, or untrusted acquisitions must not corrupt canonical state.

Phase 10 identity, secret, TLS, RBAC, and fail-closed guarantees remain authoritative.
```

---

## 61. Architecture Status

This document is a design artifact. Implementation was executed against the ingested Phase 11 specification and is **COMPLETE — ARCHITECTURALLY ACCEPTED**. Simulator/contract verification is not real-vendor E2E. Git baseline is **NOT YET ESTABLISHED**.

Current status:

```text
PHASE 11 ARCHITECTURE STATUS: ACCEPTED
PHASE 11 IMPLEMENTATION STATUS: COMPLETE — ARCHITECTURALLY ACCEPTED
REAL VENDOR E2E STATUS: NOT YET VERIFIED
PHASE 11 GIT BASELINE: NOT YET ESTABLISHED
PHASE 12 STATUS: NOT STARTED
```
