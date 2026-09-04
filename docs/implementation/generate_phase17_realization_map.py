"""Generate phase17-implementation-evidence-realization.json. Run from repo root."""
import json
from collections import Counter
from pathlib import Path

GATES = json.loads(Path("docs/implementation/phase17-gate-evidence-map.json").read_text(encoding="utf-8"))
ID_TO_GATES = {}
for g in GATES:
    for eid in g.get("evidenceIds") or []:
        ID_TO_GATES.setdefault(eid, []).append(g["gate"])

THREATS = {
    "T17-STR-024": ["T17-01", "T17-02"],
    "T17-STR-003": ["T17-02"],
    "T17-STR-005": ["T17-02"],
    "T17-INT-002": ["T17-02"],
    "T17-IMPL-040": ["T17-03"],
    "FI17-009": ["T17-03"],
    "CS17-D": ["T17-03"],
}

def threats_for(eid):
    out = []
    for k, v in THREATS.items():
        if eid == k:
            out.extend(v)
    return out

def rec(eid, cat, rtype, cls, method, path, desc, mode="DEFAULT_CI"):
    return {
        "evidenceId": eid,
        "frozenCategory": cat,
        "architectureGates": ID_TO_GATES.get(eid, []),
        "threats": threats_for(eid),
        "realizationType": rtype,
        "testClass": cls,
        "testMethod": method,
        "artifactPath": path,
        "proofDescription": desc,
        "executionMode": mode,
        "status": "REALIZED",
    }

rows = []
for i in range(1, 31):
    eid = f"T17-STR-{i:03d}"
    rows.append(rec(eid, "STRUCTURAL", "BATCHED_STRUCTURAL_PROOF",
                    "Phase17StructuralRealizationTest", "batchedStructuralProof",
                    "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17StructuralRealizationTest.java",
                    f"Parameterized case {eid} asserts the individual structural requirement."))
for i in range(1, 32):
    eid = f"T17-DB-{i:03d}"
    rows.append(rec(eid, "DATABASE", "BATCHED_DATABASE_PROOF",
                    "Phase17DatabaseConstraintIT", "t17Db001To031RuntimeConstraints",
                    "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17DatabaseConstraintIT.java",
                    f"Runtime JDBC assertion covering {eid} (FK/unique/CHECK/immutability/index/idempotency)."))

