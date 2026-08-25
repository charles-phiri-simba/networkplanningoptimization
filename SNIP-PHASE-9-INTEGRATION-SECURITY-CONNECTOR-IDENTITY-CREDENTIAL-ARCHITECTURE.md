# SNIP Phase 9 — Integration Security, Connector Identity & Credential Architecture

## 1. Purpose

Phase 9 establishes the security architecture required before SNIP may connect to real Ericsson or Nokia network-management systems.

Starting baseline:

```text
Branch: main
Commit: 7028bf39f90c26bdddb23000c0c5803c4f8c7686
Message: feat: establish SNIP Phase 8 reliable integration runtime foundation
Phase 8: ARCHITECTURALLY ACCEPTED / FROZEN
CI: PASS
Maven: 125 tests, 0 failures
Go: tests/build PASS
Working tree: clean
Phase 9: not started
```

The architectural question is:

> **How can SNIP authenticate to future Ericsson/Nokia systems using strongly isolated connector identities, externally managed secrets, verifiable TLS trust, explicit read-only authorization, and tightly controlled network egress—without exposing credentials to Agents, MCP, business logic, source data, or logs?**

The governing principle is:

> **Vendor credentials are infrastructure security material. They are never domain data.**

---

## 2. Architectural Position

Future secure integration must follow:

```text
Import Request
      ↓
Phase 8 Reliable Import Runtime
      ↓
ConnectorDefinition
      ↓
Connector Security Context
      ├─ Connector Identity
      ├─ Credential Provider
      ├─ Trust Profile
      ├─ Authorization Profile
      └─ Network Policy
      ↓
Secure Read-Only Connector Session
      ↓
Vendor Adapter
      ↓
SourceSnapshot
      ↓
Phase 8 Runtime
      ↓
Phase 7 Canonical Reconciliation
```

Phase 9 does not replace Phase 8 or Phase 7.

---

## 3. Non-Negotiable Security Rules

- Credentials never enter canonical network state.
- Credentials never enter Agent memory, prompts, LLM calls, MCP capabilities, or RAG.
- Credentials never appear in logs, traces, metrics, audit payloads, completion reports, API responses, or exception messages.
- API callers cannot choose arbitrary credential references.
- API callers cannot choose arbitrary endpoints.
- API callers cannot supply TLS trust material.
- API callers cannot provide connector lease/fencing ownership.
- Real connector sessions are read-only.
- TLS is mandatory.
- Hostname verification is strict.
- Trust-all TLS is prohibited.
- Redirects are disabled initially.
- Network destinations are allow-listed.
- Unknown authorization capability is denied.
- Security failures fail closed.
- No real ENM/NetAct connectivity is introduced in Phase 9.

---

## 4. Credential Store Strategy

Introduce an abstraction equivalent to:

```text
ConnectorCredentialProvider
  resolve(connectorIdentity)
  metadata(connectorIdentity)
```

Initial provider strategy:

```text
LOCAL_DEVELOPMENT / TEST
AZURE_KEY_VAULT contract/configuration
```

Phase 9 implements:

```text
CredentialProvider abstraction
+
local/test provider
+
Azure Key Vault provider contract/config model
```

Phase 9 does **not** require live Azure Key Vault connectivity.

The production target is Azure Key Vault or an enterprise secret manager.

---

## 5. Production Credential Architecture

Preferred production flow:

```text
SNIP Workload Identity / Managed Identity
      ↓
Azure Key Vault
      ↓
Specific connector credential
```

Avoid bootstrap client secrets for Key Vault access.

The SNIP platform identity should have only the minimum secret-read permissions required for configured connector credentials.

---

## 6. Credential Storage Prohibitions

Production connector credentials must never be stored in:

```text
Git
application.yml
application.properties
database tables
fixture JSON
Agent memory
MCP metadata
RAG/vector store
completion reports
```

The database/configuration may store safe references and metadata only.

---

## 7. Credential Reference

Model safe metadata equivalent to:

```text
CredentialReference
  credentialRef
  provider
  secretIdentifier
  credentialType
```

Never persist secret values.

---

## 8. Credential Types

Architecturally support:

```text
USERNAME_PASSWORD
CLIENT_CERTIFICATE
OAUTH2_CLIENT_CREDENTIALS
API_TOKEN
```

Phase 9 proof scope is:

