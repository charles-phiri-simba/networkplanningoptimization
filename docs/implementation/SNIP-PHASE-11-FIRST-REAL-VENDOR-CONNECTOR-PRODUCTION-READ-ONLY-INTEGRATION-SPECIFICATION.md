# SNIP Phase 11 --- First Real Vendor Connector & Production Read-Only Integration --- Implementation Specification

**Repository:**
`https://github.com/charles-phiri-simba/networkplanningoptimization.git`\
**Phase 10 immutable baseline:**
`c7d85e32ee5871d23855784d141ae66c68655bfa`\
**Architecture:**
`docs/architecture/SNIP-PHASE-11-FIRST-REAL-VENDOR-CONNECTOR-PRODUCTION-READ-ONLY-INTEGRATION-ARCHITECTURE.md`\
**Architecture status:** `PHASE 11 ARCHITECTURE STATUS: ACCEPTED`\
**Specification status:** READY FOR IMPLEMENTATION\
**Phase 11 implementation status at specification creation:** NOT
STARTED\
**Real vendor E2E status:** NOT YET VERIFIED\
**Phase 11 Git baseline:** NOT ESTABLISHED\
**Phase 12:** NOT STARTED

------------------------------------------------------------------------

## 1. Purpose

This specification translates the accepted Phase 11 architecture into an
implementation contract for Cursor.

Phase 11 introduces SNIP's first production-oriented **real-vendor
read-only connector boundary**, targeting **Ericsson ENM**, while
deliberately leaving the exact production ENM transport unresolved until
the actual authorized Ericsson interface is known.

The implementation SHALL prove the connector architecture through a
deterministic ENM simulator/fixture transport. It SHALL NOT fabricate or
claim a production Ericsson integration.

The defining rule remains:

> **Phase 11 does not teach SNIP how to change a network. It teaches
> SNIP how to safely observe a real one.**

No Phase 12 work is authorized.

------------------------------------------------------------------------

## 2. Authority and Precedence

Cursor SHALL treat the following as binding, in this order:

1.  immutable Phase 10 baseline and frozen Phase 7--10 behavior;
2.  accepted Phase 11 architecture;
3.  this Phase 11 implementation specification;
4.  existing repository conventions where they do not conflict with
    items 1--3.

If implementation requires violating an accepted architecture rule,
Cursor SHALL STOP and report the conflict rather than silently
reinterpret the design.

Cursor SHALL NOT infer an Ericsson production API, authentication
protocol, endpoint family, CLI, Bulk CM mechanism, object schema, or ENM
release that has not been explicitly selected.

------------------------------------------------------------------------

## 3. Preconditions

Before modifying code, Cursor SHALL:

1.  confirm the active branch is `main`;
2.  confirm the repository HEAD is still the Phase 10 baseline:
    `c7d85e32ee5871d23855784d141ae66c68655bfa`;
3.  confirm the Phase 11 architecture document is present and marked
    accepted;
4.  confirm no Phase 11 implementation has already been started;
5.  inspect existing Phase 7, 8, 9, and 10 connector/runtime/security
    abstractions before introducing new types;
6.  reuse existing repository naming/package conventions wherever
    compatible;
7.  avoid duplicate abstractions if an existing type can be safely
    extended.

If HEAD differs unexpectedly, or implementation code already exists
outside the authorized draft-document changes, STOP and report before
proceeding.

------------------------------------------------------------------------

## 4. Repository Document Canonicalization

The canonical Phase 11 architecture document SHALL be:

`docs/architecture/SNIP-PHASE-11-FIRST-REAL-VENDOR-CONNECTOR-PRODUCTION-READ-ONLY-INTEGRATION-ARCHITECTURE.md`

If an identical root copy exists solely as a temporary duplicate, remove
the root duplicate during Phase 11 implementation unless repository
conventions demonstrably require both copies.

Do not maintain two independently editable architecture sources.

Create the canonical implementation specification at:

`docs/implementation/SNIP-PHASE-11-FIRST-REAL-VENDOR-CONNECTOR-PRODUCTION-READ-ONLY-INTEGRATION-SPECIFICATION.md`

A temporary root copy of this specification is not required.

------------------------------------------------------------------------

## 5. Scope

### 5.1 In Scope

Phase 11 SHALL implement:

-   explicit connector classification;
-   explicit read-only connector capabilities;
-   Ericsson ENM connector orchestration;
-   an abstract `EnmTransport` boundary;
-   a deterministic simulator-backed ENM transport;
-   ENM API/profile abstraction without invented production endpoints;
-   vendor transport/source models isolated inside the Ericsson adapter;
-   vendor-neutral snapshot acquisition;
-   snapshot completeness states;
-   bounded pagination;
-   bounded retry/backoff;
-   request timeout and overall execution deadline;
-   cooperative cancellation on the vendor-import path;
-   lease/fencing revalidation during long acquisition;
-   safe COMPLETE/PARTIAL/FAILED reconciliation semantics;
-   source provenance;
-   normalized vendor failure taxonomy;
-   safe audit and metrics;
-   manual authorized import only;
-   Phase 10 credential/security integration;
-   database migrations required for snapshot/provenance metadata;
-   simulator/contract/unit/integration tests;
-   full Phase 1--10 regression;
-   documentation and completion report.