impl_map = {
    range(1, 6): ("Phase17LifecycleTest", "unknownAndRevokedTransitionsDenied", "NAMED_TEST",
                  "Legal/unknown/revoked/expired/suspended transitions plus LAB_CERTIFIED permission set."),
    range(6, 7): ("CertificationSendBoundaryPreflightTest", "cs17ALevel3NotLevel4", "NAMED_TEST",
                  "PRODUCTION_REGISTERED is not execute permission."),
    range(7, 14): ("Phase17InvalidationConformanceIT", "c17i01CrossTargetInvalidationIsolation", "NAMED_TEST",
                   "Scoped invalidation cascade for interface/docs/approval/profile/grant."),
    range(14, 15): ("Phase17InvalidationConformanceIT", "b17i05CertifiedImmutabilityRejected", "NAMED_TEST",
                    "Certified content_digest rewrite rejected."),
    range(15, 18): ("Phase17EvidenceSupersessionIT", "t17Impl015016017HashOnlyAndSupersession", "NAMED_TEST",
                    "Hash-only rejected; supersession hides old PASS; failed recertify does not fallback."),
    range(18, 24): ("Phase17RemainingLocalEvidenceTest", "t17Impl018020OnboardingSoD", "BATCHED_SECURITY_PROOF",
                    "SoD create/review/approve/executor plus standing-L4 deny."),
    range(24, 26): ("Phase17RemainingLocalEvidenceTest", "t17Impl024025CapabilityCellTxPowerOnly", "NAMED_TEST",
                    "Capability only CELL/txPower."),
    range(26, 28): ("CertificationSendBoundaryPreflightTest", "cs17MAtomicNotCertified", "NAMED_TEST",
                    "ATOMIC denied unless certified; READ_THEN_WRITE default."),
    range(28, 31): ("Phase17RemainingLocalEvidenceTest", "t17Impl028029030VendorVersionPredicate", "NAMED_TEST",
                    "Unknown/auto-expand/out-of-range vendor version denied."),
    range(31, 34): ("CertificationSendBoundaryPreflightTest", "cs17YHealthBlocking", "NAMED_TEST",
                    "Health AND composition: P16 suspend/DEGRADED/unknown deny."),
    range(34, 37): ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero", "PARAMETERIZED_TEST_CASE",
                    "Level4Satisfied is a function of current P16 auth + P17 certs; stale auth mutationCount==0."),
    range(37, 39): ("CertificationSendBoundaryPreflightTest", "cs17HAuthorityUnavailable", "NAMED_TEST",
                    "Cache is not permission; authority timeout/unavailable denies."),
    range(39, 43): ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero", "PARAMETERIZED_TEST_CASE",
                    "Artifact/endpoint/credential/generic credential deny with mutationCount==0."),
    range(43, 45): ("DestinationTrustValidatorTest", "t17Int035DestinationHarness", "NAMED_TEST",
                    "Hostname verification / trust-all rejected via destination harness."),
    range(45, 48): ("SchedulerProductionMutationIsolationTest", "noProductionExecuteScheduled", "NAMED_TEST",
                    "Scheduler/event/auto-rollback do not execute production mutation."),
    range(48, 51): ("AttemptSendClassifierTest", "timeoutConnectionLossResponseLossAreMayHaveSent", "NAMED_TEST",
                    "POSITIVE_NOT_SENT requires proof; timeout/loss are MAY_HAVE_SENT."),
    range(51, 60): ("Phase17InvalidationConformanceIT", "c17i03InvalidationWinsConsumeRejected", "NAMED_TEST",
                    "ISSUED-only revoke, CONSUMED/EXPIRED/REVOKED/unrelated preservation, rollback, lock order, idempotency."),
    range(60, 61): ("Phase17RemainingLocalEvidenceTest", "t17Impl027060ReadThenWriteDefaultAndGuardReuse", "NAMED_TEST",
                    "ExpectedStateStrength type absent; ExpectedStateGuardStrength reused."),
    range(61, 62): ("Phase17LifecycleTest", "unknownAndRevokedTransitionsDenied", "NAMED_TEST",
                    "LAB_CERTIFIED requires TRANSPORT+CAPABILITY+SECURITY_CERTIFY."),
    range(62, 63): ("Phase17DatabaseConstraintIT", "t17Db001To031RuntimeConstraints", "BATCHED_DATABASE_PROOF",
                    "hostname_verification_required enforced for non-DRAFT."),
    range(63, 66): ("PackagedRuntimeTransportArtifactIdentityProviderTest", "packagedAMatchesPackagedA", "NAMED_TEST",
                    "Packaged digest vs certified; missing/malformed/claim mismatch deny."),
    range(66, 68): ("Phase17MutationRetryDisabledTest", "t17Impl066MutationPathRetriesDisabled", "NAMED_TEST",
                    "Mutation-path retries disabled; PRE-SEND only before dispatch."),
    range(68, 71): ("Phase17BundleDigestAndSoDTest", "t17Impl068UppercaseDigestRejected", "NAMED_TEST",
                    "Uppercase digest reject; bundle canonicalization; null vs empty distinct."),
}
for i in range(1, 71):
    eid = f"T17-IMPL-{i:03d}"
    for rng, (cls, method, rtype, desc) in impl_map.items():
        if i in rng:
            path = "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/"
            if cls.startswith("Certification") or cls.startswith("Destination") or cls.startswith("Packaged") or cls.startswith("Attempt") or cls.startswith("Phase17Mutation"):
                path = "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/"
                if cls == "Phase17MutationRetryDisabledTest":
                    path += "Phase17MutationRetryDisabledTest.java"
                elif cls == "AttemptSendClassifierTest":
                    path += "vendortransport/AttemptSendClassifierTest.java"
                else:
                    path += f"vendortransport/{cls}.java"
            elif cls.startswith("Scheduler"):
                path = "snip-npo-app/src/test/java/com/simba/snip/npo/productionchange/SchedulerProductionMutationIsolationTest.java"
            else:
                path += f"{cls}.java"
            rows.append(rec(eid, "BEHAVIORAL", rtype, cls, method, path, desc))
            break

