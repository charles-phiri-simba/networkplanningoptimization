package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ActionResultRepository extends JpaRepository<ActionResultEntity, UUID> {

    Optional<ActionResultEntity> findByActionId(UUID actionId);
}