### 5.2 Out of Scope

Cursor SHALL NOT implement:

-   a guessed production Ericsson transport;
-   Nokia NetAct real integration;
-   vendor configuration writes;
-   network mutation;
-   parameter changes;
-   command execution;
-   automated remediation;
-   Agent → ENM;
-   MCP → ENM;
-   Phase 4 Action → ENM;
-   scheduled ENM polling;
-   automatic import scheduling;
-   streaming telemetry;
-   connector microservice extraction;
-   raw vendor payload archival;
-   vendor write credentials;
-   production ENM credentials;
-   broad internet egress;
-   Phase 12 functionality.

------------------------------------------------------------------------

## 6. Required Logical Architecture

The implementation SHALL preserve this flow:

``` text
Authorized operator
       │
       ▼
Manual Import API
       │
       ▼
Authorization
       │
       ▼
Phase 8 Lease/Fencing Runtime
       │
       ▼
ImportExecutionContext
(deadline + fencing + cancellation)
       │
       ▼
Phase 10 Credential/Security Runtime
       │
       ▼
EricssonEnmConnector
       │
       ▼
EnmTransport
       │
       ├── SimulatorEnmTransport
       └── ProductionEnmTransport [NOT IMPLEMENTED until interface selected]
       │
       ▼
Vendor transport/source objects
       │
       ▼
Ericsson mapper
       │
       ▼
Vendor-neutral NetworkSnapshot
       │
       ▼
Phase 7/8 validation + reconciliation
       │
       ▼
Canonical Network Model
```

The following paths SHALL remain impossible:

``` text
Agent ───────────────X──> EnmTransport
MCP ─────────────────X──> EnmTransport
Phase 4 Action ──────X──> EnmTransport
Scheduler ───────────X──> EnmTransport
```

------------------------------------------------------------------------

## 7. Package and Type Strategy

Cursor SHALL first inspect the current package structure and place new
code alongside the existing integration/connector runtime rather than
inventing a parallel subsystem.

The following names are conceptual requirements. Cursor MAY adapt exact
names to repository conventions, but the responsibilities SHALL remain
distinct:

``` text
ConnectorDescriptor
ConnectorAccessMode
ConnectorCapability
ConnectorCapabilities

EricssonEnmConnector
EnmTransport
SimulatorEnmTransport
EnmApiProfile

VendorSnapshot
SnapshotCompleteness
VendorSnapshotPage
VendorSourceObject / Ericsson source DTOs
VendorSnapshotMapper

ImportExecutionContext
ConnectorCancellationToken

VendorFailureCode
VendorConnectorException
VendorFailureMapper
VendorRetryPolicy

SourceProvenance
```

Cursor SHALL document any materially different naming in the completion
report.

------------------------------------------------------------------------

## 8. Connector Classification

Introduce or extend the connector model so that classification is
explicit rather than encoded in names.

Required semantics:

``` text
vendor             = ERICSSON
platform           = ENM
implementationType = SIMULATOR or REAL
accessMode         = READ_ONLY
environment        = configured environment
```

Required Phase 11 capabilities SHALL include only applicable read
capabilities, such as:

``` text
INVENTORY_READ
CONFIGURATION_READ
PAGINATION
SOURCE_VERSION
```

The following SHALL NOT exist in the Phase 11 real/simulator ENM
capability set:

``` text
CONFIGURATION_WRITE
NETWORK_MUTATION
COMMAND_EXECUTION
PARAMETER_CHANGE
```

If equivalent enums/types already exist, extend them minimally rather
than duplicate them.

------------------------------------------------------------------------

## 9. Ericsson Connector

Implement an `EricssonEnmConnector` (or repository-conventional
equivalent) that:

-   participates in the existing Phase 7--10 connector runtime;
-   advertises `ERICSSON`, `ENM`, `READ_ONLY`;
-   receives credentials through the existing Phase 10 abstraction;
-   receives trust/security configuration through existing Phase 9/10
    abstractions;
-   receives an `EnmTransport`;
-   does not instantiate Azure SDK clients;
-   does not read Key Vault directly;
-   does not know how Workload Identity is implemented;
-   does not expose generic arbitrary HTTP methods;
-   produces a vendor-neutral snapshot;
-   never mutates canonical entities while pages are still being
    acquired.

The connector SHALL be orchestration/translation code, not a second
security or lease subsystem.

------------------------------------------------------------------------

## 10. `EnmTransport` Contract

Create a narrow transport interface that represents the minimum
operations required to acquire the selected Phase 11 inventory slice.

It SHALL be transport-neutral enough that a future approved production
transport can implement it without changing reconciliation/domain code.

