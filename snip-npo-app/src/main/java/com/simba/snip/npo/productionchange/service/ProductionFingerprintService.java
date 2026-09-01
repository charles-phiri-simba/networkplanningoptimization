package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.audit.CanonicalJson;
import com.simba.snip.npo.productionchange.audit.Sha256Hex;
import com.simba.snip.npo.productionchange.domain.ProductionFingerprintInput;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProductionFingerprintService {

    public String compute(ProductionFingerprintInput input) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("adapterProfileId", input.adapterProfileId());
        fields.put("authorizationGeneration", input.authorizationGeneration());
        fields.put("capabilityProfileVersion", input.capabilityProfileVersion());
        fields.put("cellId", input.cellId());
        fields.put("changeControlReference", input.changeControlReference());
        fields.put("changeWindowEnd", input.changeWindowEnd());
        fields.put("changeWindowId", input.changeWindowId());
        fields.put("changeWindowStart", input.changeWindowStart());
        fields.put("credentialProfileId", input.credentialProfileId());
        fields.put("desiredValue", input.desiredValue());
        fields.put("environment", input.environment());
        fields.put("expectedValue", input.expectedValue());
        fields.put("parameter", input.parameter());
        fields.put("phase14PlanFingerprint", input.phase14PlanFingerprint());
        fields.put("phase14PlanId", input.phase14PlanId());
        fields.put("phase14PlanVersion", input.phase14PlanVersion());
        fields.put("phase15ExecutionFingerprint", input.phase15ExecutionFingerprint());
        fields.put("phase15ExecutionId", input.phase15ExecutionId());
        fields.put("platform", input.platform());
        fields.put("productionPolicyVersion", input.productionPolicyVersion());
        fields.put("productionTargetId", input.productionTargetId());
        fields.put("rollbackDesiredValue", input.rollbackDesiredValue());
        fields.put("rollbackExpectedValue", input.rollbackExpectedValue());
        fields.put("rollbackPolicyVersion", input.rollbackPolicyVersion());
        fields.put("securityProfileId", input.securityProfileId());
        fields.put("vendor", input.vendor());
        fields.put("verificationPolicyVersion", input.verificationPolicyVersion());
        return Sha256Hex.hash(CanonicalJson.serialize(fields));
    }

    public String computeTargetFingerprint(
            String targetId,
            String vendor,
            String platform,
            String environment,
            String adapterProfileId,
            String capabilityProfileVersion,
            String securityProfileId,
            String credentialProfileId,
            String certificationLevel,
            String expectedStateGuardStrength
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("adapterProfileId", adapterProfileId);
        fields.put("capabilityProfileVersion", capabilityProfileVersion);
        fields.put("certificationLevel", certificationLevel);
        fields.put("credentialProfileId", credentialProfileId);
        fields.put("environment", environment);
        fields.put("expectedStateGuardStrength", expectedStateGuardStrength);
        fields.put("platform", platform);
        fields.put("securityProfileId", securityProfileId);
        fields.put("targetId", targetId);
        fields.put("vendor", vendor);
        return Sha256Hex.hash(CanonicalJson.serialize(fields));
    }

    public String operationBindingHash(
            String cellId,
            String parameter,
            BigDecimal expectedValue,
            BigDecimal desiredValue,
            String grantType
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("cellId", cellId);
        fields.put("desiredValue", desiredValue);
        fields.put("expectedValue", expectedValue);
        fields.put("grantType", grantType);
        fields.put("parameter", parameter);
        return Sha256Hex.hash(CanonicalJson.serialize(fields));
    }
}