```text
USERNAME_PASSWORD
CLIENT_CERTIFICATE
```

OAuth2 remains architecturally supported but not implemented.

---

## 9. Credential Resolution and Rotation

Resolve credentials per connector session.

Do not bind credentials permanently at application startup.

Rotation semantics:

```text
Session 1 -> credential version A
provider rotates
Session 2 -> credential version B
```

New sessions must resolve the latest permitted version.

Phase 9 does not require a secret cache or rotation watcher.

---

## 10. Connector Identity

Introduce:

```text
ConnectorIdentity
  connectorId
  sourceSystem
  vendor
  environment
  purpose
  credentialRef
  trustProfileId
  authorizationProfileId
  networkPolicyId
  enabled
```

Connector identity is machine identity, not user or Agent identity.

Maintain:

```text
Human User Identity != Agent Identity != Connector Identity
```

---

## 11. Identity Scope

Use a dedicated identity per:

```text
source system + environment + purpose
```

Examples:

```text
ERICSSON_ENM_INT_INVENTORY_READER
ERICSSON_ENM_PROD_INVENTORY_READER
NOKIA_NETACT_INT_INVENTORY_READER
```

Avoid one universal vendor identity.

---

## 12. ConnectorDefinition

Introduce a static/configured definition equivalent to:

```text
ConnectorDefinition
  connectorId
  vendor
  sourceSystem
  sourceScope
  endpointRef
  credentialRef
  trustProfileId
  authorizationProfileId
  networkPolicyId
  authenticationMethod
  enabled
  mode
```

Phase 9 mode:

```text
MOCK_SECURE
```

No real vendor connector.

---

## 13. Fixed Security Binding

A connector definition has fixed bindings:

```text
connectorId
  ↓
endpointRef
credentialRef
trustProfile
authorizationProfile
networkPolicy
```

API callers cannot substitute any of these.

---

## 14. Endpoint Registry and SSRF Boundary

Introduce a bounded `ConnectorEndpointRegistry`.

A request selects a registered connector/source, never an arbitrary URL.

Adapters may combine:

```text
configured endpoint + fixed/validated API path + validated resource ID
```

They may not accept user-supplied full URLs.

Connector infrastructure must reject arbitrary/unregistered destinations.

---

## 15. TLS Model

TLS is mandatory for every future real connector.

Connector security must verify:

```text
certificate chain
validity period
hostname/SAN
trusted issuer
```

Prohibited:

```text
TrustAllStrategy
allow-all HostnameVerifier
disable hostname verification
plaintext fallback
```

---

## 16. ConnectorTrustProfile

Model:

```text
ConnectorTrustProfile
  trustProfileId
  trustMode
  trustStoreRef
  hostnameVerification
  allowedServerNames
  clientCertificateRef
```

Support:

```text
SYSTEM_CA
CUSTOM_CA
```

Strict hostname verification remains mandatory.

---

## 17. Per-Connector SSL Context

Do not mutate the global JVM trust store for vendor integration.

Required:

```text
ConnectorDefinition
      ↓
Connector-specific SSLContext
      ↓
ConnectorTrustProfile
```

This allows different vendor trust chains without changing global application trust behavior.

---

## 18. mTLS Decision

Locked Phase 9 decision:

```text
TLS: mandatory universally
mTLS: optional per connector profile
mTLS support: implemented and proven in Phase 9 tests
```

mTLS is not mandatory for every future vendor connector.

Client certificates/private keys come from the credential provider and should be assembled in memory where practical.

---

## 19. Read-Only Authorization Model

Introduce:

```text
ConnectorAuthorizationProfile
```

Initial profile:

```text
READ_ONLY_NETWORK_INVENTORY
```

Allowed capabilities:

```text
READ_SITE
READ_GNB
READ_CELL
READ_CONFIGURATION
READ_NEIGHBOURS
```

Explicitly prohibited:

```text
WRITE_CONFIGURATION
ACTIVATE
DEACTIVATE
LOCK
UNLOCK
RESET
EXECUTE_COMMAND
DELETE
```

Authorization is a positive allow-list.

> **Anything not explicitly allowed is denied.**

---

## 20. Adapter Capability Requirements

Future adapters declare required capabilities.

Before network access:

```text
requiredCapabilities subsetOf authorizationProfile.allowedCapabilities
```

If false:

