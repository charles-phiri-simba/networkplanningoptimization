package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkTwinVersionRepository extends JpaRepository<NetworkTwinVersionEntity, UUID> {

    List<NetworkTwinVersionEntity> findByTwin_IdOrderByVersionAsc(UUID twinId);

    Optional<NetworkTwinVersionEntity> findByTwin_IdAndVersion(UUID twinId, int version);
}
