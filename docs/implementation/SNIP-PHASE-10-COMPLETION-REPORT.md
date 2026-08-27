# SNIP Phase 10 — Completion Report

**Repository:** https://github.com/charles-phiri-simba/networkplanningoptimization.git  
**Verified locally:** `C:\workspaces\networkplanningoptimization`  
**Verification date:** 2026-08-26 (local); 2026-08-27 (personal Azure E2E)  
**Architecture:** `docs/architecture/SNIP-PHASE-10-PRODUCTION-SECRET-WORKLOAD-IDENTITY-CONNECTOR-RUNTIME-SECURITY-ARCHITECTURE.md`  
**Contract:** `docs/implementation/SNIP-PHASE-10-PRODUCTION-SECRET-WORKLOAD-IDENTITY-CONNECTOR-RUNTIME-SECURITY-SPECIFICATION.md`  
**Baseline:** `4dfd8f0ec7d254ea292ab909b709eee3e599ef45` on `main` (Phase 9 architecturally accepted). Phase 10 is architecturally accepted and frozen; Git baseline is not yet established.  
**Method:** Extend Phase 9; `mvn -B test` **159 tests, 0 failures** (re-run 2026-08-27 after RBAC/rotation proofs; PostgreSQL Testcontainers; in-memory Key Vault stand-in; stub generator; no Ollama, live Azure, ENM, or NetAct); `go test ./...` PASS; `go build ./cmd/simulator` exit 0 from `simulator/`. Phase 11 was not started. Git commit / push / new baseline were not authorised.

---

## 1. Executive Summary

Phase 10 adds a **production secret and Workload Identity envelope** around the frozen Phase 9 MOCK_SECURE connector and frozen Phase 8 import runtime. Production configuration resolves synthetic connector credentials from Azure Key Vault using explicit Workload Identity, with per-session resolution, no local-provider fallback, no older-version fallback, and lease-before-secret ordering across independently instantiated replicas.

Default CI remains Azure-independent. On 2026-08-27 the required Azure E2E ran on a disposable personal non-production lab (`rg-snip-phase10-lab`, AKS `aks-snip-phase10-lab`, Key Vault `kvsnipp10e59l`). The pod path AKS SA → Workload Identity → dedicated UAMI `id-snip-connector-phase10` → Key Vault → synthetic MOCK_SECURE import completed with zero secret values in audit. GitHub environment `azure-e2e-int` was not used; this was an interactive personal-subscription proof, not default CI.

Phase 10 does not connect to real Ericsson ENM or Nokia NetAct. No real vendor credentials are introduced. After architectural acceptance and capture of the sanitized Azure E2E evidence in §56, the disposable personal lab was deleted: `az group exists --name rg-snip-phase10-lab` returned `false`; no AKS-managed resource group associated with `aks-snip-phase10-lab` remains; no Phase 10 Azure runtime resources are intentionally retained. Git commit / push / Phase 10 Git baseline are **not** established pending explicit authorization. Phase 11 has not started.

---

## 2. Phase 9 Baseline Verification

| Check | Result |
|-------|--------|
| Started from `4dfd8f0ec7d254ea292ab909b709eee3e599ef45` | Yes |
| Phase 9 commit message | `feat: establish SNIP Phase 9 integration security foundation` |
| Phase 1–9 regressions | Covered by default Maven suite plus new Phase 10 tests |
| Phase 9 MOCK_SECURE TLS/mTLS/redaction | Preserved (`ConnectorSecureImportApiTest`) |
| Phase 8 NEW/RETRY/REPLAY, lease, fencing | Preserved |
| Phase 11 | Not started |
| No real vendor connector | Pass |

---

## 3. Scope Delivered

