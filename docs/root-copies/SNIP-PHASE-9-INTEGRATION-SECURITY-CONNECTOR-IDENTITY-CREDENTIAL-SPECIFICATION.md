# SNIP Phase 9 — Integration Security, Connector Identity & Credential Architecture Implementation Specification

## 1. Authority and Baseline

This is the bounded Cursor implementation contract for Phase 9.

Start exactly from:

```text
Branch: main
Commit: 7028bf39f90c26bdddb23000c0c5803c4f8c7686
Commit message: feat: establish SNIP Phase 8 reliable integration runtime foundation
Phase 8: ARCHITECTURALLY ACCEPTED / FROZEN
CI: success
Maven: 125 tests, 0 failures
Go: tests/build PASS
Working tree: clean
Phase 9: not started
```

Read `SNIP-PHASE-9-INTEGRATION-SECURITY-CONNECTOR-IDENTITY-CREDENTIAL-ARCHITECTURE.md` completely before modifying code.

The architecture document is authoritative.

Implement Phase 9 only.

Do not start Phase 10.

Do not connect to real Ericsson ENM or Nokia NetAct.

Do not commit or push a Phase 9 baseline until architectural review, freeze, and explicit authorization.

---

## 2. Objective

Implement the minimum security proof:

```text
Phase 8 Import Execution
      ↓
ConnectorDefinition
      ↓
Connector Identity
      ↓
Credential Provider
      ↓
Trust Profile
      ↓
Read-Only Authorization
      ↓
Network Policy
      ↓
Real TLS Secure Mock Vendor Session
      ↓
SourceSnapshot
      ↓
Phase 8 Runtime
      ↓
Phase 7 Reconciliation
```

Prove that credentials and external trust remain isolated from Agents, LLMs, MCP, canonical state and callers.

---

## 3. Preserve Phases 1–8

Do not redesign accepted architecture.

Preserve at minimum:

```text
Phase 8 NEW / RETRY / REPLAY
Phase 8 PostgreSQL lease/fencing/watchdog
Phase 8 atomic canonical commit
Phase 7 vendor adapter/canonical/reconciliation semantics
Phase 7 source authority/conflict semantics
Phase 6 Twin freshness/manual sync
Phase 5 five-Agent model
Phase 4 MCP and APPLY HIGH / DENY
Phase 2 telemetry boundary
RAG separation from operational state
```

No live network write path.

---

## 4. Repository Discovery

Before implementation:

1. verify HEAD exactly matches Phase 8 baseline;
2. inspect Phase 8 import runtime entry points;
3. inspect current fixture adapters;
4. identify where `readSnapshot()` is invoked;
5. inspect Phase 8 failure codes and retryability handling;
6. inspect current config conventions;
7. inspect existing audit persistence style;
8. inspect current test server/dependency options;
9. identify next Flyway migration;
10. update `.cursor/rules/snip-architecture.mdc` to authorize Phase 9 only while preserving frozen Phase 1–8 decisions.

---

## 5. Required Security Components

Implement responsibilities equivalent to:

```text
ConnectorDefinition
ConnectorIdentity
ConnectorRegistry
ConnectorEndpointRegistry
ConnectorCredentialProvider
CredentialProviderRegistry
LocalDevelopmentCredentialProvider
CredentialReference
CredentialMetadata
ConnectorTrustProfile
ConnectorAuthorizationProfile
ConnectorNetworkPolicy
ConnectorSecurityContext
SecureConnectorClientFactory
ReadOnlyVendorClient
ConnectorSession
ConnectorSecurityAuditEvent
```

Names/packages may follow repository conventions.

---

## 6. Fixed Connector Security Binding

Each connector definition binds:

```text
endpointRef
credentialRef
trustProfileId
authorizationProfileId
networkPolicyId
```

Public import requests must not override any of these.

Add tests proving arbitrary security-component substitution is impossible.

---

## 7. Credential Provider Contract

Implement:

```text
ConnectorCredentialProvider
```