```text
CONNECTOR_AUTHORIZATION_DENIED
```

Fail closed.

---

## 21. Defense in Depth

Read-only enforcement exists at three layers:

```text
Vendor-side RBAC
+
SNIP ConnectorAuthorizationProfile
+
Read-only connector client API
```

Even if a vendor account is accidentally overprivileged, SNIP itself must not expose write operations.

---

## 22. ReadOnlyVendorClient

Introduce a client abstraction that exposes only read/query semantics.

Avoid a generic client allowing arbitrary HTTP method + arbitrary URL.

Read-only is a semantic capability boundary, not merely an HTTP verb restriction.

---

## 23. Network Isolation Model

Every connector has an application-level egress policy.

Model:

```text
ConnectorNetworkPolicy
  networkPolicyId
  allowedHostnames[]
  allowedPorts[]
  httpsOnly
  allowRedirects
```

Locked defaults:

```text
httpsOnly = true
allowRedirects = false
```

Everything not explicitly allowed is denied.

---

## 24. SSRF Protection

Connector infrastructure must account for:

```text
loopback
link-local
cloud metadata endpoints
unexpected private destinations
file://
ftp://
unapproved ports
unapproved hostnames
```

Test-only local endpoints must be explicitly registered.

---

## 25. Future Infrastructure Enforcement

Later production deployments should complement application-level controls with:

```text
Kubernetes NetworkPolicy
Azure NSG/firewall rules
private routing/private endpoints
vendor destination allow-lists
```

These are not implemented in Phase 9.

---

## 26. ConnectorSession

Model safe session metadata:

```text
ConnectorSession
  sessionId
  connectorId
  sourceSystem
  credentialRef
  credentialVersion
  trustProfileId
  serverCertificateFingerprint
  startedAt
  endedAt
  status
```

Never persist secret values or private keys.

---

## 27. ConnectorSecurityContext

Bundle session security decisions:

```text
ConnectorSecurityContext
  connectorIdentity
  credentialHandle
  authorizationProfile
  trustProfile
  networkPolicy
```

Adapters should receive a secure read-only client/session rather than raw credentials.

---

## 28. Security Audit

Persist append-only events equivalent to:

```text
SESSION_REQUESTED
CREDENTIAL_RESOLVED
NETWORK_POLICY_VALIDATED
TLS_VALIDATED
AUTHENTICATION_SUCCEEDED
AUTHENTICATION_FAILED
AUTHORIZATION_DENIED
SESSION_COMPLETED
SESSION_FAILED
```

Audit contains metadata only.

---

## 29. Safe Audit Metadata

Allowed:

```text
connectorId
credentialRef
credentialVersion
endpointRef
trustProfileId
serverCertificateFingerprint
event type
timestamp
safe failure code
```

Prohibited:

```text
password
token
Authorization header
private key
raw secret
```

---

## 30. Secret Redaction

Secrets must never appear in:

```text
application logs
exceptions
metrics
traces
audit
API responses
health responses
test snapshots
completion report
```

Phase 9 must include canary-secret redaction tests.

---

## 31. Safe Failure Types

Use sanitized connector failures equivalent to:

```text
CREDENTIAL_RESOLUTION_FAILED
CONNECTOR_AUTHENTICATION_FAILED
TLS_TRUST_FAILED
CONNECTOR_AUTHORIZATION_DENIED
NETWORK_POLICY_DENIED
CONNECTOR_DISABLED
```

Do not expose raw client/SSL exceptions to API callers.

---

## 32. Fail-Closed Semantics

Credential resolution failure:

```text
no connector session
no vendor call
no canonical mutation
```

TLS failure:

```text
no snapshot
no import mutation
```

Authorization/network-policy failure:

```text
deny before network access where possible
```

No anonymous or trust-all fallback.

---

## 33. Local/Test Credential Provider

Implement `LocalDevelopmentCredentialProvider` only under explicit test/local-security configuration.

It may use:

```text
in-memory/generated test credentials
or local process environment variables
```

No checked-in `.env`.

No default real secret in application configuration.

---

## 34. Azure Key Vault Strategy

Locked decision:

```text
Production target:
Azure Key Vault / enterprise secret manager

Phase 9:
CredentialProvider abstraction
+
local/test provider
+
Azure Key Vault contract/configuration only

Live Azure Key Vault integration:
deferred
```

Preferred production vault authentication:

