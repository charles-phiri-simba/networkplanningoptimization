package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkImportRejectionRepository extends JpaRepository<NetworkImportRejectionEntity, UUID> {

    List<NetworkImportRejectionEntity> findAllByOrderByRejectedAtDesc();

    List<NetworkImportRejectionEntity> findByImportIdOrderByRejectedAtAsc(UUID importId);
}