- Real `AzureKeyVaultCredentialProvider` with JSON username/password and client-certificate packaging
- `WorkloadIdentityCredential` production path; `DefaultAzureCredential` local-only and refused in production
- In-memory Key Vault accessor for default CI; SDK accessor gated behind `azure-key-vault.enabled`
- External trust-material retrieval from vault when CUSTOM_CA has no in-memory certs
- Lease-before-secret ordering and multi-instance proofs against shared Testcontainers PostgreSQL
- Flyway `V11__production_secret_failure_codes.sql` (failure codes only; no secret-value columns)
- Bounded Terraform identity/federation/secret-scoped GET RBAC (`infra/phase10/`) with no secret values
- Kubernetes ServiceAccount, pod security, NetworkPolicy/Cilium FQDN egress (`deploy/k8s/`)
- Environment-gated workflow `.github/workflows/azure-e2e.yml` (not default CI)
- ADRs 069–078
- Safe readiness fields on connector security and import health

---

## 4. Production Secret Architecture

```text
AKS Pod + snip-connector-runtime SA
      ↓
Workload Identity → snip-connector-secrets-mi
      ↓
Azure Key Vault (synthetic secret/cert)
      ↓
Phase 8 lease/fencing
      ↓
Phase 9 SecureConnectorClientFactory session
      ↓
Secure mock vendor → Phase 7 reconciliation
```

Lease is acquired before vault GET and before connector I/O.

---

## 5. Azure Key Vault Provider

`SdkAzureKeyVaultSecretAccessor` is created only when `azure-key-vault.use-sdk-client=true` (AKS sets `SNIP_AZURE_KEY_VAULT_SDK=true`). Default CI and Maven tests keep the flag false so WorkloadIdentityCredential is not constructed without Azure IMDS/federation files.

Callers still select `connectorId` only.

---

## 6. Workload Identity Model

Production authentication is `WORKLOAD_IDENTITY`. `MANAGED_IDENTITY` is treated as the same alias. Kubernetes SA `snip-connector-runtime` federates to UAMI `snip-connector-secrets-mi`.

---

## 7. Dedicated Managed Identity

Represented in `infra/phase10/main.tf` as `azurerm_user_assigned_identity.connector_secrets` (default name `id-snip-connector-phase10`). Provisioned on the 2026-08-27 personal lab: UAMI `id-snip-connector-phase10`, client id `599c5fa4-c815-42ab-a5d5-60ce381b7771`. GitHub `azure-e2e-int` was not used.

---

## 8. Kubernetes ServiceAccount

`deploy/k8s/serviceaccount.yaml`: `snip-connector-runtime`, Workload Identity labels/annotations, projected token only (no long-lived SA secret object).

---

## 9. Azure Credential Selection

| Profile | Credential |
|---------|------------|
| `productionRuntime=true` | `WorkloadIdentityCredential` only; DefaultAzureCredential refused at startup and in SDK factory |
| Local Azure (non-production, `DEFAULT_AZURE_CREDENTIAL`) | `DefaultAzureCredential` |
| Default CI | No SDK client; in-memory or unconfigured accessor |

---

## 10. Environment Isolation

Secret names default to `snip-{env}-{vendor}-inventory-reader` with `environment: INT`. INT vs PROD vault isolation is a platform topology target. It was not proven against two real vaults in this environment.

---

## 11. Vault Topology

Configuration carries `vault-uri`, per-credential secret names, optional trust secret names, and optional pinned versions. Default CI `enabled: false`.

---

## 12. Azure RBAC

Terraform assigns **Key Vault Secrets User** to the dedicated UAMI at individual secret scope (credential and trust secrets only). SET/DELETE are not granted. Live least-privilege was proven from the SNIP workload identity on 2026-08-27: GET on configured secrets HTTP 200; GET `snip-unrelated-scope` 403; SET 403; DELETE 403. See §35. Vault-wide Secrets User on the connector UAMI was destroyed. GitHub `azure-e2e-int` was not used.

---

## 13. Secret Naming / References

Fixed map:

- `ericsson-enm-int-inventory-reader` → `snip-int-ericsson-inventory-reader`
- `nokia-netact-int-inventory-reader` → `snip-int-nokia-inventory-reader`

Public APIs cannot override these.

---

## 14. Secret Version Semantics

Resolved Key Vault version is stored as Phase 9 `credentialVersion` on session/audit metadata. Secret values are not persisted.

---

## 15. No Older-Version Fallback

