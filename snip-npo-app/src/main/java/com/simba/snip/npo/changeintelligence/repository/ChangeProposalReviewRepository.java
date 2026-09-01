package com.simba.snip.npo.changeintelligence.repository;

import com.simba.snip.npo.changeintelligence.persist.ChangeProposalReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChangeProposalReviewRepository extends JpaRepository<ChangeProposalReviewEntity, UUID> {

    List<ChangeProposalReviewEntity> findByProposal_IdOrderByReviewedAtAsc(UUID proposalId);
}
