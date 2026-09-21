package com.simba.snip.npo.productioncampaign.repository;

import com.simba.snip.npo.productioncampaign.entity.ProductionChangeCampaignEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductionChangeCampaignRepository extends JpaRepository<ProductionChangeCampaignEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ProductionChangeCampaignEntity c where c.campaignId = :id")
    Optional<ProductionChangeCampaignEntity> lockById(@Param("id") UUID id);
}