`InMemoryAzureKeyVaultSecretAccessor` selects the latest inserted version (or a pin). If that version is disabled, it throws `VAULT_SECRET_DISABLED` even when an older enabled version exists. Proven in `AzureKeyVaultCredentialProviderTest`.

---

## 16. Per-Session Resolution

Each `SecureConnectorClientFactory.open` calls `provider.resolve`. No vendor-secret value field is retained on the provider beyond the last **version id** for rotation metrics.

---

## 17. Key Vault Client Reuse

`SdkAzureKeyVaultSecretAccessor` holds one `SecretClient`. Values are returned per `get` and parsed locally; they are not stored on the accessor.

---

## 18. Username/Password Packaging

One JSON secret: `{"username","password"}`. Parsed only in the provider.

---

## 19. Client Certificate Packaging

JSON: `{"certificateDerBase64","privateKeyPkcs8Base64"}` with optional username/password for `BASIC_PLUS_MTLS`. Reconstructed into `CredentialHandle` only. `toString` remains redacted; `clear()` wipes key material.

---

## 20. External Trust Material

`ConnectorTrustMaterialProvider` loads PEM/DER from a configured trust secret when CUSTOM_CA has no in-memory certificates. Phase 9 tests that inject in-memory CAs are unchanged.

---

## 21. Trust Rotation

Unit proof: two vault versions of a CA PEM; a subsequent `resolve` returns the new certificate without constructing a new provider. Azure E2E rotation across replicas was not run.

---

## 22. Key Vault Failure Mapping

| Condition | Code |
|-----------|------|
| 401 / credential errors | `VAULT_AUTHENTICATION_FAILED` |
| 403 (except SecretDisabled) | `VAULT_ACCESS_DENIED` |
| 404 | `VAULT_SECRET_NOT_FOUND` |
| disabled latest / SecretDisabled | `VAULT_SECRET_DISABLED` |
| timeout / other HTTP | `VAULT_UNAVAILABLE` |
| trust PEM/DER failure | `TRUST_MATERIAL_RESOLUTION_FAILED` |

Exception messages are safe; SDK text is not copied into API payloads.

---

## 23. Retryability

Only `VAULT_UNAVAILABLE` is retryable by default. No automatic import retry loop.

---

## 24. Request Timeout

SDK GET is bounded by `snip.integration.security.azure-key-vault.timeout` (default 5s) via `CompletableFuture.get`. Timeout maps to `VAULT_UNAVAILABLE`.

---

## 25. No Local Provider Fallback

Production startup refuses `local-credentials-enabled=true`. `CredentialProviderRegistry` and `LocalDevelopmentCredentialProvider` refuse LOCAL in production. Integration test: local store populated, vault `VAULT_SECRET_NOT_FOUND` → import FAILED with that code, not COMPLETED.

---

## 26. Phase 8 Lease-Before-Secret Ordering

`NetworkImportService.importSecure` acquires the lease before `connectorClientFactory.open`. A foreign owner yields `ImportBusyException` / `LEASE_UNAVAILABLE` and does not increment in-memory vault `gets()`.

---

## 27. Multi-Instance Architecture

`snip.integration.instance-id` identifies a runtime. Proof uses two Spring application contexts sharing Testcontainers PostgreSQL, not two threads on one `NetworkImportService`.

---

## 28. Same-Scope Multi-Replica Proof

Replica A holds the ERICSSON_SECURE_MOCK/DEFAULT lease. Independently instantiated replica B `importSecure` is rejected and B's vault accessor `gets()` is unchanged.

---

## 29. Independent-Scope Proof

Replica A holds Ericsson lease; replica B acquires Nokia lease concurrently. Lease keys differ. No global lock.

---

## 30. Pod/Instance Recovery Proof

Lease expiry is forced in PostgreSQL; successor replica acquires a higher fencing token; stale `assertOwnership` throws `LEASE_LOST`. Full AKS pod-kill was not run.

---

## 31. Rotation Across Replicas

