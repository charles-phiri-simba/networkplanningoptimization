package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductionExecutionGrantRepository extends JpaRepository<ProductionExecutionGrantEntity, UUID> {
}
