package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.domain.CertificationLevel;
import com.simba.snip.npo.productionchange.domain.ExpectedStateGuardStrength;
import com.simba.snip.npo.productionchange.domain.ProductionTargetState;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkTargetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ProductionTargetRegistry {

    public static final String DEFAULT_L0_TARGET_ID = "ERICSSON-ENM-PRODUCTION-L0";

    public record TargetRegistration(
            String targetId,
            String vendor,
            String platform,
            String environment,
            String region,
            String networkDomain,
            String adapterProfileId,
            String capabilityProfileVersion,
            String securityProfileId,
            String credentialProfileId,
            String allowedObjectTypes,
            String allowedParameters,
            String changeWindowPolicy,
            String rollbackPolicy,
            String verificationPolicy,
            CertificationLevel certificationLevel,
            boolean enabled,
            ProductionTargetState targetState,
            ExpectedStateGuardStrength expectedStateGuardStrength
    ) {
        public static TargetRegistration l0Ericsson(String targetId) {
            return new TargetRegistration(
                    targetId,
                    "ERICSSON",
                    "ENM",
                    "LAB",
                    "test",
                    "RAN",
                    "ericsson-enm-write-l0",
                    "1",
                    "security-l0",
                    "credential-profile-ref-l0",
                    "CELL",
                    "txPower",
                    "MANUAL",
                    "p16-rollback-v1",
                    "p16-verification-v1",
                    CertificationLevel.L0,
                    true,
                    ProductionTargetState.ACTIVE,
                    ExpectedStateGuardStrength.READ_THEN_WRITE
            );
        }
    }

    private final ProductionNetworkTargetRepository targetRepository;
    private final ProductionFingerprintService fingerprintService;
    private final Clock clock;

    public ProductionTargetRegistry(
            ProductionNetworkTargetRepository targetRepository,
            ProductionFingerprintService fingerprintService,
            Clock clock
    ) {
        this.targetRepository = targetRepository;
        this.fingerprintService = fingerprintService;
        this.clock = clock;
    }

    @Transactional
    public ProductionNetworkTargetEntity register(TargetRegistration registration) {
        Instant now = clock.instant();
        String fingerprint = fingerprintService.computeTargetFingerprint(
                registration.targetId(),
                registration.vendor(),
                registration.platform(),
                registration.environment(),
                registration.adapterProfileId(),
                registration.capabilityProfileVersion(),
                registration.securityProfileId(),
                registration.credentialProfileId(),
                registration.certificationLevel().name(),
                registration.expectedStateGuardStrength().name()
        );
        return targetRepository.findById(registration.targetId())
                .map(existing -> {
                    existing.setAdapterProfileId(registration.adapterProfileId());
                    existing.setCapabilityProfileVersion(registration.capabilityProfileVersion());
                    existing.setSecurityProfileId(registration.securityProfileId());
                    existing.setCredentialProfileId(registration.credentialProfileId());
                    existing.setEnabled(registration.enabled());
                    existing.setTargetState(registration.targetState().name());
                    existing.setExpectedStateGuardStrength(registration.expectedStateGuardStrength().name());
                    existing.setTargetFingerprint(fingerprint);
                    existing.setUpdatedAt(now);
                    return existing;
                })
                .orElseGet(() -> targetRepository.save(ProductionNetworkTargetEntity.create(
                        registration.targetId(),
                        registration.vendor(),
                        registration.platform(),
                        registration.environment(),
                        registration.region(),
                        registration.networkDomain(),
                        registration.adapterProfileId(),
                        registration.capabilityProfileVersion(),
                        registration.securityProfileId(),
                        registration.credentialProfileId(),
                        registration.allowedObjectTypes(),
                        registration.allowedParameters(),
                        registration.changeWindowPolicy(),
                        registration.rollbackPolicy(),
                        registration.verificationPolicy(),
                        registration.certificationLevel().name(),
                        registration.enabled(),
                        registration.targetState().name(),
                        fingerprint,
                        registration.expectedStateGuardStrength().name(),
                        now
                )));
    }

    @Transactional(readOnly = true)
    public ProductionNetworkTargetEntity require(String targetId) {
        return targetRepository.findById(targetId)
                .orElseThrow(() -> new ProductionChangeException(
                        ProductionReasonCode.PRODUCTION_TARGET_NOT_FOUND,
                        "production target not found"
                ));
    }

    @Transactional(readOnly = true)
    public Optional<ProductionNetworkTargetEntity> find(String targetId) {
        return targetRepository.findById(targetId);
    }

    @Transactional(readOnly = true)
    public List<ProductionNetworkTargetEntity> list() {
        return targetRepository.findAll();
    }
}