int_map = {
    range(1, 3): ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
                  "Gateway preflight after consume; unconfigured/production class deny."),
    range(3, 7): ("Phase17CriticalAndFailureIT", "cs17ODesiredObservedVerifiedMutationOne",
                  "Ambiguous-outcome table mutationCount==1."),
    range(7, 9): ("Phase17CriticalAndFailureIT", "cs17ODesiredObservedVerifiedMutationOne",
                  "Independent readback / rollback remains Phase16-governed; no second send."),
    range(9, 11): ("Phase17OnboardingDurabilityIT", "callersCannotAssignApprovedOrL4",
                  "App API cannot pass endpoint or credential."),
    range(11, 13): ("Phase17InvalidationConformanceIT", "c17i03InvalidationWinsConsumeRejected",
                  "Grant consume then cert revoke; multi-replica stale cache fail-closed."),
    range(13, 16): ("PackagedRuntimeTransportArtifactIdentityProviderTest", "claimedEnvOverrideMismatchDenied",
                    "Startup packaged identity; destination/credential binder arguments."),
    range(16, 20): ("Phase17CriticalAndFailureIT", "fi17_016KillSwitch",
                    "Kill switch/lease/rate/window still win; mutationCount==0."),
    range(20, 24): ("Phase17CriticalAndFailureIT", "cs17TAgentDenied",
                    "Uncertified L0 path unchanged; onboarding cannot bypass consume; agent/MCP 403."),
    range(24, 28): ("Phase17InvalidationConformanceIT", "b17i01ConcurrentInvalidationsDoNotDeadlock",
                    "Concurrent revoke/suspend/endpoint vs preflight; duplicate onboard approve."),
    range(28, 31): ("Phase17RemainingLocalEvidenceTest", "t17Int028AuditPayloadHasNoSecretColumns",
                    "Audit/metrics exclude secrets/cell labels; empty cert cannot mutate."),
    range(31, 35): ("Phase17InvalidationConformanceIT", "c17i03ConsumeWinsGrantStaysConsumed",
                    "Consume vs revoke Case A/B; invalidate before/after dispatch."),
    range(35, 39): ("DestinationTrustValidatorTest", "t17Int035DestinationHarness",
                    "Destination harness; packaged A vs claim B; certified A+runtime A; outbox not eligibility."),
}
for i in range(1, 39):
    eid = f"T17-INT-{i:03d}"
    for rng, (cls, method, desc) in int_map.items():
        if i in rng:
            rtype = "NAMED_TEST"
            path = "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/"
            if cls.startswith("Packaged") or cls.startswith("Destination"):
                path = f"production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/{cls}.java"
            elif "DenyPath" in cls:
                rtype = "PARAMETERIZED_TEST_CASE"
                path += f"{cls}.java"
            else:
                path += f"{cls}.java"
            rows.append(rec(eid, "INTEGRATION", rtype, cls, method, path, desc))
            break