with behavior equivalent to:

```text
resolve(connectorIdentity)
metadata(connectorIdentity)
providerType()
```

Never return secrets in DTOs or audit.

---

## 8. Credential Provider Registry

Represent:

```text
LOCAL_DEVELOPMENT
AZURE_KEY_VAULT
```

Phase 9 requires:

- functional local/test provider;
- Azure Key Vault provider contract/configuration model;
- no live Key Vault calls.

Do not fake production Key Vault connectivity.

---

## 9. Local/Test Credential Provider

Enable only in test/local-security configuration.

Credentials should be generated/injected in memory or supplied through process environment for local manual testing.

Do not put real/test secret values into default checked-in application configuration.

---

## 10. Credential Types

Phase 9 implements proof for:

```text
USERNAME_PASSWORD
CLIENT_CERTIFICATE
```

Architecture may include future enum values:

```text
OAUTH2_CLIENT_CREDENTIALS
API_TOKEN
```

but no OAuth flow is implemented.

---

## 11. Safe Credential Values

Secret-bearing classes must not reveal values through:

```text
toString()
equals diagnostic output
exception message
JSON serialization
logging
```

Keep values session-scoped and short-lived.

---

## 12. Credential Metadata

Safe metadata:

```text
credentialRef
provider
credentialType
versionIdentifier
resolvedAt
expiresAt
```

No secret value/private key/token.

---

## 13. Credential Isolation

An Ericsson connector may resolve only its fixed Ericsson credential reference.

Cross-vendor substitution must fail.

The API must not accept arbitrary `credentialRef`.

---

## 14. Credential Rotation

Local/test provider must support deterministic version rotation.

Required proof:

```text
Session 1 -> version A
rotate provider
Session 2 -> version B
no app restart
```

Do not use an indefinite secret cache.

---

## 15. Credential Expiry

Expired credential metadata must fail before source read.

Use a safe failure code and no canonical mutation.

Use an injectable clock where practical.

---

## 16. Azure Key Vault Contract

Represent the production target:

```text
AZURE_KEY_VAULT
+
Managed Identity / Workload Identity
```

No `az login`, Azure SDK network call, or Key Vault dependency is required in Phase 9 CI unless needed only for compile-time contract and entirely offline.

Do not add Azure credentials.

---

## 17. Connector Identity / Registry

Create separate secure mock connector identities conceptually equivalent to:

```text
ERICSSON_ENM_INT_INVENTORY_READER
NOKIA_NETACT_INT_INVENTORY_READER
```

These are test/security identities only and do not imply real ENM/NetAct connectivity.

---

## 18. Connector Enabled State

`enabled=false` must fail closed before credential/network access.

Add test.

---

## 19. Trust Profiles

Implement typed/static trust profiles.

Support:

```text
SYSTEM_CA
CUSTOM_CA
```

Strict hostname verification is mandatory.

---

## 20. Per-Connector SSLContext

Build an SSL context per connector/trust profile.

Do not mutate global JVM trust configuration.

Do not use trust-all libraries/settings.

---

## 21. Real TLS Secure Mock Server

Use a real local HTTPS server in tests.

Acceptable tools include WireMock/MockWebServer/embedded HTTPS server.

The proof must use actual TLS handshakes.

---

## 22. Ephemeral Test Certificates

Prefer test-runtime-generated:

```text
CA
server certificate/key
client certificate/key
```

Do not commit production/reusable private key material.

---

## 23. Trusted TLS Proof

Trusted CA + correct hostname must succeed.

---

## 24. Untrusted TLS Proof

Unknown CA must fail:

```text
TLS_TRUST_FAILED
```

No canonical mutation.

---

## 25. Hostname Mismatch Proof

Valid chain with incorrect hostname/SAN must fail.

No hostname-verification bypass.

---

## 26. mTLS

Support connector profiles equivalent to:

```text
BASIC
BASIC_PLUS_MTLS
```

mTLS is optional per profile.

Client key/certificate material comes from the credential provider.

