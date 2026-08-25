package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NetworkSourceRepository extends JpaRepository<NetworkSourceEntity, UUID> {

    Optional<NetworkSourceEntity> findBySourceSystem(String sourceSystem);
}
