package com.simba.snip.npo.changeplanning.repository;

import com.simba.snip.npo.changeplanning.persist.NetworkChangePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkChangePlanRepository extends JpaRepository<NetworkChangePlanEntity, UUID> {

    List<NetworkChangePlanEntity> findAllByOrderByCreatedAtDesc();

    Optional<NetworkChangePlanEntity> findFirstByProposalIdAndStatusInOrderByCreatedAtDesc(
            UUID proposalId,
            List<String> activeStatuses
    );

    List<NetworkChangePlanEntity> findByProposalId(UUID proposalId);
}
