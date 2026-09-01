package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProposedActionRepository extends JpaRepository<ProposedActionEntity, UUID> {

    List<ProposedActionEntity> findAllByOrderByProposedAtDesc();

    List<ProposedActionEntity> findByAssuranceCaseIdOrderByProposedAtDesc(UUID assuranceCaseId);
}