Local: successive `resolve` calls observe v1 then v2 without restart. Genuine Azure credential rotation was run on 2026-08-27: v1 `8a4cae28aa374aea90c95ed51eacb749` then v2 `62f5d56fdda940bb9081d2255420170b` on the two unrestarted `snip-npo` replicas (startTime `2026-08-27T13:50:54Z`, restarts 0). Post-rotation import `bbf466cd-...` resolved v2 with `AUTHENTICATION_SUCCEEDED` and no secret value in audit. The Azure same-scope replica loser-without-GET proof remains Maven-only (`MultiInstanceConnectorRuntimeTest`). See §56.

---

## 32. Secret Disable/Revocation Proof

Latest disabled → `VAULT_SECRET_DISABLED`, `UNREAD`, zero entities created on that execution, mock write path not used.

---

## 33. Infrastructure-as-Code

New bounded module `infra/phase10/` (no prior Terraform in repo). Extends deployment with `deploy/k8s/*` and non-root `Dockerfile` user 10001. `docker-compose.yml` remains the local Compose path.

---

## 34. Federated Identity Credential

`azurerm_federated_identity_credential.connector_runtime` subject `system:serviceaccount:snip:snip-connector-runtime`. Applied on the 2026-08-27 personal AKS lab: federated subject matches; pods presented `AZURE_CLIENT_ID` for UAMI `id-snip-connector-phase10` and `AZURE_FEDERATED_TOKEN_FILE`; Workload Identity → Key Vault succeeded. GitHub `azure-e2e-int` was not used. See §56.

---

## 35. Key Vault RBAC Proof

Vault-wide Key Vault Secrets User on the connector UAMI was removed. The UAMI is Key Vault Secrets User only at individual secret scope:

- `/.../vaults/kvsnipp10e59l/secrets/snip-int-ericsson-inventory-reader`
- `/.../vaults/kvsnipp10e59l/secrets/snip-int-ericsson-trust`

The human owner retains Key Vault Secrets Officer on the vault (bootstrap/admin only). The SNIP workload identity has no secret-management role.

Probed from the SNIP workload identity (`snip-connector-runtime` + Workload Identity Job, not developer Azure CLI). Secret values were not printed.

| Operation | Target | HTTP | Result |
|-----------|--------|------|--------|
| GET | `snip-int-ericsson-inventory-reader` | 200 | PASS (version id only) |
| GET | `snip-int-ericsson-trust` | (session TLS_VALIDATED) | PASS via connector path |
| GET | `snip-unrelated-scope` | 403 | PASS denied |
| SET | `snip-probe-set-denied` | 403 | PASS denied |
| DELETE | `snip-unrelated-scope` | 403 | PASS denied |

Post-rotation GET of the configured credential still HTTP 200 with version `62f5d56fdda940bb9081d2255420170b`; unrelated GET/SET/DELETE remained 403.

---

## 36. Environment Isolation Proof

Distinct INT vs PROD vaults were not provisioned. Same-vault unauthorized-scope proof: GET `snip-unrelated-scope` from the SNIP workload identity returned HTTP 403 after secret-scoped RBAC.

---

## 37. Kubernetes NetworkPolicy

Default-deny egress plus DNS, PostgreSQL, and mock vendor pod allows. CiliumNetworkPolicy adds FQDN allows for `*.vault.azure.net`, `login.microsoftonline.com`, and mock hostnames. IMDS and arbitrary internet are not allowed. Applied to personal AKS `aks-snip-phase10-lab` on 2026-08-27. Cilium `toFQDNs` did not obtain DNS-proxy/FQDN-cache behavior (`cilium fqdn cache` empty; bpf `:53` `PROXY PORT NONE`); the lab used bounded Azure data-plane CIDRs `102.133.0.0/16`, `20.190.0.0/16`, and `40.126.0.0/16` on port 443 — not `0.0.0.0/0`. See §56.

---

## 38. VNet/Firewall Defense in Depth

Outside current repo authority. Documented as platform-owned. No unsafe public-network workaround added.

---

## 39. Key Vault Private Endpoint/DNS Status

**Gap:** this repository does not own VNet, Private DNS, or Key Vault firewall. Private Endpoint + private DNS + public network access disabled remains the target and is not implemented here.

---

## 40. Network Isolation E2E