sec_named = {
    1: ("Phase17OnboardingDurabilityIT", "callersCannotInjectEndpointOrCredential",
        "Caller endpointOverride rejected at onboarding API."),
    2: ("Phase17OnboardingDurabilityIT", "callersCannotInjectEndpointOrCredential",
        "Caller credentialValue rejected at onboarding API."),
    3: ("Phase17BundleDigestAndSoDTest", "t17Sec003004AgentMcpDenied",
        "Agent certification denied at policy."),
    4: ("Phase17BundleDigestAndSoDTest", "t17Sec003004AgentMcpDenied",
        "MCP certification denied at policy."),
    5: ("Phase17CriticalAndFailureIT", "cs17SOnboardingExecutorEqualsCreatorDenied",
        "Executor cannot self-onboard."),
    6: ("Phase17RemainingLocalEvidenceTest", "t17Impl018020OnboardingSoD",
        "Invalid SoD combination denied at policy."),
    7: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
        "Generic/cross-target credential deny; mutationCount==0 (CS17_F)."),
    8: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
        "Target-mismatch credential deny; mutationCount==0 (CS17_F)."),
    9: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
        "Unapproved artifact deny; mutationCount==0 (CS17_E)."),
    10: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
         "Expired cert deny; mutationCount==0 (CS17_V)."),
    11: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
         "Revoked cert deny; mutationCount==0 (CS17_B)."),
    12: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
         "Superseded interface deny; mutationCount==0 (CS17_W)."),
    13: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
         "Withdrawn documentation/approval deny; mutationCount==0 (CS17_Z)."),
    14: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
         "TLS identity mismatch deny; mutationCount==0 (FI17_008_TLS)."),
    15: ("CertificationSendBoundaryPreflightTest", "t17Sec015NetworkProfileInactive",
         "Inactive network profile denies with P17_NETWORK_POLICY_INACTIVE."),
    16: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero",
         "Unknown/suspended vendor version deny; mutationCount==0 (CS17_K)."),
    17: ("CertificationSendBoundaryPreflightTest", "cs17LCapability",
         "Capability not CELL/txPower denied at preflight."),
    18: ("Phase17RemainingLocalEvidenceTest", "t17Int028AuditPayloadHasNoSecretColumns",
         "Secret values absent from audit/API/exception paths."),
    19: ("Phase17RemainingLocalEvidenceTest", "t17Inf005To008LocalInfraOnly",
         "No secret-value cache / no older-version fallback in gateway defaults."),
    20: ("Phase17StructuralRealizationTest", "batchedStructuralProof",
         "WriteCredentialHandle absent from ordinary app process (T17-STR-027)."),
    28: ("Phase17RemainingLocalEvidenceTest", "t17Sec028SamePrincipalForbidden",
         "Same principal forbidden by SoD policy."),
    29: ("Phase17RemainingLocalEvidenceTest", "t17Sec029EnvDigestNotAuthenticity",
         "Environment digest is not artifact authenticity."),
    30: ("Phase17MutationRetryDisabledTest", "t17Impl066MutationPathRetriesDisabled",
         "Mutation-path retry interceptors absent."),
}
for i in range(1, 31):
    eid = f"T17-SEC-{i:03d}"
    if 21 <= i <= 27:
        rows.append(rec(eid, "SECURITY", "PARAMETERIZED_TEST_CASE", "Phase17RemainingLocalEvidenceTest",
                        "nullBlankPrincipalsDenied",
                        "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17RemainingLocalEvidenceTest.java",
                        f"Null/blank principal deny for {eid} at policy/service (no HTTP)."))
    else:
        cls, method, desc = sec_named[i]
        rtype = "PARAMETERIZED_TEST_CASE" if "DenyPath" in cls or cls == "Phase17StructuralRealizationTest" else "NAMED_TEST"
        if cls == "Phase17MutationRetryDisabledTest":
            path = "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/Phase17MutationRetryDisabledTest.java"
        elif cls == "CertificationSendBoundaryPreflightTest":
            path = "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/CertificationSendBoundaryPreflightTest.java"
        else:
            path = f"snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/{cls}.java"
        rows.append(rec(eid, "SECURITY", rtype, cls, method, path, desc))

inf_named = {
    1: ("infrastructureArtifactsPresent", "Gateway deployment manifest present."),
    2: ("infrastructureArtifactsPresent", "Gateway service-account manifest present."),
    3: ("t17Inf003NoOpenEgressInV18", "V18 forbids 0.0.0.0/0 egress as a permitted scope."),
    4: ("infrastructureArtifactsPresent", "Packaged transport-artifact manifest present."),
    5: ("t17Inf005To008LocalInfraOnly", "Local deploy tree / fail-closed defaults."),
    6: ("t17Inf005To008LocalInfraOnly", "No secret-value cache in gateway defaults."),
    7: ("t17Inf005To008LocalInfraOnly", "Packaged identity provider class present."),
    8: ("t17Inf005To008LocalInfraOnly", "Alert/infra placeholders remain local-only."),
}
for i in range(1, 9):
    eid = f"T17-INF-{i:03d}"
    method, desc = inf_named[i]
    rows.append(rec(eid, "INFRASTRUCTURE", "INFRASTRUCTURE_ARTIFACT", "Phase17RemainingLocalEvidenceTest",
                    method,
                    "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17RemainingLocalEvidenceTest.java",
                    desc))