It SHALL NOT encode guessed Ericsson endpoint paths.

Conceptually it needs operations equivalent to:

``` java
session/open/authenticate if required by the selected profile
fetch first inventory page
fetch continuation page
close session
```

Exact method signatures SHALL follow repository style.

The transport contract SHALL carry:

-   execution context;
-   bounded page request;
-   continuation information;
-   safe transport result/error;
-   source/profile metadata where available.

It SHALL NOT expose:

``` java
executeCommand(...)
writeConfiguration(...)
setParameter(...)
delete(...)
postArbitrary(...)
exchange(HttpMethod method, ...)
```

A production implementation SHALL NOT be created until the production
ENM interface is explicitly selected.

If a placeholder class is necessary for wiring, it MUST fail closed with
a clear `PRODUCTION_TRANSPORT_NOT_CONFIGURED`-style failure and MUST NOT
contain guessed endpoint logic.

------------------------------------------------------------------------

## 11. ENM API Profile

Introduce an `EnmApiProfile` abstraction.

For Phase 11 it SHALL describe only simulator-proven behavior, including
as applicable:

-   profile identifier;
-   supported inventory object categories;
-   pagination mode;
-   supported capabilities;
-   source-version metadata;
-   mapping strategy.

Do not encode speculative support for multiple ENM releases.

A future real profile may be added only when the actual ENM
interface/version is known.

------------------------------------------------------------------------

## 12. Simulator Transport

Implement `SimulatorEnmTransport` as the deterministic proof transport.

It SHALL NOT masquerade as a real connector.

It SHALL support deterministic scenarios for at least:

-   successful single-page inventory;
-   successful multi-page inventory;
-   401/authentication failure;
-   403/authorization failure;
-   429/rate limiting;
-   503/unavailable;
-   timeout;
-   malformed response;
-   repeated continuation token;
-   continuation cycle;
-   empty page with invalid continuation;
-   entity-limit overflow;
-   page-limit overflow;
-   partial acquisition;
-   cancellation;
-   lease loss during acquisition.

The simulator SHALL use synthetic data only.

No real ENM endpoint, hostname, credential, topology, customer
identifier, or proprietary production payload SHALL be committed.

------------------------------------------------------------------------

## 13. Initial Inventory Slice

The simulator and mapping layer SHALL prove the minimal vertical slice:

``` text
Network Node / Managed Element
        ↓
Radio Function
        ↓
Cell
```

Use existing SNIP canonical entity types wherever possible.

Ericsson-specific source DTOs MAY represent equivalent vendor concepts
internally, but they SHALL be isolated within the Ericsson adapter
package.

Do not expand Phase 11 into comprehensive ENM object coverage.

------------------------------------------------------------------------

## 14. Vendor Source Model Isolation

Source/transport DTOs SHALL:

-   live inside the Ericsson connector/adapter boundary;
-   not be JPA entities;
-   not be returned by public SNIP APIs;
-   not be consumed by Agents;
-   not be consumed by Phase 4;
-   not become canonical domain types.

Mapping SHALL follow:

``` text
transport DTO
   → Ericsson source model
   → vendor-neutral snapshot object
   → existing canonical reconciliation
```

Tests SHALL enforce that canonical/domain packages do not depend on
Ericsson source classes.

------------------------------------------------------------------------

## 15. Vendor Snapshot Model

Implement a snapshot model supporting at least:

``` text
snapshotId
executionId
connectorId
sourceVendor
sourceSystem
startedAt
completedAt
completeness
pagesExpected       optional
pagesReceived
entitiesRead
warnings            safe/sanitized
sourceCursor        optional/safe
sourceVersion       optional/safe
```

`sourceCursor` MUST NOT be persisted or exposed if the actual transport
later treats it as sensitive. Simulator values are synthetic.

Required completeness enum:

``` text
COMPLETE
PARTIAL
FAILED
```

------------------------------------------------------------------------

## 16. Snapshot Acquisition Rule

Canonical mutation during acquisition is forbidden.

The required lifecycle is:

``` text
start acquisition
      ↓
fetch bounded pages
      ↓
assemble snapshot
      ↓
validate
      ↓
classify completeness
      ↓
normalize
      ↓
revalidate lease/fencing
      ↓
reconcile
```

A test SHALL prove that failure on a later page does not leave page-1
canonical mutations behind.

------------------------------------------------------------------------

## 17. COMPLETE Snapshot Semantics

For a `COMPLETE` snapshot:

-   create/update reconciliation is allowed;
-   source absence MAY be evaluated under the existing reconciliation
    model;
-   hard deletion remains forbidden in Phase 11;
-   provenance SHALL be updated for observed facts;
-   reconciliation SHALL occur only with valid lease/fencing authority.

------------------------------------------------------------------------

## 18. PARTIAL Snapshot Semantics

For a `PARTIAL` snapshot:

