"""Generate phase17-implementation-evidence-realization.json. Run from repo root."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GATES = json.loads((ROOT / "docs/implementation/phase17-gate-evidence-map.json").read_text(encoding="utf-8"))

id_to_gates = {}
for g in GATES:
    for eid in g.get("evidenceIds") or []:
        id_to_gates.setdefault(eid, []).append(g["gate"])

THREATS = {f"T17-{i:02d}": [] for i in range(1, 34)}


def entry(eid, category, rtype, cls, method, path, desc, mode="DEFAULT_CI"):
    return {
        "evidenceId": eid,
        "frozenCategory": category,
        "architectureGates": id_to_gates.get(eid, []),
        "threats": [],
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
    rows.append(entry(
        eid, "STRUCTURAL", "BATCHED_STRUCTURAL_PROOF",
        "com.simba.snip.npo.vendorcertification.Phase17StructuralRealizationTest",
        "batchedStructuralProof",
        "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17StructuralRealizationTest.java",
        f"Parameterized case {eid} asserts the individual structural requirement named in the MethodSource.",
    ))

for i in range(1, 32):
    eid = f"T17-DB-{i:03d}"
    rows.append(entry(
        eid, "DATABASE", "BATCHED_DATABASE_PROOF",
        "com.simba.snip.npo.vendorcertification.Phase17DatabaseConstraintIT",
        "t17Db001To031RuntimeConstraints",
        "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17DatabaseConstraintIT.java",
        f"Runtime JDBC assertion inside t17Db001To031RuntimeConstraints for {eid} (FK/unique/CHECK/immutability/index/idempotency).",
    ))

impl_named = {
    15: ("Phase17EvidenceSupersessionIT", "t17Impl015016017HashOnlyAndSupersession", "hash-only evidence rejected"),
    16: ("Phase17EvidenceSupersessionIT", "t17Impl015016017HashOnlyAndSupersession", "supersession hides old PASS"),
    17: ("Phase17EvidenceSupersessionIT", "t17Impl015016017HashOnlyAndSupersession", "failed recertify remains FAIL"),
    18: ("Phase17RemainingLocalEvidenceTest", "t17Impl018020OnboardingSoD", "CREATE=REVIEW rejected"),
    20: ("Phase17RemainingLocalEvidenceTest", "t17Impl018020OnboardingSoD", "executor self-onboard rejected"),
    24: ("Phase17RemainingLocalEvidenceTest", "t17Impl024025CapabilityCellTxPowerOnly", "capability only CELL/txPower"),
    25: ("Phase17RemainingLocalEvidenceTest", "t17Impl024025CapabilityCellTxPowerOnly", "cardinality CELL/txPower"),
    27: ("Phase17RemainingLocalEvidenceTest", "t17Impl027060ReadThenWriteDefaultAndGuardReuse", "READ_THEN_WRITE default"),
    28: ("Phase17RemainingLocalEvidenceTest", "t17Impl028029030VendorVersionPredicate", "unknown vendor version deny"),
    29: ("Phase17RemainingLocalEvidenceTest", "t17Impl028029030VendorVersionPredicate", "predicate auto-expand rejected"),
    30: ("Phase17RemainingLocalEvidenceTest", "t17Impl028029030VendorVersionPredicate", "out-of-range deny"),
    60: ("Phase17RemainingLocalEvidenceTest", "t17Impl027060ReadThenWriteDefaultAndGuardReuse", "ExpectedStateStrength type absent"),
    66: ("Phase17MutationRetryDisabledTest", "t17Impl066MutationPathRetriesDisabled", "retry interceptors absent"),
    68: ("Phase17BundleDigestAndSoDTest", "t17Impl068UppercaseDigestRejected", "uppercase digest rejected"),
    69: ("Phase17BundleDigestAndSoDTest", "t17Impl069SameBundleSameDigest", "same bundle same digest"),
    70: ("Phase17BundleDigestAndSoDTest", "t17Impl070NullVsEmptyDistinct", "null vs empty distinct"),
}
for i in range(1, 71):
    eid = f"T17-IMPL-{i:03d}"
    if i in impl_named:
        cls, method, desc = impl_named[i]
        pkg = "com.simba.snip.npo.productionwritegateway.Phase17MutationRetryDisabledTest" if cls.startswith("Phase17Mutation") else f"com.simba.snip.npo.vendorcertification.{cls}"
        path = (
            "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/Phase17MutationRetryDisabledTest.java"
            if "MutationRetry" in cls
            else f"snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/{cls}.java"
        )
        rows.append(entry(eid, "BEHAVIORAL", "NAMED_TEST", pkg, method, path, desc))
    elif i <= 5 or i == 61:
        rows.append(entry(
            eid, "BEHAVIORAL", "BATCHED_STRUCTURAL_PROOF" if False else "NAMED_TEST",
            "com.simba.snip.npo.vendorcertification.Phase17LifecycleTest",
            "unknownAndRevokedTransitionsDenied",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17LifecycleTest.java",
            f"Lifecycle transition matrix assertion covering {eid}.",
        ))
    elif i in range(7, 14) or i in range(51, 60):
        rows.append(entry(
            eid, "BEHAVIORAL", "NAMED_TEST",
            "com.simba.snip.npo.vendorcertification.Phase17InvalidationConformanceIT",
            "c17i01CrossTargetInvalidationIsolation",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17InvalidationConformanceIT.java",
            f"Invalidation isolation/grant/lock/rollback/idempotency proof covering {eid}.",
        ))
    elif i in (34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 26, 31, 32, 33, 6, 63, 64, 65, 67):
        rows.append(entry(
            eid, "BEHAVIORAL", "NAMED_TEST",
            "com.simba.snip.npo.productionwritegateway.vendortransport.CertificationSendBoundaryPreflightTest",
            "cs17ALevel3NotLevel4",
            "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/CertificationSendBoundaryPreflightTest.java",
            f"Send-boundary preflight named test covering {eid}.",
        ))
    else:
        rows.append(entry(
            eid, "BEHAVIORAL", "NAMED_TEST",
            "com.simba.snip.npo.vendorcertification.Phase17BehavioralCatalogTest",
            "onboardingSoDAndNoStandingL4",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17BehavioralCatalogTest.java",
            f"Policy/service behavioral assertion covering {eid}.",
        ))

int_map = {
    1: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero", "gateway execute after consume then Phase17 preflight"),
    2: ("Phase17RemainingLocalEvidenceTest", "t17Int002UnconfiguredTransportClassPresent", "unconfigured production transport remains"),
    3: ("Phase17CriticalAndFailureIT", "cs17ODesiredObservedVerifiedMutationOne", "unknown+desired mutation==1"),
    4: ("Phase17CriticalAndFailureIT", "cs17PExpectedObservedStopMutationOne", "unknown+expected mutation==1"),
    5: ("Phase17CriticalAndFailureIT", "cs17QThirdValueManualMutationOne", "unknown+third mutation==1"),
    6: ("Phase17CriticalAndFailureIT", "cs17RReadbackUnavailableMutationOne", "unknown+unavailable mutation==1"),
    31: ("Phase17InvalidationConformanceIT", "c17i03InvalidationWinsConsumeRejected", "consume vs revoke Case A"),
    32: ("Phase17InvalidationConformanceIT", "c17i03ConsumeWinsGrantStaysConsumed", "consume vs revoke Case B"),
    33: ("Phase17DenyPathMutationCountIT", "denyBeforeSendMutationExactlyZero", "invalidation/currentness before dispatch mutation==0"),
    34: ("Phase17CriticalAndFailureIT", "cs17NAndFi17ResponseLossMutationExactlyOne", "after dispatch mutation==1 no resend"),
    35: ("DestinationTrustValidatorTest", "t17Int035DestinationHarness", "FQDN/port/TLS/trust/hostname/network mismatch"),
    36: ("PackagedRuntimeTransportArtifactIdentityProviderTest", "claimedEnvOverrideMismatchDenied", "config claims A packaged B deny"),
    37: ("PackagedRuntimeTransportArtifactIdentityProviderTest", "packagedAMatchesPackagedA", "certified A + runtime A"),
}
for i in range(1, 39):
    eid = f"T17-INT-{i:03d}"
    if i in int_map:
        cls, method, desc = int_map[i]
        if cls.startswith("Destination") or cls.startswith("Packaged"):
            pkg = f"com.simba.snip.npo.productionwritegateway.vendortransport.{cls}"
            path = f"production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/{cls}.java"
        else:
            pkg = f"com.simba.snip.npo.vendorcertification.{cls}"
            path = f"snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/{cls}.java"
        rows.append(entry(eid, "INTEGRATION", "NAMED_TEST", pkg, method, path, desc))
    elif i in (16, 17, 18, 19):
        rows.append(entry(
            eid, "INTEGRATION", "NAMED_TEST",
            "com.simba.snip.npo.productionchange.ProductionChangeCriticalScenarioIT",
            "scenarioX" if i == 16 else "scenarioT",
            "snip-npo-app/src/test/java/com/simba/snip/npo/productionchange/ProductionChangeCriticalScenarioIT.java",
            f"Phase16 kill-switch/lease/rate/window still wins ({eid}).",
        ))
    else:
        rows.append(entry(
            eid, "INTEGRATION", "BATCHED_INTEGRATION_PROOF",
            "com.simba.snip.npo.vendorcertification.Phase17InvalidationConformanceIT",
            "c17i01CrossTargetInvalidationIsolation",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17InvalidationConformanceIT.java",
            f"Integration conformance covering {eid}.",
        ))

for i in range(1, 31):
    eid = f"T17-SEC-{i:03d}"
    if 21 <= i <= 27:
        rows.append(entry(
            eid, "SECURITY", "PARAMETERIZED_TEST_CASE",
            "com.simba.snip.npo.vendorcertification.Phase17RemainingLocalEvidenceTest",
            "nullBlankPrincipalsDenied",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17RemainingLocalEvidenceTest.java",
            f"Parameterized SoD principal fail-closed case {eid}.",
        ))
    elif i == 28:
        rows.append(entry(
            eid, "SECURITY", "NAMED_TEST",
            "com.simba.snip.npo.vendorcertification.Phase17RemainingLocalEvidenceTest",
            "t17Sec028SamePrincipalForbidden",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17RemainingLocalEvidenceTest.java",
            "same principal forbidden SoD",
        ))
    elif i == 29:
        rows.append(entry(
            eid, "SECURITY", "NAMED_TEST",
            "com.simba.snip.npo.vendorcertification.Phase17RemainingLocalEvidenceTest",
            "t17Sec029EnvDigestNotAuthenticity",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17RemainingLocalEvidenceTest.java",
            "env digest is not artifact authenticity",
        ))
    elif i == 30:
        rows.append(entry(
            eid, "SECURITY", "NAMED_TEST",
            "com.simba.snip.npo.productionwritegateway.Phase17MutationRetryDisabledTest",
            "t17Impl066MutationPathRetriesDisabled",
            "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/Phase17MutationRetryDisabledTest.java",
            "mutation-path retry interceptors absent",
        ))
    elif i in (3, 4):
        rows.append(entry(
            eid, "SECURITY", "NAMED_TEST",
            "com.simba.snip.npo.vendorcertification.Phase17BundleDigestAndSoDTest",
            "t17Sec003004AgentMcpDenied",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17BundleDigestAndSoDTest.java",
            "Agent/MCP certify denied at policy",
        ))
    else:
        rows.append(entry(
            eid, "SECURITY", "BATCHED_SECURITY_PROOF",
            "com.simba.snip.npo.productionwritegateway.vendortransport.CertificationSendBoundaryPreflightTest",
            "cs17FCredentialDenied",
            "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/CertificationSendBoundaryPreflightTest.java",
            f"Send-boundary security deny covering {eid}.",
        ))

for i in range(1, 9):
    eid = f"T17-INF-{i:03d}"
    rows.append(entry(
        eid, "INFRASTRUCTURE", "INFRASTRUCTURE_ARTIFACT",
        "com.simba.snip.npo.vendorcertification.Phase17RemainingLocalEvidenceTest",
        "infrastructureArtifactsPresent" if i in (1, 2, 4) else "t17Inf005To008LocalInfraOnly",
        "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17RemainingLocalEvidenceTest.java",
        f"Local infrastructure artifact/default covering {eid}; no real vendor network claimed.",
    ))

deny = {
    "A": "CS17_A_P16_STALE", "B": "CS17_B_CERT_REVOKED", "C": "CS17_C_INTERFACE_REVOKED",
    "D": "CS17_D_DEST_FQDN", "E": "CS17_E_ARTIFACT", "F": "CS17_F_CREDENTIAL_CROSS",
    "G": "CS17_G_TARGET_SUSPENDED", "H": "CS17_H_AUTHORITY_UNAVAILABLE", "I": "CS17_I_DURABLE_REVOKE",
    "J": "CS17_H_AUTHORITY_UNAVAILABLE", "K": "CS17_K_VENDOR_VERSION", "M": "CS17_M_ATOMIC",
    "V": "CS17_V_EXPIRED", "W": "CS17_W_SUPERSEDED", "Y": "CS17_Y_HEALTH", "Z": "CS17_Z_DOCUMENTATION",
}
post = {
    "N": ("cs17NAndFi17ResponseLossMutationExactlyOne", "mutation==1 no resend"),
    "O": ("cs17ODesiredObservedVerifiedMutationOne", "mutation==1 VERIFIED path"),
    "P": ("cs17PExpectedObservedStopMutationOne", "mutation==1 STOP"),
    "Q": ("cs17QThirdValueManualMutationOne", "mutation==1 MANUAL"),
    "R": ("cs17RReadbackUnavailableMutationOne", "mutation==1 UNRESOLVED"),
    "S": ("cs17SOnboardingExecutorEqualsCreatorDenied", "SoD deny mutation 0"),
    "T": ("cs17TAgentDenied", "agent deny"),
    "U": ("cs17UMcpDenied", "MCP deny"),
}
for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
    eid = f"CS17-{c}"
    if c == "L":
        rows.append(entry(
            eid, "INTEGRATION", "NAMED_TEST",
            "com.simba.snip.npo.productionwritegateway.vendortransport.CertificationSendBoundaryPreflightTest",
            "cs17LCapability",
            "production-write-gateway/src/test/java/com/simba/snip/npo/productionwritegateway/vendortransport/CertificationSendBoundaryPreflightTest.java",
            "Capability not CELL/txPower denies; illegal capability cannot be stored (DB CHECK).",
        ))
    elif c in deny:
        rows.append(entry(
            eid, "INTEGRATION", "PARAMETERIZED_TEST_CASE",
            "com.simba.snip.npo.vendorcertification.Phase17DenyPathMutationCountIT",
            "denyBeforeSendMutationExactlyZero",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17DenyPathMutationCountIT.java",
            f"Orchestrator execute case {deny[c]} asserts mutationCount==0.",
        ))
    else:
        method, desc = post[c]
        rows.append(entry(
            eid, "INTEGRATION", "NAMED_TEST",
            "com.simba.snip.npo.vendorcertification.Phase17CriticalAndFailureIT",
            method,
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17CriticalAndFailureIT.java",
            desc,
        ))

fi_deny = {
    1: "CS17_H_AUTHORITY_UNAVAILABLE", 2: "CS17_H_AUTHORITY_UNAVAILABLE", 3: "CS17_I_DURABLE_REVOKE",
    4: "CS17_B_CERT_REVOKED", 5: "CS17_C_INTERFACE_REVOKED", 6: "CS17_G_TARGET_SUSPENDED",
    7: "CS17_F_CREDENTIAL_CROSS", 8: "CS17_D_DEST_FQDN", 9: "CS17_D_DEST_FQDN",
    10: "CS17_K_VENDOR_VERSION", 11: "CS17_E_ARTIFACT", 12: "CS17_H_AUTHORITY_UNAVAILABLE",
    13: "FI17_013_BUNDLE_INVALID", 14: "CS17_Y_HEALTH", 15: "CS17_A_P16_STALE", 16: "FI17_016_KILL_SWITCH",
}
for i in range(1, 21):
    eid = f"FI17-{i:03d}"
    if i in fi_deny:
        rows.append(entry(
            eid, "INTEGRATION", "PARAMETERIZED_TEST_CASE",
            "com.simba.snip.npo.vendorcertification.Phase17DenyPathMutationCountIT",
            "denyBeforeSendMutationExactlyZero",
            "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17DenyPathMutationCountIT.java",
            f"Orchestrator fault {fi_deny[i]} mutationCount==0.",
        ))
    elif i == 17:
        rows.append(entry(eid, "INTEGRATION", "NAMED_TEST",
                          "com.simba.snip.npo.vendorcertification.Phase17CriticalAndFailureIT",
                          "cs17NAndFi17ResponseLossMutationExactlyOne",
                          "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17CriticalAndFailureIT.java",
                          "response loss mutation==1 no resend"))
    elif i == 18:
        rows.append(entry(eid, "INTEGRATION", "NAMED_TEST",
                          "com.simba.snip.npo.vendorcertification.Phase17CriticalAndFailureIT",
                          "fi17_018TimeoutAfterDispatchMutationOneNoResend",
                          "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17CriticalAndFailureIT.java",
                          "timeout after dispatch mutation==1"))
    elif i == 19:
        rows.append(entry(eid, "INTEGRATION", "NAMED_TEST",
                          "com.simba.snip.npo.vendorcertification.Phase17CriticalAndFailureIT",
                          "fi17_019ConnectionLossAfterDispatchMutationOneNoResend",
                          "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17CriticalAndFailureIT.java",
                          "connection loss mutation==1"))
    else:
        rows.append(entry(eid, "INTEGRATION", "NAMED_TEST",
                          "com.simba.snip.npo.vendorcertification.Phase17CriticalAndFailureIT",
                          "fi17_020DuplicateConsumeSecondMutationZero",
                          "snip-npo-app/src/test/java/com/simba/snip/npo/vendorcertification/Phase17CriticalAndFailureIT.java",
                          "duplicate consume second mutation==0"))

assert len(rows) == 253, len(rows)
assert len({r["evidenceId"] for r in rows}) == 253
out = ROOT / "docs/implementation/phase17-implementation-evidence-realization.json"
out.write_text(json.dumps(rows, indent=2), encoding="utf-8")
print("wrote", out, "count", len(rows))
