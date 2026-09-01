package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionRateLimitStateEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductionRateLimitStateRepository extends JpaRepository<ProductionRateLimitStateEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductionRateLimitStateEntity s WHERE s.counterId = :id")
    Optional<ProductionRateLimitStateEntity> lockById(@Param("id") String counterId);
}
