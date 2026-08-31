package com.simba.snip.npo.changeplanning.persist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "network_change_plan_operation_dependency")
public class NetworkChangePlanOperationDependencyEntity {

    @Id
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "depends_on_operation_id", nullable = false)
    private UUID dependsOnOperationId;

    public static NetworkChangePlanOperationDependencyEntity create(
            UUID id,
            UUID planId,
            UUID operationId,
            UUID dependsOnOperationId
    ) {
        NetworkChangePlanOperationDependencyEntity entity = new NetworkChangePlanOperationDependencyEntity();
        entity.id = id;
        entity.planId = planId;
        entity.operationId = operationId;
        entity.dependsOnOperationId = dependsOnOperationId;
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public UUID getOperationId() { return operationId; }
    public UUID getDependsOnOperationId() { return dependsOnOperationId; }
}