-   absence/deletion inference is forbidden;
-   hard deletion is forbidden;
-   create/update MAY occur only if the implementation can prove it is
    safe and atomic;
-   if existing reconciliation cannot safely support partial
    create/update without ambiguity, Phase 11 SHALL choose the safer
    policy: **zero canonical mutation for PARTIAL**.

Cursor SHALL NOT weaken the architecture merely to preserve partial
data.

The chosen policy SHALL be documented and tested.

------------------------------------------------------------------------

## 19. FAILED Snapshot Semantics

For a `FAILED` snapshot:

``` text
canonical mutation = ZERO
```

This is mandatory.

Failures include, as applicable:

-   authentication failure;
-   authorization failure;
-   invalid response;
-   invalid pagination;
-   page/entity limit exceeded;
-   unrecoverable timeout;
-   retry exhaustion;
-   cancellation;
-   lease loss.

------------------------------------------------------------------------

## 20. Disappearance / Staleness

Phase 11 SHALL NOT hard-delete canonical objects because they disappear
from one complete snapshot.

If the existing model supports source observation status, use or extend
it to represent a safe state equivalent to:

``` text
ACTIVE
NOT_OBSERVED
STALE_CANDIDATE
```

If introducing these states would create disproportionate domain
changes, preserve the existing entity and persist observation/provenance
metadata sufficient to defer deletion decisions.

Hard deletion is out of scope.

------------------------------------------------------------------------

## 21. Pagination Limits

Add configurable bounded limits equivalent to:

``` text
pageSize
maxPages
maxEntities
```

Use conservative test/default values consistent with existing
configuration conventions.

The acquisition loop SHALL fail closed on:

-   repeated continuation token;
-   cursor cycle;
-   repeated page identity if detectable;
-   malformed continuation;
-   page overflow;
-   entity overflow;
-   impossible pagination metadata.

No unbounded loop is permitted.

------------------------------------------------------------------------

## 22. Timeout Model

Support both:

``` text
requestTimeout
overallExecutionTimeout
```

The overall execution deadline is authoritative.

A retry SHALL NOT start if it cannot legally fit within the remaining
execution budget.

Timeout failures SHALL map to normalized safe failure codes.

------------------------------------------------------------------------

## 23. Cooperative Cancellation

Implement cooperative cancellation specifically for the vendor-import
path.

Introduce or extend an execution context equivalent to:

``` text
ImportExecutionContext
 ├── executionId
 ├── deadline
 ├── fencingToken
 └── cancellationToken
```

Cancellation SHALL be checked:

-   before credential resolution;
-   before session establishment;
-   before each page;
-   before each retry;
-   during backoff where practical;
-   after page acquisition;
-   before snapshot finalization;
-   immediately before reconciliation.

Cancellation SHALL produce a normalized cancelled outcome and SHALL NOT
produce unsafe canonical mutation.

Do not claim this resolves the separate Phase 5 non-interruptible Agent
timeout debt.

------------------------------------------------------------------------

## 24. Lease/Fencing Revalidation

Reuse Phase 8 lease/fencing infrastructure.

Do NOT add another distributed-lock implementation.

The import path SHALL verify authority:

``` text
after lease acquisition
before credential resolution
before/after long vendor operations
between pages
before reconciliation
```

At minimum, every page boundary and the pre-reconciliation boundary
SHALL validate lease/fencing authority.

If authority is lost:

``` text
VendorFailureCode = LEASE_LOST
cancel execution
discard unsafe/uncommitted snapshot
do not reconcile
```

Tests SHALL simulate a losing replica during a multi-page acquisition.

------------------------------------------------------------------------

## 25. Retry Policy

Implement or extend a bounded vendor retry policy.

The policy SHALL support:

``` text
maxAttempts
initialBackoff
maxBackoff
jitter
Retry-After where transport semantics provide it
```

Recommended initial classification:

``` text
VENDOR_RATE_LIMITED        retryable within budget
VENDOR_TIMEOUT             retryable within budget
VENDOR_UNAVAILABLE         retryable within budget

VENDOR_AUTHENTICATION_FAILED   non-retryable
VENDOR_AUTHORIZATION_DENIED    non-retryable
VENDOR_RESPONSE_INVALID        non-retryable
VENDOR_PAGINATION_INVALID      non-retryable
SNAPSHOT_LIMIT_EXCEEDED        non-retryable
CONNECTOR_CANCELLED            non-retryable
LEASE_LOST                    non-retryable
```

Retry SHALL remain subordinate to cancellation, lease ownership, and the
overall deadline.

------------------------------------------------------------------------

## 26. Failure Taxonomy

Implement normalized failures equivalent to:

``` text
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
PRODUCTION_TRANSPORT_NOT_CONFIGURED
```

Do not leak raw vendor errors through public APIs.

Internal exceptions MAY retain sanitized diagnostic cause information.

------------------------------------------------------------------------

## 27. Phase 10 Credential Integration

The Phase 10 credential architecture SHALL remain unchanged.