Run from `snip-npo` on personal AKS on 2026-08-27: Key Vault HTTPS `404` in 56ms (connectivity, unauthenticated root); `login.microsoftonline.com` `302` after Azure AD CIDR allow; mock GET `401` / POST `403`; `https://example.com` timeout; IMDS `169.254.169.254` timeout. Cilium `toFQDNs` did not obtain DNS-proxy/FQDN-cache behavior, so the lab used the bounded Azure CIDRs in §37/§56 — not `0.0.0.0/0`. Key Vault Private Endpoint, private DNS, and public-network-disable remain unimplemented platform gaps (§39). GitHub `azure-e2e-int` was not used. No real ENM/NetAct connectivity was introduced.

---

## 41. Pod Security

Deployment: `runAsNonRoot`, `runAsUser: 10001`, `allowPrivilegeEscalation: false`, drop ALL capabilities, `RuntimeDefault` seccomp, `readOnlyRootFilesystem: true` with `/tmp` emptyDir. Dockerfile creates user 10001. Exception: local `docker-compose` api service still uses the image USER 10001; Compose does not add Kubernetes seccomp.

---

## 42. Terraform Secret-State Audit

Inspected `infra/phase10/main.tf`: variables are resource IDs, issuer URL, namespace, SA name. No username, password, token, PKCS12, or private key. Outputs are client/principal IDs only. Synthetic secret bootstrap is documented outside Terraform.

---

## 43. Azure Token Redaction

No `AccessToken` is stored, logged, or returned. SDK credentials are confined to `SdkAzureKeyVaultSecretAccessor`. Agents have no Key Vault beans in their collaboration graph.

---

## 44. Security Audit Extension

`CREDENTIAL_RESOLVED` details include `provider=` and `version=`. Failure codes include the new vault codes via existing `failure_code` column. No secret value, no token.

---

## 45. Azure E2E Workflow

`.github/workflows/azure-e2e.yml` is `workflow_dispatch` / `workflow_call` only, environment `azure-e2e-int`. Default `ci.yml` is unchanged and does not call Azure. The workflow refuses to proceed without AKS vars and states that Actions must not GetSecret for connector credentials.

---

## 46. Real AKS Workload Identity → Key Vault Proof

Run on 2026-08-27 on personal non-production AKS `aks-snip-phase10-lab`: pod path AKS SA `snip-connector-runtime` → Workload Identity → dedicated UAMI `id-snip-connector-phase10` → Key Vault `kvsnipp10e59l` → synthetic MOCK_SECURE import `f5317664-d079-49d4-b0f9-1df263089f9b` `COMPLETED` (`entitiesCreated=4`). Audit recorded `CREDENTIAL_RESOLVED` provider=`AZURE_KEY_VAULT` version=`8a4cae28aa374aea90c95ed51eacb749` with no secret value. GitHub environment `azure-e2e-int` was not used; this was an interactive personal-subscription proof, not default CI. No real ENM/NetAct connectivity was introduced. See §56.

---

## 47. Default CI

`.github/workflows/ci.yml` remains `setup-java` + `setup-go` + `go test` + `mvn -B test`. No Azure login.

---

## 48. Phase 9 Regression

`ConnectorSecureImportApiTest` retained (TLS, hostname, password, rotation, mTLS, redirect, canary redaction, readiness). Local credentials remain the Phase 9 test profile (`local-credentials-enabled=true`, `production-runtime=false`).

---

## 49. Phase 8 Runtime Regression

Lease, fencing, replay, retry, watchdog tests remain in the Maven suite. Secure import still releases the lease in `finally`.

---

## 50. Phase 7 Reconciliation Boundary

Secure mock snapshots still flow through existing normalizer/validator/reconciliation. No live vendor adapter. Import still does not call `TwinSynchronizationService`.

---

## 51. Agent / LLM / MCP Boundary

No Agent class references Key Vault, tokens, or `CredentialHandle`. No MCP tool for secrets. RAG is unchanged and does not ingest vault material.

---

## 52. Threat Model

