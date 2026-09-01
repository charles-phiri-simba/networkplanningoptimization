package com.simba.snip.npo.productionchange.repository;

import com.simba.snip.npo.productionchange.entity.ProductionChangeReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductionChangeReviewRepository extends JpaRepository<ProductionChangeReviewEntity, UUID> {

    List<ProductionChangeReviewEntity> findByProductionChangeIdOrderByReviewedAtAsc(UUID productionChangeId);
}