The connector receives the existing credential abstraction.

Forbidden:

-   Azure SDK calls from Ericsson connector code;
-   `DefaultAzureCredential` in production connector code;
-   long-lived vendor-secret cache;
-   fallback to older Key Vault secret versions;
-   secret values in connector configuration;
-   secret values in database rows;
-   secret values in metrics/audit/API responses.

Credential resolution remains per session/import according to the Phase
10 contract.

------------------------------------------------------------------------

## 28. TLS / Trust Integration

Reuse Phase 9/10 TLS/trust abstractions.

Mandatory:

-   TLS server verification;
-   hostname verification;
-   configured trust material;
-   fail closed on trust failure.

Forbidden:

``` text
trustAll=true
hostnameVerification=false
insecure=true
```

mTLS SHALL be supported only through existing abstractions/profile
capability where required. Do not invent an mTLS requirement for the
unresolved production interface.

------------------------------------------------------------------------

## 29. Read-Only Protocol Enforcement

The simulator/transport contract SHALL contain no network mutation
operations.

If HTTP semantics are modeled, safe data retrieval operations may be
represented.

Authentication operations may be modeled separately from network-state
operations.

A future authentication POST SHALL NOT be interpreted as authorization
for a network-mutating POST.

No generic arbitrary-method client shall be exposed to higher connector
layers.

------------------------------------------------------------------------

## 30. Network Egress

Preserve the Phase 9/10 application-level egress policy.

Add the ability to describe the real connector destination safely by
configuration, without committing a real endpoint.

Do not introduce:

``` text
0.0.0.0/0
```

as an ENM egress requirement.

The simulator may run locally/in-test.

Real vendor deployment remains blocked until a controlled egress
mechanism is proven for the actual ENM destination.

------------------------------------------------------------------------

## 31. Manual Import API

Reuse or extend the existing integration/import API rather than creating
an unrelated API family.

The Phase 11 real/simulator import SHALL be explicitly triggered by an
authorized request.

The endpoint SHALL:

1.  authorize the caller;
2.  identify the configured connector;
3.  reject connectors without read/import capability;
4.  enter the Phase 8 lease/fencing runtime;
5.  execute the secure connector path;
6.  return safe execution metadata.

Do not add cron/scheduled invocation.

Do not allow Agents or MCP to invoke the connector directly.

------------------------------------------------------------------------

## 32. Import Authorization

Introduce or reuse an explicit application permission/role for
triggering external vendor reads.

It SHALL mean only:

``` text
TRIGGER_VENDOR_IMPORT
```

or the repository-equivalent concept.

It SHALL NOT grant:

``` text
WRITE_VENDOR_CONFIGURATION
EXECUTE_VENDOR_COMMAND
NETWORK_MUTATION
```

Tests SHALL prove unauthorized import requests are rejected before
vendor acquisition.

------------------------------------------------------------------------

## 33. Provenance Persistence

Add provenance storage sufficient to trace canonical observations back
to:

``` text
sourceVendor
sourceSystem
sourceObjectType
sourceObjectId
sourceSnapshotId
observedAt
importExecutionId
```

Prefer normalized relational storage or existing source-reference
mechanisms over duplicating all fields across every canonical entity.

The exact schema SHALL fit the existing JPA/database model.

Requirements:

-   source object identity must be stable within the source system;
-   provenance must link to the import/snapshot;
-   no secrets may be stored;
-   raw vendor payload is not required;
-   downstream diagnostics can determine the source of an imported
    fact/entity.

------------------------------------------------------------------------

## 34. Database Migration

Use the repository's existing migration mechanism.

Create the minimum migration(s) needed for:

-   vendor snapshot metadata;
-   completeness;
-   provenance;
-   any connector execution metadata not already represented.

Migration naming/versioning SHALL follow current repository conventions.

Do not alter historical migration files.

Do not store credentials, tokens, raw headers, or complete raw ENM
payloads.

------------------------------------------------------------------------

## 35. Raw Payload Policy

Do not persist complete raw vendor responses.

Do not log them by default.

Persist only:

-   normalized values;
-   safe source identifiers;
-   snapshot metadata;
-   provenance;
-   counts;
-   safe warnings;
-   optional checksum/hash where justified.

Tests SHALL verify representative secret/header fields are not present
in persisted or returned structures.

------------------------------------------------------------------------

## 36. Audit Events

Extend existing audit mechanisms rather than creating an independent
audit system.

Support safe events equivalent to:

``` text
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
SNAPSHOT_PARTIAL
RECONCILIATION_COMPLETED
VENDOR_RATE_LIMITED
VENDOR_TIMEOUT
VENDOR_AUTHENTICATION_FAILED
VENDOR_AUTHORIZATION_DENIED
VENDOR_PROTOCOL_ERROR
LEASE_LOST
CONNECTOR_CANCELLED
SESSION_COMPLETED
LEASE_RELEASED
```