| Threat | Control |
|--------|---------|
| Workload identity impersonation | Dedicated SA + federated subject; no shared node identity in production |
| SA token theft | Projected short-lived WI token; no long-lived SA secret |
| Vault overprivilege | Secrets User only in IaC |
| Cross-environment access | Environment-scoped names; distinct vaults are the target |
| Vault outage | `VAULT_UNAVAILABLE`, fail closed, no automatic retry |
| Secret downgrade | No older-version fallback |
| Local fallback in production | Startup guard + registry + provider refuse |
| Terraform secret leakage | Values forbidden in IaC |
| Duplicate replica resolution | Lease before GET |
| Network-policy bypass | App policy canonical + K8s/Cilium deny |
| IMDS/node fallback | Explicit WI; DefaultAzureCredential banned in production |

---

## 53. Observability

Counters: `vaultCredentialResolutions`, `vaultCredentialResolutionFailures`, `vaultAccessDenied`, `vaultUnavailable`, `vaultSecretDisabled`, `workloadIdentityAuthenticationFailures`, `credentialVersionChangesObserved`, `multiReplicaLeaseContention`. No secret/token labels.

---

## 54. Tests

- `AzureKeyVaultCredentialProviderTest` — contract, mapping, no fallback, cert parse, trust rotation, retryability
- `ProductionVaultConnectorTest` — vault-backed mock import, redaction, no local fallback, lease-before-secret, disabled secret, health/readiness without vault GET
- `MultiInstanceConnectorRuntimeTest` — two Spring contexts, same-scope loser, cross-scope leases, fencing recovery
- Existing Phase 1–9 tests

---

## 55. Local E2E Evidence

Recorded after `mvn -B test`, `go test ./...`, and `go build ./cmd/simulator` in §64 / verification notes below.

---

## 56. Azure E2E Evidence

Personal lab, 2026-08-27. Subscription `Azure subscription 1` / tenant `charlesrasheyahoo.onmicrosoft.com`. Synthetic secrets only. Secret values, access tokens, and private keys are not recorded here.

| Item | Value |
|------|--------|
| Resource group | `rg-snip-phase10-lab` (southafricanorth) |
| AKS | `aks-snip-phase10-lab` Running, Free SKU, 2× `Standard_E4bs_v5`, Cilium overlay |
| ACR | `acrsnipp10e59l.azurecr.io/snip-npo:phase10` digest `sha256:19e8d4f31bfb3102dc48ff90e902e9c9170fc88a5e2282ea3f251775fc2fff9e` |
| Key Vault | `https://kvsnipp10e59l.vault.azure.net/` RBAC enabled |
| UAMI | `id-snip-connector-phase10` client id `599c5fa4-c815-42ab-a5d5-60ce381b7771` |
| Federated subject | `system:serviceaccount:snip:snip-connector-runtime` |
| Replicas | 2 `snip-npo` pods Ready, distinct `ownerInstanceId` values |

**Pod identity (not developer Azure CLI):** `AZURE_CLIENT_ID` matches the UAMI; `AZURE_FEDERATED_TOKEN_FILE` present; `SNIP_AZURE_KEY_VAULT_AUTH=WORKLOAD_IDENTITY`; `SNIP_LOCAL_CREDENTIALS_ENABLED=false`; `SNIP_PRODUCTION_RUNTIME=true`; `SNIP_AZURE_KEY_VAULT_SDK=true`.

**Health / readiness:** `credentialProviderMode=AZURE_KEY_VAULT`; both MOCK_SECURE connectors `workloadIdentityConfigured=true`, `vaultConfigured=true`, `networkPolicyConfigured=true`.

**Lease-before-secret (fail closed):** import `12a0a50b-...` acquired lease (`fencingToken=1`) then `VAULT_UNAVAILABLE` while Key Vault egress was blocked; `entitiesCreated=0`; lease released. Audit: `SESSION_REQUESTED` then `SESSION_FAILED` / `VAULT_UNAVAILABLE` with `credentialVersion=null`.

**WI → Key Vault → mock import:** import `f5317664-d079-49d4-b0f9-1df263089f9b` `COMPLETED`; `entitiesRead=4`, `entitiesCreated=4`; snapshot `er-snap-secure-001`. Security audit (no secret value):