---

## 27. mTLS Proof

Prove:

```text
trusted client cert -> success
missing client cert -> fail
untrusted client cert -> fail
```

Use real TLS.

---

## 28. Authorization Profile

Implement:

```text
READ_ONLY_NETWORK_INVENTORY
```

with:

```text
READ_SITE
READ_GNB
READ_CELL
READ_CONFIGURATION
READ_NEIGHBOURS
```

Positive allow-list only.

Unknown or missing capability is denied.

---

## 29. Adapter Required Capabilities

Adapters/connector definitions declare required read capabilities.

Validate before network read.

Missing capability:

```text
CONNECTOR_AUTHORIZATION_DENIED
```

---

## 30. ReadOnlyVendorClient

Expose only bounded read/query semantics.

Do not expose arbitrary HTTP method + arbitrary URL.

No vendor write method.

---

## 31. Sentinel Write Proof

Secure mock server may expose a sentinel write endpoint.

Normal Phase 9 imports must never call it.

This is required evidence for zero-write behavior.

---

## 32. Network Policy

Implement static/typed:

```text
allowedHostnames
allowedPorts
httpsOnly
allowRedirects
```

Locked:

```text
httpsOnly = true
allowRedirects = false
```

---

## 33. Endpoint Registry

Requests select configured connector/source, not arbitrary URL.

No external endpoint URL field in public request DTOs.

---

## 34. SSRF / Network Policy Proof

Attempt access to unapproved host/port/scheme.

Expected:

```text
NETWORK_POLICY_DENIED
```

No request reaches the unapproved target.

Test explicit local mock endpoints via approved policy entries only.

---

## 35. Redirect Policy

Disable redirects in the connector HTTP client.

Add a test if supported cleanly by chosen client/test server.

---

## 36. SecureConnectorClientFactory

Implement a boundary responsible for:

```text
resolve connector definition
validate enabled
validate authorization
resolve credential
validate credential expiry/binding
validate network policy
build SSL context
open secure read-only session
```

Adapter code receives a secure client/session, not raw credentials.

---

## 37. Connector Security Context

Build a session-level context containing safe objects/handles for:

```text
connectorIdentity
credential handle
authorization profile
trust profile
network policy
```

Controllers/Agents/reconciliation must not see raw secrets.

---

## 38. Connector Session Metadata

Record/model safe session metadata:

```text
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

No secrets.

---

## 39. Security Audit

Persist append-only safe audit events:

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

---

## 40. Canary Secret Test

Use:

```text
PHASE9_CANARY_SECRET_VALUE
```

Exercise successful/failed authentication, audit, API error, and captured logs where practical.

Assert canary is absent everywhere except the in-memory credential source/assertion setup.

This is release-blocking.

---

## 41. Safe Exceptions

Map failures to bounded codes:

```text
CREDENTIAL_RESOLUTION_FAILED
CONNECTOR_AUTHENTICATION_FAILED
TLS_TRUST_FAILED
CONNECTOR_AUTHORIZATION_DENIED
NETWORK_POLICY_DENIED
CONNECTOR_DISABLED
```

Do not expose raw SSL/client exception text in API responses.

---

## 42. Phase 8 Failure Mapping

Extend Phase 8 failure-code/retryability logic minimally.

No automatic retry.

Existing immutable attempt history and lease/fencing behavior must remain intact.

---

## 43. Integration Order

Secure connector-backed import must follow:

```text
classify execution
acquire Phase 8 lease/fencing
create Phase 9 secure connector session
read snapshot
continue Phase 8 checkpoints/reconciliation/commit
```

Do not perform expensive source I/O before same-scope ownership.

---

## 44. Security Failure / No Mutation Test

For credential/auth/TLS/network/authorization failures:

- safe terminal Phase 8 result;
- no accepted snapshot;
- no canonical mutation;
- no SourceReference mutation;
- lease released/expired safely;
- prior state unchanged.

---

## 45. Rotation Integration Test

Session A authenticates using version A.

Rotate local provider to B and mock endpoint expectations.

Session B succeeds using B without restart.

Persist/audit version metadata only.

---

## 46. Cross-Vendor Credential Test

Attempt to resolve Nokia credential under Ericsson identity.

Expected denial.

No secret exposure.

---

## 47. Disabled Connector Test

Disabled connector fails before secret/network access.

---

## 48. Authorization Test

Remove a required read capability such as `READ_CONFIGURATION`.

Expected authorization denial before network read.

---

## 49. Basic Authentication Proof

Trusted TLS + valid local username/password succeeds.

Wrong password:

```text
CONNECTOR_AUTHENTICATION_FAILED
```

No secret leakage.

---

## 50. mTLS + Basic Proof

`BASIC_PLUS_MTLS` requires both valid factors.

---

## 51. Phase 8 Secure Import E2E

Prove:

```text
Phase 8 lease/fencing
      ↓