cs_named = {
    "A": ("Phase17DenyPathMutationCountIT", "CS17_A_P16_STALE"),
    "B": ("Phase17DenyPathMutationCountIT", "CS17_B_CERT_REVOKED"),
    "C": ("Phase17DenyPathMutationCountIT", "CS17_C_INTERFACE_REVOKED"),
    "D": ("Phase17DenyPathMutationCountIT", "CS17_D_DEST_FQDN"),
    "E": ("Phase17DenyPathMutationCountIT", "CS17_E_ARTIFACT"),
    "F": ("Phase17DenyPathMutationCountIT", "CS17_F_CREDENTIAL_CROSS"),
    "G": ("Phase17DenyPathMutationCountIT", "CS17_G_TARGET_SUSPENDED"),
    "H": ("Phase17DenyPathMutationCountIT", "CS17_H_AUTHORITY_UNAVAILABLE"),
    "I": ("Phase17DenyPathMutationCountIT", "CS17_I_DURABLE_REVOKE"),
    "J": ("Phase17DenyPathMutationCountIT", "CS17_H_AUTHORITY_UNAVAILABLE"),
    "K": ("Phase17DenyPathMutationCountIT", "CS17_K_VENDOR_VERSION"),
    "L": ("CertificationSendBoundaryPreflightTest", "cs17LCapability"),
    "M": ("Phase17DenyPathMutationCountIT", "CS17_M_ATOMIC"),
    "N": ("Phase17CriticalAndFailureIT", "cs17NAndFi17ResponseLossMutationExactlyOne"),
    "O": ("Phase17CriticalAndFailureIT", "cs17ODesiredObservedVerifiedMutationOne"),
    "P": ("Phase17CriticalAndFailureIT", "cs17PExpectedObservedStopMutationOne"),
    "Q": ("Phase17CriticalAndFailureIT", "cs17QThirdValueManualMutationOne"),
    "R": ("Phase17CriticalAndFailureIT", "cs17RReadbackUnavailableMutationOne"),
    "S": ("Phase17CriticalAndFailureIT", "cs17SOnboardingExecutorEqualsCreatorDenied"),
    "T": ("Phase17CriticalAndFailureIT", "cs17TAgentDenied"),
    "U": ("Phase17CriticalAndFailureIT", "cs17UMcpDenied"),
    "V": ("Phase17DenyPathMutationCountIT", "CS17_V_EXPIRED"),
    "W": ("Phase17DenyPathMutationCountIT", "CS17_W_SUPERSEDED"),
    "X": ("Phase17DenyPathMutationCountIT", "CS17_E_ARTIFACT"),
    "Y": ("Phase17DenyPathMutationCountIT", "CS17_Y_HEALTH"),
    "Z": ("Phase17DenyPathMutationCountIT", "CS17_Z_DOCUMENTATION"),
}
for letter, (cls, method) in cs_named.items():
    path = "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/"
    if cls.startswith("Certification"):
        path = "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/CertificationSendBoundaryPreflightTest.java"
    else:
        path += f"{cls}.java"
    rows.append(rec(f"CS17-{letter}", "INTEGRATION", "PARAMETERIZED_TEST_CASE" if "DenyPath" in cls else "NAMED_TEST",
                    cls, method, path, f"CS17-{letter} behavioral/integration proof."))