- `CREDENTIAL_RESOLVED` provider=`AZURE_KEY_VAULT` version=`8a4cae28aa374aea90c95ed51eacb749`
- `NETWORK_POLICY_VALIDATED`
- `TLS_VALIDATED`
- `AUTHENTICATION_SUCCEEDED`
- `SESSION_COMPLETED`

**Network (from `snip-npo` pod):** Key Vault HTTPS `404` in 56ms (connectivity, unauthenticated root); `login.microsoftonline.com` `302` after Azure AD CIDR allow; mock GET `401` / POST `403`; `https://example.com` timeout; IMDS `169.254.169.254` timeout.

**Cilium FQDN gap:** `toFQDNs` was configured but the AKS Cilium datapath did not attach DNS proxy (`cilium fqdn cache` empty; bpf `:53` `PROXY PORT NONE`). Lab used Azure data-plane CIDRs `102.133.0.0/16` (SAN Key Vault), `20.190.0.0/16` and `40.126.0.0/16` (Azure AD) on port 443 — **not** `0.0.0.0/0`.

**Replicas:** two pods, instance ids `f53da9e6-...` and `c44d3914-...`. Same-scope follow-up imports were `REPLAY` (zero canonical mutation). Azure same-scope loser-without-GET was not captured on a long overlap; that proof remains the Maven `MultiInstanceConnectorRuntimeTest`. Fail-path above still proves lease before vault GET.

**Rotation (genuine new secret version, no snip-npo restart):**

| | Version ID |
|--|------------|
| v1 (pre-rotation) | `8a4cae28aa374aea90c95ed51eacb749` |
| v2 (post-rotation) | `62f5d56fdda940bb9081d2255420170b` |

assert v1 != v2. New synthetic username/password JSON was provisioned via ARM PUT (not an identical no-op). Mock Deployment was rolled to match the new credential; **snip-npo pods were not restarted** (`snip-npo-6b584bc4ff-2lq7b` / `bfwfj` startTime `2026-08-27T13:50:54Z`, restarts 0).

Import `bbf466cd-cf9c-4878-be36-b0ca990cea33` on the unrestarted pods: `COMPLETED` / `REPLAY` (canonical snapshot unchanged). Security audit:

- `CREDENTIAL_RESOLVED` provider=`AZURE_KEY_VAULT` version=`62f5d56fdda940bb9081d2255420170b` (v2, not v1)
- `AUTHENTICATION_SUCCEEDED`
- `SESSION_COMPLETED`

No secret value in audit. No older-version fallback. Workload-identity GET after rotation returned HTTP 200 version=`62f5d56fdda940bb9081d2255420170b`.

**Least privilege:** see §35. Vault-wide Secrets User destroyed. Developer CLI was not used to GetSecret for the application path.

**Secrets bootstrap:** synthetic files in `%TEMP%\snip-phase10-lab`; Key Vault populated via ARM REST (`--query name` only). Terraform state has no secret values.

**Teardown (after architectural acceptance):** `az group delete -n rg-snip-phase10-lab --yes --no-wait` was submitted on 2026-08-27 after the sanitized acceptance evidence in this section had been captured. Verification on the personal subscription: `az group exists --name rg-snip-phase10-lab` returned `false`. `az group list` found no resource group whose name contains `aks-snip-phase10-lab` or `MC_rg-snip-phase10-lab`. A read-only subscription scan for Phase 10 names (`snip-phase10`, `snipp10`, `snip-connector-phase10`, `MC_rg-snip-phase10`) returned no remaining resources. No Phase 10 Azure runtime resources are intentionally retained.

**Cost (order of magnitude):** 2× `Standard_E4bs_v5` plus Free AKS, Basic ACR, Standard Key Vault in `southafricanorth` — roughly tens of USD per day while the lab was up.

**GitHub Actions:** `.github/workflows/azure-e2e.yml` remains environment-gated and separate from default CI. This run was interactive, not that workflow.

---

## 57. ADRs

069–078 created under `docs/architecture/adr/` with status **Accepted for Phase 10**.

---

## 58. Performance

