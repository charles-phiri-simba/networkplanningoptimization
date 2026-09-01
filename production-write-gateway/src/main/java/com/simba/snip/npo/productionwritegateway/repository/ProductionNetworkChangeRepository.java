package com.simba.snip.npo.productionwritegateway.repository;

import com.simba.snip.npo.productionwritegateway.entity.ProductionNetworkChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductionNetworkChangeRepository extends JpaRepository<ProductionNetworkChangeEntity, UUID> {
}
