package com.simba.snip.npo.changeintelligence.repository;

import com.simba.snip.npo.changeintelligence.persist.NetworkChangeProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkChangeProposalRepository extends JpaRepository<NetworkChangeProposalEntity, UUID> {

    List<NetworkChangeProposalEntity> findAllByOrderByCreatedAtDesc();

    List<NetworkChangeProposalEntity> findByTargetEntityTypeAndTargetEntityIdAndParameterNameOrderByCreatedAtDesc(
            String targetEntityType, String targetEntityId, String parameterName
    );

    Optional<NetworkChangeProposalEntity> findFirstByTargetEntityTypeAndTargetEntityIdAndParameterNameAndStatusOrderByCreatedAtDesc(
            String targetEntityType, String targetEntityId, String parameterName, String status
    );
}
