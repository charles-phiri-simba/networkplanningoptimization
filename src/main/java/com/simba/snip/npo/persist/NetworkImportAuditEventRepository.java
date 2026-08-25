package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NetworkImportAuditEventRepository extends JpaRepository<NetworkImportAuditEventEntity, UUID> {

    List<NetworkImportAuditEventEntity> findByImportIdOrderByOccurredAtAsc(UUID importId);
}
