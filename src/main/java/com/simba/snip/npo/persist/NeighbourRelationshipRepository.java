package com.simba.snip.npo.persist;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NeighbourRelationshipRepository extends JpaRepository<NeighbourRelationshipEntity, UUID> {

    @EntityGraph(attributePaths = {"sourceCell", "targetCell"})
    List<NeighbourRelationshipEntity> findBySourceCell_IdOrderByTargetCell_CellIdAsc(UUID sourceCellId);
}