```text
Managed Identity / Workload Identity
```

not a stored Key Vault client secret.

---

## 35. Cross-Vendor Credential Isolation

A connector identity may resolve only its configured credential reference.

An Ericsson identity cannot resolve a Nokia credential reference.

The import API must not accept arbitrary secret IDs.

---

## 36. Authentication Methods

Architecturally model:

```text
BASIC
MTLS
BASIC_PLUS_MTLS
OAUTH2_CLIENT_CREDENTIALS
```

Phase 9 proof scope:

```text
BASIC
BASIC_PLUS_MTLS
```

OAuth is deferred.

---

## 37. Short-Lived Connector Sessions

Phase 9 session model:

```text
open secure session
read source data
close
```

No permanently authenticated vendor session is required.

---

## 38. Phase 8 Integration

Future secure connector flow:

```text
Import Request
      ↓
Phase 8 classify / lease / fencing
      ↓
Phase 9 ConnectorSecurityService
      ↓
Secure ConnectorSession
      ↓
NetworkSourceAdapter.readSnapshot(...)
      ↓
Phase 8 checkpoints / watchdog / atomic commit
```

Lease acquisition occurs before expensive connector I/O for the same source/scope.

Phase 9 failures terminalize Phase 8 executions safely.

---

## 39. Phase 8 Failure Mapping

Add bounded connector-security failure codes:

```text
CREDENTIAL_RESOLUTION_FAILED
CONNECTOR_AUTHENTICATION_FAILED
TLS_TRUST_FAILED
CONNECTOR_AUTHORIZATION_DENIED
NETWORK_POLICY_DENIED
CONNECTOR_DISABLED
```

Retryability is deterministic.

No automatic retry.

---

## 40. Mock Secure Vendor Server

Phase 9 proves the security model with a local secure test server.

It must support:

```text
real TLS socket
trusted server certificate
untrusted CA scenario
hostname mismatch scenario
credential validation
optional mTLS
read-only endpoint
intentional authentication failures
```

Use generated ephemeral test certificates/keys where practical.

---

## 41. Real TLS Testing

Security acceptance requires real TLS integration tests.

Mocking SSLContext behavior alone is insufficient.

---

## 42. Security Proofs

Required proofs:

### A — Credential isolation

Ericsson connector attempts Nokia credential reference:

```text
DENY
```

### B — Secret redaction

Use a canary secret and prove it never appears in logs/audit/API/errors.

### C — Trusted TLS

Trusted CA + correct hostname succeeds.

### D — Untrusted TLS

Unknown CA fails with `TLS_TRUST_FAILED`.

### E — Hostname mismatch

Valid chain + wrong hostname fails.

### F — mTLS

Approved client cert succeeds; missing/untrusted client cert fails.

### G — Authorization denial

Missing required read capability fails before read.

### H — Network policy denial

Unapproved host/port fails before connection.

### I — Disabled connector

Fails closed.

### J — Credential rotation

Session A uses version A, next session uses rotated version B without restart.

### K — Secure Phase 8 import

Lease/fencing + secure connector + Phase 7 reconciliation succeeds.

### L — Security failure / no mutation

Auth/TLS/security failure produces safe terminal Phase 8 outcome with zero canonical mutation.

---

## 43. Persistence Strategy

Prefer:

```text
ConnectorDefinition / trust / authorization / network policies:
  static typed configuration / in-code registry

ConnectorSecurityAuditEvent:
  PostgreSQL append-only
```

No dynamic connector admin CRUD in Phase 9.

Only safe security metadata may be persisted.

---

## 44. Health / Readiness

Expose only safe connector security readiness:

```text
connectorId
enabled
credentialConfigurationStatus
trustConfigurationStatus
authorizationStatus
networkPolicyStatus
overallSecurityStatus
```

Do not expose secrets.

Do not make live vendor reachability a global application startup dependency.

---

## 45. Failure Isolation

A broken Ericsson connector must not prevent the entire SNIP application from starting.

Connector state may become:

```text
READY
DEGRADED
UNAVAILABLE
```

while other connectors remain usable.

---

## 46. Threat Model

Phase 9 explicitly addresses:

```text
secret leakage
credential substitution
credential replay
cross-vendor credential confusion
endpoint substitution / SSRF
MITM
untrusted certificate
hostname spoofing
overprivileged vendor account
write-capable API exposure
log leakage
Agent/LLM prompt leakage
MCP secret exposure
```