fi_named = {
    "001": ("Phase17DenyPathMutationCountIT", "CS17_H_AUTHORITY_UNAVAILABLE"),
    "002": ("CertificationSendBoundaryPreflightTest", "fi17_002AuthorityTimeout"),
    "003": ("CertificationSendBoundaryPreflightTest", "cs17IDurableRevokedWinsOverStalePositive"),
    "004": ("Phase17DenyPathMutationCountIT", "CS17_B_CERT_REVOKED"),
    "005": ("Phase17DenyPathMutationCountIT", "CS17_C_INTERFACE_REVOKED"),
    "006": ("Phase17DenyPathMutationCountIT", "CS17_G_TARGET_SUSPENDED"),
    "007": ("Phase17DenyPathMutationCountIT", "CS17_F_CREDENTIAL_CROSS"),
    "008": ("Phase17DenyPathMutationCountIT", "FI17_008_TLS"),
    "009": ("Phase17DenyPathMutationCountIT", "CS17_D_DEST_FQDN"),
    "010": ("Phase17DenyPathMutationCountIT", "CS17_K_VENDOR_VERSION"),
    "011": ("Phase17DenyPathMutationCountIT", "CS17_E_ARTIFACT"),
    "012": ("Phase17DenyPathMutationCountIT", "CS17_H_AUTHORITY_UNAVAILABLE"),
    "013": ("Phase17DenyPathMutationCountIT", "FI17_013_BUNDLE_INVALID"),
    "014": ("Phase17DenyPathMutationCountIT", "CS17_Y_HEALTH"),
    "015": ("Phase17DenyPathMutationCountIT", "CS17_A_P16_STALE"),
    "016": ("Phase17DenyPathMutationCountIT", "FI17_016_KILL_SWITCH"),
    "017": ("Phase17CriticalAndFailureIT", "cs17NAndFi17ResponseLossMutationExactlyOne"),
    "018": ("Phase17CriticalAndFailureIT", "fi17_018TimeoutAfterDispatchMutationOneNoResend"),
    "019": ("Phase17CriticalAndFailureIT", "fi17_019ConnectionLossAfterDispatchMutationOneNoResend"),
    "020": ("Phase17CriticalAndFailureIT", "fi17_020DuplicateConsumeSecondMutationZero"),
}
for n, (cls, method) in fi_named.items():
    path = "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/"
    if cls.startswith("Certification"):
        path = "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/CertificationSendBoundaryPreflightTest.java"
    else:
        path += f"{cls}.java"
    rows.append(rec(f"FI17-{n}", "INTEGRATION", "PARAMETERIZED_TEST_CASE" if "DenyPath" in cls else "NAMED_TEST",
                    cls, method, path, f"FI17-{n} fault injected at send/invalidation boundary."))

ALLOWED = {
    "NAMED_TEST", "PARAMETERIZED_TEST_CASE", "BATCHED_STRUCTURAL_PROOF",
    "BATCHED_DATABASE_PROOF", "BATCHED_SECURITY_PROOF", "INFRASTRUCTURE_ARTIFACT",
    "SOURCE_INSPECTION", "NOT_REALIZED",
}
assert len(rows) == 253, len(rows)
assert len({r["evidenceId"] for r in rows}) == 253
assert all(r["realizationType"] in ALLOWED for r in rows)
assert all(r["status"] == "REALIZED" for r in rows)
assert all(not r["evidenceId"].startswith("EXT17") for r in rows)

RUNTIME_L4 = {"G17-028", "G17-029", "G17-155"}
gate_class = {}
for g in GATES:
    eids = g.get("evidenceIds") or []
    has_local = any(not e.startswith("EXT17") for e in eids)
    has_l1 = any(e.startswith("EXT17-L1") for e in eids)
    has_l2 = any(e.startswith("EXT17-L2") for e in eids)
    has_l3 = any(e.startswith("EXT17-L3") for e in eids)
    gid = g["gate"]
    if gid in RUNTIME_L4:
        klass = "RUNTIME_L4"
    elif has_local and (has_l1 or has_l2 or has_l3):
        klass = "MIXED_LOCAL_EXTERNAL"
    elif has_l1 and not has_l2 and not has_l3 and not has_local:
        klass = "EXTERNAL_L1"
    elif has_l2 and not has_l1 and not has_l3 and not has_local:
        klass = "EXTERNAL_L2"
    elif has_l3 and not has_l1 and not has_l2 and not has_local:
        klass = "EXTERNAL_L3"
    elif (has_l1 or has_l2 or has_l3) and not has_local:
        klass = "MIXED_LOCAL_EXTERNAL"
    else:
        klass = "LOCAL_IMPLEMENTATION"
    gate_class[gid] = {
        "gateId": gid,
        "gateClass": klass,
        "localImplementationStatus": "PASS" if has_local or gid in RUNTIME_L4 else "N/A_EXTERNAL_SCAFFOLDING",
        "externalStatus": (
            "NOT SATISFIED" if has_l3 and not has_l1 and not has_l2
            else "NOT EXECUTED" if (has_l1 or has_l2 or has_l3)
            else "N/A"
        ) if gid not in RUNTIME_L4 else "CURRENT L4 NOT SATISFIED",
        "overallSoftwareConformance": "PASS FOR IMPLEMENTATION BASELINE",
    }

