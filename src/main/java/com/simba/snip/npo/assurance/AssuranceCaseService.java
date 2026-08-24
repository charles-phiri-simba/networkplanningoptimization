package com.simba.snip.npo.assurance;

import com.simba.snip.npo.persist.AssuranceCaseEntity;
import com.simba.snip.npo.persist.AssuranceCaseRepository;
import com.simba.snip.npo.persist.AssuranceEvidenceEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AssuranceCaseService {

    private static final Set<String> ACTIVE = Set.of(CaseStatus.OPEN.name(), CaseStatus.ACKNOWLEDGED.name());

    private final AssuranceCaseRepository repository;
    private final AssuranceMetrics metrics;

    public AssuranceCaseService(AssuranceCaseRepository repository, AssuranceMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    @Transactional
    public AssuranceCaseEntity upsertActive(String cellId, DegradingRadioQualityDetector.Detection detection) {
        Optional<AssuranceCaseEntity> existing = repository.findFirstByAffectedEntityIdAndCaseTypeAndStatusIn(
                cellId, CaseType.DEGRADING_RADIO_QUALITY.name(), ACTIVE);
        List<AssuranceEvidenceEntity> facts = toEvidence(detection);
        if (existing.isPresent()) {
            AssuranceCaseEntity current = existing.get();
            current.updateObservation(
                    detection.observedAt(),
                    detection.severity().name(),
                    detection.confidence().name()
            );
            current.replaceEvidence(facts);
            metrics.incrementUpdated();
            return repository.save(current);
        }
        Instant now = Instant.now();
        AssuranceCaseEntity created = AssuranceCaseEntity.create(
                UUID.randomUUID(),
                CaseType.DEGRADING_RADIO_QUALITY.name(),
                AssuranceRules.ENTITY_CELL,
                cellId,
                detection.severity().name(),
                detection.confidence().name(),
                CaseStatus.OPEN.name(),
                now,
                detection.observedAt(),
                detection.observedAt(),
                AssuranceRules.DEGRADING_RADIO_QUALITY_RULE_ID,
                detection.synthetic()
        );
        created.replaceEvidence(facts);
        metrics.incrementCreated();
        return repository.save(created);
    }

    @Transactional(readOnly = true)
    public Optional<AssuranceCaseEntity> findById(UUID id) {
        return repository.loadById(id);
    }

    @Transactional(readOnly = true)
    public List<AssuranceCaseEntity> listAll() {
        return repository.findAllByOrderByDetectedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AssuranceCaseEntity> listForCell(String cellId) {
        return repository.findByAffectedEntityIdOrderByDetectedAtDesc(cellId);
    }

    private static List<AssuranceEvidenceEntity> toEvidence(DegradingRadioQualityDetector.Detection detection) {
        return detection.evidence().stream()
                .map(fact -> AssuranceEvidenceEntity.create(
                        UUID.randomUUID(),
                        fact.evidenceType(),
                        fact.metric(),
                        fact.value(),
                        fact.unit(),
                        fact.trend() == null ? null : fact.trend().name(),
                        fact.observedAt(),
                        fact.source(),
                        fact.synthetic(),
                        fact.description()
                ))
                .toList();
    }
}