Do not force every page to generate a high-volume persistent audit row
if the current audit architecture is not intended for that volume; a
safe count/metric may be used instead. Preserve the semantic event
story.

------------------------------------------------------------------------

## 37. Metrics

Extend existing metrics with safe, low-cardinality measurements
equivalent to:

``` text
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

Labels SHALL NOT contain:

-   username;
-   password;
-   token;
-   cookie;
-   authorization header;
-   private key;
-   raw vendor payload;
-   sensitive/high-cardinality DN or object identifier.

------------------------------------------------------------------------

## 38. Public API Safety

Public import responses MAY expose:

``` text
connectorId
vendor
platform
accessMode
executionId
snapshotId
startedAt
completedAt
completeness
pagesReceived
entitiesRead
normalized status/failure code
```

They SHALL NOT expose:

-   credentials;
-   credential references if operationally sensitive;
-   secret versions unless the existing Phase 10 safe metadata contract
    explicitly allows them;
-   tokens;
-   cookies;
-   raw vendor errors;
-   raw vendor payloads;
-   Azure identity tokens;
-   private keys.

------------------------------------------------------------------------

## 39. Health and Readiness

Normal application health/readiness SHALL NOT perform:

-   live ENM inventory calls;
-   unbounded network operations;
-   unnecessary Key Vault secret resolution.

Connector readiness MAY report safe configuration state such as:

``` text
configured
transportConfigured
credentialProviderConfigured
trustConfigured
networkPolicyConfigured
```

A live reachability diagnostic, if implemented, SHALL be explicit and
separately authorized.

------------------------------------------------------------------------

## 40. Agent Boundary Tests

Add tests proving that Phase 5 Agents:

-   do not receive `EnmTransport`;
-   do not receive vendor credentials;
-   do not invoke `EricssonEnmConnector`;
-   cannot trigger real/simulator imports through internal direct calls.

Agents may continue consuming canonical data produced by completed
reconciliation.

------------------------------------------------------------------------

## 41. MCP Boundary Tests

Add tests/static architecture checks proving no Phase 11 ENM tool is
registered in MCP and no MCP component depends on the ENM
transport/connector.

Phase 4 MCP behavior otherwise remains unchanged.

------------------------------------------------------------------------

## 42. Phase 4 Boundary Tests

Add tests/static architecture checks proving Phase 4 governed action
execution has no dependency path to:

``` text
EricssonEnmConnector
EnmTransport
vendor credential handles
```

Do not modify Phase 4 action semantics except where required for
compile-safe architectural boundary tests.

------------------------------------------------------------------------

## 43. Scheduler Boundary

No `@Scheduled`, cron configuration, scheduler registration, or
recurring import trigger SHALL be added for Phase 11.

A repository search at completion SHALL demonstrate that the new ENM
import path is not scheduler-triggered.

------------------------------------------------------------------------

## 44. Required Test Matrix

At minimum, implement tests for:

1.  connector classification is Ericsson/ENM/read-only;
2.  no write capabilities advertised;
3.  single-page successful snapshot;
4.  multi-page successful snapshot;
5.  correct vendor → neutral → canonical mapping;
6.  provenance persisted/recoverable;
7.  failed later page produces zero unsafe canonical mutation;
8.  partial snapshot never infers deletion;
9.  401 maps to authentication failure without retry;
10. 403 maps to authorization failure without retry;
11. 429 retries within budget;
12. 503 retries within budget;
13. retry exhaustion fails safely;
14. `Retry-After` honored where modeled;
15. request timeout;
16. overall deadline;
17. cancellation before first page;
18. cancellation between pages;
19. cancellation during retry/backoff;
20. lease loss between pages;
21. lease loss immediately before reconciliation;
22. stale fencing token cannot reconcile;
23. repeated continuation token rejected;
24. cursor cycle rejected;
25. page limit enforced;
26. entity limit enforced;
27. malformed response rejected;
28. raw payload not persisted;
29. secrets/tokens not logged/audited/returned;
30. unauthorized import rejected before connector use;
31. Agents have no connector access;
32. MCP has no connector access;
33. Phase 4 Actions have no connector access;
34. no scheduler path exists;
35. production transport unresolved/fails closed;
36. existing Phase 7--10 secure mock behavior remains green.

------------------------------------------------------------------------

## 45. Architecture/Dependency Tests

If the repository already uses architecture tests, extend them.

Otherwise add lightweight tests where practical to enforce:

``` text
domain/canonical packages
    MUST NOT depend on Ericsson transport/source packages

agent packages
    MUST NOT depend on EnmTransport/EricssonEnmConnector

MCP packages
    MUST NOT depend on EnmTransport/EricssonEnmConnector

Phase 4 action packages
    MUST NOT depend on EnmTransport/EricssonEnmConnector
```

Do not introduce a heavyweight framework solely for this purpose if
ordinary dependency/reflection/package tests are sufficient.

------------------------------------------------------------------------

## 46. Configuration

Add only non-secret configuration.

Configuration SHOULD support repository-equivalent properties for:

``` text
enabled
connectorId
vendor
platform
implementationType
accessMode
environment
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

