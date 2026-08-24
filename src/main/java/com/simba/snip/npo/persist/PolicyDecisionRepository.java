package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PolicyDecisionRepository extends JpaRepository<PolicyDecisionEntity, UUID> {

    Optional<PolicyDecisionEntity> findByActionId(UUID actionId);
}
