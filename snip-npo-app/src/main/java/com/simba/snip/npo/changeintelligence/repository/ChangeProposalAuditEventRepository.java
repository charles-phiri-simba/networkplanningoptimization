package com.simba.snip.npo.changeintelligence.repository;

import com.simba.snip.npo.changeintelligence.persist.ChangeProposalAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChangeProposalAuditEventRepository extends JpaRepository<ChangeProposalAuditEventEntity, UUID> {

    List<ChangeProposalAuditEventEntity> findByProposalIdOrderByOccurredAtAsc(UUID proposalId);
}
