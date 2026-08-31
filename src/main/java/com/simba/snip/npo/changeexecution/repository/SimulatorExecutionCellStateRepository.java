package com.simba.snip.npo.changeexecution.repository;

import com.simba.snip.npo.changeexecution.entity.SimulatorExecutionCellStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SimulatorExecutionCellStateRepository extends JpaRepository<SimulatorExecutionCellStateEntity, UUID> {

    Optional<SimulatorExecutionCellStateEntity> findByTargetIdAndCellIdAndParameterName(
            String targetId,
            String cellId,
            String parameterName
    );
}