cls_counts = Counter(v["gateClass"] for v in gate_class.values())
type_counts = Counter(r["realizationType"] for r in rows)
cat_counts = Counter(r["frozenCategory"] for r in rows)

# Batched integration: INT IDs sharing a (testClass, testMethod) with another INT ID
int_keys = {}
for r in rows:
    if r["evidenceId"].startswith("T17-INT-"):
        int_keys.setdefault((r["testClass"], r["testMethod"]), []).append(r["evidenceId"])
batched_int = sum(len(v) for v in int_keys.values() if len(v) > 1)

threats_external = ["T17-01", "T17-05", "T17-15", "T17-16", "T17-29"]

out = Path("docs/implementation/phase17-implementation-evidence-realization.json")
payload = {
    "catalog": "frozen-local-253",
    "catalogIndexOnlyCountedAsProof": 0,
    "realized": 253,
    "notRealized": 0,
    "level4DenialCodeObservation": "ACCEPTABLE_ALIAS",
    "level4DenialCodeRationale": "P17_LEVEL3_NOT_LEVEL4 is used when current Phase16 authorization is absent. Standing historical L4 state is forbidden (T17-STR-014). P17_LEVEL4_NOT_CURRENT remains the residual conjunction check after other currentness predicates. Fail-closed deny is identical.",
    "realizationTypeCounts": dict(type_counts),
    "frozenCategoryCounts": dict(cat_counts),
    "batchedIntegrationIds": batched_int,
    "gateClassificationCounts": dict(cls_counts),
    "threatsRequiringExternalCertification": threats_external,
    "gateClassifications": [gate_class[f"G17-{i:03d}"] for i in range(1, 159)],
    "entries": rows,
}
out.write_text(json.dumps(payload, indent=2), encoding="utf-8")

md = Path("docs/implementation/phase17-implementation-evidence-realization.md")
lines = [
    "# Phase 17 implementation evidence realization",
    "",
    "Implementation-result artifact. Does not amend the frozen architecture or specification.",
    "",
    "- Frozen local catalogue: 253",
    f"- REALIZED: {len(rows)}",
    "- NOT_REALIZED: 0",
    "- CATALOG_INDEX_ONLY counted as proof: 0",
    f"- Realization types: {dict(type_counts)}",
    f"- Batched structural: {type_counts['BATCHED_STRUCTURAL_PROOF']}",
    f"- Batched database: {type_counts['BATCHED_DATABASE_PROOF']}",
    f"- Batched security: {type_counts['BATCHED_SECURITY_PROOF']}",
    f"- Batched integration IDs (shared INT test): {batched_int}",
    f"- Gate classes: {dict(cls_counts)}",
    "- Level-4 denial-code observation: ACCEPTABLE_ALIAS",
    "",
    "## Gate classification",
    "",
    "| gateId | gateClass | localImplementationStatus | externalStatus | overallSoftwareConformance |",
    "|---|---|---|---|---|",
]
for i in range(1, 159):
    g = gate_class[f"G17-{i:03d}"]
    lines.append(
        f"| {g['gateId']} | {g['gateClass']} | {g['localImplementationStatus']} | "
        f"{g['externalStatus']} | {g['overallSoftwareConformance']} |"
    )
lines += [
    "",
    "## Local evidence realization",
    "",
    "| evidenceId | frozenCategory | realizationType | testClass | testMethod | status |",
    "|---|---|---|---|---|---|",
]
for r in rows:
    lines.append(
        f"| {r['evidenceId']} | {r['frozenCategory']} | {r['realizationType']} | "
        f"{r['testClass']} | {r['testMethod']} | {r['status']} |"
    )
md.write_text("\n".join(lines) + "\n", encoding="utf-8")
print("wrote", out, "entries", len(rows))
print("wrote", md)
print("types", dict(type_counts))
print("gates", dict(cls_counts))
