package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductionNetworkTargetRepository extends JpaRepository<ProductionNetworkTargetEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ProductionNetworkTargetEntity t WHERE t.targetId = :id")
    Optional<ProductionNetworkTargetEntity> lockById(@Param("id") String id);
}