Controls must be documented against each threat.

---

## 47. Agent / LLM / MCP / RAG Boundaries

Agents cannot access credential-provider APIs.

LLMs receive only sanitized security status.

No MCP capability exists for secret access, rotation, vendor login, or direct vendor access.

Credentials/trust/session audit are not vectorized into RAG.

---

## 48. Zero-Write Guarantee

Phase 9 introduces no vendor mutation methods.

A mock server may expose a sentinel write endpoint solely to prove SNIP never invokes it.

---

## 49. Explicitly Out of Scope

Do not implement:

```text
real Ericsson ENM connector
real Nokia NetAct connector
live Azure Key Vault calls
Azure login in CI
production credentials
OAuth token flow
scheduled imports
continuous synchronization
vendor telemetry
vendor writes
dynamic connector admin UI
dynamic connector registry
dynamic security policy editing
external PKI automation
certificate issuance
automatic secret rotation watcher
circuit breaker
connector worker microservice split
Kubernetes NetworkPolicy
Azure NSG/firewall changes
production network routing
Phase 10
```

---

## 50. Locked Phase 9 Decisions

- Phase: **Integration Security, Connector Identity & Credential Architecture**
- Baseline: `7028bf39f90c26bdddb23000c0c5803c4f8c7686`
- Production credential target: Azure Key Vault / enterprise secret manager
- Live Key Vault integration: deferred
- Phase 9 Key Vault scope: abstraction + contract/config only
- Vault authentication target: Managed Identity / Workload Identity
- Local/test credential provider: required
- Secrets in Git/config/database/domain: prohibited
- Credential resolution: per connector session
- Secret cache: none initially
- Rotation: new sessions pick up new version
- Connector identity: per source system + environment + purpose
- Fixed endpoint/credential/trust/auth/network binding: required
- Arbitrary credential ref: prohibited
- Arbitrary URL: prohibited
- TLS: mandatory
- Hostname verification: strict
- Trust-all: prohibited
- Per-connector SSL context: required
- Global JVM trust mutation: prohibited
- Custom CA: supported
- mTLS: optional per connector profile
- mTLS support/test proof: required
- Client private key: credential provider only
- Authorization: positive allow-list
- Initial profile: READ_ONLY_NETWORK_INVENTORY
- Adapter required capabilities: explicit
- Unknown capability: deny
- ReadOnlyVendorClient: required
- Network allow-list: per connector
- HTTPS-only: required
- Redirects: disabled
- SSRF protection: required
- Security audit: append-only
- Canary secret proof: required
- Security failure: fail closed
- Phase 8 runtime: unchanged
- Phase 7 reconciliation: unchanged
- Agents: no credential access
- LLM: no secret access
- MCP: no secret capability
- Real ENM/NetAct: deferred
- Vendor writes: prohibited
- Phase 10: not started

---

## 51. Expected ADR Direction

After Phase 8 ADR 058:

```text
059 Connector Identity and Fixed Security Binding
060 Credential Provider and External Secret Store Strategy
061 Managed Identity / Workload Identity for Production Vault Access
062 Per-Connector TLS Trust and Strict Hostname Verification
063 Optional mTLS and Client Certificate Handling
064 Read-Only Connector Authorization and Capability Allow-List
065 Connector Network Egress Policy and SSRF Protection
066 Secret Redaction and Connector Security Audit
067 Security Failure Mapping into Reliable Import Runtime
068 No Real Vendor Connectivity Until Security Gate Is Accepted
```

---

## 52. Architectural Outcome

At completion:

```text
Phase 8 Import Execution
      ↓
Fixed Connector Identity
      ↓
Credential Reference Resolution
      ↓
Strict TLS / optional mTLS
      ↓
Read-Only Authorization
      ↓
Network Egress Allow-List
      ↓
Secure Mock Vendor Session
      ↓
SourceSnapshot
      ↓
Phase 8 Runtime
      ↓
Phase 7 Canonical State
```

while guaranteeing:

```text
No secret leakage
No arbitrary endpoints
No arbitrary credential substitution
No vendor write authority
No Agent/LLM/MCP secret access
No real ENM/NetAct connectivity yet
```

Only after Phase 9 is accepted should SNIP consider architecting its first real vendor connector.
