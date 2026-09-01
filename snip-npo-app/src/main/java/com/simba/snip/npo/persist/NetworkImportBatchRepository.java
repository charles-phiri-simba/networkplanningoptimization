package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NetworkImportBatchRepository extends JpaRepository<NetworkImportBatchEntity, UUID> {

    List<NetworkImportBatchEntity> findAllByOrderByStartedAtDesc();

    List<NetworkImportBatchEntity> findBySourceSystemAndSourceScopeAndStatus(
            String sourceSystem, String sourceScope, String status);

    List<NetworkImportBatchEntity> findByStatus(String status);

    List<NetworkImportBatchEntity> findBySourceSystemAndSourceScopeAndSourceSnapshotIdOrderByAttemptNumberAsc(
            String sourceSystem, String sourceScope, String sourceSnapshotId);

    Optional<NetworkImportBatchEntity> findFirstBySourceSystemAndSourceScopeAndSynchronizationModeNotNullAndStatusOrderByCompletedAtDesc(
            String sourceSystem, String sourceScope, String status);

    Optional<NetworkImportBatchEntity> findFirstBySourceSystemAndStatusOrderByCompletedAtDesc(
            String sourceSystem, String status);
}