Vault GET is inside the existing Phase 8 execution timeout. SDK timeout default 5s. No additional unbounded remote call was added to health endpoints.

---

## 59. Acceptance PASS/FAIL

| Gate | Result |
|------|--------|
| Starts from Phase 9 SHA | PASS |
| Default Maven/Go verification | See local run |
| Azure E2E on non-production AKS | PASS (personal lab 2026-08-27; see §56) |
| Real vendor connector absent | PASS |
| Phase 11 not started | PASS |
| Secrets absent from Git/IaC | PASS (synthetic values not committed) |

Specification §46: both default CI **and** Azure E2E passed. Architectural review on 2026-08-27 accepted Phase 10.

---

## 60. Known Limitations

- Private Endpoint/DNS not implemented (platform gap); Key Vault public network access enabled for this personal lab
- Cilium `toFQDNs` did not populate IP sets on this AKS datapath; lab used Azure data-plane CIDR allow-lists instead of `0.0.0.0/0`
- Azure same-scope replica loser-without-GET was not captured on a long overlap in this lab
- Health `credentialProviderMode` is derived from `productionRuntime`, not a live vault probe
- Workstation Azure CLI cannot talk to `*.vault.azure.net` / `*.azurecr.io` data planes (corporate SSL inspection); AKS and ARM paths were used instead
- GitHub environment `azure-e2e-int` was not the executor of this proof

---

## 61. Technical Debt

Preserve Phase 5 non-interruptible Agent timeout and Phase 6 failed-simulation persistence debt. Phase 10 adds: enable Key Vault diagnostics correlation by executionId outside SNIP; cooperative cancellation still deferred; attach Cilium DNS proxy so `toFQDNs` can replace lab CIDR allow-lists.

---

## 62. Lessons Learned

An in-memory vault stand-in can prove application fail-closed behaviour, but it cannot substitute for Workload Identity federation or Azure RBAC. Keeping default CI Azure-free required a strict `ConditionalOnMissingBean` around the SDK client.

---

## 63. Recommended Next Phase

Do **not** start Phase 11. Real Ericsson ENM and Nokia NetAct connectivity remain deferred. A Phase 10 Git baseline is not established until explicit authorization.

---

## 64. Architectural Review — resolved

Recorded as **resolved** on architectural acceptance (2026-08-27):

1. **Direct Azure Key Vault SDK access remains canonical.** Key Vault CSI is deferred and non-canonical.
2. **AKS Workload Identity with a dedicated UAMI remains the production identity model.** DefaultAzureCredential is local development only.
3. **Secret-level RBAC for connector credential/trust material is accepted for the Phase 10 security boundary.** Vault-wide GET on the connector identity is not accepted.
4. **Per-session Key Vault resolution and no older-version fallback remain locked.** No vendor-secret value cache.
5. **Genuine rotation without application restart is accepted.** Next connector session resolves the latest enabled version.
6. **Kubernetes application/network egress defense remains accepted.** Application-level egress policy remains canonical; Kubernetes/Cilium policy is defense in depth.
7. **The current Cilium FQDN-cache/CIDR behavior remains a documented known limitation, not a blocker.**
8. **Real Ericsson ENM and Nokia NetAct connectivity remain deferred.**
9. **Phase 11 has not started.**

---

## Local verification commands

```text
mvn -B test
go test ./...
go build ./cmd/simulator
```

**DEFAULT CI STATUS:** PASS — `mvn -B test` 159 tests, 0 failures; `go test ./...` PASS; `go build ./cmd/simulator` exit 0. Azure-independent workflow unchanged.

**AZURE E2E STATUS:** PASS — personal non-production AKS lab `rg-snip-phase10-lab` (2026-08-27). WI → Key Vault → MOCK_SECURE import `COMPLETED`. Secret-scoped RBAC and genuine v1→v2 rotation proven. Not default CI. After architectural acceptance, the disposable lab was deleted; `rg-snip-phase10-lab` no longer exists; the AKS-managed resource group no longer remains; no Phase 10 Azure runtime resources are intentionally retained.

PHASE 10 STATUS: ARCHITECTURALLY ACCEPTED