Phase 9 secure mock connector
      ↓
SourceSnapshot
      ↓
Phase 8 checkpoints
      ↓
Phase 7 reconciliation
      ↓
successful canonical state
```

Existing replay/retry/fencing semantics must remain intact.

---

## 52. Agent Boundary

Exactly five Phase 5 Agents remain.

Agent packages must not depend on credential providers, credential handles/private keys, or secure connector factory.

---

## 53. LLM Boundary

No secret/raw auth error is added to prompt assembly.

Only sanitized status such as `connector authentication failed` may propagate upward.

---

## 54. MCP Boundary

Do not add MCP tools for:

```text
get secret
rotate secret
vendor login
trust modification
direct vendor access
```

---

## 55. RAG Boundary

Do not vectorize credentials, trust material, security audit or connector session data.

---

## 56. Health / Security Readiness

Expose safe status equivalent to:

```text
connectorId
enabled
credentialConfigStatus
trustConfigStatus
authorizationStatus
networkPolicyStatus
overallSecurityStatus
```

No secret values.

Do not make real vendor reachability part of global health.

---

## 57. Failure Isolation

One broken connector must not prevent the whole application from starting unless a global safety invariant is invalid.

Document the exact behavior.

---

## 58. Persistence / Flyway

Inspect actual next migration.

If Phase 8 used V9, expected:

```text
V10__connector_security_foundation.sql
```

Persist only safe security audit/session metadata where needed.

Never persist secret/private-key/token/password values.

Static connector/trust/auth/network profiles may remain typed config/in-code.

---

## 59. Observability

Add safe counters/logs equivalent to:

```text
connectorSessionsStarted
connectorSessionsSucceeded
connectorSessionsFailed
credentialResolutionFailures
connectorAuthenticationFailures
tlsTrustFailures
authorizationDenied
networkPolicyDenied
connectorCredentialRotationsObserved
```

Never use secret value as a metric label/log field.

---

## 60. Threat Model

Document controls for:

```text
secret leakage
credential substitution
credential replay
cross-vendor credential confusion
endpoint substitution
SSRF
MITM
untrusted CA
hostname spoofing
overprivileged vendor account
write-capable API exposure
log leakage
Agent/LLM leakage
MCP secret exposure
```

---

## 61. ADRs

Create sequential ADRs after Phase 8 ADR 058:

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

## 62. Documentation

Create/copy following existing repository conventions:

```text
SNIP-PHASE-9-INTEGRATION-SECURITY-CONNECTOR-IDENTITY-CREDENTIAL-ARCHITECTURE.md
SNIP-PHASE-9-INTEGRATION-SECURITY-CONNECTOR-IDENTITY-CREDENTIAL-SPECIFICATION.md
```

Update:

```text
README.md
docs/implementation/SNIP-IMPLEMENTATION-CONTEXT.md
docs/implementation/SNIP-IMPLEMENTATION-STATUS.md
.cursor/rules/snip-architecture.mdc
```

Phase 8 remains frozen.

---

## 63. Required Verification

Run:

```text
mvn -B test
go test ./...
go build ./cmd/simulator
```

Default CI requires no:

```text
Azure login
Key Vault
ENM
NetAct
production credentials
production certificates
external vendor network
Ollama
```

Real TLS/mTLS tests must run locally/in Maven using ephemeral test material.

---

## 64. Acceptance — Baseline

- [ ] Starts from `7028bf39f90c26bdddb23000c0c5803c4f8c7686`.
- [ ] Phase 1–8 regressions pass.
- [ ] Phase 8 remains frozen.
- [ ] Phase 10 not started.
- [ ] No real ENM/NetAct.

---

## 65. Acceptance — Credential Strategy

- [ ] CredentialProvider abstraction.
- [ ] Functional local/test provider.
- [ ] Azure Key Vault contract/config only.
- [ ] No live Key Vault dependency.
- [ ] No secrets persisted/committed.
- [ ] Per-session resolution.
- [ ] Rotation proof.
- [ ] Expiry fail-closed.
- [ ] Cross-vendor credential isolation.

---

## 66. Acceptance — Connector Identity

- [ ] Per source/environment/purpose identity.
- [ ] Fixed security binding.
- [ ] Arbitrary credentialRef prohibited.
- [ ] Arbitrary endpoint prohibited.
- [ ] Disabled connector fail-closed.

---

## 67. Acceptance — TLS / mTLS

- [ ] TLS mandatory.
- [ ] Strict hostname verification.
- [ ] Trust-all prohibited.
- [ ] Per-connector SSLContext.
- [ ] Global JVM trust not mutated.
- [ ] Trusted TLS proof.
- [ ] Untrusted CA proof.
- [ ] Hostname mismatch proof.
- [ ] mTLS support/proof with real TLS socket.

---

## 68. Acceptance — Authorization

- [ ] Positive allow-list.
- [ ] `READ_ONLY_NETWORK_INVENTORY`.
- [ ] Required adapter capabilities.
- [ ] Missing/unknown capability denied.
- [ ] No vendor write capability.
- [ ] ReadOnlyVendorClient bounded.
- [ ] Sentinel write endpoint never invoked.

---

## 69. Acceptance — Network Isolation

- [ ] Endpoint registry.
- [ ] Hostname allow-list.
- [ ] Port allow-list.
- [ ] HTTPS-only.
- [ ] Redirects disabled.
- [ ] Arbitrary URL prohibited.
- [ ] SSRF-oriented denial proof.
- [ ] Network-policy denial before request.

---

## 70. Acceptance — Secret Safety / Audit

- [ ] Append-only security audit.
- [ ] No secret in audit/API/logs/errors.
- [ ] Safe credential version provenance.
- [ ] Safe server certificate fingerprint.
- [ ] Canary secret proof passes.
- [ ] Safe exception mapping.
- [ ] No Agent credential access.
- [ ] No LLM secret access.
- [ ] No MCP secret capability.
- [ ] No RAG security material.

---

## 71. Acceptance — Phase 8 Integration

- [ ] Phase 8 lease acquired before connector I/O.
- [ ] Secure read continues through Phase 8.
- [ ] Security failure yields safe terminal outcome.
- [ ] No canonical mutation on security failure.
- [ ] Replay/retry/fencing remain intact.
- [ ] No automatic retry.
- [ ] No automatic Twin synchronization.

---

## 72. Acceptance — CI / Hygiene

- [ ] Maven tests pass.
- [ ] Real TLS/mTLS tests included.
- [ ] Go tests pass.
- [ ] Go build passes.
- [ ] CI needs no Azure/vendor systems.
- [ ] No private keys/production certs committed.
- [ ] No secrets/generated binaries/IDE/log/model/DB data committed.
- [ ] ADRs 059–068 created.
- [ ] Docs/status/rule updated.
- [ ] Phase 10 not started.

---

## 73. Required Completion Report

Create:

```text
docs/implementation/SNIP-PHASE-9-COMPLETION-REPORT.md
```

Include:

1. Executive Summary
2. Phase 8 Baseline Verification
3. Scope Delivered
4. Security Architecture
5. ConnectorDefinition
6. Connector Identity
7. Fixed Security Binding
8. Credential Provider
9. Local/Test Credential Provider
10. Azure Key Vault Contract
11. Production Managed/Workload Identity Direction
12. Credential Types
13. Credential Resolution
14. Credential Rotation
15. Credential Expiry
16. Cross-Vendor Credential Isolation
17. Trust Profile
18. Per-Connector SSLContext
19. TLS Policy
20. Trusted CA Proof
21. Untrusted CA Proof
22. Hostname Verification Proof
23. mTLS Model
24. mTLS Proof
25. Authorization Profile
26. Adapter Required Capabilities
27. ReadOnlyVendorClient
28. Sentinel Write Proof
29. Network Policy
30. SSRF Protection
31. Endpoint Registry
32. Redirect Policy
33. Connector Security Context
34. Secure Connector Client Factory
35. Connector Session
36. Security Audit
37. Secret Redaction
38. Canary Secret Proof
39. Safe Exception Mapping
40. Phase 8 Failure Mapping
41. Secure Import E2E
42. Security Failure / No Mutation Proof
43. Credential Rotation Proof
44. Connector Readiness / Failure Isolation
45. Phase 8 Runtime Boundary
46. Phase 7 Reconciliation Boundary
47. Phase 6 Twin Boundary
48. Phase 5 Agent Boundary
49. Phase 4 MCP Boundary
50. Telemetry / RAG Boundary
51. Threat Model
52. Persistence / Flyway
53. Observability
54. Tests
55. Local E2E Evidence
56. ADRs
57. Performance
58. Acceptance PASS/FAIL
59. Known Limitations
60. Technical Debt
61. Lessons Learned
62. Recommended Next Phase
63. Architectural Questions

End with exactly one:

```text
PHASE 9 STATUS: ACCEPTANCE RECOMMENDED
```

or:

```text
PHASE 9 STATUS: ACCEPTANCE NOT RECOMMENDED
```

Do not mark Phase 9 architecturally accepted yourself.

---

## 74. Architectural Questions for Review

Without broadening implementation, recommend:

1. whether Phase 10 should implement the first real connector or first prove Kubernetes/multi-instance connector security;
2. whether live Azure Key Vault integration should occur before real vendor connectivity;
3. whether Basic auth should remain supported once real connector capabilities are known;
4. whether mTLS should become mandatory for any connector class;
5. whether security profiles remain static/configured or become centrally managed later;
6. whether cloud network-policy enforcement should precede the first real connector;
7. whether cooperative cancellation from Phase 8 should be added before long-running real connector I/O.

---

## 75. Git Safety

During Phase 9 implementation:

- do not commit;
- do not push;
- do not amend Phase 8;
- do not create a Phase 10 branch;
- do not self-authorize acceptance.

Leave Phase 9 as uncommitted working-tree changes.

---

## 76. Final Instruction to Cursor

Treat this as authorization for **Phase 9 only**.

The objective is:

> **Prove that SNIP can establish a secure, read-only connector session using a fixed connector identity, externally referenced credentials, strict TLS trust, optional mTLS, explicit read authorization, and allow-listed network egress while preserving Phase 8 reliability and preventing credentials from reaching Agents, LLMs, MCP, canonical state, logs, or API consumers.**

Preserve all Phase 1–8 architecture.

Do not implement real Ericsson ENM.
Do not implement real Nokia NetAct.
Do not perform live Azure Key Vault calls.
Do not add production credentials.
Do not add OAuth.
Do not add vendor writes.
Do not add scheduled synchronization.
Do not add vendor telemetry.
Do not add cloud network infrastructure.
Do not start Phase 10.

When implementation and validation are complete:

1. produce the Phase 9 completion report;
2. leave all Phase 9 work uncommitted;
3. do not push;
4. STOP for architectural review.
