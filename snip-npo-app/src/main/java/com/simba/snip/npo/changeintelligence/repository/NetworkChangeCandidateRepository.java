package com.simba.snip.npo.changeintelligence.repository;

import com.simba.snip.npo.changeintelligence.persist.NetworkChangeCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkChangeCandidateRepository extends JpaRepository<NetworkChangeCandidateEntity, UUID> {

    List<NetworkChangeCandidateEntity> findByProposal_IdOrderByRankOrderAscCandidateValueAsc(UUID proposalId);
}