Simulator defaults SHALL be synthetic.

A production endpoint SHALL NOT be committed.

Production configuration SHALL fail closed when required fields are
absent.

------------------------------------------------------------------------

## 47. Production Transport Selection

Until the real ENM interface is known:

-   do not implement REST endpoint guesses;
-   do not implement CLI invocation guesses;
-   do not implement Bulk CM guesses;
-   do not add Ericsson SDK dependencies speculatively;
-   do not create fake production credentials;
-   do not claim real ENM compatibility.

The production transport selection remains an explicit architecture
follow-up.

When selected later, it must fit behind `EnmTransport` without changing
canonical/domain consumers.

------------------------------------------------------------------------

## 48. Dependency Policy

Prefer existing Java/Spring libraries already in the repository.

Do not add an Ericsson-specific or new HTTP dependency unless required
by an explicitly selected transport.

Simulator behavior should use existing test/runtime facilities.

Any new dependency SHALL be justified in the completion report.

------------------------------------------------------------------------

## 49. Security Hygiene

Before completion, scan tracked/staged Phase 11 content for:

``` text
password
passwd
secret
token
Authorization
Bearer
client_secret
private_key
BEGIN PRIVATE KEY
real hostnames/IPs
```

Review matches contextually; do not blindly delete legitimate
identifiers such as `credentialReference`.

No real credentials or production endpoints may enter Git.

------------------------------------------------------------------------

## 50. Documentation Updates

Update as appropriate:

-   `README.md`;
-   `docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md`;
-   `docs/implementation/SNIP-IMPLEMENTATION-STATUS.md`;
-   `.cursor/rules/snip-architecture.mdc`;
-   accepted Phase 11 architecture status/bindings;
-   any connector/runtime documentation affected by the implementation.

Documentation SHALL clearly distinguish:

``` text
SIMULATOR/CONTRACT VERIFIED
```

from:

``` text
REAL VENDOR E2E VERIFIED
```

If no real ENM was used, the latter remains `NOT YET VERIFIED`.

------------------------------------------------------------------------

## 51. Completion Report

Create:

`docs/implementation/SNIP-PHASE-11-FIRST-REAL-VENDOR-CONNECTOR-PRODUCTION-READ-ONLY-INTEGRATION-COMPLETION-REPORT.md`

The report SHALL include:

-   Phase 10 parent baseline SHA;
-   implementation scope;
-   files/classes added/modified;
-   database migrations;
-   connector classification;
-   simulator scenarios;
-   snapshot semantics;
-   pagination/retry/timeout/cancellation behavior;
-   lease/fencing behavior;
-   provenance implementation;
-   security boundary;
-   Agent/MCP/Phase 4/scheduler isolation;
-   test commands and exact counts;
-   Go regression results;
-   known limitations;
-   carried technical debt;
-   new technical debt;
-   real vendor E2E status;
-   production ENM transport status;
-   statement that no real vendor credentials/endpoints were committed;
-   statement that no network mutation capability was introduced;
-   statement that Phase 12 was not started.

Before architectural acceptance, its final status SHALL be:

``` text
PHASE 11 STATUS: IMPLEMENTED — PENDING ARCHITECTURAL ACCEPTANCE
```

Do not mark Phase 11 architecturally accepted yourself.

------------------------------------------------------------------------

## 52. Required Verification Commands

At minimum run:

``` bash
mvn -B test
go test ./...
go build ./cmd/simulator
git status --short
```

Also run repository-appropriate searches/tests proving:

-   no scheduler invokes the new connector;
-   no Agent depends on the connector;
-   no MCP component depends on the connector;
-   no Phase 4 action depends on the connector;
-   no production endpoint/credential was introduced;
-   no network-write method/capability was introduced.

If the Maven test count changes, record the exact final count in the
completion report.

All tests SHALL pass before requesting architectural review.

------------------------------------------------------------------------

## 53. Default CI

Default GitHub Actions CI SHALL remain:

-   Azure-independent;
-   vendor-independent;
-   deterministic;
-   synthetic/simulator-backed.

Do not require:

-   `az login`;
-   Azure credentials;
-   Key Vault access;
-   real ENM connectivity;
-   real ENM credentials.

A future real-vendor E2E workflow may be environment-gated, but it is
not required to implement Phase 11 simulator/contract acceptance.

------------------------------------------------------------------------

## 54. Real Vendor E2E Gate

Do not execute a real ENM E2E unless an authorized environment and the
actual supported ENM interface are explicitly provided later.

When unavailable, record:

``` text
REAL VENDOR E2E STATUS: NOT YET VERIFIED
```

This is not an implementation failure.

Do not substitute the simulator for real-vendor verification.

------------------------------------------------------------------------

## 55. Acceptance Criteria

Phase 11 implementation is ready for architectural review only when all
of the following are true:

