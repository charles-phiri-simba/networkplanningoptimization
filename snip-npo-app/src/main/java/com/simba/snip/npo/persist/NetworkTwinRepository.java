package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NetworkTwinRepository extends JpaRepository<NetworkTwinEntity, UUID> {

    Optional<NetworkTwinEntity> findByScopeTypeAndScopeId(String scopeType, String scopeId);
}
