package com.simba.snip.npo.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "neighbour_relationship")
public class NeighbourRelationshipEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_cell_id", nullable = false)
    private CellEntity sourceCell;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_cell_id", nullable = false)
    private CellEntity targetCell;

    @Column(name = "relation_type", nullable = false, length = 64)
    private String relationType;

    @Column(nullable = false, length = 32)
    private String status;

    public UUID getId() {
        return id;
    }

    public CellEntity getSourceCell() {
        return sourceCell;
    }

    public CellEntity getTargetCell() {
        return targetCell;
    }

    public String getRelationType() {
        return relationType;
    }

    public String getStatus() {
        return status;
    }
}