-   Phase 10 behavior remains green;
-   Ericsson ENM is represented as the first real-vendor target;
-   connector is strictly read-only;
-   `EnmTransport` exists;
-   simulator transport proves the contract;
-   no guessed production transport exists;
-   vendor source models are isolated;
-   vendor-neutral snapshots exist;
-   COMPLETE/PARTIAL/FAILED semantics are enforced;
-   failed snapshots cause zero canonical mutation;
-   partial snapshots cannot infer deletion;
-   pagination is bounded;
-   retries are bounded;
-   timeouts are bounded;
-   cooperative cancellation is implemented;
-   lease/fencing is revalidated during acquisition;
-   stale owners cannot reconcile;
-   provenance is recoverable;
-   raw vendor payload is not persisted by default;
-   credentials remain under Phase 10 architecture;
-   TLS/read-only security remains under Phase 9/10;
-   import is manual and authorized;
-   Agents have no direct connector access;
-   MCP has no direct connector access;
-   Phase 4 Actions have no direct connector access;
-   no scheduler path exists;
-   default CI remains vendor/Azure independent;
-   all Java tests pass;
-   all Go tests/build pass;
-   completion report is complete;
-   real vendor E2E status is stated accurately;
-   Phase 12 has not started.

------------------------------------------------------------------------

## 56. Explicit Forbidden Changes

Cursor SHALL NOT:

-   use `git add .`, commit, push, or establish a Phase 11 baseline
    during implementation;
-   amend the Phase 10 baseline;
-   modify Phase 10 frozen semantics without explicit architectural
    need;
-   implement vendor writes;
-   expose arbitrary HTTP methods;
-   guess Ericsson APIs;
-   add real credentials;
-   add real endpoints;
-   add Azure developer authentication requirements to default CI;
-   make Agents vendor clients;
-   make MCP a vendor client;
-   connect Phase 4 actions to ENM;
-   add scheduled vendor imports;
-   claim real-vendor E2E from simulator evidence;
-   start Phase 12.

------------------------------------------------------------------------

## 57. Implementation Order

Cursor SHALL implement in controlled increments:

``` text
1. Inspect Phase 7–10 connector/runtime/security code
2. Canonicalize Phase 11 documents/status
3. Add connector classification/capabilities
4. Add execution context + cancellation integration
5. Add snapshot/completeness/provenance model
6. Add EnmTransport contract
7. Add simulator transport + ENM profile
8. Add Ericsson connector + mapper
9. Integrate bounded pagination/retry/timeout
10. Integrate lease/fencing revalidation
11. Integrate safe reconciliation semantics
12. Add persistence migration/repositories as required
13. Integrate manual authorized import
14. Add audit/metrics/redaction
15. Add architecture-boundary tests
16. Add complete failure/scenario test matrix
17. Run full Java/Go regression
18. Update docs/context/status/rules
19. Produce completion report
20. STOP for architectural review
```

Do not skip directly to API wiring before the safety semantics exist.

------------------------------------------------------------------------

## 58. Expected End State

At the end of Cursor implementation, SNIP should be able to demonstrate:

``` text
manual authorized import
        ↓
Phase 8 lease/fencing
        ↓
Phase 10 credential boundary
        ↓
Ericsson ENM read-only connector
        ↓
simulated ENM transport
        ↓
bounded multi-page acquisition
        ↓
complete/partial/failed snapshot classification
        ↓
vendor-neutral mapping
        ↓
safe canonical reconciliation
        ↓
provenance
```

while simultaneously proving:

``` text
NO real vendor write
NO guessed production transport
NO Agent → ENM
NO MCP → ENM
NO Phase 4 → ENM
NO scheduler → ENM
NO secret leakage
NO failed-snapshot canonical corruption
NO stale-fencing reconciliation
```

------------------------------------------------------------------------

## 59. Stop Condition

After implementation and verification, Cursor SHALL STOP.

It SHALL NOT commit or push.

It SHALL return a Phase 11 completion report for architectural review.

The expected status at that point is:

``` text
PHASE 11 ARCHITECTURE STATUS: ACCEPTED
PHASE 11 IMPLEMENTATION STATUS: COMPLETE — PENDING ARCHITECTURAL ACCEPTANCE
REAL VENDOR E2E STATUS: NOT YET VERIFIED
PHASE 11 GIT BASELINE: NOT ESTABLISHED
PHASE 12 STATUS: NOT STARTED
```

No Git baseline is authorized until the implementation has been reviewed
and Phase 11 is explicitly accepted.

------------------------------------------------------------------------

## 60. Cursor Execution Instruction

Implement **only Phase 11** according to this specification and the
accepted architecture.

Preserve all frozen Phase 1--10 behavior.

Do not guess the Ericsson production interface.

Use the simulator to prove the connector contract.

Do not introduce any network-write capability.

Do not commit.

Do not push.

Do not start Phase 12.

When implementation and all verification are complete, provide the
completion report and STOP for architectural review.
